package Zeze.Onz;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Onz分布式事务参与方：嵌入zeze应用，注册本地procedure/saga，处理两阶段提交的ready/Commit/Rollback与flush应答，并管理saga上下文生命周期。 */
public class Onz extends AbstractOnz {
	public static final String eServiceName = "Onz";

	private static final Logger logger = LogManager.getLogger(Onz.class);

	private final ConcurrentHashMap<String, OnzProcedureStub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzProcedure> readyProcedures = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzSaga> sagas = new LongConcurrentHashMap<>();
	private final OnzService service;
	private final Application zeze;
	// 本集群的Onz参与方身份（FlushReady.Participant，协调者按它去重计数）。
	// 组成=projectName#serverId（对齐raft版SM会话名的缺省全局唯一约定，见
	// Config.ServiceManagerConf注释）。协调者按参与方去重只需要"不同集群不同名、
	// 同集群重试同名"，本身份与协调者在zezeConfigs里为本集群起的别名（zezeProcedures
	// 的键，buildSavedCommits/decodeSagaParticipant持久化的那个名字，非共享SM模式下
	// 参与方无从得知）是否一致不影响计数正确性：别名不参与去重键。
	private final String participantName;
	// saga上下文兜底清理：正常流程FuncSagaEnd在步骤成功后数秒内到达；发送失败或
	// 协调者崩溃由redo重发（saga参与方进持久化快照）——TTL清理是资源回收
	// 兜底（防rpc/bean泄漏），不是正确性机制：正确性依赖redo在本TTL内到达。
	// 联动契约：本值必须 ≥ 协调者最大恢复预算 = 崩溃检测+重启+
	// RedoPreparingMinAgeMs(120s，OnzServer)+redo周期(60s，OnzServer)。预算内恢复
	// 则redo补发的FuncSagaEnd(cancel)必命中存活上下文；超预算恢复命中已清上下文
	// =eSagaNotFound，补偿丢失（超龄者由OnzServer.redo分诊error，人工对账）。
	// 计时基准为最后活动时间（构造/补偿失败放回时刻，见OnzSaga.lastActiveTime）。
	public static final long eDefaultSagaContextTimeoutMs = 3600_000;
	private long sagaContextTimeoutMs = eDefaultSagaContextTimeoutMs;
	private Future<?> sagaCleanupTimer;
	// ready等待超时自愈回滚的tid记账（值=回滚时刻，有界），仅供
	// 过期清理循迹；决定性状态在readyProcedures的槽位哨兵（TimeoutRolledBackMarker）——
	//「取走登记」（Commit/Rollback的remove）与「自愈标记」（replace CAS置哨兵）在同一
	// map的CAS原子域内互斥可见，消除remove→mark两语句间隙被并发Commit穿过（两表皆null
	// 按已提交幂等重发假应答成功，分歧暴露日志漏报）的窗口；先mark后remove则产生假阳性。
	// 条目随saga清理周期过期（过期时连同槽位哨兵一起回收）。
	private static final long TimeoutRolledBackTtlMs = 3600_000;
	private final LongConcurrentHashMap<Long> timeoutRolledBack = new LongConcurrentHashMap<>();
	static final OnzProcedure TimeoutRolledBackMarker = new OnzProcedure(null, null, null, null, null);

	public long getSagaContextTimeoutMs() {
		return sagaContextTimeoutMs;
	}

	/** sagaContextTimeoutMs≤0会使cleanupTimeoutSagas对所有空闲上下文立即清理
	 * （补偿链断裂，sagaContextTimeoutMs字段注释的联动契约失效），构造期拒绝（onz-06）。 */
	public void setSagaContextTimeoutMs(long sagaContextTimeoutMs) {
		if (sagaContextTimeoutMs <= 0)
			throw new IllegalArgumentException("sagaContextTimeoutMs <= 0: " + sagaContextTimeoutMs);
		this.sagaContextTimeoutMs = sagaContextTimeoutMs;
	}

