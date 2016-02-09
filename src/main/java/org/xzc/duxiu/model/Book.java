package org.xzc.duxiu.model;

import java.util.Date;

import org.xzc.duxiu.b0119.Step2Worker;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "book")
public class Book {
	@DatabaseField(id = true)
	public String dxNumber;
	@DatabaseField
	public String d;
	@DatabaseField
	public String title;
	@DatabaseField
	public String normalTitle;
	@DatabaseField
	public int maxPage;
	@DatabaseField
	public int currentPage;
	@DatabaseField
	public int fcurrentPage;
	@DatabaseField
	public String chuandiUrl;
	@DatabaseField
	public String url;
	@DatabaseField(dataType = DataType.DATE_STRING)
	public Date updateAt;
	@DatabaseField
	public int status;
	public boolean baoku;

	@Override
	public String toString() {
		return "Book [chuandiUrl=" + chuandiUrl + ", url=" + url + "]";
	}

	public int lastEmailId = 0;
	public int mode = Step2Worker.INIT_MODE;
}
