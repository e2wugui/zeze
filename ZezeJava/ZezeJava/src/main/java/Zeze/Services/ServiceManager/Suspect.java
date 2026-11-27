package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/**
 * Suspect 协议（S→C，fire-and-forget）：SM 在会话断开时广播疑似失效节点的 serverId 与版本，
 * 仅作接管提示，最终由租约表裁决。
 */
public class Suspect extends Protocol<BIdentify> {
	public static final int ProtocolId_ = Bean.hash32(Suspect.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, Suspect.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Suspect() {
		this.Argument = new BIdentify();
	}

	public Suspect(BIdentify arg) {
		this.Argument = arg;
	}
}
