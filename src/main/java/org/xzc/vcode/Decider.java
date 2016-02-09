package org.xzc.vcode;

public interface Decider {
	enum Result {
		ACCEPT, REJECT, STOP
	};

	Result accept(IPosition p);

	Decider ACCEPT = new Decider() {
		public Result accept(IPosition p) {
			return Result.ACCEPT;
		}
	};
}
