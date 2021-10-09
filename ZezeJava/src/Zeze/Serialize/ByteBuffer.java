package Zeze.Serialize;

import Zeze.*;
import java.util.*;

public final class ByteBuffer {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] Bytes;
	private byte[] Bytes;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] getBytes()
	public byte[] getBytes() {
		return Bytes;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private void setBytes(byte[] value)
	private void setBytes(byte[] value) {
		Bytes = value;
	}
	private int ReadIndex;
	public int getReadIndex() {
		return ReadIndex;
	}
	public void setReadIndex(int value) {
		ReadIndex = value;
	}
	private int WriteIndex;
	public int getWriteIndex() {
		return WriteIndex;
	}
	public void setWriteIndex(int value) {
		WriteIndex = value;
	}
	public int getCapacity() {
		return getBytes().length;
	}
	public int getSize() {
		return getWriteIndex() - getReadIndex();
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static ByteBuffer Wrap(byte[] bytes)
	public static ByteBuffer Wrap(byte[] bytes) {
		return new ByteBuffer(bytes, 0, bytes.length);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static ByteBuffer Wrap(byte[] bytes, int offset, int length)
	public static ByteBuffer Wrap(byte[] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);
		return new ByteBuffer(bytes, offset, offset + length);
	}

	public static ByteBuffer Wrap(Zeze.Net.Binary binary) {
		return Wrap(binary.getBytes(), binary.getOffset(), binary.getCount());
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
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: this.Bytes = new byte[ToPower2(capacity)];
		this.setBytes(new byte[ToPower2(capacity)]);
		this.setReadIndex(0);
		this.setWriteIndex(0);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private ByteBuffer(byte[] bytes, int readIndex, int writeIndex)
	private ByteBuffer(byte[] bytes, int readIndex, int writeIndex) {
		this.setBytes(bytes);
		this.setReadIndex(readIndex);
		this.setWriteIndex(writeIndex);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void FreeInternalBuffer()
	public void FreeInternalBuffer() {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes = Array.Empty<byte>();
		setBytes(Array.<Byte>Empty());
		Reset();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Append(byte b)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Append(byte b)
	public void Append(byte b) {
		EnsureWrite(1);
		getBytes()[getWriteIndex()] = b;
		setWriteIndex(getWriteIndex() + 1);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Append(byte[] bs)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Append(byte[] bs)
	public void Append(byte[] bs) {
		Append(bs, 0, bs.length);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Append(byte[] bs, int offset, int len)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Append(byte[] bs, int offset, int len)
	public void Append(byte[] bs, int offset, int len) {
		EnsureWrite(len);
		Buffer.BlockCopy(bs, offset, getBytes(), getWriteIndex(), len);
		setWriteIndex(getWriteIndex() + len);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Replace(int writeIndex, byte[] src)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Replace(int writeIndex, byte[] src)
	public void Replace(int writeIndex, byte[] src) {
		Replace(writeIndex, src, 0, src.length);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Replace(int writeIndex, byte[] src, int offset, int len)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Replace(int writeIndex, byte[] src, int offset, int len)
	public void Replace(int writeIndex, byte[] src, int offset, int len) {
		if (writeIndex < this.getReadIndex() || writeIndex + len > this.getWriteIndex()) {
			throw new RuntimeException();
		}
		Buffer.BlockCopy(src, offset, this.getBytes(), writeIndex, len);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void BeginWriteWithSize4(out int state)
	public void BeginWriteWithSize4(tangible.OutObject<Integer> state) {
		state.outArgValue = getSize();
		EnsureWrite(4);
		setWriteIndex(getWriteIndex() + 4);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void EndWriteWithSize4(int state)
	public void EndWriteWithSize4(int state) {
		var oldWriteIndex = state + getReadIndex();
		Replace(oldWriteIndex, BitConverter.GetBytes(getWriteIndex() - oldWriteIndex - 4));
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void BeginWriteSegment(out int oldSize)
	public void BeginWriteSegment(tangible.OutObject<Integer> oldSize) {
		oldSize.outArgValue = getSize();
		EnsureWrite(1);
		setWriteIndex(getWriteIndex() + 1);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void EndWriteSegment(int oldSize)
	public void EndWriteSegment(int oldSize) {
		int startPos = getReadIndex() + oldSize;
		int segmentSize = getWriteIndex() - startPos - 1;

		// 0 111 1111
		if (segmentSize < 0x80) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos] = (byte)segmentSize;
			getBytes()[startPos] = (byte)segmentSize;
		}
		else if (segmentSize < 0x4000) { // 10 11 1111, -
			EnsureWrite(1);
			getBytes()[getWriteIndex()] = getBytes()[startPos + 1];
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos + 1] = (byte)segmentSize;
			getBytes()[startPos + 1] = (byte)segmentSize;

//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos] = (byte)((segmentSize >> 8) | 0x80);
			getBytes()[startPos] = (byte)((segmentSize >> 8) | 0x80);
			setWriteIndex(getWriteIndex() + 1);
		}
		else if (segmentSize < 0x200000) { // 110 1 1111, -,-
			EnsureWrite(2);
			getBytes()[getWriteIndex() + 1] = getBytes()[startPos + 2];
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos + 2] = (byte)segmentSize;
			getBytes()[startPos + 2] = (byte)segmentSize;

			getBytes()[getWriteIndex()] = getBytes()[startPos + 1];
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos + 1] = (byte)(segmentSize >> 8);
			getBytes()[startPos + 1] = (byte)(segmentSize >> 8);

//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos] = (byte)((segmentSize >> 16) | 0xc0);
			getBytes()[startPos] = (byte)((segmentSize >> 16) | 0xc0);
			setWriteIndex(getWriteIndex() + 2);
		}
		else if (segmentSize < 0x10000000) { // 1110 1111,-,-,-
			EnsureWrite(3);
			getBytes()[getWriteIndex() + 2] = getBytes()[startPos + 3];
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos + 3] = (byte)segmentSize;
			getBytes()[startPos + 3] = (byte)segmentSize;

			getBytes()[getWriteIndex() + 1] = getBytes()[startPos + 2];
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos + 2] = (byte)(segmentSize >> 8);
			getBytes()[startPos + 2] = (byte)(segmentSize >> 8);

			getBytes()[getWriteIndex()] = getBytes()[startPos + 1];
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos + 1] = (byte)(segmentSize >> 16);
			getBytes()[startPos + 1] = (byte)(segmentSize >> 16);

//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[startPos] = (byte)((segmentSize >> 24) | 0xe0);
			getBytes()[startPos] = (byte)((segmentSize >> 24) | 0xe0);
			setWriteIndex(getWriteIndex() + 3);
		}
		else {
			throw new RuntimeException("exceed max segment size");
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void ReadSegment(out int startIndex, out int segmentSize)
	private void ReadSegment(tangible.OutObject<Integer> startIndex, tangible.OutObject<Integer> segmentSize) {
		EnsureRead(1);
		int h = getBytes()[getReadIndex()];
	setReadIndex(getReadIndex() + 1);

		startIndex.outArgValue = getReadIndex();

		if (h < 0x80) {
			segmentSize.outArgValue = h;
			setReadIndex(getReadIndex() + segmentSize.outArgValue);
		}
		else if (h < 0xc0) {
			EnsureRead(1);
			segmentSize.outArgValue = ((h & 0x3f) << 8) | getBytes()[getReadIndex()];
			int endPos = getReadIndex() + segmentSize.outArgValue;
			getBytes()[getReadIndex()] = getBytes()[endPos];
			setReadIndex(getReadIndex() + segmentSize.outArgValue + 1);
		}
		else if (h < 0xe0) {
			EnsureRead(2);
			segmentSize.outArgValue = ((h & 0x1f) << 16) | ((int)getBytes()[getReadIndex()] << 8) | getBytes()[getReadIndex() + 1];
			int endPos = getReadIndex() + segmentSize.outArgValue;
			getBytes()[getReadIndex()] = getBytes()[endPos];
			getBytes()[getReadIndex() + 1] = getBytes()[endPos + 1];
			setReadIndex(getReadIndex() + segmentSize.outArgValue + 2);
		}
		else if (h < 0xf0) {
			EnsureRead(3);
			segmentSize.outArgValue = ((h & 0x0f) << 24) | ((int)getBytes()[getReadIndex()] << 16) | ((int)getBytes()[getReadIndex() + 1] << 8) | getBytes()[getReadIndex() + 2];
			int endPos = getReadIndex() + segmentSize.outArgValue;
			getBytes()[getReadIndex()] = getBytes()[endPos];
			getBytes()[getReadIndex() + 1] = getBytes()[endPos + 1];
			getBytes()[getReadIndex() + 2] = getBytes()[endPos + 2];
			setReadIndex(getReadIndex() + segmentSize.outArgValue + 3);
		}
		else {
			throw new RuntimeException("exceed max size");
		}
		if (getReadIndex() > getWriteIndex()) {
			throw new RuntimeException("segment data not enough");
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void BeginReadSegment(out int saveState)
	public void BeginReadSegment(tangible.OutObject<Integer> saveState) {
		int startPos;
		tangible.OutObject<Integer> tempOut_startPos = new tangible.OutObject<Integer>();
		int _;
		tangible.OutObject<Integer> tempOut__ = new tangible.OutObject<Integer>();
		ReadSegment(tempOut_startPos, tempOut__);
	_ = tempOut__.outArgValue;
	startPos = tempOut_startPos.outArgValue;

		saveState.outArgValue = getReadIndex();
		setReadIndex(startPos);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void EndReadSegment(int saveState)
	public void EndReadSegment(int saveState) {
		setReadIndex(saveState);
	}

	/** 
	 这个方法把剩余可用数据移到buffer开头。
	 【注意】这个方法会修改ReadIndex，WriteIndex。
	 最好仅在全部读取写入处理完成以后调用处理一次，
	 为下一次写入读取做准备。
	*/
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Campact()
	public void Campact() {
		int size = this.getSize();
		if (size > 0) {
			if (getReadIndex() > 0) {
				Buffer.BlockCopy(getBytes(), getReadIndex(), getBytes(), 0, size);
				setReadIndex(0);
				setWriteIndex(size);
			}
		}
		else {
			Reset();
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public byte[] Copy()
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public byte[] Copy()
	public byte[] Copy() {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] copy = new byte[Size];
		byte[] copy = new byte[getSize()];
		Buffer.BlockCopy(getBytes(), getReadIndex(), copy, 0, getSize());
		return copy;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void Reset()
	public void Reset() {
		setWriteIndex(0);
		setReadIndex(getWriteIndex());
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private static int ToPower2(int needSize)
	private static int ToPower2(int needSize) {
		int size = 1024;
		while (size < needSize) {
			size <<= 1;
		}
		return size;
	}


//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void EnsureWrite(int size)
	public void EnsureWrite(int size) {
		int newSize = getWriteIndex() + size;
		if (newSize > getCapacity()) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] newBytes = new byte[ToPower2(newSize)];
			byte[] newBytes = new byte[ToPower2(newSize)];
			setWriteIndex(getWriteIndex() - getReadIndex());
			Buffer.BlockCopy(getBytes(), getReadIndex(), newBytes, 0, getWriteIndex());
			setReadIndex(0);
			setBytes(newBytes);
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void EnsureRead(int size)
	private void EnsureRead(int size) {
		if (getReadIndex() + size > getWriteIndex()) {
			 throw new RuntimeException("EnsureRead " + size);
		}
	}

	public void WriteBool(boolean b) {
		EnsureWrite(1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex++] = (byte)(b ? 1 : 0);
		getBytes()[getWriteIndex()] = (byte)(b ? 1 : 0);
	setWriteIndex(getWriteIndex() + 1);
	}

	public boolean ReadBool() {
		EnsureRead(1);
		var tempVar = getBytes()[getReadIndex()] != 0;
	setReadIndex(getReadIndex() + 1);
	return tempVar;
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void WriteByte(byte x)
	public void WriteByte(byte x) {
		EnsureWrite(1);
		getBytes()[getWriteIndex()] = x;
	setWriteIndex(getWriteIndex() + 1);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte ReadByte()
	public byte ReadByte() {
		EnsureRead(1);
		var tempVar = getBytes()[getReadIndex()];
	setReadIndex(getReadIndex() + 1);
	return tempVar;
	}

	public void WriteShort(short x) {
		if (x >= 0) {
			if (x < 0x80) {
				EnsureWrite(1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex++] = (byte)x;
				getBytes()[getWriteIndex()] = (byte)x;
			setWriteIndex(getWriteIndex() + 1);
				return;
			}

			if (x < 0x4000) {
				EnsureWrite(2);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)x;
				getBytes()[getWriteIndex() + 1] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 8) | 0x80);
				getBytes()[getWriteIndex()] = (byte)((x >> 8) | 0x80);
				setWriteIndex(getWriteIndex() + 2);
				return;
			}
		}
		EnsureWrite(3);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = 0xff;
		getBytes()[getWriteIndex()] = (byte)0xff;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)x;
		getBytes()[getWriteIndex() + 2] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 8);
		getBytes()[getWriteIndex() + 1] = (byte)(x >> 8);
		setWriteIndex(getWriteIndex() + 3);
	}

	public short ReadShort() {
		EnsureRead(1);
		int h = getBytes()[getReadIndex()];
		if (h < 0x80) {
			setReadIndex(getReadIndex() + 1);
			return (short)h;
		}
		if (h < 0xc0) {
			EnsureRead(2);
			int x = ((h & 0x3f) << 8) | getBytes()[getReadIndex() + 1];
			setReadIndex(getReadIndex() + 2);
			return (short)x;
		}
		if ((h == 0xff)) {
			EnsureRead(3);
			int x = (getBytes()[getReadIndex() + 1] << 8) | getBytes()[getReadIndex() + 2];
			setReadIndex(getReadIndex() + 3);
			return (short)x;
		}
		throw new RuntimeException();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteInt4(int x)
	public void WriteInt4(int x) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] bs = BitConverter.GetBytes(x);
		byte[] bs = BitConverter.GetBytes(x);
		//if (false == BitConverter.IsLittleEndian)
		//    Array.Reverse(bs);
		Append(bs);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public int ReadInt4()
	public int ReadInt4() {
		EnsureRead(4);
		int x = BitConverter.ToInt32(getBytes(), getReadIndex());
		setReadIndex(getReadIndex() + 4);
		return x;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteLong8(long x)
	public void WriteLong8(long x) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] bs = BitConverter.GetBytes(x);
		byte[] bs = BitConverter.GetBytes(x);
		//if (false == BitConverter.IsLittleEndian)
		//    Array.Reverse(bs);
		Append(bs);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public long ReadLong8()
	public long ReadLong8() {
		EnsureRead(8);
		long x = BitConverter.ToInt64(getBytes(), getReadIndex());
		setReadIndex(getReadIndex() + 8);
		return x;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteInt(int x)
	public void WriteInt(int x) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: WriteUint((uint)x);
		WriteUint((int)x);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public int ReadInt()
	public int ReadInt() {
		return (int)ReadUint();
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void WriteUint(uint x)
	public void WriteUint(int x) {
		// 0 111 1111
		if (x < 0x80) {
			EnsureWrite(1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex++] = (byte)x;
			getBytes()[getWriteIndex()] = (byte)x;
		setWriteIndex(getWriteIndex() + 1);
		}
		else if (x < 0x4000) { // 10 11 1111, -
			EnsureWrite(2);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)x;
			getBytes()[getWriteIndex() + 1] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 8) | 0x80);
			getBytes()[getWriteIndex()] = (byte)((x >>> 8) | 0x80);
			setWriteIndex(getWriteIndex() + 2);
		}
		else if (x < 0x200000) { // 110 1 1111, -,-
			EnsureWrite(3);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)x;
			getBytes()[getWriteIndex() + 2] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 16) | 0xc0);
			getBytes()[getWriteIndex()] = (byte)((x >>> 16) | 0xc0);
			setWriteIndex(getWriteIndex() + 3);
		}
		else if (x < 0x10000000) { // 1110 1111,-,-,-
			EnsureWrite(4);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)x;
			getBytes()[getWriteIndex() + 3] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 24) | 0xe0);
			getBytes()[getWriteIndex()] = (byte)((x >>> 24) | 0xe0);
			setWriteIndex(getWriteIndex() + 4);
		}
		else {
			EnsureWrite(5);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = 0xf0;
			getBytes()[getWriteIndex()] = (byte)0xf0;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 4] = (byte)x;
			getBytes()[getWriteIndex() + 4] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 3] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 24);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 24);
			setWriteIndex(getWriteIndex() + 5);
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public uint ReadUint()
	public int ReadUint() {
		EnsureRead(1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint h = Bytes[ReadIndex];
		int h = getBytes()[getReadIndex()];
		if (h < 0x80) {
			setReadIndex(getReadIndex() + 1);
			return h;
		}
		else if (h < 0xc0) {
			EnsureRead(2);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
			int x = ((h & 0x3f) << 8) | getBytes()[getReadIndex() + 1];
			setReadIndex(getReadIndex() + 2);
			return x;
		}
		else if (h < 0xe0) {
			EnsureRead(3);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((h & 0x1f) << 16) | ((uint)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
			int x = ((h & 0x1f) << 16) | ((int)getBytes()[getReadIndex() + 1] << 8) | getBytes()[getReadIndex() + 2];
			setReadIndex(getReadIndex() + 3);
			return x;
		}
		else if (h < 0xf0) {

			EnsureRead(4);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((h & 0x0f) << 24) | ((uint)Bytes[ReadIndex + 1] << 16) | ((uint)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
			int x = ((h & 0x0f) << 24) | ((int)getBytes()[getReadIndex() + 1] << 16) | ((int)getBytes()[getReadIndex() + 2] << 8) | getBytes()[getReadIndex() + 3];
			setReadIndex(getReadIndex() + 4);
			return x;
		}
		else {
			EnsureRead(5);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((uint)Bytes[ReadIndex + 1] << 24) | ((uint)(Bytes[ReadIndex + 2] << 16)) | ((uint)Bytes[ReadIndex + 3] << 8) | Bytes[ReadIndex + 4];
			int x = ((int)getBytes()[getReadIndex() + 1] << 24) | ((int)(getBytes()[getReadIndex() + 2] << 16)) | ((int)getBytes()[getReadIndex() + 3] << 8) | getBytes()[getReadIndex() + 4];
			setReadIndex(getReadIndex() + 5);
			return x;
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteLong(long x)
	public void WriteLong(long x) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: WriteUlong((ulong)x);
		WriteUlong((long)x);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public long ReadLong()
	public long ReadLong() {
		return (long)ReadUlong();
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void WriteUlong(ulong x)
	public void WriteUlong(long x) {
		// 0 111 1111
		if (x < 0x80L) {
			EnsureWrite(1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex++] = (byte)x;
			getBytes()[getWriteIndex()] = (byte)x;
		setWriteIndex(getWriteIndex() + 1);
		}
		else if (x < 0x4000L) { // 10 11 1111, -
			EnsureWrite(2);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)x;
			getBytes()[getWriteIndex() + 1] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 8) | 0x80);
			getBytes()[getWriteIndex()] = (byte)((x >>> 8) | 0x80);
			setWriteIndex(getWriteIndex() + 2);
		}
		else if (x < 0x200000L) { // 110 1 1111, -,-
			EnsureWrite(3);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)x;
			getBytes()[getWriteIndex() + 2] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 16) | 0xc0);
			getBytes()[getWriteIndex()] = (byte)((x >>> 16) | 0xc0);
			setWriteIndex(getWriteIndex() + 3);
		}
		else if (x < 0x10000000L) { // 1110 1111,-,-,-
			EnsureWrite(4);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)x;
			getBytes()[getWriteIndex() + 3] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 24) | 0xe0);
			getBytes()[getWriteIndex()] = (byte)((x >>> 24) | 0xe0);
			setWriteIndex(getWriteIndex() + 4);
		}
		else if (x < 0x800000000L) { // 1111 0xxx,-,-,-,-
			EnsureWrite(5);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 4] = (byte)x;
			getBytes()[getWriteIndex() + 4] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 3] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 24);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 24);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 32) | 0xf0);
			getBytes()[getWriteIndex()] = (byte)((x >>> 32) | 0xf0);
			setWriteIndex(getWriteIndex() + 5);
		}
		else if (x < 0x40000000000L) { // 1111 10xx,
			EnsureWrite(6);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 5] = (byte)x;
			getBytes()[getWriteIndex() + 5] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 4] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 4] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 3] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 24);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 24);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 32);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 32);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 40) | 0xf8);
			getBytes()[getWriteIndex()] = (byte)((x >>> 40) | 0xf8);
			setWriteIndex(getWriteIndex() + 6);
		}
		else if (x < 0x200000000000L) { // 1111 110x,
			EnsureWrite(7);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 6] = (byte)x;
			getBytes()[getWriteIndex() + 6] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 5] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 5] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 4] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 4] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)(x >> 24);
			getBytes()[getWriteIndex() + 3] = (byte)(x >>> 24);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 32);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 32);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 40);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 40);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = (byte)((x >> 48) | 0xfc);
			getBytes()[getWriteIndex()] = (byte)((x >>> 48) | 0xfc);
			setWriteIndex(getWriteIndex() + 7);
		}
		else if (x < 0x100000000000000L) { // 1111 1110
			EnsureWrite(8);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 7] = (byte)x;
			getBytes()[getWriteIndex() + 7] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 6] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 6] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 5] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 5] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 4] = (byte)(x >> 24);
			getBytes()[getWriteIndex() + 4] = (byte)(x >>> 24);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)(x >> 32);
			getBytes()[getWriteIndex() + 3] = (byte)(x >>> 32);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 40);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 40);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 48);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 48);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = 0xfe;
			getBytes()[getWriteIndex()] = (byte)0xfe;
			setWriteIndex(getWriteIndex() + 8);
		}
		else { // 1111 1111
			EnsureWrite(9);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex] = 0xff;
			getBytes()[getWriteIndex()] = (byte)0xff;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 8] = (byte)x;
			getBytes()[getWriteIndex() + 8] = (byte)x;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 7] = (byte)(x >> 8);
			getBytes()[getWriteIndex() + 7] = (byte)(x >>> 8);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 6] = (byte)(x >> 16);
			getBytes()[getWriteIndex() + 6] = (byte)(x >>> 16);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 5] = (byte)(x >> 24);
			getBytes()[getWriteIndex() + 5] = (byte)(x >>> 24);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 4] = (byte)(x >> 32);
			getBytes()[getWriteIndex() + 4] = (byte)(x >>> 32);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 3] = (byte)(x >> 40);
			getBytes()[getWriteIndex() + 3] = (byte)(x >>> 40);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 2] = (byte)(x >> 48);
			getBytes()[getWriteIndex() + 2] = (byte)(x >>> 48);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Bytes[WriteIndex + 1] = (byte)(x >> 56);
			getBytes()[getWriteIndex() + 1] = (byte)(x >>> 56);
			setWriteIndex(getWriteIndex() + 9);
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ulong ReadUlong()
	public long ReadUlong() {
		EnsureRead(1);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint h = Bytes[ReadIndex];
		int h = getBytes()[getReadIndex()];
		if (h < 0x80) {
			setReadIndex(getReadIndex() + 1);
			return h;
		}
		else if (h < 0xc0) {
			EnsureRead(2);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
			int x = ((h & 0x3f) << 8) | getBytes()[getReadIndex() + 1];
			setReadIndex(getReadIndex() + 2);
			return x;
		}
		else if (h < 0xe0) {
			EnsureRead(3);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((h & 0x1f) << 16) | ((uint)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
			int x = ((h & 0x1f) << 16) | ((int)getBytes()[getReadIndex() + 1] << 8) | getBytes()[getReadIndex() + 2];
			setReadIndex(getReadIndex() + 3);
			return x;
		}
		else if (h < 0xf0) {
			EnsureRead(4);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint x = ((h & 0x0f) << 24) | ((uint)Bytes[ReadIndex + 1] << 16) | ((uint)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
			int x = ((h & 0x0f) << 24) | ((int)getBytes()[getReadIndex() + 1] << 16) | ((int)getBytes()[getReadIndex() + 2] << 8) | getBytes()[getReadIndex() + 3];
			setReadIndex(getReadIndex() + 4);
			return x;
		}
		else if (h < 0xf8) {
			EnsureRead(5);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xl = ((uint)Bytes[ReadIndex + 1] << 24) | ((uint)(Bytes[ReadIndex + 2] << 16)) | ((uint)Bytes[ReadIndex + 3] << 8) | (Bytes[ReadIndex + 4]);
			int xl = ((int)getBytes()[getReadIndex() + 1] << 24) | ((int)(getBytes()[getReadIndex() + 2] << 16)) | ((int)getBytes()[getReadIndex() + 3] << 8) | (getBytes()[getReadIndex() + 4]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xh = h & 0x07;
			int xh = h & 0x07;
			setReadIndex(getReadIndex() + 5);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ((ulong)xh << 32) | xl;
			return ((long)xh << 32) | xl;
		}
		else if (h < 0xfc) {
			EnsureRead(6);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xl = ((uint)Bytes[ReadIndex + 2] << 24) | ((uint)(Bytes[ReadIndex + 3] << 16)) | ((uint)Bytes[ReadIndex + 4] << 8) | (Bytes[ReadIndex + 5]);
			int xl = ((int)getBytes()[getReadIndex() + 2] << 24) | ((int)(getBytes()[getReadIndex() + 3] << 16)) | ((int)getBytes()[getReadIndex() + 4] << 8) | (getBytes()[getReadIndex() + 5]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xh = ((h & 0x03) << 8) | Bytes[ReadIndex + 1];
			int xh = ((h & 0x03) << 8) | getBytes()[getReadIndex() + 1];
			setReadIndex(getReadIndex() + 6);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ((ulong)xh << 32) | xl;
			return ((long)xh << 32) | xl;
		}
		else if (h < 0xfe) {
			EnsureRead(7);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xl = ((uint)Bytes[ReadIndex + 3] << 24) | ((uint)(Bytes[ReadIndex + 4] << 16)) | ((uint)Bytes[ReadIndex + 5] << 8) | (Bytes[ReadIndex + 6]);
			int xl = ((int)getBytes()[getReadIndex() + 3] << 24) | ((int)(getBytes()[getReadIndex() + 4] << 16)) | ((int)getBytes()[getReadIndex() + 5] << 8) | (getBytes()[getReadIndex() + 6]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xh = ((h & 0x01) << 16) | ((uint)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
			int xh = ((h & 0x01) << 16) | ((int)getBytes()[getReadIndex() + 1] << 8) | getBytes()[getReadIndex() + 2];
			setReadIndex(getReadIndex() + 7);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ((ulong)xh << 32) | xl;
			return ((long)xh << 32) | xl;
		}
		else if (h < 0xff) {
			EnsureRead(8);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xl = ((uint)Bytes[ReadIndex + 4] << 24) | ((uint)(Bytes[ReadIndex + 5] << 16)) | ((uint)Bytes[ReadIndex + 6] << 8) | (Bytes[ReadIndex + 7]);
			int xl = ((int)getBytes()[getReadIndex() + 4] << 24) | ((int)(getBytes()[getReadIndex() + 5] << 16)) | ((int)getBytes()[getReadIndex() + 6] << 8) | (getBytes()[getReadIndex() + 7]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xh = ((uint)Bytes[ReadIndex + 1] << 16) | ((uint)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
			int xh = ((int)getBytes()[getReadIndex() + 1] << 16) | ((int)getBytes()[getReadIndex() + 2] << 8) | getBytes()[getReadIndex() + 3];
			setReadIndex(getReadIndex() + 8);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ((ulong)xh << 32) | xl;
			return ((long)xh << 32) | xl;
		}
		else {
			EnsureRead(9);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xl = ((uint)Bytes[ReadIndex + 5] << 24) | ((uint)(Bytes[ReadIndex + 6] << 16)) | ((uint)Bytes[ReadIndex + 7] << 8) | (Bytes[ReadIndex + 8]);
			int xl = ((int)getBytes()[getReadIndex() + 5] << 24) | ((int)(getBytes()[getReadIndex() + 6] << 16)) | ((int)getBytes()[getReadIndex() + 7] << 8) | (getBytes()[getReadIndex() + 8]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint xh = ((uint)Bytes[ReadIndex + 1] << 24) | ((uint)Bytes[ReadIndex + 2] << 16) | ((uint)Bytes[ReadIndex + 3] << 8) | Bytes[ReadIndex + 4];
			int xh = ((int)getBytes()[getReadIndex() + 1] << 24) | ((int)getBytes()[getReadIndex() + 2] << 16) | ((int)getBytes()[getReadIndex() + 3] << 8) | getBytes()[getReadIndex() + 4];
			setReadIndex(getReadIndex() + 9);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ((ulong)xh << 32) | xl;
			return ((long)xh << 32) | xl;
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteFloat(float x)
	public void WriteFloat(float x) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] bs = BitConverter.GetBytes(x);
		byte[] bs = BitConverter.GetBytes(x);
		//if (false == BitConverter.IsLittleEndian)
		//    Array.Reverse(bs);
		Append(bs);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public float ReadFloat()
	public float ReadFloat() {
		EnsureRead(4);
		float x = BitConverter.ToSingle(getBytes(), getReadIndex());
		setReadIndex(getReadIndex() + 4);
		return x;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteDouble(double x)
	public void WriteDouble(double x) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] bs = BitConverter.GetBytes(x);
		byte[] bs = BitConverter.GetBytes(x);
		//if (false == BitConverter.IsLittleEndian)
		//    Array.Reverse(bs);
		Append(bs);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public double ReadDouble()
	public double ReadDouble() {
		EnsureRead(8);
		double x = BitConverter.ToDouble(getBytes(), getReadIndex());
		setReadIndex(getReadIndex() + 8);
		return x;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteString(string x)
	public void WriteString(String x) {
		WriteBytes(x.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public String ReadString() {
		int n = ReadInt();
		EnsureRead(n);
		String x = Encoding.UTF8.GetString(getBytes(), getReadIndex(), n);
		setReadIndex(getReadIndex() + n);
		return x;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteBytes(byte[] x)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteBytes(byte[] x)
	public void WriteBytes(byte[] x) {
		WriteBytes(x, 0, x.length);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void WriteBytes(byte[] x, int offset, int length)
	public void WriteBytes(byte[] x, int offset, int length) {
		WriteInt(length);
		EnsureWrite(length);
		Buffer.BlockCopy(x, offset, getBytes(), getWriteIndex(), length);
		setWriteIndex(getWriteIndex() + length);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteBinary(Zeze.Net.Binary binary)
	public void WriteBinary(Zeze.Net.Binary binary) {
		WriteBytes(binary.getBytes(), binary.getOffset(), binary.getCount());
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
			return new Net.Binary(ReadByteBuffer());
		}
		return new Zeze.Net.Binary(ReadBytes());
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] ReadBytes()
	public byte[] ReadBytes() {
		int n = ReadInt();
		EnsureRead(n);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] x = new byte[n];
		byte[] x = new byte[n];
		Buffer.BlockCopy(getBytes(), getReadIndex(), x, 0, n);
		setReadIndex(getReadIndex() + n);
		return x;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void SkipBytes()
	public void SkipBytes() {
		int n = ReadInt();
		EnsureRead(n);
		setReadIndex(getReadIndex() + n);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void SkipBytes4()
	public void SkipBytes4() {
		int n = ReadInt4();
		EnsureRead(n);
		setReadIndex(getReadIndex() + n);
	}

	/** 
	 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
	 
	 @return 
	*/
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public ByteBuffer ReadByteBuffer()
	public ByteBuffer ReadByteBuffer() {
		int n = ReadInt();
		EnsureRead(n);
		int cur = getReadIndex();
		setReadIndex(getReadIndex() + n);
		return ByteBuffer.Wrap(getBytes(), cur, n);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public void WriteByteBuffer(ByteBuffer o)
	public void WriteByteBuffer(ByteBuffer o) {
		WriteBytes(o.getBytes(), o.getReadIndex(), o.getSize());
	}

	@Override
	public String toString() {
		return BitConverter.toString(getBytes(), getReadIndex(), getSize());
	}

	@Override
	public boolean equals(Object obj) {
		boolean tempVar = obj instanceof ByteBuffer;
		ByteBuffer other = tempVar ? (ByteBuffer)obj : null;
		return (tempVar) && Equals(other);
	}

	public boolean equals(ByteBuffer other) {
		if (other == null) {
			return false;
		}

		if (this.getSize() != other.getSize()) {
			return false;
		}

		for (int i = 0, n = this.getSize(); i < n; i++) {
			if (getBytes()[getReadIndex() + i] != other.getBytes()[other.getReadIndex() + i]) {
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

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static int calc_hashnr(byte[] keys)
	public static int calc_hashnr(byte[] keys) {
		return calc_hashnr(keys, 0, keys.length);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static int calc_hashnr(byte[] keys, int offset, int len)
	public static int calc_hashnr(byte[] keys, int offset, int len) {
		int end = offset + len;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = 0;
		int hash = 0;
		for (int i = offset; i < end; ++i) {
			hash *= 16777619;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: hash ^= (uint)keys[i];
			hash ^= (int)keys[i];
		}
		return (int)hash;
	}

	@Override
	public int hashCode() {
		return (int)calc_hashnr(getBytes(), getReadIndex(), getSize());
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

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static void VerifyArrayIndex(byte[] bytes, int offset, int length)
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
			case BOOL:
				bb.ReadBool();
				break;
			case BYTE:
				bb.ReadByte();
				break;
			case SHORT:
				bb.ReadShort();
				break;
			case INT:
				bb.ReadInt();
				break;
			case LONG:
				bb.ReadLong();
				break;
			case FLOAT:
				bb.ReadFloat();
				break;
			case DOUBLE:
				bb.ReadDouble();
				break;
			case STRING:
			case BYTES:
			case LIST:
			case SET:
			case MAP:
			case BEAN:
				bb.SkipBytes();
				break;
			case DYNAMIC:
				bb.ReadLong8();
				bb.SkipBytes();
				break;
			default:
				throw new RuntimeException("SkipUnknownField");
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

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static bool Equals(byte[] left, byte[] right)
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

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static int Compare(byte[] left, byte[] right)
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
			return (new Integer(left.length)).compareTo(right.length); // shorter is small
		}

		for (int i = 0; i < left.length; i++) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: int c = left[i].CompareTo(right[i]);
			int c = (new Byte(left[i])).compareTo(right[i]);
			if (0 != c) {
				return c;
			}
		}
		return 0;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public static byte[] Copy(byte[] src)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public static byte[] Copy(byte[] src)
	public static byte[] Copy(byte[] src) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] result = new byte[src.Length];
		byte[] result = new byte[src.length];
		Buffer.BlockCopy(src, 0, result, 0, src.length);
		return result;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public static byte[] Copy(byte[] src, int offset, int length)
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public static byte[] Copy(byte[] src, int offset, int length)
	public static byte[] Copy(byte[] src, int offset, int length) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] result = new byte[length];
		byte[] result = new byte[length];
		Buffer.BlockCopy(src, offset, result, 0, length);
		return result;
	}
}