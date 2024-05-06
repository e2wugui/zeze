package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.AchillesHeelConfig;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.NormalClose;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GlobalAgent extends ReentrantLock implements IGlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalAgent.class);

	public static final class Agent extends GlobalAgentBase {
		private final Connector connector;
		private final AtomicLong loginTimes = new AtomicLong();
		private boolean activeClose;
		private volatile long lastErrorTime;

		public Agent(Application zeze, GlobalClient client, String host, int port, int _GlobalCacheManagerHashIndex) {
			super(zeze);
			connector = new Connector(host, port, true);
			connector.userState = this;
			super.globalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			connector.setMaxReconnectDelay(AchillesHeelConfig.reconnectTimer);
			client.getConfig().addConnector(connector);
		}

		@Override
		protected void cancelPending() {
			// 非Raft版本没有Pending，不需要执行操作。以后如果实现了Pending，需要实现Cancel。
		}

		@Override
		public void keepAlive() {
			if (null == getConfig())
				return; // not login

			new KeepAlive().Send(connector.TryGetReadySocket(), rpc -> {
				if (!rpc.isTimeout() && rpc.getResultCode() == 0)
					setActiveTime(System.currentTimeMillis()); // KeepAlive.Response
				return 0;
			}, getConfig().keepAliveTimeout);
		}

		public AtomicLong getLoginTimes() {
			return loginTimes;
		}

		public int getGlobalCacheManagerHashIndex() {
			return globalCacheManagerHashIndex;
		}

		private static void throwException(String msg, Throwable cause) {
			var txn = Transaction.getCurrent();
			if (txn != null)
				txn.throwAbort(msg, cause);
			throw new IllegalStateException(msg, cause);
		}

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

		public AsyncSocket connect() {
			try {
				var so = connector.TryGetReadySocket();
				if (so != null)
					return so;

				return connector.WaitReady();
			} catch (Throwable abort) { // rethrow RuntimeException
				setFastFail();
				throwException("GlobalAgent Login Failed", abort);
			}
			return null; // never run here
		}

		public void close() {
			try {
				lock();
				try {
					// 简单保护一下重复主动调用 Close
					if (activeClose)
						return;
					activeClose = true;
				} finally {
					unlock();
				}
				var ready = connector.TryGetReadySocket();
				if (ready != null)
					new NormalClose().SendForWait(ready).await();
			} finally {
				connector.stop(); // 正常关闭，先设置这个，以后 OnSocketClose 的时候判断做不同的处理。
			}
		}
	}

	private final Application zeze;
	private final GlobalClient client;
	private final Agent[] agents;

	public Agent[] getAgents() {
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

	public GlobalAgent(Application app, String[] hostNameOrAddress, int port) {
		zeze = app;

		client = new GlobalClient(this, zeze);

		client.AddFactoryHandle(Reduce.TypeId_, new Service.ProtocolFactoryHandle<>(
				Reduce::new, this::processReduceRequest, TransactionLevel.None, DispatchMode.Critical));
		client.AddFactoryHandle(Acquire.TypeId_, new Service.ProtocolFactoryHandle<>(
				Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
				Login::new, null, TransactionLevel.None, DispatchMode.Critical));
		client.AddFactoryHandle(ReLogin.TypeId_, new Service.ProtocolFactoryHandle<>(
				ReLogin::new, null, TransactionLevel.None, DispatchMode.Critical));
		client.AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
				NormalClose::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));

		agents = new Agent[hostNameOrAddress.length];
		for (int i = 0; i < hostNameOrAddress.length; i++) {
			var hp = hostNameOrAddress[i].split("_", -1);
			agents[i] = new Agent(zeze, client, hp[0], hp.length > 1 ? Integer.parseInt(hp[1]) : port, i);
		}
	}

	public Application getZeze() {
		return zeze;
	}

	public GlobalClient getClient() {
		return client;
	}

	@Override
	public int getGlobalCacheManagerHashIndex(@NotNull Binary gkey) {
		return gkey.hashCode() % agents.length;
	}

	@Override
	public void close() {
		try {
			stop();
		} catch (Exception e) {
			Task.forceThrow(e);
		}
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
		var socket = agent.connect();
		// 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
		// 一个请求异常不关闭连接，尝试继续工作。
		var rpc = new Acquire(gkey, state);
		if (fresh)
			rpc.setResultCode(GlobalCacheManagerConst.AcquireFreshSource);
		try {
			if (noWait) {
				rpc.Send(socket);
				return null;
			}
			rpc.SendForWait(socket, agent.getConfig().acquireTimeout).get();
		} catch (Throwable e) { // rethrow Error or RuntimeException
			agent.setFastFail(); // 一般是超时失败，此时必须进入快速失败模式。
			var trans = Transaction.getCurrent();
			if (trans == null)
				throw new GoBackZeze("Acquire", e);
			trans.throwAbort("Acquire", e);
			// never run here
		}
		/*
		if (rpc.ResultCode != 0) { // 这个用来跟踪调试，正常流程使用Result.State检查结果。
			logger.warn("Acquire ResultCode={} {}", rpc.ResultCode, rpc.Result);
		}
		*/
		if (!rpc.isTimeout())
			agent.setActiveTime(System.currentTimeMillis()); // Acquire.Response

		if (rpc.getResultCode() == GlobalCacheManagerConst.AcquireModifyFailed
				|| rpc.getResultCode() == GlobalCacheManagerConst.AcquireShareFailed) {
			var tableId = (int)ByteBuffer.ToLong(gkey.bytesUnsafe(), gkey.getOffset(), Math.min(4, gkey.size()));
			var trans = Transaction.getCurrent();
			if (trans == null)
				throw new GoBackZeze("GlobalAgent.Acquire Failed: " + gkey + " tableId=" + tableId);
			trans.throwAbort("GlobalAgent.Acquire Failed: " + gkey + " tableId=" + tableId, null);
			// never run here
		}
		var rc = rpc.getResultCode();
		state = rpc.Result.state;
		return //rc == 0 ? AcquireResult.getSuccessResult(state) :
				new AcquireResult(rc, state, rpc.Result.reducedTid);
	}

	public int processReduceRequest(Reduce rpc) {
		switch (rpc.Argument.state) {
		case GlobalCacheManagerConst.StateInvalid: {
			var bb = ByteBuffer.Wrap(rpc.Argument.globalKey);
			var tableId = bb.ReadInt4();
			var table1 = zeze.getTable(tableId);
			if (null == table1) {
				logger.warn("ReduceInvalid Table Not Found={},ServerId={}",
						tableId, zeze.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.globalKey = rpc.Argument.globalKey;
				rpc.Result.state = GlobalCacheManagerConst.StateInvalid;
				rpc.SendResultCode(0);
				return 0;
			}
			return table1.reduceInvalid(rpc, bb);
		}
		case GlobalCacheManagerConst.StateShare: {
			var bb = ByteBuffer.Wrap(rpc.Argument.globalKey);
			var tableId = bb.ReadInt4();
			var table = zeze.getTable(tableId);
			if (table == null) {
				logger.warn("ReduceShare Table Not Found={},ServerId={}",
						tableId, zeze.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.globalKey = rpc.Argument.globalKey;
				rpc.Result.state = GlobalCacheManagerConst.StateInvalid;
				rpc.SendResultCode(0);
				return 0;
			}
			return table.reduceShare(rpc, bb);
		}
		default:
			rpc.Result = rpc.Argument;
			rpc.SendResultCode(GlobalCacheManagerConst.ReduceErrorState);
			return 0;
		}
	}

	public void start() throws Exception {
		lock();
		try {
			client.start();

			for (var agent : agents) {
				try {
					agent.connect();
				} catch (Throwable ex) { // logger.error
					// 允许部分GlobalCacheManager连接错误时，继续启动程序，虽然后续相关事务都会失败。
					logger.error("GlobalAgent.Connect", ex);
				}
			}
		} finally {
			unlock();
		}
	}

	public void stop() throws Exception {
		lock();
		try {
			for (var agent : agents)
				agent.close();
			client.stop();
		} finally {
			unlock();
		}
	}
}
