package Zeze.Onz;

import Zeze.Transaction.Bean;

public class OnzSaga extends OnzProcedure {
	public OnzSaga(long onzTid, OnzSagaStub<?, ?> stub, Bean argument, Bean result) {
		super(onzTid, stub, argument, result);
	}

	@Override
	public long call() throws Exception {
		// saga模式不需要侵入zeze事务。
		return getStub().call(this, getArgument(), getResult());
	}

	@Override
	public void sendReadyAndWait() {
		// sage 模式，执行阶段不需要任何等待。
		// 由于上面的call已经去掉了侵入，这里不重载也没什么问题。重载一下更明确。
		// 关键还要看下面的todo
		// todo saga需要共享flush阶段的2段式提交吗？
	}

	@Override
	protected void sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
		// 由于上面的call已经去掉了侵入，这里不重载也没什么问题。重载一下更明确。
	}

	@Override
	protected void flushWait() {
		// 由于上面的call已经去掉了侵入，这里不重载也没什么问题。重载一下更明确。
	}
}
