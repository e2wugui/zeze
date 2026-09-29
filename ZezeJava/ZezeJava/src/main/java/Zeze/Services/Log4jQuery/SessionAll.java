package Zeze.Services.Log4jQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.LogAgent;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func1;
import Zeze.Util.KV;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 跨全部日志服务端的聚合查询会话：对每台建 Session，归并 search/browse 结果并跟踪各台完成状态。
 * 部分失败降级（单台异常不牺牲其余台）+ 会话成员集维护收敛在 operate 内：在册死亡成员自愈重建
 * （renewDeadMembers）与缺册成员补入（reconcileMissingMembers）互补——构造期跳过或构造后上台的
 * 服务器由后者补入，会话构成始终向注册表收敛。死亡成员重建后从该成员已投递水位续扫
 * （见memberSeekBase），已投递区间不跨页重复投递。
 */
public class SessionAll implements AutoCloseable {
	private static final @NotNull Logger logger = LogManager.getLogger(SessionAll.class);

	private static final long MEMBER_RETRY_BACKOFF_NANOS = Duration.ofSeconds(60).toNanos();

	private final LogAgent agent;
	// N02：成员会话死亡自愈重建需要（newSession(serverName, logName)），构造期固定。
	private final String logName;
	private final ConcurrentHashMap<String, Session> alls = new ConcurrentHashMap<>();
	private final ConcurrentHashSet<String> finishedSession = new ConcurrentHashSet<>();
	// 缺册补员的 per-member 退避表（服务器名→上次尝试失败时刻，System.nanoTime时基）：
	// 退避窗内不重试，持续故障期不对死地址逐 operate 打点；成功补入即移除。
	private final ConcurrentHashMap<String, Long> memberRetryBackoff = new ConcurrentHashMap<>();
	// 成员级已投递水位（服务器名→该成员最近返回日志的最大time）：死亡成员重建
	// （renewDeadMembers）后的续扫基点来源。新服务端会话beginTime=-2，翻页仍携带
	// condition.beginTime会触发服务端reset+seek(beginTime)从查询下界整段重扫——死亡前
	// 已投递区间跨页重复返回（merge无去重，"不丢不重"契约破坏且无信号）。以水位作为
	// 重建会话的下发beginTime（协议每请求独立beginTime现成），从首条>=水位处续扫。
	// 水位取max的损失论证：seek定位点=扫描序中首条time>=水位的日志，末投递页内必有
	// time==水位的日志，定位点必不晚于它——跳过的扫描前缀全部已投递（不丢）；重复收敛
	// 为水位边界同时间的少量条目（时间粒度），死亡页未送达区间在定位点之后照常补投。
	private final ConcurrentHashMap<String, Long> deliveredWatermark = new ConcurrentHashMap<>();
	// 成员级续扫基点（捕获语义）：重建时刻的快照，此后该成员的翻页固定下发该值——
	// 服务端beginTime去重哨兵据此短路、游标连续推进。不得逐页跟踪水位：每页变值会使
	// 服务端不断reset+seek到更新的水位，跳过未投递区间丢数据。reset=true（新查询序列）
	// 与close时失效。
	private final ConcurrentHashMap<String, Long> memberSeekBase = new ConcurrentHashMap<>();

	public SessionAll(LogAgent agent, String logName) {
		this.agent = agent;
		this.logName = logName;
		try {
			for (var serverName : agent.getLogServers()) {
				try {
					alls.put(serverName, agent.newSession(serverName, logName));
				} catch (Exception e) {
					// 单台不可用（摘除残留条目、连接失败/超时等）：告警并跳过，查全服降级为部分结果。
					logger.warn("newSession fail, skip log server '{}', logName '{}'", serverName, logName, e);
				}
			}
		} catch (Throwable e) {
			// 防御兜底：非单台失败的意外异常，关闭已建会话后重抛，构造中途夭折不泄漏服务端查询句柄。
			try {
				close();
			} catch (Exception closeEx) {
				e.addSuppressed(closeEx);
			}
			throw Task.forceThrow(e);
		}
	}

	public LogAgent getAgent() {
		return agent;
	}

	/**
	 * 会话当前实际成员集（已成功入会的服务器名，副本快照）：会话构成是绑定快照与复用判定的
	 * 唯一权威——注册表键集不可证明会话构成：构造期不可达台被跳过，构造迭代与注册表更新
	 * 之间存在竞态窗口。
	 */
	public Set<String> memberNames() {
		return Set.copyOf(alls.keySet());
	}

