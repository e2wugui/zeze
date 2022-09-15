package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class Update extends Rpc<BServiceInfo, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(Update.class.getName()); // -723792139
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3571175157

	public static final int Success = 0;
	public static final int ServiceNotRegister = 1;
	public static final int ServerStateError = 2;
	public static final int ServiceIdentityNotExist = 3;
	public static final int ServiceNotSubscribe = 4;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Update() {
		Argument = new BServiceInfo();
		Result = EmptyBean.instance;
	}

	public Update(BServiceInfo arg) {
		Argument = arg;
		Result = EmptyBean.instance;
	}
}
