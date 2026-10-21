package Zeze.Onz;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import Zeze.Application;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/** Onz分布式事务参与方，管理本地procedure、saga及其提交与补偿。 */
public class Onz extends AbstractOnz {
	public static final String eServiceName = "Onz";

	private static final Logger logger = LogManager.getLogger(Onz.class);

	private final ConcurrentHashMap<String, OnzProcedureStub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzProcedure> readyProcedures = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzSaga> sagas = new LongConcurrentHashMap<>();
	private final @Nullable OnzService service;
	private final Application zeze;
	// FlushReady去重身份：projectName#serverId，重试时不变，无需与协调者配置别名一致。
	private final String participantName;
	// 按最后活动时间回收saga上下文；TTL须覆盖协调者崩溃检测、重启及redo预算。
	// 超期后补偿上下文丢失，迟到的FuncSagaEnd只能得到eSagaNotFound。
	public static final long eDefaultSagaContextTimeoutMs = 3600_000;
	// 等锁预算与任务看门狗对齐，避免挂死业务持续占用派发线程。
	private static final long SAGA_END_LOCK_WAIT_MS = 120_000;
	private long sagaContextTimeoutMs = eDefaultSagaContextTimeoutMs;
	private @Nullable Future<?> sagaCleanupTimer;
	// 回滚时间戳仅用于TTL清理；实际状态由readyProcedures中的哨兵表示。
	// 哨兵替换与Commit/Rollback取走条目在同一map内原子互斥。
	private static final long TimeoutRolledBackTtlMs = 3600_000;
	private final LongConcurrentHashMap<Long> timeoutRolledBack = new LongConcurrentHashMap<>();
	static final OnzProcedure TimeoutRolledBackMarker = new OnzProcedure(null, null, null, null, null);
	// saga在结果发送后本地回滚的标记，由FuncSagaEnd消费；时间戳用于TTL清理。
	private static final long SagaRolledBackAfterReadyTtlMs = 3600_000;
	private final LongConcurrentHashMap<Long> sagaRolledBackAfterReady = new LongConcurrentHashMap<>();

	// 全量检查点同步占用派发线程，超过1秒告警以暴露决策等待的预算竞争。
	private static final long CheckpointRunSlowWarnMs = 1_000;

	public long getSagaContextTimeoutMs() {
		return sagaContextTimeoutMs;
	}

	/** 超时必须为正，避免立即清理所有空闲补偿上下文。 */
	public void setSagaContextTimeoutMs(long sagaContextTimeoutMs) {
		if (sagaContextTimeoutMs <= 0)
			throw new IllegalArgumentException("sagaContextTimeoutMs <= 0: " + sagaContextTimeoutMs);
		this.sagaContextTimeoutMs = sagaContextTimeoutMs;
	}

	void markReadyProcedure(OnzProcedure procedure) {
		if (null != readyProcedures.putIfAbsent(procedure.getOnzTid(), procedure))
			throw new RuntimeException("ready procedure exist. " + procedure.getOnzTid());
	}

	/** 原子替换自己的ready条目为回滚哨兵；失败时调用方必须等待既成决策。 */
	boolean markTimeoutRolledBack(OnzProcedure procedure) {
		var tid = procedure.getOnzTid();
		if (!readyProcedures.replace(tid, procedure, TimeoutRolledBackMarker))
			return false;
		timeoutRolledBack.put(tid, System.currentTimeMillis());
		return true;
	}

	/**
	 * ready后本地回滚：procedure回填哨兵以识别迟到的Commit；
	 * saga单独记账，由FuncSagaEnd清理上下文并返回eSagaNotFound。
	 */
	boolean markRolledBackAfterReady(OnzProcedure procedure) {
		var tid = procedure.getOnzTid();
		if (procedure instanceof OnzSaga)
			return null == sagaRolledBackAfterReady.putIfAbsent(tid, System.currentTimeMillis());
		if (readyProcedures.putIfAbsent(tid, TimeoutRolledBackMarker) != null)
			return false;
		timeoutRolledBack.put(tid, System.currentTimeMillis());
		return true;
	}

	public Application getZeze() {
		return zeze;
	}

	public String getParticipantName() {
		return participantName;
	}