	public BResult.Data operate(Func1<Session, TaskCompletionSource<BResult.Data>> op)
			throws Exception {

		// 缺册补员先行——补入的新成员须参与本轮查询；与文末renewDeadMembers（在册死亡重建）
		// 合成会话成员集维护的单点。
		reconcileMissingMembers();

		// 逐台收集失败：单台异常（RPC超时/连接抖动/发送失败）不牺牲其余台结果，
		// 与构造期"跳过不可用台"降级及close()的逐台收集同构；失败台不标记finishedSession——下次operate自然重试它。
		Exception firstFailure = null;
		var failedServers = new ArrayList<String>();
		// N02（FND28）：会话级死亡的成员（重建候选，见renewDeadMembers）——与网络类瞬时失败区分，
		// 后者重试即可、重建只会白白丢弃仍有效的会话与游标。
		var deadMembers = new ArrayList<String>();
		// 异步发送所有请求。
		var futures = new ArrayList<KV<TaskCompletionSource<BResult.Data>, Session>>();
		for (var session : alls.values()) {
			if (finishedSession.contains(session.getName()))
				continue;
			try {
				futures.add(KV.create(op.call(session), session));
			} catch (Exception e) {
				// 发送阶段失败（连接已死等GetReadySocket同步抛出）：与future.get()失败同构，同样按单台降级。
				failedServers.add(session.getName());
				if (Session.isSessionLevelError(e))
					deadMembers.add(session.getName());
				if (firstFailure == null)
					firstFailure = e;
				else
					firstFailure.addSuppressed(e);
			}
		}
		// 等待结果并排序。
		var rs = new ArrayList<BResult.Data>(futures.size());
		var comparator = new Comparator<BLog.Data>() {
			@Override
			public int compare(BLog.Data o1, BLog.Data o2) {
				return Long.compare(o1.getTime(), o2.getTime());
			}
		};
		var remain = false;
		for (var future : futures) {
			BResult.Data r;
			try {
				r = future.getKey().get();
			} catch (Exception e) {
				failedServers.add(future.getValue().getName());
				if (Session.isSessionLevelError(e))
					deadMembers.add(future.getValue().getName());
				if (firstFailure == null)
					firstFailure = e;
				else
					firstFailure.addSuppressed(e);
				continue;
			}
			// 不变式：错误码台（死会话的服务端 LogicError）不会走到这里——Session.search/browse
			// 返回的 TCS 在 get 时对非零 resultCode 抛异常（Session.checkResultCode），与 RPC 超时/
			// 连接抖动同走上面的 failedServers 路径：不标记 finishedSession（下次 operate 重试）、
			// 全败时上抛。因此能到达本行的只剩零码结果，!isRemain() 才可信地表示"该台查完"。
			r.getLogs().sort(comparator);
			// 成员级已投递水位推进（重建续扫基点来源，见deliveredWatermark注释）：
			// 取本页各日志time的最大值与既有水位合并。
			for (var log : r.getLogs())
				deliveredWatermark.merge(future.getValue().getName(), log.getTime(), Math::max);
			remain = remain || r.isRemain();
			if (!r.isRemain())
				finishedSession.add(future.getValue().getName());
			rs.add(r);
		}
		if (firstFailure != null) {
			if (rs.isEmpty())
				// 全部失败必须抛：空BResult.Data在调用方语义是"查完无匹配"，与"查询失败"不可混淆。
				// 全败的会话级死亡由调用方（FileSessionManager.operateRecovering，zoker-03）整视图重建兜底。
				throw firstFailure;
			// 部分失败：降级返回已有结果（失败台的remain遗漏是降级语义的一部分），warn是唯一可观测补偿。
			logger.warn("operate partial fail, success={}, failed={}", rs.size(), failedServers, firstFailure);
		}
		// N02（FND28）：部分失败中的会话级死亡成员立即重建——此前死会话id被复用台每次operate
		// 恒定失败，静默缺数直到2h客户端TTL驱逐/changeSession；重建后下一页/下一查询恢复全服视图
		// （本页仍按降级语义缺该台数据）。
		renewDeadMembers(deadMembers);
		if (rs.isEmpty())
			return new BResult.Data();

		// 归并所有结果。
		var rData =  merge(rs);
		rData.setRemain(remain);
		return rData;
	}

	/**
	 * 缺册补员：注册表新增或构造期跳过的服务器不在 alls 内，无补入入口则该台数据持续静默缺席、
	 * remain 提前 false。operate 入口对差集逐台尝试补入，补入的新成员参与本轮查询；失败记
	 * per-member 退避时间戳（60s 窗内不重试）。与 {@link #renewDeadMembers}（在册死亡重建）互补。
	 */
	private void reconcileMissingMembers() {
		var now = System.nanoTime();
		for (var serverName : agent.getLogServers()) {
			if (alls.containsKey(serverName))
				continue;
			var lastFail = memberRetryBackoff.get(serverName);
			if (lastFail != null && now - lastFail < MEMBER_RETRY_BACKOFF_NANOS)
				continue;
			try {
				alls.put(serverName, agent.newSession(serverName, logName));
				memberRetryBackoff.remove(serverName);
				logger.warn("reconciled missing member into all-servers session. server='{}', logName '{}'",
						serverName, logName);
			} catch (Exception e) {
				memberRetryBackoff.put(serverName, now);
				logger.warn("reconcile missing member fail, backoff before next retry. server='{}', logName '{}'",
						serverName, logName, e);
			}
		}
	}

