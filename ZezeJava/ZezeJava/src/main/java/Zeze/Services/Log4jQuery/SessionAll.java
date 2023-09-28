package Zeze.Services.Log4jQuery;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.LogAgent;
import Zeze.Util.Func1;
import Zeze.Util.TaskCompletionSource;

public class SessionAll implements AutoCloseable {
	private final LogAgent agent;
	private final ConcurrentHashMap<String, Session> alls = new ConcurrentHashMap<>();

	public SessionAll(LogAgent agent) {
		this.agent = agent;
		for (var serverName : agent.getLogServers())
			alls.put(serverName, agent.newSession(serverName));
	}

	public LogAgent getAgent() {
		return agent;
	}

	public SortedMap<Long, BLog.Data> operate(Func1<Session, TaskCompletionSource<BResult.Data>> op)
			throws Exception {

		var futures = new ArrayList<TaskCompletionSource<BResult.Data>>();
		for (var session : alls.values())
			futures.add(op.call(session));

		var result = new TreeMap<Long, BLog.Data>();
		for (var future : futures) {
			var r = future.get();
			for (var log : r.getLogs())
				result.put(log.getTime(), log);
		}
		return result;
	}

	public SortedMap<Long, BLog.Data> search(int limit, boolean reset,
											 BCondition.Data condition) throws Exception {
		return operate((session) -> session.search(limit, reset, condition));
	}

	public SortedMap<Long, BLog.Data> browse(int limit, float offsetFactor, boolean reset,
											 BCondition.Data condition) throws Exception {
		return operate((session) -> session.browse(limit, offsetFactor, reset, condition));
	}

	@Override
	public void close() throws Exception {
		for (var session : alls.values())
			session.close();
	}
}
