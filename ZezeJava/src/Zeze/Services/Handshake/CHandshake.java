package Zeze.Services.Handshake;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import java.util.*;
import java.math.*;

public final class CHandshake extends Protocol<CHandshakeArgument> {
	public final static int ProtocolId_ = Bean.Hash16(CHandshake.class.FullName);

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public CHandshake() {

	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public CHandshake(byte dh_group, byte[] dh_data)
	public CHandshake(byte dh_group, byte[] dh_data) {
		getArgument().dh_group = dh_group;
		getArgument().dh_data = dh_data;
	}
}