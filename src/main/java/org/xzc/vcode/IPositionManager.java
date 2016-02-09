package org.xzc.vcode;

public interface IPositionManager {
	public void bind(IWorker worker);

	public void loop() throws Exception;
}
