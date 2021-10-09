package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;

public class EmptyBean extends Bean {
	@Override
	public void Decode(ByteBuffer bb) {
		bb.ReadInt();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(0);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	public Bean CopyBean() {
		return new EmptyBean();
	}

	public static final long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

	@Override
	public long getTypeId() {
		return TYPEID;
	}

	@Override
	public String toString() {
		return "()";
	}
}