	/**
	 * 原连接断开时按对端IP寻找存活连接，无可用连接则返回null供调用方重试。
	 * 同IP多协调者可能错投；未知tid会被幂等放行，符合flush超时降级语义。
	 */
	@Nullable AsyncSocket resolveFlushSocket(@Nullable AsyncSocket sender) {
		if (null != sender && !sender.isClosed())
			return sender;
		var remote = null != sender ? sender.getRemoteInet() : null;
		var remoteIp = null != remote ? remote.getAddress() : null;
		if (null != remoteIp && null != service) {
			for (var so : service.establishedSockets()) {
				if (so.isClosed())
					continue;
				var inet = so.getRemoteInet();
				if (null != inet && remoteIp.equals(inet.getAddress())) {
					logger.warn("onz flush reroute: coordinator socket closed, FlushReady over live socket."
						+ " participant={} oldSocket={} newSocket={}", participantName, sender, so);
					return so;
				}
			}
		}
		return null;
	}

	// 共享SM时须使用集群唯一名，与协调者specialZezeNames一致；独立SM默认使用Onz。
	private String registerServiceName = eServiceName;

	public String getRegisterServiceName() {
		return registerServiceName;
	}

	/** 必须在 {@link #start()} 注册服务之前设置。 */
	public void setRegisterServiceName(String registerServiceName) {
		this.registerServiceName = Objects.requireNonNull(registerServiceName, "registerServiceName");
	}

	public static class OnzService extends Service {
		public static final String eName = "Zeze.Onz.Server";

		public OnzService(Application zeze) {
			super(eName, zeze);
		}

		/** 返回已建立连接；调用方须过滤尚未摘除的已关闭连接。 */
		Iterable<AsyncSocket> establishedSockets() {
			return socketMap;
		}
	}

	public Onz(Application zeze) {
		this.zeze = zeze;
		this.participantName = zeze.getProjectName() + "#" + zeze.getConfig().getServerId();
		var config = zeze.getConfig();
		if (null != config.getServiceConf(OnzService.eName)) {
			service = new OnzService(zeze);
			RegisterProtocols(service);
		} else {
			service = null;
		}
	}

	public void start() throws Exception {
		if (null != service) {
			service.start();
			var kv = service.getOneAcceptorAddress();
			var ip = kv.getKey();
			var port = kv.getValue();
			var zeze = service.getZeze();
			//noinspection DataFlowIssue
			var config = zeze.getConfig();
			var identity = String.valueOf(config.getServerId());
			Objects.requireNonNull(zeze.getServiceManager()).registerService(new BServiceInfo(registerServiceName, identity, 0, ip, port));
		}
		sagaCleanupTimer = TaskSpec.ofAction(this::cleanupTimeoutSagas).schedulePeriodNow(60_000, 60_000);
	}

	public void stop() throws Exception {
		if (null != sagaCleanupTimer) {
			sagaCleanupTimer.cancel(false);
			sagaCleanupTimer = null;
		}
		// 未决saga上下文不持久化，停机记录对账信息；补偿由协调者决策。
		for (var saga : sagas) {
			if (!saga.isEnd())
				logger.warn("onz stop: 未决saga上下文随进程丢失（重启后不恢复，协调者的cancel/redo将得"
						+ " eSagaNotFound）. tid={}, name={}, idle={}ms",
					saga.getOnzTid(), saga.getName(),
					System.currentTimeMillis() - saga.getLastActiveTime());
		}
		if (null != service)
			service.stop();
	}

