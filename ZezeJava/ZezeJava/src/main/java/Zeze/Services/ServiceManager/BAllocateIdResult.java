package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public final class BAllocateIdResult extends Bean {
	private String name;
	private long startId;
	private int count;

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
	}

	public long getStartId() {
		return startId;
	}

	public void setStartId(long value) {
		startId = value;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int value) {
		count = value;
	}

	@Override
	public void decode(IByteBuffer bb) {
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
		return "BAllocateIdResult{" + "Name='" + name + '\'' + ", StartId=" + startId + ", Count=" + count + '}';
	}
}
