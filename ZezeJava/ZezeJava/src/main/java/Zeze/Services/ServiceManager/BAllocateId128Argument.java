package Zeze.Services.ServiceManager;

import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/** AllocateId128请求bean：全局名与申请数量。 */
public final class BAllocateId128Argument implements Serializable {
	private Binary name;
	private int count;

	public BAllocateId128Argument() {
		name = Binary.Empty;
	}

	public BAllocateId128Argument(String name, int count) {
		this.name = new Binary(name);
		this.count = count;
	}

	public Binary getBinaryName() {
		return name;
	}

	public String getName() {
		return new String(name.bytesUnsafe(), 0, name.size(), StandardCharsets.UTF_8);
	}

	public void setName(String value) {
		name = new Binary(value);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int value) {
		count = value;
	}

	@Override
	public void decode(IByteBuffer ibb) {
		// 直接拷贝构造而不 intern，避免对端可控的 name 无限驻留 BinaryPool。
		name = ibb.ReadBinary();
		count = ibb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(name);
		bb.WriteInt(count);
	}

	@Override
	public int preAllocSize() {
		return 20;
	}

	@Override
	public String toString() {
		return "BAllocateId128Argument{name='" + getName() + "',count=" + count + '}';
	}
}