	/** 按最后活动时间清理超时saga，跳过业务或补偿在途的条目。 */
	public void cleanupTimeoutSagas() {
		var now = System.currentTimeMillis();
		for (var saga : sagas) {
			if (!saga.isEnd() && now - saga.getLastActiveTime() >= sagaContextTimeoutMs) {
				// 业务或补偿在途时跳过，避免清理仍需使用的上下文。
				if (!saga.tryLockBusiness())
					continue;
				try {
					// 加锁后复查，避免清理刚完成的上下文。
					if (!saga.isEnd() && sagas.remove(saga.getOnzTid(), saga))
						logger.warn("cleanup timeout saga context. tid={}, name={}", saga.getOnzTid(), saga.getName());
				} finally {
					saga.unlockBusiness();
				}
			}
		}
		for (var it = timeoutRolledBack.keyIterator(); it.hasNext(); ) {
			var tid = it.next();
			var stamp = timeoutRolledBack.get(tid);
			if (stamp != null && now - stamp >= TimeoutRolledBackTtlMs) {
				// 哨兵过期后无法区分重复Commit与迟到决策，清理前告警留痕。
				logger.warn("expire timeout-rolled-back sentinel. tid={} age={}ms"
					+ " (recovery budget exceeded; Commit/Rollback arriving later resolves"
					+ " to the idempotent unknown-tid path without divergence signal)", tid, now - stamp);
				timeoutRolledBack.remove(tid);
				readyProcedures.remove(tid, TimeoutRolledBackMarker); // 连同槽位哨兵一起过期
			}
		}
		// 清理前告警，记录超出恢复预算的saga回滚标记。
		for (var it = sagaRolledBackAfterReady.keyIterator(); it.hasNext(); ) {
			var tid = it.next();
			var stamp = sagaRolledBackAfterReady.get(tid);
			if (stamp != null && now - stamp >= SagaRolledBackAfterReadyTtlMs) {
				logger.warn("expire rolled-back-after-ready saga marker. tid={} age={}ms"
					+ " (recovery budget exceeded; later FuncSagaEnd resolves to eSagaNotFound"
					+ " path without divergence signal unless context also expired)", tid, now - stamp);
				sagaRolledBackAfterReady.remove(tid);
			}
		}
	}

	public <A extends Bean, R extends Bean> void register(
		String name, OnzFuncProcedure<A, R> func,
		Class<A> argumentClass, Class<R> resultClass) {

		if (null != procedureStubs.putIfAbsent(name,
			new OnzProcedureStub<>(this, name, func, argumentClass, resultClass)))
			throw new RuntimeException("duplicate Onz Procedure Name=" + name);
	}

	/**
	 * 注册saga业务及补偿函数。协调者不传补偿载荷，funcCancel收到cancelClass的默认实例。
	 * 补偿所需数据须从OnzSaga上下文或业务表获取；argument/result可能已被业务改写。
	 */
	public <A extends Bean, R extends Bean, T extends Bean> void registerSaga(
		String name, OnzFuncSaga<A, R> func, OnzFuncSagaEnd<T> funcCancel,
		Class<A> argumentClass, Class<R> resultClass, Class<T> cancelClass) {

		if (null != procedureStubs.putIfAbsent(name,
			new OnzSagaStub<>(this, name, func, argumentClass, resultClass, funcCancel, cancelClass)))
			throw new RuntimeException("duplicate Onz Procedure Name=" + name);
	}

	@Override
	protected long ProcessCheckpointRequest(Checkpoint r) {
		var begin = System.currentTimeMillis();
		//noinspection DataFlowIssue
		service.getZeze().checkpointRun();
		var elapsed = System.currentTimeMillis() - begin;
		if (elapsed >= CheckpointRunSlowWarnMs)
			logger.warn("onz checkpointRun slow: {}ms (dispatch worker occupied;"
				+ " delays FlushReady/FuncSagaEnd/FuncProcedure handling and competes"
				+ " with decision-wait budgets, ONZ-F25-07)", elapsed);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCommitRequest(Commit r) {
		var tid = r.Argument.getOnzTid();
		var procedure = readyProcedures.remove(tid);
		if (null != procedure) {
			if (procedure == TimeoutRolledBackMarker) {
				// 本地已回滚而协调者要求提交，返回eDivergence暴露分歧；哨兵只消费一次。
				logger.error("Commit for timeout-rolled-back onz tid={}"
					+ " (participant rolled back before decision arrived; coordinator committed)"
					+ " -- data divergence exposed.", tid);
				timeoutRolledBack.remove(tid); // 记账清理（漏删亦由TTL回收）
				return errorCode(eDivergence); // 框架对非0返回值回发结果码（对齐eSagaNotFound路径）
			} else
				procedure.commit();
		} else {
			// 无法区分重复提交与状态丢失，保留幂等应答并告警。
			logger.warn("Commit for unknown onz tid={}"
				+ " (duplicate after commit -- normal idempotent redo; or participant state lost"
				+ " / timeout-rolled-back sentinel expired -- possible data divergence)", tid);
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessRollbackRequest(Rollback r) {
		var tid = r.Argument.getOnzTid();
		var procedure = readyProcedures.remove(tid);
		if (null != procedure) {
			if (procedure == TimeoutRolledBackMarker) {
				// 本地与协调者均回滚；eDivergence仅报告决策超出等待预算。
				timeoutRolledBack.remove(tid);
				return errorCode(eDivergence); // 框架对非0返回值回发结果码（对齐eSagaNotFound路径）
			} else
				procedure.rollback();
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) throws Exception {
		var stub = procedureStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);
		// saga必须经FuncSaga调用，否则会绕过两阶段决策且没有补偿上下文。
		if (stub instanceof OnzSagaStub) {
			logger.error("onz funcProcedure type mismatch: name='{}' registered as saga, use callSagaAsync. tid={}",
				r.Argument.getFuncName(), r.Argument.getOnzTid());
			return errorCode(eProcedureNotFound);
		}
		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);
		return Objects.requireNonNull(TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call());
	}

	@Override
	protected long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception {
		var stub = procedureStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);
		if (!(stub instanceof OnzSagaStub)) {
			logger.error("onz funcSaga type mismatch: name='{}' registered as procedure, use callProcedureAsync. tid={}",
				r.Argument.getFuncName(), r.Argument.getOnzTid());
			return errorCode(eProcedureNotFound);
		}

		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);

		// 先锁定再注册，防止FuncSagaEnd抢先补偿尚未执行的业务。
		// 注册前到达的cancel由协调者延迟重试兜底。
		var rc = Procedure.Exception;
		((OnzSaga)procedure).lockBusiness();
		try {
			if (null != sagas.putIfAbsent(r.Argument.getOnzTid(), (OnzSaga)procedure))
				return errorCode(eSagaTidExist);
			rc = Objects.requireNonNull(TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call());
		} finally {
			// 失败条目必须在解锁前移除，避免等待中的cancel补偿已回滚业务。
			if (rc != 0)
				sagas.remove(r.Argument.getOnzTid(), procedure); // 两参remove防御tid条目被替换
			((OnzSaga)procedure).unlockBusiness();
		}
		return rc;
	}

