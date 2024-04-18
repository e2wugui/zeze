package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class Edit extends Rpc<BEdit, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(Edit.class.getName()); // -547826719
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 54241

	static {
		register(TypeId_, Edit.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Edit() {
		Argument = new BEdit();
		Result = EmptyBean.instance;
	}

	public Edit(BEdit arg) {
		Argument = arg;
		Result = EmptyBean.instance;
	}
}
