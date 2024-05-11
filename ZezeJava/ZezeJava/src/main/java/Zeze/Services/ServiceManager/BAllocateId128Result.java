package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Id128;

public final class BAllocateId128Result implements Serializable {
	private String name;
	private Id128 startId;
	private int count;

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
	}

	public Id128 getStartId() {
		return startId;
	}

	public void setStartId(Id128 value) {
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
		startId = new Id128();
		startId.decode(bb);
		count = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(name);
		startId.encode(bb);
		bb.WriteInt(count);
	}

	private static int _PRE_ALLOC_SIZE_ = 32;

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
		return "BAllocateId128Result{name='" + name + "',startId=" + startId + ",count=" + count + '}';
	}
}
