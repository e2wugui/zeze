package Zeze.Onz;

import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class OnzSaga extends OnzProcedure {
	private boolean end = false;
	private final long startTime = System.currentTimeMillis();

	public OnzSaga(Rpc<?, ?> rpc,
				   BFuncProcedure.Data funcArgument,
				   OnzSagaStub<?, ?, ?> stub, Bean argument, Bean result) {
		super(rpc, funcArgument, stub, argument, result);
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public void sendReadyAndWait() {
		// 发送rpc结果
		var req = (FuncSaga)getRpc();
		var bbResult = ByteBuffer.Allocate();
		getResult().encode(bbResult);
		req.Result.setFuncResult(new Binary(bbResult));
		req.SendResult();

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
