package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public final class BAllocateIdArgument implements Serializable {
	private String name;
	private int count;

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
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
		count = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(name);
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
		return "BAllocateIdArgument{name='" + name + "',count=" + count + '}';
	}
}
