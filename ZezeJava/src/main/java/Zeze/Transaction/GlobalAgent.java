package Zeze.Transaction;

import Zeze.Net.*;
import Zeze.Services.*;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import Zeze.*;
import Zeze.Services.GlobalCacheManager.*;

public final class GlobalAgent {
	private static final Logger logger = LogManager.getLogger(GlobalAgent.class);

	private GlobalClient Client;
	public GlobalClient getClient() {
		return Client;
	}

	public static class Agent {
		Zeze.Net.Connector connector;
		private final AtomicLong LoginTimes = new AtomicLong();
		public final AtomicLong getLoginTimes() {
			return LoginTimes;
		}
		private final int GlobalCacheManagerHashIndex;
		public final int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}
		private boolean ActiveClose = false;
		private volatile long LastErrorTime = 0;
		private final static long FastErrorPeriod = 10 * 1000; // 10 seconds

		public Agent(GlobalClient client, String host, int port, int _GlobalCacheManagerHashIndex) throws Throwable {
			connector = new Zeze.Net.Connector(host, port, true);
			connector.UserState = this;
			GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
			client.getConfig().AddConnector(connector);
		}

		private void ThrowException(String msg, Throwable cause) {
			var txn = Transaction.getCurrent();
			if (null != txn)
				txn.ThrowAbort(msg, cause);
			throw new RuntimeException(msg, cause);
		}

		public final AsyncSocket Connect() {
			try {
				var so = connector.TryGetReadySocket();
				if (null != so)
					return so;

				synchronized (this) {
					if (System.currentTimeMillis() - LastErrorTime < FastErrorPeriod)
						ThrowException("GlobalAgent In FastErrorPeriod", null); // abort
					// else continue
				}

				return connector.WaitReady();
			} catch (Throwable abort) {
				final var now = System.currentTimeMillis();
				synchronized (this) {
					if (now - LastErrorTime > FastErrorPeriod)
						LastErrorTime = now;
				}
				ThrowException("GlobalAgent Login Failed", abort);
			}
			return null; // never got here.
		}

		public final void Close() {
			try {
				synchronized (this) {
					// 简单保护一下重复主动调用 Close
					if (ActiveClose) {
						return;
					}
					ActiveClose = true;
				}
				var ready = connector.TryGetReadySocket();
				if (null != ready)
					new NormalClose().SendForWait(ready).Wait();
			}
			finally {
				connector.Stop(); // 正常关闭，先设置这个，以后 OnSocketClose 的时候判断做不同的处理。
			}
		}

		public final void OnSocketClose(GlobalClient client, Throwable ignoredEx) {
			synchronized (this) {
				if (ActiveClose)
					return; // Connector 的状态在它自己的回调里面处理。
			}

			if (connector.isHandshakeDone()) {
				for (var database : client.getZeze().getDatabases().values()) {
					for (var table : database.getTables()) {
						table.ReduceInvalidAllLocalOnly(getGlobalCacheManagerHashIndex());
					}
				}
				client.getZeze().CheckpointRun();
			}
		}
	}

	public Agent[] Agents;

	public int GetGlobalCacheManagerHashIndex(GlobalTableKey gkey) {
		return gkey.hashCode() % Agents.length;
	}

	public Acquire Acquire(GlobalTableKey gkey, int state) {
		if (null != getClient()) {
			var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
			var socket = agent.Connect();

			// 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
			// 一个请求异常不关闭连接，尝试继续工作。
			var rpc = new Acquire(gkey, state);
			try {
				rpc.SendForWait(socket, 12000).get();
			} catch (InterruptedException | ExecutionException e) {
				//noinspection ConstantConditions
				Transaction.getCurrent().ThrowAbort("Acquire", e);
			}
			/*
			if (rpc.ResultCode != 0) // 这个用来跟踪调试，正常流程使用Result.State检查结果。
			{
			    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
			}
			*/
			if (rpc.getResultCode() == GlobalCacheManagerServer.AcquireModifyFailed
					|| rpc.getResultCode() == GlobalCacheManagerServer.AcquireShareFailed) {
				//noinspection ConstantConditions
				Transaction.getCurrent().ThrowAbort("GlobalAgent.Acquire Failed", null);
			}
			return rpc;
		}
		logger.debug("Acquire local ++++++");
		var result = new Acquire();
		result.Result.State = state;
		return result;
	}

	public int ProcessReduceRequest(Protocol p) {
		var rpc = (Reduce)p;
		switch (rpc.Argument.State) {
			case GlobalCacheManagerServer.StateInvalid:
				var table1 = getZeze().GetTable(rpc.Argument.GlobalTableKey.TableName);
				if (null == table1) {
					logger.warn("ReduceInvalid Table Not Found={},ServerId={}",
							rpc.Argument.GlobalTableKey.TableName, getZeze().getConfig().getServerId());
					// 本地没有找到表格看作成功。
					rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
					rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
					rpc.SendResultCode(0);
					return 0;
				}
				return table1.ReduceInvalid(rpc);

			case GlobalCacheManagerServer.StateShare:
				var table2 = Zeze.GetTable(rpc.Argument.GlobalTableKey.TableName);
				if (table2 == null)
				{
					logger.warn("ReduceShare Table Not Found={},ServerId={}",
							rpc.Argument.GlobalTableKey.TableName, getZeze().getConfig().getServerId());

					// 本地没有找到表格看作成功。
					rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
					rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
					rpc.SendResultCode(0);
					return 0;
				}
				return table2.ReduceShare(rpc);

			default:
				rpc.Result = rpc.Argument;
				rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
				return 0;
		}
	}

	private final Application Zeze;
	public Application getZeze() {
		return Zeze;
	}

	public GlobalAgent(Application app) {
		Zeze = app;
	}

	public void Start(String hostNameOrAddress, int port) throws Throwable {
		synchronized (this) {
			if (null != getClient()) {
				return;
			}

			Client = new GlobalClient(this, getZeze());

			getClient().AddFactoryHandle(
					(new Reduce()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new Reduce(),
							this::ProcessReduceRequest,
							TransactionLevel.None));

			getClient().AddFactoryHandle(
					(new Acquire()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new Acquire(),
							null,
							TransactionLevel.None));

			getClient().AddFactoryHandle(
					(new Login()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new Login(),
							null,
							TransactionLevel.None));

			getClient().AddFactoryHandle(
					(new ReLogin()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new ReLogin(),
							null,
							TransactionLevel.None));

			getClient().AddFactoryHandle(
					(new NormalClose()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new NormalClose(),
							null,
							TransactionLevel.None));

			var globals = hostNameOrAddress.split("[;]", -1);
			Agents = new Agent[globals.length];
			for (int i = 0; i < globals.length; ++i) {
				var hp = globals[i].split("[:]", -1);
				if (hp.length > 1) {
					Agents[i] = new Agent(Client, hp[0], Integer.parseInt(hp[1]), i);
				}
				else {
					Agents[i] = new Agent(Client, hp[0], port, i);
				}
			}

			Client.Start();

			for (var agent : Agents) {
				try {
					agent.Connect();
				}
				catch (Throwable ex) {
					// 允许部分GlobalCacheManager连接错误时，继续启动程序，虽然后续相关事务都会失败。
					logger.error("GlobalAgent.Connect", ex);
				}
			}
		}
	}

	public void Stop() throws Throwable {
		synchronized (this) {
			if (null == Client) {
				return;
			}
			for (var agent : Agents) {
				agent.Close();
			}
			Client.Stop();
		}
	}
}
