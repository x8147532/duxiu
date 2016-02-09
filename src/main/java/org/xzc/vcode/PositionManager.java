package org.xzc.vcode;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class PositionManager implements IPositionManager {
	private static final Logger log = Logger.getLogger( PositionManager.class );
	private int batch = 1;
	private LinkedBlockingQueue<IPosition> unbindedPositionList = new LinkedBlockingQueue<IPosition>();
	private LinkedBlockingQueue<IPosition> bindedPositionList = new LinkedBlockingQueue<IPosition>();

	public PositionManager() {
	}

	public synchronized void bind(IWorker worker) {
		try {
			while (canContinue()) {
				IPosition p = unbindedPositionList.poll( 1, TimeUnit.SECONDS );
				if (p == null)
					continue;
				p.bindWorker( worker );
				bindedPositionList.put( p );
				break;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	public int getBatch() {
		return batch;
	}

	public void init() {
		for (int i = 0; i < batch; ++i)
			unbindedPositionList.add( new Position( "p" + i, new File( "vcode_" + i + ".png" ), save ) );
	}

	public void loop() throws Exception {
		loop( -1 );
	}

	public void setBatch(int batch) {
		this.batch = batch;
	}

	public boolean canContinue() {
		return true;
	}

	public void loop(int maxLoop) throws Exception {
		boolean hasCount = maxLoop > 0;
		while (maxLoop != 0 && canContinue()) {
			if (hasCount)
				--maxLoop;
			IPosition p = bindedPositionList.poll( 1, TimeUnit.SECONDS );//拿到一个已经绑定的p
			if (p == null) {
				if (hasCount)
					++maxLoop;
				continue;
			}
			try {
				p.process();//处理
			} finally {
				p.unbindWorker();
				unbindedPositionList.put( p );//将p解绑
			}
		}
	}

	public void close() {
		unbindedPositionList.clear();
		unbindedPositionList = null;
		for (IPosition p : bindedPositionList)
			p.unbindWorker();
		bindedPositionList.clear();
		bindedPositionList = null;
	}

	private boolean save;

	public void setSave(boolean save) {
		this.save = save;
	}

}
