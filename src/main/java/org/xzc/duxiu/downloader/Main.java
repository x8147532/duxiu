package org.xzc.duxiu.downloader;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ParseException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.xzc.duxiu.AppConfig;
import org.xzc.duxiu.model.Book;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Params;
import org.xzc.http.Req;

import com.j256.ormlite.dao.RuntimeExceptionDao;

@Deprecated
public class Main {
	private static final String[] KEYS = {
			"bm", "sslogin", "dxid", "spage", "chapters",
			"username", "unitid", "gid", "d", "type",
			"bookname", "author", "isbn", "publisher", "publishdate",
			"ssnumber", "email", "detailurl", "islogin", "choren",
			"mobile", "epage", "ipaduserid", "ipadkey"
	};

	private static final String[] KEYS2 = {
			"bookname", "type", "username", "d", "islogin",
			"sp", "ssuid", "detailurl", "unitid", "dxid",
			"author", "ssnumber", "isbn", "publisher", "publishdate",
			"spage", "ipadkey", "ipaduserid", "ipadtype", "choren", "mobile"
	};

	public static void main(String[] args) throws ParseException, IOException {
		String email = "duruofeixh2@163.com";
		String content;
		Document d;
		Params params;
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext( AppConfig.class );
		RuntimeExceptionDao<Book, String> bookDao = (RuntimeExceptionDao<Book, String>) ac.getBean( "bookDao" );

		Book b = new Book();
		b.dxNumber = "000015527100";
		b.d = "ECCB6B06B08B3B0FF4E6E34401554181";
		BasicCookieStore bcs = new BasicCookieStore();

		initCookie( bcs,
				"msign_dsr=1452673887279; bdshare_firstime=1452993030281; DSSTASH_LOG=C%5f1%2dUN%5f1071%2dUS%5f1923%2dT%5f1453190803584; duxiu=userName%5fdsr%2c%3dshjtdx%2c%21userid%5fdsr%2c%3d1923%2c%21char%5fdsr%2c%3d%u9600%2c%21metaType%2c%3d769%2c%21dsr%5ffrom%2c%3d1%2c%21logo%5fdsr%2c%3dlogo0408%2ejpg%2c%21logosmall%5fdsr%2c%3dsmall0408%2ejpg%2c%21title%5fdsr%2c%3d%u4e0a%u6d77%u4ea4%u901a%u5927%u5b66%2c%21url%5fdsr%2c%3debook%2c%21compcode%5fdsr%2c%3d2219%2c%21province%5fdsr%2c%3d%u4e0a%u6d77%2c%21readDom%2c%3d0%2c%21isdomain%2c%3d4491%2c%21showcol%2c%3d0%2c%21hu%2c%3d0%2c%21uscol%2c%3d0%2c%21isfirst%2c%3d0%2c%21istest%2c%3d0%2c%21cdb%2c%3d0%2c%21og%2c%3d0%2c%21testornot%2c%3d1%2c%21remind%2c%3d0%2c%21datecount%2c%3d317%2c%21userIPType%2c%3d2%2c%21lt%2c%3d0%2c%21ttt%2c%3dduxiu%2c%21enc%5fdsr%2c%3d21BB4A2FBA2A116639409B865C623EC6; AID_dsr=1071; CNZZDATA2088844=cnzz_eid%3D2035744086-1452671659-http%253A%252F%252Fqw.duxiu.com%252F%26ntime%3D1453190557; JSESSIONID=903CF4DCFA892802FA029E485E168FFF.tomcat217" );

		HC hc = HCs.makeHC( bcs );

		//第一步 进入页面
		Book bindb = bookDao.queryForId( b.dxNumber );
		if (bindb != null) {
			b = bindb;
		} else {
			initBook( hc, b );
			bookDao.create( b );
		}
		System.out.println( b );
		System.out.println( b.d );
		//第二步 模拟点击文献咨询
		content = hc.asString(
				Req.get( "http://book.duxiu.com/gofirstdrs.jsp" ).params( "dxNumber", b.dxNumber, "d", b.d ) );
		d = Jsoup.parse( content );
		params = new Params();
		for (String key : KEYS) {
			params.add( "fbf." + key, d.select( "[name=fbf." + key + "]" ).val() );
		}

		Scanner scanner = new Scanner( System.in );
		while (true) {
			if (b.currentPage == b.maxPage)
				break;
			int from = b.currentPage;
			if (from < 1)
				from = 1;
			int to = from + b.maxPage / 5 - 1;
			if (to - from + 1 > 50)
				to = from + 49;
			if (to > b.maxPage)
				to = b.maxPage;
			String uppages = from + "-" + to;
			content = hc.asString( Req.get( "http://www.xvccs.cn/book.do" ).params( params ) );
			//下载验证码
			FileUtils.writeByteArrayToFile( new File( "vcode.png" ),
					hc.asByteArray( Req.get( "http://www.xvccs.cn/vImage.jsp" ) ) );
			//第三步 填写咨询的内容 并提交
			params = new Params();
			d = Jsoup.parse( content );
			for (String key : KEYS2)
				params.add( "fbf." + key, d.select( "[name=fbf." + key + "]" ).val() );
			params.add( "fbf.email", email );
			params.add( "fbf.uppages", uppages );
			params.add( "fbf.mulutag", "1" );
			System.out.println( "请输入验证码" );
			String yzm = scanner.nextLine();
			params.add( "fbf.verifycode", yzm );
			params.add( "x", "44" );
			params.add( "y", "10" );
			Req req = Req.get( "http://www.xvccs.cn/booksubmit.do" ).params( params );
			content = hc.asString( req );
			int status;
			if (content.contains( "咨询提交成功" )) {
				status = 0;
				System.out.println( "成功 " + uppages );
				b.currentPage = to;
				bookDao.update( b );
			} else if (content.contains( "您输入的验证码不正确" )) {
				status = 1;
				System.out.println( "验证码错误" );
			} else if (content.contains( "本书的咨询量已经达到上限" )) {
				status = 2;
				System.out.println( "本书的咨询量已经达到上限" );
				break;
			} else {
				status = -1;
				System.out.println( content );
			}
		}
	}

	private static boolean initBook(HC hc, Book b) {
		Req req = Req.get( "http://book.duxiu.com/bookDetail.jsp" ).params( "dxNumber", b.dxNumber, "d", b.d );
		String content = hc.asString( req );
		System.out.println( content );
		Document d = Jsoup.parse( content );
		for (Element e : d.select( ".do .btn a" )) {
			if (e.text().contains( "图书馆文献传递" )) {
				b.d = StringUtils.substringBetween( e.attr( "href" ), "d=", "')" );
				break;
			}
		}
		for (Element e : d.select( ".right.info table p" )) {
			String text = e.text();
			if (text.contains( "【页  数】" )) {
				b.maxPage = Integer.parseInt( StringUtils.substringAfter( text, "【页  数】" ).trim() );
			}
			if (text.contains( "【页 数】" )) {
				b.maxPage = Integer.parseInt( StringUtils.substringAfter( text, "【页 数】" ).trim() );
			}
		}

		b.title = d.select( "#topsw" ).val();

		return false;
	}

	private static void initCookie(BasicCookieStore bcs, String cookie) {
		for (String s : cookie.split( ";" )) {
			String[] ss = s.split( "=" );
			BasicClientCookie c = new BasicClientCookie( ss[0], ss[1] );
			c.setDomain( ".duxiu.com" );
			c.setPath( "/" );
			c.setSecure( false );
			c.setExpiryDate( DateTime.now().plusYears( 1 ).toDate() );
			bcs.addCookie( c );
		}
	}
}
