package org.xzc.duxiu.downloader;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xzc.duxiu.model.Book;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Params;
import org.xzc.http.Req;

@Deprecated
public class ZixunRunner {
	private String email;
	private Scanner scanner;
	private String yzm;
	private BasicCookieStore bcs;
	private HC hc;

	public ZixunRunner() {
		bcs = new BasicCookieStore();
		hc = HCs.makeHC( bcs );
		scanner = new Scanner( System.in );
		email = "duruofeixh2@163.com";
	}

	private static final String[] KEYS = {};
	private static final String[] KEYS2 = {};

	public void process(Book b) throws Exception {
		String url = "http://book.duxiu.com/gofirstdrs.jsp?dxNumber=000015527100&d=449DBD02837967B2AB873B1A5CE1BE44";
		String content;
		Document d;
		Params params;
		String cookie = "";
		//第一步 进入页面
		//第二步 模拟点击文献咨询
		content = hc.asString( Req.get( url ).cookie( cookie ) );
		d = Jsoup.parse( content );
		params = new Params();
		for (String key : KEYS) {
			params.add( "fbf." + key, d.select( "[name=fbf." + key + "]" ).val() );
		}
		CloseableHttpResponse res = hc
				.asRes( Req.get( "http://www.xvccs.cn/book.do" ).params( params ) );
		content = EntityUtils.toString( res.getEntity(), "utf-8" );
		HttpClientUtils.closeQuietly( res );
		FileUtils.writeByteArrayToFile( new File( "vcode.png" ),
				hc.asByteArray( Req.get( "http://www.xvccs.cn/vImage.jsp" ) ) );
		Scanner scanner = new Scanner( System.in );
		//第三步 填写咨询的内容 并提交
		params = new Params();
		d = Jsoup.parse( content );
		for (String key : KEYS2)
			params.add( "fbf." + key, d.select( "[name=fbf." + key + "]" ).val() );
		params.add( "fbf.email", "duruofeixh2@163.com" );
		params.add( "fbf.uppages", "1-1" );
		params.add( "fbf.mulutag", "1" );
		System.out.println( "请输入验证码" );
		String yzm = scanner.nextLine();
		params.add( "fbf.verifycode", yzm );
		params.add( "x", "44" );
		params.add( "y", "10" );
		Req req = Req.get( "http://www.xvccs.cn/booksubmit.do" ).params( params );
		content = hc.asString( req );
		//处理返回结果
		System.out.println( content );
	}

	private void doAfter(String yzm) {

	}

}
