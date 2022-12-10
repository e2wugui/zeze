package Zeze.Services;

import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister;
import Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.Register;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Builtin.ServiceManagerWithRaft.UnRegister;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Builtin.ServiceManagerWithRaft.Update;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.UniqueRequestId;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Action0;
import Zeze.Util.Func0;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft {
	private static final Logger logger = LogManager.getLogger(ServiceManagerWithRaft.class);
	private final Rocks rocks;

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf, Zeze.Config config,
								  boolean RocksDbWriteOptionSync) throws Throwable {
		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync, SMServer::new);
		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().Start();

	}

	/**
	 * 所有Raft网络层收到的请求和Rpc的结果，全部加锁，直接运行。
	 * 这样整个程序就单线程化了。
	 */
	public static class SMServer extends Zeze.Raft.Server {
		public SMServer(Zeze.Raft.Raft raft, String name, Zeze.Config config) throws Throwable {
			super(raft, name, config);
		}

		@Override
		public synchronized <P extends Protocol<?>> void dispatchRaftRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) throws Throwable {
			Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, DispatchMode.Direct);
		}

		@Override
		public synchronized void dispatchRaftRequest(UniqueRequestId key, Func0<?> func, String name, Action0 cancel, DispatchMode mode) {
			try {
				func.call();
			} catch (Throwable ex) {
				logger.error("impossible!", ex);
			}
		}
	}
	@Override
	protected long ProcessAllocateIdRequest(AllocateId r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessOfflineRegisterRequest(OfflineRegister r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessReadyServiceListRequest(ReadyServiceList r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessRegisterRequest(Register r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessUnRegisterRequest(UnRegister r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessUpdateRequest(Update r) throws Throwable {
		return 0;
	}
}