	void markReadyProcedure(OnzProcedure procedure) {
		if (null != readyProcedures.putIfAbsent(procedure.getOnzTid(), procedure))
			throw new RuntimeException("ready procedure exist. " + procedure.getOnzTid());
	}

	/** 参与方ready等待超时自愈——CAS把槽位原子置换为超时哨兵。
	 * 仅当条目仍是自己时成功：迟到的Commit/Rollback可能已并发取走，由调用方等待
	 * 既成决策（不得覆盖）。成功即已标记，另记时间戳供TTL清理。 */
	boolean markTimeoutRolledBack(OnzProcedure procedure) {
		var tid = procedure.getOnzTid();
		if (!readyProcedures.replace(tid, procedure, TimeoutRolledBackMarker))
			return false;
		timeoutRolledBack.put(tid, System.currentTimeMillis());
		return true;
	}

	/**
	 * 参与方"ready已发、Commit决策已送达"后本地失败回滚（如perform的停机拒绝）——
	 * putIfAbsent回填超时哨兵：决策RPC已先行取走条目（replace必失败，故用putIfAbsent），
	 * 回填成功则迟到的redo Commit取到哨兵走ProcessCommitRequest的分歧error路径二次确认；
	 * 条目仍在（决策未到达/已是哨兵）时失败不覆盖。成功后与markTimeoutRolledBack同型记账，
	 * 由TTL连同槽位哨兵一起回收。
	 * 仅procedure参与方：saga的上下文在sagas表、从不进readyProcedures（决策走FuncSagaEnd
	 * 不走Commit），回填只会放幽灵哨兵（同tid意外收到Commit时误报分歧）；saga的停机分歧
	 * 由Transaction.perform的error日志暴露。
	 */
	boolean markRolledBackAfterReady(OnzProcedure procedure) {
		if (procedure instanceof OnzSaga)
			return false;
		var tid = procedure.getOnzTid();
		if (readyProcedures.putIfAbsent(tid, TimeoutRolledBackMarker) != null)
			return false;
		timeoutRolledBack.put(tid, System.currentTimeMillis());
		return true;
	}

	public Application getZeze() {
		return zeze;
	}

	/** FlushReady.Participant 的取值来源（构造期固定，见participantName注释）。 */
	public String getParticipantName() {
		return participantName;
	}

	// 共享SM部署（OnzServer三参构造器）下本集群Onz服务的SM注册名：共享SM里
	// 多个集群若都按缺省"Onz"注册，同名条目混在一张通告表里，协调者无从按集群路由——共享模式
	// 要求各集群配置互异唯一名（与协调者specialZezeNames逐名对齐，协调者按名订阅+按名查询）。
	// 非共享模式（每集群独立SM）保持缺省"Onz"不动：协调者按"Onz"订阅，既有用法零变化。
	private String registerServiceName = eServiceName;

	/** SM注册服务名（缺省"Onz"；共享SM部署改为集群唯一名，见registerServiceName注释）。 */
	public String getRegisterServiceName() {
		return registerServiceName;
	}

	/** 必须在 {@link #start()} 之前调用：start()把该名注册进ServiceManager，之后不可改。 */
	public void setRegisterServiceName(String registerServiceName) {
		this.registerServiceName = Objects.requireNonNull(registerServiceName, "registerServiceName");
	}

	public static class OnzService extends Service {
		public static final String eName = "Zeze.Onz.Server";

		public OnzService(Application zeze) {
			super(eName, zeze);
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
			var config = zeze.getConfig();
			var identity = String.valueOf(config.getServerId());
			// 注册名可配：共享SM部署下各集群以唯一名注册（协调者按名订阅路由），
			// 非共享缺省"Onz"（协调者按"Onz"订阅，见registerServiceName注释）。
			zeze.getServiceManager().registerService(new BServiceInfo(registerServiceName, identity, 0, ip, port));
		}
		sagaCleanupTimer = TaskSpec.ofAction(this::cleanupTimeoutSagas).schedulePeriodNow(60_000, 60_000);
	}

