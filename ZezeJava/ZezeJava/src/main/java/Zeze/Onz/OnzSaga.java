package Zeze.Onz;

import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Bean;

public class OnzSaga extends OnzProcedure {
	private boolean end = false;
	private final long startTime = System.currentTimeMillis();

	public OnzSaga(AsyncSocket onzServer,
				   BFuncProcedure.Data funcArgument,
				   OnzSagaStub<?, ?, ?> stub, Bean argument, Bean result) {
		super(onzServer, funcArgument, stub, argument, result);
	}

	public long getStartTime() {
		return startTime;
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