	@Override
	protected long ProcessFuncSagaEndRequest(Zeze.Builtin.Onz.FuncSagaEnd r) throws Exception {
		var tid = r.Argument.getOnzTid();
		// 结果已发但本地回滚：清理上下文并报告缺失，避免补偿未落库的写入。
		if (null != sagaRolledBackAfterReady.remove(tid)) {
			var contextRemoved = null != sagas.remove(tid);
			logger.error("FuncSagaEnd for rolled-back-after-ready saga tid={}"
				+ " (result sent but local transaction rolled back while stopping -- write never committed;"
				+ " coordinator treats step as succeeded, divergence, manual check required)."
				+ " cancel={} contextRemoved={}", tid, r.Argument.isCancel(), contextRemoved);
			return errorCode(eSagaNotFound);
		}
		var context = sagas.get(tid);
		if (context == null)
			return errorCode(eSagaNotFound);

		// 等业务完成再决策；超时或中断时不应答，释放派发线程并由协调者redo重试。
		var locked = false;
		try {
			locked = context.tryLockBusiness(SAGA_END_LOCK_WAIT_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (!locked) {
			logger.warn("FuncSagaEnd wait businessLock timeout, give up this round (redo will resend)."
				+ " tid={} cancel={} sagaContextTimeoutMs兜底仍在", tid, r.Argument.isCancel());
			return 0; // 不应答：等价rpc超时，协调者侧按投递失败保留决策记录
		}
		try {
			if (r.Argument.isCancel()) {
				// 补偿期间保持条目在场并持锁，失败留待redo，成功后才移除。
				final var stub = (OnzSagaStub<?, ?, ?>)context.getStub();
				final var cancelArgument = Objects.requireNonNull(stub).decodeCancelArgument(r.Argument.getFuncArgument());
				// 等锁期间业务可能已失败并移除条目，此时不能再补偿。
				if (sagas.get(tid) != context)
					return errorCode(eSagaNotFound);
				var rc = TaskSpec.ofProcedure(zeze.newProcedure(() -> stub.end(context, cancelArgument), context.getName())).call();
				if (Objects.requireNonNull(rc) != 0) {
					// 补偿失败刷新活动时间，保留上下文等待redo。
					context.refreshLastActive();
					// 统一协议错误码，避免业务rc被误解为eSagaNotFound而停止重试。
					logger.warn("saga compensate fail, keep context for resend. tid={} userRc={}", tid, rc);
					return errorCode(eCompensateFail);
				}
			}
			if (!sagas.remove(tid, context))
				return errorCode(eSagaNotFound);
			context.setEnd();
		} finally {
			context.unlockBusiness();
		}

		r.SendResult();
		return 0;
	}
}
