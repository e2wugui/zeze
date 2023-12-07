package Zeze.Onz;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Builtin.Onz.Ready;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class OnzTransaction<A extends Data, R extends Data> {
	protected static final Logger logger = LogManager.getLogger();

	private OnzServer onzServer;
	private int flushMode = Onz.eFlushImmediately;
	private int flushTimeout = 10_000;
	private A argument;
	private R result;
	private boolean pendingAsync = false;

	synchronized void waitPendingAsync() throws InterruptedException {
		while (pendingAsync) {
			this.wait();
		}
	}

	public synchronized void setPendingAsync(boolean pending) {
		this.pendingAsync = pending;
		this.notify();
	}

	protected abstract long perform() throws Exception;

	public void setOnzServer(OnzServer onzServer) {
		this.onzServer = onzServer;
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
			} catch (InterruptedException | ExecutionException e) {
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
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("cancel if saga success.", ex);
			}
		}
		for (var future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				logger.error("await cancel result.", e);
			}
		}
	}

	void commit() throws ExecutionException, InterruptedException {
		// 对于saga，readies是空的。
		for (var ready : readies)
			ready.SendResult();

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		endSaga();
	}

	void rollback() {
		// 对于saga，readies是空的。
		for (var ready : readies)
			ready.trySendResultCode(IModule.errorCode(Onz.ModuleId, Onz.eRollback));

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		cancelSaga();
	}

	void waitFlushDone() {
		if (flushMode == Onz.eFlushImmediately)
			flushDone.await();
	}

	private final long onzTid = 0; // allocate todo
	private final ConcurrentHashSet<Rpc<?, ?>> readies = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	private final TaskCompletionSource<Integer> flushDone = new TaskCompletionSource<>();

	// 以下两个集合在一个事务内只能启用一个。即不能混用FuncProcedure和FuncSaga
	private final ConcurrentHashMap<AsyncSocket, TaskCompletionSource<?>> zezeProcedures = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<AsyncSocket, TaskCompletionSource<?>> zezeSagas = new ConcurrentHashMap<>();

	public long getOnzTid() {
		return onzTid;
	}

	void trySetReady(Ready r) {
		readies.add(r);
	}

	void trySetFlushReady(FlushReady r) {
		flushReadies.add(r);

		if (flushReadies.size() == zezeProcedures.size() || flushReadies.size() == zezeSagas.size()) {
			// 简单的用数量判断，足够可靠了。
			for (var ready : flushReadies)
				ready.SendResult();
			flushDone.setResult(0);
		}
	}
}
