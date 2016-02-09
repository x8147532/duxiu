package org.xzc.duxiu.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "bkbook")
public class BKBook {
	@DatabaseField(id = true)
	public String dxid;
	
	@DatabaseField
	public String title;
	
	@DatabaseField
	public String url;
	
	@DatabaseField
	public int status;

	@Override
	public String toString() {
		return "BKBook [dxid=" + dxid + ", title=" + title + ", url=" + url + ", status=" + status + "]";
	}
	
}
