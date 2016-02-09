package org.xzc.http;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;

public class Params {

	public Params() {
	}

	public Params(String name, Object value) {
		add( name, value );
	}

	public Params(Object... args) {
		for (int i = 0; i < args.length; i += 2) {
			String name = args[i].toString();
			String value = args[i + 1].toString();
			add( name, value );
		}
	}

	private List<NameValuePair> params = new ArrayList<NameValuePair>();

	public Params add(String name, Object value) {
		params.add( new BasicNameValuePair( name, value.toString() ) );
		return this;
	}

	public boolean isEmpty() {
		return params.isEmpty();
	}

	private String encoding = "utf-8";

	public Params encoding(String encoding) {
		this.encoding = encoding;
		return this;
	}

	public UrlEncodedFormEntity toEntity() {
		UrlEncodedFormEntity e = null;
		try {
			e = new UrlEncodedFormEntity( params, encoding );
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return e;
	}

	public Params paramsTo(RequestBuilder rb) {
		for (NameValuePair nvp : params)
			rb.addParameter( nvp );
		return this;
	}

	public Params datasTo(RequestBuilder rb) {
		rb.setEntity( toEntity() );
		return this;
	}

	public Params headersTo(RequestBuilder rb) {
		for (NameValuePair nvp : params) {
			rb.addHeader( nvp.getName(), nvp.getValue() );
		}
		return this;
	}

	public List<NameValuePair> getParamList() {
		return params;
	}
}
