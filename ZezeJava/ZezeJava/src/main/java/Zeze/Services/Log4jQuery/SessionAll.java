package Zeze.Services.Log4jQuery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.LogAgent;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func1;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;

public class SessionAll implements AutoCloseable {
	private final LogAgent agent;
	private final ConcurrentHashMap<String, Session> alls = new ConcurrentHashMap<>();
	private final ConcurrentHashSet<String> finishedSession = new ConcurrentHashSet<>();

	public SessionAll(LogAgent agent) {
		this.agent = agent;
		for (var serverName : agent.getLogServers())
			alls.put(serverName, agent.newSession(serverName));
	}

	public LogAgent getAgent() {
		return agent;
	}

	public BResult.Data operate(Func1<Session, TaskCompletionSource<BResult.Data>> op)
			throws Exception {

		// 异步发送所有请求。
		var futures = new ArrayList<KV<TaskCompletionSource<BResult.Data>, Session>>();
		for (var session : alls.values()) {
			if (!finishedSession.contains(session.getName()))
				futures.add(KV.create(op.call(session), session));
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
			var r = future.getKey().get();
			// 返回的结果基本有序，只是偶尔log4j会有一点点乱序，这里该用什么sort更快？
			r.getLogs().sort(comparator);
			remain = remain || r.isRemain();
			if (!r.isRemain())
				finishedSession.add(future.getValue().getName());
			rs.add(r);
		}
		if (rs.isEmpty())
			return new BResult.Data();

		// 归并所有结果。
		var rData =  merge(rs);
		rData.setRemain(remain);
		return rData;
	}

	public static BResult.Data merge(java.util.List<BResult.Data> rs) {
		switch (rs.size()) {
		case 0:
			throw new IllegalArgumentException("rs.isEmpty.");

		case 1:
			return rs.get(0); // 只有一个。

		case 2:
			return merge(rs.get(0), rs.get(1));

		default:
			// 这里直接一个循环处理不差吧，递归好像不能省什么。
			var tmp = new ArrayList<BResult.Data>();
			var odd = rs.size() % 2 == 1;
			var pairEnd = odd ? rs.size() - 1 : rs.size();
			for (var i = 0; i < pairEnd; i += 2) {
				tmp.add(merge(rs.get(i), rs.get(i + 1)));
			}
			if (odd)
				tmp.add(rs.get(rs.size() - 1));
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
		for (var session : alls.values())
			session.close();
		alls.clear();
		finishedSession.clear();
	}
}
