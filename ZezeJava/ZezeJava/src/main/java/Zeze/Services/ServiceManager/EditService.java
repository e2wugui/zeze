package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class EditService extends Rpc<BEditService, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(EditService.class.getName()); // -1344046521
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 31303

	static {
		register(TypeId_, EditService.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public EditService() {
		Argument = new BEditService();
		Result = EmptyBean.instance;
	}

	public EditService(BEditService arg) {
		Argument = arg;
		Result = EmptyBean.instance;
	}
}
