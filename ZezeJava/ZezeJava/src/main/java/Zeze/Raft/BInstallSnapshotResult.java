package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class BInstallSnapshotResult extends Bean {
	private long Term;

	// 非标准Raft协议参数：用来支持续传。
	// >=0 : 让Leader从该位置继续传输数据。
	// -1  : 让Leader按自己顺序传输数据。
	private long Offset = -1;

	public long getTerm() {
		return Term;
	}

	public void setTerm(long value) {
		Term = value;
	}

	public long getOffset() {
		return Offset;
	}

	public void setOffset(long value) {
		Offset = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteLong(Offset);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		Offset = bb.ReadLong();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d Offset=%d)", Term, Offset);
	}
}
