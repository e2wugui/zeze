package Zeze.Services.Handshake;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import java.util.*;
import java.math.*;

public final class SHandshakeArgument extends Zeze.Transaction.Bean {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] dh_data;
	public byte[] dh_data;
	public boolean s2cneedcompress;
	public boolean c2sneedcompress;

	@Override
	public void Decode(ByteBuffer bb) {
		dh_data = bb.ReadBytes();
		s2cneedcompress = bb.ReadBool();
		c2sneedcompress = bb.ReadBool();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBytes(dh_data);
		bb.WriteBool(s2cneedcompress);
		bb.WriteBool(c2sneedcompress);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}
}