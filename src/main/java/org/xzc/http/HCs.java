package org.xzc.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

public class HCs {
	public static final int DEFAULT_TIMEOUT = 30000;
	public static final boolean DEFAULT_IGNORE_COOKIE = true;
	private static final int DEFAULT_BATCH = 2;

	public static HC makeHC() {
		return makeHC( DEFAULT_IGNORE_COOKIE );
	}

	public static HC makeHC(BasicCookieStore bcs) {
		return makeHC( DEFAULT_TIMEOUT, DEFAULT_BATCH, null, 0, false, bcs );
	}

	public static HC makeHC(boolean ignoreCookie) {
		return makeHC( DEFAULT_TIMEOUT, null, 0, ignoreCookie );
	}

	public static HC makeHC(int timeout) {
		return makeHC( timeout, DEFAULT_BATCH, null, 0, DEFAULT_IGNORE_COOKIE, null );
	}

	public static HC makeHC(int timeout, int batch, HttpHost proxy, boolean ignoreCookie, BasicCookieStore bcs) {
		return makeHC( timeout, batch, proxy, null, null, ignoreCookie, bcs );
	}

	public static HC makeHC(int timeout, int batch, String host, int port, boolean ignoreCookie) {
		return makeHC( timeout, batch, host, port, null, null, ignoreCookie, null );
	}

	public static HC makeHC(int timeout, int batch, String host, int port, boolean ignoreCookie, BasicCookieStore bcs) {
		HttpHost proxy = host == null ? null : new HttpHost( host, port );
		return makeHC( timeout, batch, proxy, ignoreCookie, bcs );
	}

	public static HC makeHC(int timeout, String host, int port, boolean ignoreCookie) {
		return makeHC( timeout, DEFAULT_BATCH, host, port, ignoreCookie, null );
	}

	public static HC makeHC(String host, int port, boolean ignoreCookie) {
		return makeHC( DEFAULT_TIMEOUT, host, port, ignoreCookie );
	}

	public static HC makeHC(String host, int port, String proxyUsername, String proxyPassword, boolean ignoreCookie) {
		return makeHC( DEFAULT_TIMEOUT, DEFAULT_BATCH, host, port, proxyUsername, proxyPassword, ignoreCookie, null );
	}

	public static HC makeHC(int timeout, int batch, HttpHost proxy, String proxyUsername, String proxyPassword,
			boolean ignoreCookie, BasicCookieStore bcs) {
		Builder b = RequestConfig.custom()
				.setConnectionRequestTimeout( timeout )
				.setConnectTimeout( timeout )
				.setSocketTimeout( timeout );
		if (ignoreCookie) {
			b.setCookieSpec( CookieSpecs.IGNORE_COOKIES );
		} else {
			b.setCookieSpec( CookieSpecs.NETSCAPE );
		}
		RequestConfig rc = b.build();
		PoolingHttpClientConnectionManager m = new PoolingHttpClientConnectionManager();
		m.setMaxTotal( batch * 2 );
		m.setDefaultMaxPerRoute( batch );
		BasicCredentialsProvider bcp = new BasicCredentialsProvider();
		if (proxy != null && proxyUsername != null && proxyPassword != null) {
			bcp.setCredentials( new AuthScope( proxy.getHostName(), proxy.getPort() ),
					new UsernamePasswordCredentials( proxyUsername, proxyPassword ) );
		}
		CloseableHttpClient chc = HttpClients.custom()
				.setDefaultCredentialsProvider( bcp )
				.setProxy( proxy )
				.addInterceptorFirst( new HttpRequestInterceptor() {
					public void process(HttpRequest req, HttpContext context) throws HttpException, IOException {
						if (req.getFirstHeader( "User-Agent" ) == null) {
							req.addHeader( "User-Agent",
									"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.87 Safari/537.36 QQBrowser/9.2.5584.400" );
						}
						req.addHeader( "X-Client-IP", "1.2.4.8" );
						req.addHeader( "X-Forwarded-For", "1.2.4.8" );
						req.addHeader( "X-Real-IP", "1.2.4.8" );
					}
				} )
				.setConnectionManager( m )
				.setDefaultRequestConfig( rc )
				.setDefaultCookieStore( bcs )
				.build();
		return new HC( chc, proxy );
	}

	public static HC makeHC(int timeout, int batch, String host, int port, String proxyUsername, String proxyPassword,
			boolean ignoreCookie, BasicCookieStore bcs) {
		if (host == null) {
			return makeHC( timeout, batch, null, ignoreCookie, bcs );
		} else {
			return makeHC( timeout, batch, new HttpHost( host, port ), proxyUsername, proxyPassword, ignoreCookie,
					bcs );
		}
	}

}
