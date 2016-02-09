package org.xzc.duxiu.model;

import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "zxurl")
public class ZxUrl {
	@DatabaseField(id = true)
	public String zxUrl;
	@DatabaseField
	public String pdfUrl;
	@DatabaseField
	public String dxNumber;
	@DatabaseField
	public String title;
	@DatabaseField
	public String pages;
	@DatabaseField
	public int status;
	@DatabaseField(dataType = DataType.DATE_STRING)
	public Date updateAt;
	
	@Override
	public String toString() {
		return "ZxUrl [zxUrl=" + zxUrl + ", pdfUrl=" + pdfUrl + "]";
	}
	
}
