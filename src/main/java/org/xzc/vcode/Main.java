package org.xzc.vcode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
	public static void main(String[] args) throws Exception {
		int batch = 4;
		PositionManager pm = new PositionManager();
		pm.setBatch( batch );
		List<Worker> workerList = new ArrayList<Worker>();
		ExecutorService es = Executors.newFixedThreadPool( batch );
		for (int i = 0; i < batch * 2; ++i) {
			//添加工作者
			Worker worker = new Worker( "worker" + i, es, pm );
			worker.initAsync();
			workerList.add( worker );
		}
		pm.loop();
		es.shutdown();
		es.awaitTermination( 1, TimeUnit.HOURS );
	}
}
