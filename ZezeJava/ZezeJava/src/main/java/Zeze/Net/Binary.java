package Zeze.Net;

import java.io.IOException;
import java.io.RandomAccessFile;
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

	private final byte[] _Bytes;
	private final int Offset;
	private final int Count;

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 */
	public Binary(byte[] bytes, int offset, int count) {
		_Bytes = bytes;
		Offset = offset;
		Count = count;
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
	public Binary(Serializable _s_) {
		this(ByteBuffer.Encode(_s_).Copy());
	}

	public byte[] bytesUnsafe() {
		return _Bytes;
	}

	public byte[] toBytes() {
		return Arrays.copyOfRange(_Bytes, Offset, Offset + Count);
	}

	public void writeToFile(RandomAccessFile file) throws IOException {
		file.write(_Bytes, Offset, Count);
	}

	public ByteBuffer Wrap() {
		return ByteBuffer.Wrap(_Bytes, Offset, Count);
	}

	public byte get(int index) {
		return _Bytes[index];
	}

	public int getOffset() {
		return Offset;
	}

	public int size() {
		return Count;
	}

	public void Encode(ByteBuffer bb) {
		bb.WriteBytes(_Bytes, Offset, Count);
	}

	public void Decode(Serializable _s_) {
		_s_.Decode(ByteBuffer.Wrap(_Bytes, Offset, Count));
	}

	@Override
	public int compareTo(Binary other) {
		int n = Count;
		int c = n - other.Count;
		return c != 0 ? c : Arrays.compare(_Bytes, Offset, Offset + n, other._Bytes, other.Offset, n);
	}

	public boolean equals(Binary other) {
		return this == other || other != null && Count == other.Count &&
				Arrays.equals(_Bytes, Offset, Offset + Count, other._Bytes, other.Offset, other.Offset + Count);
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof Binary && equals((Binary)other);
	}

	@Override
	public int hashCode() {
		return ByteBuffer.calc_hashnr(_Bytes, Offset, Count);
	}

	@Override
	public String toString() {
		return BitConverter.toStringWithLimit(_Bytes, Offset, Count, 16);
	}
}
