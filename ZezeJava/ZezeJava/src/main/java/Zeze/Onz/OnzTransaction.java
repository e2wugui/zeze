package Zeze.Onz;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
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
		// R2-M③：逐参与方容错（对齐cancelSaga/commit()的FND4-86模式）——原先发送循环无
		// try/catch，第一个参与方的getZezeInstance/SendForWait异常中断整个循环：后续参与方
		// 收不到FuncSagaEnd(cancel=false)，上下文与setEnd滞留，只能等参与方cleanupTimeoutSagas
		// （默认1小时）回收；await循环同理，一个异常跳过其余等待。记error后继续，保证全部
		// 参与方都被通知。调用方commit()已有兜底catch（endSaga失败不转rollback），语义不变。
		for (var e : zezeSagas.entrySet()) {
			try {
				var r = new FuncSagaEnd();
				r.Argument.setOnzTid(onzTid);
				r.Argument.setCancel(false);
				futures.add(r.SendForWait(onzServer.getZezeInstance(e.getKey())));
			} catch (Exception ex) {
				logger.error("end saga send fail. tid={}, zeze={}", onzTid, e.getKey(), ex);
			}
		}
		for (var future : futures) {
			try {
				future.await();
			} catch (Exception ex) {
				logger.error("await end saga result. tid={}", onzTid, ex);
			}
		}
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
		// R3-C D①（M②）：记录每个步骤的rpc是否以异常收场（超时/发送失败）——只有这类步骤
		// 才可能处于"FuncSagaEnd先于FuncSaga注册被处理"的乱序窗口（成功/业务失败步骤的
		// FuncSaga已被参与方处理过，注册必然先于任何FuncSagaEnd），其eSagaNotFound需要重试。
		var stepRpcFailed = new ArrayList<Boolean>();
		var stepZeze = new ArrayList<String>();
		for (var e : zezeSagas.entrySet()) {
			try {
				// FND7-34：失败/超时的步骤同样发送cancel——不再只补偿成功的步骤。
				// saga参与方sendReadyAndWait覆写为"发结果即本地提交"，协调者rpc超时不代表
				// 参与方未提交：跳过补偿的话，超时步骤的写已持久化而协调者按失败补偿其余
				// 步骤并报告整体失败——部分提交的静默分歧。超时步骤的上下文在参与方1h超时
				// 清理（Onz.cleanupTimeoutSagas）前仍在，cancel能真正补偿；上下文不存在
				// （业务失败已自清理、请求从未到达）则应答eSagaNotFound，可辨识忽略。
				var rpcFailed = e.getValue().isCompletedExceptionally();
				if (rpcFailed)
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
				stepRpcFailed.add(rpcFailed);
				stepZeze.add(e.getKey());
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
				if (code == AbstractOnz.eSagaNotFound) {
					// 步骤从未注册或已自清理：无补偿对象，可辨识忽略。但rpc层失败的步骤可能
					// 是FuncSagaEnd先于FuncSaga注册被处理（R3-C D①乱序窗口）——单次延迟重试。
					if (stepRpcFailed.get(i))
						retryCancelNotFoundOnce(stepZeze.get(i));
					continue;
				}
				if (code != 0) {
					logger.fatal("cancel saga error {}", code);
				}
			} catch (Exception e) {
				logger.error("await cancel result.", e);
				// R3-C补遗：应答超时=结果未知——NotFound可能正在途中（参与方已应答但协调者
				// 未收到）。乱序窗口的重试补偿不得依赖应答必达：rpcFailed步骤超时同样调度单次
				// 重试。幂等安全：迟到NotFound即放弃；成功补偿后再cancel得NotFound同样无害；
				// 请求未到达则这次到达完成补偿。非rpcFailed步骤不重试（正常完成步骤的NotFound
				// 是终态，cancel在途终会到达，语义与主分支一致）。30轮压测轮5/9实证丢失窗口。
				if (stepRpcFailed.get(i))
					retryCancelNotFoundOnce(stepZeze.get(i));
			}
		}
	}

	/**
	 * R3-C D①（M② 乱序窗口）：FuncSaga与FuncSagaEnd在参与方侧同为Normal派发（共享线程池，
	 * 不保证同连接处理顺序，参见ThreadingServer.ProcessKeepAlive的Direct注解），理论上补偿
	 * 请求可先于原请求被处理——参与方查无上下文应答eSagaNotFound，而上下文随后才注册并
	 * 执行业务，该次补偿被静默吞掉且无人再发。窗口的现实前提是参与方派发线程在出队后停滞
	 * 约rpc超时（flushTimeout）量级（池饥饿/长GC），重试延迟取同量级的flushTimeout：重发一次
	 * cancel；仍eSagaNotFound即放弃（请求确实未到达或业务已自清理，无补偿对象）。正常完成
	 * （成功/业务失败）的步骤不重试——它们的FuncSaga已被参与方应答过，注册必然先于
	 * FuncSagaEnd，NotFound是终态。
	 * <p>
	 * 选型说明：不采用"FuncSaga上下文注册改Direct派发"——那需要把整个业务执行（含DB事务与
	 * sendReadyAndWait）搬进IO线程或拆分生成处理器契约，爆炸半径远大于协调者侧一次延迟重发。
	 * 也不新增"尚未注册"错误码——参与方无法区分"尚未注册"与"已清理"，且错误码常量在生成代码。
	 */
	private void retryCancelNotFoundOnce(@NotNull String zezeName) {
		try {
			Thread.sleep(flushTimeout);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			logger.error("cancel saga retry interrupted, give up. tid={}, zeze={}", onzTid, zezeName, ie);
			return;
		}
		try {
			var r = new FuncSagaEnd();
			r.Argument.setOnzTid(onzTid);
			r.Argument.setCancel(true);
			r.SendForWait(onzServer.getZezeInstance(zezeName), flushTimeout).get();
			var code = IModule.getErrorCode(r.getResultCode()); // 线上为moduleId组合值，解码后比较
			if (code == AbstractOnz.eSagaNotFound)
				logger.warn("cancel saga retry still not found, give up. tid={}, zeze={}", onzTid, zezeName);
			else if (code != 0)
				logger.fatal("cancel saga retry error {}", code);
			// code==0：乱序窗口内迟到注册的步骤已得到补偿。
		} catch (Exception ex) {
			logger.error("cancel saga retry fail. tid={}, zeze={}", onzTid, zezeName, ex);
		}
	}

	// saga参与方持久化编码（OH1-F1）：BSavedCommits.Onzs是set[string]的集群名集合，bean为
	// 生成代码（不可加字段），以带前缀编码区分参与方类型——集群名是zezeConfigs解析的'='左段，
	// 不可能包含'='（分隔符），"saga="前缀与任何集群名（以及旧版本持久化的ip_port）零碰撞；
	// 旧记录无前缀即procedure参与方，格式向后兼容。
	private static final String SagaParticipantPrefix = "saga=";

	static String encodeSagaParticipant(String zezeName) {
		return SagaParticipantPrefix + zezeName;
	}

	/** 解码持久化条目：saga参与方返回集群名，procedure参与方返回null。 */
	static String decodeSagaParticipant(String savedOnz) {
		return savedOnz.startsWith(SagaParticipantPrefix)
				? savedOnz.substring(SagaParticipantPrefix.length())
				: null;
	}

	public BSavedCommits.Data buildSavedCommits() {
		var bState = new BSavedCommits.Data();
		// 按集群名持久化（FND4-90）：地址会漂移（重连/SM通告变更），redo时由
		// getZezeInstance现查当前地址——旧地址不再作为幻影参与方被反复重试。
		for (var e : zezeProcedures.keySet()) {
			bState.getOnzs().add(e);
		}
		// saga参与方同样持久化（OH1-F1）：原先只收集zezeProcedures，saga事务该集合恒空——
		// 协调者在saveCommitPoint后崩溃（或cancelSaga的FuncSagaEnd超时丢失且不重试）时，
		// redoTimer对残留决策记录解出空参与方列表直接removeCommitRecord，已提交步骤永久
		// 未补偿，整体事务按失败收场——静默部分提交分歧。带前缀编码，redo按参与方类型分流。
		for (var e : zezeSagas.keySet()) {
			bState.getOnzs().add(encodeSagaParticipant(e));
		}
		return bState;
	}

	void commit(byte[] tidBytes, BSavedCommits.Data state) {
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
			try {
				r.SendForWait(onzServer.getZezeInstance(zeze)).await();
				if (r.getResultCode() != 0) {
					logger.fatal("rollback error {}", IModule.getErrorCode(r.getResultCode()));
				}
			} catch (Exception ex) { // FND7-68：逐参与方捕获（对齐commit()的FND4-86模式）。
				// rollback()运行在OnzServer.perform的rc!=0路径或catch块内：异常外传会被
				// perform的catch二次rollback从头重试，再抛则替换原始错误（业务rc丢失，最终
				// 只报Procedure.Exception）；且第一个参与方失败即中断循环，后续参与方收不到
				// Rollback，只能等参与方ready等待超时自愈（FND5-45）与redoTimer的老化回滚
				// 兜底，不一致窗口被拉长。记fatal后继续，保证全部参与方都收到Rollback。
				logger.fatal("rollback send/await fail. tid={}, zeze={}", onzTid, zeze, ex);
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
