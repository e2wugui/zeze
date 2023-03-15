package Zeze.Serialize;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicBeanData;
import Zeze.Util.BitConverter;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashMap;
import org.jetbrains.annotations.NotNull;

public class ByteBuffer implements Comparable<ByteBuffer> {
	public static final VarHandle intLeHandler = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
	public static final VarHandle intBeHandler = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
	public static final VarHandle longLeHandler = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
	public static final VarHandle longBeHandler = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
	public static final boolean IGNORE_INCOMPATIBLE_FIELD = false; // 不忽略兼容字段则会抛异常
	public static final byte[] Empty = new byte[0];

	public byte[] Bytes;
	public int ReadIndex;
	public int WriteIndex;

	public int Capacity() {
		return Bytes.length;
	}

	public int capacity() {
		return Bytes.length;
	}

	public int Size() {
		return WriteIndex - ReadIndex;
	}

	public int size() {
		return WriteIndex - ReadIndex;
	}

	public static ByteBuffer Wrap(ByteBuffer bb) {
		return Wrap(bb.Bytes, bb.ReadIndex, bb.Size());
	}

	public static ByteBuffer Wrap(byte[] bytes) {
		return new ByteBuffer(bytes, 0, bytes.length);
	}

	public static ByteBuffer Wrap(byte[] bytes, int offset, int length) {
		VerifyArrayIndex(bytes, offset, length);
		return new ByteBuffer(bytes, offset, offset + length);
	}

	public static ByteBuffer Wrap(Binary binary) {
		return binary.Wrap();
	}

	public static ByteBuffer Allocate() {
		return Allocate(16);
	}

	public static ByteBuffer Allocate(int capacity) {
		// add pool?
		// 缓存 ByteBuffer 还是 byte[] 呢？
		// 最大的问题是怎么归还？而且 Bytes 是公开的，可能会被其他地方引用，很难确定什么时候回收。
		// buffer 使用2的幂，数量有限，使用简单策略即可。
		// Dictionary<capacity, List<byte[]>> pool;
		// socket的内存可以归还。
		return new ByteBuffer(capacity);
	}

	protected ByteBuffer(int capacity) {
		Bytes = capacity == 0 ? Empty : new byte[capacity]; // ToPower2(capacity)
	}

	protected ByteBuffer(byte[] bytes, int readIndex, int writeIndex) {
		Bytes = bytes;
		ReadIndex = readIndex;
		WriteIndex = writeIndex;
	}

	public void FreeInternalBuffer() {
		Bytes = Empty;
		Reset();
	}

	public void wraps(byte[] bytes) {
		Bytes = bytes;
		ReadIndex = 0;
		WriteIndex = bytes.length;
	}

	public void wraps(byte[] bytes, int offset, int length) {
		VerifyArrayIndex(bytes, offset, length);
		Bytes = bytes;
		ReadIndex = offset;
		WriteIndex = offset + length;
	}

