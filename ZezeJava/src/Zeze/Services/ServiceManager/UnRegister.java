package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/** 
动态服务关闭时，注销自己，当与本服务器的连接关闭时，默认也会注销。
最好主动注销，方便以后错误处理。
*/
public final class UnRegister extends Rpc<ServiceInfo, EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash16(UnRegister.class.getName());

	public static final int Success = 0;
	public static final int NotExist = 1;

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
	
	public UnRegister() {
		Argument = new ServiceInfo();
		Result = new EmptyBean();
	}
}
