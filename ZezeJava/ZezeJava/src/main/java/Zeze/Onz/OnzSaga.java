package Zeze.Onz;

import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

/** saga参与方执行上下文：业务与FuncSagaEnd补偿/结束经businessLock互斥，发送结果即本地提交。 */
public class OnzSaga extends OnzProcedure {
	private volatile boolean end = false; // setEnd在协议线程，isEnd在Checkpoint flush线程
	// TTL计时基准=最后活动时间：构造时刻初始化，FuncSagaEnd补偿失败放回
	// sagas时刷新。不变式：补偿重试链推进期间（放回→redo重发→再放回），上下文不因
	// 构造时刻的TTL到期被清（完整因果链见Onz.sagaContextTimeoutMs契约）。业务完成
	// 不刷新活动时间：超长业务完成后滞留超TTL仍会被清（收窄非闭合，超龄NotFound由
	// OnzServer.redo分诊error）。
	private volatile long lastActiveTime;
	// FuncSaga的业务在任务池异步执行，FuncSagaEnd(cancel/end)可能在业务仍在
	// 执行时到达（协调者超时补偿就是冲着慢步骤去的）。业务与cancel/end互斥：补偿必须
	// 串行在业务完成之后——业务随后失败回滚时抢先执行的补偿就是过补偿（反向分歧）。
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

	final void unlockBusiness() {
		businessLock.unlock();
	}

	@Override
	public void sendReadyAndWait() {
		var req = (FuncSaga)getRpc();
		var bbResult = ByteBuffer.Allocate();
		getResult().encode(bbResult);
		req.Result.setFuncResult(new Binary(bbResult));
		req.SendResult();

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
