package org.xzc.duxiu.b0119;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.BasicCookieStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xzc.duxiu.model.Book;
import org.xzc.duxiu.model.Email;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Params;
import org.xzc.http.Req;
import org.xzc.vcode.PositionManager;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.UpdateBuilder;

public class Step2Worker extends AbstractWorker {

	private LinkedBlockingDeque<Book> bookList;
	private HC hc;
	private Scanner scanner;
	private RuntimeExceptionDao<Email, Integer> emailDao;
	private RuntimeExceptionDao<Book, String> bookDao;
	private BookService bs;

	public Step2Worker(String tag, ExecutorService es, PositionManager pm, LinkedBlockingDeque<Book> bookList,
			RuntimeExceptionDao<Book, String> bookDao,
			RuntimeExceptionDao<Email, Integer> emailDao, BookService bs) {
		super( tag, es, pm );
		this.bookList = bookList;
		BasicCookieStore bcs = new BasicCookieStore();
		this.hc = HCs.makeHC( 30000, 2, "202.120.17.158", 2076, false, bcs );
		//this.hc = HCs.makeHC( 30000, 2, "202.195.192.197", 3128, false, bcs );
		//this.hc = HCs.makeHC( 30000, 2, null, 2076, false, bcs );
		this.scanner = new Scanner( System.in );
		this.bookDao = bookDao;
		this.emailDao = emailDao;
		this.bs = bs;
	}

	private byte[] data;

	public byte[] getVCodeData() {
		return data;
	}

	public void process(String ptag) {
		System.out.println( SITE_NAMES[book.mode] + " 请输入验证码 " + ptag + " " + book.title + " " + from + "-" + to + " "
				+ book.maxPage );
		String yzm = scanner.nextLine();
		doAfterAsync( yzm );
	}

	private Map<String, String> map;
	private Document d;

	@Override
	protected void doAfter(String yzm) throws Exception {
		email = emailDao.queryBuilder().where().eq( "status", 0 ).and().gt( "id", book.lastEmailId ).queryForFirst();
		if (email == null) {
			throw new IllegalStateException( "没有可用的邮箱" );
		}
		map.put( "fbf.email", email.email );
		Params params = new Params();
		map.put( "fbf.verifycode", yzm );
		for (Element e : d.select( "#content input" )) {
			String name = e.attr( "name" );
			String value = map.get( name );
			if (value == null)
				value = e.val();
			params.add( name, value );
		}
		String content = hc.asString( Req.get( "http://www.cssxf.cn/booksubmit.do" ).params( params ) );
		if (content.contains( "您输入的验证码不正确" )) {
			System.out.println( "验证码错误, 重试." );
			bookList.addFirst( book );
		} else if (content.contains( "本书的咨询量已经达到上限" )) {
			System.out.println( book.title + " 本书的咨询量已经达到上限" );
			if (++book.mode <= MAX_MODE) {
				//book.lastEmailId = 0;
				bookList.addFirst( book );
			} else {
				book.status = 1;
				book.updateAt = new Date();
				bookDao.update( book );
			}
		} else if (content.contains( "您今天咨询的图书已经达到上限" )) {
			//跳过该邮箱!
			book.lastEmailId = email.id;
			System.out.println( book.title + " 您今天咨询的图书已经达到上限" );
			bookList.addFirst( book );
		} else if (content.contains( "你此次咨询的页数将会使今天的咨询量超过允许的范围" )) {
			System.out.println( "你此次咨询的页数将会使今天的咨询量超过允许的范围" );
			//这个账号不行了
			email.status = 1;
			email.updateAt = new Date();
			book.lastEmailId = email.id;
			bookList.add( book );
			UpdateBuilder<Email, Integer> ub = emailDao.updateBuilder();
			ub.where().eq( "id", email.id ).and().eq( "status", 0 );
			ub.updateColumnValue( "status", 1 );
			if (ub.update() > 0) {
				System.out.println( email.email + "已用完" );
			}
		} else if (content.contains( "咨询提交成功" )) {
			System.out.println( String.format( "%s %d-%d %d 成功", book.title, from, to, book.maxPage ) );
			if (fan) {
				book.fcurrentPage = from;
			} else {
				book.currentPage = to;
			}
			if (( !fan && book.currentPage == book.maxPage ) || ( fan && book.fcurrentPage == 1 )) {
				book.status = 2;
			} else {
				bookList.addFirst( book );
				book.status = 0;
			}
			book.updateAt = new Date();
			bookDao.update( book );
		} else {
			System.out.println( content );
			System.out.println( book );
		}
	}

