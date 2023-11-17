package Zeze.Serialize;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 暂时只支持读,不支持写
public class NioByteBuffer implements Comparable<NioByteBuffer> {
	public static final java.nio.ByteBuffer Empty = java.nio.ByteBuffer.wrap(ByteBuffer.Empty);

	public java.nio.ByteBuffer bb;

	public int capacity() {
		return bb.capacity();
	}

	public int size() {
		return bb.remaining();
	}

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
		VerifyArrayIndex(bytes, length);
		return new NioByteBuffer(bytes, 0, length);
	}

	public static @NotNull NioByteBuffer Wrap(byte @NotNull [] bytes, int offset, int length) {
		VerifyArrayIndex(bytes, offset, length);
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

	public void FreeInternalBuffer() {
		bb = Empty;
	}

	public byte @NotNull [] Copy() {
		int size = size();
		if (size == 0)
			return ByteBuffer.Empty;
		byte[] copy = new byte[size];
		bb.get(bb.position(), copy);
		return copy;
	}

	public void Reset() {
		bb.position(0);
		bb.limit(0);
	}

	private void throwEnsureReadException(int size) {
		throw new IllegalStateException("ensureRead " + bb.position() + '+' + size + " > " + bb.limit());
	}

	protected void ensureRead(int size) {
		if (bb.position() + size > bb.limit())
			throwEnsureReadException(size);
	}

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

	public byte ReadByte() {
		ensureRead(1);
		return bb.get();
	}

	public int ReadInt4() {
		ensureRead(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}

	public void ReadInt4s(int @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 4)
			buf[offset + i] = bb.getInt(readIndex);
	}

	public long ReadLong8() {
		ensureRead(8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getLong();
	}

	public void ReadLong8s(long @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = bb.getLong(readIndex);
	}

	// 返回值应被看作是无符号32位整数
	public int ReadUInt() {
		return (int)ReadULong();
	}

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

	// 返回值应被看作是无符号64位整数
	public long ReadULong() {
		int b = ReadByte();
		switch ((b >> 4) & 0xf) {
		//@formatter:off
		case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: return b;
		case  8: case  9: case 10: case 11: return ((b & 0x3f) <<  8) + ReadLong1();
		case 12: case 13:                   return ((b & 0x1f) << 16) + ReadLong2BE();
		case 14:                            return ((b & 0x0f) << 24) + ReadLong3BE();
		default:
			switch (b & 0xf) {
			case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7:
				return ((long)(b & 7) << 32) + ReadLong4BE();
			case  8: case  9: case 10: case 11: return ((long)(b & 3) << 40) + ReadLong5BE();
			case 12: case 13:                   return ((long)(b & 1) << 48) + ReadLong6BE();
			case 14:                            return ReadLong7BE();
			default:                            return ReadLong8BE();
			}
		//@formatter:on
		}
	}

	public void SkipULong() {
		int b = ReadByte();
		switch ((b >> 4) & 0xf) {
		//@formatter:off
		case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: return;
		case  8: case  9: case 10: case 11: Skip(1); return;
		case 12: case 13:                   Skip(2); return;
		case 14:                            Skip(3); return;
		default:
			switch (b & 0xf) {
			case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: Skip(4); return;
			case  8: case  9: case 10: case 11: Skip(5); return;
			case 12: case 13:                   Skip(6); return;
			case 14:                            Skip(7); return;
			default:                            Skip(8);
			}
		//@formatter:on
		}
	}

	public long ReadLong1() {
		ensureRead(1);
		return bb.get() & 0xff;
	}

	public long ReadLong2BE() {
		ensureRead(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort();
	}

	public long ReadLong3BE() {
		ensureRead(3);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v = bb.getShort() & 0xffff;
		return (v << 8) + (bb.get() & 0xff);
	}

	public long ReadLong4BE() {
		ensureRead(4);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getInt() & 0xffff_ffffL;
	}

	public long ReadLong5BE() {
		ensureRead(5);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v = bb.getInt() & 0xffff_ffffL;
		return (v << 8) + (bb.get() & 0xff);
	}

	public long ReadLong6BE() {
		ensureRead(6);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v = bb.getInt() & 0xffff_ffffL;
		return (v << 16) + (bb.getShort() & 0xffff);
	}

	public long ReadLong7BE() {
		ensureRead(7);
		bb.order(ByteOrder.BIG_ENDIAN);
		long v1 = bb.getInt() & 0xffff_ffffL;
		long v2 = bb.getShort() & 0xffff;
		return (v1 << 24) + (v2 << 8) + (bb.get() & 0xff);
	}

	public long ReadLong8BE() {
		ensureRead(8);
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getLong();
	}

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

	public int ReadInt() {
		return (int)ReadLong();
	}

	public float ReadFloat() {
		ensureRead(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getFloat();
	}

	public void ReadFloats(float @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 4)
			buf[offset + i] = bb.getFloat(readIndex);
	}

	public double ReadDouble() {
		ensureRead(8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getDouble();
	}

	public void ReadDoubles(double @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		ensureRead(n);
		int readIndex = bb.position();
		bb.position(readIndex + n);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = bb.getDouble(readIndex);
	}

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

	public @NotNull Binary ReadBinary() {
		var bytes = ReadBytes();
		return bytes.length > 0 ? new Binary(bytes) : Binary.Empty;
	}

	public byte @NotNull [] ReadBytes() {
		int n = ReadUInt();
		if (n == 0)
			return ByteBuffer.Empty;
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadBytes: " + n
					+ " at " + bb.position() + '/' + bb.limit());
		}
		ensureRead(n);
		byte[] v = new byte[n];
		bb.get(v);
		return v;
	}

	public void Skip(int n) {
		ensureRead(n);
		bb.position(bb.position() + n);
	}

	public void SkipBytes() {
		int n = ReadUInt();
		if (n < 0) {
			throw new IllegalStateException("invalid length for SkipBytes: " + n
					+ " at " + bb.position() + '/' + bb.limit());
		}
		Skip(n);
	}

	public void SkipBytes4() {
		int n = ReadInt4();
		if (n < 0) {
			throw new IllegalStateException("invalid length for SkipBytes4: " + n
					+ " at " + bb.position() + '/' + bb.limit());
		}
		Skip(n);
	}

	/**
	 * 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 */
/*
	public @NotNull NioByteBuffer ReadByteBuffer() {
		int n = ReadUInt();
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadByteBuffer: " + n
					+ " at " + bb.position() + '/' + bb.limit());
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

	public static void VerifyArrayIndex(byte @NotNull [] bytes, int length) {
		int bytesLen = bytes.length;
		if ((length | (bytesLen - length)) < 0)
			throw new IllegalArgumentException(length + " > " + bytesLen);
	}

	public static void VerifyArrayIndex(byte @NotNull [] bytes, int offset, int length) {
		int bytesLen = bytes.length, end;
		if ((offset | length | (end = offset + length) | (bytesLen - end)) < 0)
			throw new IllegalArgumentException(offset + " + " + length + " > " + bytesLen);
	}

	public <T extends Serializable> void decode(@NotNull Collection<T> c, @NotNull Supplier<T> factory) {
		for (int n = ReadUInt(); n > 0; n--) {
			T v = factory.get();
			v.decode(this);
			c.add(v);
		}
	}

	public int ReadTagSize(int tagByte) {
		int deltaId = (tagByte & ByteBuffer.ID_MASK) >> ByteBuffer.TAG_SHIFT;
		return deltaId < 0xf ? deltaId : 0xf + ReadUInt();
	}

	public boolean ReadBool(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.INTEGER)
			return ReadLong() != 0;
		if (type == ByteBuffer.FLOAT)
			return ReadFloat() != 0;
		if (type == ByteBuffer.DOUBLE)
			return ReadDouble() != 0;
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return false;
		}
		throw new IllegalStateException("can not ReadBool for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public byte ReadByte(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.INTEGER)
			return (byte)ReadLong();
		if (type == ByteBuffer.FLOAT)
			return (byte)ReadFloat();
		if (type == ByteBuffer.DOUBLE)
			return (byte)ReadDouble();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadByte for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public short ReadShort(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.INTEGER)
			return (short)ReadLong();
		if (type == ByteBuffer.FLOAT)
			return (short)ReadFloat();
		if (type == ByteBuffer.DOUBLE)
			return (short)ReadDouble();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadShort for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public int ReadInt(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.INTEGER)
			return (int)ReadLong();
		if (type == ByteBuffer.FLOAT)
			return (int)ReadFloat();
		if (type == ByteBuffer.DOUBLE)
			return (int)ReadDouble();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadInt for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public long ReadLong(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.INTEGER)
			return ReadLong();
		if (type == ByteBuffer.FLOAT)
			return (long)ReadFloat();
		if (type == ByteBuffer.DOUBLE)
			return (long)ReadDouble();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadLong for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public float ReadFloat(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.FLOAT)
			return ReadFloat();
		if (type == ByteBuffer.DOUBLE)
			return (float)ReadDouble();
		if (type == ByteBuffer.INTEGER)
			return ReadLong();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadFloat for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public double ReadDouble(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.DOUBLE)
			return ReadDouble();
		if (type == ByteBuffer.FLOAT)
			return ReadFloat();
		if (type == ByteBuffer.INTEGER)
			return ReadLong();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadDouble for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Binary ReadBinary(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.BYTES)
			return ReadBinary();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Binary.Empty;
		}
		throw new IllegalStateException("can not ReadBinary for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull String ReadString(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.BYTES)
			return ReadString();
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return "";
		}
		throw new IllegalStateException("can not ReadString for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Vector2 ReadVector2() {
		ensureRead(8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		return new Vector2(x, y);
	}

	public @NotNull Vector3 ReadVector3() {
		ensureRead(12);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		float z = bb.getFloat();
		return new Vector3(x, y, z);
	}

	public @NotNull Vector4 ReadVector4() {
		ensureRead(16);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		float z = bb.getFloat();
		float w = bb.getFloat();
		return new Vector4(x, y, z, w);
	}

	public @NotNull Quaternion ReadQuaternion() {
		ensureRead(16);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float x = bb.getFloat();
		float y = bb.getFloat();
		float z = bb.getFloat();
		float w = bb.getFloat();
		return new Quaternion(x, y, z, w);
	}

	public @NotNull Vector2Int ReadVector2Int() {
		int x = ReadInt();
		int y = ReadInt();
		return new Vector2Int(x, y);
	}

	public @NotNull Vector3Int ReadVector3Int() {
		int x = ReadInt();
		int y = ReadInt();
		int z = ReadInt();
		return new Vector3Int(x, y, z);
	}

	public @NotNull Vector2 ReadVector2(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR2)
			return ReadVector2();
		if (type == ByteBuffer.VECTOR3)
			return ReadVector3();
		if (type == ByteBuffer.VECTOR4)
			return ReadVector4();
		if (type == ByteBuffer.VECTOR2INT)
			return new Vector2(ReadVector2Int());
		if (type == ByteBuffer.VECTOR3INT)
			return new Vector3(ReadVector3Int());
		if (type == ByteBuffer.FLOAT)
			return new Vector2(ReadFloat(), 0);
		if (type == ByteBuffer.DOUBLE)
			return new Vector2((float)ReadDouble(), 0);
		if (type == ByteBuffer.INTEGER)
			return new Vector2(ReadLong(), 0);
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector2.ZERO;
		}
		throw new IllegalStateException("can not ReadVector2 for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Vector3 ReadVector3(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR3)
			return ReadVector3();
		if (type == ByteBuffer.VECTOR2)
			return new Vector3(ReadVector2());
		if (type == ByteBuffer.VECTOR4)
			return ReadVector4();
		if (type == ByteBuffer.VECTOR3INT)
			return new Vector3(ReadVector3Int());
		if (type == ByteBuffer.VECTOR2INT)
			return new Vector3(ReadVector2Int());
		if (type == ByteBuffer.FLOAT)
			return new Vector3(ReadFloat(), 0, 0);
		if (type == ByteBuffer.DOUBLE)
			return new Vector3((float)ReadDouble(), 0, 0);
		if (type == ByteBuffer.INTEGER)
			return new Vector3(ReadLong(), 0, 0);
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector3.ZERO;
		}
		throw new IllegalStateException("can not ReadVector3 for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Vector4 ReadVector4(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR4)
			return ReadVector4();
		if (type == ByteBuffer.VECTOR3)
			return new Vector4(ReadVector3());
		if (type == ByteBuffer.VECTOR2)
			return new Vector4(ReadVector2());
		if (type == ByteBuffer.VECTOR3INT)
			return new Vector4(ReadVector3Int());
		if (type == ByteBuffer.VECTOR2INT)
			return new Vector4(ReadVector2Int());
		if (type == ByteBuffer.FLOAT)
			return new Vector4(ReadFloat(), 0, 0, 0);
		if (type == ByteBuffer.DOUBLE)
			return new Vector4((float)ReadDouble(), 0, 0, 0);
		if (type == ByteBuffer.INTEGER)
			return new Vector4(ReadLong(), 0, 0, 0);
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector4.ZERO;
		}
		throw new IllegalStateException("can not ReadVector4 for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Quaternion ReadQuaternion(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR4)
			return ReadQuaternion();
		if (type == ByteBuffer.VECTOR3)
			return new Quaternion(ReadVector3());
		if (type == ByteBuffer.VECTOR2)
			return new Quaternion(ReadVector2());
		if (type == ByteBuffer.VECTOR3INT)
			return new Quaternion(ReadVector3Int());
		if (type == ByteBuffer.VECTOR2INT)
			return new Quaternion(ReadVector2Int());
		if (type == ByteBuffer.FLOAT)
			return new Quaternion(ReadFloat(), 0, 0, 0);
		if (type == ByteBuffer.DOUBLE)
			return new Quaternion((float)ReadDouble(), 0, 0, 0);
		if (type == ByteBuffer.INTEGER)
			return new Quaternion(ReadLong(), 0, 0, 0);
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Quaternion.ZERO;
		}
		throw new IllegalStateException("can not ReadQuaternion for type=" + type
				+ " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Vector2Int ReadVector2Int(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR2INT)
			return ReadVector2Int();
		if (type == ByteBuffer.VECTOR3INT)
			return ReadVector3Int();
		if (type == ByteBuffer.VECTOR2)
			return new Vector2Int(ReadVector2());
		if (type == ByteBuffer.VECTOR3)
			return new Vector3Int(ReadVector3());
		if (type == ByteBuffer.VECTOR4)
			return new Vector3Int(ReadVector4());
		if (type == ByteBuffer.INTEGER)
			return new Vector2Int(ReadInt(), 0);
		if (type == ByteBuffer.FLOAT)
			return new Vector2Int((int)ReadFloat(), 0);
		if (type == ByteBuffer.DOUBLE)
			return new Vector2Int((int)ReadDouble(), 0);
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector2Int.ZERO;
		}
		throw new IllegalStateException("can not ReadVector2Int for type=" + type
				+ " at " + bb.position() + '/' + bb.limit());
	}

	public @NotNull Vector3Int ReadVector3Int(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR3INT)
			return ReadVector3Int();
		if (type == ByteBuffer.VECTOR2INT)
			return new Vector3Int(ReadVector2Int());
		if (type == ByteBuffer.VECTOR3)
			return new Vector3Int(ReadVector3());
		if (type == ByteBuffer.VECTOR2)
			return new Vector3Int(ReadVector2());
		if (type == ByteBuffer.VECTOR4)
			return new Vector3Int(ReadVector4());
		if (type == ByteBuffer.INTEGER)
			return new Vector3Int(ReadInt(), 0, 0);
		if (type == ByteBuffer.FLOAT)
			return new Vector3Int((int)ReadFloat(), 0, 0);
		if (type == ByteBuffer.DOUBLE)
			return new Vector3Int((int)ReadDouble(), 0, 0);
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector3Int.ZERO;
		}
		throw new IllegalStateException("can not ReadVector3Int for type=" + type
				+ " at " + bb.position() + '/' + bb.limit());
	}

	public <T extends Serializable> @NotNull T ReadBean(@NotNull T bean, int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.BEAN)
			bean.decode(this);
		else if (type == ByteBuffer.DYNAMIC) {
			SkipLong();
			bean.decode(this);
		} else if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(tag);
		else {
			throw new IllegalStateException("can not ReadBean(" + bean.getClass().getName() + ") for type=" + type
					+ " at " + bb.position() + '/' + bb.limit());
		}
		return bean;
	}

	public @NotNull DynamicBean ReadDynamic(@NotNull DynamicBean dynBean, int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.DYNAMIC)
			dynBean.decode(this);
		else if (type == ByteBuffer.BEAN)
			dynBean.newBean(0).decode(this);
		else if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(tag);
		else
			throw new IllegalStateException("can not ReadDynamic for type=" + type + " at " + bb.position() + '/' + bb.limit());
		return dynBean;
	}

	public @NotNull DynamicData ReadDynamic(@NotNull DynamicData dynBean, int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.DYNAMIC) {
			dynBean.decode(this);
			return dynBean;
		}
		if (type == ByteBuffer.BEAN) {
			var bean = dynBean.toData(0);
			if (bean != null) {
				bean.decode(this);
				return dynBean;
			}
		}
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return dynBean;
		}
		throw new IllegalStateException("can not ReadDynamic for type=" + type + " at " + bb.position() + '/' + bb.limit());
	}

	public void SkipUnknownFieldOrThrow(int tag, @NotNull String curType) {
		if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(tag);
		else {
			throw new IllegalStateException("can not read " + curType + " for type=" + (tag & ByteBuffer.TAG_MASK)
					+ " at " + bb.position() + '/' + bb.limit());
		}
	}

	public void SkipUnknownField(int tag, int count) {
		while (--count >= 0)
			SkipUnknownField(tag);
	}

	public void SkipUnknownField(int type1, int type2, int count) {
		type1 |= 0x10; // ensure high bits not zero
		type2 |= 0x10; // ensure high bits not zero
		while (--count >= 0) {
			SkipUnknownField(type1);
			SkipUnknownField(type2);
		}
	}

	public void SkipUnknownField(int tag) {
		int type = tag & ByteBuffer.TAG_MASK;
		switch (type) {
		case ByteBuffer.INTEGER:
			SkipLong();
			return;
		case ByteBuffer.FLOAT:
			if (tag == ByteBuffer.FLOAT) // high bits == 0
				return;
			Skip(4);
			return;
		case ByteBuffer.DOUBLE:
		case ByteBuffer.VECTOR2:
			Skip(8);
			return;
		case ByteBuffer.VECTOR2INT:
			SkipLong();
			SkipLong();
			return;
		case ByteBuffer.VECTOR3:
			Skip(12);
			return;
		case ByteBuffer.VECTOR3INT:
			SkipLong();
			SkipLong();
			SkipLong();
			return;
		case ByteBuffer.VECTOR4:
			Skip(16);
			return;
		case ByteBuffer.BYTES:
			SkipBytes();
			return;
		case ByteBuffer.LIST:
			int t = ReadByte();
			SkipUnknownField(t, ReadTagSize(t));
			return;
		case ByteBuffer.MAP:
			t = ReadByte();
			SkipUnknownField(t >> ByteBuffer.TAG_SHIFT, t, ReadUInt());
			return;
		case ByteBuffer.DYNAMIC:
			SkipLong();
			//noinspection fallthrough
		case ByteBuffer.BEAN:
			while ((t = ReadByte()) != 0) {
				if ((t & ByteBuffer.ID_MASK) == 0xf0)
					SkipUInt();
				SkipUnknownField(t);
			}
			return;
		default:
			throw new IllegalStateException("SkipUnknownField: type=" + type
					+ " at " + bb.position() + '/' + bb.limit());
		}
	}

	public void skipAllUnknownFields(int tag) {
		while (tag != 0) {
			SkipUnknownField(tag);
			ReadTagSize(tag = ReadByte());
		}
	}

	public long readUnknownIndex() {
		return bb.hasRemaining() ? ReadUInt() : Long.MAX_VALUE;
	}

	@Override
	public int compareTo(@NotNull NioByteBuffer nbb) {
		return bb.compareTo(nbb.bb);
	}
}
