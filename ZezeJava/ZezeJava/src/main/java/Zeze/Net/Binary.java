package Zeze.Net;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import Zeze.Dbh2.Database;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.BitConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bean 类型 binary 的辅助类。
 * 构造之后就是只读的。
 * 【警告】byte[] bytes 参数传入以后，就不能再修改了。
 */
public final class Binary implements Comparable<Binary> {
	public static final Binary Empty = new Binary(ByteBuffer.Empty);

	private final byte @NotNull [] bytes;
	private final int offset;
	private final int count;

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 */
	public Binary(byte @NotNull [] bytes) {
		this.bytes = bytes;
		this.offset = 0;
		this.count = bytes.length;
	}

	public boolean startsWith(Binary prefix) {
		int prefixSize = prefix.count;
		return count >= prefixSize && Arrays.equals(
				bytes, offset, offset + prefixSize,
				prefix.bytes, prefix.offset, prefix.offset + prefixSize);
	}

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 */
	public Binary(byte @NotNull [] bytes, int count) {
		ByteBuffer.VerifyArrayIndex(bytes, count);
		this.bytes = bytes;
		this.offset = 0;
		this.count = count;
	}

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 */
	public Binary(byte @NotNull [] bytes, int offset, int count) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, count);
		this.bytes = bytes;
		this.offset = offset;
		this.count = count;
	}

	/**
	 * 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 * 【一般用于临时存储】
	 */
	public Binary(@NotNull ByteBuffer bb) {
		this(bb.Bytes, bb.ReadIndex, bb.size());
	}

	public Binary(@NotNull Serializable s) {
		this(ByteBuffer.encode(s));
	}

	public Binary(@NotNull String s) {
		this(s.getBytes(StandardCharsets.UTF_8));
	}

	public byte @NotNull [] bytesUnsafe() {
		return bytes;
	}

	public byte @NotNull [] copyIf() {
		return Database.copyIf(bytes, offset, count);
	}

	public byte @NotNull [] toBytes() {
		return Arrays.copyOfRange(bytes, offset, offset + count);
	}

	public void writeToFile(@NotNull RandomAccessFile file) throws IOException {
		file.write(bytes, offset, count);
	}

	public @NotNull ByteBuffer Wrap() {
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

	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteBytes(bytes, offset, count);
	}

	public void decode(@NotNull Serializable _s_) {
		_s_.decode(ByteBuffer.Wrap(bytes, offset, count));
	}

	public @NotNull String toString(@NotNull Charset charset) {
		return new String(bytes, offset, count, charset);
	}

	@Override
	public int compareTo(@NotNull Binary other) {
		return Arrays.compareUnsigned(bytes, offset, offset + count,
				other.bytes, other.offset, other.offset + other.count);
	}

	public boolean equals(@Nullable Binary other) {
		return this == other || other != null && count == other.count &&
				Arrays.equals(bytes, offset, offset + count, other.bytes, other.offset, other.offset + count);
	}

	@Override
	public boolean equals(@Nullable Object other) {
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
	public @NotNull String toString() {
		return BitConverter.toStringWithLimit(bytes, offset, count, 16, 4);
	}
}
