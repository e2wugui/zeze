package Zeze.Serialize;

import java.util.*;

import Zeze.Util.BitConverter;

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
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);
		return new ByteBuffer(bytes, offset, offset + length);
	}

	public static ByteBuffer Wrap(Zeze.Net.Binary binary) {
		return Wrap(binary.InternalGetBytesUnsafe(), binary.getOffset(), binary.getCount());
	}

	public static ByteBuffer Allocate() {
		return Allocate(1024);
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
		this.Bytes = new byte[ToPower2(capacity)];
		this.ReadIndex = 0;
		this.WriteIndex = 0;
	}

	private ByteBuffer(byte[] bytes, int readIndex, int writeIndex) {
		this.Bytes = bytes;
		this.ReadIndex = readIndex;
		this.WriteIndex = writeIndex;
	}

	public static final byte[] Empty = new byte[0];

	public void FreeInternalBuffer() {
		Bytes = Empty;
		Reset();
	}

	public void Append(byte b) {
		EnsureWrite(1);
		Bytes[WriteIndex] = b;
		WriteIndex += 1;
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
		if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex) {
			throw new RuntimeException();
		}
		System.arraycopy(src, offset, Bytes, writeIndex, len);
	}

	public int BeginWriteWithSize4() {
		int saveSize = Size();
		EnsureWrite(4);
		WriteIndex += 4;
		return saveSize;
	}

	public static int ToInt(byte[] bytes, int offset) {
		return (bytes[offset] & 0xff) |
				((bytes[offset + 1] & 0xff) << 8) |
				((bytes[offset + 2] & 0xff) << 16) |
				(bytes[offset + 3] << 24);
	}

	public static long ToLong(byte[] bytes, int offset) {
		return (bytes[offset] & 0xff) |
				((bytes[offset + 1] & 0xff) << 8) |
				((bytes[offset + 2] & 0xff) << 16) |
				((long)(bytes[offset + 3] & 0xff) << 24) |
				((long)(bytes[offset + 4] & 0xff) << 32) |
				((long)(bytes[offset + 5] & 0xff) << 40) |
				((long)(bytes[offset + 6] & 0xff) << 48) |
				((long)bytes[offset + 7] << 56);
	}

	public static float ToFloat(byte[] bytes, int offset) {
		return Float.intBitsToFloat(ToInt(bytes, offset));
	}

	public static double ToDouble(byte[] bytes, int offset) {
		return Double.longBitsToDouble(ToLong(bytes, offset));
	}

	public void EndWriteWithSize4(int state) {
		var oldWriteIndex = state + ReadIndex;
		if (oldWriteIndex + 4 > Capacity())
			throw new RuntimeException();
		int v = WriteIndex - oldWriteIndex - 4;
		Bytes[oldWriteIndex] = (byte)v;
		Bytes[oldWriteIndex + 1] = (byte)(v >>> 8);
		Bytes[oldWriteIndex + 2] = (byte)(v >>> 16);
		Bytes[oldWriteIndex + 3] = (byte)(v >>> 24);
	}

	public int BeginWriteSegment() {
		int oldSize = Size();
		EnsureWrite(1);
		WriteIndex += 1;
		return oldSize;
	}

	public void EndWriteSegment(int oldSize) {
		int startPos = ReadIndex + oldSize;
		int segmentSize = WriteIndex - startPos - 1;

		// 0 111 1111
		if (segmentSize < 0x80) {
			Bytes[startPos] = (byte)segmentSize;
		}
		else if (segmentSize < 0x4000) { // 10 11 1111, -
			EnsureWrite(1);
			Bytes[WriteIndex] = Bytes[startPos + 1];
			Bytes[startPos + 1] = (byte)segmentSize;

			Bytes[startPos] = (byte)((segmentSize >>> 8) | 0x80);
			WriteIndex += 1;
		}
		else if (segmentSize < 0x200000) { // 110 1 1111, -,-
			EnsureWrite(2);
			Bytes[WriteIndex + 1] = Bytes[startPos + 2];
			Bytes[startPos + 2] = (byte)segmentSize;

			Bytes[WriteIndex] = Bytes[startPos + 1];
			Bytes[startPos + 1] = (byte)(segmentSize >>> 8);

			Bytes[startPos] = (byte)((segmentSize >>> 16) | 0xc0);
			WriteIndex += 2;
		}
		else if (segmentSize < 0x10000000) { // 1110 1111,-,-,-
			EnsureWrite(3);
			Bytes[WriteIndex + 2] = Bytes[startPos + 3];
			Bytes[startPos + 3] = (byte)segmentSize;

			Bytes[WriteIndex + 1] = Bytes[startPos + 2];
			Bytes[startPos + 2] = (byte)(segmentSize >>> 8);

			Bytes[WriteIndex] = Bytes[startPos + 1];
			Bytes[startPos + 1] = (byte)(segmentSize >>> 16);

			Bytes[startPos] = (byte)((segmentSize >>> 24) | 0xe0);
			WriteIndex += 3;
		}
		else {
			throw new RuntimeException("exceed max segment size");
		}
	}

	private int ReadSegment() {
		EnsureRead(1);
		int h = Bytes[ReadIndex] & 0xff;
		ReadIndex += 1;

		int segmentSize;
		int startPos = ReadIndex;

		if (h < 0x80) {
			segmentSize = h;
			ReadIndex += segmentSize;
		}
		else if (h < 0xc0) {
			EnsureRead(1);
			segmentSize = ((h & 0x3f) << 8) | (Bytes[ReadIndex] & 0xff);
			int endPos = ReadIndex + segmentSize;
			Bytes[ReadIndex] = Bytes[endPos];
			ReadIndex += segmentSize + 1;
		}
		else if (h < 0xe0) {
			EnsureRead(2);
			segmentSize = ((h & 0x1f) << 16) | ((Bytes[ReadIndex] & 0xff) << 8) | (Bytes[ReadIndex + 1] & 0xff);
			int endPos = ReadIndex + segmentSize;
			Bytes[ReadIndex] = Bytes[endPos];
			Bytes[ReadIndex + 1] = Bytes[endPos + 1];
			ReadIndex += segmentSize + 2;
		}
		else if (h < 0xf0) {
			EnsureRead(3);
			segmentSize = ((h & 0x0f) << 24) | ((Bytes[ReadIndex] & 0xff) << 16)
					| ((Bytes[ReadIndex + 1] & 0xff) << 8) | (Bytes[ReadIndex + 2] & 0xff);
			int endPos = ReadIndex + segmentSize;
			Bytes[ReadIndex] = Bytes[endPos];
			Bytes[ReadIndex + 1] = Bytes[endPos + 1];
			Bytes[ReadIndex + 2] = Bytes[endPos + 2];
			ReadIndex += segmentSize + 3;
		}
		else {
			throw new RuntimeException("exceed max size");
		}
		if (ReadIndex > WriteIndex) {
			throw new RuntimeException("segment data not enough");
		}
		return startPos;
	}

	public int BeginReadSegment() {
		int startPos = ReadSegment();
		int saveState = ReadIndex;
		ReadIndex = startPos;
		return saveState;
	}

	public void EndReadSegment(int saveState) {
		ReadIndex = saveState;
	}

	/**
	 这个方法把剩余可用数据移到buffer开头。
	 【注意】这个方法会修改ReadIndex，WriteIndex。
	 最好仅在全部读取写入处理完成以后调用处理一次，
	 为下一次写入读取做准备。
	*/
	public void Campact() {
		int size = this.Size();
		if (size > 0) {
			if (ReadIndex > 0) {
				System.arraycopy(Bytes, ReadIndex, Bytes, 0, size);
				ReadIndex =0;
				WriteIndex = size;
			}
		}
		else {
			Reset();
		}
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
		int size = 1024;
		while (size < needSize) {
			size <<= 1;
		}
		return size;
	}

	public void EnsureWrite(int size) {
		int newSize = WriteIndex + size;
		if (newSize > Capacity()) {
			byte[] newBytes = new byte[ToPower2(newSize)];
			WriteIndex = WriteIndex - ReadIndex;
			System.arraycopy(Bytes, ReadIndex, newBytes, 0, WriteIndex);
			ReadIndex = 0;
			Bytes = newBytes;
		}
	}

	private void EnsureRead(int size) {
		if (ReadIndex + size > WriteIndex) {
			 throw new RuntimeException("EnsureRead " + size);
		}
	}

	public void WriteBool(boolean b) {
		EnsureWrite(1);
		Bytes[WriteIndex] = (byte)(b ? 1 : 0);
		WriteIndex += 1;
	}

	public boolean ReadBool() {
		EnsureRead(1);
		var tempVar = Bytes[ReadIndex] != 0;
		ReadIndex += 1;
		return tempVar;
	}

	public void WriteByte(byte x) {
		EnsureWrite(1);
		Bytes[WriteIndex] = x;
		WriteIndex += 1;
	}

	public byte ReadByte() {
		EnsureRead(1);
		var tempVar = Bytes[ReadIndex];
		ReadIndex += 1;
		return tempVar;
	}

	public void WriteShort(short x) {
		if (x >= 0) {
			if (x < 0x80) {
				EnsureWrite(1);
				Bytes[WriteIndex] = (byte)x;
				WriteIndex += 1;
				return;
			}

			if (x < 0x4000) {
				EnsureWrite(2);
				Bytes[WriteIndex + 1] = (byte)x;
				Bytes[WriteIndex] = (byte)((x >>> 8) | 0x80);
				WriteIndex += 2;
				return;
			}
		}
		EnsureWrite(3);
		Bytes[WriteIndex] = (byte)0xff;
		Bytes[WriteIndex + 2] = (byte)x;
		Bytes[WriteIndex + 1] = (byte)(x >>> 8);
		WriteIndex += 3;
	}

	public short ReadShort() {
		EnsureRead(1);
		int h = Bytes[ReadIndex];
		h = h & 0xff;
		if (h < 0x80) {
			ReadIndex += 1;
			return (short)h;
		}
		if (h < 0xc0) {
			EnsureRead(2);
			int x = ((h & 0x3f) << 8) | (Bytes[ReadIndex + 1] & 0xff);
			ReadIndex += 2;
			return (short)x;
		}
		if ((h == 0xff)) {
			EnsureRead(3);
			int x = ((Bytes[ReadIndex + 1] & 0xff) << 8) | (Bytes[ReadIndex + 2] & 0xff);
			ReadIndex += 3;
			return (short)x;
		}
		throw new RuntimeException();
	}

	public void WriteInt4(int x) {
		EnsureWrite(4);
		Bytes[WriteIndex] = (byte)x;
		Bytes[WriteIndex + 1] = (byte)(x >>> 8);
		Bytes[WriteIndex + 2] = (byte)(x >>> 16);
		Bytes[WriteIndex + 3] = (byte)(x >>> 24);
		WriteIndex += 4;
	}

	public int ReadInt4() {
		EnsureRead(4);
		int x = ToInt(Bytes, ReadIndex);
		ReadIndex += 4;
		return x;
	}

	public void WriteLong8(long x) {
		EnsureWrite(8);
		Bytes[WriteIndex] = (byte)x;
		Bytes[WriteIndex + 1] = (byte)(x >>> 8);
		Bytes[WriteIndex + 2] = (byte)(x >>> 16);
		Bytes[WriteIndex + 3] = (byte)(x >>> 24);
		Bytes[WriteIndex + 4] = (byte)(x >>> 32);
		Bytes[WriteIndex + 5] = (byte)(x >>> 40);
		Bytes[WriteIndex + 6] = (byte)(x >>> 48);
		Bytes[WriteIndex + 7] = (byte)(x >>> 56);
		WriteIndex += 8;
	}

	public long ReadLong8() {
		EnsureRead(8);
		long x = ToLong(Bytes, ReadIndex);
		ReadIndex += 8;
		return x;
	}

	public void WriteInt(int x) {
		if (x >= 0) {
			// 0 111 1111
			if (x < 0x80) {
				EnsureWrite(1);
				Bytes[WriteIndex] = (byte)x;
				WriteIndex += 1;
				return;
			}

			if (x < 0x4000) { // 10 11 1111, -
				EnsureWrite(2);
				Bytes[WriteIndex + 1] = (byte)x;
				Bytes[WriteIndex] = (byte)((x >>> 8) | 0x80);
				WriteIndex += 2;
				return;
			}

			if (x < 0x200000) { // 110 1 1111, -,-
				EnsureWrite(3);
				Bytes[WriteIndex + 2] = (byte)x;
				Bytes[WriteIndex + 1] = (byte)(x >>> 8);
				Bytes[WriteIndex] = (byte)((x >>> 16) | 0xc0);
				WriteIndex += 3;
				return;
			}

			if (x < 0x10000000) { // 1110 1111,-,-,-
				EnsureWrite(4);
				Bytes[WriteIndex + 3] = (byte)x;
				Bytes[WriteIndex + 2] = (byte)(x >>> 8);
				Bytes[WriteIndex + 1] = (byte)(x >>> 16);
				Bytes[WriteIndex] = (byte)((x >>> 24) | 0xe0);
				WriteIndex += 4;
				return;
			}
		}

		EnsureWrite(5);
		Bytes[WriteIndex] = (byte)0xf0;
		Bytes[WriteIndex + 4] = (byte)x;
		Bytes[WriteIndex + 3] = (byte)(x >>> 8);
		Bytes[WriteIndex + 2] = (byte)(x >>> 16);
		Bytes[WriteIndex + 1] = (byte)(x >>> 24);
		WriteIndex += 5;
	}

	public int ReadInt() {
		EnsureRead(1);
		int h = Bytes[ReadIndex];
		h = h & 0xff;
		if (h < 0x80) {
			ReadIndex += 1;
			return h;
		}

		if (h < 0xc0) {
			EnsureRead(2);
			int x = ((h & 0x3f) << 8) | (Bytes[ReadIndex + 1] & 0xff);
			ReadIndex += 2;
			return x;
		}

		if (h < 0xe0) {
			EnsureRead(3);
			int x = ((h & 0x1f) << 16) | ((Bytes[ReadIndex + 1] & 0xff) << 8) | (Bytes[ReadIndex + 2] & 0xff);
			ReadIndex += 3;
			return x;
		}

		if (h < 0xf0) {
			EnsureRead(4);
			int x = ((h & 0x0f) << 24) | ((Bytes[ReadIndex + 1] & 0xff) << 16)
					| ((Bytes[ReadIndex + 2] & 0xff) << 8) | (Bytes[ReadIndex + 3] & 0xff);
			ReadIndex += 4;
			return x;
		}

		EnsureRead(5);
		int x = ((Bytes[ReadIndex + 1] & 0xff) << 24)
				| (((Bytes[ReadIndex + 2] & 0xff) << 16))
				| ((Bytes[ReadIndex + 3] & 0xff) << 8)
				| (Bytes[ReadIndex + 4] & 0xff);
		ReadIndex += 5;
		return x;
	}

	public void WriteLong(long x) {
		if (x >= 0) {
			// 0 111 1111
			if (x < 0x80L) {
				EnsureWrite(1);
				Bytes[WriteIndex] = (byte)x;
				WriteIndex += 1;
				return;
			}

			if (x < 0x4000L) { // 10 11 1111, -
				EnsureWrite(2);
				Bytes[WriteIndex + 1] = (byte)x;
				Bytes[WriteIndex] = (byte)((x >>> 8) | 0x80);
				WriteIndex += 2;
				return;
			}

			if (x < 0x200000L) { // 110 1 1111, -,-
				EnsureWrite(3);
				Bytes[WriteIndex + 2] = (byte)x;
				Bytes[WriteIndex + 1] = (byte)(x >>> 8);
				Bytes[WriteIndex] = (byte)((x >>> 16) | 0xc0);
				WriteIndex += 3;
				return;
			}

			if (x < 0x10000000L) { // 1110 1111,-,-,-
				EnsureWrite(4);
				Bytes[WriteIndex + 3] = (byte)x;
				Bytes[WriteIndex + 2] = (byte)(x >>> 8);
				Bytes[WriteIndex + 1] = (byte)(x >>> 16);
				Bytes[WriteIndex] = (byte)((x >>> 24) | 0xe0);
				WriteIndex += 4;
				return;
			}

			if (x < 0x800000000L) { // 1111 0xxx,-,-,-,-
				EnsureWrite(5);
				Bytes[WriteIndex + 4] = (byte)x;
				Bytes[WriteIndex + 3] = (byte)(x >>> 8);
				Bytes[WriteIndex + 2] = (byte)(x >>> 16);
				Bytes[WriteIndex + 1] = (byte)(x >>> 24);
				Bytes[WriteIndex] = (byte)((x >>> 32) | 0xf0);
				WriteIndex += 5;
				return;
			}

			if (x < 0x40000000000L) { // 1111 10xx,
				EnsureWrite(6);
				Bytes[WriteIndex + 5] = (byte)x;
				Bytes[WriteIndex + 4] = (byte)(x >>> 8);
				Bytes[WriteIndex + 3] = (byte)(x >>> 16);
				Bytes[WriteIndex + 2] = (byte)(x >>> 24);
				Bytes[WriteIndex + 1] = (byte)(x >>> 32);
				Bytes[WriteIndex] = (byte)((x >>> 40) | 0xf8);
				WriteIndex += 6;
				return;
			}

			if (x < 0x2000000000000L) { // 1111 110x,
				EnsureWrite(7);
				Bytes[WriteIndex + 6] = (byte)x;
				Bytes[WriteIndex + 5] = (byte)(x >>> 8);
				Bytes[WriteIndex + 4] = (byte)(x >>> 16);
				Bytes[WriteIndex + 3] = (byte)(x >>> 24);
				Bytes[WriteIndex + 2] = (byte)(x >>> 32);
				Bytes[WriteIndex + 1] = (byte)(x >>> 40);
				Bytes[WriteIndex] = (byte)((x >>> 48) | 0xfc);
				WriteIndex += 7;
				return;
			}

			if (x < 0x100000000000000L) { // 1111 1110
				EnsureWrite(8);
				Bytes[WriteIndex + 7] = (byte)x;
				Bytes[WriteIndex + 6] = (byte)(x >>> 8);
				Bytes[WriteIndex + 5] = (byte)(x >>> 16);
				Bytes[WriteIndex + 4] = (byte)(x >>> 24);
				Bytes[WriteIndex + 3] = (byte)(x >>> 32);
				Bytes[WriteIndex + 2] = (byte)(x >>> 40);
				Bytes[WriteIndex + 1] = (byte)(x >>> 48);
				Bytes[WriteIndex] = (byte)0xfe;
				WriteIndex += 8;
				return;
			}
		}

		// 1111 1111
		EnsureWrite(9);
		Bytes[WriteIndex] = (byte)0xff;
		Bytes[WriteIndex + 8] = (byte)x;
		Bytes[WriteIndex + 7] = (byte)(x >>> 8);
		Bytes[WriteIndex + 6] = (byte)(x >>> 16);
		Bytes[WriteIndex + 5] = (byte)(x >>> 24);
		Bytes[WriteIndex + 4] = (byte)(x >>> 32);
		Bytes[WriteIndex + 3] = (byte)(x >>> 40);
		Bytes[WriteIndex + 2] = (byte)(x >>> 48);
		Bytes[WriteIndex + 1] = (byte)(x >>> 56);
		WriteIndex += 9;
	}

	public long ReadLong() {
		EnsureRead(1);
		int h = Bytes[ReadIndex];
		h = h & 0xff;
		if (h < 0x80) {
			ReadIndex += 1;
			return h;
		}

		if (h < 0xc0) {
			EnsureRead(2);
			int x = ((h & 0x3f) << 8) | (Bytes[ReadIndex + 1] & 0xff);
			ReadIndex += 2;
			return x;
		}

		if (h < 0xe0) {
			EnsureRead(3);
			int x = ((h & 0x1f) << 16) | ((Bytes[ReadIndex + 1] & 0xff) << 8) | (Bytes[ReadIndex + 2] & 0xff);
			ReadIndex += 3;
			return x;
		}

		if (h < 0xf0) {
			EnsureRead(4);
			int x = ((h & 0x0f) << 24) | ((Bytes[ReadIndex + 1]  & 0xff) << 16)
					| ((Bytes[ReadIndex + 2] & 0xff) << 8) | (Bytes[ReadIndex + 3] & 0xff);
			ReadIndex += 4;
			return x;
		}

		if (h < 0xf8) {
			EnsureRead(5);
			int xl = ((Bytes[ReadIndex + 1] & 0xff) << 24) | ((Bytes[ReadIndex + 2] & 0xff) << 16)
					| ((Bytes[ReadIndex + 3] & 0xff)<< 8) | (Bytes[ReadIndex + 4] & 0xff);
			int xh = h & 0x07;
			ReadIndex += 5;
			return ((long)xh << 32) | xl;
		}

		if (h < 0xfc) {
			EnsureRead(6);
			int xl = ((Bytes[ReadIndex + 2] & 0xff) << 24) | ((Bytes[ReadIndex + 3] & 0xff) << 16)
					| ((Bytes[ReadIndex + 4] & 0xff) << 8) | (Bytes[ReadIndex + 5] & 0xff);
			int xh = ((h & 0x03) << 8) | (Bytes[ReadIndex + 1] & 0xff);
			ReadIndex += 6;
			return ((long)xh << 32) | xl;
		}

		if (h < 0xfe) {
			EnsureRead(7);
			int xl = ((Bytes[ReadIndex + 3] & 0xff) << 24) | ((Bytes[ReadIndex + 4] & 0xff) << 16)
					| ((Bytes[ReadIndex + 5] & 0xff) << 8) | (Bytes[ReadIndex + 6] & 0xff);
			int xh = ((h & 0x01) << 16) | ((Bytes[ReadIndex + 1] & 0xff) << 8) | (Bytes[ReadIndex + 2] & 0xff);
			ReadIndex += 7;
			return ((long)xh << 32) | xl;
		}

		if (h < 0xff) {
			EnsureRead(8);
			int xl = ((Bytes[ReadIndex + 4] & 0xff) << 24) | ((Bytes[ReadIndex + 5] & 0xff) << 16)
					| ((Bytes[ReadIndex + 6] & 0xff) << 8) | (Bytes[ReadIndex + 7] & 0xff);
			int xh = ((Bytes[ReadIndex + 1] & 0xff) << 16) | ((Bytes[ReadIndex + 2] & 0xff) << 8)
					| (Bytes[ReadIndex + 3] & 0xff);
			ReadIndex += 8;
			return ((long)xh << 32) | xl;
		}

		EnsureRead(9);
		int xl = ((Bytes[ReadIndex + 5] & 0xff) << 24) | ((Bytes[ReadIndex + 6] & 0xff) << 16)
				| ((Bytes[ReadIndex + 7] & 0xff) << 8) | (Bytes[ReadIndex + 8] & 0xff);
		int xh = ((Bytes[ReadIndex + 1] & 0xff) << 24) | ((Bytes[ReadIndex + 2] & 0xff) << 16)
				| ((Bytes[ReadIndex + 3] & 0xff) << 8) | (Bytes[ReadIndex + 4] & 0xff);
		ReadIndex += 9;
		return ((long)xh << 32) | xl;
	}

	public void WriteFloat(float x) {
		WriteInt4(Float.floatToRawIntBits(x));
	}

	public float ReadFloat() {
		EnsureRead(4);
		float x = ToFloat(Bytes, ReadIndex);
		ReadIndex+= 4;
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
		WriteBytes(x.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public String ReadString() {
		int n = ReadInt();
		EnsureRead(n);
		String x = new String(Bytes, ReadIndex, n, java.nio.charset.StandardCharsets.UTF_8);
		ReadIndex += n;
		return x;
	}

	public void WriteBytes(byte[] x) {
		WriteBytes(x, 0, x.length);
	}

	public void WriteBytes(byte[] x, int offset, int length) {
		WriteInt(length);
		EnsureWrite(length);
		System.arraycopy(x, offset, Bytes, WriteIndex, length);
		WriteIndex += length;
	}

	public void WriteBinary(Zeze.Net.Binary binary) {
		WriteBytes(binary.InternalGetBytesUnsafe(), binary.getOffset(), binary.getCount());
	}

	private static boolean BinaryNoCopy = false;
	public static boolean getBinaryNoCopy() {
		return BinaryNoCopy;
	}

	public static void setBinaryNoCopy(boolean value) {
		BinaryNoCopy = value;
	}
	// XXX 对于byte[]类型直接使用引用，不拷贝。全局配置，只能用于Linkd这种纯转发的程序，优化。

	public Zeze.Net.Binary ReadBinary() {
		if (getBinaryNoCopy()) {
			return new Zeze.Net.Binary(ReadByteBuffer());
		}
		return new Zeze.Net.Binary(ReadBytes());
	}

	public byte[] ReadBytes() {
		int n = ReadInt();
		EnsureRead(n);
		byte[] x = new byte[n];
		System.arraycopy(Bytes, ReadIndex, x, 0, n);
		ReadIndex += n;
		return x;
	}

	public void SkipBytes() {
		int n = ReadInt();
		EnsureRead(n);
		ReadIndex += n;
	}

	public void SkipBytes4() {
		int n = ReadInt4();
		EnsureRead(n);
		ReadIndex += n;
	}

	/**
	 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 */
	public ByteBuffer ReadByteBuffer() {
		int n = ReadInt();
		EnsureRead(n);
		int cur = ReadIndex;
		ReadIndex += n;
		return ByteBuffer.Wrap(Bytes, cur, n);
	}

	public void WriteByteBuffer(ByteBuffer o) {
		WriteBytes(o.Bytes, o.ReadIndex, o.Size());
	}

	/*
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(java.nio.charset.StandardCharsets.UTF_8);

	public static String ToHex(byte[] bytes, int offset, int len) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    int end = offset + len;
	    for (int j = offset; j < end; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, java.nio.charset.StandardCharsets.UTF_8);
	}
	*/

	@Override
	public String toString() {
		return BitConverter.toString(Bytes, ReadIndex, Size());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ByteBuffer other) {
			return equals(other);
		}
		return false;
	}

	public boolean equals(ByteBuffer other) {
		if (other == null) {
			return false;
		}

		if (this.Size() != other.Size()) {
			return false;
		}

		for (int i = 0, n = this.Size(); i < n; i++) {
			if (Bytes[ReadIndex + i] != other.Bytes[other.ReadIndex + i]) {
				return false;
			}
		}

		return true;
	}

	public static int calc_hashnr(long value) {
		return calc_hashnr(String.valueOf(value));
	}

	public static int calc_hashnr(String str) {
		return calc_hashnr(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public static int calc_hashnr(byte[] keys) {
		return calc_hashnr(keys, 0, keys.length);
	}

	public static int calc_hashnr(byte[] keys, int offset, int len) {
		int end = offset + len;
		int hash = 0;
		for (int i = offset; i < end; ++i) {
			hash *= 16777619;
			hash ^= keys[i];
		}
		return hash;
	}

	@Override
	public int hashCode() {
		return calc_hashnr(Bytes, ReadIndex, Size());
	}

	// 只能增加新的类型定义，增加时记得同步 SkipUnknownField
	public static final int INT = 0, LONG = 1, STRING = 2, BOOL = 3, BYTE = 4, SHORT = 5, FLOAT = 6, DOUBLE = 7, BYTES = 8, LIST = 9, SET = 10, MAP = 11, BEAN = 12, DYNAMIC = 13, TAG_MAX = 31;

	public static final int TAG_SHIFT = 5;
	public static final int TAG_MASK = (1 << TAG_SHIFT) - 1;
	public static final int ID_MASK = (1 << (31 - TAG_SHIFT)) - 1;

	/*
	// 在生成代码的时候使用这个方法检查。生成后的代码不使用这个方法。
	// 可以定义的最大 Variable.Id 为 Zeze.Transaction.Bean.MaxVariableId
	public static int MakeTagId(int tag, int id)
	{
	    if (tag < 0 || tag > TAG_MAX)
	        throw new OverflowException("tag < 0 || tag > TAG_MAX");
	    if (id < 0 || id > ID_MASK)
	        throw new OverflowException("id < 0 || id > ID_MASK");

	    return (id << TAG_SHIFT) | tag;
	}

	public static int GetTag(int tagid)
	{
	    return tagid & TAG_MASK;
	}

	public static int GetId(int tagid)
	{
	}
	*/

	public static void VerifyArrayIndex(byte[] bytes, int offset, int length) {
		if (offset < 0 || offset > bytes.length) {
			throw new RuntimeException(String.format("%1$s,%2$s,%3$s", bytes.length, offset, length));
		}
		int endindex = offset + length;
		if (endindex < 0 || endindex > bytes.length) {
			throw new RuntimeException(String.format("%1$s,%2$s,%3$s", bytes.length, offset, length));
		}
		if (offset > endindex) {
			throw new RuntimeException(String.format("%1$s,%2$s,%3$s", bytes.length, offset, length));
		}
	}

	public static ByteBuffer Encode(Serializable sa) {
		ByteBuffer bb = ByteBuffer.Allocate();
		sa.Encode(bb);
		return bb;
	}

	public static void SkipUnknownField(int tagid, ByteBuffer bb) {
		int tagType = tagid & TAG_MASK;
		switch (tagType) {
			case BOOL -> bb.ReadBool();
			case BYTE -> bb.ReadByte();
			case SHORT -> bb.ReadShort();
			case INT -> bb.ReadInt();
			case LONG -> bb.ReadLong();
			case FLOAT -> bb.ReadFloat();
			case DOUBLE -> bb.ReadDouble();
			case STRING, BYTES, LIST, SET, MAP, BEAN -> bb.SkipBytes();
			case DYNAMIC -> {
				bb.ReadLong8();
				bb.SkipBytes();
			}
			default -> throw new RuntimeException("SkipUnknownField");
		}
	}

	public static <T> void BuildString(StringBuilder sb, java.lang.Iterable<T> c) {
		sb.append("[");
		for (var e : c) {
			sb.append(e);
			sb.append(",");
		}
		sb.append("]");
	}


	public static <TK, TV> void BuildString(StringBuilder sb, Map<TK, TV> dic) {
		sb.append("{");
		for (var e : dic.entrySet()) {
			sb.append(e.getKey()).append(':');
			sb.append(e.getValue()).append(',');
		}
		sb.append('}');
	}

	public static boolean Equals(byte[] left, byte[] right) {
		if (left == null || right == null) {
			return left == right;
		}
		if (left.length != right.length) {
			return false;
		}
		for (int i = 0; i < left.length; i++) {
			if (left[i] != right[i]) {
				return false;
			}
		}
		return true;
	}

	public static int Compare(byte[] left, byte[] right) {
		if (left == null || right == null) {
			if (left == right) { // both null
				return 0;
			}
			if (left == null) { // null is small
				return -1;
			}
			return 1;
		}

		if (left.length != right.length) {
			return Integer.compare(left.length, right.length); // shorter is small
		}

		for (int i = 0; i < left.length; i++) {
			int c = Byte.compare(left[i], right[i]);
			if (0 != c) {
				return c;
			}
		}
		return 0;
	}

	public static byte[] Copy(byte[] src) {
		byte[] result = new byte[src.length];
		System.arraycopy(src, 0, result, 0, src.length);
		return result;
	}

	public static byte[] Copy(byte[] src, int offset, int length) {
		byte[] result = new byte[length];
		System.arraycopy(src, offset, result, 0, length);
		return result;
	}
}
