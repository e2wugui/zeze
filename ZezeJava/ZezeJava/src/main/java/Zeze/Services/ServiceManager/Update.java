package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class Update extends Rpc<ServiceInfo, EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash32(Update.class.getName());

	public final static int Success = 0;
	public final static int ServiceNotRegister = 1;
	public final static int ServerStateError = 2;
	public final static int ServiceIdentityNotExist = 3;
	public final static int ServiceNotSubscribe = 4;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Update() {
		this.Argument = new ServiceInfo();
		this.Result = new EmptyBean();
	}
}
