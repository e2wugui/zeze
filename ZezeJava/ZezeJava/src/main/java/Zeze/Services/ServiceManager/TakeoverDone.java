package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/**
 * S→C 方向的接管完成回执（非raft版手写协议，对齐 Suspect 手写先例；raft版为 Gen 的
 * Zeze.Builtin.ServiceManagerWithRaft.TakeoverDone）。GS 的 tryTransfer 全部 scope 搬运
 * 完成后发送；SM 据此停止 version 梯队升档（任一档收到即停）。
 */
public class TakeoverDone extends Protocol<BTakeoverDone> {
	public static final int ProtocolId_ = Bean.hash32(TakeoverDone.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, TakeoverDone.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public TakeoverDone() {
		this.Argument = new BTakeoverDone();
	}

	public TakeoverDone(BTakeoverDone arg) {
		this.Argument = arg;
	}
}
