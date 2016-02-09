package org.xzc.vcode;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class Worker implements IWorker {
	private static final Logger log = Logger.getLogger( Worker.class );
	private static final Random r = new Random();
	private String tag;
	private ExecutorService es;
	private IPositionManager pm;

	private Callback cb = new Callback() {
		public void callback(Object result) throws Exception {
			System.out.println( code + ( code.equals( result ) ? "==" : "!=" ) + result );
		}
	};

	private String code;

	public Worker(String tag, ExecutorService es, IPositionManager pm) {
		this.tag = tag;
		this.es = es;
		this.pm = pm;
	}

	/**
	 * 这个方法是异步的
	 */
	public void doAfterAsync(final String yzm) {
		es.submit( new Callable<Void>() {
			public Void call() throws Exception {
				System.out.println( "提交表单!" );
				Thread.sleep( r.nextInt( 1000 ) + 500 );
				cb.callback( yzm );
				initAsync();
				return null;
			}
		} );
	}

	public String getTag() {
		return tag;
	}

	public byte[] getVCodeData() {
		return StringUtils.reverse( code ).getBytes( Charset.forName( "utf-8" ));
	}

	public void initAsync() {
		es.submit( new Callable<Void>() {
			public Void call() throws Exception {
				code = RandomStringUtils.random( 4, true, false ).toLowerCase();
				log.info( "为 " + tag + " 产生了验证码" + code );
				//初始化完成之后就去绑定
				pm.bind( Worker.this );
				return null;
			}
		} );
	}

	public void onReject() {
	}

	public void process(String ptag) {
	}

	public State getState() {
		return State.RUNNING;
	}

}
