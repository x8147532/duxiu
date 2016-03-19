package org.xzc.duxiu.downloader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xzc.duxiu.AppConfig;
import org.xzc.duxiu.model.BKBook;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Req;

import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class })
public class BaokuDownload {

	public static final class Page {
		public String url;
		public String name;

		public Page(String url, String name) {
			this.url = url;
			this.name = name;
		}
	}

	private static final File rootDir;
	private static final File finishedRootDir;
	private static final File finishedPdfRootDir;

	static {
		Properties p = new Properties();
		try {
			p.load( new FileInputStream( "config.properties" ) );
		} catch (Exception e) {
		}
		rootDir = new File( p.getProperty( "rootDir" ) );
		finishedRootDir = new File( p.getProperty( "finishedRootDir" ) );
		finishedPdfRootDir = new File( p.getProperty( "finishedPdfRootDir" ) );
	}

	private static final FileFilter fileFilter = new FileFilter() {
		public boolean accept(File f) {
			return f.isFile();
		}
	};

	private static final FileFilter dirFilter = new FileFilter() {
		public boolean accept(File f) {
			return f.isDirectory();
		}
	};

	private static final int batch = 16;
	private static final int timeout = 120000;

	private static final Pattern pattern = Pattern.compile( "\\[(\\d+), (\\d+)\\]" );

	private static final Map<Integer, String> indexToPrefix = new HashMap<Integer, String>();

	static {
		indexToPrefix.put( 1, "bok" );
		indexToPrefix.put( 2, "leg" );
		indexToPrefix.put( 3, "fow" );
		indexToPrefix.put( 4, "!" );
		indexToPrefix.put( 5, "0" );
		indexToPrefix.put( 7, "cov" );
	}

	private static final int addPages(List<Page> pages, String prefix, int from, int to, int headerIndex) {
		for (int i = from; i <= to; ++i) {
			String name;
			if (prefix.charAt( 0 ) == '0') {
				name = Integer.toString( i );
			} else {
				name = "0_header_" + ( headerIndex++ );
			}
			String url = prefix + StringUtils.leftPad( Integer.toString( i ), 6 - prefix.length(), '0' );
			pages.add( new Page( url, name ) );
		}
		return headerIndex;
	}

	private static List<Page> makePages(String pagesStr) {
		Matcher matcher = pattern.matcher( pagesStr );
		int index = 0;
		List<Page> pages = new ArrayList<Page>();
		int headerIndex = 1;
		while (matcher.find()) {
			int from = Integer.parseInt( matcher.group( 1 ) );
			int to = Integer.parseInt( matcher.group( 2 ) );
			if (from <= to) {
				String prefix = indexToPrefix.get( index );
				if (prefix == null)
					continue;
				headerIndex = addPages( pages, prefix, index == 7 ? 1 : from, to, headerIndex );
			}
			++index;
		}
		return pages;
	}

	@Autowired
	private RuntimeExceptionDao<BKBook, String> bkbookDao;

	final BasicCookieStore bcs = new BasicCookieStore();

	//final HC hc = HCs.makeHC( timeout, batch, "202.120.17.158", 2076, false, bcs );
	final HC hc = HCs.makeHC( timeout, batch, "202.195.192.197", 3128, false, bcs );
	//final HC hc = HCs.makeHC( timeout, batch, "202.120.17.177", 2076, false, bcs );

	public void 处理包库验证码问题(HC hc) throws IOException {
		Scanner scanner = new Scanner( System.in );
		while (true) {
			CloseableHttpResponse res = hc.asRes( Req.get( "http://img.sslibrary.com/n/processVerifyPng.ac" ) );
			int code = res.getStatusLine().getStatusCode();
			if (code != 200)
				break;
			byte[] data = EntityUtils.toByteArray( res.getEntity() );
			HttpClientUtils.closeQuietly( res );
			FileUtils.writeByteArrayToFile( new File( "vcode_0.png" ), data );
			System.out.println( "请输入验证码" );
			String yzm = scanner.nextLine();
			String content = hc.getAsString( "http://img.sslibrary.com/n/processVerify.ac?ucode=" + yzm );
			if (!content.contains( "验证码输入有误" )) {
				break;
			}
		}
	}

