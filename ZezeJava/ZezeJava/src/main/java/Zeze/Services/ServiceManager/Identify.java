package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/**
 * C→S fire-and-forget：客户端上报自己的serverId，SM记录在会话上（identifyServerId），
 * 断线时据此广播Suspect。替代OfflineRegister的注册语义：无状态簿记、无取消语义、
 * Direct派发无锁。重连时重发=恢复提示资格（与正确性无关，正确性由租约裁决）。
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
