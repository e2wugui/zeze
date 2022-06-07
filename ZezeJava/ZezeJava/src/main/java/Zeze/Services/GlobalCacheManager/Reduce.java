package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;

public class Reduce extends Zeze.Net.Rpc<GlobalKeyState, GlobalKeyState> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Reduce.class.getName()); // -1004125491
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
		Argument = new GlobalKeyState();
		Result = new GlobalKeyState();
	}

	public Reduce(Binary gkey, int state) {
		Argument = new GlobalKeyState();
		Result = new GlobalKeyState();
		Argument.GlobalKey = gkey;
		Argument.State = state;
	}
}
