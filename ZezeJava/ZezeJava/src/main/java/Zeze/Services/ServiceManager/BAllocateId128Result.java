package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;

public final class BAllocateId128Result implements Serializable {
	private final @NotNull Id128 startId = new Id128();
	private int count;

	public @NotNull Id128 getStartId() {
		return startId;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int value) {
		count = value;
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		startId.decode(bb);
		count = bb.ReadInt();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		startId.encode(bb);
		bb.WriteInt(count);
	}

	@Override
	public int preAllocSize() {
		return 21;
	}

	@Override
	public @NotNull String toString() {
		return "BAllocateId128Result{startId=" + startId + ",count=" + count + '}';
	}
}
