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
	// FuncSagaEnd处理器对businessLock的有界等待预算（onz-02，FND26）：对齐Task看门狗的
	// 全额（Task.defaultTimeout=120s，只中断一次）——存活慢业务在其自然生命周期内完成，
	// 挂死业务等满后放弃本次处理交redo重发（详见ProcessFuncSagaEndRequest）。
	private static final long SAGA_END_LOCK_WAIT_MS = 120_000;
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
	// onz-B（FND28）：saga参与方"结果已发→本地RejectWhileStopping回滚"的tid记号（值=回滚时刻，
	// 仅供过期清理循迹）。saga的决策走FuncSagaEnd不走Commit/Rollback，不能回填readyProcedures
	// 哨兵（同tid永远不会收到Commit，只会放幽灵哨兵）——记号由ProcessFuncSagaEndRequest头部
	// 消费：命中即摘除上下文并应答eSagaNotFound，使协调者endSaga/redo的既有NotFound暴露链
	// （endSaga error+保留eCommitting交redo超龄分诊，onz-05口径）可见该分歧。TTL与
	// timeoutRolledBack一致（下方清理循环同点回收）。
	private static final long SagaRolledBackAfterReadyTtlMs = 3600_000;
	private final LongConcurrentHashMap<Long> sagaRolledBackAfterReady = new LongConcurrentHashMap<>();

	// onz-07（FND25裁定：最小侵入形态）：checkpointRun是全应用检查点（Application.checkpointRun
	// 直调checkpoint.runOnce），同步执行在本协议（TransactionLevel.None+DispatchMode.Normal）的
	// 共享派发worker内——协调者waitFlushDone降级路径对每个参与方串行发Checkpoint并各自await
	//（OnzTransaction.checkpoint，rpc默认超时5s），期间该worker被独占，并发的FlushReady/
	// FuncSagaEnd/FuncProcedure处理被推迟，与参与方决策等待预算（flushTimeout默认10s）和
	// FlushReady预算（2×flushTimeout，onz-02）竞争复合放大。"有界化（仅本事务相关）"不可行：
	// Checkpoint协议bean无参（生成物不可加字段）且zeze检查点机器是应用级队列，无按事务过滤
	// 入口；线程模型重排（转投检查点专用线程异步应答）留档不实施。最小正确动作=占用可观测：
	// 超过门槛记warn（零状态一次性），暴露worker被检查点独占的时长供容量核对（Onz参与方
	// worker池容量需覆盖在途2pc决策等待数+检查点串行段）。门槛1s：远低于Checkpoint rpc默认
	// 超时5s（超过即协调者侧fatal噪声+redo兜底）且比flushTimeout默认10s低一个量级——达到门槛
	// 即说明已侵入预算竞争区间，早暴露早扩容/错峰。
	private static final long CheckpointRunSlowWarnMs = 1_000;

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
	 * saga参与方（onz-B，FND28）走sagaRolledBackAfterReady记号而非readyProcedures哨兵：
	 * saga的上下文在sagas表、从不进readyProcedures（决策走FuncSagaEnd不走Commit），回填
	 * 只会放幽灵哨兵；记号由ProcessFuncSagaEndRequest头部消费（摘上下文+eSagaNotFound），
	 * 使协调者endSaga/redo的既有NotFound暴露链可见"结果已发而本地回滚"的分歧——此前该
	 * 形态仅参与方单侧error日志，协调者按提交收场零感知。
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

	/** FlushReady.Participant 的取值来源（构造期固定，见participantName注释）。 */
	public String getParticipantName() {
		return participantName;
	}

	/**
	 * onz-A（FND28）：FlushReady发送目标的断连感知重路由。
	 * 原sender是FuncProcedure/FuncSaga请求到达的连接（协调者出站连接在参与方侧的accept形态），
	 * 断开后原实现仍对死socket重发——Send恒false→flush失败→RelativeRecordSet失败保留→每轮
	 * checkpoint对同一死socket重发，永不收敛：已应答写入滞留内存直至停机丢失（eFlushAsync）；
	 * eFlushImmediately在业务线程同步失败则halt(543543)。协调者重启/闪断后会重建到本服务的
	 * 连接（其Connector自动重连/OnzServer.getZezeInstance现查新地址），但新连接只承载新请求——
	 * 这里按对端IP在本服务的存活连接中现查同一协调者的通道重路由（对齐"现查新地址、不缓存
	 * 死地址"的既有形态）。找不到时返回null，调用方按发送失败处理，留待下轮checkpoint重试；
	 * 协调者恢复对本集群的任何活动（新事务/redo/降级Checkpoint）都会带来新连接，重路由即收敛。
	 * 错投容忍：同IP多OnzServer进程时可能投给同IP的另一协调者——其OnzAgent按tid查无在途
	 * 事务则幂等放行应答（OnzAgent.ProcessFlushReadyRequest），参与方得以落库；flush闸门本为
	 * 尽力同时性（waitFlushDone超时降级随意放行），落库是数据保全方向。
	 */
	AsyncSocket resolveFlushSocket(AsyncSocket sender) {
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

		/** 当前全部已建立连接（含已关闭未摘除的条目，调用方自理isClosed）——
		 * onz-A的FlushReady重路由枚举用，包内可见。 */
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
		// onz-B记号过期（预算契约被打破的最后留痕，对齐上方哨兵过期口径）：过期后迟到的
		// FuncSagaEnd命中仍存活的上下文会走正常end/补偿（cancel对已回滚的写=过补偿）——但
		// saga上下文同以1h空闲TTL清理，超龄窗口内两者已被一并回收，迟到者命中null走
		// eSagaNotFound（协调者redo超龄分诊仍可见分歧）；warn是记号消失前的最后信号。
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
	 * 注册saga参与方（业务+补偿）。
	 *
	 * <p><b>补偿参数恒为空bean（onz-04，FND26契约明示）</b>：协调者的全部四个FuncSagaEnd生产点
	 * （endSaga/cancelSaga/retryCancelNotFoundOnce/sendRedoDecision）只设置OnzTid与Cancel标志，
	 * 从不设置FuncArgument——{@code funcCancel}收到的cancelArgument是cancelClass的默认构造
	 * 实例（空载荷），补偿函数只能从{@link OnzSaga}上下文自取数据（且业务可能已原地改写
	 * argument/result bean）。cancelClass仅声明补偿参数的类型面，不承载协调者侧传参语义；
	 * 协调者无从得知参与方补偿bean类型（发结果即本地提交的saga模型下无该注册面），属有意为之。
	 * 需要传参的补偿请把参数持久化进业务侧（saga result/自有表），补偿时读回。</p>
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
		// onz-07：全量检查点在派发worker内同步执行（预算推导与"为何只告警不改执行模型"见
		// CheckpointRunSlowWarnMs注释）——度量占用时长，超门槛warn暴露worker被独占。
		var begin = System.currentTimeMillis();
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
			if (procedure == TimeoutRolledBackMarker) {
				// 超时自愈后到达的Rollback：结局与本地已回滚一致（本参与方无数据分歧），静默回收哨兵。
				// onz-01（FND25裁定）：与Commit同应答eDivergence——信号语义是"协调者决策延迟超过了
				// 参与方决策等待预算"（ONZ-F25-01根因的协调者侧可见化）：对Commit决策=部分提交分歧，
				// 对Rollback决策=仅预算违例（结局一致）。协调者侧特判（OnzTransaction.rollback）按投递
				// 了结收场：allDelivered不变、不保留记录重发——重发只会命中null幂等路径，不成环。
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
		// 注册类型与调用协议错配门控（onz-03）：saga注册名被callProcedureAsync调用时，
		// OnzSagaStub.newProcedure返回的OnzSaga将以procedure语义执行——sendReadyAndWait被
		// 覆写为"发结果即本地提交"，不进readyProcedures、无两阶段决策、无补偿上下文
		//（sagas注册只在ProcessFuncSagaRequest路径）；失败路径的Rollback命中null条目假应答
		// 成功，协调者删记录而参与方写入已持久化=静默部分提交。显式回错让部署错配在调用期
		// 响亮失败，不依赖类型系统的偶然行为（错误码复用eProcedureNotFound：该名字对
		// procedure调用形态不存在；新增错误码须改生成物，不在手写域）。
		if (stub instanceof OnzSagaStub) {
			logger.error("onz funcProcedure type mismatch: name='{}' registered as saga, use callSagaAsync. tid={}",
					r.Argument.getFuncName(), r.Argument.getOnzTid());
			return errorCode(eProcedureNotFound);
		}
		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);
		return TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call();
	}

	@Override
	protected long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception {
		var stub = procedureStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);
		// 对称门控（onz-03）：procedure注册名被callSagaAsync调用——此前依赖下方(OnzSaga)强转的
		// CCE偶然响亮，显式回错对齐FuncProcedure方向的错误语义，部署错配可辨识定位。
		if (!(stub instanceof OnzSagaStub)) {
			logger.error("onz funcSaga type mismatch: name='{}' registered as procedure, use callProcedureAsync. tid={}",
					r.Argument.getFuncName(), r.Argument.getOnzTid());
			return errorCode(eProcedureNotFound);
		}

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
		// onz-B（FND28）：记号命中=结果已发（协调者已把本步骤计入成功链路）而本地因停机拒绝回滚，
		// 写入从未落库——无论end（cancel=false）还是补偿（cancel=true）都无对象：end只是摘上下文；
		// 补偿会逆转一个不存在的写（过补偿）。摘除上下文后按eSagaNotFound应答：协调者侧经
		// endSaga的NotFound error（commit路径保留eCommitting交redo）与redo超龄分诊的既有
		// "丢写嫌疑"暴露链（onz-05口径）可见分歧并人工对账。记号一次性消耗（remove），重复/
		// 并发的FuncSagaEnd随后命中无上下文的正常eSagaNotFound路径，应答语义一致。
		if (null != sagaRolledBackAfterReady.remove(tid)) {
			var contextRemoved = null != sagas.remove(tid);
			logger.error("FuncSagaEnd for rolled-back-after-ready saga tid={}"
					+ " (result sent but local transaction rolled back while stopping -- write never committed;"
					+ " coordinator treats step as succeeded, divergence, manual check required)."
					+ " cancel={} contextRemoved={}", tid, r.Argument.isCancel(), contextRemoved);
			return errorCode(eSagaNotFound);
		}
		var context = sagas.get(tid);
		// onz-01（FND30）：eSagaNotFound只在缺席为终态时应答（见下方cancel分支的
		// 不变量注释——条目在场+businessLock=compensating互斥域，无摘除窗口）。
		if (context == null)
			return errorCode(eSagaNotFound);

		// 等业务完成再决策（FuncSagaEnd可能在慢业务执行期间到达）。
		// 业务失败已在finally中自清理条目：锁到手后remove失败即eSagaNotFound，
		// 失败步骤不会被补偿（无过补偿）；业务成功则条目仍在，补偿/结束串行执行。
		// 等锁有界（onz-02，FND26）：挂死业务（扛过看门狗一次性中断，Task.defaultTimeout只中断
		// 一次）永久持有businessLock且无TTL回收路径（cleanupTimeoutSagas对tryLock失败的条目永久
		// 跳过），redo每60s重发的FuncSagaEnd若无界等锁，每轮净增一个永久阻塞的Normal派发worker
		// ——数十轮后耗尽全池，参与方整体活性死亡。等待预算对齐看门狗的全额（120s）：存活业务
		// （慢而非死）在其自然生命周期内完成并释放锁，FuncSagaEnd串行正确执行；挂死业务则等满
		// 预算后放弃本次——不应答（返回0不SendResult，等价rpc超时：协调者保留决策记录交redo
		// 重发，连接断开时由重连重注册路径自愈），worker有界归还。不引入锁中断：
		// businessLock的不可中断性是补偿与业务互斥的正确性机制。等待线程被看门狗中断时
		// 同样放弃（中断标记保留，由派发框架收尾）。
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
			// 没有设置cancel标志时，表示事务正常结束，用来删除sagas上下文。
			if (r.Argument.isCancel()) {
				// onz-01（FND30）不变量：eSagaNotFound只得上面"条目缺席"的路径应答——缺席即
				// 终态（从未注册/已终结/业务失败自清理/TTL回收）。补偿期间条目保持在sagas
				// 在场、businessLock全程持有=compensating互斥域：重发/并发的FuncSagaEnd先
				// get命中再在tryLockBusiness上排队，不出现"补偿执行中上下文却不在sagas"
				// 的窗口——摘除点在补偿成功之后，失败则条目原地保留等redo重发（协调者按
				// eCompensateFail保留决策记录逐轮补发，重发在锁上排队串行重试）。
				// 补偿参数decode先于补偿执行：载荷损坏截断/cancelClass构造失败时抛出，条目
				// 原样在表（协调者redo可重试），由cleanupTimeoutSagas（默认1小时）兜底。
				final var stub = (OnzSagaStub<?, ?, ?>)context.getStub();
				final var cancelArgument = stub.decodeCancelArgument(r.Argument.getFuncArgument());
				// 在场复核（锁内happens-before观察点，对齐ProcessFuncSagaRequest的finally
				// 注释）：get命中的引用可能在等锁期间被业务失败自清理摘除（锁内remove后
				// unlock），写从未落库，补偿即过补偿（反向分歧）；条目不在/已换实例=缺席
				// 终态，不得执行补偿。
				if (sagas.get(tid) != context)
					return errorCode(eSagaNotFound);
				var rc = TaskSpec.ofProcedure(zeze.newProcedure(() -> stub.end(context, cancelArgument), context.getName())).call();
				if (rc != 0) {
					// 补偿失败：条目从未摘除、留在sagas等redo重发，由cleanupTimeoutSagas
					// 超时兜底（默认1小时，可配置）。refreshLastActive（计时基准见
					// OnzSaga.lastActiveTime）：等待重发的窗口不消耗TTL预算。
					context.refreshLastActive();
					// 裸rc禁止上线：用户补偿结果码与协议错误码共用低32位命名空间（协调者统一
					// getErrorCode解码），rc恰为2时被误判eSagaNotFound而删决策记录——留在表中的
					// 补偿上下文永无重试。统一回eCompensateFail（协调者按未知非零码保留记录
					// 交redo重发），用户rc记录在此。
					logger.warn("saga compensate fail, keep context for resend. tid={} userRc={}", tid, rc);
					return errorCode(eCompensateFail);
				}
				// 补偿成功才摘除（摘除点后移的核心）。两参remove失败→条目已不在（或已换
				// 实例）：缺席是终态，eSagaNotFound为真良性应答；锁内串行域推演不可达
				//（锁内能摘除本条目的只有本分支），纯防御。
				if (!sagas.remove(tid, context))
					return errorCode(eSagaNotFound);
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
