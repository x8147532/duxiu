package org.xzc.duxiu.model;

import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "email")
public class Email {
	@DatabaseField(generatedId = true)
	public int id;

	@DatabaseField
	public String email;

	@DatabaseField(dataType = DataType.DATE_STRING)
	public Date updateAt;

	@DatabaseField
	public int status;

}
