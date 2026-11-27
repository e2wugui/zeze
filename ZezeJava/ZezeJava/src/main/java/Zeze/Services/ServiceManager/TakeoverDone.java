package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/**
 * TakeoverDone 协议（C→S，fire-and-forget）：GS 完成全部 scope 接管后向 SM 上报回执，
 * 供 SM 停止 version 梯队升档。
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
