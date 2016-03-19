package org.xzc.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.xzc.http.HC;
import org.xzc.http.HCs;
import org.xzc.http.Req;

public class TestSJTU {
	@Test
	public void test111() {
		HC hc = HCs.makeHC();
		String cookie = "ASP.NET_SessionId=qaei0j344l2ax3nki0cqv455;";
		for (int bsid = 370000; bsid <= 371000; ++bsid) {
			String content = hc.asString(
					Req.get( "http://electsys.sjtu.edu.cn/edu/lesson/viewLessonArrangeDetail2.aspx?bsid=" + bsid )
							.cookie( cookie ) );
			Document doc = Jsoup.parse( content );
			Elements tds = doc.select( "#LessonArrangeDetail1_dataListKc table td" );
			if (tds.size() > 2) {
				String kh = tds.get( 2 ).text();
				if (kh.contains( "SE" ) && kh.contains( "2015-2016-2" )) {
					System.out.println( tds.get( 1 ).text() + " " + bsid );
				}
			}
		}
	}
}
