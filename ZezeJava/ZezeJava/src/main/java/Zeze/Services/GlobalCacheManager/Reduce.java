package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;

public class Reduce extends Zeze.Net.Rpc<BGlobalKeyState, BGlobalKeyState> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(Reduce.class.getName()); // -1004125491
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3290841805

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Reduce() {
		Argument = new BGlobalKeyState();
		Result = new BGlobalKeyState();
	}

	public Reduce(Binary gkey, int state) {
		Argument = new BGlobalKeyState();
		Result = new BGlobalKeyState();
		Argument.globalKey = gkey;
		Argument.state = state;
	}
}
