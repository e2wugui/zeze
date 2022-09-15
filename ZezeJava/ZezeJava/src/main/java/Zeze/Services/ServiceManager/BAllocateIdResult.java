package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BAllocateIdResult extends Bean {
	private String Name;
	private long StartId;
	private int Count;

	public String getName() {
		return Name;
	}

	public void setName(String value) {
		Name = value;
	}

	public long getStartId() {
		return StartId;
	}

	public void setStartId(long value) {
		StartId = value;
	}

	public int getCount() {
		return Count;
	}

	public void setCount(int value) {
		Count = value;
	}

	@Override
	public void decode(ByteBuffer bb) {
		setName(bb.ReadString());
		setStartId(bb.ReadLong());
		setCount(bb.ReadInt());
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(getName());
		bb.WriteLong(getStartId());
		bb.WriteInt(getCount());
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public String toString() {
		return "BAllocateIdResult{" + "Name='" + Name + '\'' + ", StartId=" + StartId + ", Count=" + Count + '}';
	}
}
