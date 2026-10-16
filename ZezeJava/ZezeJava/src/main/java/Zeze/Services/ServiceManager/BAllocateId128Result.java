package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Id128;

/** AllocateId128应答bean：号段起始Id128与数量。 */
public final class BAllocateId128Result implements Serializable {
	private Id128 startId = new Id128(); // 从0开始
	private int count;

	public Id128 getStartId() {
		return startId;
	}

	public void setStartId(Id128 id128) {
		this.startId = id128;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int value) {
		count = value;
	}

	@Override
	public void decode(IByteBuffer bb) {
		startId.decodeRaw(bb);
		count = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		startId.encodeRaw(bb);
		bb.WriteInt(count);
	}

	@Override
	public int preAllocSize() {
		return 21;
	}

	@Override
	public String toString() {
		return "BAllocateId128Result{startId=" + startId + ",count=" + count + '}';
	}
}
