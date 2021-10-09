package Zeze.Services.Handshake;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import java.util.*;
import java.math.*;

public final class SHandshake extends Zeze.Net.Protocol<SHandshakeArgument> {
	public final static int ProtocolId_ = Bean.Hash16(SHandshake.class.FullName);

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SHandshake() {
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public SHandshake(byte[] dh_data, bool s2cneedcompress, bool c2sneedcompress)
	public SHandshake(byte[] dh_data, boolean s2cneedcompress, boolean c2sneedcompress) {
		getArgument().dh_data = dh_data;
		getArgument().s2cneedcompress = s2cneedcompress;
		getArgument().c2sneedcompress = c2sneedcompress;
	}
}