	public static String normalize(String name) {
		//格式化标题
		String s = "\\/:*?\"<>|";
		for (int i = 0; i < s.length(); ++i)
			name = name.replace( s.charAt( i ), '_' );
		return name;
	}

	@Test
	public void 将可以包库的图书加入数据库() throws Exception {
		String searchUrl = "http://book.duxiu.com/search?channel=search&gtag=&sw=%E6%97%A5%E8%AF%AD&ecode=utf-8&Field=all&Sort=&adminid=1071&btype=1&seb=0&pid=0&showc=0&fenleiID=&searchtype=1&authid=0&exp=0&expertsw=&sectyear=2012&Pages=";
		int index = 1;
		Scanner scanner = new Scanner( System.in );
		while (true) {
			System.out.println( index + "页" );
			String url = searchUrl + index;
			String content = hc.getAsString( url );
			Document doc = Jsoup.parse( content, url );
			Elements es = doc.select( ".book1 img" );
			boolean find = false;
			for (Element e : es) {
				if (e.attr( "src" ).equals( "/images/readAll_bk.jpg" )) {
					find = true;
					String imgUrl = e.parents().select( ".book1" ).select( "#b_img img" ).first().absUrl( "src" );
					FileUtils.writeByteArrayToFile( new File( "vcode_0.png" ), hc.getAsByteArray( imgUrl ) );
					String bkUrl = e.parent().absUrl( "href" );
					Element titleElement = e.parent().parent().select( "a" ).first();
					BKBook bb = new BKBook();
					bb.dxid = StringUtils.substringBetween( bkUrl, "dxid=", "&" );
					if (bkbookDao.queryForId( bb.dxid ) != null) {
						continue;
					}
					//bb.title = StringUtils.substringBetween( titleElement.text(), "《", "》" );
					bb.title = titleElement.text();
					bb.title = bb.title.substring( 1, bb.title.length() - 1 );

					bb.title = normalize( bb.title );

					bb.url = titleElement.absUrl( "href" );
					System.out.println( String.format( "是否要包库下载 %s %s ? (0:否, 1:是)", bb.title, bb.url ) );
					bb.status = scanner.nextInt() == 1 ? 0 : -2;
					bkbookDao.create( bb );
				}
			}
			if (!find)
				break;
			++index;
		}
	}

	@Test
	public void 将已经完成的图书转移到指定目录() throws SQLException {
		QueryBuilder<BKBook, String> qb = bkbookDao.queryBuilder();
		qb.where().eq( "status", 1 );
		final List<BKBook> list = qb.query();
		System.out.println( "共有" + list.size() + "个" );
		Map<String, File> map = new HashMap<String, File>();
		for (File d : rootDir.listFiles( dirFilter )) {
			String name = d.getName();
			String dxid = StringUtils.substringAfterLast( name, "_" );
			map.put( dxid, d );
		}
		for (BKBook bb : list) {
			File d = map.get( bb.dxid );
			if (d == null)
				continue;
			//System.out.println(
			//		d.getAbsolutePath() + " -> " + new File( finishedRootDir, d.getName() ).getAbsolutePath() );
			if (d.renameTo( new File( finishedRootDir, d.getName() ) )) {
				bb.status = 3;
			}
		}
		bkbookDao.callBatchTasks( new Callable<Void>() {
			public Void call() throws Exception {
				for (BKBook bb : list)
					bkbookDao.update( bb );
				return null;
			}
		} );
	}

