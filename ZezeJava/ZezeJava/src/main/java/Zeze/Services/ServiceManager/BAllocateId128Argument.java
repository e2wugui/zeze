package Zeze.Services.ServiceManager;

import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.BinaryPool;
import org.jetbrains.annotations.NotNull;

public final class BAllocateId128Argument implements Serializable {
	private static final BinaryPool namePool = new BinaryPool();

	private @NotNull Binary name;
	private int count;

	public BAllocateId128Argument() {
		name = Binary.Empty;
	}

	public BAllocateId128Argument(@NotNull String name, int count) {
		this.name = new Binary(name);
		this.count = count;
	}

	public @NotNull Binary getBinaryName() {
		return name;
	}

	public @NotNull String getName() {
		return new String(name.bytesUnsafe(), 0, name.size(), StandardCharsets.UTF_8);
	}

	public void setName(@NotNull String value) {
		name = new Binary(value);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int value) {
		count = value;
	}

	@Override
	public void decode(@NotNull IByteBuffer ibb) {
		var bb = (ByteBuffer)ibb; // 特殊优化实现
		int size = bb.ReadUInt();
		var beginIndex = bb.ReadIndex;
		var endIndex = beginIndex + size;
		namePool.intern(bb.Bytes, beginIndex, endIndex);
		bb.ReadIndex = endIndex;
		count = bb.ReadInt();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteBinary(name);
		bb.WriteInt(count);
	}

	@Override
	public int preAllocSize() {
		return 20;
	}

	@Override
	public @NotNull String toString() {
		return "BAllocateId128Argument{name='" + getName() + "',count=" + count + '}';
	}
}
