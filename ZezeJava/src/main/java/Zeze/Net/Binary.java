package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import Zeze.Util.BitConverter;

/**
 * Bean 类型 binary 的辅助类。
 * 构造之后就是只读的。
 * 【警告】byte[] bytes 参数传入以后，就不能再修改了。
 */
public final class Binary {
	private final byte[] _Bytes;

	public byte get(int index) {
		return _Bytes[index];
	}

	byte[] InternalGetBytesUnsafe() {
		return _Bytes;
	}

	public void Encode(Zeze.Serialize.ByteBuffer bb) {
		bb.WriteBytes(_Bytes, Offset, Count);
	}

	public Zeze.Serialize.ByteBuffer Wrap() {
		return Zeze.Serialize.ByteBuffer.Wrap(_Bytes, Offset, Count);
	}

	private final int Offset;
	public int getOffset() {
		return Offset;
	}

	private final int Count;
	public int size() {
		return Count;
	}

	public static final Binary Empty = new Binary(ByteBuffer.Empty);

	/** 
	 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。

	*/
	public Binary(byte[] bytes, int offset, int count) {
		_Bytes = bytes;
		Offset = offset;
		Count = count;
	}

	/** 
	 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	*/
	public Binary(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/** 
	 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
	 【一般用于临时存储】
	*/
	public Binary(Zeze.Serialize.ByteBuffer bb) {
		this(bb.Bytes, bb.ReadIndex, bb.Size());

	}

	/** 
	 这里调用Copy是因为ByteBuffer可能分配的保留内存较大。Copy返回实际大小的数据。
	 使用这个方法的地方一般是应用。这个数据可能被存储到表中。
	*/
	public Binary(Zeze.Serialize.Serializable _s_) {
		this(Zeze.Serialize.ByteBuffer.Encode(_s_).Copy());
	}

	public void Decode(Zeze.Serialize.Serializable _s_) {
		var _copy_ = Zeze.Serialize.ByteBuffer.Wrap(_Bytes, getOffset(), size()).Copy();
		var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_copy_);
		_s_.Decode(_bb_);
	}

	@Override
	public String toString() {
		return BitConverter.toString(_Bytes, Offset, Count);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Binary) {
			var other = (Binary)obj;
			return equals(other);
		}

		return false;
	}

	public boolean equals(Binary other) {
		if (other == null) {
			return false;
		}

		if (this.size() != other.size()) {
			return false;
		}

		for (int i = 0, n = this.size(); i < n; ++i) {
			if (_Bytes[getOffset() + i] != other._Bytes[other.getOffset() + i]) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Zeze.Serialize.ByteBuffer.calc_hashnr(_Bytes, getOffset(), size());
	}
}