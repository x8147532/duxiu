package org.xzc.duxiu.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page {
	private static final Pattern pattern = Pattern.compile( "\\[(\\d+)\\s*,\\s*(\\d+)\\]" );
	public int[][] pages = new int[8][2];

	public boolean isHeader() {
		return pages[5][1] == 0;
	}

	public String getTitle() {
		if (isHeader())
			return "0header";
		return pages[5][0] + "-" + pages[5][1];
	}

	public static Page parse(String pages) {
		Page p = new Page();
		Matcher m = pattern.matcher( pages );
		int index = 0;
		while (m.find()) {
			String g1 = m.group( 1 );
			String g2 = m.group( 2 );
			p.pages[index][0] = Integer.parseInt( g1 );
			p.pages[index][1] = Integer.parseInt( g2 );
			++index;
		}
		return p;
	}
}
