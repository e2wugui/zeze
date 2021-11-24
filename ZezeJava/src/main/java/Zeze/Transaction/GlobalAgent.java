package Zeze.Transaction;

import Zeze.Net.*;
import Zeze.Services.*;
import Zeze.Util.TaskCompletionSource;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
		private AtomicLong LoginedTimes = new AtomicLong();
		public final AtomicLong getLoginedTimes() {
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
				if (null != Logined) {
					try {
						return Logined.get(0, TimeUnit.MILLISECONDS);
					}
					catch (TimeoutException skipAndContinue) {
						// 这里为什么 skipAndContinue, 忘了！
					}
					catch (InterruptedException | ExecutionException abort) {
						Transaction.getCurrent().ThrowAbort(null, abort);
					}
				}

				if (System.currentTimeMillis() - LastErrorTime < ForbitPeriod) {
					Transaction.getCurrent().ThrowAbort("GloalAgent.Connect: In Forbit Period", null);
				}

				if (null == getSocket()) {
					setLogined(new TaskCompletionSource<AsyncSocket>());
					setSocket(client.NewClientSocket(getHost(), getPort(), this,null));
					// 每次新建连接创建future，没并发问题吧，还没仔细考虑。
				}
			}

			// 重新设置一个总超时。整个登录流程有ConnectTimeout,LoginTimeout。
			// 要注意，这个超时发生时，登录流程可能还在进行中。
			// 这里先不清理，下一次进来再次等待（需要确认这样可行）。
			try {
				return Logined.get(5000, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException | ExecutionException | TimeoutException abort) {
				synchronized (this) {
					// 并发的等待，简单用个规则：在间隔期内不再设置。
					long now = System.currentTimeMillis();
					if (now - LastErrorTime > ForbitPeriod) {
						LastErrorTime = now;
					}
				}
				Transaction.getCurrent().ThrowAbort("GloalAgent Login Failed", abort);
			}
			return null; // never go here.
		}

		public final void Close() {
			var tmp = getSocket();
			try {
				synchronized (this) {
					// 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
					if (null == getSocket()) {
						return;
					}

					setSocket(null); // 正常关闭，先设置这个，以后 OnSocketClose 的时候判断做不同的处理。
				}

				var normalClose = new NormalClose();
				var future = new TaskCompletionSource<Long>();
				normalClose.Send(tmp, (ThisRpc) -> {
						if (normalClose.isTimeout()) {
							future.SetResult(-100L); // 关闭错误就不抛异常了。
						}
						else {
							future.SetResult(normalClose.getResultCode());
							if (normalClose.getResultCode() != 0) {
								logger.error("GlobalAgent:NormalClose ResultCode={}", normalClose.getResultCode());
							}
						}
						return 0;
				});
				future.Wait();
			}
			finally {
				if (null != tmp)
					tmp.close();
				// 关闭时，让等待Login的线程全部失败。
				Logined.TrySetException(new RuntimeException("GlobalAgent.Close"));
			}
		}

		public final void OnSocketClose(GlobalClient client, Throwable ex) {
			synchronized (this) {
				if (null == getSocket()) {
					// active close
					return;
				}
				setSocket(null);
			}
			for (var database : client.getZeze().getDatabases().values()) {
				for (var table : database.getTables()) {
					table.ReduceInvalidAllLocalOnly(getGlobalCacheManagerHashIndex());
				}
			}
			client.getZeze().CheckpointRun();
			getLogined().TrySetException(ex); // 连接关闭，这个继续保持。仅在Connect里面需要时创建。
		}
	}

	public Agent[] Agents;

	public int GetGlobalCacheManagerHashIndex(GlobalTableKey gkey) {
		return gkey.hashCode() % Agents.length;
	}

	public Acquire Acquire(GlobalTableKey gkey, int state) {
		if (null != getClient()) {
			var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
			var socket = agent.Connect(getClient());

			// 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
			// 一个请求异常不关闭连接，尝试继续工作。
			var rpc = new Acquire(gkey, state);
			try {
				rpc.SendForWait(socket, 12000).get();
			} catch (InterruptedException | ExecutionException e) {
				Transaction.getCurrent().ThrowAbort("Acquire", e);
			}
			/*
			if (rpc.ResultCode != 0) // 这个用来跟踪调试，正常流程使用Result.State检查结果。
			{
			    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
			}
			*/
			if (rpc.getResultCode() == GlobalCacheManagerServer.AcquireModifyFaild
					|| rpc.getResultCode() == GlobalCacheManagerServer.AcquireShareFaild) {
				Transaction.getCurrent().ThrowAbort("GlobalAgent.Acquire Faild", null);
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

	private Application Zeze;
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

			setClient(new GlobalClient(this, getZeze()));

			getClient().AddFactoryHandle(
					(new Reduce()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new Reduce(),
							(p) -> ProcessReduceRequest(p),
							true));

			getClient().AddFactoryHandle(
					(new Acquire()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new Acquire(),
							null,
							true));

			getClient().AddFactoryHandle(
					(new Login()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new Login(),
							null,
							true));

			getClient().AddFactoryHandle(
					(new ReLogin()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new ReLogin(),
							null,
							true));

			getClient().AddFactoryHandle(
					(new NormalClose()).getTypeId(),
					new Service.ProtocolFactoryHandle(
							() -> new NormalClose(),
							null,
							true));

			var globals = hostNameOrAddress.split("[;]", -1);
			Agents = new Agent[globals.length];
			for (int i = 0; i < globals.length; ++i) {
				var hp = globals[i].split("[:]", -1);
				if (hp.length > 1) {
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
				catch (Throwable ex) {
					// 允许部分GlobalCacheManager连接错误时，继续启动程序，虽然后续相关事务都会失败。
					logger.error("GlobalAgent.Connect", ex);
				}
			}
		}
	}

	public void Stop() throws Throwable {
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