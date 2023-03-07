package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Net.Binary;
import Zeze.Raft.Agent;
import Zeze.Raft.RaftConfig;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dbh2Agent extends AbstractDbh2Agent {
	private static final Logger logger = LogManager.getLogger(Dbh2Agent.class);
	private final Agent raftClient;
	private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
	private volatile long lastErrorTime;
	private final Dbh2Config config = new Dbh2Config();
	private volatile long activeTime = System.currentTimeMillis();

	public Binary get(String databaseName, String tableName, Binary key) {
		return null;
	}

	public void beginTransaction() {
	}

	private void verifyFastFail() {
		if (System.currentTimeMillis() - lastErrorTime < config.serverFastErrorPeriod)
			throw new RuntimeException("FastErrorPeriod");
	}

	private void setFastFail() {
		var now = System.currentTimeMillis();
		if (now - lastErrorTime > config.serverFastErrorPeriod)
			lastErrorTime = now;
	}

	public void keepAlive() {
		if (loginFuture.isDone() && !loginFuture.isCompletedExceptionally() && !loginFuture.isCancelled())
			return; // not login

		var rpc = new KeepAlive();
		raftClient.send(rpc, p -> {
			if (!rpc.isTimeout() && (rpc.getResultCode() == 0 || rpc.getResultCode() == Procedure.RaftApplied))
				activeTime = System.currentTimeMillis(); // KeepAlive.Response
			return 0;
		});
	}

	public Dbh2Agent(RaftConfig raftConf) throws Exception {
		raftClient = new Agent("dbh2.raft", raftConf);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
	}

	private void raftOnSetLeader(Agent agent) {
	}

	public final void close() throws Exception {
		raftClient.stop();
	}
}
