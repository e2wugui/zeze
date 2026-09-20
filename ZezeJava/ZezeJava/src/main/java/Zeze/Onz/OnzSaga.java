package Zeze.Onz;

import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class OnzSaga extends OnzProcedure {
	private volatile boolean end = false; // setEnd在协议线程，isEnd在Checkpoint flush线程
	private final long startTime = System.currentTimeMillis();
	// FND7-34：FuncSaga的业务在任务池异步执行，FuncSagaEnd(cancel/end)可能在业务仍在
	// 执行时到达（协调者超时补偿就是冲着慢步骤去的）。业务与cancel/end互斥：补偿必须
	// 串行在业务完成之后——业务随后失败回滚时抢先执行的补偿就是过补偿（反向分歧）。
	private final ReentrantLock businessLock = new ReentrantLock();

	public OnzSaga(Rpc<?, ?> rpc,
				   BFuncProcedure.Data funcArgument,
				   OnzSagaStub<?, ?, ?> stub, Bean argument, Bean result) {
		super(rpc, funcArgument, stub, argument, result);
	}

	public long getStartTime() {
		return startTime;
	}

	final void lockBusiness() {
		businessLock.lock();
	}

	/** 业务在途（执行中或FuncSagaEnd补偿中）时拿不到锁：cleanupTimeoutSagas据此跳过本轮（OH1-F3）。 */
	final boolean tryLockBusiness() {
		return businessLock.tryLock();
	}

	final void unlockBusiness() {
		businessLock.unlock();
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
