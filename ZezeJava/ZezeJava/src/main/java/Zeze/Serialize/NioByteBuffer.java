package Zeze.Serialize;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 暂时只支持读,不支持写
public class NioByteBuffer implements IByteBuffer, Comparable<NioByteBuffer> {
	public static final java.nio.ByteBuffer Empty = java.nio.ByteBuffer.wrap(ByteBuffer.Empty);

	public java.nio.ByteBuffer bb;

	@Override
	public int getReadIndex() {
		return bb.position();
	}

	@Override
	public void setReadIndex(int ri) {
		bb.position(ri);
	}

	@Override
	public int getWriteIndex() {
		return bb.limit();
	}

	@Override
	public int capacity() {
		return bb.capacity();
	}

	@Override
	public int size() {
		return bb.remaining();
	}

	@Override
	public boolean isEmpty() {
		return !bb.hasRemaining();
	}

	public static @NotNull NioByteBuffer Wrap(@NotNull java.nio.ByteBuffer bb) {
		return new NioByteBuffer(bb);
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes) {
		return new NioByteBuffer(java.nio.ByteBuffer.wrap(bytes));
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, length);
		return new NioByteBuffer(java.nio.ByteBuffer.wrap(bytes, 0, length));
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);
		return new NioByteBuffer(java.nio.ByteBuffer.wrap(bytes, offset, length));
	}

	public static @NotNull NioByteBuffer Wrap(@NotNull Binary binary) {
		return new NioByteBuffer(java.nio.ByteBuffer.wrap(binary.bytesUnsafe(), binary.getOffset(), binary.size()));
	}

	protected NioByteBuffer(@NotNull java.nio.ByteBuffer bb) {
		this.bb = bb;
	}

	@Override
	public void FreeInternalBuffer() {
		bb = Empty;
	}

	@Override
	public byte @NotNull [] Copy() {
		int size = size();
		if (size == 0)
			return ByteBuffer.Empty;
		byte[] copy = new byte[size];
		// bb.get(bb.position(), copy); // need JDK13+
		int pos = bb.position();
		bb.get(copy);
		bb.position(pos);
		return copy;
	}

	@Override
	public void Reset() {
		bb.position(0);
		bb.limit(0);
	}

	@Override
	public void ensureRead(int size) {
		if (bb.position() + size > bb.limit())
			throwEnsureReadException(size);
	}

	@Override
	public boolean ReadBool() {
		ensureRead(1);
		int pos = bb.position();
		int b = bb.get(pos);
		if ((b & ~1) == 0) { // fast-path
			bb.position(pos + 1);
			return b != 0;
		}
		return ReadLong() != 0; // rare-path
	}

	@Override
	public byte ReadByte() {
		ensureRead(1);
		return bb.get();
	}

	@Override
	public int ReadInt4() {
		ensureRead(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}

	@Override
	public void ReadInt4s(int @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 4)
			buf[offset + i] = bb.getInt(readIndex);
	}

	@Override
	public long ReadLong8() {
		ensureRead(8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getLong();
	}

	@Override
	public void ReadLong8s(long @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = bb.getLong(readIndex);
	}

	@Override
	public void SkipUInt() {
		ensureRead(1);
		int readIndex = bb.position();
		int v = bb.get(readIndex) & 0xff;
		if (v < 0x80)
			bb.position(readIndex + 1);
		else if (v < 0xc0) {
			ensureRead(2);
			bb.position(readIndex + 2);
		} else if (v < 0xe0) {
			ensureRead(3);
			bb.position(readIndex + 3);
		} else if (v < 0xf0) {
			ensureRead(4);
			bb.position(readIndex + 4);
		} else {
			ensureRead(5);
			bb.position(readIndex + 5);
		}
	}

	@Override
	public long ReadLong2BE() {
		ensureRead(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort() & 0xffff;
	}

	@Override
	public long ReadLong3BE() {
		ensureRead(3);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v = bb.getShort() & 0xffff;
		return (v << 8) + (bb.get() & 0xff);
	}

	@Override
	public long ReadLong4BE() {
		ensureRead(4);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getInt() & 0xffff_ffffL;
	}

	@Override
	public long ReadLong5BE() {
		ensureRead(5);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v = bb.getInt() & 0xffff_ffffL;
		return (v << 8) + (bb.get() & 0xff);
	}

	@Override
	public long ReadLong6BE() {
		ensureRead(6);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v = bb.getInt() & 0xffff_ffffL;
		return (v << 16) + (bb.getShort() & 0xffff);
	}

	@Override
	public long ReadLong7BE() {
		ensureRead(7);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v1 = bb.getInt() & 0xffff_ffffL;
		long v2 = bb.getShort() & 0xffff;
		return (v1 << 24) + (v2 << 8) + (bb.get() & 0xff);
	}

	@Override
	public long ReadLong8BE() {
		ensureRead(8);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getLong();
	}

	@Override
	public float ReadFloat() {
		ensureRead(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getFloat();
	}

	@Override
	public void ReadFloats(float @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 4)
			buf[offset + i] = bb.getFloat(readIndex);
	}

	@Override
	public double ReadDouble() {
		ensureRead(8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getDouble();
	}

	@Override
	public void ReadDoubles(double @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = bb.getDouble(readIndex);
	}

	@Override
	public @NotNull String ReadString() {
		int n = ReadUInt();
		if (n == 0)
			return "";
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadString: " + n
					+ " at " + bb.position() + '/' + bb.limit());
		}
		ensureRead(n);
		if (bb.hasArray()) {
			int pos = bb.position();
			bb.position(pos + n);
			return new String(bb.array(), pos, n, StandardCharsets.UTF_8);
		}
		var buf = new byte[n];
		bb.get(buf);
		return new String(buf, StandardCharsets.UTF_8);
	}

	@Override
	public byte @NotNull [] ReadBytes() {
		int n = ReadUInt();
		if (n == 0)
			return ByteBuffer.Empty;
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadBytes: " + n
					+ " at " + bb.position() + '/' + bb.limit());
		}
		ensureRead(n);
		var bytes = new byte[n];
		bb.get(bytes);
		return bytes;
	}

	@Override
	public void Skip(int n) {
		ensureRead(n);
		bb.position(bb.position() + n);
	}

	@Override
	public @NotNull ByteBuffer ReadByteBuffer() {
		return ByteBuffer.Wrap(ReadBytes());
	}

	@Override
	public String toString() {
		return bb.toString();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other instanceof NioByteBuffer)
			return equals((NioByteBuffer)other);
		if (other instanceof ByteBuffer) {
			var bb = (ByteBuffer)other;
			return this.bb.equals(java.nio.ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.size()));
		}
		if (other instanceof byte[])
			return bb.equals(java.nio.ByteBuffer.wrap((byte[])other));
		if (other instanceof Binary) {
			var binary = (Binary)other;
			return bb.equals(java.nio.ByteBuffer.wrap(binary.bytesUnsafe(), binary.getOffset(), binary.size()));
		}
		return false;
	}

	public boolean equals(@Nullable NioByteBuffer other) {
		return this == other || other != null && bb.equals(other.bb);
	}

	@Override
	public int hashCode() {
		return bb.hashCode();
	}

	@Override
	public @NotNull Vector2 ReadVector2() {
		ensureRead(8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		return new Vector2(x, y);
	}

	@Override
	public @NotNull Vector3 ReadVector3() {
		ensureRead(12);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		float z = bb.getFloat();
		return new Vector3(x, y, z);
	}

	@Override
	public @NotNull Vector4 ReadVector4() {
		ensureRead(16);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		float z = bb.getFloat();
		float w = bb.getFloat();
		return new Vector4(x, y, z, w);
	}

	@Override
	public @NotNull Quaternion ReadQuaternion() {
		ensureRead(16);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		float z = bb.getFloat();
		float w = bb.getFloat();
		return new Quaternion(x, y, z, w);
	}

	@Override
	public @Nullable ByteBuffer readUnknownField(int idx, int tag, @Nullable ByteBuffer unknown) {
		int beginIdx = bb.position();
		SkipUnknownField(tag);
		int size = bb.position() - beginIdx;
		if (size > 0) {
			if (unknown == null)
				unknown = ByteBuffer.Allocate(ByteBuffer.WriteUIntSize(idx) + 1 + ByteBuffer.WriteUIntSize(size) + size);
			unknown.WriteUInt(idx);
			unknown.WriteByte(tag & TAG_MASK);
			unknown.WriteUInt(size);
			unknown.EnsureWrite(size);
			bb.get(unknown.Bytes, unknown.WriteIndex, size);
			unknown.WriteIndex += size;
			return unknown;
		}
		throw new UnsupportedOperationException("readUnknownField: unsupported for derived bean");
	}

	@Override
	public int compareTo(@NotNull NioByteBuffer nbb) {
		return bb.compareTo(nbb.bb);
	}
}
