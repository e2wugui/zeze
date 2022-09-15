package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/**
 * 动态服务启动时通过这个rpc注册自己。
 */
public class Register extends Rpc<BServiceInfo, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(Register.class.getName()); // -1080792024
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3214175272

	public static final int Success = 0;
	public static final int DuplicateRegister = 1;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Register() {
		Argument = new BServiceInfo();
		Result = EmptyBean.instance;
	}

	public Register(BServiceInfo arg) {
		Argument = arg;
		Result = EmptyBean.instance;
	}
}
