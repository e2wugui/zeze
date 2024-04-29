package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public final class BAllocateIdResult implements Serializable {
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
		name = bb.ReadString();
		startId = bb.ReadLong();
		count = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(name);
		bb.WriteLong(startId);
		bb.WriteInt(count);
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
		return "BAllocateIdResult{name='" + name + "',startId=" + startId + ",count=" + count + '}';
	}
}
