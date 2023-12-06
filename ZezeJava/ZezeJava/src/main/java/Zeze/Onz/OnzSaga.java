package Zeze.Onz;

import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Bean;

public class OnzSaga extends OnzProcedure {
	private boolean end = false;

	public OnzSaga(AsyncSocket onzServer,
				   long onzTid, int flushMode,
				   OnzSagaStub<?, ?> stub, Bean argument, Bean result) {
		super(onzServer, onzTid, flushMode, stub, argument, result);
	}

	@Override
	public void sendReadyAndWait() {
		// sage 模式，执行阶段不需要任何等待。
	}

	// saga需要共享flush阶段的2段式提交

	@Override
	public boolean isEnd() {
		return end;
	}

	public void setEnd() {
		end = true;
	}
}
