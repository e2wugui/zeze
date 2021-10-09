package Zeze.Services.Handshake;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import java.util.*;
import java.math.*;

public final class CHandshakeArgument extends Zeze.Transaction.Bean {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte dh_group;
	public byte dh_group;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] dh_data;
	public byte[] dh_data;

	@Override
	public void Decode(ByteBuffer bb) {
		dh_group = bb.ReadByte();
		dh_data = bb.ReadBytes();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteByte(dh_group);
		bb.WriteBytes(dh_data);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}
}