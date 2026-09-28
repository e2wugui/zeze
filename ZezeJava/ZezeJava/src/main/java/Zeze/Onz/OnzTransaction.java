package Zeze.Onz;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/** 协调者侧的Onz分布式事务抽象：由业务实现perform，经callProcedure/callSaga驱动参与方，并负责Commit/Rollback决策与flush等待。 */
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

	/** 失败/异常回滚前的有界排空（onz-02，调用方OnzServer.drainPendingAsyncBeforeRollback）。
	 * 上限=flushTimeout：单步rpc超时（callSagaAsync/callProcedureAsync的setTimeout）是续作
	 * 在途一步的完成预算——成功路径的无界waitPendingAsync()是正确性前提（决策=commit必须
	 * 等续作终态），失败路径决策=rollback，等死不可取，超预算交调用方warn后继续。
	 * @return true=旗已清（续作完成）；false=超预算仍在途（契约违例：永挂/超长链）。 */
	boolean waitPendingAsync(long timeoutMs) throws InterruptedException {
		lock();
		try {
			var nanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
			while (pendingAsync) {
				if (nanos <= 0)
					return false;
				// awaitNanos返回剩余预算（超时≤0）：虚假唤醒带剩余值重等；signal只来自setPendingAsync。
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

	/** flushTimeout同时用作参与方决策等待（OnzProcedure.sendReadyAndWait的awaitDecision）、
	 * 调用rpc超时（OnzAgent.callProcedureAsync/callSagaAsync的setTimeout）与FlushReady
	 * 握手预算（onz-02的2×派生基值）：≤0会使awaitDecision(0)恒超时——每个procedure参与方
	 * 100%概率超时自愈回滚并进入假成功分歧路径（ONZ-F25-01族），构造期拒绝（onz-06）。 */
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
		var zezeInstance = onzServer.getZezeInstance(zezeName);
		// 限制每个zeze集群最多一个调用：键为集群名，重连返回新socket不能绕过限制。
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
		// procedure saga 互斥。
		if (!zezeProcedures.isEmpty())
			throw new RuntimeException("can not mix funcProcedure and funcSaga. procedure has called.");
		var zezeInstance = onzServer.getZezeInstance(zezeName);
		// 限制每个zeze集群最多一个调用：键为集群名。
		var newCall = new OutObject<TaskCompletionSource<R2>>();
		zezeSagas.computeIfAbsent(zezeName, __ -> newCall.value
				= OnzAgent.callSagaAsync(
				this, zezeInstance, onzProcedureName, argument, result, flushMode));
		if (newCall.value == null)
			throw new RuntimeException("too many funcSaga on same zezeInstance.");
		return newCall.value;
	}

	/** @return 决策是否对全部saga参与方干净投递了end（无eSagaNotFound应答）；false=存在
	 * NotFound应答的参与方——调用方据此置commitFail保留eCommitting记录交redo的超龄NotFound
	 * 分诊终判（onz-05）。发送/等待异常仍只记error不置失败：end无数据效应，投递不确定
	 * （超时/发送失败）不是对账依据，滞留上下文由参与方cleanupTimeoutSagas兜底回收；
	 * 只有NotFound（参与方明确应答"上下文不在"）才是丢写嫌疑信号。
	 * <p>
	 * onz-05语义论证：endSaga只在全部步骤成功应答后发送（commit()在perform rc==0路径），
	 * 成功步骤的上下文在end送达前不应消失——end本身是上下文的唯一正常清理者；业务失败
	 * 自清理不可能（失败步骤不进入本路径）；TTL清理需上下文空闲超sagaContextTimeoutMs
	 *（默认1h），与end的正常送达时延（秒级）矛盾。故NotFound指向异常消失：最恶性的形态
	 * 是参与方在"发结果（sendReadyAndWait发即返回）→本地finalCommit落库"间隙宕机（写丢失，
	 * 协调者却已把该步骤计入成功链路不再补偿=假成功丢写），也可能是end已应用的重复
	 * （窄窗口）/参与方在落库后宕机（写安全）——参与方侧不可区分，交redo按记录年龄统一分诊。 */
	private boolean endSaga() {
		// 执行过程中发生异常或者错误不能到达这里，而是rollback里面的cancelSaga。
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var rpcs = new ArrayList<Rpc<?, ?>>();
		var stepZeze = new ArrayList<String>();
		// 逐参与方容错（同cancelSaga/commit()）——发送循环若无
		// try/catch，第一个参与方的getZezeInstance/SendForWait异常中断整个循环：后续参与方
		// 收不到FuncSagaEnd(cancel=false)，上下文与setEnd滞留，只能等参与方cleanupTimeoutSagas
		// （默认1小时）回收；await循环同理，一个异常跳过其余等待。记error后继续，保证全部
		// 参与方都被通知。调用方commit()已有兜底catch（endSaga失败不转rollback）。
		for (var e : zezeSagas.entrySet()) {
			try {
				var r = new FuncSagaEnd();
				r.Argument.setOnzTid(onzTid);
				r.Argument.setCancel(false);
				// 同cancelSaga：FuncSagaEnd在参与方侧与在途业务/flush互斥（businessLock），
				// 应答可能慢于rpc默认超时，等待沿用flushTimeout——默认5s超时只会产生噪声
				// error日志，不改变任何决策（end无数据效应）。
				futures.add(r.SendForWait(onzServer.getZezeInstance(e.getKey()), flushTimeout));
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
		// 应答非0码是正常完成（Rpc直接setResult），必须显式检查（对齐cancelSaga/commit()）；
		// 线上结果码为moduleId组合值，解码后比较。
		var allEnded = true;
		for (int i = 0; i < rpcs.size(); i++) {
			if (IModule.getErrorCode(rpcs.get(i).getResultCode()) == AbstractOnz.eSagaNotFound) {
				allEnded = false;
				logger.error("end saga eSagaNotFound: 成功步骤的上下文在end送达前消失"
						+ " (possible lost write if participant crashed between result-send and local commit,"
						+ " ONZ-F25-05; keep eCommitting record for redo aged triage). tid={}, zeze={}",
						onzTid, stepZeze.get(i));
			}
		}
		return allEnded;
	}

	/** @return 决策是否对全部saga参与方投递了结（成功/终态NotFound）；false=存在投递
	 * 不确定（发送失败/超时）或致命应答的步骤——rollback()据此供commit失败路径保留
	 * 决策记录交redo补发。 */
	private boolean cancelSaga() {
		// 等待已经发出的saga的结果（包括失败的），
		// 因为saga可能异步发送，并且中途发生了错误，
		// 此时需要继续把没得到的结果等到。
		// 记录每个步骤的rpc是否"未被应答地失败"（超时/发送失败）——只有这类步骤
		// 才可能处于"FuncSagaEnd先于FuncSaga注册被处理"的乱序窗口（已应答步骤——成功/
		// 业务失败/应答decode失败，以OnzAgent.CallAnsweredException完成——的FuncSaga
		// 已被参与方处理过，注册必然先于任何FuncSagaEnd），其eSagaNotFound需要重试。
		var rpcFailedSteps = new HashMap<String, Boolean>();
		for (var e : zezeSagas.entrySet()) {
			var rpcFailed = true;
			try {
				e.getValue().get();
				rpcFailed = false;
			} catch (Exception ex) {
				logger.error("await saga result.", ex);
				rpcFailed = !(ex instanceof CompletionException
						&& ex.getCause() instanceof OnzAgent.CallAnsweredException);
			}
			rpcFailedSteps.put(e.getKey(), rpcFailed);
		}
		var allDelivered = true;
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var rpcs = new ArrayList<FuncSagaEnd>();
		var stepRpcFailed = new ArrayList<Boolean>();
		var stepZeze = new ArrayList<String>();
		for (var e : zezeSagas.entrySet()) {
			try {
				// 失败/超时的步骤同样发送cancel——不只补偿成功的步骤。
				// saga参与方sendReadyAndWait覆写为"发结果即本地提交"，协调者rpc超时不代表
				// 参与方未提交：跳过补偿的话，超时步骤的写已持久化而协调者按失败补偿其余
				// 步骤并报告整体失败——部分提交的静默分歧。超时步骤的上下文在参与方1h超时
				// 清理（Onz.cleanupTimeoutSagas）前仍在，cancel能真正补偿；上下文不存在
				// （业务失败已自清理、请求从未到达）则应答eSagaNotFound，可辨识忽略。
				var rpcFailed = rpcFailedSteps.getOrDefault(e.getKey(), true);
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
				allDelivered = false; // 发送失败=投递不确定（返回值契约见方法javadoc）
				logger.error("cancel saga.", ex);
			}
		}
		for (int i = 0; i < futures.size(); i++) {
			try {
				futures.get(i).get();
					// 参与方处理器 return errorCode(eSagaNotFound) 时线上结果码是
					// makeTypeId(ModuleId, code) 的组合值（rpc 的 resultCode 原样携带），直接与
					// 常量2比较恒不相等——NotFound会落进 fatal 分支（假致命日志），"可辨识忽略"
					// 失效。先经 IModule.getErrorCode 解码再比较。
				var code = IModule.getErrorCode(rpcs.get(i).getResultCode());
				if (code == AbstractOnz.eSagaNotFound) {
					// 步骤从未注册或已自清理：无补偿对象，可辨识忽略。但未应答失败的步骤可能
					// 是FuncSagaEnd先于FuncSaga注册被处理（乱序窗口）——单次延迟重试。
					if (stepRpcFailed.get(i) && !retryCancelNotFoundOnce(stepZeze.get(i)))
						allDelivered = false;
					continue;
				}
				if (code != 0) {
					allDelivered = false;
					logger.fatal("cancel saga error {}", code);
				}
			} catch (Exception e) {
				allDelivered = false;
				logger.error("await cancel result.", e);
				// 应答超时=结果未知——NotFound可能正在途中（参与方已应答但协调者
				// 未收到）。乱序窗口的重试补偿不得依赖应答必达：rpcFailed步骤超时同样调度单次
				// 重试。幂等安全：迟到NotFound即放弃；成功补偿后再cancel得NotFound同样无害；
				// 请求未到达则这次到达完成补偿。已应答步骤不重试（其NotFound是终态，
				// cancel在途终会到达，语义与主分支一致）。
				if (stepRpcFailed.get(i))
					retryCancelNotFoundOnce(stepZeze.get(i));
			}
		}
		return allDelivered;
	}

	/**
	 * 乱序窗口：FuncSaga与FuncSagaEnd在参与方侧同为Normal派发（共享线程池，
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
	 *
	 * @return true=该步骤补偿已了结（补发成功，或重试仍NotFound——上下文确不在，无补偿
	 * 对象）；false=补发投递不确定/致命应答，调用方保留决策记录交redo。
	 */
	private boolean retryCancelNotFoundOnce(@NotNull String zezeName) {
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
			r.SendForWait(onzServer.getZezeInstance(zezeName), flushTimeout).get();
			var code = IModule.getErrorCode(r.getResultCode()); // 线上为moduleId组合值，解码后比较
			if (code == AbstractOnz.eSagaNotFound) {
				logger.warn("cancel saga retry still not found, give up. tid={}, zeze={}", onzTid, zezeName);
				return true;
			}
			if (code != 0) {
				logger.fatal("cancel saga retry error {}", code);
				return false;
			}
			return true; // code==0：乱序窗口内迟到注册的步骤已得到补偿。
		} catch (Exception ex) {
			logger.error("cancel saga retry fail. tid={}, zeze={}", onzTid, zezeName, ex);
			return false;
		}
	}

	// saga参与方持久化编码：BSavedCommits.Onzs是set[string]的集群名集合，bean为
	// 生成代码（不可加字段），以带前缀编码区分参与方类型——集群名不可能包含'='：
	// 非共享构造器按'='分隔解析（zeze.split("=")左段天然无'='），共享SM构造器对别名
	// 显式拒绝'='（onz-03），两处构造器一致维护本不变式。"saga="前缀与任何集群名
	//（以及旧版本持久化的ip_port）零碰撞；旧记录无前缀即procedure参与方，格式向后兼容。
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
		// 按集群名持久化：地址会漂移（重连/SM通告变更），redo时由
		// getZezeInstance现查当前地址——旧地址不作为幻影参与方被反复重试。
		for (var e : zezeProcedures.keySet()) {
			bState.getOnzs().add(e);
		}
		// saga参与方同样持久化：若只收集zezeProcedures，saga事务该集合恒空——
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
			// rollback全部投递了结才删记录：补偿投递不确定（发送失败/超时）或致命应答时
			// 保留ePreparing记录交redo补发收敛（对齐stopped拒删保记录的语义）——删了则未确认
			// 的Rollback/FuncSagaEnd(cancel)失去唯一自动补发通道（saga参与方只能等TTL丢弃，
			// 已提交步骤的补偿永久丢失）；单故障（仅持久化失败、rollback全成）时删记录避免redo空转。
			if (rollback())
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
				// onz-01（FND25裁定：协调者侧分歧信号补强先行，死线传播留评估）：参与方应答
				// eDivergence=已超时自愈回滚的参与方收到迟到Commit=部分提交分歧（协调者提交了
				// 已回滚的参与方；哨兵由ProcessCommitRequest一次性消耗，本应答不会重复出现）。
				// 这是"无故障假成功"的唯一协调者侧可见信号，error带tid/参与方暴露后按成功收场：
				// 不置commitFail（记录照删）、不重发——重发只会命中null幂等路径应答0，无法翻正
				// 已回滚的参与方，保留记录空转一轮毫无收益且与"应答0"语义等价（防循环）。
				// 死线传播（Commit下发前对最早ready参与方剩余预算的强制检查/心跳续期）留后续设计。
				if (IModule.getErrorCode(r.getResultCode()) == AbstractOnz.eDivergence) {
					logger.error("onz divergence: 已超时回滚的参与方收到迟到决策=部分提交分歧"
							+ " (participant rolled back before Commit arrived while coordinator committed;"
							+ " data divergence). tid={}, zeze={}. 收场不变：不置commitFail、记录照删、不重发.",
							onzTid, zeze);
					continue; // 继续通知其余参与方（同下方fatal的既定形态）
				}
				// 参与方未决（ready条目还在），保留索引重发是唯一收敛路径。
				commitFail = true;
				logger.fatal("commit error {}", IModule.getErrorCode(r.getResultCode()));
			}
		}

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		// saga同样已过决策点（成功步骤已提交）：endSaga失败不能转rollback，
		// 否则cancelSaga会把已成功的步骤补偿掉。滞留的saga上下文由参与方超时清理兜底（Onz.cleanupTimeoutSagas）。
		// onz-05（FND25裁定）：endSaga的eSagaNotFound应答置commitFail保留eCommitting记录交redo
		// ——此前NotFound被静默吞掉且记录照删，"发结果→本地落库"间隙宕机的丢写无任何对账通道。
		// redo按60s周期幂等重发end：上下文复活不可能但重发无副作用（在则end成功应答0），
		// NotFound由redo既有超龄分诊（SagaNotFoundAgedBudgetMs，OnzServer）终判——年轻保留重试、
		// 超龄error+保留交settleStuckRecord人工清算，复用rollback决策NotFound的同一条老化路径。
		// 发送/等待异常不置commitFail（end无数据效应，见endSaga javadoc）。
		try {
			if (!endSaga())
				commitFail = true;
		} catch (Exception ex) {
			logger.fatal("endSaga fail. tid={}", onzTid, ex);
		}

		if (!commitFail)
			onzServer.removeCommitRecord(tidBytes);
		// else: 保留eCommitting索引，redoTimer重发Commit，全部应答后由redo清理。
	}

	/** @return 决策是否对全部参与方投递了结（Rollback全部应答0 + cancelSaga全部了结）；
	 * false=存在投递不确定（发送失败/超时）或致命应答的参与方——commit失败路径据此保留
	 * ePreparing记录交redo补发。 */
	boolean rollback() {
		var allDelivered = true;
		// 对于saga，是空的。
		for (var zeze : zezeProcedures.keySet()) {
			var r = new Rollback();
			r.Argument.setOnzTid(onzTid);
			try {
				r.SendForWait(onzServer.getZezeInstance(zeze)).await();
				if (r.getResultCode() != 0) {
					// onz-01（FND25裁定）：与commit()同源的eDivergence特判——已超时自愈回滚的
					// 参与方收到迟到Rollback。Rollback决策下结局一致（两侧均已回滚，本参与方无数据
					// 分歧），但信号本身有效：协调者决策延迟超过了参与方决策等待预算（ONZ-F25-01
					// 根因），error带tid/参与方暴露预算违例后按投递了结收场（allDelivered不变、
					// 不保留记录重发——重发只会命中null幂等路径应答0，防循环；对齐commit()特判）。
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
			} catch (Exception ex) { // 逐参与方捕获（同commit()）。
				// rollback()运行在OnzServer.perform的rc!=0路径或catch块内：异常外传会被
				// perform的catch二次rollback从头重试，再抛则替换原始错误（业务rc丢失，最终
				// 只报Procedure.Exception）；且第一个参与方失败即中断循环，后续参与方收不到
				// Rollback，只能等参与方ready等待超时自愈与redoTimer的老化回滚
				// 兜底，不一致窗口被拉长。记fatal后继续，保证全部参与方都收到Rollback。
				allDelivered = false;
				logger.fatal("rollback send/await fail. tid={}, zeze={}", onzTid, zeze, ex);
			}
		}

		// 对于procedure，下面函数里面访问的zezeSagas是空的。
		if (!cancelSaga())
			allDelivered = false;
		return allDelivered;
	}

	void waitFlushDone() {
		if (flushMode != Onz.eFlushImmediately || zezeProcedures.isEmpty()) {
			// saga事务（或eFlushAsync）不计数等待：参与方首次flush早于FuncSagaEnd（setEnd），
			// 按设计不发FlushReady，计数永不满足，等待只会固定挂满flushTimeout再降级。
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
			//  2. 需要完整Checkpoint的zeze不持久化，以后也不持续触发。
			//  3. 这里等待触发结果返回。
			// 决策点(commit已持久化)之后的异常不外传（对齐commit()的同类原则）：
			// 单个checkpoint失败仅记fatal，该参与方由redoTimer的Commit重发兜底，
			// 异常逃逸会让OnzServer.perform的catch执行rollback()并向调用方返回失败
				// ——已实际提交的事务报告假阴性，调用方重发导致业务重复执行。
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
	 * 任何状态不被应答的ready都会演变成参与方事务失败halt。 */
	private static void replyReady(Rpc<?, ?> ready) {
		if (!ready.isSendResultDone()) // 这里忽略重复发送警告。
			ready.SendResult();
	}

	private static void checkpoint(AsyncSocket zeze) {
		var r = new Checkpoint();
		r.SendForWait(zeze).await();
	}

	private long onzTid;
	// 全量ready登记（按rpc对象）：开闸/降级时逐条应答用，只增不清（每条ready背后是一个
	// 等待应答的参与方事务）。计数判据不在本集合上（见distinctParticipants）。
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	// 完成判据按参与方身份去重计数——flush失败重试每次new FlushReady
	// 发出新的rpc对象，按rpc对象计数会被重试虚增（闸门可提前打开且无任何日志）。
	// distinctParticipants=已确认的不同参与方集合（key=FlushReady.Participant，参与方集群身份）；
	// legacyReadies=空Participant（旧版本参与方）按rpc对象身份兜底计数的兼容集合。
	private final Set<String> distinctParticipants = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashSet<Rpc<?, ?>> legacyReadies = new ConcurrentHashSet<>();
	private final TaskCompletionSource<Integer> flushDone = new TaskCompletionSource<>();
	// true之后到达的FlushReady一律立即应答（不计数门控）：等待收齐、降级、或免等（saga/eFlushAsync）。
	private volatile boolean flushGateOpen;

	// 以下两个集合在一个事务内只能启用一个。即不能混用FuncProcedure和FuncSaga
	// 去重键=集群名：socket实例在重连后变化——以实例为键使"每集群最多一个调用"
	// 在事务内重连窗口失效，且新旧两个地址都被持久化为参与方，redo对死地址永不收敛。
	private final ConcurrentHashMap<String, TaskCompletionSource<?>> zezeProcedures = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, TaskCompletionSource<?>> zezeSagas = new ConcurrentHashMap<>();

	public long getOnzTid() {
		return onzTid;
	}

	void trySetFlushReady(FlushReady r) {
		logger.debug("FlushReady sender={} argument={}", r.Argument, r.getSender());

		// saga事务不计数等待：重试flush的ready到达时协调者已越过等待点，立即应答。
		if (flushGateOpen || !zezeSagas.isEmpty()) {
			replyReady(r);
			return;
		}

		flushReadies.add(r);
		// 按参与方身份去重计数。
		var participant = r.Argument.getParticipant();
		if (participant.isEmpty()) {
			// 兼容窗口：旧版本参与方的协议没有Participant（decode缺省空串，异常路径同理），
			// 无法按身份去重，按rpc对象身份兜底计数——旧版本重发仍可能虚增
			// 计数提前开闸。每事务warn一次暴露兼容窗口的存在，升级完成即消失。
			if (legacyReadies.isEmpty())
				logger.warn("FlushReady without Participant (old client?), count by rpc identity. tid={}", onzTid);
			legacyReadies.add(r);
		} else if (!distinctParticipants.add(participant)) {
			// 同一参与方第二条ready：flush失败重试的正确性机制，不计数，warn暴露重发。
			logger.warn("duplicate FlushReady from same participant (flush retry?). tid={}, participant={}",
					onzTid, participant);
		}
		// 完成判据=不同参与方计数==procedure参与方数（zezeProcedures的key就是集群名）。
		// ready最早在Commit决策之后才可能到达，那时zezeProcedures已定型（commit()在
		// waitPendingAsync之后），N是稳定值；>=防并发add后单次检查跳过N（错过开闸只能等
		// flushTimeout降级，方向安全但无谓）。
		if (distinctParticipants.size() + legacyReadies.size() >= zezeProcedures.size()) {
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
