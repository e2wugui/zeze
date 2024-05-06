package Zeze.Onz;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Builtin.Onz.Rollback;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Transaction.Data;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class OnzTransaction<A extends Data, R extends Data> extends ReentrantLock {
	protected static final Logger logger = LogManager.getLogger();

	private OnzServer onzServer;
	private int flushMode = Onz.eFlushImmediately;
	private int flushTimeout = 10_000;
	private A argument;
	private R result;
	private boolean pendingAsync = false;
	private final Condition thisCond = newCondition();

	void waitPendingAsync() throws InterruptedException {
		lock();
		try {
			while (pendingAsync) {
				thisCond.await();
			}
		} finally {
			unlock();
		}
	}

	public void setPendingAsync(boolean pending) {
		lock();
		try {
			this.pendingAsync = pending;
			this.notify();
		} finally {
			unlock();
		}
	}

	protected abstract long perform() throws Exception;

	public void setOnzServer(OnzServer onzServer) {
		this.onzServer = onzServer;
		this.onzTid = onzServer.nextOnzTid();
	}

	public int getFlushMode() {
		return flushMode;
	}

	public int getFlushTimeout() {
		return flushTimeout;
	}

	public A getArgument() {
		return argument;
	}

	public R getResult() {
		return result;
	}

	public void setFlushMode(int flushMode) {
		this.flushMode = flushMode;
	}

	public void setFlushTimeout(int flushTimeout) {
		this.flushTimeout = flushTimeout;
	}

	public void setArgument(A argument) {
		this.argument = argument;
	}

	public void setResult(R result) {
		this.result = result;
	}

	// 远程调用辅助函数
	public <A2 extends Data, R2 extends Data> TaskCompletionSource<R2>
	callProcedureAsync(String zezeName, String onzProcedureName, A2 argument, R2 result) {
		// procedure sage 互斥。
		if (!zezeSagas.isEmpty())
			throw new RuntimeException("can not mix funcProcedure and funcSaga. saga has called.");
		var zezeInstance = onzServer.getZezeInstance(zezeName);
		// 限制每个zeze集群最多一个调用.
		var newCall = new OutObject<TaskCompletionSource<R2>>();
		zezeProcedures.computeIfAbsent(zezeInstance, __ -> newCall.value
				= OnzAgent.callProcedureAsync(
				this, zezeInstance, onzProcedureName, argument, result, flushMode));
		if (newCall.value == null)
			throw new RuntimeException("too many funcProcedure on same zezeInstance.");
		return newCall.value;
	}

	public <A2 extends Data, R2 extends Data> TaskCompletionSource<R2>
	callSagaAsync(String zezeName, String onzProcedureName, A2 argument, R2 result) {
		// procedure sage 互斥。
		if (!zezeProcedures.isEmpty())
			throw new RuntimeException("can not mix funcProcedure and funcSaga. procedure has called.");
		var zezeInstance = onzServer.getZezeInstance(zezeName);
		// 限制每个zeze集群最多一个调用.
		var newCall = new OutObject<TaskCompletionSource<R2>>();
		zezeSagas.computeIfAbsent(zezeInstance, __ -> newCall.value
				= OnzAgent.callSagaAsync(
				this, zezeInstance, onzProcedureName, argument, result, flushMode));
		if (newCall.value == null)
			throw new RuntimeException("too many funcSaga on same zezeInstance.");
		return newCall.value;
	}

	private void endSaga() throws ExecutionException, InterruptedException {
		// 执行过程中发生异常或者错误不能到达这里，而是rollback里面的cancelSaga。
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : zezeSagas.entrySet()) {
			var r = new FuncSagaEnd();
			r.Argument.setCancel(false);
			futures.add(r.SendForWait(e.getKey()));
		}
		for (var future : futures)
			future.await();
	}

	private void cancelSaga() {
		// 等待已经发出的saga的结果（包括失败的），
		// 因为saga可能异步发送，并且中途发生了错误，
		// 此时需要继续把没得到的结果等到。
		// 然后根据结果决定怎么发送FuncSagaEnd
		for (var saga : zezeSagas.values()) {
			try {
				saga.get();
			} catch (Exception e) {
				logger.error("await saga result.", e);
			}
		}
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : zezeSagas.entrySet()) {
			try {
				if (e.getValue().get() != null) { // 成功的发送cancel。
					var r = new FuncSagaEnd();
					r.Argument.setCancel(true);
					futures.add(r.SendForWait(e.getKey()));
				}
			} catch (Exception ex) {
				logger.error("cancel if saga success.", ex);
			}
		}
		for (var future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				logger.error("await cancel result.", e);
			}
		}
	}

	public BSavedCommits.Data buildSavedCommits() {
		var bState = new BSavedCommits.Data();
		for (var e : zezeProcedures.keySet()) {
			bState.getOnzs().add(Objects.requireNonNull(e.getConnector()).getName());
		}
		return bState;
	}

	void commit(byte[] tidBytes, BSavedCommits.Data state) throws ExecutionException, InterruptedException {
		// 对于saga，zezeProcedures都是空的。
		// 持久化，并且在异常情况下，重发Commit。
		//  可以解决commit阶段网络异常导致zeze服务器没有收到commit，
		//  可以解决commit阶段OnzAgent宕机导致commit丢失，
		//  但是无法解决所有问题，比如：后面的flush阶段的完整性是不完备的，存在降级（FlushAsync），只是一种尽量的策略。
		//  无法解决zeze服commit后flush前的zeze服宕机问题。实现起来麻烦，而且成效不够显著。
		//  本质的核心问题是Onz的zeze端没有持久化ready，导致即使补发commit也无法非常可靠。

		try {
			onzServer.saveCommitPoint(tidBytes, state, AbstractOnz.eCommitting);
		} catch (Throwable ex) {
			rollback();
			onzServer.removeCommitIndex(tidBytes);
			throw new RuntimeException(ex);
		}

		for (var zeze : zezeProcedures.keySet()) {
			var r = new Commit();
			r.Argument.setOnzTid(onzTid);
			r.SendForWait(zeze).await();
			if (r.getResultCode() != 0)
				logger.fatal("commit error {}", IModule.getErrorCode(r.getResultCode()));
		}

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		endSaga();

		// commit success
		onzServer.removeCommitIndex(tidBytes);
	}

	void rollback() {
		// 对于saga，是空的。
		for (var zeze : zezeProcedures.keySet()) {
			var r = new Rollback();
			r.Argument.setOnzTid(onzTid);
			r.SendForWait(zeze).await();
			if (r.getResultCode() != 0) {
				logger.fatal("rollback error {}", IModule.getErrorCode(r.getResultCode()));
			}
		}

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		cancelSaga();
	}

	void waitFlushDone() {
		if (flushMode == Onz.eFlushImmediately) {
			try {
				flushDone.get(flushTimeout, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				logger.warn("waitFlushDone", e);
				// 马上回复现有的flushReady。允许它们继续flush。降为FlushAsync。
				for (var ready : flushReadies)
					ready.SendResult();
				// 触发当前没有flushReady或者所有相关zeze的完整Checkpoint。
				//  1. 安全起见是所有zeze，上面的ready.SendResult也可能丢失。
				//  2. 需要完整Checkpoint的zeze要不要持久化，以后持续触发。这点看起来没有必要。
				//  3. 这里要不要等待触发结果返回。先处理成等待。
				for (var zeze : zezeProcedures.keySet())
					checkpoint(zeze);
				for (var zeze : zezeSagas.keySet())
					checkpoint(zeze);
			}
		}
	}

	private static void checkpoint(AsyncSocket zeze) {
		var r = new Checkpoint();
		r.SendForWait(zeze).await();
	}

	private long onzTid;
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	private final TaskCompletionSource<Integer> flushDone = new TaskCompletionSource<>();

	// 以下两个集合在一个事务内只能启用一个。即不能混用FuncProcedure和FuncSaga
	private final ConcurrentHashMap<AsyncSocket, TaskCompletionSource<?>> zezeProcedures = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<AsyncSocket, TaskCompletionSource<?>> zezeSagas = new ConcurrentHashMap<>();

	public long getOnzTid() {
		return onzTid;
	}

	void trySetFlushReady(FlushReady r) {
		logger.debug("FlushReady sender={} argument={}", r.Argument, r.getSender());
		flushReadies.add(r);

		if (flushReadies.size() == zezeProcedures.size() || flushReadies.size() == zezeSagas.size()) {
			// 简单的用数量判断，足够可靠了。
			for (var ready : flushReadies)
				ready.SendResult();
			flushDone.setResult(0);
		}
	}
}
