package Zeze.Services;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Config;
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
import Zeze.Util.Id128;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Raft版全局缓存管理器的客户端Agent：按gkey哈希分片到多个raft分片Agent（RaftAgent），
 * 代替进程内全局锁表执行Acquire/Reduce并处理服务端下发的Reduce请求。
 */
public class GlobalCacheManagerWithRaftAgent extends AbstractGlobalCacheManagerWithRaftAgent implements IGlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerWithRaftAgent.class);

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
				//noinspection DataFlowIssue
				agent.getRaftClient().getClient().start();

			for (var agent : agents)
				agent.waitLoginSuccess();
		} finally {
			unlock();
		}
	}

	@Override
	public final void stop() throws Exception {
		lock();
		try {
			for (var agent : agents) {
				try {
					agent.close();
				} catch (Exception e) {
					// 单个分片关闭失败仅记日志并继续，避免中断其余分片与应用停机
					logger.error("GlobalCacheManagerWithRaftAgent.Stop Agent={}",
							agent.getGlobalCacheManagerHashIndex(), e);
				}
			}
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
		public void SendResult(@Nullable Binary result) {
			if (!tryMarkSendResultDone())
				return;
			resultEncoded = result;
			setRequest(false);
			forwardResult(result);
		}

		@Override
		protected boolean sendResultCode(long code, @Nullable Binary result) {
			if (!tryMarkSendResultDone())
				return false;
			setResultCode(code);
			resultEncoded = result;
			setRequest(false);
			forwardResult(result);
			return true; // 与Rpc一致，表示赢得应答权，并非网络已经成功写出。
		}

		private void forwardResult(@Nullable Binary result) {
			real.Result.setGlobalKey(real.Argument.getGlobalKey()); // no change
			real.Result.setState(Result.state);
			// 同步转发被降级方 tid，保持与非 Raft 链路协议一致
			var reducedTid = Result.reducedTid;
			real.Result.setReduceTid(reducedTid != null ? reducedTid : Id128.Zero);
			real.SendResultCode(getResultCode(), result);
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
			} else
				table.reduceInvalid(new ReduceBridge(rpc), bb);
			break;
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
			} else
				table.reduceShare(new ReduceBridge(rpc), bb);
			break;
		}
		default:
			rpc.Result = rpc.Argument;
			rpc.SendResultCode(GlobalCacheManagerConst.ReduceErrorState);
			break;
		}
		return 0;
	}

	@Override
	public final int getGlobalCacheManagerHashIndex(Binary gkey) {
		return Integer.remainderUnsigned(gkey.hashCode(), agents.length);
	}

	@Override
	public @Nullable AcquireResult acquire(Binary gkey, int state, boolean fresh, boolean noWait) {
		var agent = agents[getGlobalCacheManagerHashIndex(gkey)]; // hash
		if (agent.isReleasing()) {
			agent.setFastFail();
			var trans = Transaction.getCurrent();
			if (trans == null)
				throw new GoBackZeze("Acquire In Releasing");
			trans.throwAbort("Acquire In Releasing", null);
		}
		if (getZeze().getConfig().getEnableGlobalFastFail())
			agent.verifyFastFail();
		try {
			agent.waitLoginSuccess();
		} catch (Throwable e) { // abort need catch all. will re throw another exception.
			agent.setFastFail();
			Transaction trans = Transaction.getCurrent();
			if (trans == null)
				throw Task.forceThrow(e);
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
				throw new IllegalStateException("Acquire Timeout", e); // 根因随链传播，否则真实异常被吞
			trans.throwAbort("Acquire Timeout", e);
			// never run here
		}
		if (!rpc.isTimeout())
			agent.setActiveTime(System.currentTimeMillis()); // Acquire.Response

		var rc = rpc.getResultCode();
		// GCM-raft 服务端不启用 uniqueRequest（重发即重执行）；若后续接入去重，需为 RaftApplied 放行
		if (rc < 0 || rc == GlobalCacheManagerConst.AcquireModifyFailed
				|| rc == GlobalCacheManagerConst.AcquireShareFailed) {
			Transaction trans = Transaction.getCurrent();
			var msg = "GlobalAgent.Acquire Failed: " + rc;
			if (trans == null)
				throw new IllegalStateException(msg);
			trans.throwAbort(msg, null);
			// never run here
		}
		state = rpc.Result.getState();
		return
				new AcquireResult(rc, state, rpc.Result.getReduceTid());
	}

	// 1. 【Login|ReLogin|NormalClose】会被Raft.Agent重发处理，这要求GlobalRaft能处理重复请求。
	// 2. 【Login|NormalClose】有多个事务处理，这跟rpc.UniqueRequestId唯一性有矛盾。【可行方法：去掉唯一判断，让流程正确处理重复请求。】
	// 3. 【ReLogin】没有数据修改，完全允许重复，并且不判断唯一性。
	// 4. Raft 高可用性，所以认为服务器永远不会关闭，就不需要处理服务器关闭时清理本地状态。
	public static class RaftAgent extends GlobalAgentBase {
		private static final long LOGIN_RETRY_MIN_DELAY_MS = 500;
		private static final long LOGIN_RETRY_MAX_DELAY_MS = 1000;

		private final GlobalCacheManagerWithRaftAgent globalCacheManagerWithRaftAgent;
		private final Agent raftClient;
		private final AtomicLong loginTimes = new AtomicLong();
		private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
		private @Nullable ScheduledFuture<?> loginRetryTask;
		private volatile boolean activeClose;
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
		@Contract("_, _ -> fail")
		private static void throwException(String msg, @Nullable Throwable cause) {
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
		@SuppressWarnings("ConstantConditions") // 保留对登录 Future 快照的防御检查。
		protected void cancelPending() {
			var tmp = loginFuture;
			if (null != tmp) {
				tmp.cancel(true);
			}
			raftClient.cancelPending();
		}

		@Override
		public void keepAlive() {
			var rpc = new KeepAlive();
			raftClient.send(rpc, p -> {
				if (!rpc.isTimeout() && (rpc.getResultCode() == 0 || rpc.getResultCode() == Procedure.RaftApplied))
					setActiveTime(System.currentTimeMillis()); // KeepAlive.Response
				return 0;
			});
		}

		public RaftAgent(GlobalCacheManagerWithRaftAgent global, Application zeze,
						 int _GlobalCacheManagerHashIndex, @Nullable RaftConfig raftConf) throws Exception {
			super(zeze);
			globalCacheManagerWithRaftAgent = global;
			super.globalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			raftClient = new Agent("global.raft", zeze, Objects.requireNonNull(raftConf), Agent.NetClient::new);
			raftClient.setOnSetLeader(this::raftOnSetLeader);
			// Reduce 处理会等待本地锁，必须派发到工作线程池执行
			raftClient.dispatchProtocolToInternalThreadPool = true;
			getGlobalCacheManagerWithRaftAgent().RegisterProtocols(Objects.requireNonNull(raftClient.getClient()));
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
				cancelLoginRetryTask();
				loginFuture.cancel(true);
			} finally {
				unlock();
			}
			try {
				if (loginTimes.get() > 0)
					raftClient.sendForWait(new NormalClose()).await(10 * 1000); // 10s
			} catch (Exception e) {
				// NormalClose 超时或异常不中断停机，确保 finally 中始终停止 raftClient
				logger.error("NormalClose await fail", e);
			} finally {
				try {
					raftClient.stop();
				} catch (Exception e) {
					// stop 失败时复位 activeClose，允许上层重试
					logger.error("RaftClient Stop Fail Agent={}", globalCacheManagerHashIndex, e);
					lock();
					try {
						activeClose = false;
					} finally {
						unlock();
					}
				}
			}
		}

		public final void waitLoginSuccess() {
			var deadline = System.currentTimeMillis() + getConfig().loginTimeout;
			for (; ; ) {
				if (activeClose)
					throw new IllegalStateException("GlobalCacheManagerWithRaftAgent closed.");
				var volatileTmp = loginFuture;
				if (volatileTmp.isDone() || volatileTmp.await(Math.max(1, deadline - System.currentTimeMillis()))) {
					if (activeClose)
						throw new IllegalStateException("GlobalCacheManagerWithRaftAgent closed.");
					try {
						if (Boolean.TRUE.equals(volatileTmp.get()))
							return;
						throw new IllegalStateException("login fail.");
					} catch (IllegalStateException e) {
						throw e;
					} catch (Throwable ignored) {
						// 等待期间raftOnSetLeader执行startNewLogin，cancel旧future并替换；
						// 被替换不是失败，重读最新future继续等待至deadline。
						if (volatileTmp == loginFuture)
							throw new IllegalStateException("login canceled.");
					}
				}
				if (activeClose)
					throw new IllegalStateException("GlobalCacheManagerWithRaftAgent closed.");
				if (System.currentTimeMillis() >= deadline)
					throw new IllegalStateException("login timeout.");
			}
		}

		private void cancelLoginRetryTask() {
			var task = loginRetryTask;
			if (task != null) {
				loginRetryTask = null;
				task.cancel(false);
			}
		}

		private @Nullable TaskCompletionSource<Boolean> startNewLogin() {
			lock();
			try {
				cancelLoginRetryTask();
				var old = loginFuture;
				if (activeClose) {
					old.cancel(true);
					return null;
				}
				var next = new TaskCompletionSource<Boolean>();
				loginFuture = next;
				old.cancel(true); // 先发布新future再取消旧future，避免等待者误抛CancellationException触发FastFail。
				return next;
			} finally {
				unlock();
			}
		}

		private void scheduleLoginRetry(Agent agent, Config config, TaskCompletionSource<Boolean> future, int attempt) {
			if (activeClose || future != loginFuture)
				return;
			long delayMs = attempt <= 1 ? LOGIN_RETRY_MIN_DELAY_MS : LOGIN_RETRY_MAX_DELAY_MS;
			lock();
			try {
				if (activeClose || future != loginFuture)
					return;
				cancelLoginRetryTask();
				loginRetryTask = TaskSpec.ofAction(() -> {
					lock();
					try {
						if (activeClose || future != loginFuture)
							return;
						loginRetryTask = null;
					} finally {
						unlock();
					}
					sendLogin(agent, config, future, attempt + 1);
				}).name("GlobalCacheManagerWithRaftAgent.LoginRetry").scheduleNow(delayMs);
			} finally {
				unlock();
			}
		}

		private void raftOnSetLeader(Agent agent) {
			if (activeClose)
				return;
			var client = agent.getClient();
			if (client == null)
				return;
			var zeze = client.getZeze();
			if (zeze == null)
				return;
			var config = zeze.getConfig();
			var future = startNewLogin();
			if (future == null)
				return;
			sendLogin(agent, config, future, 1);
		}

		private void sendLogin(Agent agent, Config config, TaskCompletionSource<Boolean> future, int attempt) {
			if (activeClose || future != loginFuture)
				return;
			if (loginTimes.get() == 0) {
				var login = new Login();
				login.Argument.setServerId(config.getServerId());
				login.Argument.setGlobalCacheManagerHashIndex(globalCacheManagerHashIndex);
				login.Argument.setDebugMode(Reflect.inDebugMode);
				login.setTimeout(config.getGlobalCacheManagerLoginTimeout());

				agent.send(login, p -> {
					if (activeClose || future != loginFuture)
						return 0;
					var rpc = (Login)p;
					var rc = rpc.getResultCode();
					if (rpc.isTimeout() || rc != 0) {
						logger.error("Login Timeout Or ResultCode != 0. isTimeout={}, Code={}",
								rpc.isTimeout(), rc);
						if (rpc.isTimeout() || rc == GlobalCacheManagerConst.LoginBindSocketFail
								|| rc == Procedure.RaftRetry) {
							scheduleLoginRetry(agent, config, future, attempt);
						}
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
				relogin.Argument.setDebugMode(Reflect.inDebugMode);
				relogin.setTimeout(config.getGlobalCacheManagerLoginTimeout());

				agent.send(relogin, p -> {
					if (activeClose || future != loginFuture)
						return 0;
					var rpc = (ReLogin)p;
					var rc = rpc.getResultCode();
					if (rpc.isTimeout() || rc != 0) {
						logger.error("ReLogin Timeout Or ResultCode != 0. isTimeout={}, Code={}",
								rpc.isTimeout(), rc);
						if (rpc.isTimeout() || rc == GlobalCacheManagerConst.ReLoginBindSocketFail
								|| rc == Procedure.RaftRetry) {
							scheduleLoginRetry(agent, config, future, attempt);
						}
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