	/**
	 * 成员会话死亡的自愈重建（N02，FND28）：对本轮operate判定为会话级死亡（服务端已拒绝该
	 * 会话——典型：闲置超sessionIdleTimeoutMillis被回收）的成员建新会话替换进alls，其余成员
	 * （含网络类瞬时失败的）游标不动。旧成员不发CloseSession：会话级死亡=服务端已无此会话
	 * （无服务端句柄可释放），对死id再发Close只会得到同样的拒绝。新建失败（服务器暂不可达）
	 * 保留死成员条目并warn——下轮operate继续失败并重试重建，不静默缺席成员。
	 * 重建会话从该成员已投递水位续扫（memberSeekBase捕获，见其注释）——本页按降级语义缺
	 * 该台数据，下一页起恢复投递且不重扫已投递区间。
	 */
	private void renewDeadMembers(List<String> deadMembers) {
		for (var name : deadMembers) {
			try {
				alls.put(name, agent.newSession(name, logName));
				// 重建会话的续扫基点=重建时刻的已投递水位（捕获语义，见memberSeekBase注释）：
				// 下一页起对该成员下发beginTime=水位，新会话从首条>=水位处续扫——已投递区间
				// 不再整段重扫（无水位=该成员尚未投递过任何页，按原条件从头扫描即正确）。
				var watermark = deliveredWatermark.get(name);
				if (null != watermark)
					memberSeekBase.put(name, watermark);
				logger.warn("renewed dead member session. server='{}', logName '{}'", name, logName);
			} catch (Exception e) {
				logger.warn("renew dead member session fail, keep dead entry for next retry. server='{}', logName '{}'",
						name, logName, e);
			}
		}
	}

	/**
	 * 重建/续扫成员的下发条件：beginTime覆写为该成员的续扫基点（无基点=原样）。
	 * 每请求按需拷贝（只在有基点的成员上），其余成员共享原条件对象零开销。
	 */
	private BCondition.Data seekCondition(String memberName, BCondition.Data condition) {
		var base = memberSeekBase.get(memberName);
		if (null == base)
			return condition;
		var overridden = condition.copy();
		overridden.setBeginTime(base);
		return overridden;
	}

	public static BResult.Data merge(java.util.List<BResult.Data> rs) {
		switch (rs.size()) {
		case 0:
			throw new IllegalArgumentException("rs.isEmpty.");

		case 1:
			return rs.get(0);

		case 2:
			return merge(rs.get(0), rs.get(1));

		default:
			var tmp = new ArrayList<BResult.Data>();
			var odd = rs.size() % 2 == 1;
			var pairEnd = odd ? rs.size() - 1 : rs.size();
			for (var i = 0; i < pairEnd; i += 2) {
				tmp.add(merge(rs.get(i), rs.get(i + 1)));
			}
			if (odd)
				tmp.add(rs.getLast());
			return merge(tmp);
		}
	}

	public static BResult.Data merge(BResult.Data left, BResult.Data right) {
		var result = new BResult.Data();
		int indexLeft = 0;
		int indexRight = 0;
		while (indexLeft < left.getLogs().size() && indexRight < right.getLogs().size()) {
			if (left.getLogs().get(indexLeft).getTime() <= right.getLogs().get(indexRight).getTime()) {
				result.getLogs().add(left.getLogs().get(indexLeft));
				++indexLeft;
			} else {
				result.getLogs().add(right.getLogs().get(indexRight));
				++indexRight;
			}
		}
		// 下面两种情况不会同时存在，同时存在"在上面"处理。
		if (indexLeft < left.getLogs().size()) {
			while (indexLeft < left.getLogs().size()) {
				result.getLogs().add(left.getLogs().get(indexLeft));
				++indexLeft;
			}
		} else if (indexRight < right.getLogs().size()) {
			while (indexRight < right.getLogs().size()) {
				result.getLogs().add(right.getLogs().get(indexRight));
				++indexRight;
			}
		}
		return result;
	}

	public BResult.Data search(int limit, boolean reset, BCondition.Data condition) throws Exception {
		if (reset)
			clearQuerySequenceState();
		return operate((session) -> session.search(limit, reset, seekCondition(session.getName(), condition)));
	}

	public BResult.Data browse(int limit, float offsetFactor, boolean reset, BCondition.Data condition) throws Exception {
		if (reset)
			clearQuerySequenceState();
		return operate((session) -> session.browse(limit, offsetFactor, reset, seekCondition(session.getName(), condition)));
	}

	/** 新查询序列（reset=true）：完成集与续扫基点/水位随旧序列一起失效。 */
	private void clearQuerySequenceState() {
		finishedSession.clear();
		memberSeekBase.clear();
		deliveredWatermark.clear();
	}

	@Override
	public void close() throws Exception {
		// 逐台关闭并收集异常：单台close失败（连接已死等）不能中断其余，否则服务端会话句柄泄漏。
		Exception first = null;
		for (var session : alls.values()) {
			try {
				session.close();
			} catch (Exception e) {
				if (first == null)
					first = e;
				else
					first.addSuppressed(e);
			}
		}
		alls.clear();
		finishedSession.clear();
		memberSeekBase.clear();
		deliveredWatermark.clear();
		if (first != null)
			throw first;
	}
}