	public void stop() throws Exception {
		if (null != sagaCleanupTimer) {
			sagaCleanupTimer.cancel(false);
			sagaCleanupTimer = null;
		}
		if (null != service)
			service.stop();
	}

	/**
	 * 清理超时仍未收到FuncSagaEnd的saga上下文，超时按最后活动时间计
	 * （构造/补偿失败放回时刻）。定时器周期调用，测试可直接调用。
	 * 业务在途（执行中/FuncSagaEnd补偿中等锁）的条目跳过本轮，等业务完成
	 * 后的下个周期再清。按最后活动计时保证补偿重试链不被构造时刻的TTL掐断（完整
	 * 因果链见sagaContextTimeoutMs契约）；超龄的NotFound由OnzServer.redo分诊error。
	 */
	public void cleanupTimeoutSagas() {
		var now = System.currentTimeMillis();
		for (var saga : sagas) {
			if (!saga.isEnd() && now - saga.getLastActiveTime() >= sagaContextTimeoutMs) {
				// 正常流程FuncSagaEnd在步骤成功后数秒内到达；协调者崩溃后重启，redo会补发
				// FuncSagaEnd（saga参与方进buildSavedCommits持久化快照）。但补发收敛
				// 以sagaContextTimeoutMs为预算：协调者超预算恢复时上下文
				// 已被本清理回收，redo只得超龄NotFound（OnzServer.redo分诊error，人工对账）。
				// 滞留条目持有rpc（sender socket引用）与业务bean，且end=false会扭曲flush语义判断。
				// 先tryLock businessLock：拿不到=业务在途（含FuncSagaEnd正
				// 阻塞等慢业务），删条目会让等待方remove失败应答eSagaNotFound——
				// 补偿丢失。跳过本轮，等下个周期。
				if (!saga.tryLockBusiness())
					continue;
				try {
					// 拿到锁后复查isEnd：刚完成的FuncSagaEnd可能已置end并清理（竞争失败
					// 也是无害no-op，复查只为日志干净）。
					if (!saga.isEnd() && sagas.remove(saga.getOnzTid(), saga))
						logger.warn("cleanup timeout saga context. tid={}, name={}", saga.getOnzTid(), saga.getName());
				} finally {
					saga.unlockBusiness();
				}
			}
		}
		// 超时回滚登记过期：Rollback最迟在崩溃协调者重启+redo一轮内到达，
		// 1小时足够（对齐sagaContextTimeoutMs默认）。
		for (var it = timeoutRolledBack.keyIterator(); it.hasNext(); ) {
			var tid = it.next();
			var stamp = timeoutRolledBack.get(tid);
			if (stamp != null && now - stamp >= TimeoutRolledBackTtlMs) {
				// onz-04：摘哨兵=分歧信号消失点。此后迟到的Commit命中remove=null走幂等应答0
				//（与已提交的重复发送不可区分，见ProcessCommitRequest），协调者按提交收场而
				// 本地已回滚=静默分歧。warn对齐本方法saga上下文清理的"放弃对象"口径：预算契约
				//（协调者恢复≤TTL）被打破的最后留痕——saga路径同场景有协调者侧超龄NotFound
				// 分诊（OnzServer.SagaNotFoundAgedBudgetMs），本warn是其参与方侧对称信号。
				logger.warn("expire timeout-rolled-back sentinel. tid={} age={}ms"
						+ " (recovery budget exceeded; Commit/Rollback arriving later resolves"
						+ " to the idempotent unknown-tid path without divergence signal)", tid, now - stamp);
				timeoutRolledBack.remove(tid);
				readyProcedures.remove(tid, TimeoutRolledBackMarker); // 连同槽位哨兵一起过期
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

	public <A extends Bean, R extends Bean, T extends Bean> void registerSaga(
			String name, OnzFuncSaga<A, R> func, OnzFuncSagaEnd<T> funcCancel,
			Class<A> argumentClass, Class<R> resultClass, Class<T> cancelClass) {

		if (null != procedureStubs.putIfAbsent(name,
				new OnzSagaStub<>(this, name, func, argumentClass, resultClass, funcCancel, cancelClass)))
			throw new RuntimeException("duplicate Onz Procedure Name=" + name);
	}

	@Override
	protected long ProcessCheckpointRequest(Checkpoint r) {
		service.getZeze().checkpointRun();
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCommitRequest(Commit r) {
		var tid = r.Argument.getOnzTid();
		var procedure = readyProcedures.remove(tid);
		if (null != procedure) {
			if (procedure == TimeoutRolledBackMarker) {
				// 本参与方已超时自愈回滚（或ready后本地回滚回填哨兵），协调者却持久化了
				// commit决策——真实不一致（静默部分提交），error暴露。
				// onz-01（FND25裁定）：应答eDivergence取代此前的应答0——这是"无故障假成功"分歧
				// 的唯一协调者侧可见信号（此前仅本参与方一条error、无人关联；协调者按0收场删记录，
				// 调用方完全无感）。FND5-44"改错误码会让redo无限重发"的顾虑不适用于本形态：
				// 哨兵随本次remove一次性消耗，迟到的重发命中null走幂等应答0（与已提交的重复发送
				// 不可区分，见下方else分支）——"永无应答成功的可能"只存在于对null路径持续回错的
				// 形态；协调者侧对eDivergence特判按成功收场且不重发（OnzTransaction.commit），
				// 不成环。
				logger.error("Commit for timeout-rolled-back onz tid={}"
						+ " (participant rolled back before decision arrived; coordinator committed)"
						+ " -- data divergence exposed.", tid);
				timeoutRolledBack.remove(tid); // 记账清理（漏删亦由TTL回收）
				return errorCode(eDivergence); // 框架对非0返回值回发结果码（对齐eSagaNotFound路径）
			} else
				procedure.commit();
		} else {
			// onz-04：null两源不可区分——已提交的重复发送（redo重发/应答丢失，正常幂等），
			// 或哨兵TTL过期后迟到的Commit（本地已回滚、协调者按提交收场=静默分歧；参与方
			// 无决策时戳，无法像协调者对saga NotFound那样按记录年龄分诊）。warn带tid暴露
			// 信号（对齐saga路径超龄分诊的可观测口径）：重复发送的良性warn是该可观测性的
			// 既定代价，频度有界于redo轮次；应答语义不变。
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
			if (procedure == TimeoutRolledBackMarker)
				// 超时自愈后到达的Rollback：与本地已回滚一致，静默回收哨兵（无重复发送告警价值）。
				timeoutRolledBack.remove(tid);
			else
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
		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);
		return TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call();
	}

	@Override
	protected long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception {
		var stub = procedureStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);

		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);

		// 步骤失败（业务返回非0或异常）时本地事务已回滚：协调者cancelSaga只对成功的
		// 步骤发FuncSagaEnd（失败步骤被跳过），正常结束路径endSaga也只在成功时到达，
		// 这里不清理则条目永久滞留（持有rpc与业务bean）。
		// 业务执行期间持有businessLock，与并发的FuncSagaEnd(cancel/end)互斥——
		// 协调者超时补偿会在业务仍执行时到达，不互斥则补偿与业务并发/抢先，业务随后
		// 失败回滚时补偿就成了过补偿。
		// 注册必须在businessLock之内：putIfAbsent若先于lockBusiness，停滞
		// 窗口内并发的FuncSagaEnd(cancel)（同为Normal派发，不保证处理顺序）可抢先拿锁、
		// remove并补偿一个从未执行的业务，随后真实业务提交且无人补偿——双向分歧。
		// 先拿锁再注册后，等锁的FuncSagaEnd必然观察到已注册条目（锁的happens-before）。
		// 注册前FuncSagaEnd到达→eSagaNotFound的窗口由协调者既有
		// retryCancelNotFoundOnce兜底：此时协调者对本步骤的rpc必未完成（业务还没跑），
		// cancelSaga将其归入rpcFailed类单次延迟重试，重试时业务在途或已完成，补偿串行正确。
		var rc = Procedure.Exception;
		((OnzSaga)procedure).lockBusiness();
		try {
			// eSagaTidExist的提前返回处于try/finally解锁结构内（锁不泄漏）；此路径rc!=0，
			// finally的两参remove对本实例（不在map中）是no-op，不会误删已存在的条目。
			if (null != sagas.putIfAbsent(r.Argument.getOnzTid(), (OnzSaga)procedure))
				return errorCode(eSagaTidExist);
			rc = TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call();
		} finally {
			// 失败清理必须在businessLock之内、解锁之前：解锁与remove的间隙里，
			// 等锁的FuncSagaEnd(cancel)会抢先 acquire 并在sagas.remove(tid,context)成功后
			// 对已回滚（写从未发生）的业务执行补偿——过补偿（反向分歧）。锁内先remove再
			// unlock，等锁方醒来必然观察到条目已消失（锁的happens-before），应答eSagaNotFound。
			if (rc != 0)
				sagas.remove(r.Argument.getOnzTid(), procedure); // 两参remove防御tid条目被替换
			((OnzSaga)procedure).unlockBusiness();
		}
		return rc;
	}

	@Override
	protected long ProcessFuncSagaEndRequest(Zeze.Builtin.Onz.FuncSagaEnd r) throws Exception {
		var tid = r.Argument.getOnzTid();
		var context = sagas.get(tid);
		if (context == null)
			return errorCode(eSagaNotFound);

		// 等业务完成再决策（FuncSagaEnd可能在慢业务执行期间到达）。
		// 业务失败已在finally中自清理条目：锁到手后remove失败即eSagaNotFound，
		// 失败步骤不会被补偿（无过补偿）；业务成功则条目仍在，补偿/结束串行执行。
		context.lockBusiness();
		try {
			// 没有设置cancel标志时，表示事务正常结束，用来删除sagas上下文。
			if (r.Argument.isCancel()) {
				// 补偿参数decode必须先于sagas.remove——decode抛异常（载荷损坏截断/
				// cancelClass构造失败）若发生在remove之后，putIfAbsent回补被跳过：上下文
				// 已删除，补偿永久丢失（因果链见sagaContextTimeoutMs契约）。decode先行，失败时
				// 条目仍在（协调者/人工可重试），由cleanupTimeoutSagas（默认1小时）兜底。
				final var stub = (OnzSagaStub<?, ?, ?>)context.getStub();
				final var cancelArgument = stub.decodeCancelArgument(r.Argument.getFuncArgument());
				if (!sagas.remove(tid, context))
					return errorCode(eSagaNotFound);
				var rc = TaskSpec.ofProcedure(zeze.newProcedure(() -> stub.end(context, cancelArgument), context.getName())).call();
				if (rc != 0) {
					// 补偿失败：上下文必须放回sagas等重发——不放回则重发只得eSagaNotFound，
					// 补偿永久丢失（因果链见sagaContextTimeoutMs契约）。
					// 放回后由cleanupTimeoutSagas超时兜底（默认1小时，可配置）。
					if (null != sagas.putIfAbsent(tid, context))
						logger.error("saga context re-insert conflict. tid={}", tid);
					else
						// 放回即最后活动（计时基准见OnzSaga.lastActiveTime）：
						// 等待重发的窗口不消耗TTL预算。
						context.refreshLastActive();
					// 裸rc禁止上线：用户补偿结果码与协议错误码共用低32位命名空间（协调者统一
					// getErrorCode解码），rc恰为2时被误判eSagaNotFound而删决策记录——放回的
					// 补偿上下文永无重试。统一回eCompensateFail（协调者按未知非零码保留记录
					// 交redo重发），用户rc记录在此。
					logger.warn("saga compensate fail, keep context for resend. tid={} userRc={}", tid, rc);
					return errorCode(eCompensateFail);
				}
			} else {
				if (!sagas.remove(tid, context))
					return errorCode(eSagaNotFound);
			}
			context.setEnd();
		} finally {
			context.unlockBusiness();
		}

		r.SendResult();
		return 0;
	}
}
