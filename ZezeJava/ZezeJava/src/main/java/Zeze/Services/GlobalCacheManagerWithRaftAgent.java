package Zeze.Services;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire;
import Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
import Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose;
import Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce;
import Zeze.Net.Binary;
import Zeze.Raft.Agent;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.GlobalAgentBase;
import Zeze.Transaction.GoBackZeze;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlobalCacheManagerWithRaftAgent extends AbstractGlobalCacheManagerWithRaftAgent implements IGlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerWithRaftAgent.class);
	// private static final boolean isDebugEnabled = logger.isDebugEnabled();

	private final Application zz;
	private final RaftAgent[] agents;

	public RaftAgent[] getAgents() {
		return agents;
	}

	@Override
	public int getAgentCount() {
		return agents.length;
	}

	@Override
	public GlobalAgentBase getAgent(int index) {
		return agents[index];
	}

	public GlobalCacheManagerWithRaftAgent(Application zeze, String[] hosts) throws Exception {
		zz = zeze;

		agents = new RaftAgent[hosts.length];
		for (int i = 0; i < hosts.length; ++i) {
			var raftConf = RaftConfig.load(hosts[i]);
			agents[i] = new RaftAgent(this, zz, i, raftConf);
		}
	}

	public final Application getZeze() {
		return zz;
	}

	public final void start() throws Exception {
		lock();
		try {
			for (var agent : agents)
				agent.getRaftClient().getClient().start();

			for (var agent : agents) {
				try {
					agent.waitLoginSuccess();
				} catch (Exception ignored) {
					// raft 登录需要选择leader，所以总是会起新的登录，第一次等待会失败，所以下面尝试两次。
					agent.waitLoginSuccess();
				}
			}
		} finally {
			unlock();
		}
	}

	@Override
	public void close() {
		try {
			stop();
		} catch (Exception e) {
			Task.forceThrow(e);
		}
	}

	public final void stop() throws Exception {
		lock();
		try {
			for (var agent : agents)
				agent.close();
		} finally {
			unlock();
		}
	}

	public static class ReduceBridge extends Zeze.Services.GlobalCacheManager.Reduce {
		private final Reduce real;

		public ReduceBridge(Reduce real) {
			this.real = real;
			Argument.globalKey = real.Argument.getGlobalKey();
			Argument.state = real.Argument.getState();
			setResultCode(real.getResultCode());
		}

		@Override
		public void SendResult(Binary result) {
			real.Result.setGlobalKey(real.Argument.getGlobalKey()); // no change
			real.Result.setState(Result.state);
			real.setResultCode(getResultCode());
			real.SendResult(result);
		}
	}

	@Override
	protected long ProcessReduceRequest(Reduce rpc) {
		switch (rpc.Argument.getState()) {
		case GlobalCacheManagerConst.StateInvalid: {
			var bb = ByteBuffer.Wrap(rpc.Argument.getGlobalKey());
			var tableId = bb.ReadInt4();
			var table = zz.getTable(tableId);
			if (table == null) {
				logger.warn("ReduceInvalid Table Not Found={},ServerId={}",
						tableId, zz.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.setGlobalKey(rpc.Argument.getGlobalKey());
				rpc.Result.setState(GlobalCacheManagerConst.StateInvalid);
				rpc.SendResultCode(0);
				return 0;
			}
			return table.reduceInvalid(new ReduceBridge(rpc), bb);
		}

		case GlobalCacheManagerConst.StateShare: {
			var bb = ByteBuffer.Wrap(rpc.Argument.getGlobalKey());
			var tableId = bb.ReadInt4();
			var table = zz.getTable(tableId);
			if (table == null) {
				logger.warn("ReduceShare Table Not Found={},ServerId={}",
						tableId, zz.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.setGlobalKey(rpc.Argument.getGlobalKey());
				rpc.Result.setState(GlobalCacheManagerConst.StateInvalid);
				rpc.SendResultCode(0);
				return 0;
			}
			return table.reduceShare(new ReduceBridge(rpc), bb);
		}

		default:
			rpc.Result = rpc.Argument;
			rpc.SendResultCode(GlobalCacheManagerConst.ReduceErrorState);
			return 0;
		}
	}

	@Override
	public final int getGlobalCacheManagerHashIndex(@NotNull Binary gkey) {
		return gkey.hashCode() % agents.length;
	}

	@Override
	public @Nullable AcquireResult acquire(@NotNull Binary gkey, int state, boolean fresh, boolean noWait) {
		var agent = agents[getGlobalCacheManagerHashIndex(gkey)]; // hash
		if (agent.isReleasing()) {
			agent.setFastFail();
			var trans = Transaction.getCurrent();
			if (trans == null)
				throw new GoBackZeze("Acquire In Releasing");
			trans.throwAbort("Acquire In Releasing", null);
		}
		agent.verifyFastFail();
		try {
			agent.waitLoginSuccess();
		} catch (Throwable e) { // abort need catch all. will re throw another exception.
			agent.setFastFail();
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				Task.forceThrow(e);
			trans.throwAbort("WaitLoginSuccess", e);
			// never run here
		}

		var rpc = new Acquire();
		if (fresh)
			rpc.setResultCode(GlobalCacheManagerConst.AcquireFreshSource);
		rpc.Argument.setGlobalKey(gkey);
		rpc.Argument.setState(state);
		// 让协议包更小，这里仅仅把ServerId当作ClientId。
		// Global是专用服务，用这个够区分了。
		rpc.getUnique().setClientId(String.valueOf(getZeze().getConfig().getServerId()));
		rpc.setTimeout(agent.getConfig().acquireTimeout);
		try {
			var future = agent.raftClient.sendForWait(rpc);
			if (noWait)
				return null;
			future.await();
		} catch (Throwable e) { // abort need catch all. will re throw another exception.
			agent.setFastFail();
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new IllegalStateException("Acquire Timeout");
			trans.throwAbort("Acquire Timeout", null);
			// never run here
		}
		if (!rpc.isTimeout())
			agent.setActiveTime(System.currentTimeMillis()); // Acquire.Response

		if (rpc.getResultCode() < 0) {
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new IllegalStateException("GlobalAgent.Acquire Failed");
			trans.throwAbort("GlobalAgent.Acquire Failed", null);
			// never run here
		}
		if (rpc.getResultCode() == GlobalCacheManagerConst.AcquireModifyFailed
				|| rpc.getResultCode() == GlobalCacheManagerConst.AcquireShareFailed) {
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new IllegalStateException("GlobalAgent.Acquire Failed");
			trans.throwAbort("GlobalAgent.Acquire Failed", null);
			// never run here
		}
		var rc = rpc.getResultCode();
		state = rpc.Result.getState();
		return //rc == 0 ? AcquireResult.getSuccessResult(state) :
				new AcquireResult(rc, state, rpc.Result.getReduceTid());
	}

	// 1. 【Login|ReLogin|NormalClose】会被Raft.Agent重发处理，这要求GlobalRaft能处理重复请求。
	// 2. 【Login|NormalClose】有多个事务处理，这跟rpc.UniqueRequestId唯一性有矛盾。【可行方法：去掉唯一判断，让流程正确处理重复请求。】
	// 3. 【ReLogin】没有数据修改，完全允许重复，并且不判断唯一性。
	// 4. Raft 高可用性，所以认为服务器永远不会关闭，就不需要处理服务器关闭时清理本地状态。
	public static class RaftAgent extends GlobalAgentBase {
		private final GlobalCacheManagerWithRaftAgent globalCacheManagerWithRaftAgent;
		private final Agent raftClient;
		private final AtomicLong loginTimes = new AtomicLong();
		private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
		private boolean activeClose;
		private volatile long lastErrorTime;

		void verifyFastFail() {
			if (System.currentTimeMillis() - lastErrorTime < getConfig().serverFastErrorPeriod)
				throwException("GlobalAgent In FastErrorPeriod", null); // abort
			// else continue
		}

		void setFastFail() {
			var now = System.currentTimeMillis();
			if (now - lastErrorTime > getConfig().serverFastErrorPeriod)
				lastErrorTime = now;
		}

		@SuppressWarnings("SameParameterValue")
		private static void throwException(String msg, Throwable cause) {
			var txn = Transaction.getCurrent();
			if (txn != null)
				txn.throwAbort(msg, cause);
			throw new IllegalStateException(msg, cause);
		}

		public RaftAgent(GlobalCacheManagerWithRaftAgent global, Application zeze,
						 int _GlobalCacheManagerHashIndex) throws Exception {
			this(global, zeze, _GlobalCacheManagerHashIndex, null);
		}

		@Override
		protected void cancelPending() {
			var tmp = loginFuture;
			if (null != tmp) {
				tmp.cancel(true);
			}
			raftClient.cancelPending();
		}

		@Override
		public void keepAlive() {
			if (null == getConfig())
				return; // not login

			var rpc = new KeepAlive();
			raftClient.send(rpc, p -> {
				if (!rpc.isTimeout() && (rpc.getResultCode() == 0 || rpc.getResultCode() == Procedure.RaftApplied))
					setActiveTime(System.currentTimeMillis()); // KeepAlive.Response
				return 0;
			});
		}

		public RaftAgent(GlobalCacheManagerWithRaftAgent global, Application zeze,
						 int _GlobalCacheManagerHashIndex, RaftConfig raftConf) throws Exception {
			super(zeze);
			globalCacheManagerWithRaftAgent = global;
			super.globalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			raftClient = new Agent("global.raft", zeze, raftConf, Agent.NetClient::new);
			raftClient.setOnSetLeader(this::raftOnSetLeader);
			raftClient.dispatchProtocolToInternalThreadPool = true;
			getGlobalCacheManagerWithRaftAgent().RegisterProtocols(raftClient.getClient());
		}

		public final GlobalCacheManagerWithRaftAgent getGlobalCacheManagerWithRaftAgent() {
			return globalCacheManagerWithRaftAgent;
		}

		public final int getGlobalCacheManagerHashIndex() {
			return globalCacheManagerHashIndex;
		}

		public final Agent getRaftClient() {
			return raftClient;
		}

		public final AtomicLong getLoginTimes() {
			return loginTimes;
		}

		public final boolean getActiveClose() {
			return activeClose;
		}

		public final void close() throws Exception {
			lock();
			try {
				// 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
				if (activeClose)
					return;
				activeClose = true;
			} finally {
				unlock();
			}
			if (loginTimes.get() > 0)
				raftClient.sendForWait(new NormalClose()).await(10 * 1000); // 10s
			raftClient.stop();
		}

		public final void waitLoginSuccess() throws ExecutionException, InterruptedException {
			var volatileTmp = loginFuture;
			if (volatileTmp.isDone()) {
				if (volatileTmp.get())
					return;
				throw new IllegalStateException("login fail.");
			}
			if (!volatileTmp.await(getConfig().loginTimeout))
				throw new IllegalStateException("login timeout.");
			// 再次查看结果。
			if (volatileTmp.isDone() && volatileTmp.get())
				return;
			// 只等待一次，不成功则失败。
			throw new IllegalStateException("login timeout.");
		}

		private TaskCompletionSource<Boolean> startNewLogin() {
			lock();
			try {
				loginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
				return loginFuture = new TaskCompletionSource<>();
			} finally {
				unlock();
			}
		}

		private void raftOnSetLeader(Agent agent) {
			var client = agent.getClient();
			if (client == null)
				return;
			var zeze = client.getZeze();
			if (zeze == null)
				return;
			var config = zeze.getConfig();
			var future = startNewLogin();

			if (loginTimes.get() == 0) {
				var login = new Login();
				login.Argument.setServerId(config.getServerId());
				login.Argument.setGlobalCacheManagerHashIndex(globalCacheManagerHashIndex);

				agent.send(login, p -> {
					var rpc = (Login)p;
					if (rpc.isTimeout() || rpc.getResultCode() != 0) {
						logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
						// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
					} else {
						setActiveTime(System.currentTimeMillis());
						loginTimes.getAndIncrement();
						this.initialize(login.Result.getMaxNetPing(),
								login.Result.getServerProcessTime(), login.Result.getServerReleaseTimeout());
						future.setResult(true);
					}
					return 0;
				});
			} else {
				var relogin = new ReLogin();
				relogin.Argument.setServerId(config.getServerId());
				relogin.Argument.setGlobalCacheManagerHashIndex(globalCacheManagerHashIndex);
				agent.send(relogin, p -> {
					var rpc = (ReLogin)p;
					if (rpc.isTimeout() || rpc.getResultCode() != 0) {
						logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
						// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
					} else {
						setActiveTime(System.currentTimeMillis());
						loginTimes.getAndIncrement();
						future.setResult(true);
					}
					return 0;
				});
			}
		}
	}
}
