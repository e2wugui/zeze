package Zeze.Services.Log4jQuery;

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
 * 部分失败降级（单台异常不牺牲其余台）+ 会话级死亡成员的自愈重建（renewDeadMembers，N02）。
 */
public class SessionAll implements AutoCloseable {
	private static final @NotNull Logger logger = LogManager.getLogger(SessionAll.class);

	private final LogAgent agent;
	// N02：成员会话死亡自愈重建需要（newSession(serverName, logName)），构造期固定。
	private final String logName;
	private final ConcurrentHashMap<String, Session> alls = new ConcurrentHashMap<>();
	private final ConcurrentHashSet<String> finishedSession = new ConcurrentHashSet<>();

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
	 * 唯一权威（FND30 zokermanager-01/02）——注册表键集不可证明会话构成：构造期不可达台被
	 * 跳过，构造迭代与注册表更新（SM 事件线程并发 put）之间存在竞态窗口。
	 */
	public Set<String> memberNames() {
		return Set.copyOf(alls.keySet());
	}

	public BResult.Data operate(Func1<Session, TaskCompletionSource<BResult.Data>> op)
			throws Exception {

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
	 * 成员会话死亡的自愈重建（N02，FND28）：对本轮operate判定为会话级死亡（服务端已拒绝该
	 * 会话——典型：闲置超sessionIdleTimeoutMillis被回收）的成员建新会话替换进alls，其余成员
	 * （含网络类瞬时失败的）游标不动。旧成员不发CloseSession：会话级死亡=服务端已无此会话
	 * （无服务端句柄可释放），对死id再发Close只会得到同样的拒绝。新建失败（服务器暂不可达）
	 * 保留死成员条目并warn——下轮operate继续失败并重试重建，不静默缺席成员。
	 */
	private void renewDeadMembers(List<String> deadMembers) {
		for (var name : deadMembers) {
			try {
				alls.put(name, agent.newSession(name, logName));
				logger.warn("renewed dead member session. server='{}', logName '{}'", name, logName);
			} catch (Exception e) {
				logger.warn("renew dead member session fail, keep dead entry for next retry. server='{}', logName '{}'",
						name, logName, e);
			}
		}
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
			finishedSession.clear();
		return operate((session) -> session.search(limit, reset, condition));
	}

	public BResult.Data browse(int limit, float offsetFactor, boolean reset, BCondition.Data condition) throws Exception {
		if (reset)
			finishedSession.clear();
		return operate((session) -> session.browse(limit, offsetFactor, reset, condition));
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
		if (first != null)
			throw first;
	}
}
