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

	public static @NotNull NioByteBuffer Wrap(@NotNull java.nio.ByteBuffer obb) {
		return Wrap(obb.array(), obb.position(), obb.remaining());
	}

	public static @NotNull NioByteBuffer Wrap(@NotNull NioByteBuffer nbb) {
		var obb = nbb.bb;
		return Wrap(obb.array(), obb.position(), obb.remaining());
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes) {
		return new NioByteBuffer(bytes, 0, bytes.length);
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, length);
		return new NioByteBuffer(bytes, 0, length);
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);
		return new NioByteBuffer(bytes, offset, offset + length);
	}

	public static @NotNull NioByteBuffer Wrap(@NotNull Binary binary) {
		return new NioByteBuffer(binary.bytesUnsafe(), binary.getOffset(), binary.size());
	}

	public static @NotNull NioByteBuffer Allocate() {
		return Allocate(16);
	}

	public static @NotNull NioByteBuffer Allocate(int capacity) {
		// add pool?
		// 缓存 ByteBuffer 还是 byte[] 呢？
		// 最大的问题是怎么归还？而且 Bytes 是公开的，可能会被其他地方引用，很难确定什么时候回收。
		// buffer 使用2的幂，数量有限，使用简单策略即可。
		// Dictionary<capacity, List<byte[]>> pool;
		// socket的内存可以归还。
		return new NioByteBuffer(capacity);
	}

	protected NioByteBuffer(int capacity) {
		bb = capacity == 0 ? Empty : java.nio.ByteBuffer.allocate(capacity); // ToPower2(capacity)
	}

	protected NioByteBuffer(byte @NotNull [] bytes, int readIndex, int writeIndex) {
		bb = java.nio.ByteBuffer.wrap(bytes, readIndex, writeIndex - readIndex);
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
		// bb.get(getReadIndex(), copy); // need JDK13+
		int pos = getReadIndex();
		bb.get(copy);
		bb.position(pos);
		return copy;
	}

	@Override
	public void Reset() {
		bb.position(0);
		bb.limit(0);
	}

	private void throwEnsureReadException(int size) {
		throw new IllegalStateException("ensureRead " + getReadIndex() + '+' + size + " > " + getWriteIndex());
	}

	@Override
	public void ensureRead(int size) {
		if (getReadIndex() + size > getWriteIndex())
			throwEnsureReadException(size);
	}

	@Override
	public boolean ReadBool() {
		ensureRead(1);
		int pos = getReadIndex();
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
		int readIndex = getReadIndex();
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
		int readIndex = getReadIndex();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = bb.getLong(readIndex);
	}

	@Override
	public void SkipUInt() {
		ensureRead(1);
		int readIndex = getReadIndex();
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
	public long ReadLong1() {
		ensureRead(1);
		return bb.get() & 0xff;
	}

	@Override
	public long ReadLong2BE() {
		ensureRead(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort();
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
	public long ReadLong() {
		ensureRead(1);
		int b = bb.get();
		switch ((b >> 3) & 0x1f) {
		//@formatter:off
		case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
		case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return b;
		case 0x08: case 0x09: case 0x0a: case 0x0b: return ((b - 0x40 ) <<  8) + ReadLong1();
		case 0x14: case 0x15: case 0x16: case 0x17: return ((b + 0x40 ) <<  8) + ReadLong1();
		case 0x0c: case 0x0d:                       return ((b - 0x60 ) << 16) + ReadLong2BE();
		case 0x12: case 0x13:                       return ((b + 0x60 ) << 16) + ReadLong2BE();
		case 0x0e:                                  return ((b - 0x70L) << 24) + ReadLong3BE();
		case 0x11:                                  return ((b + 0x70L) << 24) + ReadLong3BE();
		case 0x0f:
			switch (b & 7) {
			case 0: case 1: case 2: case 3: return ((long)(b - 0x78) << 32) + ReadLong4BE();
			case 4: case 5:                 return ((long)(b - 0x7c) << 40) + ReadLong5BE();
			case 6:                         return ReadLong6BE();
			default: long r = ReadLong7BE(); return r < 0x80_0000_0000_0000L ?
					r : ((r - 0x80_0000_0000_0000L) << 8) + ReadLong1();
			}
		default: // 0x10
			switch (b & 7) {
			case 4: case 5: case 6: case 7: return ((long)(b + 0x78) << 32) + ReadLong4BE();
			case 2: case 3:                 return ((long)(b + 0x7c) << 40) + ReadLong5BE();
			case 1:                         return 0xffff_0000_0000_0000L + ReadLong6BE();
			default: long r = ReadLong7BE(); return r >= 0x80_0000_0000_0000L ?
					0xff00_0000_0000_0000L + r : ((r + 0x80_0000_0000_0000L) << 8) + ReadLong1();
			}
		//@formatter:on
		}
	}

	@Override
	public void SkipLong() {
		ensureRead(1);
		int b = bb.get();
		switch ((b >> 3) & 0x1f) {
		//@formatter:off
		case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
		case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return;
		case 0x08: case 0x09: case 0x0a: case 0x0b:
		case 0x14: case 0x15: case 0x16: case 0x17: Skip(1); return;
		case 0x0c: case 0x0d: case 0x12: case 0x13: Skip(2); return;
		case 0x0e: case 0x11:                       Skip(3); return;
		case 0x0f:
			switch (b & 7) {
			case 0: case 1: case 2: case 3: Skip(4); return;
			case 4: case 5:                 Skip(5); return;
			case 6:                         Skip(6); return;
			default: ensureRead(1); Skip(6 + (bb.get() >>> 31)); return;
			}
		default: // 0x10
			switch (b & 7) {
			case 4: case 5: case 6: case 7: Skip(4); return;
			case 2: case 3:                 Skip(5); return;
			case 1:                         Skip(6); return;
			default: ensureRead(1); Skip(7 - (bb.get() >>> 31));
			}
		//@formatter:on
		}
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
		int readIndex = getReadIndex();
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
		int readIndex = getReadIndex();
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
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		ensureRead(n);
		if (bb.hasArray()) {
			int pos = getReadIndex();
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
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		ensureRead(n);
		byte[] v = new byte[n];
		bb.get(v);
		return v;
	}

	@Override
	public void Skip(int n) {
		ensureRead(n);
		bb.position(getReadIndex() + n);
	}

	/**
	 * 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 */
/*
	public @NotNull NioByteBuffer ReadByteBuffer() {
		int n = ReadUInt();
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadByteBuffer: " + n
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		ensureRead(n);
		int cur = ReadIndex;
		ReadIndex = cur + n;
		return Wrap(Bytes, cur, n);
	}

	@SuppressWarnings("unchecked")
	public <T extends java.io.Serializable> T ReadJavaObject() {
		var bb = ReadByteBuffer();
		try (var bs = new ByteArrayInputStream(bb.Bytes, bb.ReadIndex, bb.size());
			 var os = new ObjectInputStream(bs)) {
			return (T)os.readObject();
		} catch (IOException | ClassNotFoundException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}
*/
	@Override
	public String toString() {
		return bb.toString();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other instanceof NioByteBuffer)
			return equals((NioByteBuffer)other);
/*
		if (other instanceof byte[]) {
			var bytes = (byte[])other;
			return Arrays.equals(Bytes, ReadIndex, WriteIndex, bytes, 0, bytes.length);
		}
		if (other instanceof Binary) {
			var binary = (Binary)other;
			return Arrays.equals(Bytes, ReadIndex, WriteIndex,
					binary.bytesUnsafe(), binary.getOffset(), binary.getOffset() + binary.size());
		}
*/
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
	public int compareTo(@NotNull NioByteBuffer nbb) {
		return bb.compareTo(nbb.bb);
	}
}
