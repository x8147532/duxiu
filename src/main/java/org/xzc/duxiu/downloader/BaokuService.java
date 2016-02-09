package org.xzc.duxiu.downloader;

import org.apache.http.HttpHost;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.xzc.duxiu.model.BKBook;
import org.xzc.http.HC;

public class BaokuService {
	private HC hc;

	public BaokuService(HC hc) {
		this.hc = hc;
	}

	public String getBaokuUrl(BKBook bb) {
		String content = hc.getAsString( bb.url );
		for (Element e : Jsoup.parse( content, bb.url ).select( ".btn a" )) {
			if (e.text().equals( "包库全文阅读" )) {
				return e.absUrl( "href" );
			}
		}
		return null;
	}

	public boolean isInvalid(byte[] data) {
		if (data[0] != (byte)0x89 || data[1] != (byte)0x50 || data[2] != (byte)0x4e || data[3] != (byte)0x47)
			return true;
		return isInvalid2( data );
	}

	public boolean isInvalid2(byte[] data) {
		if (data.length == 667 || data.length == 687 || data.length == 3705 || data.length == 3798
				|| data.length == 17663)
			return true;
		try {
			return new String( data, "utf-8" ).contains( "500 Internal Server Error" );
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isException(byte[] data) {
		try {
			if (data.length < 40 * 1024) {
				String vc = new String( data, "utf-8" );
				return vc.contains( "我们检测到您的操作可能有异常" );
			}
		} catch (Exception e) {
		}
		return false;
	}

}