	private Book book;
	private int from;
	private int to;
	private Email email;

	public String[] SITE_NAMES = { null, "深圳文献港", "独秀", "龙岩", "长春网络图书馆", "百链", "法源" };
	private static final int MAX_MODE = 4;
	public static final int INIT_MODE = MAX_MODE;

	private Req getReq(String chuandiUrl, int mode) {
		// http://www.sslibrary.com/
		// http://www.dzelib.com.cn
		// http://bj.netlib.superlib.net/
		// http://www.nbdl.gov.cn/
		// http://bl.fulink.edu.cn/
		// http://www.gzlib.org/
		// http://www.zhizhen.com/
		// http://www.dayi100.com
		// http://book.zjelib.cn
		// http://book.ucdrs.superlib.net
		// book.zzqg.superlib.net
		// /gofirstdrs.jsp?dxNumber=000015336472&d=01566FA88118FC561FE735BFCD4EA928
		switch (mode) {
		case 1:
			return Req.get( "http://book.szdnet.org.cn" + chuandiUrl );
		case 2:
			return Req.get( "http://book.duxiu.com" + chuandiUrl );
		case 3:
			return Req.get( "http://book.ly.superlib.net" + chuandiUrl );
		case 4:
			return Req.get( "http://book.ccelib.com" + chuandiUrl );
		case 5:
			return Req.get( "http://book.blyun.com" + chuandiUrl );
		//case 6:
		//	return Req.get( "http://book.lawy.cn" + chuandiUrl );
		/*case 3:
			String dxNumber = StringUtils.substringBetween( chuandiUrl, "dxNumber=", "&" );
			String d = StringUtils.substringAfter( chuandiUrl, "d=" );
			return Req.get( "http://ss.zhizhen.com/gofirst?dxid=" + dxNumber + "&d=" + d );*/
		default:
			throw new IllegalArgumentException();
		}
	}

	private boolean fan = false;

	@Override
	protected void init() throws Exception {
		data = null;
		while (pm.canContinue()) {
			book = bookList.poll( 1, TimeUnit.SECONDS );
			if (book == null)
				continue;
			break;
		}
		if (book == null)
			return;

		Req req;
		String content;
		while (true) {
			try {
				req = getReq( book.chuandiUrl, book.mode );
				content = hc.asString( req );
				if (content.contains( "验证错误" ) || book.fcurrentPage == 0) {
					System.out.println( "重新获取chuandiUrl " + book );
					Book nb = bs.getBook( hc, book.url );
					if (nb != null && nb.chuandiUrl != null) {
						book.fcurrentPage = nb.fcurrentPage;
						book.chuandiUrl = nb.chuandiUrl;
						bookDao.update( book );
					} else {
						book.status = -2;
						bookDao.update( book );
					}
				} else
					break;
			} catch (RuntimeException e) {
				//ignore
			}
		}
		if (book.chuandiUrl == null)
			return;
		Document d = Jsoup.parse( content );
		Params params = new Params();
		for (Element e : d.select( "#subtoRefer input" )) {
			params.add( e.attr( "name" ), e.val() );
		}
		req = Req.get( "http://www.cssxf.cn/book.do" ).params( params );
		content = hc.asString( req );
		this.d = Jsoup.parse( content );
		int maxPage = 50;
		if (book.currentPage > 0) {
			fan = false;
			from = book.currentPage + 1;
			to = Math.min( from + Math.min( maxPage, book.maxPage / 5 ) - 1, book.maxPage );
		} else {
			fan = true;
			to = book.fcurrentPage - 1;
			from = to - Math.min( maxPage, book.maxPage / 5 ) + 1;
			from = Math.max( from, 1 );
		}
		map = new HashMap<String, String>();
		map.put( "fbf.uppages", from + "-" + to );
		map.put( "fbf.mulutag", "1" );
		data = hc.getAsByteArray( "http://www.cssxf.cn/vImage.jsp" );
	}

}
