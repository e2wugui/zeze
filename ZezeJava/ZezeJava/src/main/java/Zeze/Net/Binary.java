package Zeze.Net;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.BitConverter;

/**
 * Bean 类型 binary 的辅助类。
 * 构造之后就是只读的。
 * 【警告】byte[] bytes 参数传入以后，就不能再修改了。
 */
public final class Binary implements Comparable<Binary> {
	public static final Binary Empty = new Binary(ByteBuffer.Empty);

	private final byte[] bytes;
	private final int offset;
	private final int count;

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 */
	public Binary(byte[] bytes, int offset, int count) {
		this.bytes = bytes;
		this.offset = offset;
		this.count = count;
	}

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 */
	public Binary(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 * 【一般用于临时存储】
	 */
	public Binary(ByteBuffer bb) {
		this(bb.Bytes, bb.ReadIndex, bb.Size());
	}

	/**
	 * 这里调用Copy是因为ByteBuffer可能分配的保留内存较大。Copy返回实际大小的数据。
	 * 使用这个方法的地方一般是应用。这个数据可能被存储到表中。
	 */
	public Binary(Serializable s) {
		this(ByteBuffer.encode(s).Copy());
	}

	public Binary(String s) {
		this(s.getBytes(StandardCharsets.UTF_8));
	}

	public byte[] bytesUnsafe() {
		return bytes;
	}

	public byte[] toBytes() {
		return Arrays.copyOfRange(bytes, offset, offset + count);
	}

	public void writeToFile(RandomAccessFile file) throws IOException {
		file.write(bytes, offset, count);
	}

	public ByteBuffer Wrap() {
		return ByteBuffer.Wrap(bytes, offset, count);
	}

	public byte get(int index) {
		return bytes[index];
	}

	public int getOffset() {
		return offset;
	}

	public int size() {
		return count;
	}

	public void encode(ByteBuffer bb) {
		bb.WriteBytes(bytes, offset, count);
	}

	public void decode(Serializable _s_) {
		_s_.decode(ByteBuffer.Wrap(bytes, offset, count));
	}

	public String toString(Charset charset) {
		return new String(bytes, offset, count, charset);
	}

	@Override
	public int compareTo(Binary other) {
		return Arrays.compareUnsigned(bytes, offset, offset + count,
				other.bytes, other.offset, other.offset + other.count);
	}

	public boolean equals(Binary other) {
		return this == other || other != null && count == other.count &&
				Arrays.equals(bytes, offset, offset + count, other.bytes, other.offset, other.offset + count);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Binary)
			return equals((Binary)other);
		if (other instanceof byte[]) {
			var bytes = (byte[])other;
			return Arrays.equals(this.bytes, offset, offset + count, bytes, 0, bytes.length);
		}
		if (other instanceof ByteBuffer) {
			var bb = (ByteBuffer)other;
			return Arrays.equals(bytes, offset, offset + count, bb.Bytes, bb.ReadIndex, bb.WriteIndex);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ByteBuffer.calc_hashnr(bytes, offset, count);
	}

	@Override
	public String toString() {
		return BitConverter.toStringWithLimit(bytes, offset, count, 16);
	}
}
