package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicLong;
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
import Zeze.Services.GlobalCacheManagerServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GlobalAgent implements IGlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalAgent.class);

	public static final class Agent extends GlobalAgentBase {
		private final Connector connector;
		private final AtomicLong LoginTimes = new AtomicLong();
		private final int GlobalCacheManagerHashIndex;
		private boolean ActiveClose;
		private volatile long LastErrorTime;

		public Agent(GlobalClient client, String host, int port, int _GlobalCacheManagerHashIndex) {
			connector = new Zeze.Net.Connector(host, port, true);
			connector.UserState = this;
			GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			connector.setMaxReconnectDelay(AchillesHeelConfig.ReconnectTimer);
			client.getConfig().AddConnector(connector);
		}

		@Override
		public void keepAlive() {
			if (null == getConfig())
				return; // not login

			new KeepAlive().Send(connector.TryGetReadySocket(), rpc -> {
				if (!rpc.isTimeout() && rpc.getResultCode() == 0)
					setActiveTime(System.currentTimeMillis()); // KeepAlive.Response
				return 0;
			}, getConfig().KeepAliveTimeout);
		}

		public AtomicLong getLoginTimes() {
			return LoginTimes;
		}

		public int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}

		private void ThrowException(String msg, Throwable cause) {
			var txn = Transaction.getCurrent();
			if (txn != null)
				txn.ThrowAbort(msg, cause);
			throw new RuntimeException(msg, cause);
		}

		void verifyFastFail() {
			synchronized (this) {
				if (System.currentTimeMillis() - LastErrorTime < getConfig().ServerFastErrorPeriod)
					ThrowException("GlobalAgent In FastErrorPeriod", null); // abort
				// else continue
			}
		}

		void setFastFail() {
			var now = System.currentTimeMillis();
			synchronized (this) {
				if (now - LastErrorTime > getConfig().ServerFastErrorPeriod)
					LastErrorTime = now;
			}
		}

		public AsyncSocket Connect() {
			try {
				var so = connector.TryGetReadySocket();
				if (so != null)
					return so;

				return connector.WaitReady();
			} catch (Throwable abort) {
				setFastFail();
				ThrowException("GlobalAgent Login Failed", abort);
			}
			return null; // never got here.
		}

		public void Close() {
			try {
				synchronized (this) {
					// 简单保护一下重复主动调用 Close
					if (ActiveClose)
						return;
					ActiveClose = true;
				}
				var ready = connector.TryGetReadySocket();
				if (ready != null)
					new NormalClose().SendForWait(ready).await();
			} finally {
				connector.Stop(); // 正常关闭，先设置这个，以后 OnSocketClose 的时候判断做不同的处理。
			}
		}
	}

	private final Application Zeze;
	private GlobalClient Client;
	public Agent[] Agents;

	public GlobalAgent(Application app) {
		Zeze = app;
	}

	public Application getZeze() {
		return Zeze;
	}

	public GlobalClient getClient() {
		return Client;
	}

	@Override
	public int GetGlobalCacheManagerHashIndex(Binary gkey) {
		return gkey.hashCode() % Agents.length;
	}

	@Override
	public void close() {
		try {
			Stop();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AcquireResult Acquire(Binary gkey, int state, boolean fresh) {
		if (Client != null) {
			var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
			agent.verifyFastFail();
			var socket = agent.Connect();
			// 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
			// 一个请求异常不关闭连接，尝试继续工作。
			var rpc = new Acquire(gkey, state);
			if (fresh)
				rpc.setResultCode(GlobalCacheManagerServer.AcquireFreshSource);
			try {
				rpc.SendForWait(socket, agent.getConfig().AcquireTimeout).get();
			} catch (Throwable e) {
				agent.setFastFail(); // 一般是超时失败，此时必须进入快速失败模式。
				var trans = Transaction.getCurrent();
				if (trans == null)
					throw new GoBackZeze("Acquire", e);
				trans.ThrowAbort("Acquire", e);
				// never got here
			}
			/*
			if (rpc.ResultCode != 0) // 这个用来跟踪调试，正常流程使用Result.State检查结果。
			{
			    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
			}
			*/
			if (!rpc.isTimeout())
				agent.setActiveTime(System.currentTimeMillis()); // Acquire.Response

			if (rpc.getResultCode() == GlobalCacheManagerServer.AcquireModifyFailed
					|| rpc.getResultCode() == GlobalCacheManagerServer.AcquireShareFailed) {
				var trans = Transaction.getCurrent();
				if (trans == null)
					throw new GoBackZeze("GlobalAgent.Acquire Failed");
				trans.ThrowAbort("GlobalAgent.Acquire Failed", null);
				// never got here
			}
			return new AcquireResult(rpc.getResultCode(), rpc.Result.State);
		}
		logger.debug("Acquire local ++++++");
		return new AcquireResult(0, state);
	}

	public int ProcessReduceRequest(Reduce rpc) {
		switch (rpc.Argument.State) {
		case GlobalCacheManagerServer.StateInvalid: {
			var bb = ByteBuffer.Wrap(rpc.Argument.GlobalKey);
			var tableId = bb.ReadInt4();
			var table1 = Zeze.GetTable(tableId);
			if (null == table1) {
				logger.warn("ReduceInvalid Table Not Found={},ServerId={}",
						tableId, Zeze.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
				rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
				rpc.SendResultCode(0);
				return 0;
			}
			return table1.ReduceInvalid(rpc, bb);
		}
		case GlobalCacheManagerServer.StateShare: {
			var bb = ByteBuffer.Wrap(rpc.Argument.GlobalKey);
			var tableId = bb.ReadInt4();
			var table = Zeze.GetTable(tableId);
			if (table == null) {
				logger.warn("ReduceShare Table Not Found={},ServerId={}",
						tableId, Zeze.getConfig().getServerId());
				// 本地没有找到表格看作成功。
				rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
				rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
				rpc.SendResultCode(0);
				return 0;
			}
			return table.ReduceShare(rpc, bb);
		}
		default:
			rpc.Result = rpc.Argument;
			rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
			return 0;
		}
	}

	public synchronized void Start(String[] hostNameOrAddress, int port) throws Throwable {
		if (Client != null)
			return;

		Client = new GlobalClient(this, Zeze);
		Client.AddFactoryHandle(Reduce.TypeId_,
				new Service.ProtocolFactoryHandle<>(Reduce::new, this::ProcessReduceRequest, TransactionLevel.None));
		Client.AddFactoryHandle(Acquire.TypeId_,
				new Service.ProtocolFactoryHandle<>(Acquire::new, null, TransactionLevel.None));
		Client.AddFactoryHandle(Login.TypeId_,
				new Service.ProtocolFactoryHandle<>(Login::new, null, TransactionLevel.None));
		Client.AddFactoryHandle(ReLogin.TypeId_,
				new Service.ProtocolFactoryHandle<>(ReLogin::new, null, TransactionLevel.None));
		Client.AddFactoryHandle(NormalClose.TypeId_,
				new Service.ProtocolFactoryHandle<>(NormalClose::new, null, TransactionLevel.None));
		Client.AddFactoryHandle(KeepAlive.TypeId_,
				new Service.ProtocolFactoryHandle<>(KeepAlive::new, null, TransactionLevel.None));

		Agents = new Agent[hostNameOrAddress.length];
		for (int i = 0; i < hostNameOrAddress.length; i++) {
			var hp = hostNameOrAddress[i].split(":", -1);
			Agents[i] = new Agent(Client, hp[0], hp.length > 1 ? Integer.parseInt(hp[1]) : port, i);
		}

		Client.Start();

		for (var agent : Agents) {
			try {
				agent.Connect();
			} catch (Throwable ex) {
				// 允许部分GlobalCacheManager连接错误时，继续启动程序，虽然后续相关事务都会失败。
				logger.error("GlobalAgent.Connect", ex);
			}
		}
	}

	public synchronized void Stop() throws Throwable {
		if (Client == null)
			return;
		for (var agent : Agents)
			agent.Close();
		Client.Stop();
		Client = null;
	}
}
