package org.xzc.http;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 对hc的简单的封装
 * @author xzchaoo
 *
 */
public class HC {
	private CloseableHttpClient chc;

	public CloseableHttpClient getCHC() {
		return chc;
	}

	private HttpHost proxy;

	public HttpHost getProxy() {
		return proxy;
	}

	public HC(CloseableHttpClient chc, HttpHost proxy) {
		this.chc = chc;
		this.proxy = proxy;
	}

	public String getAsString(String url) {
		return asString( RequestBuilder.get( url ).build() );
	}

	public JSONObject getAsJSON(String url) {
		return asJSON( RequestBuilder.get( url ).build() );
	}

	public JSONObject asJSON(HttpUriRequest req) {
		return JSON.parseObject( asString( req ) );
	}

	public String asString(Req req) {
		return asString( req.build() );
	}

	public JSONObject asJSON(Req req) {
		return asJSON( req.build() );
	}

	public String asString(final HttpUriRequest req) {
		return asString( req, "utf-8" );
	}

	private static <T> T safeRun(SafeRunner<T> sr) {
		try {
			return sr.run();
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException( e );
		}
	}

	public void close() {
		HttpClientUtils.closeQuietly( chc );
	}

	public String getAsString(String url, String encoding) {
		return asString( RequestBuilder.get( url ).build(), encoding );
	}

	public String asString(final HttpUriRequest req, final String encoding) {
		return safeRun( new SafeRunner<String>() {
			public String run() throws Exception {
				CloseableHttpResponse res = null;
				try {
					res = chc.execute( req );
					String content = EntityUtils.toString( res.getEntity(), encoding );
					return content;
				} finally {
					HttpClientUtils.closeQuietly( res );
				}
			}
		} );
	}

	public byte[] getAsByteArray(String url) {
		return asByteArray( RequestBuilder.get( url ).build() );
	}

	public byte[] asByteArray(final HttpUriRequest req) {
		return safeRun( new SafeRunner<byte[]>() {
			public byte[] run() throws Exception {
				CloseableHttpResponse res = null;
				try {
					res = chc.execute( req );
					return EntityUtils.toByteArray( res.getEntity() );
				} finally {
					HttpClientUtils.closeQuietly( res );
				}
			}
		} );
	}

	public void consume(final HttpUriRequest req) {
		safeRun( new SafeRunner<Void>() {
			public Void run() throws Exception {
				CloseableHttpResponse res = null;
				try {
					res = chc.execute( req );
					EntityUtils.consumeQuietly( res.getEntity() );
				} finally {
					HttpClientUtils.closeQuietly( res );
				}
				return null;
			}
		} );
	}

	public void consume(Req req) {
		consume( req.build() );
	}

	public byte[] asByteArray(Req req) {
		return asByteArray( req.build() );
	}

	public CloseableHttpResponse asRes(final HttpUriRequest req) {
		return safeRun( new SafeRunner<CloseableHttpResponse>() {
			public CloseableHttpResponse run() throws Exception {
				return chc.execute( req );
			}
		} );
	}

	public CloseableHttpResponse asRes(Req req) {
		return asRes( req.build() );
	}

	public String asString(Req req, String encoding) {
		return asString( req.build(), encoding );
	}

}
