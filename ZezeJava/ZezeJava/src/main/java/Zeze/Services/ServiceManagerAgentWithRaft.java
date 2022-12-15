package Zeze.Services;

import java.io.IOException;
import Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify;
import Zeze.Builtin.ServiceManagerWithRaft.Register;
import Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit;
import Zeze.Builtin.ServiceManagerWithRaft.UnRegister;
import Zeze.Builtin.ServiceManagerWithRaft.Update;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;

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

	@Override
	protected boolean sendReadyList(String serviceName, long serialId) {
		return false;
	}

	@Override
	protected void allocate(AutoKey autoKey) {

	}

	@Override
	public BServiceInfo registerService(BServiceInfo info) {
		return null;
	}

	@Override
	public BServiceInfo updateService(BServiceInfo info) {
		return null;
	}

	@Override
	public void unRegisterService(BServiceInfo info) {

	}

	@Override
	public SubscribeState subscribeService(BSubscribeInfo info) {
		return null;
	}

	@Override
	public void unSubscribeService(String serviceName) {

	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		return false;
	}

	@Override
	public void offlineRegister(BOfflineNotify argument) {

	}

	@Override
	public void close() throws IOException {

	}
}
