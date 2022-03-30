package Zeze.Serialize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.BitConverter;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashMap;

public final class ByteBuffer {
	public byte[] Bytes;
	public int ReadIndex;
	public int WriteIndex;

	public int Capacity() {
		return Bytes.length;
	}

	public int Size() {
		return WriteIndex - ReadIndex;
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

	private ByteBuffer(int capacity) {
		Bytes = new byte[capacity]; // ToPower2(capacity)
		ReadIndex = 0;
		WriteIndex = 0;
	}

	private ByteBuffer(byte[] bytes, int readIndex, int writeIndex) {
		Bytes = bytes;
		ReadIndex = readIndex;
		WriteIndex = writeIndex;
	}

	public static final byte[] Empty = new byte[0];

	public void FreeInternalBuffer() {
		Bytes = Empty;
		Reset();
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
		int v = WriteIndex - oldWriteIndex - 4;
		byte[] bytes = Bytes;
		bytes[oldWriteIndex] = (byte)v;
		bytes[oldWriteIndex + 1] = (byte)(v >> 8);
		bytes[oldWriteIndex + 2] = (byte)(v >> 16);
		bytes[oldWriteIndex + 3] = (byte)(v >> 24);
	}

	public static int ToInt(byte[] bytes, int offset) {
		return (bytes[offset] & 0xff) +
				((bytes[offset + 1] & 0xff) << 8) +
				((bytes[offset + 2] & 0xff) << 16) +
				(bytes[offset + 3] << 24);
	}

	public static long ToLong(byte[] bytes, int offset) {
		return (bytes[offset] & 0xff) +
				((bytes[offset + 1] & 0xff) << 8) +
				((bytes[offset + 2] & 0xff) << 16) +
				((long)(bytes[offset + 3] & 0xff) << 24) +
				((long)(bytes[offset + 4] & 0xff) << 32) +
				((long)(bytes[offset + 5] & 0xff) << 40) +
				((long)(bytes[offset + 6] & 0xff) << 48) +
				((long)bytes[offset + 7] << 56);
	}

	public static float ToFloat(byte[] bytes, int offset) {
		return Float.intBitsToFloat(ToInt(bytes, offset));
	}

	public static double ToDouble(byte[] bytes, int offset) {
		return Double.longBitsToDouble(ToLong(bytes, offset));
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
		byte[] copy = new byte[size];
		System.arraycopy(Bytes, ReadIndex, copy, 0, size);
		return copy;
	}

	public void Reset() {
		ReadIndex = 0;
		WriteIndex = 0;
	}

	private static int ToPower2(int needSize) {
		int size = 16;
		while (size < needSize)
			size <<= 1;
		return size;
	}

	public void EnsureWrite(int size) {
		int newSize = WriteIndex + size;
		if (newSize > Capacity()) {
			byte[] newBytes = new byte[ToPower2(newSize)];
			System.arraycopy(Bytes, ReadIndex, newBytes, 0, WriteIndex -= ReadIndex);
			ReadIndex = 0;
			Bytes = newBytes;
		}
	}

	private void EnsureRead(int size) {
		if (ReadIndex + size > WriteIndex) {
			throw new IllegalStateException("EnsureRead " + size);
		}
	}

	public void WriteBool(boolean b) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = (byte)(b ? 1 : 0);
	}

	public boolean ReadBool() {
		return ReadLong() != 0;
	}

