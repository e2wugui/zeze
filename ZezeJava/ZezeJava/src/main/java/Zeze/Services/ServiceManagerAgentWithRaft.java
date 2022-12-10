package Zeze.Services;

import Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify;
import Zeze.Builtin.ServiceManagerWithRaft.Register;
import Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit;
import Zeze.Builtin.ServiceManagerWithRaft.UnRegister;
import Zeze.Builtin.ServiceManagerWithRaft.Update;

public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	@Override
	protected long ProcessCommitServiceListRequest(CommitServiceList r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessNotifyServiceListRequest(NotifyServiceList r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessOfflineNotifyRequest(OfflineNotify r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessRegisterRequest(Register r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessSubscribeFirstCommitRequest(SubscribeFirstCommit r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessUnRegisterRequest(UnRegister r) throws Throwable {
		return 0;
	}

	@Override
	protected long ProcessUpdateRequest(Update r) throws Throwable {
		return 0;
	}
}
