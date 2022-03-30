package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class CHandshakeDone extends Protocol<EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash32(CHandshakeDone.class.getName());

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public CHandshakeDone() {
		Argument = new EmptyBean();
	}
}
