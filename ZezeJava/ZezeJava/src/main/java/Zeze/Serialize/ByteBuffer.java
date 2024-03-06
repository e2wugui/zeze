package Zeze.Serialize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import Zeze.Net.Binary;
import Zeze.Util.BitConverter;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashMap;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ByteBuffer implements IByteBuffer, Comparable<ByteBuffer> {
	public static final @NotNull VarHandle intLeHandler = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
	public static final @NotNull VarHandle intBeHandler = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
	public static final @NotNull VarHandle longLeHandler = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
	public static final @NotNull VarHandle longBeHandler = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
	public static final byte[] Empty = new byte[0];

	public byte @NotNull [] Bytes;
	public int ReadIndex;
	public int WriteIndex;

	@Override
	public int getReadIndex() {
		return ReadIndex;
	}

	@Override
	public void setReadIndex(int ri) {
		ReadIndex = ri;
	}

	@Override
	public int getWriteIndex() {
		return WriteIndex;
	}

	@Override
	public int capacity() {
		return Bytes.length;
	}

	@Override
	public int size() {
		return WriteIndex - ReadIndex;
	}

	@Override
	public boolean isEmpty() {
		return ReadIndex >= WriteIndex;
	}

	public static @NotNull ByteBuffer Wrap(@NotNull ByteBuffer bb) {
		return Wrap(bb.Bytes, bb.ReadIndex, bb.size());
	}

	public static @NotNull ByteBuffer Wrap(byte @NotNull [] bytes) {
		return new ByteBuffer(bytes, 0, bytes.length);
	}

	public static @NotNull ByteBuffer Wrap(byte @NotNull [] bytes, int length) {
		VerifyArrayIndex(bytes, length);
		return new ByteBuffer(bytes, 0, length);
	}

	public static @NotNull ByteBuffer Wrap(byte @NotNull [] bytes, int offset, int length) {
		VerifyArrayIndex(bytes, offset, length);
		return new ByteBuffer(bytes, offset, offset + length);
	}

	public static @NotNull ByteBuffer Wrap(@NotNull Binary binary) {
		return binary.Wrap();
	}

	public static @NotNull ByteBuffer Allocate() {
		return Allocate(16);
	}

	public static @NotNull ByteBuffer Allocate(int capacity) {
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

	protected ByteBuffer(byte @NotNull [] bytes, int readIndex, int writeIndex) {
		Bytes = bytes;
		ReadIndex = readIndex;
		WriteIndex = writeIndex;
	}

	@Override
	public void FreeInternalBuffer() {
		Bytes = Empty;
		Reset();
	}

	public void wraps(byte @NotNull [] bytes) {
		Bytes = bytes;
		ReadIndex = 0;
		WriteIndex = bytes.length;
	}

	public void wraps(byte @NotNull [] bytes, int length) {
		VerifyArrayIndex(bytes, length);
		Bytes = bytes;
		ReadIndex = 0;
		WriteIndex = length;
	}

	public void wraps(byte @NotNull [] bytes, int offset, int length) {
		VerifyArrayIndex(bytes, offset, length);
		Bytes = bytes;
		ReadIndex = offset;
		WriteIndex = offset + length;
	}

	public void Append(byte @NotNull [] bs) {
		Append(bs, 0, bs.length);
	}

	public void Append(byte @NotNull [] bs, int offset, int len) {
		EnsureWrite(len);
		System.arraycopy(bs, offset, Bytes, WriteIndex, len);
		WriteIndex += len;
	}

	public void Replace(int writeIndex, byte @NotNull [] src) {
		Replace(writeIndex, src, 0, src.length);
	}

	public void Replace(int writeIndex, byte @NotNull [] src, int offset, int len) {
		if (writeIndex < ReadIndex || writeIndex + len > WriteIndex) {
			throw new IllegalStateException("Replace writeIndex=" + writeIndex + ", len=" + len
					+ " at " + ReadIndex + '/' + WriteIndex);
		}
		System.arraycopy(src, offset, Bytes, writeIndex, len);
	}

	public int BeginWriteWithSize4() {
		int saveSize = size();
		EnsureWrite(4);
		WriteIndex += 4;
		return saveSize;
	}

	public void EndWriteWithSize4(int saveSize) {
		int oldWriteIndex = ReadIndex + saveSize;
		if (oldWriteIndex + 4 > WriteIndex) {
			throw new IllegalStateException("EndWriteWithSize4 saveSize=" + saveSize
					+ " at " + ReadIndex + '/' + WriteIndex);
		}
		intLeHandler.set(Bytes, oldWriteIndex, WriteIndex - oldWriteIndex - 4);
	}

	public static int ToInt(byte @NotNull [] bytes, int offset) {
		return (int)intLeHandler.get(bytes, offset);
	}

	public static int ToIntBE(byte @NotNull [] bytes, int offset) {
		return (int)intBeHandler.get(bytes, offset);
	}

	public static long ToLong(byte @NotNull [] bytes, int offset) {
		return (long)longLeHandler.get(bytes, offset);
	}

	public static long ToLongBE(byte @NotNull [] bytes, int offset) {
		return (long)longBeHandler.get(bytes, offset);
	}

	@SuppressWarnings("fallthrough")
	public static long ToLong(byte @NotNull [] bytes, int offset, int length) {
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
	public static long ToLongBE(byte @NotNull [] bytes, int offset, int length) {
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

	public static float ToFloat(byte @NotNull [] bytes, int offset) {
		return Float.intBitsToFloat(ToInt(bytes, offset));
	}

	public static float ToFloatBE(byte @NotNull [] bytes, int offset) {
		return Float.intBitsToFloat(ToIntBE(bytes, offset));
	}

	public static double ToDouble(byte @NotNull [] bytes, int offset) {
		return Double.longBitsToDouble(ToLong(bytes, offset));
	}

	public static double ToDoubleBE(byte @NotNull [] bytes, int offset) {
		return Double.longBitsToDouble(ToLongBE(bytes, offset));
	}

	/**
	 * 这个方法把剩余可用数据移到buffer开头。
	 * 【注意】这个方法会修改ReadIndex，WriteIndex。
	 * 最好仅在全部读取写入处理完成以后调用处理一次，
	 * 为下一次写入读取做准备。
	 */
	public void Compact() {
		int size = size();
		if (size > 0) {
			if (ReadIndex > 0) {
				System.arraycopy(Bytes, ReadIndex, Bytes, 0, size);
				ReadIndex = 0;
				WriteIndex = size;
			}
		} else
			Reset();
	}

	@Override
	public byte @NotNull [] Copy() {
		int size = size();
		if (size == 0)
			return Empty;
		byte[] copy = new byte[size];
		System.arraycopy(Bytes, ReadIndex, copy, 0, size);
		return copy;
	}

	public byte @NotNull [] CopyIf() {
		return WriteIndex == Bytes.length && ReadIndex == 0 ? Bytes : Copy();
	}

	@Override
	public void Reset() {
		ReadIndex = 0;
		WriteIndex = 0;
	}

	protected int toPower2(int needSize) {
		if (Integer.compareUnsigned(needSize, 0x4000_0000) > 0) {
			throw new IllegalStateException("invalid needSize=" + needSize
					+ " at " + ReadIndex + '/' + WriteIndex);
		}
		int size = 16;
		while (size < needSize)
			size <<= 1;
		return size;
	}

	private void growCapacity(int newSize) {
		byte[] newBytes = new byte[toPower2(newSize)];
		System.arraycopy(Bytes, ReadIndex, newBytes, 0, WriteIndex -= ReadIndex);
		ReadIndex = 0;
		Bytes = newBytes;
	}

	public void EnsureWrite(int size) {
		int newSize = WriteIndex + size;
		if (newSize > Bytes.length)
			growCapacity(newSize);
	}

	private void growCapacityNoCompact(int newSize) {
		byte[] newBytes = new byte[toPower2(newSize)];
		System.arraycopy(Bytes, 0, newBytes, 0, WriteIndex);
		Bytes = newBytes;
	}

	public void ensureWriteNoCompact(int size) {
		int newSize = WriteIndex + size;
		if (newSize > Bytes.length)
			growCapacityNoCompact(newSize);
	}

	@Override
	public void ensureRead(int size) {
		if (ReadIndex + size > WriteIndex)
			throwEnsureReadException(size);
	}

	public void WriteBool(boolean b) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = (byte)(b ? 1 : 0);
	}

	@Override
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

	@Override
	public byte ReadByte() {
		ensureRead(1);
		return Bytes[ReadIndex++];
	}

	public void WriteInt4(int v) {
		EnsureWrite(4);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + 4;
		intLeHandler.set(Bytes, writeIndex, v);
	}

	public void WriteInt4BE(int v) {
		EnsureWrite(4);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + 4;
		intBeHandler.set(Bytes, writeIndex, v);
	}

	public void WriteInt4s(int @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		EnsureWrite(n);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + n;
		for (int i = 0; i < length; i++, writeIndex += 4)
			intLeHandler.set(Bytes, writeIndex, buf[offset + i]);
	}

	@Override
	public int ReadInt4() {
		ensureRead(4);
		int v = ToInt(Bytes, ReadIndex);
		ReadIndex += 4;
		return v;
	}

	@Override
	public void ReadInt4s(int @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		ensureRead(n);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + n;
		for (int i = 0; i < length; i++, readIndex += 4)
			buf[offset + i] = ToInt(Bytes, readIndex);
	}

	public void WriteLong8(long v) {
		EnsureWrite(8);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + 8;
		longLeHandler.set(Bytes, writeIndex, v);
	}

	public void WriteLong8BE(long v) {
		EnsureWrite(8);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + 8;
		longBeHandler.set(Bytes, writeIndex, v);
	}

	public void WriteLong8s(long @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		EnsureWrite(n);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + n;
		for (int i = 0; i < length; i++, writeIndex += 8)
			longLeHandler.set(Bytes, writeIndex, buf[offset + i]);
	}

	@Override
	public long ReadLong8() {
		ensureRead(8);
		long v = ToLong(Bytes, ReadIndex);
		ReadIndex += 8;
		return v;
	}

	@Override
	public void ReadLong8s(long @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		ensureRead(n);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + n;
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = ToLong(Bytes, readIndex);
	}

	// v看成无符号数时,与WriteULongSize的结果一致,即相当于WriteULongSize(v & 0xffff_ffffL);
	public static int WriteUIntSize(int v) {
		long u = v & 0xffff_ffffL;
		//@formatter:off
		if (u <        0x80) return 1;
		if (u <      0x4000) return 2;
		if (u <   0x20_0000) return 3;
		if (u < 0x1000_0000) return 4;
		return 5;
		//@formatter:on
	}

	// v看成无符号数时,与WriteULong的结果一致,即相当于WriteULong(v & 0xffff_ffffL);
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

	@Override
	public void SkipUInt() {
		ensureRead(1);
		int readIndex = ReadIndex;
		int v = Bytes[readIndex] & 0xff;
		if (v < 0x80)
			ReadIndex = readIndex + 1;
		else if (v < 0xc0) {
			ensureRead(2);
			ReadIndex = readIndex + 2;
		} else if (v < 0xe0) {
			ensureRead(3);
			ReadIndex = readIndex + 3;
		} else if (v < 0xf0) {
			ensureRead(4);
			ReadIndex = readIndex + 4;
		} else {
			ensureRead(5);
			ReadIndex = readIndex + 5;
		}
	}

	public static int WriteLongSize(long v) {
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

	public static int WriteULongSize(long v) {
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

	public void WriteVector2(@NotNull Vector2 v) {
		EnsureWrite(8);
		byte[] bytes = Bytes;
		int i = WriteIndex;
		intLeHandler.set(bytes, i, Float.floatToRawIntBits(v.x));
		intLeHandler.set(bytes, i + 4, Float.floatToRawIntBits(v.y));
		WriteIndex = i + 8;
	}

	public void WriteVector3(@NotNull Vector3 v) {
		EnsureWrite(12);
		byte[] bytes = Bytes;
		int i = WriteIndex;
		intLeHandler.set(bytes, i, Float.floatToRawIntBits(v.x));
		intLeHandler.set(bytes, i + 4, Float.floatToRawIntBits(v.y));
		intLeHandler.set(bytes, i + 8, Float.floatToRawIntBits(v.z));
		WriteIndex = i + 12;
	}

	public void WriteVector4(@NotNull Vector4 v) {
		EnsureWrite(16);
		byte[] bytes = Bytes;
		int i = WriteIndex;
		intLeHandler.set(bytes, i, Float.floatToRawIntBits(v.x));
		intLeHandler.set(bytes, i + 4, Float.floatToRawIntBits(v.y));
		intLeHandler.set(bytes, i + 8, Float.floatToRawIntBits(v.z));
		intLeHandler.set(bytes, i + 12, Float.floatToRawIntBits(v.w));
		WriteIndex = i + 16;
	}

	public void WriteQuaternion(@NotNull Quaternion v) {
		WriteVector4(v);
	}

	public void WriteVector2Int(@NotNull Vector2Int v) {
		WriteInt(v.x);
		WriteInt(v.y);
	}

	public void WriteVector3Int(@NotNull Vector3Int v) {
		WriteInt(v.x);
		WriteInt(v.y);
		WriteInt(v.z);
	}

	@Override
	public long ReadLong2BE() {
		ensureRead(2);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 2;
		return ((bytes[readIndex] & 0xff) << 8) +
				(bytes[readIndex + 1] & 0xff);
	}

	@Override
	public long ReadLong3BE() {
		ensureRead(3);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 3;
		return ((bytes[readIndex] & 0xff) << 16) +
				((bytes[readIndex + 1] & 0xff) << 8) +
				(bytes[readIndex + 2] & 0xff);
	}

	@Override
	public long ReadLong4BE() {
		ensureRead(4);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 4;
		return (int)intBeHandler.get(Bytes, readIndex) & 0xffff_ffffL;
	}

	@Override
	public long ReadLong5BE() {
		ensureRead(5);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 5;
		return ((long)(bytes[readIndex] & 0xff) << 32) +
				((int)intBeHandler.get(bytes, readIndex + 1) & 0xffff_ffffL);
	}

	@Override
	public long ReadLong6BE() {
		ensureRead(6);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 6;
		return ((long)(bytes[readIndex] & 0xff) << 40) +
				((long)(bytes[readIndex + 1] & 0xff) << 32) +
				((int)intBeHandler.get(bytes, readIndex + 2) & 0xffff_ffffL);
	}

	@Override
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

	@Override
	public long ReadLong8BE() {
		ensureRead(8);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 8;
		return (long)longBeHandler.get(Bytes, readIndex);
	}

	public void WriteInt(int v) {
		WriteLong(v);
	}

	public void WriteFloat(float v) {
		WriteInt4(Float.floatToRawIntBits(v));
	}

	public void WriteFloats(float @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		EnsureWrite(n);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + n;
		for (int i = 0; i < length; i++, writeIndex += 4)
			intLeHandler.set(Bytes, writeIndex, Float.floatToRawIntBits(buf[offset + i]));
	}

	@Override
	public float ReadFloat() {
		ensureRead(4);
		float v = ToFloat(Bytes, ReadIndex);
		ReadIndex += 4;
		return v;
	}

	@Override
	public void ReadFloats(float @NotNull [] buf, int offset, int length) {
		int n = length * 4;
		ensureRead(n);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + n;
		for (int i = 0; i < length; i++, readIndex += 4)
			buf[offset + i] = ToFloat(Bytes, readIndex);
	}

	public void WriteDouble(double v) {
		WriteLong8(Double.doubleToRawLongBits(v));
	}

	public void WriteDoubles(double @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		EnsureWrite(n);
		int writeIndex = WriteIndex;
		WriteIndex = writeIndex + n;
		for (int i = 0; i < length; i++, writeIndex += 8)
			longLeHandler.set(Bytes, writeIndex, Double.doubleToRawLongBits(buf[offset + i]));
	}

	@Override
	public double ReadDouble() {
		ensureRead(8);
		double v = ToDouble(Bytes, ReadIndex);
		ReadIndex += 8;
		return v;
	}

	@Override
	public void ReadDoubles(double @NotNull [] buf, int offset, int length) {
		int n = length * 8;
		ensureRead(n);
		int readIndex = ReadIndex;
		ReadIndex = readIndex + n;
		for (int i = 0; i < length; i++, readIndex += 8)
			buf[offset + i] = ToDouble(Bytes, readIndex);
	}

	public static int utf8Size(@Nullable String str) {
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

	public void WriteString(@Nullable String str) {
		int bn = utf8Size(str);
		if (bn <= 0) {
			WriteByte(0);
			return;
		}
		WriteUInt(bn);
		EnsureWrite(bn);
		byte[] buf = Bytes;
		int wi = WriteIndex;
		//noinspection DataFlowIssue
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

	@Override
	public @NotNull String ReadString() {
		int n = ReadUInt();
		if (n == 0)
			return "";
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadString: " + n
					+ " at " + ReadIndex + '/' + WriteIndex);
		}
		ensureRead(n);
		String v = new String(Bytes, ReadIndex, n, StandardCharsets.UTF_8);
		ReadIndex += n;
		return v;
	}

	public void WriteBytes(byte @NotNull [] v) {
		WriteBytes(v, 0, v.length);
	}

	public void WriteBytes(byte @NotNull [] v, int offset, int length) {
		WriteUInt(length);
		EnsureWrite(length);
		System.arraycopy(v, offset, Bytes, WriteIndex, length);
		WriteIndex += length;
	}

	public void WriteBinary(@NotNull Binary binary) {
		binary.encode(this);
	}

	@Override
	public byte @NotNull [] ReadBytes() {
		int n = ReadUInt();
		if (n == 0)
			return Empty;
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadBytes: " + n
					+ " at " + ReadIndex + '/' + WriteIndex);
		}
		ensureRead(n);
		byte[] v = new byte[n];
		System.arraycopy(Bytes, ReadIndex, v, 0, n);
		ReadIndex += n;
		return v;
	}

	@Override
	public void Skip(int n) {
		ensureRead(n);
		ReadIndex += n;
	}

	/**
	 * 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 */
	@Override
	public @NotNull ByteBuffer ReadByteBuffer() {
		int n = ReadUInt();
		if (n < 0) {
			throw new IllegalStateException("invalid length for ReadByteBuffer: " + n
					+ " at " + ReadIndex + '/' + WriteIndex);
		}
		ensureRead(n);
		int cur = ReadIndex;
		ReadIndex = cur + n;
		return Wrap(Bytes, cur, n);
	}

	public void WriteByteBuffer(@NotNull ByteBuffer bb) {
		WriteBytes(bb.Bytes, bb.ReadIndex, bb.size());
	}

	public void WriteJavaObject(@NotNull java.io.Serializable obj) {
		try (var bs = new ByteArrayOutputStream();
			 var os = new ObjectOutputStream(bs)) {
			os.writeObject(obj);
			WriteBytes(bs.toByteArray());
		} catch (IOException e) {
			Task.forceThrow(e);
		}
	}

	@Override
	public String toString() {
		return BitConverter.toStringWithLimit(Bytes, ReadIndex, size(), 16, 4);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other instanceof ByteBuffer)
			return equals((ByteBuffer)other);
		if (other instanceof NioByteBuffer)
			return ((NioByteBuffer)other).bb.equals(java.nio.ByteBuffer.wrap(Bytes, ReadIndex, size()));
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

	public boolean equals(@Nullable ByteBuffer other) {
		return this == other || other != null &&
				Arrays.equals(Bytes, ReadIndex, WriteIndex, other.Bytes, other.ReadIndex, other.WriteIndex);
	}

	public static int calc_hashnr(long value) {
		return (int)((value * 0x9E3779B97F4A7C15L) >> 32);
	}

	public static int calc_hashnr(@NotNull String str) {
		int hash = 0;
		for (int i = 0, n = str.length(); i < n; i++)
			hash = (hash * 16777619) ^ str.charAt(i);
		return hash;
	}

	public static int calc_hashnr(byte @NotNull [] keys) {
		return calc_hashnr(keys, 0, keys.length);
	}

	public static int calc_hashnr(byte @NotNull [] keys, int offset, int len) {
		int hash = 0;
		for (int end = offset + len; offset < end; offset++)
			hash = (hash * 16777619) ^ keys[offset];
		return hash;
	}

	@Override
	public int hashCode() {
		return calc_hashnr(Bytes, ReadIndex, size());
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

	public static @NotNull ByteBuffer encode(@NotNull Serializable sa) {
		int preAllocSize = sa.preAllocSize();
		ByteBuffer bb = Allocate(Math.min(preAllocSize, 65536));
		sa.encode(bb);
		if (preAllocSize < bb.WriteIndex)
			sa.preAllocSize(bb.WriteIndex);
		return bb;
	}

	public void encode(@NotNull Collection<? extends Serializable> c) {
		WriteUInt(c.size());
		for (var s : c)
			s.encode(this);
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

	@Override
	public @NotNull Vector2 ReadVector2() {
		ensureRead(8);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		ReadIndex = i + 8;
		return new Vector2(x, y);
	}

	@Override
	public @NotNull Vector3 ReadVector3() {
		ensureRead(12);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		float z = ToFloat(Bytes, i + 8);
		ReadIndex = i + 12;
		return new Vector3(x, y, z);
	}

	@Override
	public @NotNull Vector4 ReadVector4() {
		ensureRead(16);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		float z = ToFloat(Bytes, i + 8);
		float w = ToFloat(Bytes, i + 12);
		ReadIndex = i + 16;
		return new Vector4(x, y, z, w);
	}

	@Override
	public @NotNull Quaternion ReadQuaternion() {
		ensureRead(16);
		int i = ReadIndex;
		float x = ToFloat(Bytes, i);
		float y = ToFloat(Bytes, i + 4);
		float z = ToFloat(Bytes, i + 8);
		float w = ToFloat(Bytes, i + 12);
		ReadIndex = i + 16;
		return new Quaternion(x, y, z, w);
	}

	@Override
	public @Nullable ByteBuffer readUnknownField(int idx, int tag, @Nullable ByteBuffer unknown) {
		int beginIdx = ReadIndex;
		SkipUnknownField(tag);
		int size = ReadIndex - beginIdx;
		if (size > 0) {
			if (unknown == null)
				unknown = ByteBuffer.Allocate(ByteBuffer.WriteUIntSize(idx) + 1 + ByteBuffer.WriteUIntSize(size) + size);
			unknown.WriteUInt(idx);
			unknown.WriteByte(tag & TAG_MASK);
			unknown.WriteUInt(size);
			unknown.Append(Bytes, beginIdx, size);
			return unknown;
		}
		throw new UnsupportedOperationException("readUnknownField: unsupported for derived bean");
	}

	public int writeUnknownField(int lastIdx, long idx, @NotNull ByteBuffer unknown) {
		int i = (int)idx;
		WriteTag(lastIdx, i, unknown.ReadByte());
		int size = unknown.ReadUInt();
		int readIndex = unknown.ReadIndex;
		Append(unknown.Bytes, readIndex, size);
		unknown.ReadIndex = readIndex + size;
		return i;
	}

	public void writeAllUnknownFields(int lastIdx, long idx, ByteBuffer unknown) {
		while (idx != Long.MAX_VALUE) {
			lastIdx = writeUnknownField(lastIdx, idx, unknown);
			idx = unknown.readUnknownIndex();
		}
	}

	public static <T> void BuildString(@NotNull StringBuilder sb, @Nullable Iterable<T> c) {
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

	public static <TK, TV> void BuildString(@NotNull StringBuilder sb, @Nullable Map<TK, TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			for (var e : map.entrySet())
				sb.append(e.getKey()).append(':').append(e.getValue()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TV> void BuildString(@NotNull StringBuilder sb, @Nullable IntHashMap<TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			for (var it = map.iterator(); it.moveToNext(); )
				sb.append(it.key()).append(':').append(it.value()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TV> void BuildString(@NotNull StringBuilder sb, @Nullable LongHashMap<TV> map) {
		sb.append('{');
		if (map == null || map.isEmpty())
			sb.append('}');
		else {
			for (var it = map.iterator(); it.moveToNext(); )
				sb.append(it.key()).append(':').append(it.value()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <TK> void BuildString(@NotNull StringBuilder sb, @Nullable IdentityHashSet<TK> set) {
		sb.append('{');
		if (set == null || set.isEmpty())
			sb.append('}');
		else {
			for (var it = set.iterator(); it.moveToNext(); )
				sb.append(it.value()).append(',');
			sb.setCharAt(sb.length() - 1, '}');
		}
	}

	public static <T> void BuildSortedString(@NotNull StringBuilder sb, @Nullable Iterable<T> c) {
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

	public static <TK, TV> void BuildSortedString(@NotNull StringBuilder sb, @Nullable Map<TK, TV> map) {
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

	public static <TV> void BuildSortedString(@NotNull StringBuilder sb, @Nullable IntHashMap<TV> map) {
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

	public static boolean Equals(byte @Nullable [] left, byte @Nullable [] right) {
		return Arrays.equals(left, right);
	}

	public static int Compare(byte @Nullable [] left, byte @Nullable [] right) {
		return Arrays.compareUnsigned(left, right);
	}

	public static byte[] Copy(byte @NotNull [] src) {
		return src.clone();
	}

	public static byte[] Copy(byte @NotNull [] src, int offset, int length) {
		return Arrays.copyOfRange(src, offset, offset + length);
	}

	@Override
	public int compareTo(@NotNull ByteBuffer o) {
		return Arrays.compareUnsigned(Bytes, ReadIndex, WriteIndex, o.Bytes, o.ReadIndex, o.WriteIndex);
	}

	/** 从输入流中读取未知长度的数据,一直取到无法获取为止 */
	public @NotNull ByteBuffer readStream(@NotNull InputStream is) throws IOException {
		for (; ; ) {
			EnsureWrite(8192);
			byte[] buf = Bytes;
			int wi = WriteIndex;
			int n = is.read(buf, wi, buf.length - wi);
			if (n <= 0) {
				WriteIndex = wi;
				break;
			}
			WriteIndex = wi + n;
		}
		return this;
	}
}
