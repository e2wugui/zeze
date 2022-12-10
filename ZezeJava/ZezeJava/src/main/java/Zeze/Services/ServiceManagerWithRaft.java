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
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;

public class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft {
	private final Rocks rocks;

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf, Zeze.Config config,
								  boolean RocksDbWriteOptionSync) throws Throwable {
		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync);
		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().Start();

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