	public void Append(byte b) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = b;
	}

	public void Append(byte[] bs) {
		Append(bs, 0, bs.length);
	}

	public void Append(byte[] bs, int offset, int len) {
		EnsureWrite(len);
		System.arraycopy(bs, offset, Bytes, WriteIndex, len);
		WriteIndex += len;
	}

	public void Replace(int writeIndex, byte[] src) {
		Replace(writeIndex, src, 0, src.length);
	}

	public void Replace(int writeIndex, byte[] src, int offset, int len) {
		if (writeIndex < ReadIndex || writeIndex + len > WriteIndex)
			throw new IllegalStateException();
		System.arraycopy(src, offset, Bytes, writeIndex, len);
	}

	public int BeginWriteWithSize4() {
		int saveSize = Size();
		EnsureWrite(4);
		WriteIndex += 4;
		return saveSize;
	}

	public void EndWriteWithSize4(int saveSize) {
		int oldWriteIndex = ReadIndex + saveSize;
		if (oldWriteIndex + 4 > WriteIndex)
			throw new IllegalStateException();
		intLeHandler.set(Bytes, oldWriteIndex, WriteIndex - oldWriteIndex - 4);
	}

	public static int ToInt(byte[] bytes, int offset) {
		return (int)intLeHandler.get(bytes, offset);
	}

	public static int ToIntBE(byte[] bytes, int offset) {
		return (int)intBeHandler.get(bytes, offset);
	}

	public static long ToLong(byte[] bytes, int offset) {
		return (long)longLeHandler.get(bytes, offset);
	}

	public static long ToLongBE(byte[] bytes, int offset) {
		return (long)longBeHandler.get(bytes, offset);
	}

	@SuppressWarnings("fallthrough")
	public static long ToLong(byte[] bytes, int offset, int length) {
		long v = 0;
		//@formatter:off
		switch (length) {
		default: return (long)longLeHandler.get(bytes, offset);
		case 7:	v = ((long)(bytes[offset + 6] & 0xff) << 48);
		case 6: v += ((long)(bytes[offset + 5] & 0xff) << 40);
		case 5: v += ((long)(bytes[offset + 4] & 0xff) << 32);
		case 4: v += ((int)intLeHandler.get(bytes, offset) & 0xffff_ffffL); break;
		case 3: v += ((bytes[offset + 2] & 0xff) << 16);
		case 2: v += ((bytes[offset + 1] & 0xff) << 8);
		case 1: v += (bytes[offset] & 0xff);
		case 0:
		}
		//@formatter:on
		return v;
	}

	@SuppressWarnings("fallthrough")
	public static long ToLongBE(byte[] bytes, int offset, int length) {
		long v = 0;
		int s = 0;
		//@formatter:off
		switch (length) {
		default: return (long)longBeHandler.get(bytes, offset);
		case 7:	v = (bytes[offset + 6] & 0xff); s = 8;
		case 6: v += (bytes[offset + 5] & 0xff) << s; s += 8;
		case 5: v += (bytes[offset + 4] & 0xff) << s; s += 8;
		case 4: v += ((int)intBeHandler.get(bytes, offset) & 0xffff_ffffL) << s; break;
		case 3: v += (bytes[offset + 2] & 0xff); s = 8;
		case 2: v += (bytes[offset + 1] & 0xff) << s; s += 8;
		case 1: v += (bytes[offset] & 0xff) << s;
		case 0:
		}
		//@formatter:on
		return v;
	}

	public static float ToFloat(byte[] bytes, int offset) {
		return Float.intBitsToFloat(ToInt(bytes, offset));
	}

	public static float ToFloatBE(byte[] bytes, int offset) {
		return Float.intBitsToFloat(ToIntBE(bytes, offset));
	}

	public static double ToDouble(byte[] bytes, int offset) {
		return Double.longBitsToDouble(ToLong(bytes, offset));
	}

	public static double ToDoubleBE(byte[] bytes, int offset) {
		return Double.longBitsToDouble(ToLongBE(bytes, offset));
	}

	/**
	 * 这个方法把剩余可用数据移到buffer开头。
	 * 【注意】这个方法会修改ReadIndex，WriteIndex。
	 * 最好仅在全部读取写入处理完成以后调用处理一次，
	 * 为下一次写入读取做准备。
	 */
	public void Compact() {
		int size = Size();
		if (size > 0) {
			if (ReadIndex > 0) {
				System.arraycopy(Bytes, ReadIndex, Bytes, 0, size);
				ReadIndex = 0;
				WriteIndex = size;
			}
		} else
			Reset();
	}

	public byte[] Copy() {
		int size = Size();
		if (size == 0)
			return Empty;
		byte[] copy = new byte[size];
		System.arraycopy(Bytes, ReadIndex, copy, 0, size);
		return copy;
	}

	public void Reset() {
		ReadIndex = 0;
		WriteIndex = 0;
	}

	protected static int toPower2(int needSize) {
		int size = 16;
		while (size < needSize)
			size <<= 1;
		return size;
	}

	public void EnsureWrite(int size) {
		int newSize = WriteIndex + size;
		if (newSize > Capacity()) {
			byte[] newBytes = new byte[toPower2(newSize)];
			System.arraycopy(Bytes, ReadIndex, newBytes, 0, WriteIndex -= ReadIndex);
			ReadIndex = 0;
			Bytes = newBytes;
		}
	}

	protected void ensureRead(int size) {
		if (ReadIndex + size > WriteIndex)
			throw new IllegalStateException("EnsureRead " + ReadIndex + '+' + size + " > " + WriteIndex);
	}

	public void WriteBool(boolean b) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = (byte)(b ? 1 : 0);
	}

	public boolean ReadBool() {
		ensureRead(1);
		int b = Bytes[ReadIndex];
		if ((b & ~1) == 0) { // fast-path
			ReadIndex++;
			return b != 0;
		}
		return ReadLong() != 0; // rare-path
	}

	public void WriteByte(byte v) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = v;
	}

	public void WriteByte(int v) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = (byte)v;
	}

	public byte ReadByte() {
		ensureRead(1);
		return Bytes[ReadIndex++];
	}

	public void WriteInt4(int v) {
		EnsureWrite(4);
		int writeIndex = WriteIndex;
		intLeHandler.set(Bytes, writeIndex, v);
		WriteIndex = writeIndex + 4;
	}

	public int ReadInt4() {
		ensureRead(4);
		int v = ToInt(Bytes, ReadIndex);
		ReadIndex += 4;
		return v;
	}

	public void WriteLong8(long v) {
		EnsureWrite(8);
		int writeIndex = WriteIndex;
		longLeHandler.set(Bytes, writeIndex, v);
		WriteIndex = writeIndex + 8;
	}

	public long ReadLong8() {
		ensureRead(8);
		long v = ToLong(Bytes, ReadIndex);
		ReadIndex += 8;
		return v;
	}

	public static int writeUIntSize(int v) {
		long u = v & 0xffff_ffffL;
		//@formatter:off
		if (u <        0x80) return 1;
		if (u <      0x4000) return 2;
		if (u <   0x20_0000) return 3;
		if (u < 0x1000_0000) return 4;
		return 5;
		//@formatter:on
	}

	public void WriteUInt(int v) {
		long u = v & 0xffff_ffffL;
		if (u < 0x80) { // 0xxx xxxx
			EnsureWrite(1);
			Bytes[WriteIndex++] = (byte)u;
		} else if (u < 0x4000) { // 10xx xxxx +1B
			EnsureWrite(2);
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)((u >> 8) + 0x80);
			bytes[writeIndex + 1] = (byte)u;
			WriteIndex = writeIndex + 2;
		} else if (u < 0x20_0000) { // 110x xxxx +2B
			EnsureWrite(3);
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)((u >> 16) + 0xc0);
			bytes[writeIndex + 1] = (byte)(u >> 8);
			bytes[writeIndex + 2] = (byte)u;
			WriteIndex = writeIndex + 3;
		} else if (u < 0x1000_0000) { // 1110 xxxx +3B
			EnsureWrite(4);
			int writeIndex = WriteIndex;
			intBeHandler.set(Bytes, writeIndex, v + 0xe000_0000);
			WriteIndex = writeIndex + 4;
		} else { // 1111 0000 +4B
			EnsureWrite(5);
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)0xf0;
			intBeHandler.set(bytes, writeIndex + 1, v);
			WriteIndex = writeIndex + 5;
		}
	}

	public int ReadUInt() {
		ensureRead(1);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		int v = bytes[readIndex] & 0xff;
		if (v < 0x80) {
			ReadIndex = readIndex + 1;
		} else if (v < 0xc0) {
			ensureRead(2);
			v = ((v & 0x3f) << 8)
					+ (bytes[readIndex + 1] & 0xff);
			ReadIndex = readIndex + 2;
		} else if (v < 0xe0) {
			ensureRead(3);
			v = ((v & 0x1f) << 16)
					+ ((bytes[readIndex + 1] & 0xff) << 8)
					+ (bytes[readIndex + 2] & 0xff);
			ReadIndex = readIndex + 3;
		} else if (v < 0xf0) {
			ensureRead(4);
			v = (int)intBeHandler.get(bytes, readIndex) & 0xfff_ffff;
			ReadIndex = readIndex + 4;
		} else {
			ensureRead(5);
			v = (int)intBeHandler.get(bytes, readIndex + 1);
			ReadIndex = readIndex + 5;
		}
		return v;
	}

	public static int writeLongSize(long v) {
		//@formatter:off
		if (v >= 0) {
			if (v <                0x40 ) return 1;
			if (v <              0x2000 ) return 2;
			if (v <           0x10_0000 ) return 3;
			if (v <          0x800_0000 ) return 4;
			if (v <       0x4_0000_0000L) return 5;
			if (v <     0x200_0000_0000L) return 6;
			if (v <  0x1_0000_0000_0000L) return 7;
			if (v < 0x80_0000_0000_0000L) return 8;
										  return 9;
		}
		if (v >= -               0x40 ) return 1;
		if (v >= -             0x2000 ) return 2;
		if (v >= -          0x10_0000 ) return 3;
		if (v >= -         0x800_0000 ) return 4;
		if (v >= -      0x4_0000_0000L) return 5;
		if (v >= -    0x200_0000_0000L) return 6;
		if (v >= - 0x1_0000_0000_0000L) return 7;
		if (v >= -0x80_0000_0000_0000L) return 8;
		return 9;
		//@formatter:on
	}

	public void WriteLong(long v) {
		if (v >= 0) {
			if (v < 0x40) { // 00xx xxxx
				EnsureWrite(1);
				Bytes[WriteIndex++] = (byte)v;
			} else if (v < 0x2000) { // 010x xxxx +1B
				EnsureWrite(2);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 8) + 0x40);
				bytes[writeIndex + 1] = (byte)v;
				WriteIndex = writeIndex + 2;
			} else if (v < 0x10_0000) { // 0110 xxxx +2B
				EnsureWrite(3);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 16) + 0x60);
				bytes[writeIndex + 1] = (byte)(v >> 8);
				bytes[writeIndex + 2] = (byte)v;
				WriteIndex = writeIndex + 3;
			} else if (v < 0x800_0000) { // 0111 0xxx +3B
				EnsureWrite(4);
				int writeIndex = WriteIndex;
				intBeHandler.set(Bytes, writeIndex, (int)v + 0x7000_0000);
				WriteIndex = writeIndex + 4;
			} else if (v < 0x4_0000_0000L) { // 0111 10xx +4B
				EnsureWrite(5);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 32) + 0x78);
				intBeHandler.set(bytes, writeIndex + 1, (int)v);
				WriteIndex = writeIndex + 5;
			} else if (v < 0x200_0000_0000L) { // 0111 110x +5B
				EnsureWrite(6);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 40) + 0x7c);
				bytes[writeIndex + 1] = (byte)(v >> 32);
				intBeHandler.set(bytes, writeIndex + 2, (int)v);
				WriteIndex = writeIndex + 6;
			} else if (v < 0x1_0000_0000_0000L) { // 0111 1110 +6B
				EnsureWrite(7);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x7e;
				bytes[writeIndex + 1] = (byte)(v >> 40);
				bytes[writeIndex + 2] = (byte)(v >> 32);
				intBeHandler.set(bytes, writeIndex + 3, (int)v);
				WriteIndex = writeIndex + 7;
			} else if (v < 0x80_0000_0000_0000L) { // 0111 1111 0 +7B
				EnsureWrite(8);
				int writeIndex = WriteIndex;
				longBeHandler.set(Bytes, writeIndex, v + 0x7f00_0000_0000_0000L);
				WriteIndex = writeIndex + 8;
			} else { // 0111 1111 1 +8B
				EnsureWrite(9);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x7f;
				longBeHandler.set(bytes, writeIndex + 1, v + 0x8000_0000_0000_0000L);
				WriteIndex = writeIndex + 9;
			}
		} else {
			if (v >= -0x40) { // 11xx xxxx
				EnsureWrite(1);
				Bytes[WriteIndex++] = (byte)v;
			} else if (v >= -0x2000) { // 101x xxxx +1B
				EnsureWrite(2);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 8) - 0x40);
				bytes[writeIndex + 1] = (byte)v;
				WriteIndex = writeIndex + 2;
			} else if (v >= -0x10_0000) { // 1001 xxxx +2B
				EnsureWrite(3);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 16) - 0x60);
				bytes[writeIndex + 1] = (byte)(v >> 8);
				bytes[writeIndex + 2] = (byte)v;
				WriteIndex = writeIndex + 3;
			} else if (v >= -0x800_0000) { // 1000 1xxx +3B
				EnsureWrite(4);
				int writeIndex = WriteIndex;
				intBeHandler.set(Bytes, writeIndex, (int)v - 0x7000_0000);
				WriteIndex = writeIndex + 4;
			} else if (v >= -0x4_0000_0000L) { // 1000 01xx +4B
				EnsureWrite(5);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 32) - 0x78);
				intBeHandler.set(bytes, writeIndex + 1, (int)v);
				WriteIndex = writeIndex + 5;
			} else if (v >= -0x200_0000_0000L) { // 1000 001x +5B
				EnsureWrite(6);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 40) - 0x7c);
				bytes[writeIndex + 1] = (byte)(v >> 32);
				intBeHandler.set(bytes, writeIndex + 2, (int)v);
				WriteIndex = writeIndex + 6;
			} else if (v >= -0x1_0000_0000_0000L) { // 1000 0001 +6B
				EnsureWrite(7);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x81;
				bytes[writeIndex + 1] = (byte)(v >> 40);
				bytes[writeIndex + 2] = (byte)(v >> 32);
				intBeHandler.set(bytes, writeIndex + 3, (int)v);
				WriteIndex = writeIndex + 7;
			} else if (v >= -0x80_0000_0000_0000L) { // 1000 0000 1 +7B
				EnsureWrite(8);
				int writeIndex = WriteIndex;
				longBeHandler.set(Bytes, writeIndex, v - 0x7f00_0000_0000_0000L);
				WriteIndex = writeIndex + 8;
			} else { // 1000 0000 0 +8B
				EnsureWrite(9);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x80;
				longBeHandler.set(bytes, writeIndex + 1, v - 0x8000_0000_0000_0000L);
				WriteIndex = writeIndex + 9;
			}
		}
	}

	public static int writeULongSize(long v) {
		//@formatter:off
		if (v <                 0x80 ) return v >= 0 ? 1 : 9;
		if (v <               0x4000 ) return 2;
		if (v <            0x20_0000 ) return 3;
		if (v <          0x1000_0000 ) return 4;
		if (v <        0x8_0000_0000L) return 5;
		if (v <      0x400_0000_0000L) return 6;
		if (v <   0x2_0000_0000_0000L) return 7;
		if (v < 0x100_0000_0000_0000L) return 8;
		return 9;
		//@formatter:on
	}

	// 参数v被看作是无符号64位整数
	public void WriteULong(long v) {
		if (v < 0x80) {
			if (v >= 0) { // 0xxx xxxx
				EnsureWrite(1);
				Bytes[WriteIndex++] = (byte)v;
				return;
			}
		} else {
			if (v < 0x4000) { // 10xx xxxx +1B
				EnsureWrite(2);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 8) + 0x80);
				bytes[writeIndex + 1] = (byte)v;
				WriteIndex = writeIndex + 2;
				return;
			}
			if (v < 0x20_0000) { // 110x xxxx +2B
				EnsureWrite(3);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 16) + 0xc0);
				bytes[writeIndex + 1] = (byte)(v >> 8);
				bytes[writeIndex + 2] = (byte)v;
				WriteIndex = writeIndex + 3;
				return;
			}
			if (v < 0x1000_0000) { // 1110 xxxx +3B
				EnsureWrite(4);
				int writeIndex = WriteIndex;
				intBeHandler.set(Bytes, writeIndex, (int)v + 0xe000_0000);
				WriteIndex = writeIndex + 4;
				return;
			}
			if (v < 0x8_0000_0000L) { // 1111 0xxx +4B
				EnsureWrite(5);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 32) + 0xf0);
				intBeHandler.set(bytes, writeIndex + 1, (int)v);
				WriteIndex = writeIndex + 5;
				return;
			}
			if (v < 0x400_0000_0000L) { // 1111 10xx +5B
				EnsureWrite(6);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 40) + 0xf8);
				bytes[writeIndex + 1] = (byte)(v >> 32);
				intBeHandler.set(bytes, writeIndex + 2, (int)v);
				WriteIndex = writeIndex + 6;
				return;
			}
			if (v < 0x2_0000_0000_0000L) { // 1111 110x +6B
				EnsureWrite(7);
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((v >> 48) + 0xfc);
				bytes[writeIndex + 1] = (byte)(v >> 40);
				bytes[writeIndex + 2] = (byte)(v >> 32);
				intBeHandler.set(bytes, writeIndex + 3, (int)v);
				WriteIndex = writeIndex + 7;
				return;
			}
			if (v < 0x100_0000_0000_0000L) { // 1111 1110 +7B
				EnsureWrite(8);
				int writeIndex = WriteIndex;
				longBeHandler.set(Bytes, writeIndex, v + 0xfe00_0000_0000_0000L);
				WriteIndex = writeIndex + 8;
				return;
			}
		}
		EnsureWrite(9); // 1111 1111 +8B
		byte[] bytes = Bytes;
		int writeIndex = WriteIndex;
		bytes[writeIndex] = (byte)0xff;
		longBeHandler.set(bytes, writeIndex + 1, v);
		WriteIndex = writeIndex + 9;
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

	public void WriteVector2(Vector2 v) {
		EnsureWrite(8);
		byte[] bytes = Bytes;
		int i = WriteIndex;
		intLeHandler.set(bytes, i, Float.floatToRawIntBits(v.x));
		intLeHandler.set(bytes, i + 4, Float.floatToRawIntBits(v.y));
		WriteIndex = i + 8;
	}

	public void WriteVector3(Vector3 v) {
		EnsureWrite(12);
		byte[] bytes = Bytes;
		int i = WriteIndex;
		intLeHandler.set(bytes, i, Float.floatToRawIntBits(v.x));
		intLeHandler.set(bytes, i + 4, Float.floatToRawIntBits(v.y));
		intLeHandler.set(bytes, i + 8, Float.floatToRawIntBits(v.z));
		WriteIndex = i + 12;
	}

	public void WriteVector4(Vector4 v) {
		EnsureWrite(16);
		byte[] bytes = Bytes;
		int i = WriteIndex;
		intLeHandler.set(bytes, i, Float.floatToRawIntBits(v.x));
		intLeHandler.set(bytes, i + 4, Float.floatToRawIntBits(v.y));
		intLeHandler.set(bytes, i + 8, Float.floatToRawIntBits(v.z));
		intLeHandler.set(bytes, i + 12, Float.floatToRawIntBits(v.w));
		WriteIndex = i + 16;
	}

	public void WriteQuaternion(Quaternion v) {
		WriteVector4(v);
	}

	public void WriteVector2Int(Vector2Int v) {
		WriteInt(v.x);
		WriteInt(v.y);
	}

	public void WriteVector3Int(Vector3Int v) {
		WriteInt(v.x);
		WriteInt(v.y);
		WriteInt(v.z);
	}

	public long ReadLong1() {
		ensureRead(1);
		return Bytes[ReadIndex++] & 0xff;
	}

	public long ReadLong2BE() {
		ensureRead(2);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 2;
		return ((bytes[readIndex] & 0xff) << 8) +
				(bytes[readIndex + 1] & 0xff);
	}

	public long ReadLong3BE() {
		ensureRead(3);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 3;
		return ((bytes[readIndex] & 0xff) << 16) +
				((bytes[readIndex + 1] & 0xff) << 8) +
				(bytes[readIndex + 2] & 0xff);
	}

	public long ReadLong4BE() {
		ensureRead(4);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 4;
		return (int)intBeHandler.get(Bytes, readIndex) & 0xffff_ffffL;
	}

	public long ReadLong5BE() {
		ensureRead(5);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 5;
		return ((long)(bytes[readIndex] & 0xff) << 32) +
				((int)intBeHandler.get(bytes, readIndex + 1) & 0xffff_ffffL);
	}

	public long ReadLong6BE() {
		ensureRead(6);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 6;
		return ((long)(bytes[readIndex] & 0xff) << 40) +
				((long)(bytes[readIndex + 1] & 0xff) << 32) +
				((int)intBeHandler.get(bytes, readIndex + 2) & 0xffff_ffffL);
	}

	public long ReadLong7BE() {
		ensureRead(7);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 7;
		return ((long)(bytes[readIndex] & 0xff) << 48) +
				((long)(bytes[readIndex + 1] & 0xff) << 40) +
				((long)(bytes[readIndex + 2] & 0xff) << 32) +
				((int)intBeHandler.get(bytes, readIndex + 3) & 0xffff_ffffL);
	}

	public long ReadLong8BE() {
		ensureRead(8);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 8;
		return (long)longBeHandler.get(Bytes, readIndex);
	}

	public long ReadLong() {
		ensureRead(1);
		int b = Bytes[ReadIndex++];
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

	public void WriteInt(int v) {
		WriteLong(v);
	}

	public int ReadInt() {
		return (int)ReadLong();
	}

	public void WriteFloat(float v) {
		WriteInt4(Float.floatToRawIntBits(v));
	}

	public float ReadFloat() {
		ensureRead(4);
		float v = ToFloat(Bytes, ReadIndex);
		ReadIndex += 4;
		return v;
	}

	public void WriteDouble(double v) {
		WriteLong8(Double.doubleToRawLongBits(v));
	}

	public double ReadDouble() {
		ensureRead(8);
		double v = ToDouble(Bytes, ReadIndex);
		ReadIndex += 8;
		return v;
	}

	public static int utf8Size(String str) {
		if (str == null)
			return 0;
		int bn = 0;
		for (int i = 0, cn = str.length(); i < cn; i++) {
			int c = str.charAt(i);
			if (c < 0x80)
				bn++;
			else if ((c & 0xfc00) == 0xd800 && i + 1 < cn && (str.charAt(i + 1) & 0xfc00) == 0xdc00) { // UTF-16 surrogate
				bn += 4;
				i++;
			} else
				bn += (c < 0x800 ? 2 : 3);
		}
		return bn;
	}

	public void WriteString(String str) {
		int bn = utf8Size(str);
		if (bn <= 0) {
			WriteByte(0);
			return;
		}
		WriteUInt(bn);
		EnsureWrite(bn);
		byte[] buf = Bytes;
		int wi = WriteIndex;
		int cn = str.length();
		if (bn == cn) {
			for (int i = 0; i < cn; i++)
				buf[wi++] = (byte)str.charAt(i);
		} else {
			for (int i = 0; i < cn; i++) {
				int c = str.charAt(i);
				if (c < 0x80)
					buf[wi++] = (byte)c; // 0xxx xxxx
				else {
					if (c < 0x800)
						buf[wi++] = (byte)(0xc0 + (c >> 6)); // 110x xxxx  10xx xxxx
					else {
						if ((c & 0xfc00) == 0xd800 && i + 1 < cn && ((bn = str.charAt(i + 1)) & 0xfc00) == 0xdc00) { // UTF-16 surrogate
							i++;
							c = (c << 10) + bn + (0x10000 - (0xd800 << 10) - 0xdc00);
							buf[wi++] = (byte)(0xf0 + (c >> 18)); // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
							buf[wi++] = (byte)(0x80 + ((c >> 12) & 0x3f));
						} else
							buf[wi++] = (byte)(0xe0 + (c >> 12)); // 1110 xxxx  10xx xxxx  10xx xxxx
						buf[wi++] = (byte)(0x80 + ((c >> 6) & 0x3f));
					}
					buf[wi++] = (byte)(0x80 + (c & 0x3f));
				}
			}
		}
		WriteIndex = wi;
	}

	public String ReadString() {
		int n = ReadUInt();
		ensureRead(n);
		String v = new String(Bytes, ReadIndex, n, StandardCharsets.UTF_8);
		ReadIndex += n;
		return v;
	}

	public void WriteBytes(byte[] v) {
		WriteBytes(v, 0, v.length);
	}

	public void WriteBytes(byte[] v, int offset, int length) {
		WriteUInt(length);
		EnsureWrite(length);
		System.arraycopy(v, offset, Bytes, WriteIndex, length);
		WriteIndex += length;
	}

	public void WriteBinary(Binary binary) {
		binary.encode(this);
	}

	public Binary ReadBinary() {
		return new Binary(ReadBytes());
	}

	public byte[] ReadBytes() {
		int n = ReadUInt();
		if (n == 0)
			return Empty;
		ensureRead(n);
		byte[] v = new byte[n];
		System.arraycopy(Bytes, ReadIndex, v, 0, n);
		ReadIndex += n;
		return v;
	}

	public void SkipBytes() {
		int n = ReadUInt();
		ensureRead(n);
		ReadIndex += n;
	}

	public void SkipBytes4() {
		int n = ReadInt4();
		ensureRead(n);
		ReadIndex += n;
	}

	/**
	 * 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 */
	public ByteBuffer ReadByteBuffer() {
		int n = ReadUInt();
		ensureRead(n);
		int cur = ReadIndex;
		ReadIndex += n;
		return Wrap(Bytes, cur, n);
	}

	public void WriteByteBuffer(ByteBuffer o) {
		WriteBytes(o.Bytes, o.ReadIndex, o.Size());
	}

	@Override
	public String toString() {
		return BitConverter.toStringWithLimit(Bytes, ReadIndex, Size(), 16);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ByteBuffer)
			return equals((ByteBuffer)other);
		if (other instanceof byte[]) {
			var bytes = (byte[])other;
			return Arrays.equals(Bytes, ReadIndex, WriteIndex, bytes, 0, bytes.length);
		}
		if (other instanceof Binary) {
			var binary = (Binary)other;
			return Arrays.equals(Bytes, ReadIndex, WriteIndex,
					binary.bytesUnsafe(), binary.getOffset(), binary.getOffset() + binary.size());
		}
		return false;
	}

	public boolean equals(ByteBuffer other) {
		return this == other || other != null &&
				Arrays.equals(Bytes, ReadIndex, WriteIndex, other.Bytes, other.ReadIndex, other.WriteIndex);
	}

	public static int calc_hashnr(long value) {
		return (int)((value * 0x9E3779B97F4A7C15L) >> 32);
	}

	public static int calc_hashnr(String str) {
		int hash = 0;
		for (int i = 0, n = str.length(); i < n; i++)
			hash = (hash * 16777619) ^ str.charAt(i);
		return hash;
	}

	public static int calc_hashnr(byte[] keys) {
		return calc_hashnr(keys, 0, keys.length);
	}

	public static int calc_hashnr(byte[] keys, int offset, int len) {
		int hash = 0;
		for (int end = offset + len; offset < end; offset++)
			hash = (hash * 16777619) ^ keys[offset];
		return hash;
	}

	@Override
	public int hashCode() {
		return calc_hashnr(Bytes, ReadIndex, Size());
	}

	// 只能增加新的类型定义，增加时记得同步 SkipUnknownField
	public static final int
			INTEGER = 0, // byte,short,int,long,bool
			FLOAT = 1, // float
			DOUBLE = 2, // double
			BYTES = 3, // binary,string
			LIST = 4, // list,set
			MAP = 5, // map
			BEAN = 6, // bean
			DYNAMIC = 7, // dynamic
			VECTOR2 = 8, // float{x,y}
			VECTOR2INT = 9, // int{x,y}
			VECTOR3 = 10, // float{x,y,z}
			VECTOR3INT = 11, // int{x,y,z}
			VECTOR4 = 12; // float{x,y,z,w} Quaternion

	public static final int TAG_SHIFT = 4;
	public static final int TAG_MASK = (1 << TAG_SHIFT) - 1;
	public static final int ID_MASK = 0xff - TAG_MASK;

	/*
	// 在生成代码的时候使用这个方法检查。生成后的代码不使用这个方法。
	// 可以定义的最大 Variable.Id 为 Zeze.Transaction.Bean.MaxVariableId
	public static int MakeTagId(int tag, int id) {
	    if (tag < 0 || tag > TAG_MAX)
	        throw new OverflowException("tag < 0 || tag > TAG_MAX");
	    if (id < 0 || id > ID_MASK)
	        throw new OverflowException("id < 0 || id > ID_MASK");

	    return (id << TAG_SHIFT) + tag;
	}

	public static int GetTag(int tagId) {
	    return tagId & TAG_MASK;
	}

	public static int GetId(int tagId) {
	}
	*/

	public static void VerifyArrayIndex(byte[] bytes, int offset, int length) {
		int bytesLen = bytes.length;
		long offsetL = offset & 0xffff_ffffL;
		long endIndexL = (offset + length) & 0xffff_ffffL;
		if (endIndexL > bytesLen || offsetL > endIndexL)
			throw new IllegalArgumentException(String.format("%d,%d,%d", bytesLen, offset, length));
	}

	public static ByteBuffer encode(Serializable sa) {
		int preAllocSize = sa.preAllocSize();
		ByteBuffer bb = Allocate(Math.min(preAllocSize, 65536));
		sa.encode(bb);
		if (preAllocSize < bb.WriteIndex)
			sa.preAllocSize(bb.WriteIndex);
		return bb;
	}

	public void encode(Collection<? extends Serializable> c) {
		WriteUInt(c.size());
		for (var s : c)
			s.encode(this);
	}

	public <T extends Serializable> void decode(Collection<T> c, Supplier<T> factory) {
		for (int n = ReadUInt(); n > 0; n--) {
			T v = factory.get();
			v.decode(this);
			c.add(v);
		}
	}

	public int WriteTag(int lastVarId, int varId, int type) {
		int deltaId = varId - lastVarId;
		if (deltaId < 0xf)
			WriteByte((deltaId << TAG_SHIFT) + type);
		else {
			WriteByte(0xf0 + type);
			WriteUInt(deltaId - 0xf);
		}
		return varId;
	}

	public void WriteListType(int listSize, int elemType) {
		if (listSize < 0xf)
			WriteByte((listSize << TAG_SHIFT) + elemType);
		else {
			WriteByte(0xf0 + elemType);
			WriteUInt(listSize - 0xf);
		}
	}

	public void WriteMapType(int mapSize, int keyType, int valueType) {
		WriteByte((keyType << TAG_SHIFT) + valueType);
		WriteUInt(mapSize);
	}

	public int ReadTagSize(int tagByte) {
		int deltaId = (tagByte & ID_MASK) >> TAG_SHIFT;
		return deltaId < 0xf ? deltaId : 0xf + ReadUInt();
	}

	public boolean ReadBool(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return ReadLong() != 0;
		if (type == FLOAT)
			return ReadFloat() != 0;
		if (type == DOUBLE)
			return ReadDouble() != 0;
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return false;
		}
		throw new IllegalStateException("can not ReadBool for type=" + type);
	}

	public byte ReadByte(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return (byte)ReadLong();
		if (type == FLOAT)
			return (byte)ReadFloat();
		if (type == DOUBLE)
			return (byte)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return 0;
		}
		throw new IllegalStateException("can not ReadByte for type=" + type);
	}

	public short ReadShort(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return (short)ReadLong();
		if (type == FLOAT)
			return (short)ReadFloat();
		if (type == DOUBLE)
			return (short)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return 0;
		}
		throw new IllegalStateException("can not ReadShort for type=" + type);
	}

	public int ReadInt(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return (int)ReadLong();
		if (type == FLOAT)
			return (int)ReadFloat();
		if (type == DOUBLE)
			return (int)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return 0;
		}
		throw new IllegalStateException("can not ReadInt for type=" + type);
	}

	public long ReadLong(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return ReadLong();
		if (type == FLOAT)
			return (long)ReadFloat();
		if (type == DOUBLE)
			return (long)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return 0;
		}
		throw new IllegalStateException("can not ReadLong for type=" + type);
	}

	public float ReadFloat(int type) {
		type &= TAG_MASK;
		if (type == FLOAT)
			return ReadFloat();
		if (type == DOUBLE)
			return (float)ReadDouble();
		if (type == INTEGER)
			return ReadLong();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return 0;
		}
		throw new IllegalStateException("can not ReadFloat for type=" + type);
	}

	public double ReadDouble(int type) {
		type &= TAG_MASK;
		if (type == DOUBLE)
			return ReadDouble();
		if (type == FLOAT)
			return ReadFloat();
		if (type == INTEGER)
			return ReadLong();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return 0;
		}
		throw new IllegalStateException("can not ReadDouble for type=" + type);
	}

	public Binary ReadBinary(int type) {
		type &= TAG_MASK;
		if (type == BYTES)
			return ReadBinary();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Binary.Empty;
		}
		throw new IllegalStateException("can not ReadBinary for type=" + type);
	}

	public String ReadString(int type) {
		type &= TAG_MASK;
		if (type == BYTES)
			return ReadString();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return "";
		}
		throw new IllegalStateException("can not ReadString for type=" + type);
	}

	public Vector2 ReadVector2() {
		ensureRead(8);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		ReadIndex = i + 8;
		return new Vector2(x, y);
	}

	public Vector3 ReadVector3() {
		ensureRead(12);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		float z = ToFloat(Bytes, i + 8);
		ReadIndex = i + 12;
		return new Vector3(x, y, z);
	}

	public Vector4 ReadVector4() {
		ensureRead(16);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		float z = ToFloat(Bytes, i + 8);
		float w = ToFloat(Bytes, i + 12);
		ReadIndex = i + 16;
		return new Vector4(x, y, z, w);
	}

	public Quaternion ReadQuaternion() {
		ensureRead(16);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		float z = ToFloat(Bytes, i + 8);
		float w = ToFloat(Bytes, i + 12);
		ReadIndex = i + 16;
		return new Quaternion(x, y, z, w);
	}

	public Vector2Int ReadVector2Int() {
		int x = ReadInt();
		int y = ReadInt();
		return new Vector2Int(x, y);
	}

	public Vector3Int ReadVector3Int() {
		int x = ReadInt();
		int y = ReadInt();
		int z = ReadInt();
		return new Vector3Int(x, y, z);
	}

	public Vector2 ReadVector2(int type) {
		type &= TAG_MASK;
		if (type == VECTOR2)
			return ReadVector2();
		if (type == VECTOR3)
			return ReadVector3();
		if (type == VECTOR4)
			return ReadVector4();
		if (type == VECTOR2INT)
			return new Vector2(ReadVector2Int());
		if (type == VECTOR3INT)
			return new Vector3(ReadVector3Int());
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Vector2.ZERO;
		}
		throw new IllegalStateException("can not ReadVector2 for type=" + type);
	}

	public Vector3 ReadVector3(int type) {
		type &= TAG_MASK;
		if (type == VECTOR3)
			return ReadVector3();
		if (type == VECTOR2)
			return new Vector3(ReadVector2());
		if (type == VECTOR4)
			return ReadVector4();
		if (type == VECTOR3INT)
			return new Vector3(ReadVector3Int());
		if (type == VECTOR2INT)
			return new Vector3(ReadVector2Int());
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Vector3.ZERO;
		}
		throw new IllegalStateException("can not ReadVector3 for type=" + type);
	}

	public Vector4 ReadVector4(int type) {
		type &= TAG_MASK;
		if (type == VECTOR4)
			return ReadVector4();
		if (type == VECTOR3)
			return new Vector4(ReadVector3());
		if (type == VECTOR2)
			return new Vector4(ReadVector2());
		if (type == VECTOR3INT)
			return new Vector4(ReadVector3Int());
		if (type == VECTOR2INT)
			return new Vector4(ReadVector2Int());
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Vector4.ZERO;
		}
		throw new IllegalStateException("can not ReadVector4 for type=" + type);
	}

	public Quaternion ReadQuaternion(int type) {
		type &= TAG_MASK;
		if (type == VECTOR4)
			return ReadQuaternion();
		if (type == VECTOR3)
			return new Quaternion(ReadVector3());
		if (type == VECTOR2)
			return new Quaternion(ReadVector2());
		if (type == VECTOR3INT)
			return new Quaternion(ReadVector3Int());
		if (type == VECTOR2INT)
			return new Quaternion(ReadVector2Int());
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Quaternion.ZERO;
		}
		throw new IllegalStateException("can not ReadQuaternion for type=" + type);
	}

	public Vector2Int ReadVector2Int(int type) {
		type &= TAG_MASK;
		if (type == VECTOR2INT)
			return ReadVector2Int();
		if (type == VECTOR3INT)
			return ReadVector3Int();
		if (type == VECTOR2)
			return new Vector2Int(ReadVector2());
		if (type == VECTOR3)
			return new Vector3Int(ReadVector3());
		if (type == VECTOR4)
			return new Vector3Int(ReadVector4());
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Vector2Int.ZERO;
		}
		throw new IllegalStateException("can not ReadVector2Int for type=" + type);
	}

	public Vector3Int ReadVector3Int(int type) {
		type &= TAG_MASK;
		if (type == VECTOR3INT)
			return ReadVector3Int();
		if (type == VECTOR2INT)
			return new Vector3Int(ReadVector2Int());
		if (type == VECTOR3)
			return new Vector3Int(ReadVector3());
		if (type == VECTOR2)
			return new Vector3Int(ReadVector2());
		if (type == VECTOR4)
			return new Vector3Int(ReadVector4());
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return Vector3Int.ZERO;
		}
		throw new IllegalStateException("can not ReadVector3Int for type=" + type);
	}

	public <T extends Serializable> T ReadBean(T bean, int type) {
		type &= TAG_MASK;
		if (type == BEAN)
			bean.decode(this);
		else if (type == DYNAMIC) {
			ReadLong();
			bean.decode(this);
		} else if (IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(type);
		else
			throw new IllegalStateException("can not ReadBean(" + bean.getClass().getName() + ") for type=" + type);
		return bean;
	}

	public DynamicBean ReadDynamic(DynamicBean dynBean, int type) {
		type &= TAG_MASK;
		if (type == DYNAMIC) {
			dynBean.decode(this);
			return dynBean;
		}
		if (type == BEAN) {
			dynBean.newBean(0).decode(this);
			return dynBean;
		}
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return dynBean;
		}
		throw new IllegalStateException("can not ReadDynamic for type=" + type);
	}

	public DynamicBeanData ReadDynamic(DynamicBeanData dynBean, int type) {
		type &= TAG_MASK;
		if (type == DYNAMIC) {
			dynBean.decode(this);
			return dynBean;
		}
		if (type == BEAN) {
			var bean = dynBean.getCreateData().apply(0);
			if (bean != null) {
				bean.decode(this);
				return dynBean;
			}
		}
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(type);
			return dynBean;
		}
		throw new IllegalStateException("can not ReadDynamic for type=" + type);
	}

	public void SkipUnknownFieldOrThrow(int type, String curType) {
		if (IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(type);
		else
			throw new IllegalStateException("can not read " + curType + " for type=" + type);
	}

	public void SkipUnknownField(int type, int count) {
		while (--count >= 0)
			SkipUnknownField(type);
	}

	public void SkipUnknownField(int type1, int type2, int count) {
		type1 |= 0x10; // ensure high bits not zero
		type2 |= 0x10; // ensure high bits not zero
		while (--count >= 0) {
			SkipUnknownField(type1);
			SkipUnknownField(type2);
		}
	}

	public void SkipUnknownField(int type) {
		switch (type & TAG_MASK) {
		case INTEGER:
			ReadLong();
			return;
		case FLOAT:
			if (type == 1) // FLOAT == 1
				return;
			ensureRead(4);
			ReadIndex += 4;
			return;
		case DOUBLE:
		case VECTOR2:
			ensureRead(8);
			ReadIndex += 8;
			return;
		case VECTOR2INT:
			ReadLong();
			ReadLong();
			return;
		case VECTOR3:
			ensureRead(12);
			ReadIndex += 12;
			return;
		case VECTOR3INT:
			ReadLong();
			ReadLong();
			ReadLong();
			return;
		case VECTOR4:
			ensureRead(16);
			ReadIndex += 16;
			return;
		case BYTES:
			SkipBytes();
			return;
		case LIST:
			int t = ReadByte();
			SkipUnknownField(t, ReadTagSize(t));
			return;
		case MAP:
			t = ReadByte();
			SkipUnknownField(t >> TAG_SHIFT, t, ReadUInt());
			return;
		case DYNAMIC:
			ReadLong();
			//noinspection fallthrough
		case BEAN:
			while ((t = ReadByte()) != 0) {
				if ((t & ID_MASK) == 0xf0)
					ReadUInt();
				SkipUnknownField(t);
			}
			return;
		default:
			throw new IllegalStateException("SkipUnknownField: " + type);
		}
	}

	public static <T> void BuildString(StringBuilder sb, Iterable<T> c) {
		sb.append('[');
		int i = sb.length();
		if (c != null) {
			for (var e : c)
				sb.append(e).append(',');
		}
		int j = sb.length();
		if (i == j)
			sb.append(']');
		else
			sb.setCharAt(j - 1, ']');
	}

	public static <TK, TV> void BuildString(StringBuilder sb, Map<TK, TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			for (var e : map.entrySet())
				sb.append(e.getKey()).append(':').append(e.getValue()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TV> void BuildString(StringBuilder sb, IntHashMap<TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			for (var it = map.iterator(); it.moveToNext(); )
				sb.append(it.key()).append(':').append(it.value()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TV> void BuildString(StringBuilder sb, LongHashMap<TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			for (var it = map.iterator(); it.moveToNext(); )
				sb.append(it.key()).append(':').append(it.value()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TK> void BuildString(StringBuilder sb, IdentityHashSet<TK> set) {
		sb.append('{');
		if (set == null || set.isEmpty())
			sb.append('}');
		else {
			for (var it = set.iterator(); it.moveToNext(); )
				sb.append(it.value()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <T> void BuildSortedString(StringBuilder sb, Iterable<T> c) {
		var strs = new ArrayList<String>();
		if (c != null) {
			for (var e : c)
				strs.add(e.toString());
		}
		Collections.sort(strs); // 排序，便于测试比较。

		sb.append('[');
		int i = sb.length();
		for (var e : strs)
			sb.append(e).append(',');
		int j = sb.length();
		if (i == j)
			sb.append(']');
		else
			sb.setCharAt(j - 1, ']');
	}

	public static <TK, TV> void BuildSortedString(StringBuilder sb, Map<TK, TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			var strs = new ArrayList<String>();
			for (var e : map.entrySet())
				strs.add(e.getKey() + ":" + e.getValue());
			Collections.sort(strs); // 排序，便于测试比较。

			for (var str : strs)
				sb.append(str).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TV> void BuildSortedString(StringBuilder sb, IntHashMap<TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			var strs = new ArrayList<String>();
			for (var it = map.iterator(); it.moveToNext(); )
				strs.add(it.key() + ":" + it.value());
			Collections.sort(strs); // 排序，便于测试比较。

			for (var str : strs)
				sb.append(str).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static boolean Equals(byte[] left, byte[] right) {
		return Arrays.equals(left, right);
	}

	public static int Compare(byte[] left, byte[] right) {
		return Arrays.compareUnsigned(left, right);
	}

	public static byte[] Copy(byte[] src) {
		return src.clone();
	}

	public static byte[] Copy(byte[] src, int offset, int length) {
		return Arrays.copyOfRange(src, offset, offset + length);
	}

	@Override
	public int compareTo(@NotNull ByteBuffer o) {
		return Arrays.compareUnsigned(Bytes, ReadIndex, WriteIndex, o.Bytes, o.ReadIndex, o.WriteIndex);
	}
}
