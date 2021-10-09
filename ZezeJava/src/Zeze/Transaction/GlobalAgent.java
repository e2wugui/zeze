package Zeze.Transaction;

import Zeze.Net.*;
import Zeze.Services.*;
import NLog.*;
import Zeze.*;

public final class GlobalAgent {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
	private GlobalClient Client;
	public GlobalClient getClient() {
		return Client;
	}
	private void setClient(GlobalClient value) {
		Client = value;
	}

	public static class Agent {
		private AsyncSocket Socket;
		public final AsyncSocket getSocket() {
			return Socket;
		}
		private void setSocket(AsyncSocket value) {
			Socket = value;
		}
		private TaskCompletionSource<AsyncSocket> Logined;
		public final TaskCompletionSource<AsyncSocket> getLogined() {
			return Logined;
		}
		private void setLogined(TaskCompletionSource<AsyncSocket> value) {
			Logined = value;
		}
		private String Host;
		public final String getHost() {
			return Host;
		}
		private int Port;
		public final int getPort() {
			return Port;
		}
		private Zeze.Util.AtomicLong LoginedTimes = new Util.AtomicLong();
		public final Zeze.Util.AtomicLong getLoginedTimes() {
			return LoginedTimes;
		}
		private int GlobalCacheManagerHashIndex;
		public final int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}

		private long LastErrorTime = 0;
		public static final long ForbitPeriod = 10 * 1000; // 10 seconds

		public Agent(String host, int port, int _GlobalCacheManagerHashIndex) {
			this.Host = host;
			this.Port = port;
			GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
		}

		public final AsyncSocket Connect(GlobalClient client) {
			synchronized (this) {
				// 这个能放到(lock(this)外吗？严格点，放这里更安全。
				// TODO IsCompletedSuccessfully net471之类的没有这个方法，先不管。net471之类的unity要用。
				if (null != getLogined() && getLogined().Task.IsCompletedSuccessfully) {
					return getLogined().Task.Result;
				}

				if (Zeze.Util.Time.getNowUnixMillis() - LastErrorTime < ForbitPeriod) {
					throw new AbortException("GloalAgent.Connect: In Forbit Login Period");
				}

				if (null == getSocket()) {
					setSocket(client.NewClientSocket(getHost(), getPort(), this));
					// 每次新建连接创建future，没并发问题吧，还没仔细考虑。
					setLogined(new TaskCompletionSource<AsyncSocket>());
				}
			}
			// 重新设置一个总超时。整个登录流程有ConnectTimeout,LoginTimeout。
			// 要注意，这个超时发生时，登录流程可能还在进行中。
			// 这里先不清理，下一次进来再次等待（需要确认这样可行）。
			if (false == getLogined().Task.Wait(5000)) {
				synchronized (this) {
					// 并发的等待，简单用个规则：在间隔期内不再设置。
					long now = Zeze.Util.Time.getNowUnixMillis();
					if (now - LastErrorTime > ForbitPeriod) {
						LastErrorTime = now;
					}
				}
				throw new AbortException("GloalAgent.Connect: Login Timeout");
			}
			return getSocket();
		}

		public final void Close() {
			var tmp = getSocket();
			synchronized (this) {
				// 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
				if (null == getSocket()) {
					return;
				}

				setSocket(null); // 正常关闭，先设置这个，以后 OnSocketClose 的时候判断做不同的处理。
			}
			if (getLogined().Task.IsCompletedSuccessfully) {
				var normalClose = new GlobalCacheManager.NormalClose();
				var future = new TaskCompletionSource<Integer>();
				normalClose.Send(tmp, (_) -> {
							if (normalClose.isTimeout()) {
								future.SetResult(-100); // 关闭错误就不抛异常了。
							}
							else {
								future.SetResult(normalClose.getResultCode());
								if (normalClose.getResultCode() != 0) {
									logger.Error("GlobalAgent:NormalClose ResultCode={0}", normalClose.getResultCode());
								}
							}
							return 0;
				});
				future.Task.Wait();
			}
			getLogined().TrySetException(new RuntimeException("GlobalAgent.Close")); // 这个，，，，
			tmp.close();
		}

