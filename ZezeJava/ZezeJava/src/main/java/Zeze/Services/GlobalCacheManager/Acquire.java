package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;

public class Acquire extends Zeze.Net.Rpc<BGlobalKeyState, BGlobalKeyState> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(Acquire.class.getName()); // 1225831619
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1225831619

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Acquire() {
		Argument = new BGlobalKeyState();
		Result = new BGlobalKeyState();
	}

	public Acquire(Binary gkey, int state) {
		Argument = new BGlobalKeyState();
		Result = new BGlobalKeyState();
		Argument.GlobalKey = gkey;
		Argument.State = state;
	}
}
