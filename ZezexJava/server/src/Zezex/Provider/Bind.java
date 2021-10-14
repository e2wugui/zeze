package Zezex.Provider;

import Zezex.*;

// auto-generated



public final class Bind extends Zeze.Net.Rpc<Zezex.Provider.BBind, Zeze.Transaction.EmptyBean> {
	public static final int ModuleId_ = 10001;
	public static final int ProtocolId_ = 53591;
	public static final int TypeId_ = ModuleId_ << 16 | ProtocolId_;

	@Override
	public int getModuleId() {
		return ModuleId_;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}