		public final void OnSocketClose(GlobalClient client, RuntimeException ex) {
			synchronized (this) {
				if (null == getSocket()) {
					// active close
					return;
				}
				setSocket(null);
			}
			if (getLogined().Task.IsCompletedSuccessfully) {
				for (var database : client.getZeze().getDatabases().values()) {
					for (var table : database.Tables) {
						table.ReduceInvalidAllLocalOnly(getGlobalCacheManagerHashIndex());
					}
				}
				client.getZeze().CheckpointRun();
			}
			getLogined().TrySetException(ex); // 连接关闭，这个继续保持。仅在Connect里面需要时创建。
		}
	}

	public Agent[] Agents;

	public int GetGlobalCacheManagerHashIndex(GlobalCacheManager.GlobalTableKey gkey) {
		return gkey.hashCode() % Agents.length;
	}

	public int Acquire(GlobalCacheManager.GlobalTableKey gkey, int state) {
		if (null != getClient()) {
			var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
			var socket = agent.Connect(getClient());

			// 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
			// 一个请求异常不关闭连接，尝试继续工作。
			GlobalCacheManager.Acquire rpc = new GlobalCacheManager.Acquire(gkey, state);
			rpc.SendForWait(socket, 12000).Task.Wait();
			/*
			if (rpc.ResultCode != 0) // 这个用来跟踪调试，正常流程使用Result.State检查结果。
			{
			    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
			}
			*/
			switch (rpc.getResultCode()) {
				case GlobalCacheManager.AcquireModifyFaild:
				case GlobalCacheManager.AcquireShareFaild:
					throw new AbortException("GlobalAgent.Acquire Faild");
			}
			return rpc.getResult().getState();
		}
		logger.Debug("Acquire local ++++++");
		return state;
	}

	public int ProcessReduceRequest(Protocol p) {
		GlobalCacheManager.Reduce rpc = (GlobalCacheManager.Reduce)p;
		switch (rpc.getArgument().getState()) {
			case GlobalCacheManager.StateInvalid:
				return getZeze().GetTable(rpc.getArgument().getGlobalTableKey().getTableName()).ReduceInvalid(rpc);

			case GlobalCacheManager.StateShare:
				return getZeze().GetTable(rpc.getArgument().getGlobalTableKey().getTableName()).ReduceShare(rpc);

			default:
				rpc.setResult(rpc.getArgument());
				rpc.SendResultCode(GlobalCacheManager.ReduceErrorState);
				return 0;
		}
	}

	private Application Zeze;
	public Application getZeze() {
		return Zeze;
	}

	public GlobalAgent(Application app) {
		Zeze = app;
	}

	public void Start(String hostNameOrAddress, int port) {
		synchronized (this) {
			if (null != getClient()) {
				return;
			}

			setClient(new GlobalClient(this, getZeze()));
			// Zeze-App 自动启用持久化的全局唯一的Rpc.SessionId生成器。
			getClient().setSessionIdGenerator(getZeze().getServiceManagerAgent().GetAutoKey(getClient().getName()).Next);

			getClient().AddFactoryHandle((new GlobalCacheManager.Reduce()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Reduce(), Handle = ProcessReduceRequest, NoProcedure = true});
			getClient().AddFactoryHandle((new GlobalCacheManager.Acquire()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Acquire(), NoProcedure = true});
			getClient().AddFactoryHandle((new GlobalCacheManager.Login()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Login(), NoProcedure = true});
			getClient().AddFactoryHandle((new GlobalCacheManager.ReLogin()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.ReLogin(), NoProcedure = true});
			getClient().AddFactoryHandle((new GlobalCacheManager.NormalClose()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.NormalClose(), NoProcedure = true});

			var globals = hostNameOrAddress.split("[;]", -1);
			Agents = new Agent[globals.Length];
			for (int i = 0; i < globals.Length; ++i) {
				var hp = globals[i].split("[:]", -1);
				if (hp.Length > 1) {
					Agents[i] = new Agent(hp[0], Integer.parseInt(hp[1]), i);
				}
				else {
					Agents[i] = new Agent(hp[0], port, i);
				}
			}
			for (var agent : Agents) {
				try {
					agent.Connect(getClient());
				}
				catch (RuntimeException ex) {
					// 允许部分GlobalCacheManager连接错误时，继续启动程序，虽然后续相关事务都会失败。
					logger.Error(ex, "GlobalAgent.Connect");
				}
			}
		}
	}

	public void Stop() {
		synchronized (this) {
			if (null == getClient()) {
				return;
			}

			for (var agent : Agents) {
				agent.Close();
			}

			getClient().Stop();
			setClient(null);
		}
	}
}