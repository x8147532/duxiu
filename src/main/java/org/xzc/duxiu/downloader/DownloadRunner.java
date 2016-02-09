package org.xzc.duxiu.downloader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xzc.duxiu.AppConfig;
import org.xzc.duxiu.model.Page;
import org.xzc.duxiu.model.ZxUrl;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Req;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class DownloadRunner {
	final File rootDir = new File( "E:\\下载仓库\\new" );

	/**
	 * 下载为电子书的方式
	 * @throws Exception
	 */
	@Test
	public void downloadPdf() throws Exception {
		//选出状态为0或4的zxurl
		QueryBuilder<ZxUrl, String> qb = zxUrlDao.queryBuilder();
		qb.where().eq( "status", 0 ).or().eq( "status",4 );
		final LinkedBlockingQueue<ZxUrl> list = new LinkedBlockingQueue<ZxUrl>( qb.query() );

		System.out.println( list.size() + "个" );

		int batch = 16;
		ExecutorService es = Executors.newFixedThreadPool( batch );

		//准备一些代理
		final HC hc = HCs.makeHC( 120000, batch, "202.195.192.197", 3128, true );
		//final HC hc = HCs.makeHC( 120000, batch, "202.120.17.158", 2076, true );
		//final HC hc = HCs.makeHC( 120000, batch, "119.254.100.50", 110, "70862045@qq.com", "xzc@7086204511", true,null );
		//final HC hc = HCs.makeHC( 120000, batch, "121.201.14.165", 110, "70862045@qq.com", "xzc@7086204511", true,null );
		//final HC hc = HCs.makeHC( 120000, batch, "222.35.17.177", 2076, true );
		//final HC hc = HCs.makeHC( 120000, batch, null, 2076, true );
		//final HC hc = HCs.makeHC( 120000, batch, "221.163.38.72", 110, true );
		System.out.println( hc.asString( Req.get( "http://1212.ip138.com/ic.asp" ), "gb2312" ) );
		Thread.sleep( 1000 );
		final boolean redownload = true;
		final boolean forceRedownload = false;
		final Object lock = new Object();
		final Set<String> set = new HashSet<String>();
		for (int i = 0; i < batch; ++i) {
			es.submit( new Runnable() {
				public void run() {
					while (true) {
						ZxUrl zxUrl = list.poll();
						if (zxUrl == null)
							break;
						if (forceRedownload
								|| ( redownload && zxUrl.status != 0 && zxUrl.status != 1 && ( zxUrl.updateAt == null
										|| zxUrl.updateAt.getTime() < System.currentTimeMillis()
												- 120 * 60 * 1000 ) )) {
							synchronized (lock) {
								try {
									zxUrl = initZxUrl( hc, zxUrl.zxUrl );
									zxUrlDao.update( zxUrl );
									System.out.println( "更新 " + zxUrl );
								} catch (RuntimeException e) {
									System.out.println( zxUrl );
									e.printStackTrace();
									continue;
								}
								try {
									Thread.sleep( 5000 );
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						try {
							File file;
							Page p;
							synchronized (lock) {
								File dir = new File( rootDir, zxUrl.title );
								if (!dir.exists())
									dir.mkdirs();
								p = Page.parse( zxUrl.pages );
								file = new File( dir, p.getTitle() + ".pdf" );
								if (file.exists()) {
									zxUrl.status = 1;
									zxUrlDao.update( zxUrl );
									continue;
								}
								String key = file.getAbsolutePath();
								if (set.contains( key ))
									continue;
								set.add( key );
							}
							System.out.println(
									String.format( "正在下载 %s [%s]\r\n%s\r\n%s", zxUrl.title, p.getTitle(), zxUrl.zxUrl,
											zxUrl.pdfUrl ) );
							byte[] data = hc.getAsByteArray( zxUrl.pdfUrl );
							if (data.length > 1024) {
								System.out.println(
										String.format( "成功 %s [%s]", zxUrl.title, p.getTitle() ) );
								FileUtils.writeByteArrayToFile( file, data );
								zxUrl.status = 1;
								zxUrlDao.update( zxUrl );
							} else {
								String content = new String( data );
								if (content.contains( "405 Method Not Allowed" )
										|| content.contains( "403 Forbidden" ) || content.contains( "读取PDF失败" )
										|| content.contains( "You don't have permission to access" )) {
									zxUrl.status = 4;
									zxUrlDao.update( zxUrl );
									System.out.println( "4 " + zxUrl );
								} else {
									System.out.println( content );
									System.out.println( "有问题" );
									System.out.println( zxUrl.zxUrl );
									System.out.println( zxUrl.pdfUrl );
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {

						}
					}
				}
			} );
		}
		es.shutdown();
		es.awaitTermination( 1, TimeUnit.HOURS );
		System.out.println( "结束" );
	}

	@Autowired
	private RuntimeExceptionDao<ZxUrl, String> zxUrlDao;

	/**
	 * 初始化这个zxurl
	 * @param hc
	 * @param url
	 * @return
	 */
	private static ZxUrl initZxUrl(HC hc, String url) {
		String content = hc.getAsString( url );
		String title = StringUtils.substringBetween( content, "<title>", "</title>" );
		if (content.contains( "您要访问的链接已经失效" )) {
			System.out.println( url );
			System.out.println( "已经失效" );
			throw new IllegalStateException( "已经失效" );
		} else if (title.contains( "提示页面" )) {
			System.out.println( "验证码!" );
			System.out.println( url );
			System.exit( 0 );
			//throw new IllegalStateException( "验证码" );
		}
		String s = "\\/:*?\"<>|";
		for (int i = 0; i < s.length(); ++i)
			title = title.replace( s.charAt( i ), '_' );

		String pdfUrl = "http://bfts.5read.com/ss2pdf/ss2pdf.dll?"
				+ StringUtils.substringBetween( content, "http://bfts.5read.com/ss2pdf/ss2pdf.dll?", "\"" );
		ZxUrl zxUrl = new ZxUrl();
		zxUrl.pages = StringUtils.substringBetween( content, "var pages = ", ";" );
		zxUrl.title = title;
		zxUrl.zxUrl = url;
		zxUrl.pdfUrl = pdfUrl;
		zxUrl.dxNumber = StringUtils.substringBetween( content, "dxid=", "&" );
		zxUrl.updateAt = new Date();
		zxUrl.status = 0;
		return zxUrl;
	}

	@Test
	public void 将咨询获得的url导入数据库() throws Exception {
		final LinkedBlockingQueue<String> urls = new LinkedBlockingQueue<String>(
				new HashSet<String>(FileUtils.readLines( new File( "zxurls" ) )) );
		int batch = 16;
		final HC hc = HCs.makeHC( 30000, batch, "202.120.17.158", 2076, true );
		ExecutorService es = Executors.newFixedThreadPool( batch );
		for (int i = 0; i < batch; ++i) {
			es.submit( new Callable<Void>() {
				public Void call() throws Exception {
					while (true) {
						String url = urls.poll();
						if (url == null)
							break;
						ZxUrl zxUrl = zxUrlDao.queryForId( url );
						if (zxUrl != null)
							continue;
						Thread.sleep( 1000 );
						System.out.println( url );
						zxUrl = initZxUrl( hc, url );

						/*QueryBuilder<ZxUrl, String> qb = zxUrlDao.queryBuilder();
						qb.where().eq( "dxNumber", zxUrl.dxNumber ).and().eq( "pages", zxUrl.pages );
						if (qb.countOf() == 0L)
							zxUrl.status = 0;
						else
							zxUrl.status = -2;*/
						zxUrlDao.create( zxUrl );
					}
					return null;
				}
			} );
		}
		es.shutdown();
		es.awaitTermination( 1, TimeUnit.HOURS );
		System.out.println( "结束" );
	}

	public static class Result {
		public boolean hasHeader;
		public int maxPage;
		public List<int[]> que = new ArrayList<int[]>();
	}

	@Test
	public void 查看缺少() throws Exception {
		File dir = rootDir;
		File[] ds = dir.listFiles( new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory();
			}
		} );
		for (File d : ds) {
			Result r = new Result();
			List<int[]> pages = new ArrayList<int[]>();
			String[] ss = d.list();
			for (String s : ss)
				if (s.equals( "0header.pdf" )) {
					r.hasHeader = true;
				} else {
					String[] ss2 = StringUtils.substringBefore( s, "." ).split( "-" );
					int from = Integer.parseInt( ss2[0] );
					int to = Integer.parseInt( ss2[1] );
					r.maxPage = Math.max( r.maxPage, to );
					pages.add( new int[] { from, to } );
				}
			Collections.sort( pages, new Comparator<int[]>() {
				public int compare(int[] o1, int[] o2) {
					return o1[0] - o2[0];
				}
			} );
			boolean que = false;
			if (!r.hasHeader) {
				que = true;
			}
			int last = 0;
			for (int[] p : pages) {
				int from = p[0];
				int to = p[1];
				if (last + 1 != from) {
					que = true;
				}
				last = to;
			}
			if (que) {
				System.out.println( d.getName() + " max=" + r.maxPage );
				if (!r.hasHeader)
					System.out.println( "缺少头" );
				last = 0;
				for (int[] p : pages) {
					int from = p[0];
					int to = p[1];
					if (last + 1 != from) {
						System.out.println( "缺少 " + ( last + 1 ) + "-" + ( from - 1 ) );
					}
					last = to;
				}
				System.out.println();
			}
		}
	}
}
