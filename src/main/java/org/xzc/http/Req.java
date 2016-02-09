package org.xzc.http;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

public class Req {
	private RequestBuilder rb;

	private Req(RequestBuilder rb) {
		this.rb = rb;
	}

	public static Req get(String url) {
		return new Req( RequestBuilder.get( url ) );
	}

	public static Req post(String url) {
		return new Req( RequestBuilder.post( url ) );
	}

	public Req header(String name, Object value) {
		rb.addHeader( name, value.toString() );
		return this;
	}

	public Req header(Header header) {
		rb.addHeader( header );
		return this;
	}

	public Req headers(Params p) {
		p.headersTo( rb );
		return this;
	}

	public Req headerss(Object... headers) {
		return headers( new Params( headers ) );
	}

	public Req param(String name, Object value) {
		rb.addParameter( name, value.toString() );
		return this;
	}

	public Req param(NameValuePair nvp) {
		rb.addParameter( nvp );
		return this;
	}

	public Req params(Params p) {
		p.paramsTo( rb );
		return this;
	}

	public Req params(Object... args) {
		new Params( args ).paramsTo( rb );
		return this;
	}

	public Req datas(Params p) {
		p.datasTo( rb );
		return this;
	}

	public Req datas(Object... args) {
		new Params( args ).datasTo( rb );
		return this;
	}

	public RequestBuilder getBuilder() {
		return rb;
	}

	public Req host(String host) {
		rb.addHeader( "Host", host );
		return this;
	}

	public HttpUriRequest build() {
		return rb.build();
	}

	public Req cookie(String cookie) {
		return header( "Cookie", cookie );
	}
}
