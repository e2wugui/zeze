package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class AnnounceServers extends Rpc<BAnnounceServers, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(AnnounceServers.class.getName()); // 893380350
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 893380350

	static {
		register(TypeId_, AnnounceServers.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public AnnounceServers() {
		Argument = new BAnnounceServers();
		Result = EmptyBean.instance;
	}

	public AnnounceServers(BAnnounceServers argument) {
		Argument = argument;
		Result = EmptyBean.instance;
	}
}
