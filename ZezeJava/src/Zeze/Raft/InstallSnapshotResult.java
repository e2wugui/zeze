package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class InstallSnapshotResult extends Bean {
	private long Term;
	public long getTerm() {
		return Term;
	}
	public void setTerm(long value) {
		Term = value;
	}
	// 非标准Raft协议参数：用来支持续传。
	// >=0 : 让Leader从该位置继续传输数据。
	// -1  : 让Leader按自己顺序传输数据。
	private long Offset = -1;
	public long getOffset() {
		return Offset;
	}
	public void setOffset(long value) {
		Offset = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setOffset(bb.ReadLong());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteLong(getOffset());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s Offset=%2$s)", getTerm(), getOffset());
	}
}