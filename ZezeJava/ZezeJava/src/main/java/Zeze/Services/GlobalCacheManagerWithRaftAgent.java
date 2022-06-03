package Zeze.Services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire;
import Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
import Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose;
import Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.GlobalAgentBase;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlobalCacheManagerWithRaftAgent extends AbstractGlobalCacheManagerWithRaftAgent implements IGlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerWithRaftAgent.class);

	private final Zeze.Application zz;
	public RaftAgent[] Agents;

	public GlobalCacheManagerWithRaftAgent(Zeze.Application zeze) {
		zz = zeze;
	}

	public final Zeze.Application getZeze() {
		return zz;
	}

	public final synchronized void Start(String[] hosts) throws Throwable {
		if (Agents != null)
			return;

		Agents = new RaftAgent[hosts.length];
		for (int i = 0; i < hosts.length; ++i) {
			var raftConf = Zeze.Raft.RaftConfig.Load(hosts[i]);
			Agents[i] = new RaftAgent(this, zz, i, raftConf);
		}

		for (var agent : Agents)
			agent.getRaftClient().getClient().Start();

		for (var agent : Agents)
			agent.WaitLoginSuccess();
	}

	@Override
	public void close() throws IOException {
		try {
			Stop();
		} catch (IOException | RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public final synchronized void Stop() throws Throwable {
		if (Agents == null)
			return;

		for (var agent : Agents)
			agent.Close();
		Agents = null;
	}

	public static class ReduceBridge extends Zeze.Services.GlobalCacheManager.Reduce {
		private final Reduce Real;

		public ReduceBridge(Reduce real) {
			Real = real;
			Argument.GlobalKey = real.Argument.getGlobalKey();
			Argument.State = real.Argument.getState();
			Argument.GlobalSerialId = real.Argument.getGlobalSerialId();
		}

		@Override
		public void SendResult() {
			SendResult(null);
		}

		@Override
		public void SendResult(Zeze.Net.Binary result) {
			Real.Result.setGlobalKey(Real.Argument.getGlobalKey()); // no change
			Real.Result.setGlobalSerialId(Result.GlobalSerialId);
			Real.Result.setState(Result.State);
			Real.SendResult(result);
		}

		@Override
		public void SendResultCode(long code) {
			SendResultCode(code, null);
		}

		@Override
		public void SendResultCode(long code, Zeze.Net.Binary result) {
			Real.Result.setGlobalKey(Real.Argument.getGlobalKey()); // no change
			Real.Result.setGlobalSerialId(Result.GlobalSerialId);
			Real.Result.setState(Result.State);
			Real.SendResultCode(code, result);
		}
	}

	@Override
	protected long ProcessReduceRequest(Reduce rpc) {
		switch (rpc.Argument.getState()) {
		case GlobalCacheManagerServer.StateInvalid: {
			var bb = ByteBuffer.Wrap(rpc.Argument.getGlobalKey());
			var tableId = bb.ReadInt4();
			var table = zz.GetTable(tableId);
			if (table == null) {
				logger.warn("ReduceInvalid Table Not Found={},ServerId={}",
						tableId, zz.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.setGlobalKey(rpc.Argument.getGlobalKey());
				rpc.Result.setState(GlobalCacheManagerServer.StateInvalid);
				rpc.SendResultCode(0);
				return 0;
			}
			return table.ReduceInvalid(new ReduceBridge(rpc), bb);
		}

		case GlobalCacheManagerServer.StateShare: {
			var bb = ByteBuffer.Wrap(rpc.Argument.getGlobalKey());
			var tableId = bb.ReadInt4();
			var table = zz.GetTable(tableId);
			if (table == null) {
				logger.warn("ReduceShare Table Not Found={},ServerId={}",
						tableId, zz.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.setGlobalKey(rpc.Argument.getGlobalKey());
				rpc.Result.setState(GlobalCacheManagerServer.StateInvalid);
				rpc.SendResultCode(0);
				return 0;
			}
			return table.ReduceShare(new ReduceBridge(rpc), bb);
		}

		default:
			rpc.Result = rpc.Argument;
			rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
			return 0;
		}
	}

	@Override
	public final int GetGlobalCacheManagerHashIndex(Binary gkey) {
		return gkey.hashCode() % Agents.length;
	}

	@Override
	public IGlobalAgent.AcquireResult Acquire(Binary gkey, int state, boolean fresh) {
		if (Agents != null) {
			var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash

			try {
				agent.WaitLoginSuccess();
			} catch (Throwable e) {
				Transaction trans = Transaction.getCurrent();
				if (trans == null)
					throw new RuntimeException(e);
				trans.ThrowAbort("WaitLoginSuccess", e);
				// never got here
			}

			var rpc = new Acquire();
			if (fresh)
				rpc.setResultCode(GlobalCacheManagerServer.AcquireFreshSource);
			rpc.Argument.setGlobalKey(gkey);
			rpc.Argument.setState(state);
			agent.RaftClient.SendForWait(rpc).await();
			if (false == rpc.isTimeout())
				agent.setActiveTime(System.currentTimeMillis()); // Acquire.Response

			if (rpc.getResultCode() < 0) {
				Transaction trans = Transaction.getCurrent();
				if (trans == null)
					throw new IllegalStateException("GlobalAgent.Acquire Failed");
				trans.ThrowAbort("GlobalAgent.Acquire Failed", null);
				// never got here
			}
			if (rpc.getResultCode() == GlobalCacheManagerServer.AcquireModifyFailed
					|| rpc.getResultCode() == GlobalCacheManagerServer.AcquireShareFailed) {
				Transaction trans = Transaction.getCurrent();
				if (trans == null)
					throw new IllegalStateException("GlobalAgent.Acquire Failed");
				trans.ThrowAbort("GlobalAgent.Acquire Failed", null);
				// never got here
			}
			return new IGlobalAgent.AcquireResult(
					rpc.getResultCode(), rpc.Result.getState(), rpc.Result.getGlobalSerialId());
		}
		logger.debug("Acquire local ++++++");
		return new IGlobalAgent.AcquireResult(0, state, 0);
	}

	// 1. 【Login|ReLogin|NormalClose】会被Raft.Agent重发处理，这要求GlobalRaft能处理重复请求。
	// 2. 【Login|NormalClose】有多个事务处理，这跟rpc.UniqueRequestId唯一性有矛盾。【可行方法：去掉唯一判断，让流程正确处理重复请求。】
	// 3. 【ReLogin】没有数据修改，完全允许重复，并且不判断唯一性。
	// 4. Raft 高可用性，所以认为服务器永远不会关闭，就不需要处理服务器关闭时清理本地状态。
	public static class RaftAgent extends GlobalAgentBase {
		private final GlobalCacheManagerWithRaftAgent GlobalCacheManagerWithRaftAgent;
		private final int GlobalCacheManagerHashIndex;
		private final Zeze.Raft.Agent RaftClient;
		private final AtomicLong LoginTimes = new AtomicLong();
		private volatile TaskCompletionSource<Boolean> LoginFuture = new TaskCompletionSource<>();
		private boolean ActiveClose;

		public RaftAgent(GlobalCacheManagerWithRaftAgent global, Zeze.Application zeze,
						 int _GlobalCacheManagerHashIndex) throws Throwable {
			this(global, zeze, _GlobalCacheManagerHashIndex, null);
		}

		@Override
		public void keepAlive() {
			if (null == getConfig())
				return; // not login

			var rpc = new KeepAlive();
			RaftClient.Send(rpc, p -> {
				if (!rpc.isTimeout() && (rpc.getResultCode() == 0 || rpc.getResultCode() == Procedure.RaftApplied))
					setActiveTime(System.currentTimeMillis()); // KeepAlive.Response
				return 0;
			});
		}

		public RaftAgent(GlobalCacheManagerWithRaftAgent global, Zeze.Application zeze,
						 int _GlobalCacheManagerHashIndex, Zeze.Raft.RaftConfig raftConf) throws Throwable {
			GlobalCacheManagerWithRaftAgent = global;
			GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			RaftClient = new Zeze.Raft.Agent("Zeze.GlobalRaft.Agent", zeze, raftConf);
			RaftClient.setOnSetLeader(this::RaftOnSetLeader);
			RaftClient.DispatchProtocolToInternalThreadPool = true;
			getGlobalCacheManagerWithRaftAgent().RegisterProtocols(RaftClient.getClient());
		}

		public final GlobalCacheManagerWithRaftAgent getGlobalCacheManagerWithRaftAgent() {
			return GlobalCacheManagerWithRaftAgent;
		}

		public final int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}

		public final Zeze.Raft.Agent getRaftClient() {
			return RaftClient;
		}

		public final AtomicLong getLoginTimes() {
			return LoginTimes;
		}

		public final boolean getActiveClose() {
			return ActiveClose;
		}

		public final void Close() throws Throwable {
			synchronized (this) {
				// 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
				if (ActiveClose)
					return;
				ActiveClose = true;
			}
			if (LoginTimes.get() > 0)
				RaftClient.SendForWait(new NormalClose()).await(10 * 1000); // 10s
			RaftClient.getClient().Stop();
		}

		public final void WaitLoginSuccess() throws ExecutionException, InterruptedException {
			while (true) {
				try {
					var volatileTmp = LoginFuture;
					if (volatileTmp.isDone() && volatileTmp.get())
						return;
					volatileTmp.await();
				} catch (RuntimeException ignored) {
				}
			}
		}

		private synchronized TaskCompletionSource<Boolean> StartNewLogin() {
			LoginFuture.cancel(false); // 如果旧的Future上面有人在等，让他们失败。
			return LoginFuture = new TaskCompletionSource<>();
		}

		private void RaftOnSetLeader(Zeze.Raft.Agent agent) {
			var client = agent.getClient();
			if (client == null)
				return;
			var zeze = client.getZeze();
			if (zeze == null)
				return;
			var config = zeze.getConfig();
			if (config == null)
				return;

			var future = StartNewLogin();

			if (LoginTimes.get() == 0) {
				var login = new Login();
				login.Argument.setServerId(config.getServerId());
				login.Argument.setGlobalCacheManagerHashIndex(GlobalCacheManagerHashIndex);

				agent.Send(login, p -> {
					var rpc = (Login)p;
					if (rpc.isTimeout() || rpc.getResultCode() != 0) {
						logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
						// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
					} else {
						LoginTimes.incrementAndGet();
						this.initialize(login.Result.getMaxNetPing(), login.Result.getServerProcessTime(), login.Result.getServerReleaseTimeout());
						future.SetResult(true);
					}
					return 0;
				}, true);
			} else {
				var relogin = new ReLogin();
				relogin.Argument.setServerId(config.getServerId());
				relogin.Argument.setGlobalCacheManagerHashIndex(GlobalCacheManagerHashIndex);
				agent.Send(relogin, p -> {
					var rpc = (ReLogin)p;
					if (rpc.isTimeout() || rpc.getResultCode() != 0) {
						logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
						// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
					} else {
						LoginTimes.incrementAndGet();
						future.SetResult(true);
					}
					return 0;
				}, true);
			}
		}
	}
}
