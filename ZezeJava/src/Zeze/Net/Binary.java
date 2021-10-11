package Zeze.Net;

import Zeze.Serialize.ByteBuffer;

// Bean 类型 binary 的辅助类。
// 构造之后就是只读的。
// byte[] bytes 参数传入以后，就不能再修改了。
public final class Binary {
	private byte[] _Bytes;

	public byte get(int index) {
		return _Bytes[index];
	}

	public byte[] getBytesInternalOnlyUnsafe() {
		return _Bytes;
	}

	private int Offset;
	public int getOffset() {
		return Offset;
	}

	private int Count;
	public int getCount() {
		return Count;
	}

	public static final Binary Empty = new Binary(ByteBuffer.Empty);

	/** 
	 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 
	 @param bytes
	 @param offset
	 @param count
	*/
	public Binary(byte[] bytes, int offset, int count) {
		_Bytes = bytes;
		Offset = offset;
		Count = count;
	}

	/** 
	 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 
	 @param bytes
	*/
	public Binary(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/** 
	 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 【一般用于临时存储】
	 
	 @param bb
	*/
	public Binary(Zeze.Serialize.ByteBuffer bb) {
		this(bb.Bytes, bb.ReadIndex, bb.Size());

	}

	/** 
	 这里调用Copy是因为ByteBuffer可能分配的保留内存较大。Copy返回实际大小的数据。
	 使用这个方法的地方一般是应用。这个数据可能被存储到表中。
	 
	 @param _s_
	*/
	public Binary(Zeze.Serialize.Serializable _s_) {
		this(Zeze.Serialize.ByteBuffer.Encode(_s_).Copy());
	}

	public void Decode(Zeze.Serialize.Serializable _s_) {
		Zeze.Serialize.ByteBuffer _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_Bytes, getOffset(), getCount());
		_s_.Decode(_bb_);
	}

	@Override
	public String toString() {
		return ByteBuffer.ToHex(_Bytes, Offset, Count);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Binary) {
			Binary other = (Binary)obj;
			return equals(other);
		}

		return false;
	}

	public boolean equals(Binary other) {
		if (other == null) {
			return false;
		}

		if (this.getCount() != other.getCount()) {
			return false;
		}

		for (int i = 0, n = this.getCount(); i < n; ++i) {
			if (_Bytes[getOffset() + i] != other._Bytes[other.getOffset() + i]) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return (int)Zeze.Serialize.ByteBuffer.calc_hashnr(_Bytes, getOffset(), getCount());
	}
}