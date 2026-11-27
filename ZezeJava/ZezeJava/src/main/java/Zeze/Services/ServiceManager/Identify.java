package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/**
 * Identify 协议（C→S，fire-and-forget）：客户端上报自身 serverId 与版本，
 * SM 记录于会话并在断线时据此广播 Suspect。
 */
public class Identify extends Protocol<BIdentify> {
	public static final int ProtocolId_ = Bean.hash32(Identify.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, Identify.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Identify() {
		this.Argument = new BIdentify();
	}

	public Identify(BIdentify arg) {
		this.Argument = arg;
	}
}
