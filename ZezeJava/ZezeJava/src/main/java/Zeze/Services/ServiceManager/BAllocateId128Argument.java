package Zeze.Services.ServiceManager;

import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

public final class BAllocateId128Argument implements Serializable {
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
		// 直接拷贝bytes构造Binary，不做intern：name是对端可控数据，intern会把每个唯一name
		// 永久驻留静态无界BinaryPool（向Id128端口发包即可无界撑堆）；Binary为内容等值语义，
		// 消费方（Id128UdpServer.cache的computeIfAbsent、getName）不依赖引用同一性。
		name = ibb.ReadBinary();
		count = ibb.ReadInt();
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