	@Test
	public void 将已经转成pdf的图书转移到指定目录() throws SQLException {
		for (File d : finishedRootDir.listFiles( dirFilter )) {
			//File from = new File( d, "全页照片.pdf" );
			File to = new File( d, d.getName() + ".pdf" );
			//if (from.exists()) {
			//	from.renameTo( to );
			//}
			if (to.exists()) {
				if (!d.renameTo( new File( finishedPdfRootDir, d.getName() ) )) {
					System.out.println( "移动 " + d.getAbsolutePath() + " 失败, 可能是正在被使用." );
				}
			}
		}
	}

	@Test
	public void 删除无效文件() throws IOException {
		BaokuService bs = new BaokuService( null );

		//列出文件夹
		File[] dirs = rootDir.listFiles( new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory();
			}
		} );

		for (File d : dirs) {
			//列出文件
			File[] files = d.listFiles( new FileFilter() {
				public boolean accept(File f) {
					return f.isFile() && f.length() < 40 * 1024; //文件并且小于40KB
				}
			} );
			for (File f : files) {
				try {
					byte[] data = FileUtils.readFileToByteArray( f );
					if (bs.isInvalid( data )) {
						System.out.println( "删除 " + f.getAbsolutePath() );
						f.delete();
					}
				} catch (Exception e) {
				}
			}
		}
	}

	//final HC hc = HCs.makeHC( timeout, batch, "202.120.17.158", 2076, false, bcs );
	@Test
	public void 下载包库图书() throws Exception {
		QueryBuilder<BKBook, String> qb = bkbookDao.queryBuilder();
		qb.where().eq( "status", 0 );
		List<BKBook> list = qb.query();

		BaokuService bs = new BaokuService( hc );

		int failCount = 0;
		for (int i = 0; i < list.size(); ++i) {
			bcs.clear();

			BKBook bb = list.get( i );
			String baokuUrl = bs.getBaokuUrl( bb );
			if (baokuUrl == null) {
				continue;
			}
			int status = download( hc, bb, baokuUrl, bs, bcs );
			System.out.println( "status=" + status );
			if (status == 0) {
				failCount = 0;
			} else if (status == 1 || status == -1) {
				--i;
				failCount = 0;
			} else {
				--i;
				处理包库验证码问题( hc );
			}
		}
	}

	private static void clearPng(File d) {
		File[] files = d.listFiles( new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith( ".png" );
			}
		} );
		for (File f : files) {
			f.delete();
		}
	}

	@Test
	public void 制作pdf() throws Exception {
		final LinkedBlockingQueue<File> dirs = new LinkedBlockingQueue<File>(
				Arrays.asList( finishedRootDir.listFiles( dirFilter ) ) );
		int batch = 1;
		System.out.println( dirs.size() + "个" );
		ExecutorService es = Executors.newFixedThreadPool( batch );
		for (int i = 0; i < batch; ++i)
			es.submit( new Callable<Void>() {
				public Void call() throws Exception {
					File d;
					while (( d = dirs.poll() ) != null) {
						File to = new File( d, d.getName() + ".pdf" );
						if (to.exists()) {
							clearPng( d );
							continue;
						}
						System.out.println( d.getAbsolutePath() );
						File[] files = d.listFiles();
						Arrays.sort( files, new Comparator<File>() {
							public int compare(File o1, File o2) {
								String n1 = o1.getName();
								String n2 = o2.getName();
								int v1 = n1.startsWith( "0_header_" )
										? Integer.parseInt( StringUtils.substringBetween( n1, "0_header_", ".png" ) )
												- 10000
										: Integer.parseInt( StringUtils.substringBefore( n1, ".png" ) );
								int v2 = n2.startsWith( "0_header_" )
										? Integer.parseInt( StringUtils.substringBetween( n2, "0_header_", ".png" ) )
												- 10000
										: Integer.parseInt( StringUtils.substringBefore( n2, ".png" ) );
								return v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
							}
						} );
						Image img = Image.getInstance( files[0].toURI().toURL() );
						com.itextpdf.text.Document document = new com.itextpdf.text.Document( img );
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream( to );
							PdfWriter.getInstance( document, fos );
							document.open();
							for (File image : files) {
								img = Image.getInstance( image.toURI().toURL() );
								document.setPageSize( img );
								document.newPage();
								img.setAbsolutePosition( 0, 0 );
								document.add( img );
							}
							document.close();
							IOUtils.closeQuietly( fos );
							clearPng( d );
						} catch (Exception e) {
							e.printStackTrace();
							IOUtils.closeQuietly( fos );
							to.delete();
						}
					}
					return null;
				}
			} );
		es.shutdown();
		es.awaitTermination( 1, TimeUnit.HOURS );
	}

	/**
	 * 
	 * @param hc
	 * @param bb
	 * @param bkUrl
	 * @param bs
	 * @return
	 * 没有任何问题就返回0 曾经出现过验证码问题或下载失败返回1 需要验证码才能继续返回2 其他问题返回-1
	 * @throws Exception
	 */
	public int download(final HC hc, BKBook bb, String bkUrl, final BaokuService bs, final BasicCookieStore bcs)
			throws Exception {
		String content = hc.getAsString( bkUrl );
		if (content.contains( "您要访问的链接已经失效" )) {
			System.out.println( "您要访问的链接已经失效" );
			return -1;
		} else if (content.contains( "我们检测到您的操作可能有异常" )) {
			System.out.println( "我们检测到您的操作可能有异常\r\n请去 http://img.sslibrary.com/n/antispiderShowVerify.ac 解锁" );
			return 2;
		} else if (content.contains( "您访问过快" )) {
			System.out.println( "您访问过快" );
			return -1;
		}
		final String jpgPath = "http://img.sslibrary.com"
				+ StringUtils.substringBetween( content, "jpgPath: \"", "\"" );
		String pagesStr = StringUtils.substringBetween( content, "pages = ", ";" );
		System.out.println( "开始下载" + bb + "\r\n" + pagesStr );
		final File dir = new File( rootDir, bb.title + "_" + bb.dxid );
		final LinkedBlockingDeque<Page> pages2 = new LinkedBlockingDeque<Page>( makePages( pagesStr ) );
		ExecutorService es = Executors.newFixedThreadPool( batch );
		final Map<Page, Integer> count = new HashMap<Page, Integer>();
		final AtomicBoolean fullySuccess = new AtomicBoolean( true );
		final AtomicInteger exceptionCount = new AtomicInteger( 0 );
		final AtomicBoolean stop = new AtomicBoolean( false );
		final AtomicInteger successCount = new AtomicInteger( 0 );
		for (int i = 0; i < batch; ++i) {
			Thread.sleep( 100 );
			es.submit( new Runnable() {
				public void run() {
					int expCount = 0;
					while (!stop.get()) {
						Page p = null;
						try {
							p = pages2.poll();
							if (p == null)
								break;
							File file = new File( dir, p.name + ".png" );
							if (file.exists()) {
								continue;
							}
							byte[] data = hc.getAsByteArray( jpgPath + p.url + "?zoom=2" );
							if (bs.isException( data )) {
								System.out.print( "异常 " );
								if (++expCount >= 5) {
									fullySuccess.set( false );
									stop.set( true );
									break;
								} else {
									pages2.addFirst( p );
									bcs.clear();
									continue;
								}
							}
							if (bs.isInvalid( data )) {
								System.out.println( p.name + " 下载无效" );
								Integer c = count.get( p );
								c = c == null ? 1 : c + 1;
								count.put( p, c );
								if (c < 10)
									pages2.addFirst( p );
								else {
									System.out.println( "重试次数太多 放弃 " + p.name );
									fullySuccess.set( false );
								}
								continue;
							}
							successCount.incrementAndGet();
							FileUtils.writeByteArrayToFile( file, data );
							System.out.print( p.name + " " );
						} catch (Exception e) {
							e.printStackTrace();
							if (p != null)
								pages2.addFirst( p );
						}
					}
				}
			} );
		}
		es.shutdown();
		es.awaitTermination( 1, TimeUnit.HOURS );
		if (fullySuccess.get() && pages2.isEmpty()) {
			bb.status = 1;
			bkbookDao.update( bb );
		}
		System.out.println();
		System.out.println( "本轮成功 " + successCount.get() + "个" );
		return stop.get() ? 1 : 0;
		//return fullySuccess.get() && !stop.get() ? 0 : stop.get() ? 1 : 0;
	}

	public void download2() throws URISyntaxException {
		//0
		//1 书名页 bok001
		//2 版权页 leg001
		//3 前言页 fow001
		//4 目录页 !00001
		//5 正文页
		//6
		//7 封面封底页 cov001
		String url = "http://img.sslibrary.com/n/01c78d8eaf228bc8165b030706f4b0f8MC130517466266/img1/754FA0F52CB726D5AFDAC7DC2CE6213DF7D8954AF3B1D7EF50D29357CAA551F0EBF79C8F545C631814B2D5EF2C250C5C3E911B2BB78A935017673FC87A56BC0EA09E3927AFD50D34562340FEBB7F2F1F65213C2E9CD236C42AACA01AC07B3C2A83B3DA65400A41A7CF2BCB11892AE234DFEE/nf1/qw/12709447/E825B7D539C3404483BA9AF685633AD7/bok001?zoom=0";
		//url = StringUtils.substringBefore( url, "?" );
		//System.out.println( url );
		url = StringUtils.substringBeforeLast( url, "/" );
		url += "/bok001";
		HC hc = HCs.makeHC();
		byte[] data = hc.getAsByteArray( url );
		System.out.println( data.length );
	}

	@Test
	public void test2() {
		List<String> cookies = new ArrayList<String>();
		for (int i = 0; i < 10; ++i)
			cookies.add( getCookie() );

		BasicCookieStore bcs = new BasicCookieStore();
		HC hc = HCs.makeHC( timeout, batch, null, 2076, true, bcs );
		for (int i = 0; i < 1000; ++i) {
			String cookie = cookies.get( i % cookies.size() );
			String ip = "1.9.6.3";
			String content = hc.asString(
					Req.get( "http://book.duxiu.com/bookDetail.jsp?dxNumber=000006716773&d=AD13F9EB00EBD2DA046925371443E8D6" )
							.cookie( cookie ).header( "X-Forwarded-For", ip ).header( "X-Client-IP", ip ) );
			Document doc = Jsoup.parse( content );
			String title = doc.select( "title" ).text();
			System.out.println( title );
		}
		/*
		List<Cookie> cookies = bcs.getCookies();
		StringBuilder sb = new StringBuilder();
		for (Cookie c : cookies) {
			sb.append( c.getName() + "=" + c.getValue() + ";" );
		}
		System.out.println( sb.toString() );*/
	}

	private String getCookie() {
		BasicCookieStore bcs = new BasicCookieStore();
		final HC hc = HCs.makeHC( timeout, 1, "202.120.17.158", 2076, false, bcs );
		hc.consume( Req.get( "http://www.duxiu.com" ) );

		List<Cookie> cookies = bcs.getCookies();
		StringBuilder sb = new StringBuilder();
		for (Cookie c : cookies) {
			sb.append( c.getName() + "=" + c.getValue() + ";" );
		}
		String cookie = sb.toString();
		System.out.println( cookie );
		hc.close();
		cookie = cookie.replace( "JSESSIONID", "abc" );
		return cookie;
	}

	@Test
	public void testIP() throws Exception {
		BasicCookieStore bcs = new BasicCookieStore();
		final HC hc = HCs.makeHC( timeout, batch, null, 2076, true, bcs );
		Scanner scanner = new Scanner( System.in );
		for (int adminid = 0; adminid <= 2000; ++adminid) {
			if (adminid % 100 == 0) {
				System.out.println( adminid );
			}
			String url = "http://book.duxiu.com/search?channel=search&gtag=&sw=%E6%97%A5%E8%AF%AD&ecode=utf-8&Field=all&Sort=&seb=0&year=&sectyear=&fenleiID=&searchtype=1&authid=0&exp=0&expertsw=&btype=1&pid=0&seb=0&showc=&adminid="
					+ adminid;
			String cookie = "AID_dsr=1071;duxiu=userName%5fdsr%2c%3dshjtdx%2c%21userid%5fdsr%2c%3d1923%2c%21char%5fdsr%2c%3d%u9600%2c%21metaType%2c%3d769%2c%21dsr%5ffrom%2c%3d1%2c%21logo%5fdsr%2c%3dlogo0408%2ejpg%2c%21logosmall%5fdsr%2c%3dsmall0408%2ejpg%2c%21title%5fdsr%2c%3d%u4e0a%u6d77%u4ea4%u901a%u5927%u5b66%2c%21url%5fdsr%2c%3debook%2c%21compcode%5fdsr%2c%3d2219%2c%21province%5fdsr%2c%3d%u4e0a%u6d77%2c%21readDom%2c%3d0%2c%21isdomain%2c%3d4491%2c%21showcol%2c%3d0%2c%21hu%2c%3d0%2c%21uscol%2c%3d0%2c%21isfirst%2c%3d0%2c%21istest%2c%3d0%2c%21cdb%2c%3d0%2c%21og%2c%3d0%2c%21testornot%2c%3d1%2c%21remind%2c%3d0%2c%21datecount%2c%3d295%2c%21userIPType%2c%3d2%2c%21lt%2c%3d0%2c%21ttt%2c%3dduxiu%2c%21enc%5fdsr%2c%3d8DAD664B9C7F0D9F89D5B24077D1BE2E;msign_dsr=1455117168998;";
			String ip = "36.36.36.3";
			String content = hc.asString(
					Req.get( url ).cookie( cookie ).header( "X-Forwarded-For", ip ).header( "X-Client-IP", ip ) );
			System.out.println( content );
			if (content.contains( "验证码" )) {
				byte[] data = hc.getAsByteArray( "http://book.duxiu.com/processVerifyPng.ac" );
				FileUtils.writeByteArrayToFile( new File( "vcode_0.png" ), data );
				System.out.println( "请处理验证码" );
				String yzm = scanner.nextLine();
				hc.consume( Req.get( "http://book.duxiu.com/processVerify.ac?ucode=" + yzm ) );
				--adminid;
				bcs.clear();
				continue;
			}
			Document document = Jsoup.parse( content );
			int count = -1;
			try {
				count = Integer.parseInt(
						StringUtils.substringBetween( document.select( "#searchinfo b" ).text(), "中文图书", "种" ).trim() );
			} catch (Exception e) {

			}
			Elements es = document.select( "#leftcat" );
			String school = "";

			if (es.size() > 1) {
				Element e = es.get( 1 );
				if (e != null) {
					school = e.text();
				}
			}
			if (count > 0 || ( count == -1 && !school.equals( "" ) )) {
				System.out.println( adminid + " " + school + " " + count );
			}
		}
	}

	public void 将bkurl文件里的链接加入数据库() throws IOException {
		List<String> lines = FileUtils.readLines( new File( "bkurl" ) );
		HC hc = HCs.makeHC( timeout, batch, "202.120.17.158", 2076, false, null );
		BaokuService bs = new BaokuService( hc );
		for (String url : lines) {
			String dxNumber = StringUtils.substringBetween( url, "dxNumber=", "&" );
			if (!bkbookDao.idExists( dxNumber )) {
				BKBook bb = bs.getBKBook( url );
				bkbookDao.createIfNotExists( bb );
				System.out.println( bb );
			}
		}
	}

}