	public void WriteByte(byte x) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = x;
	}

	public void WriteByte(int x) {
		EnsureWrite(1);
		Bytes[WriteIndex++] = (byte)x;
	}

	public byte ReadByte() {
		EnsureRead(1);
		return Bytes[ReadIndex++];
	}

	public void WriteInt4(int x) {
		EnsureWrite(4);
		byte[] bytes = Bytes;
		int writeIndex = WriteIndex;
		bytes[writeIndex] = (byte)x;
		bytes[writeIndex + 1] = (byte)(x >> 8);
		bytes[writeIndex + 2] = (byte)(x >> 16);
		bytes[writeIndex + 3] = (byte)(x >> 24);
		WriteIndex = writeIndex + 4;
	}

	public int ReadInt4() {
		EnsureRead(4);
		int x = ToInt(Bytes, ReadIndex);
		ReadIndex += 4;
		return x;
	}

	public void WriteLong8(long x) {
		EnsureWrite(8);
		byte[] bytes = Bytes;
		int writeIndex = WriteIndex;
		bytes[writeIndex] = (byte)x;
		bytes[writeIndex + 1] = (byte)(x >> 8);
		bytes[writeIndex + 2] = (byte)(x >> 16);
		bytes[writeIndex + 3] = (byte)(x >> 24);
		bytes[writeIndex + 4] = (byte)(x >> 32);
		bytes[writeIndex + 5] = (byte)(x >> 40);
		bytes[writeIndex + 6] = (byte)(x >> 48);
		bytes[writeIndex + 7] = (byte)(x >> 56);
		WriteIndex = writeIndex + 8;
	}

	public long ReadLong8() {
		EnsureRead(8);
		long x = ToLong(Bytes, ReadIndex);
		ReadIndex += 8;
		return x;
	}

	public static int writeUIntSize(int x) {
		long u = x & 0xffff_ffffL;
		//@formatter:off
		if (u <        0x80) return 1;
		if (u <      0x4000) return 2;
		if (u <   0x20_0000) return 3;
		if (u < 0x1000_0000) return 4;
		return 5;
		//@formatter:on
	}

	public void WriteUInt(int x) {
		long u = x & 0xffff_ffffL;
		if (u < 0x80) {
			EnsureWrite(1); // 0xxx xxxx
			Bytes[WriteIndex++] = (byte)u;
		} else if (u < 0x4000) {
			EnsureWrite(2); // 10xx xxxx +1B
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)((u >> 8) + 0x80);
			bytes[writeIndex + 1] = (byte)u;
			WriteIndex = writeIndex + 2;
		} else if (u < 0x20_0000) {
			EnsureWrite(3); // 110x xxxx +2B
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)((u >> 16) + 0xc0);
			bytes[writeIndex + 1] = (byte)(u >> 8);
			bytes[writeIndex + 2] = (byte)u;
			WriteIndex = writeIndex + 3;
		} else if (u < 0x1000_0000) {
			EnsureWrite(4); // 1110 xxxx +3B
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)((u >> 24) + 0xe0);
			bytes[writeIndex + 1] = (byte)(u >> 16);
			bytes[writeIndex + 2] = (byte)(u >> 8);
			bytes[writeIndex + 3] = (byte)u;
			WriteIndex = writeIndex + 4;
		} else {
			EnsureWrite(5); // 1111 0000 +4B
			byte[] bytes = Bytes;
			int writeIndex = WriteIndex;
			bytes[writeIndex] = (byte)0xf0;
			bytes[writeIndex + 1] = (byte)(u >> 24);
			bytes[writeIndex + 2] = (byte)(u >> 16);
			bytes[writeIndex + 3] = (byte)(u >> 8);
			bytes[writeIndex + 4] = (byte)u;
			WriteIndex = writeIndex + 5;
		}
	}

	public int ReadUInt() {
		EnsureRead(1);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		int x = bytes[readIndex] & 0xff;
		if (x < 0x80) {
			ReadIndex = readIndex + 1;
		} else if (x < 0xc0) {
			EnsureRead(2);
			x = ((x & 0x3f) << 8)
					+ (bytes[readIndex + 1] & 0xff);
			ReadIndex = readIndex + 2;
		} else if (x < 0xe0) {
			EnsureRead(3);
			x = ((x & 0x1f) << 16)
					+ ((bytes[readIndex + 1] & 0xff) << 8)
					+ (bytes[readIndex + 2] & 0xff);
			ReadIndex = readIndex + 3;
		} else if (x < 0xf0) {
			EnsureRead(4);
			x = ((x & 0xf) << 24)
					+ ((bytes[readIndex + 1] & 0xff) << 16)
					+ ((bytes[readIndex + 2] & 0xff) << 8)
					+ (bytes[readIndex + 3] & 0xff);
			ReadIndex = readIndex + 4;
		} else {
			EnsureRead(5);
			x = (bytes[readIndex + 1] << 24)
					+ ((bytes[readIndex + 2] & 0xff) << 16)
					+ ((bytes[readIndex + 3] & 0xff) << 8)
					+ (bytes[readIndex + 4] & 0xff);
			ReadIndex = readIndex + 5;
		}
		return x;
	}

	public static int writeLongSize(long x) {
		//@formatter:off
		if (x >= 0) {
			if (x <                0x40 ) return 1;
			if (x <              0x2000 ) return 2;
			if (x <           0x10_0000 ) return 3;
			if (x <          0x800_0000 ) return 4;
			if (x <       0x4_0000_0000L) return 5;
			if (x <     0x200_0000_0000L) return 6;
			if (x <  0x1_0000_0000_0000L) return 7;
			if (x < 0x80_0000_0000_0000L) return 8;
										  return 9;
		}
		if (x >= -               0x40 ) return 1;
		if (x >= -             0x2000 ) return 2;
		if (x >= -          0x10_0000 ) return 3;
		if (x >= -         0x800_0000 ) return 4;
		if (x >= -      0x4_0000_0000L) return 5;
		if (x >= -    0x200_0000_0000L) return 6;
		if (x >= - 0x1_0000_0000_0000L) return 7;
		if (x >= -0x80_0000_0000_0000L) return 8;
		return 9;
		//@formatter:on
	}

	public void WriteLong(long x) {
		if (x >= 0) {
			if (x < 0x40) {
				EnsureWrite(1); // 00xx xxxx
				Bytes[WriteIndex++] = (byte)x;
			} else if (x < 0x2000) {
				EnsureWrite(2); // 010x xxxx +1B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 8) + 0x40);
				bytes[writeIndex + 1] = (byte)x;
				WriteIndex = writeIndex + 2;
			} else if (x < 0x10_0000) {
				EnsureWrite(3); // 0110 xxxx +2B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 16) + 0x60);
				bytes[writeIndex + 1] = (byte)(x >> 8);
				bytes[writeIndex + 2] = (byte)x;
				WriteIndex = writeIndex + 3;
			} else if (x < 0x800_0000) {
				EnsureWrite(4); // 0111 0xxx +3B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 24) + 0x70);
				bytes[writeIndex + 1] = (byte)(x >> 16);
				bytes[writeIndex + 2] = (byte)(x >> 8);
				bytes[writeIndex + 3] = (byte)x;
				WriteIndex = writeIndex + 4;
			} else if (x < 0x4_0000_0000L) {
				EnsureWrite(5); // 0111 10xx +4B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 32) + 0x78);
				bytes[writeIndex + 1] = (byte)(x >> 24);
				bytes[writeIndex + 2] = (byte)(x >> 16);
				bytes[writeIndex + 3] = (byte)(x >> 8);
				bytes[writeIndex + 4] = (byte)x;
				WriteIndex = writeIndex + 5;
			} else if (x < 0x200_0000_0000L) {
				EnsureWrite(6); // 0111 110x +5B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 40) + 0x7c);
				bytes[writeIndex + 1] = (byte)(x >> 32);
				bytes[writeIndex + 2] = (byte)(x >> 24);
				bytes[writeIndex + 3] = (byte)(x >> 16);
				bytes[writeIndex + 4] = (byte)(x >> 8);
				bytes[writeIndex + 5] = (byte)x;
				WriteIndex = writeIndex + 6;
			} else if (x < 0x1_0000_0000_0000L) {
				EnsureWrite(7); // 0111 1110 +6B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x7e;
				bytes[writeIndex + 1] = (byte)(x >> 40);
				bytes[writeIndex + 2] = (byte)(x >> 32);
				bytes[writeIndex + 3] = (byte)(x >> 24);
				bytes[writeIndex + 4] = (byte)(x >> 16);
				bytes[writeIndex + 5] = (byte)(x >> 8);
				bytes[writeIndex + 6] = (byte)x;
				WriteIndex = writeIndex + 7;
			} else if (x < 0x80_0000_0000_0000L) {
				EnsureWrite(8); // 0111 1111 0 +7B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x7f;
				bytes[writeIndex + 1] = (byte)(x >> 48);
				bytes[writeIndex + 2] = (byte)(x >> 40);
				bytes[writeIndex + 3] = (byte)(x >> 32);
				bytes[writeIndex + 4] = (byte)(x >> 24);
				bytes[writeIndex + 5] = (byte)(x >> 16);
				bytes[writeIndex + 6] = (byte)(x >> 8);
				bytes[writeIndex + 7] = (byte)x;
				WriteIndex = writeIndex + 8;
			} else {
				EnsureWrite(9); // 0111 1111 1 +8B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x7f;
				bytes[writeIndex + 1] = (byte)((x >> 56) + 0x80);
				bytes[writeIndex + 2] = (byte)(x >> 48);
				bytes[writeIndex + 3] = (byte)(x >> 40);
				bytes[writeIndex + 4] = (byte)(x >> 32);
				bytes[writeIndex + 5] = (byte)(x >> 24);
				bytes[writeIndex + 6] = (byte)(x >> 16);
				bytes[writeIndex + 7] = (byte)(x >> 8);
				bytes[writeIndex + 8] = (byte)x;
				WriteIndex = writeIndex + 9;
			}
		} else {
			if (x >= -0x40) {
				EnsureWrite(1); // 11xx xxxx
				Bytes[WriteIndex++] = (byte)x;
			} else if (x >= -0x2000) {
				EnsureWrite(2); // 101x xxxx +1B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 8) - 0x40);
				bytes[writeIndex + 1] = (byte)x;
				WriteIndex = writeIndex + 2;
			} else if (x >= -0x10_0000) {
				EnsureWrite(3); // 1001 xxxx +2B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 16) - 0x60);
				bytes[writeIndex + 1] = (byte)(x >> 8);
				bytes[writeIndex + 2] = (byte)x;
				WriteIndex = writeIndex + 3;
			} else if (x >= -0x800_0000) {
				EnsureWrite(4); // 1000 1xxx +3B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 24) - 0x70);
				bytes[writeIndex + 1] = (byte)(x >> 16);
				bytes[writeIndex + 2] = (byte)(x >> 8);
				bytes[writeIndex + 3] = (byte)x;
				WriteIndex = writeIndex + 4;
			} else if (x >= -0x4_0000_0000L) {
				EnsureWrite(5); // 1000 01xx +4B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 32) - 0x78);
				bytes[writeIndex + 1] = (byte)(x >> 24);
				bytes[writeIndex + 2] = (byte)(x >> 16);
				bytes[writeIndex + 3] = (byte)(x >> 8);
				bytes[writeIndex + 4] = (byte)x;
				WriteIndex = writeIndex + 5;
			} else if (x >= -0x200_0000_0000L) {
				EnsureWrite(6); // 1000 001x +5B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)((x >> 40) - 0x7c);
				bytes[writeIndex + 1] = (byte)(x >> 32);
				bytes[writeIndex + 2] = (byte)(x >> 24);
				bytes[writeIndex + 3] = (byte)(x >> 16);
				bytes[writeIndex + 4] = (byte)(x >> 8);
				bytes[writeIndex + 5] = (byte)x;
				WriteIndex = writeIndex + 6;
			} else if (x >= -0x1_0000_0000_0000L) {
				EnsureWrite(7); // 1000 0001 +6B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x81;
				bytes[writeIndex + 1] = (byte)(x >> 40);
				bytes[writeIndex + 2] = (byte)(x >> 32);
				bytes[writeIndex + 3] = (byte)(x >> 24);
				bytes[writeIndex + 4] = (byte)(x >> 16);
				bytes[writeIndex + 5] = (byte)(x >> 8);
				bytes[writeIndex + 6] = (byte)x;
				WriteIndex = writeIndex + 7;
			} else if (x >= -0x80_0000_0000_0000L) {
				EnsureWrite(8); // 1000 0000 1 +7B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x80;
				bytes[writeIndex + 1] = (byte)(x >> 48);
				bytes[writeIndex + 2] = (byte)(x >> 40);
				bytes[writeIndex + 3] = (byte)(x >> 32);
				bytes[writeIndex + 4] = (byte)(x >> 24);
				bytes[writeIndex + 5] = (byte)(x >> 16);
				bytes[writeIndex + 6] = (byte)(x >> 8);
				bytes[writeIndex + 7] = (byte)x;
				WriteIndex = writeIndex + 8;
			} else {
				EnsureWrite(9); // 1000 0000 0 +8B
				byte[] bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (byte)0x80;
				bytes[writeIndex + 1] = (byte)((x >> 56) - 0x80);
				bytes[writeIndex + 2] = (byte)(x >> 48);
				bytes[writeIndex + 3] = (byte)(x >> 40);
				bytes[writeIndex + 4] = (byte)(x >> 32);
				bytes[writeIndex + 5] = (byte)(x >> 24);
				bytes[writeIndex + 6] = (byte)(x >> 16);
				bytes[writeIndex + 7] = (byte)(x >> 8);
				bytes[writeIndex + 8] = (byte)x;
				WriteIndex = writeIndex + 9;
			}
		}
	}

	public long ReadLong1() {
		EnsureRead(1);
		return Bytes[ReadIndex++] & 0xff;
	}

	public long ReadLong2BE() {
		EnsureRead(2);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 2;
		return ((bytes[readIndex] & 0xff) << 8) +
				(bytes[readIndex + 1] & 0xff);
	}

	public long ReadLong3BE() {
		EnsureRead(3);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 3;
		return ((bytes[readIndex] & 0xff) << 16) +
				((bytes[readIndex + 1] & 0xff) << 8) +
				(bytes[readIndex + 2] & 0xff);
	}

	public long ReadLong4BE() {
		EnsureRead(4);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 4;
		return ((long)(bytes[readIndex] & 0xff) << 24) +
				((bytes[readIndex + 1] & 0xff) << 16) +
				((bytes[readIndex + 2] & 0xff) << 8) +
				(bytes[readIndex + 3] & 0xff);
	}

	public long ReadLong5BE() {
		EnsureRead(5);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 5;
		return ((long)(bytes[readIndex] & 0xff) << 32) +
				((long)(bytes[readIndex + 1] & 0xff) << 24) +
				((bytes[readIndex + 2] & 0xff) << 16) +
				((bytes[readIndex + 3] & 0xff) << 8) +
				(bytes[readIndex + 4] & 0xff);
	}

	public long ReadLong6BE() {
		EnsureRead(6);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 6;
		return ((long)(bytes[readIndex] & 0xff) << 40) +
				((long)(bytes[readIndex + 1] & 0xff) << 32) +
				((long)(bytes[readIndex + 2] & 0xff) << 24) +
				((bytes[readIndex + 3] & 0xff) << 16) +
				((bytes[readIndex + 4] & 0xff) << 8) +
				(bytes[readIndex + 5] & 0xff);
	}

	public long ReadLong7BE() {
		EnsureRead(7);
		byte[] bytes = Bytes;
		int readIndex = ReadIndex;
		ReadIndex = readIndex + 7;
		return ((long)(bytes[readIndex] & 0xff) << 48) +
				((long)(bytes[readIndex + 1] & 0xff) << 40) +
				((long)(bytes[readIndex + 2] & 0xff) << 32) +
				((long)(bytes[readIndex + 3] & 0xff) << 24) +
				((bytes[readIndex + 4] & 0xff) << 16) +
				((bytes[readIndex + 5] & 0xff) << 8) +
				(bytes[readIndex + 6] & 0xff);
	}

	public long ReadLong() {
		EnsureRead(1);
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

	public void WriteInt(int x) {
		WriteLong(x);
	}

	public int ReadInt() {
		return (int)ReadLong();
	}

	public void WriteFloat(float x) {
		WriteInt4(Float.floatToRawIntBits(x));
	}

	public float ReadFloat() {
		EnsureRead(4);
		float x = ToFloat(Bytes, ReadIndex);
		ReadIndex += 4;
		return x;
	}

	public void WriteDouble(double x) {
		WriteLong8(Double.doubleToRawLongBits(x));
	}

	public double ReadDouble() {
		EnsureRead(8);
		double x = ToDouble(Bytes, ReadIndex);
		ReadIndex += 8;
		return x;
	}

	public void WriteString(String x) {
		WriteBytes(x.getBytes(StandardCharsets.UTF_8));
	}

	public String ReadString() {
		int n = ReadUInt();
		EnsureRead(n);
		String x = new String(Bytes, ReadIndex, n, StandardCharsets.UTF_8);
		ReadIndex += n;
		return x;
	}

	public void WriteBytes(byte[] x) {
		WriteBytes(x, 0, x.length);
	}

	public void WriteBytes(byte[] x, int offset, int length) {
		WriteUInt(length);
		EnsureWrite(length);
		System.arraycopy(x, offset, Bytes, WriteIndex, length);
		WriteIndex += length;
	}

	public void WriteBinary(Binary binary) {
		binary.Encode(this);
	}

	public Binary ReadBinary() {
		return new Binary(ReadBytes());
	}

	public byte[] ReadBytes() {
		int n = ReadUInt();
		EnsureRead(n);
		byte[] x = new byte[n];
		System.arraycopy(Bytes, ReadIndex, x, 0, n);
		ReadIndex += n;
		return x;
	}

	public void SkipBytes() {
		int n = ReadUInt();
		EnsureRead(n);
		ReadIndex += n;
	}

	public void SkipBytes4() {
		int n = ReadInt4();
		EnsureRead(n);
		ReadIndex += n;
	}

	/**
	 * 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 */
	public ByteBuffer ReadByteBuffer() {
		int n = ReadUInt();
		EnsureRead(n);
		int cur = ReadIndex;
		ReadIndex += n;
		return Wrap(Bytes, cur, n);
	}

	public void WriteByteBuffer(ByteBuffer o) {
		WriteBytes(o.Bytes, o.ReadIndex, o.Size());
	}

	@Override
	public String toString() {
		return BitConverter.toString(Bytes, ReadIndex, Size());
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ByteBuffer && equals((ByteBuffer)obj);
	}

	public boolean equals(ByteBuffer other) {
		return other != null &&
				Arrays.equals(Bytes, ReadIndex, WriteIndex, other.Bytes, other.ReadIndex, other.WriteIndex);
	}

	public static int calc_hashnr(long value) {
		return calc_hashnr(String.valueOf(value));
	}

	public static int calc_hashnr(String str) {
		return calc_hashnr(str.getBytes(StandardCharsets.UTF_8));
	}

	public static int calc_hashnr(byte[] keys) {
		return calc_hashnr(keys, 0, keys.length);
	}

	public static int calc_hashnr(byte[] keys, int offset, int len) {
		int hash = 0;
		for (int end = offset + len; offset < end; offset++)
			hash = hash * 16777619 ^ keys[offset];
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
			DYNAMIC = 7; // dynamic

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
		int endIndex;
		if (offset < 0 || offset > bytes.length
				|| (endIndex = offset + length) < 0 || endIndex > bytes.length || offset > endIndex)
			throw new IllegalArgumentException(String.format("%d,%d,%d", bytes.length, offset, length));
	}

	public static ByteBuffer Encode(Serializable sa) {
		int preAllocSize = sa.getPreAllocSize();
		ByteBuffer bb = Allocate(Math.min(preAllocSize, 65536));
		sa.Encode(bb);
		if (preAllocSize < bb.WriteIndex)
			sa.setPreAllocSize(bb.WriteIndex);
		return bb;
	}

	public void Encode(Collection<? extends Serializable> c) {
		WriteUInt(c.size());
		for (var s : c)
			s.Encode(this);
	}

	public <T extends Serializable> void Decode(Collection<T> c,
												Supplier<T> factory) {
		for (int n = ReadUInt(); n > 0; n--) {
			T v = factory.get();
			v.Decode(this);
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
		SkipUnknownField(type);
		return false;
	}

	public byte ReadByte(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return (byte)ReadLong();
		if (type == FLOAT)
			return (byte)ReadFloat();
		if (type == DOUBLE)
			return (byte)ReadDouble();
		SkipUnknownField(type);
		return 0;
	}

	public short ReadShort(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return (short)ReadLong();
		if (type == FLOAT)
			return (short)ReadFloat();
		if (type == DOUBLE)
			return (short)ReadDouble();
		SkipUnknownField(type);
		return 0;
	}

	public int ReadInt(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return (int)ReadLong();
		if (type == FLOAT)
			return (int)ReadFloat();
		if (type == DOUBLE)
			return (int)ReadDouble();
		SkipUnknownField(type);
		return 0;
	}

	public long ReadLong(int type) {
		type &= TAG_MASK;
		if (type == INTEGER)
			return ReadLong();
		if (type == FLOAT)
			return (long)ReadFloat();
		if (type == DOUBLE)
			return (long)ReadDouble();
		SkipUnknownField(type);
		return 0;
	}

	public float ReadFloat(int type) {
		type &= TAG_MASK;
		if (type == FLOAT)
			return ReadFloat();
		if (type == DOUBLE)
			return (float)ReadDouble();
		if (type == INTEGER)
			return ReadLong();
		SkipUnknownField(type);
		return 0;
	}

	public double ReadDouble(int type) {
		type &= TAG_MASK;
		if (type == DOUBLE)
			return ReadDouble();
		if (type == FLOAT)
			return ReadFloat();
		if (type == INTEGER)
			return ReadLong();
		SkipUnknownField(type);
		return 0;
	}

	public Binary ReadBinary(int type) {
		type &= TAG_MASK;
		if (type == BYTES)
			return ReadBinary();
		SkipUnknownField(type);
		return Binary.Empty;
	}

	public String ReadString(int type) {
		type &= TAG_MASK;
		if (type == BYTES)
			return ReadString();
		SkipUnknownField(type);
		return "";
	}

	public <T extends Serializable> T ReadBean(T bean, int type) {
		type &= TAG_MASK;
		if (type == BEAN)
			bean.Decode(this);
		else if (type == DYNAMIC) {
			ReadLong();
			bean.Decode(this);
		} else
			SkipUnknownField(type);
		return bean;
	}

	public DynamicBean ReadDynamic(DynamicBean dynBean, int type) {
		type &= TAG_MASK;
		if (type == DYNAMIC) {
			dynBean.Decode(this);
			return dynBean;
		}
		if (type == BEAN) {
			Bean bean = dynBean.getCreateBeanFromSpecialTypeId().apply(0);
			if (bean != null) {
				bean.Decode(this);
				return dynBean;
			}
		}
		SkipUnknownField(type);
		return dynBean;
	}

	public void SkipUnknownField(int type, int count) {
		while (--count >= 0)
			SkipUnknownField(type);
	}

	public void SkipUnknownField(int type1, int type2, int count) {
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
			EnsureRead(4);
			ReadIndex += 4;
			return;
		case DOUBLE:
			EnsureRead(8);
			ReadIndex += 8;
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
		case BEAN:
			while ((t = ReadByte()) != 0) {
				if ((t & ID_MASK) == 0xf0)
					ReadUInt();
				SkipUnknownField(t);
			}
			return;
		default:
			throw new IllegalStateException("SkipUnknownField");
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
		return Arrays.compare(left, right);
	}

	public static byte[] Copy(byte[] src) {
		return src.clone();
	}

	public static byte[] Copy(byte[] src, int offset, int length) {
		return Arrays.copyOfRange(src, offset, offset + length);
	}
}
