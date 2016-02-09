package org.xzc.duxiu.b0119;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.xzc.vcode.IWorker;
import org.xzc.vcode.PositionManager;

public abstract class AbstractWorker implements IWorker {
	protected final String tag;
	protected final ExecutorService es;
	protected final PositionManager pm;

	public AbstractWorker(String tag, ExecutorService es, PositionManager pm) {
		this.tag = tag;
		this.es = es;
		this.pm = pm;
	}

	protected boolean needInit;

	public final void doAfterAsync(final String yzm) {
		es.submit( new Callable<Void>() {
			public Void call() throws Exception {
				try {
					needInit = true;
					doAfter( yzm );
				} catch (Exception e) {
					needInit = true;
					e.printStackTrace();
				} finally {
					if (needInit)
						initAsync();
				}
				return null;
			}
		} );
	}

	public final String getTag() {
		return tag;
	}

	public final void initAsync() {
		es.submit( new Callable<Void>() {
			public Void call() throws Exception {
				while (pm.canContinue()) {
					try {
						init();
						pm.bind( AbstractWorker.this );
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		} );
	}

	public void onReject() {
	}

	protected abstract void doAfter(String yzm) throws Exception;

	protected abstract void init() throws Exception;

	protected State state = State.RUNNING;

	public final State getState() {
		return state;
	}

}
