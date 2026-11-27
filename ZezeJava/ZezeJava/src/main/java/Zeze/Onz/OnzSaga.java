package Zeze.Onz;

import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;

/** saga参与方执行上下文：业务与FuncSagaEnd补偿/结束经businessLock互斥，本地提交后发送结果。 */
public class OnzSaga extends OnzProcedure {
	private volatile boolean end = false; // setEnd在协议线程，isEnd在Checkpoint flush线程
	// TTL 计时基准：构造时初始化，仅在 FuncSagaEnd 补偿失败放回时刷新，防止补偿重试链期间上下文过期。
	private volatile long lastActiveTime;
	// 业务执行与 FuncSagaEnd 互斥，保证补偿严格串行在业务完成之后。
	private final ReentrantLock businessLock = new ReentrantLock();

	public OnzSaga(Rpc<?, ?> rpc,
				   BFuncProcedure.Data funcArgument,
				   OnzSagaStub<?, ?, ?> stub, Bean argument, Bean result) {
		super(rpc, funcArgument, stub, argument, result);
		lastActiveTime = System.currentTimeMillis();
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	/** FuncSagaEnd补偿失败放回sagas时刷新（唯一的最后活动时间刷新点）。 */
	void refreshLastActive() {
		lastActiveTime = System.currentTimeMillis();
	}

	final void lockBusiness() {
		businessLock.lock();
	}

	/** 业务在途（执行中或FuncSagaEnd补偿中）时拿不到锁：cleanupTimeoutSagas据此跳过本轮。 */
	final boolean tryLockBusiness() {
		return businessLock.tryLock();
	}

	/**
	 * 有界等待业务锁，供 FuncSagaEnd 使用：避免挂死业务长期占用派发线程，超时后放弃本次处理交由协调者 redo 重发。
	 */
	final boolean tryLockBusiness(long timeoutMs) throws InterruptedException {
		return businessLock.tryLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	final void unlockBusiness() {
		businessLock.unlock();
	}

	@Override
	public void sendReadyAndWait() {
		var req = (FuncSaga)getRpc();
		var bbResult = ByteBuffer.Allocate();
		//noinspection DataFlowIssue
		getResult().encode(bbResult);
		//noinspection DataFlowIssue
		req.Result.setFuncResult(new Binary(bbResult));
		Transaction.whileCommit(req::SendResult);

		// saga模式，执行阶段不需要任何等待。
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
