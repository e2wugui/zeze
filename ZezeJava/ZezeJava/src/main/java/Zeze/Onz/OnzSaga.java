package Zeze.Onz;

import Zeze.Transaction.Bean;

public class OnzSaga extends OnzProcedure {
	public OnzSaga(long onzTid, OnzSagaStub<?, ?> stub, Bean argument, Bean result) {
		super(onzTid, stub, argument, result);
	}

	@Override
	public void sendReadyAndWait() {
		// sage 模式，执行阶段不需要任何等待。
		// 但是由于共享flush阶段的2段式提交，所以不能override call方法去除侵入。
	}
}
