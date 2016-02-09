package org.xzc.duxiu.b0119;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xzc.duxiu.model.Book;
import org.xzc.http.HC;
import org.xzc.http.Req;

public class BookService {
	public Book getBook(HC hc, String url) {
		String dxNumber = StringUtils.substringBetween( url, "dxNumber=", "&" );
		String dd = StringUtils.substringBetween( url, "d=", "&" );
		Book b = new Book();
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
				maxPage = Integer.parseInt( StringUtils.substringAfter( text, "【页 数】" ).trim() );
				break;
			}
		}
		for (Element e : d.select( ".do .btn a" )) {
			String text = e.text();
			if (text.contains( "图书馆文献传递" )) {
				chuandiUrl = StringUtils.substringBetween( e.attr( "href" ), "'", "'" );
				break;
			}
		}
		b.title = title;
		b.maxPage = maxPage;
		b.fcurrentPage = b.maxPage + 1;
		b.url = url;
		b.updateAt = new Date();
		if (chuandiUrl != null) {
			b.chuandiUrl = chuandiUrl;
			b.status = 0;
		} else {
			b.status = -1;
		}
		return b;
	}
}
