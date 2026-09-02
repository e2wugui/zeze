package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/**
 * S→C fire-and-forget 广播：SM在会话关闭时立即（不延迟、不挑选、不取SM锁）向所有
 * 已Identify会话之外的全部会话广播疑似死者的serverId。仅是提示（hint）：
 * 接收方转化为takeover.tryTransfer，由租约表裁决，未过期租约会被安排精确重试。
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
