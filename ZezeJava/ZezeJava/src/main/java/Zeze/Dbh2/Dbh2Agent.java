package Zeze.Dbh2;

import java.util.concurrent.ExecutionException;
import Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
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
	private boolean activeClose;
	private volatile long lastErrorTime;
	private final Dbh2Config config = new Dbh2Config();
	private volatile long activeTime = System.currentTimeMillis();

	public Binary get(String databaseName, String tableName, Binary key) {
		return null;
	}

	public void beginTransaction() {
	}

	void verifyFastFail() {
		if (System.currentTimeMillis() - lastErrorTime < config.serverFastErrorPeriod)
			throw new RuntimeException("FastErrorPeriod");
	}

	void setFastFail() {
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
		var client = agent.getClient();
		if (client == null)
			return;

		var future = startNewLogin();
		var login = new Login(); // todo 换成dbh2.login

		agent.send(login, p -> {
			var rpc = (Login)p;
			if (rpc.isTimeout() || rpc.getResultCode() != 0) {
				logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
				// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
			} else {
				activeTime = System.currentTimeMillis();
				future.setResult(true);
			}
			return 0;
		}, true);
	}

	public final void close() throws Exception {
		synchronized (this) {
			// 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
			if (activeClose)
				return;
			activeClose = true;
		}
		raftClient.stop();
	}

	public final void waitLoginSuccess() throws ExecutionException, InterruptedException {
		var volatileTmp = loginFuture;
		if (volatileTmp.isDone()) {
			if (volatileTmp.get())
				return;
			throw new IllegalStateException("login fail.");
		}
		if (!volatileTmp.await(config.loginTimeout))
			throw new IllegalStateException("login timeout.");
		// 再次查看结果。
		if (volatileTmp.isDone() && volatileTmp.get())
			return;
		// 只等待一次，不成功则失败。
		throw new IllegalStateException("login timeout.");
	}

	private synchronized TaskCompletionSource<Boolean> startNewLogin() {
		loginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
		return loginFuture = new TaskCompletionSource<>();
	}
}
