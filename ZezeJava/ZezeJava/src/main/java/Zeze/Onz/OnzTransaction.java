package Zeze.Onz;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletionException;
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
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

/** 协调者侧的Onz分布式事务抽象：由业务实现perform，经callProcedure/callSaga驱动参与方，并负责Commit/Rollback决策与flush等待。 */
public abstract class OnzTransaction<A extends Data, R extends Data> extends ReentrantLock {
	protected static final Logger logger = LogManager.getLogger(OnzTransaction.class);

	private @Nullable OnzServer onzServer;
	private int flushMode = Onz.eFlushImmediately;
	private int flushTimeout = 10_000;
	private @Nullable A argument;
	private @Nullable R result;
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

	/**
	 * 失败或异常回滚前有界等待异步续作完成，避免无限挂起。
	 * @return true 表示异步续作已清空，false 表示超时仍有在途续作
	 */
	boolean waitPendingAsync(long timeoutMs) throws InterruptedException {
		lock();
		try {
			var nanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
			while (pendingAsync) {
				if (nanos <= 0)
					return false;
				nanos = thisCond.awaitNanos(nanos);
			}
			return true;
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

	private OnzServer getOnzServer() {
		var value = onzServer;
		assert value != null;
		return value;
	}

	public void setOnzServer(OnzServer onzServer) {
		this.onzServer = onzServer;
		this.onzTid = onzServer.nextOnzTid();
		this.commitDecided = false;
	}

	public int getFlushMode() {
		return flushMode;
	}

	public int getFlushTimeout() {
		return flushTimeout;
	}

	public @Nullable A getArgument() {
		return argument;
	}

	public @Nullable R getResult() {
		return result;
	}

	public void setFlushMode(int flushMode) {
		this.flushMode = flushMode;
	}

	/** 设置必须为正的超时基值（同时用于参与方决策等待、步骤 RPC 超时与 FlushReady 握手预算）。 */
	public void setFlushTimeout(int flushTimeout) {
		if (flushTimeout <= 0)
			throw new IllegalArgumentException("flushTimeout <= 0: " + flushTimeout);
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
		// procedure saga 互斥。
		if (!zezeSagas.isEmpty())
			throw new RuntimeException("can not mix funcProcedure and funcSaga. saga has called.");
		var zezeInstance = getOnzServer().getZezeInstance(zezeName);
		lock();
		try {
			// 与saga登记同锁复核：建连期间并发续作可能已登记另一种调用。
			if (!zezeSagas.isEmpty())
				throw new RuntimeException("can not mix funcProcedure and funcSaga. saga has called.");
			// 限制每个zeze集群最多一个调用：键为集群名，重连返回新socket不能绕过限制。
			var newCall = new OutObject<TaskCompletionSource<R2>>();
			zezeProcedures.computeIfAbsent(zezeName, __ -> newCall.value
					= OnzAgent.callProcedureAsync(
					this, zezeInstance, onzProcedureName, argument, result, flushMode));
			if (newCall.value == null)
				throw new RuntimeException("too many funcProcedure on same zezeInstance.");
			return newCall.value;
		} finally {
			unlock();
		}
	}

	public <A2 extends Data, R2 extends Data> TaskCompletionSource<R2>
	callSagaAsync(String zezeName, String onzProcedureName, A2 argument, R2 result) {
		// procedure saga 互斥。
		if (!zezeProcedures.isEmpty())
			throw new RuntimeException("can not mix funcProcedure and funcSaga. procedure has called.");
		var zezeInstance = getOnzServer().getZezeInstance(zezeName);
		// 登记、持久化完整参与方快照、投递在同一事务锁内顺序完成。
		// 建连仍在锁外；同一集群的并发步骤不能越过唯一性检查或覆盖先前意图。
		lock();
		try {
			// procedure登记也持本锁，检查与登记原子互斥；锁外检查仅供快速拒绝。
			if (!zezeProcedures.isEmpty())
				throw new RuntimeException("can not mix funcProcedure and funcSaga. procedure has called.");
			var future = new TaskCompletionSource<R2>();
			registerSagaIntent(zezeName, future);
			try {
				OnzAgent.sendSaga(this, zezeInstance, onzProcedureName, argument, result, flushMode, future);
			} catch (Throwable failure) {
				// 意图已持久化，发送结果不确定时保留参与方并结束future，交既有补偿/redo处理。
				future.setException(failure);
				throw failure;
			}
			return future;
		} finally {
			unlock();
		}
	}

	// 调用方持本事务锁，保存失败时只撤销尚未投递的本次登记。
	private void registerSagaIntent(String zezeName, TaskCompletionSource<?> future) {
		if (zezeSagas.putIfAbsent(zezeName, future) != null)
			throw new RuntimeException("too many funcSaga on same zezeInstance.");
		try {
			getOnzServer().saveSagaPreparingForCrashWindow(this);
		} catch (Throwable failure) {
			zezeSagas.remove(zezeName, future);
			throw failure;
		}
	}

	/**
	 * 通知全部 saga 参与方正常结束（cancel=false）。
	 * 逐参与方容错，发送/等待异常仅记日志；返回 false 表示存在 eSagaNotFound 应答，需保留 eCommitting 交 redo 超龄分诊。
	 */
	private boolean endSaga() {
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var rpcs = new ArrayList<Rpc<?, ?>>();
		var stepZeze = new ArrayList<String>();
		for (var e : zezeSagas.entrySet()) {
			try {
				var r = new FuncSagaEnd();
				r.Argument.setOnzTid(onzTid);
				r.Argument.setCancel(false);
				// 参与方需获取 businessLock，等待沿用 flushTimeout 避免过早超时。
				futures.add(r.SendForWait(getOnzServer().getZezeInstance(e.getKey()), flushTimeout));
				rpcs.add(r);
				stepZeze.add(e.getKey());
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
		var allEnded = true;
		for (int i = 0; i < rpcs.size(); i++) {
			if (IModule.getErrorCode(rpcs.get(i).getResultCode()) == AbstractOnz.eSagaNotFound) {
				allEnded = false;
				logger.error("end saga eSagaNotFound: 成功步骤的上下文在end送达前消失"
						+ " (keep eCommitting record for redo aged triage). tid={}, zeze={}",
						onzTid, stepZeze.get(i));
			}
		}
		return allEnded;
	}

	/** saga 步骤的应答形态，供 cancelSaga 对 eSagaNotFound 分诊。 */
	private enum SagaStepAnswer {
		/** 已应答成功（含结果解码失败）：NotFound 视为上下文丢失嫌疑，保留决策记录。 */
		ANSWERED_SUCCESS,
		/** 已应答业务失败：参与方已自清理，NotFound 为良性终态。 */
		ANSWERED_FAIL,
		/** 未应答失败（超时或发送失败）：可能处于乱序窗口，NotFound 触发单次延迟重试。 */
		RPC_FAILED
	}

	// 本事务已成功补偿的步骤，防止 rollback 重入时将良性重复 NotFound 误报为上下文丢失。
	private final Set<String> cancelDeliveredOk = ConcurrentHashMap.newKeySet();

	/**
	 * 补偿已发出的全部 saga 步骤（包括超时/失败步骤）。
	 * @return true 表示全部参与方已投递了结；false 表示存在不确定投递、致命应答或上下文丢失嫌疑，需保留记录交 redo
	 */
	private boolean cancelSaga() {
		// 等待已发出的全部 saga 异步结果并记录应答形态，供后续 NotFound 分诊。
		var stepAnswers = new HashMap<String, SagaStepAnswer>();
		for (var e : zezeSagas.entrySet()) {
			var answer = SagaStepAnswer.RPC_FAILED;
			try {
				e.getValue().get();
				answer = SagaStepAnswer.ANSWERED_SUCCESS;
			} catch (Exception ex) {
				logger.error("await saga result.", ex);
				if (ex instanceof CompletionException && ex.getCause() instanceof OnzAgent.CallAnsweredException answered)
					answer = answered.participantSucceeded ? SagaStepAnswer.ANSWERED_SUCCESS : SagaStepAnswer.ANSWERED_FAIL;
			}
			stepAnswers.put(e.getKey(), answer);
		}
		var allDelivered = true;
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var rpcs = new ArrayList<FuncSagaEnd>();
		var stepAnswer = new ArrayList<SagaStepAnswer>();
		var stepZeze = new ArrayList<String>();
		for (var e : zezeSagas.entrySet()) {
			try {
				// 失败/超时步骤同样发送 cancel：参与方可能已本地提交，无上下文时将应答 eSagaNotFound。
				var answer = stepAnswers.getOrDefault(e.getKey(), SagaStepAnswer.RPC_FAILED);
				if (answer == SagaStepAnswer.RPC_FAILED)
					logger.warn("saga step failed (maybe timeout), send cancel anyway. tid={}, zeze={}",
							onzTid, e.getKey());
				var r = new FuncSagaEnd();
				r.Argument.setOnzTid(onzTid);
				r.Argument.setCancel(true);
				futures.add(r.SendForWait(getOnzServer().getZezeInstance(e.getKey()), flushTimeout));
				rpcs.add(r);
				stepAnswer.add(answer);
				stepZeze.add(e.getKey());
			} catch (Exception ex) {
				allDelivered = false;
				logger.error("cancel saga.", ex);
			}
		}
		for (int i = 0; i < futures.size(); i++) {
			try {
				futures.get(i).get();
				var code = IModule.getErrorCode(rpcs.get(i).getResultCode());
				if (code == AbstractOnz.eSagaNotFound) {
					if (stepAnswer.get(i) == SagaStepAnswer.RPC_FAILED) {
						if (!retryCancelNotFoundOnce(stepZeze.get(i)))
							allDelivered = false;
					} else if (stepAnswer.get(i) == SagaStepAnswer.ANSWERED_SUCCESS
							&& !cancelDeliveredOk.contains(stepZeze.get(i))) {
						// 已应答成功但未曾补偿过的步骤缺失上下文，保留记录并登记丢失嫌疑。
						allDelivered = false;
						getOnzServer().noteSagaContextLost(onzTid, stepZeze.get(i));
					}
					continue;
				}
				if (code != 0) {
					allDelivered = false;
					logger.fatal("cancel saga error {}", code);
					continue;
				}
				cancelDeliveredOk.add(stepZeze.get(i));
			} catch (Exception e) {
				allDelivered = false;
				logger.error("await cancel result.", e);
				// 未应答步骤超时时同样做单次延迟重试，覆盖乱序窗口内 NotFound 在途或丢失的情况。
				if (stepAnswer.get(i) == SagaStepAnswer.RPC_FAILED)
					retryCancelNotFoundOnce(stepZeze.get(i));
			}
		}
		return allDelivered;
	}

	/**
	 * 针对未应答步骤的乱序窗口（FuncSagaEnd 先于 FuncSaga 到达并注册），延迟 flushTimeout 后重试一次 cancel。
	 * @return true 表示补发补偿成功；false 表示仍未找到或失败，保留决策记录交由 redo 确认
	 */
	private boolean retryCancelNotFoundOnce(String zezeName) {
		try {
			Thread.sleep(flushTimeout);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			logger.error("cancel saga retry interrupted, give up. tid={}, zeze={}", onzTid, zezeName, ie);
			return false;
		}
		try {
			var r = new FuncSagaEnd();
			r.Argument.setOnzTid(onzTid);
			r.Argument.setCancel(true);
			r.SendForWait(getOnzServer().getZezeInstance(zezeName), flushTimeout).get();
			var code = IModule.getErrorCode(r.getResultCode());
			if (code == AbstractOnz.eSagaNotFound) {
				logger.warn("cancel saga retry still not found, keep decision record for redo."
								+ " tid={}, zeze={}",
						onzTid, zezeName);
				return false;
			}
			if (code != 0) {
				logger.fatal("cancel saga retry error {}", code);
				return false;
			}
			return true;
		} catch (Exception ex) {
			logger.error("cancel saga retry fail. tid={}, zeze={}", onzTid, zezeName, ex);
			return false;
		}
	}

	// saga 参与方持久化前缀编码（集群名禁止含 '='，无前缀即 procedure 参与方，兼容旧记录）。
	private static final String SagaParticipantPrefix = "saga=";

	static String encodeSagaParticipant(String zezeName) {
		return SagaParticipantPrefix + zezeName;
	}

	/** 解码持久化条目：saga参与方返回集群名，procedure参与方返回null。 */
	static @Nullable String decodeSagaParticipant(String savedOnz) {
		return savedOnz.startsWith(SagaParticipantPrefix)
				? savedOnz.substring(SagaParticipantPrefix.length())
				: null;
	}

	public BSavedCommits.Data buildSavedCommits() {
		var bState = new BSavedCommits.Data();
		// 按集群名持久化，redo 时动态解析当前地址；saga 参与方带前缀编码以便分流。
		for (var e : zezeProcedures.keySet()) {
			bState.getOnzs().add(e);
		}
		for (var e : zezeSagas.keySet()) {
			bState.getOnzs().add(encodeSagaParticipant(e));
		}
		return bState;
	}

	private boolean commitDecided;

	boolean isCommitDecided() {
		return commitDecided;
	}

	void commit(byte[] tidBytes, BSavedCommits.Data state) {
		// 先持久化 eCommitting 决策，确保异常或宕机后可由 redoTimer 幂等补发 Commit。
		try {
			getOnzServer().saveCommitPoint(tidBytes, state, AbstractOnz.eCommitting);
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
		commitDecided = true;

		// 决策点已持久化，后续通知失败不得上抛触发 rollback；保留 eCommitting 交由 redoTimer 幂等补发。
		var commitFail = false;
		for (var zeze : zezeProcedures.keySet()) {
			var r = new Commit();
			r.Argument.setOnzTid(onzTid);
			try {
				r.SendForWait(getOnzServer().getZezeInstance(zeze)).await();
			} catch (Exception ex) {
				commitFail = true;
				markParticipantFlushImpossible(zeze);
				logger.fatal("commit await fail. tid={}, keep eCommitting for redo.", onzTid, ex);
				continue;
			}
			if (r.getResultCode() != 0) {
				// 参与方已超时自愈回滚返回一次性 eDivergence：记录 error 暴露分歧后按该参与方了结，不重发。
				if (IModule.getErrorCode(r.getResultCode()) == AbstractOnz.eDivergence) {
					markParticipantFlushImpossible(zeze);
					logger.error("onz divergence: 已超时回滚的参与方收到迟到决策=部分提交分歧"
							+ " (participant rolled back before Commit arrived while coordinator committed;"
							+ " data divergence). tid={}, zeze={}. 收场不变：不置commitFail、记录照删、不重发.",
							onzTid, zeze);
					continue;
				}
				commitFail = true;
				markParticipantFlushImpossible(zeze);
				logger.fatal("commit error {}", IModule.getErrorCode(r.getResultCode()));
			}
		}

		// saga 已过决策点，endSaga 异常不得转 rollback；存在 eSagaNotFound 时保留记录交 redo 超龄分诊。
		try {
			if (!endSaga())
				commitFail = true;
		} catch (Exception ex) {
			logger.fatal("endSaga fail. tid={}", onzTid, ex);
		}

		if (!commitFail)
			getOnzServer().removeCommitRecord(tidBytes);
	}

	/**
	 * 向全部参与方发送 Rollback / cancelSaga。
	 * @return true 表示全部参与方投递了结，false 表示存在投递不确定或致命应答，需保留 ePreparing 交 redo 补发
	 */
	boolean rollback() {
		var allDelivered = true;
		for (var zeze : zezeProcedures.keySet()) {
			var r = new Rollback();
			r.Argument.setOnzTid(onzTid);
			try {
				r.SendForWait(getOnzServer().getZezeInstance(zeze)).await();
				if (r.getResultCode() != 0) {
					// 参与方已超时回滚返回 eDivergence：结局一致但记录预算违例告警，不重发。
					if (IModule.getErrorCode(r.getResultCode()) == AbstractOnz.eDivergence) {
						logger.error("onz divergence signal: 已超时回滚的参与方收到迟到决策"
								+ " (participant rolled back before Rollback arrived -- outcome aligned,"
								+ " no data divergence for this participant; signal = coordinator decision"
								+ " latency exceeded participant decision-wait budget, ONZ-F25-01 root cause)."
								+ " tid={}, zeze={}. 按投递了结：不保留记录重发.", onzTid, zeze);
					} else {
						allDelivered = false;
						logger.fatal("rollback error {}", IModule.getErrorCode(r.getResultCode()));
					}
				}
			} catch (Exception ex) {
				// 逐参与方容错，避免单个失败中断其余参与方的 Rollback 通知。
				allDelivered = false;
				logger.fatal("rollback send/await fail. tid={}, zeze={}", onzTid, zeze, ex);
			}
		}

		if (!cancelSaga())
			allDelivered = false;
		return allDelivered;
	}

	void waitFlushDone() {
		if (flushMode != Onz.eFlushImmediately || zezeProcedures.isEmpty()) {
			// 非立即刷盘或 saga 事务免等 FlushReady，直接开闸放行后续可能到达的重试 ready。
			openFlushGate();
			return;
		}
		try {
			flushDone.get(flushTimeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.warn("waitFlushDone", e);
			// 超时降级：先开闸放行已到达的 ready，再对全部参与方触发一次 Checkpoint；已过决策点，异常不外传。
			openFlushGate();
			for (var zeze : zezeProcedures.keySet()) {
				try {
					checkpoint(getOnzServer().getZezeInstance(zeze));
				} catch (Exception ex) { // logger.fatal
					logger.fatal("waitFlushDone checkpoint fail. tid={}, zeze={}", onzTid, zeze, ex);
				}
			}
		}
	}

	/** 幂等应答 FlushReady，避免参与方提交路径一直阻塞。 */
	private static void replyReady(Rpc<?, ?> ready) {
		if (!ready.isSendResultDone())
			ready.SendResult();
	}

	private static void checkpoint(AsyncSocket zeze) {
		var r = new Checkpoint();
		r.SendForWait(zeze).await();
	}

	private long onzTid;
	// 已登记的全部 FlushReady（开闸时逐条应答）；完成计数按参与方身份去重（旧端无 Participant 时按 RPC 实例兜底）。
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	private final Set<String> distinctParticipants = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashSet<Rpc<?, ?>> legacyReadies = new ConcurrentHashSet<>();
	// Commit 投递失败的参与方不会发送 FlushReady，从完成判据分母中剔除。
	private final Set<String> flushImpossibleParticipants = ConcurrentHashMap.newKeySet();
	private final TaskCompletionSource<Integer> flushDone = new TaskCompletionSource<>();
	// true 后到达的 FlushReady 一律立即应答（已收齐、已降级或免等）。
	private volatile boolean flushGateOpen;

	// 以下两个集合按集群名去重（每集群最多一个调用），同一事务内互斥（不可混用 Procedure 与 Saga）。
	private final ConcurrentHashMap<String, TaskCompletionSource<?>> zezeProcedures = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, TaskCompletionSource<?>> zezeSagas = new ConcurrentHashMap<>();

	public long getOnzTid() {
		return onzTid;
	}

	void trySetFlushReady(FlushReady r) {
		logger.debug("FlushReady sender={} argument={}", r.Argument, r.getSender());

		if (flushGateOpen || flushMode != Onz.eFlushImmediately || !zezeSagas.isEmpty()) {
			replyReady(r);
			return;
		}

		if (flushReadies.isEmpty()) {
			// 首条 ready 到达起排 1.5×flushTimeout 兜底定时，到期强制开闸降级，留余量覆盖参与方 2×flushTimeout 预算。
			TaskSpec.ofAction(this::openFlushGateOnHoldExpired).schedule(flushTimeout + Math.max(1, flushTimeout / 2));
		}
		flushReadies.add(r);
		var participant = r.Argument.getParticipant();
		if (participant.isEmpty()) {
			// 兼容旧版参与方（未携带 Participant），按 RPC 对象兜底计数并告警一次。
			if (legacyReadies.isEmpty())
				logger.warn("FlushReady without Participant (old client?), count by rpc identity. tid={}", onzTid);
			legacyReadies.add(r);
		} else if (!distinctParticipants.add(participant)) {
			// 同一参与方重试发送的 FlushReady 不重复计数。
			logger.warn("duplicate FlushReady from same participant (flush retry?). tid={}, participant={}",
					onzTid, participant);
		}
		checkFlushGate();
		// 并发开闸可能发生在本条登记前，登记后复查确保必有应答。
		if (flushGateOpen)
			replyReady(r);
	}

	/** 幂等开闸：应答全部已暂存的 FlushReady 并完成 flushDone。 */
	private void openFlushGate() {
		if (flushGateOpen)
			return;
		flushGateOpen = true;
		for (var ready : flushReadies)
			replyReady(ready);
		flushDone.setResult(0);
	}

	// 持有期限到期后强制开闸降级为异步 flush。
	private void openFlushGateOnHoldExpired() {
		if (flushGateOpen)
			return;
		logger.warn("FlushReady hold budget (1.5*flushTimeout) expired, degrade flush to async. tid={}", onzTid);
		openFlushGate();
	}

	// 将 Commit 投递失败的参与方移出刷盘等待分母，并立即复检开闸条件。
	void markParticipantFlushImpossible(String zeze) {
		flushImpossibleParticipants.add(zeze);
		checkFlushGate();
	}

	private void checkFlushGate() {
		if (flushGateOpen)
			return;
		if (distinctParticipants.size() + legacyReadies.size()
				>= zezeProcedures.size() - flushImpossibleParticipants.size()) {
			openFlushGate();
		}
	}
}
