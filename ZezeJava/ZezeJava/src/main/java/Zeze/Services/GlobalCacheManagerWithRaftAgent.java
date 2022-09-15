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
import Zeze.Transaction.GoBackZeze;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlobalCacheManagerWithRaftAgent extends AbstractGlobalCacheManagerWithRaftAgent implements IGlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerWithRaftAgent.class);
	// private static final boolean isDebugEnabled = logger.isDebugEnabled();

	private final Zeze.Application zz;
	public final RaftAgent[] Agents;

	public GlobalCacheManagerWithRaftAgent(Zeze.Application zeze, String[] hosts) throws Throwable {
		zz = zeze;

		Agents = new RaftAgent[hosts.length];
		for (int i = 0; i < hosts.length; ++i) {
			var raftConf = Zeze.Raft.RaftConfig.Load(hosts[i]);
			Agents[i] = new RaftAgent(this, zz, i, raftConf);
		}
	}

	public final Zeze.Application getZeze() {
		return zz;
	}

	public final synchronized void Start() throws Throwable {
		for (var agent : Agents)
			agent.getRaftClient().getClient().Start();

		for (var agent : Agents) {
			// raft 登录需要选择leader，所以总是会起新的登录，第一次等待会失败，所以下面尝试两次。
			for (int i = 0; i < 2; ++i) {
				try {
					agent.WaitLoginSuccess();
				} catch (Throwable ignored) {
				}
			}
		}
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
		for (var agent : Agents)
			agent.Close();
	}

	public static class ReduceBridge extends Zeze.Services.GlobalCacheManager.Reduce {
		private final Reduce Real;

		public ReduceBridge(Reduce real) {
			Real = real;
			Argument.GlobalKey = real.Argument.getGlobalKey();
			Argument.State = real.Argument.getState();
			setResultCode(real.getResultCode());
		}

		@Override
		public void SendResult(Zeze.Net.Binary result) {
			Real.Result.setGlobalKey(Real.Argument.getGlobalKey()); // no change
			Real.Result.setState(Result.State);
			Real.setResultCode(getResultCode());
			Real.SendResult(result);
		}
	}

	@Override
	protected long ProcessReduceRequest(Reduce rpc) {
		switch (rpc.Argument.getState()) {
		case GlobalCacheManagerConst.StateInvalid: {
			var bb = ByteBuffer.Wrap(rpc.Argument.getGlobalKey());
			var tableId = bb.ReadInt4();
			var table = zz.GetTable(tableId);
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
			var table = zz.GetTable(tableId);
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
	public final int getGlobalCacheManagerHashIndex(Binary gkey) {
		return gkey.hashCode() % Agents.length;
	}

	@Override
	public AcquireResult acquire(Binary gkey, int state, boolean fresh, boolean noWait) {
		var agent = Agents[getGlobalCacheManagerHashIndex(gkey)]; // hash
		if (agent.isReleasing()) {
			agent.setFastFail();
			var trans = Transaction.getCurrent();
			if (trans == null)
				throw new GoBackZeze("Acquire In Releasing");
			trans.ThrowAbort("Acquire In Releasing", null);
		}
		agent.verifyFastFail();
		try {
			agent.WaitLoginSuccess();
		} catch (Throwable e) {
			agent.setFastFail();
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new RuntimeException(e);
			trans.ThrowAbort("WaitLoginSuccess", e);
			// never got here
		}

		var rpc = new Acquire();
		if (fresh)
			rpc.setResultCode(GlobalCacheManagerConst.AcquireFreshSource);
		rpc.Argument.setGlobalKey(gkey);
		rpc.Argument.setState(state);
		// 让协议包更小，这里仅仅把ServerId当作ClientId。
		// Global是专用服务，用这个够区分了。
		rpc.getUnique().setClientId(String.valueOf(getZeze().getConfig().getServerId()));
		rpc.setTimeout(agent.getConfig().AcquireTimeout);
		try {
			var future = agent.RaftClient.SendForWait(rpc);
			if (noWait)
				return null;
			future.await();
		} catch (Throwable e) {
			agent.setFastFail();
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new IllegalStateException("Acquire Timeout");
			trans.ThrowAbort("Acquire Timeout", null);
			// never got here
		}
		if (!rpc.isTimeout())
			agent.setActiveTime(System.currentTimeMillis()); // Acquire.Response

		if (rpc.getResultCode() < 0) {
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new IllegalStateException("GlobalAgent.Acquire Failed");
			trans.ThrowAbort("GlobalAgent.Acquire Failed", null);
			// never got here
		}
		if (rpc.getResultCode() == GlobalCacheManagerConst.AcquireModifyFailed
				|| rpc.getResultCode() == GlobalCacheManagerConst.AcquireShareFailed) {
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw new IllegalStateException("GlobalAgent.Acquire Failed");
			trans.ThrowAbort("GlobalAgent.Acquire Failed", null);
			// never got here
		}
		var rc = rpc.getResultCode();
		state = rpc.Result.getState();
		return rc == 0 ? AcquireResult.getSuccessResult(state) : new AcquireResult(rc, state);
	}

	// 1. 【Login|ReLogin|NormalClose】会被Raft.Agent重发处理，这要求GlobalRaft能处理重复请求。
	// 2. 【Login|NormalClose】有多个事务处理，这跟rpc.UniqueRequestId唯一性有矛盾。【可行方法：去掉唯一判断，让流程正确处理重复请求。】
	// 3. 【ReLogin】没有数据修改，完全允许重复，并且不判断唯一性。
	// 4. Raft 高可用性，所以认为服务器永远不会关闭，就不需要处理服务器关闭时清理本地状态。
	public static class RaftAgent extends GlobalAgentBase {
		private final GlobalCacheManagerWithRaftAgent GlobalCacheManagerWithRaftAgent;
		private final Zeze.Raft.Agent RaftClient;
		private final AtomicLong LoginTimes = new AtomicLong();
		private volatile TaskCompletionSource<Boolean> LoginFuture = new TaskCompletionSource<>();
		private boolean ActiveClose;
		private volatile long LastErrorTime;

		void verifyFastFail() {
			if (System.currentTimeMillis() - LastErrorTime < getConfig().ServerFastErrorPeriod)
				ThrowException("GlobalAgent In FastErrorPeriod", null); // abort
			// else continue
		}

		void setFastFail() {
			var now = System.currentTimeMillis();
			if (now - LastErrorTime > getConfig().ServerFastErrorPeriod)
				LastErrorTime = now;
		}

		@SuppressWarnings("SameParameterValue")
		private static void ThrowException(String msg, Throwable cause) {
			var txn = Transaction.getCurrent();
			if (txn != null)
				txn.ThrowAbort(msg, cause);
			throw new RuntimeException(msg, cause);
		}

		public RaftAgent(GlobalCacheManagerWithRaftAgent global, Zeze.Application zeze,
						 int _GlobalCacheManagerHashIndex) throws Throwable {
			this(global, zeze, _GlobalCacheManagerHashIndex, null);
		}

		@Override
		protected void cancelPending() {
			var tmp = LoginFuture;
			if (null != tmp) {
				tmp.cancel(true);
			}
			RaftClient.CancelPending();
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
			super(zeze);
			GlobalCacheManagerWithRaftAgent = global;
			super.globalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			RaftClient = new Zeze.Raft.Agent("global.raft", zeze, raftConf);
			RaftClient.setOnSetLeader(this::RaftOnSetLeader);
			RaftClient.DispatchProtocolToInternalThreadPool = true;
			getGlobalCacheManagerWithRaftAgent().RegisterProtocols(RaftClient.getClient());
		}

		public final GlobalCacheManagerWithRaftAgent getGlobalCacheManagerWithRaftAgent() {
			return GlobalCacheManagerWithRaftAgent;
		}

		public final int getGlobalCacheManagerHashIndex() {
			return globalCacheManagerHashIndex;
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
			RaftClient.Stop();
		}

		public final void WaitLoginSuccess() throws ExecutionException, InterruptedException {
			var volatileTmp = LoginFuture;
			if (volatileTmp.isDone()) {
				if (volatileTmp.get())
					return;
				throw new IllegalStateException("login fail.");
			}
			if (!volatileTmp.await(getConfig().LoginTimeout))
				throw new IllegalStateException("login timeout.");
			// 再次查看结果。
			if (volatileTmp.isDone() && volatileTmp.get())
				return;
			// 只等待一次，不成功则失败。
			throw new IllegalStateException("login timeout.");
		}

		private synchronized TaskCompletionSource<Boolean> StartNewLogin() {
			LoginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
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
				login.Argument.setGlobalCacheManagerHashIndex(globalCacheManagerHashIndex);

				agent.Send(login, p -> {
					var rpc = (Login)p;
					if (rpc.isTimeout() || rpc.getResultCode() != 0) {
						logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
						// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
					} else {
						setActiveTime(System.currentTimeMillis());
						LoginTimes.getAndIncrement();
						this.initialize(login.Result.getMaxNetPing(), login.Result.getServerProcessTime(), login.Result.getServerReleaseTimeout());
						future.SetResult(true);
					}
					return 0;
				}, true);
			} else {
				var relogin = new ReLogin();
				relogin.Argument.setServerId(config.getServerId());
				relogin.Argument.setGlobalCacheManagerHashIndex(globalCacheManagerHashIndex);
				agent.Send(relogin, p -> {
					var rpc = (ReLogin)p;
					if (rpc.isTimeout() || rpc.getResultCode() != 0) {
						logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
						// 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
					} else {
						setActiveTime(System.currentTimeMillis());
						LoginTimes.getAndIncrement();
						future.SetResult(true);
					}
					return 0;
				}, true);
			}
		}
	}
}
