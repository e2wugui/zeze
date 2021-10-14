package Zezex.Provider;

import Zezex.*;

// auto-generated



public final class Transmit extends Zeze.Net.Protocol<Zezex.Provider.BTransmit> {
	public static final int ModuleId_ = 10001;
	public static final int ProtocolId_ = 28188;
	public static final int TypeId_ = ModuleId_ << 16 | ProtocolId_;

	@Override
	public int getModuleId() {
		return ModuleId_;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Transmit() {
	}

}