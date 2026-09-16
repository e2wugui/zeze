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
import org.jetbrains.annotations.NotNull;

public abstract class OnzTransaction<A extends Data, R extends Data> extends ReentrantLock {
	protected static final @NotNull Logger logger = LogManager.getLogger(OnzTransaction.class);

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
			thisCond.signal();
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
		// 限制每个zeze集群最多一个调用：键为集群名（FND4-90），重连返回新socket不再绕过限制。
		var newCall = new OutObject<TaskCompletionSource<R2>>();
		zezeProcedures.computeIfAbsent(zezeName, __ -> newCall.value
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
		// 限制每个zeze集群最多一个调用：键为集群名（FND4-90）。
		var newCall = new OutObject<TaskCompletionSource<R2>>();
		zezeSagas.computeIfAbsent(zezeName, __ -> newCall.value
				= OnzAgent.callSagaAsync(
				this, zezeInstance, onzProcedureName, argument, result, flushMode));
		if (newCall.value == null)
			throw new RuntimeException("too many funcSaga on same zezeInstance.");
		return newCall.value;
	}

	private void endSaga() {
		// 执行过程中发生异常或者错误不能到达这里，而是rollback里面的cancelSaga。
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : zezeSagas.entrySet()) {
			var r = new FuncSagaEnd();
			r.Argument.setOnzTid(onzTid);
			r.Argument.setCancel(false);
			futures.add(r.SendForWait(onzServer.getZezeInstance(e.getKey())));
		}
		for (var future : futures)
			future.await();
	}

	private void cancelSaga() {
		// 等待已经发出的saga的结果（包括失败的），
		// 因为saga可能异步发送，并且中途发生了错误，
		// 此时需要继续把没得到的结果等到。
		for (var saga : zezeSagas.values()) {
			try {
				saga.get();
			} catch (Exception e) {
				logger.error("await saga result.", e);
			}
		}
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var rpcs = new ArrayList<FuncSagaEnd>();
		for (var e : zezeSagas.entrySet()) {
			try {
				// FND7-34：失败/超时的步骤同样发送cancel——不再只补偿成功的步骤。
				// saga参与方sendReadyAndWait覆写为"发结果即本地提交"，协调者rpc超时不代表
				// 参与方未提交：跳过补偿的话，超时步骤的写已持久化而协调者按失败补偿其余
				// 步骤并报告整体失败——部分提交的静默分歧。超时步骤的上下文在参与方1h超时
				// 清理（Onz.cleanupTimeoutSagas）前仍在，cancel能真正补偿；上下文不存在
				// （业务失败已自清理、请求从未到达）则应答eSagaNotFound，可辨识忽略。
				if (e.getValue().isCompletedExceptionally())
					logger.warn("saga step failed (maybe timeout), send cancel anyway. tid={}, zeze={}",
							onzTid, e.getKey());
				var r = new FuncSagaEnd();
				r.Argument.setOnzTid(onzTid);
				r.Argument.setCancel(true);
				// cancel目标可能正是执行超时的慢参与方：FuncSagaEnd的处理在参与方侧与仍在
				// 执行的业务互斥（OnzSaga.businessLock）后才补偿，慢步骤的应答自然来得慢，
				// 等待沿用flushTimeout，不用默认5s过早放弃。
				futures.add(r.SendForWait(onzServer.getZezeInstance(e.getKey()), flushTimeout));
				rpcs.add(r);
			} catch (Exception ex) {
				logger.error("cancel saga.", ex);
			}
		}
		for (int i = 0; i < futures.size(); i++) {
			try {
				futures.get(i).get();
				// R3-C复审：参与方处理器 return errorCode(eSagaNotFound) 时线上结果码是
				// makeTypeId(ModuleId, code) 的组合值（rpc 的 resultCode 原样携带），直接与
				// 常量2比较恒不相等——NotFound落进 fatal 分支（假致命日志）且"可辨识忽略"
				// 从未生效。先经 IModule.getErrorCode 解码再比较。
				var code = IModule.getErrorCode(rpcs.get(i).getResultCode());
				if (code == AbstractOnz.eSagaNotFound)
					continue; // 步骤从未注册或已自清理：无补偿对象，可辨识忽略。
				if (code != 0) {
					logger.fatal("cancel saga error {}", code);
				}
			} catch (Exception e) {
				logger.error("await cancel result.", e);
			}
		}
	}

	public BSavedCommits.Data buildSavedCommits() {
		var bState = new BSavedCommits.Data();
		// 按集群名持久化（FND4-90）：地址会漂移（重连/SM通告变更），redo时由
		// getZezeInstance现查当前地址——旧地址不再作为幻影参与方被反复重试。
		for (var e : zezeProcedures.keySet()) {
			bState.getOnzs().add(e);
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
			onzServer.removeCommitRecord(tidBytes);
			throw new RuntimeException(ex);
		}

		// saveCommitPoint(eCommitting)成功以后，commit决策已持久化。
		// 此后Commit/FuncSagaEnd应答超时或发送失败不能把异常抛出去转成rollback()
		// （OnzServer.perform的catch会执行rollback）：参与方可能已按Commit提交，
		// 再发Rollback会让未决的参与方回滚，造成一边已提交一边已回滚的部分提交（破坏2pc决策）。
		// 正确的方向是维持commit决策并保留commitIndex，由redoTimer周期重发Commit（幂等）直到全部完成。
		var commitFail = false;
		for (var zeze : zezeProcedures.keySet()) {
			var r = new Commit();
			r.Argument.setOnzTid(onzTid);
			try {
				r.SendForWait(onzServer.getZezeInstance(zeze)).await();
			} catch (Exception ex) { // await 不声明受检异常（中断在内部分理），统一捕获
				commitFail = true;
				logger.fatal("commit await fail. tid={}, keep eCommitting for redo.", onzTid, ex);
				continue; // 继续通知其余参与方
			}
			if (r.getResultCode() != 0) {
				// 参与方未决（ready条目还在），保留索引重发是唯一收敛路径。
				commitFail = true;
				logger.fatal("commit error {}", IModule.getErrorCode(r.getResultCode()));
			}
		}

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		// saga同样已过决策点（成功步骤已提交）：endSaga失败不能转rollback，
		// 否则cancelSaga会把已成功的步骤补偿掉。滞留的saga上下文由参与方超时清理兜底（Onz.cleanupTimeoutSagas）。
		try {
			endSaga();
		} catch (Exception ex) {
			logger.fatal("endSaga fail. tid={}", onzTid, ex);
		}

		if (!commitFail)
			onzServer.removeCommitRecord(tidBytes);
		// else: 保留eCommitting索引，redoTimer重发Commit，全部应答后由redo清理。
	}

	void rollback() {
		// 对于saga，是空的。
		for (var zeze : zezeProcedures.keySet()) {
			var r = new Rollback();
			r.Argument.setOnzTid(onzTid);
			r.SendForWait(onzServer.getZezeInstance(zeze)).await();
			if (r.getResultCode() != 0) {
				logger.fatal("rollback error {}", IModule.getErrorCode(r.getResultCode()));
			}
		}

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		cancelSaga();
	}

	void waitFlushDone() {
		if (flushMode != Onz.eFlushImmediately || zezeProcedures.isEmpty()) {
			// saga事务（或eFlushAsync）不计数等待：参与方首次flush早于FuncSagaEnd（setEnd），
			// 按设计不发FlushReady，计数永不满足，等待只会固定挂满flushTimeout再降级（FND3-52）。
			// 开闸：此后到达的ready（saga重试flush等）一律立即应答。
			flushGateOpen = true;
			return;
		}
		try {
			flushDone.get(flushTimeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.warn("waitFlushDone", e);
			// 马上回复现有的flushReady。允许它们继续flush。降为FlushAsync。
			for (var ready : flushReadies)
				replyReady(ready);
			// 触发当前没有flushReady或者所有相关zeze的完整Checkpoint。
			//  1. 安全起见是所有zeze，上面的ready.SendResult也可能丢失。
			//  2. 需要完整Checkpoint的zeze要不要持久化，以后持续触发。这点看起来没有必要。
			//  3. 这里要不要等待触发结果返回。先处理成等待。
			// 决策点(commit已持久化)之后的异常不外传（对齐commit()的同类原则）：
			// 单个checkpoint失败仅记fatal，该参与方由redoTimer的Commit重发兜底，
			// 异常逃逸会让OnzServer.perform的catch执行rollback()并向调用方返回失败
			// ——已实际提交的事务报告假阴性，调用方重发导致业务重复执行（FND4-86）。
			for (var zeze : zezeProcedures.keySet()) {
				try {
					checkpoint(onzServer.getZezeInstance(zeze));
				} catch (Exception ex) { // logger.fatal
					logger.fatal("waitFlushDone checkpoint fail. tid={}, zeze={}", onzTid, zeze, ex);
				}
			}
		} finally {
			// 开闸瞬间可能有ready正走进计数分支（读到旧闸值、计数未满足）而未被上面的降级应答
			// 覆盖：补发应答。此后到达的由trySetFlushReady到达即应答。
			flushGateOpen = true;
			for (var ready : flushReadies)
				replyReady(ready);
		}
	}

	/** 应答一条FlushReady（幂等）：参与方在Checkpoint.flush提交路径死等应答，
	 * 任何状态不被应答的ready都会演变成参与方事务失败halt（FND3-52）。 */
	private static void replyReady(Rpc<?, ?> ready) {
		if (!ready.isSendResultDone()) // 这里忽略重复发送警告。
			ready.SendResult();
	}

	private static void checkpoint(AsyncSocket zeze) {
		var r = new Checkpoint();
		r.SendForWait(zeze).await();
	}

	private long onzTid;
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	private final TaskCompletionSource<Integer> flushDone = new TaskCompletionSource<>();
	// true之后到达的FlushReady一律立即应答（不再计数门控）：等待收齐、降级、或免等（saga/eFlushAsync）。
	private volatile boolean flushGateOpen;

	// 以下两个集合在一个事务内只能启用一个。即不能混用FuncProcedure和FuncSaga
	// 去重键=集群名（FND4-90）：socket实例在重连后变化——以实例为键使"每集群最多一个调用"
	// 在事务内重连窗口失效，且新旧两个地址都被持久化为参与方，redo对死地址永不收敛。
	private final ConcurrentHashMap<String, TaskCompletionSource<?>> zezeProcedures = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, TaskCompletionSource<?>> zezeSagas = new ConcurrentHashMap<>();

	public long getOnzTid() {
		return onzTid;
	}

	void trySetFlushReady(FlushReady r) {
		logger.debug("FlushReady sender={} argument={}", r.Argument, r.getSender());

		// saga事务不计数等待：重试flush的ready到达时协调者已越过等待点，立即应答（FND3-52）。
		if (flushGateOpen || !zezeSagas.isEmpty()) {
			replyReady(r);
			return;
		}

		flushReadies.add(r);
		if (flushReadies.size() == zezeProcedures.size()) {
			// 简单的用数量判断，足够可靠了。
			flushGateOpen = true;
			for (var ready : flushReadies)
				replyReady(ready);
			flushDone.setResult(0);
			return;
		}
		// 计数未满足但闸已开（与waitFlushDone收尾并发）：立即应答，等waitFlushDone的扫尾
		// 应答覆盖本条会多等其剩余的checkpoint等待时长。
		if (flushGateOpen)
			replyReady(r);
	}
}
