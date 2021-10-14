package Zezex.Linkd;

import Zezex.*;

// auto-generated



public final class Auth extends Zeze.Net.Rpc<Zezex.Linkd.BAuth, Zeze.Transaction.EmptyBean> {
	public static final int ModuleId_ = 10000;
	public static final int ProtocolId_ = 34483;
	public static final int TypeId_ = ModuleId_ << 16 | ProtocolId_;

	@Override
	public int getModuleId() {
		return ModuleId_;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
	public static final int Success = 0;
	public static final int Error = 1;

}