package Zeze.Services.Log4jQuery;

import Zeze.Builtin.LogService.BBrowse;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BResult;
import Zeze.Builtin.LogService.BSearch;
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSession;
import Zeze.Builtin.LogService.Search;
import Zeze.Services.LogAgent;
import Zeze.Util.TaskCompletionSource;

public class Session implements AutoCloseable {
	private final String serverName;
	private final LogAgent agent;
	private final long sessionId;

	public LogAgent getAgent() {
		return agent;
	}

	public String getName() {
		return serverName;
	}

	public Session(LogAgent agent, String serverName, String logName) {
		this.agent = agent;
		this.serverName = serverName;
		var r = new NewSession();
		r.Argument.setLogName(logName);
		r.SendForWait(agent.__getLogServer(serverName).GetReadySocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error " + r.getResultCode());
		sessionId = r.Result.getId();
	}

	public TaskCompletionSource<BResult.Data> search(int limit, boolean reset,
													 BCondition.Data condition) {
		var r = new Search(new BSearch.Data(sessionId, limit, reset, condition));
		return r.SendForWait(agent.__getLogServer(serverName).GetReadySocket());
	}

	public TaskCompletionSource<BResult.Data> browse(int limit, float offsetFactor, boolean reset,
													 BCondition.Data condition) {
		var r = new Browse(new BBrowse.Data(sessionId, limit, offsetFactor, reset, condition));
		return r.SendForWait(agent.__getLogServer(serverName).GetReadySocket());
	}

	@Override
	public void close() throws Exception {
		var r = new CloseSession();
		r.SendForWait(agent.__getLogServer(serverName).GetReadySocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("close session error " + r.getResultCode());
	}
}
