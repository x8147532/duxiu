package org.xzc.duxiu.b0119;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.xzc.duxiu.model.Book;
import org.xzc.duxiu.model.Email;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Req;
import org.xzc.vcode.PositionManager;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class })
public class Main {
	@Autowired
	private RuntimeExceptionDao<Book, String> bookDao;
	@Autowired
	private RuntimeExceptionDao<Email, Integer> emailDao;

	private static final File VCODE_FILE = new File( "vcode.png" );

	@Autowired
	private BookService bs;

	@Test
	public void 开始咨询书本() throws Exception {
		QueryBuilder<Book, String> qb = bookDao.queryBuilder();
		qb.where().eq( "status", 0 );
		//qb.where().eq( "status", 1 ).and().lt( columnName, value )
		LinkedBlockingDeque<Book> bookList = new LinkedBlockingDeque<Book>( qb.query() );
		PositionManager pm = new PositionManager() {
			public boolean canContinue() {
				try {
					return bookDao.queryBuilder().where().eq( "status", 0 ).countOf() > 0;
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
			}
		};
		pm.setBatch( 3 );
		pm.setSave( true );
		pm.init();

		ExecutorService es = Executors.newFixedThreadPool( 16 );
		for (int i = 0; i < 9; ++i) {
			Step2Worker sw = new Step2Worker( "sw" + i, es, pm, bookList, bookDao, emailDao, bs );
			sw.initAsync();
		}

		pm.loop();
		es.shutdown();
		es.awaitTermination( 1, TimeUnit.SECONDS );
	}

	@Test
	public void test1_简化() throws Exception {
		HC hc = HCs.makeHC( 30000, 2, null, 2076, false );
		//String cookie = "msign_dsr=1452673887279; bdshare_firstime=1452993030281; duxiu=userName%5fdsr%2c%3djskjdx%2c%21userid%5fdsr%2c%3d2115%2c%21char%5fdsr%2c%3d%u804a%2c%21metaType%2c%3d265%2c%21dsr%5ffrom%2c%3d1%2c%21logo%5fdsr%2c%3dlogo0408%2ejpg%2c%21logosmall%5fdsr%2c%3dsmall0408%2ejpg%2c%21title%5fdsr%2c%3d%u6c5f%u82cf%u79d1%u6280%u5927%u5b66%2c%21url%5fdsr%2c%3debook%2c%21compcode%5fdsr%2c%3d1488%2c%21province%5fdsr%2c%3d%u6c5f%u82cf%2c%21readDom%2c%3d0%2c%21isdomain%2c%3d4363%2c%21showcol%2c%3d0%2c%21hu%2c%3d0%2c%21uscol%2c%3d0%2c%21isfirst%2c%3d0%2c%21istest%2c%3d0%2c%21cdb%2c%3d0%2c%21og%2c%3d0%2c%21testornot%2c%3d1%2c%21remind%2c%3d0%2c%21datecount%2c%3d346%2c%21userIPType%2c%3d2%2c%21lt%2c%3d0%2c%21ttt%2c%3dduxiu%2c%21enc%5fdsr%2c%3dA4E4D6B780BF9A42B58FC1D1E82F4896; AID_dsr=1195; DSSTASH_LOG=C%5f1%2dUN%5f1195%2dUS%5f2115%2dT%5f1453274033508; JSESSIONID=160264B479958A1A0C86A420CF733FDE.tomcat217; CNZZDATA2088844=cnzz_eid%3D2035744086-1452671659-http%253A%252F%252Fqw.duxiu.com%252F%26ntime%3D1453273364";
		//String cookie = "msign_dsr=1452673887279; bdshare_firstime=1452993030281; DSSTASH_LOG=C%5f1%2dUN%5f1071%2dUS%5f1923%2dT%5f1453190803584; duxiu=userName%5fdsr%2c%3dshjtdx%2c%21userid%5fdsr%2c%3d1923%2c%21char%5fdsr%2c%3d%u9600%2c%21metaType%2c%3d769%2c%21dsr%5ffrom%2c%3d1%2c%21logo%5fdsr%2c%3dlogo0408%2ejpg%2c%21logosmall%5fdsr%2c%3dsmall0408%2ejpg%2c%21title%5fdsr%2c%3d%u4e0a%u6d77%u4ea4%u901a%u5927%u5b66%2c%21url%5fdsr%2c%3debook%2c%21compcode%5fdsr%2c%3d2219%2c%21province%5fdsr%2c%3d%u4e0a%u6d77%2c%21readDom%2c%3d0%2c%21isdomain%2c%3d4491%2c%21showcol%2c%3d0%2c%21hu%2c%3d0%2c%21uscol%2c%3d0%2c%21isfirst%2c%3d0%2c%21istest%2c%3d0%2c%21cdb%2c%3d0%2c%21og%2c%3d0%2c%21testornot%2c%3d1%2c%21remind%2c%3d0%2c%21datecount%2c%3d317%2c%21userIPType%2c%3d2%2c%21lt%2c%3d0%2c%21ttt%2c%3dduxiu%2c%21enc%5fdsr%2c%3d21BB4A2FBA2A116639409B865C623EC6; AID_dsr=1071; CNZZDATA2088844=cnzz_eid%3D2035744086-1452671659-http%253A%252F%252Fqw.duxiu.com%252F%26ntime%3D1453190557; JSESSIONID=903CF4DCFA892802FA029E485E168FFF.tomcat217";
		List<String> urls = FileUtils.readLines( new File( "urls" ) );
		for (String url : urls) {
			//System.out.println( hc.asString( Req.get( url ).cookie( cookie ) ) );
			//break;
			addBook( hc, url );
		}
	}

	@Test
	public void test1() throws Exception {
		HC hc = HCs.makeHC( "202.120.17.158", 2076, false );
		String url0 = "http://book.duxiu.com/advsearch?Book=分布式系统&bstype=1&rn=50&ecode=utf-8&Sort=&channel=search&Pages=";
		//String url0 = "http://book.duxiu.com/search?channel=search&gtag=&sw=分布式系统&ecode=utf-8&Field=1&Sort=&adminid=&btype=&seb=0&pid=0&year=&sectyear=&showc=0&fenleiID=&searchtype=1&authid=0&exp=0&expertsw=&Pages=";
		int page = 0;
		while (true) {
			++page;
			System.out.println( "page=" + page );
			String content = hc.asString( Req.get( url0 + page ) );
			if (content.contains( "我们检测到您的操作可能有异常" )) {
				System.out.println( "异常" );
				break;
			}
			Document d1 = Jsoup.parse( content );
			Elements es1 = d1.select( ".book1 #bb a" );
			if (es1.isEmpty())
				break;
			for (Element e1 : es1) {
				String url = "http://book.duxiu.com/" + e1.attr( "href" );
				addBook( hc, url, true );
			}
			Thread.sleep( 1000 );
		}
	}

	private void addBook(HC hc, String url) {
		addBook( hc, url, false );
	}

	private void addBook(HC hc, String url, boolean update) {
		String dxNumber = StringUtils.substringBetween( url, "dxNumber=", "&" );
		String dd = StringUtils.substringBetween( url, "d=", "&" );
		Book b = bookDao.queryForId( dxNumber );
		if (b != null && !update)
			return;
		b = new Book();
		b.dxNumber = dxNumber;
		b.d = dd;
		String content = hc.asString( Req.get( url ) );
		if (content.contains( "我们检测到您的操作可能有异常" )) {
			System.out.println( content );
			System.out.println( "异常" );
			System.exit( 0 );
		}
		Document d = Jsoup.parse( content );
		String title = d.select( "#topsw" ).val();
		int maxPage = 0;
		String chuandiUrl = null;
		for (Element e : d.select( ".right.info p" )) {
			String text = e.text();
			if (text.startsWith( "【页 数】" )) {
				Pattern pattern = Pattern.compile( "(\\d+)" );
				Matcher ma = pattern.matcher( text );
				if (ma.find()) {
					maxPage = Integer.parseInt( ma.group( 1 ) );
				} else {
					System.out.println( "没有找到页数 " + title + " " + url );
				}
				//maxPage = Integer.parseInt( StringUtils.substringAfter( text, "【页 数】" ).trim() );
				break;
			}
		}
		for (Element e : d.select( ".do .btn a" )) {
			String text = e.text();
			if (text.contains( "图书馆文献传递" )) {
				chuandiUrl = StringUtils.substringBetween( e.attr( "href" ), "'", "'" );
			} else if (text.contains( "包库全文阅读" )) {
				b.baoku = true;
			}
		}
		b.title = title;
		b.maxPage = maxPage;
		b.url = url;
		b.updateAt = new Date();
		if (chuandiUrl != null) {
			b.chuandiUrl = chuandiUrl;
			b.status = 0;
		} else {
			b.status = -1;
		}
		if (b.baoku) {
			System.out.println( "发现全文" );
			b.status = -2;
		}
		if (update) {
			Book bindb = bookDao.queryForId( b.dxNumber );
			bindb.chuandiUrl = b.chuandiUrl;
			bindb.status = b.status;
			bookDao.update( bindb );
		} else
			bookDao.create( b );
	}

	@Test
	public void testpage() {
		int maxPage = 245;
		int from, to = maxPage;
		int maxPagePerTime = 50;
		while (true) {
			from = to - Math.min( maxPagePerTime, maxPage / 5 ) + 1;
			from = Math.max( from, 1 );
			System.out.println( from + "->" + to );
			to = from - 1;
			if (from == 1)
				break;
		}
	}
}