package org.xzc.vcode;

public interface IPosition {
	public void bindWorker(IWorker worker);//绑定工作者

	public String getTag();//tag

	public void process();//处理

	public void reject();//放弃该worker

	void unbindWorker();//解绑
}
