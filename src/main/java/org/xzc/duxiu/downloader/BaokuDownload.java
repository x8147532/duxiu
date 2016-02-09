package org.xzc.duxiu.downloader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JScrollBar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.BasicCookieStore;
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

	public static class HC2 {
		public HC hc;
		public boolean ok;

		public HC2(HC hc) {
			this.hc = hc;
			this.ok = true;
		}

	}

	public void 处理包库验证码问题() throws IOException {
		final HC hc = HCs.makeHC( timeout, batch, "202.120.17.158", 2076, false, null );
		//http://img.sslibrary.com/n/antispiderShowVerify.ac
		Scanner scanner = new Scanner( System.in );
		while (true) {
			byte[] data = hc.getAsByteArray( "http://img.sslibrary.com/n/processVerifyPng.ac" );
			FileUtils.writeByteArrayToFile( new File( "vcode_0.png" ), data );
			System.out.println( "请输入验证码" );
			String yzm = scanner.nextLine();
			String content = hc.getAsString( "http://img.sslibrary.com/n/processVerify.ac?ucode=" + yzm );
			if (!content.contains( "验证码输入有误" )) {
				//成功
				break;
			}
		}
		hc.close();
	}

	File rootDir = new File( "E:\\下载仓库\\bk" );

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

	@Autowired
	private RuntimeExceptionDao<BKBook, String> bkbookDao;

	@Test
	public void 将可以包库的图书加入数据库() throws Exception {
		String searchUrl = "http://book.duxiu.com/search?channel=search&gtag=&sw=%E4%BA%91%E8%AE%A1%E7%AE%97&ecode=utf-8&Field=all&Sort=3&adminid=1071&btype=1&seb=0&pid=0&year=&sectyear=&showc=0&fenleiID=&searchtype=1&authid=0&exp=0&expertsw=&Pages=";
		int index = 1;
		//final HC hc = HCs.makeHC( timeout, batch, "202.120.17.158", 2076, false, null );
		final HC hc = HCs.makeHC( timeout, batch, "202.195.192.197", 3128, false, null );
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
					bb.title = StringUtils.substringBetween( titleElement.text(), "《", "》" );
					String s = "\\/:*?\"<>|";
					for (int i = 0; i < s.length(); ++i)
						bb.title = bb.title.replace( s.charAt( i ), '_' );

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

	private static final int batch = 16;
	private static final int timeout = 120000;

	@Test
	public void 下载包库图书() throws Exception {
		QueryBuilder<BKBook, String> qb = bkbookDao.queryBuilder();
		qb.where().eq( "status", 0 );
		List<BKBook> list = qb.query();

		BasicCookieStore bcs = new BasicCookieStore();
		final HC hc = HCs.makeHC( timeout, batch, "202.195.192.197", 3128, false, bcs );
		//final HC hc = HCs.makeHC( timeout, batch, "202.120.17.158", 2076, false, bcs );
		//final HC hc = HCs.makeHC( timeout, batch, null, 2076, true, null );
		BaokuService bs = new BaokuService( hc );
		int failCount = 0;
		for (BKBook bb : list) {
			String baokuUrl = bs.getBaokuUrl( bb );
			if (baokuUrl == null) {
				continue;
			}
			System.out.println( "开始下载 " + bb );
			if (!download( hc, bb, baokuUrl, bs )) {
				if (++failCount >= 3)
					break;
			} else {
				failCount = 0;
			}
			bcs.clear();
		}
	}

	public boolean download(final HC hc, BKBook bb, String bkUrl, final BaokuService bs) throws Exception {
		System.out.println( bkUrl );
		String content = hc.getAsString( bkUrl );
		if (content.contains( "您要访问的链接已经失效" )) {
			System.out.println( "您要访问的链接已经失效" );
			return false;
		} else if (content.contains( "我们检测到您的操作可能有异常" )) {
			System.out.println( "我们检测到您的操作可能有异常\r\n请去 http://img.sslibrary.com/n/antispiderShowVerify.ac 解锁" );
			return false;
		} else if (content.contains( "您访问过快" )) {
			System.out.println( "您访问过快" );
			return false;
		}
		String title = StringUtils.substringBetween( content, "<title>", "</title>" ).trim();
		System.out.println( title );
		final String jpgPath = "http://img.sslibrary.com"
				+ StringUtils.substringBetween( content, "jpgPath: \"", "\"" );
		String pagesStr = StringUtils.substringBetween( content, "pages = ", ";" );
		Pattern pattern = Pattern.compile( "\\[(\\d+), (\\d+)\\]" );
		Matcher matcher = pattern.matcher( pagesStr );
		int index = 0;
		List<Page> pages = new ArrayList<Page>();
		Map<Integer, String> indexToPrefix = new HashMap<Integer, String>();
		indexToPrefix.put( 1, "bok" );
		indexToPrefix.put( 2, "leg" );
		indexToPrefix.put( 3, "fow" );
		indexToPrefix.put( 4, "!" );
		indexToPrefix.put( 5, "0" );
		indexToPrefix.put( 7, "cov" );
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
		final File dir = new File( rootDir, title );
		final LinkedBlockingDeque<Page> pages2 = new LinkedBlockingDeque<Page>( pages );
		ExecutorService es = Executors.newFixedThreadPool( batch );
		final Map<Page, Integer> count = new HashMap<Page, Integer>();
		final AtomicBoolean fullySuccess = new AtomicBoolean( true );
		final AtomicInteger exceptionCount = new AtomicInteger( 0 );
		final AtomicBoolean stop=new AtomicBoolean( false );
		for (int i = 0; i < batch; ++i) {
			es.submit( new Runnable() {
				public void run() {
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
								HttpHost proxy = hc.getProxy();
								System.out.println(
										proxy == null ? "本机"
												: hc.getProxy().getHostName() + " : "
														+ "我们检测到您的操作可能有异常\r\n请去 http://img.sslibrary.com/n/antispiderShowVerify.ac 解锁" );
								fullySuccess.set( false );
								stop.set( true );
								break;
								/*
								if (exceptionCount.incrementAndGet() < 100) {
									Thread.sleep( 1000 );
									continue;
								} else {
									System.exit( 0 );
								}*/
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
		if (fullySuccess.get()) {
			bb.status = 1;
			bkbookDao.update( bb );
		}
		System.out.println();
		return true;
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
}
