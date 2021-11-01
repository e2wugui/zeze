package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/** 
动态服务启动时通过这个rpc注册自己。
*/
public class Register extends Rpc<ServiceInfo, EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash32(Register.class.getName());

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

	public Register(){
		this.Argument = new ServiceInfo();
		this.Result = new EmptyBean();
	}

}
