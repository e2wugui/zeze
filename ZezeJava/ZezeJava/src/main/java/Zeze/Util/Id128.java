package Zeze.Util;

import java.math.BigInteger;
import java.util.ArrayList;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.BeanKey;
import org.jetbrains.annotations.NotNull;

public class Id128 implements BeanKey, Comparable<Id128>, Serializable, Cloneable {
	public static final Id128 Zero = new Id128() {
		@Override
		public void assign(@NotNull Id128 id128) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void increment(long num) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			throw new UnsupportedOperationException();
		}
	};

	private long low; // unsigned
	private long high; // unsigned

	public Id128() {
	}

	/**
	 * @param high high is unsigned
	 * @param low  low is unsigned
	 */
	public Id128(long high, long low) {
		this.low = low;
		this.high = high;
	}

	public long getLow() {
		return low;
	}

	public long getHigh() {
		return high;
	}

	public void assign(@NotNull Id128 id128) {
		low = id128.low;
		high = id128.high;
	}

	/**
	 * 增加id的值。直接改变现有变量。
	 *
	 * @param num num is unsigned
	 */
	public void increment(long num) {
		low += num;
		if (Long.compareUnsigned(low, num) < 0)
			high += 1;
	}

	/**
	 * 增加id的值，返回一个新对象。
	 *
	 * @param num num is unsigned
	 * @return new Id128 instance that added.
	 */
	public @NotNull Id128 add(long num) {
		var result = clone();
		result.increment(num);
		return result;
	}

	@Override
	public @NotNull ArrayList<BVariable.Data> variables() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id128 clone() {
		try {
			return (Id128)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public void encodeRaw(@NotNull ByteBuffer bb) {
		bb.WriteULong(high);
		bb.WriteULong(low);
	}

	public void decodeRaw(@NotNull IByteBuffer bb) {
		high = bb.ReadULong();
		low = bb.ReadULong();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteByte((1 << ByteBuffer.TAG_SHIFT) + ByteBuffer.BYTES);
		int wi = bb.WriteIndex;
		bb.WriteByte(0); // skip binary size
		bb.WriteULong(high);
		bb.WriteULong(low);
		bb.Bytes[wi] = (byte)(bb.WriteIndex - wi - 1); // binary size [2,18]
		bb.WriteByte(0); // end of bean
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		int t = bb.ReadByte();
		if (bb.ReadTagSize(t) == 1) {
			t &= ByteBuffer.TAG_MASK;
			if (t != ByteBuffer.BYTES)
				throw new IllegalStateException("decode Id128 error: type=" + t);
			int n = bb.ReadUInt();
			int e = bb.getReadIndex() + n;
			high = bb.ReadULong();
			low = bb.ReadULong();
			if (bb.getReadIndex() != e)
				throw new IllegalStateException("decode Id128 error: size=" + n);
			bb.ReadTagSize(t = bb.ReadByte());
		}
		bb.skipAllUnknownFields(t);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(low) ^ Long.hashCode(high);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Id128))
			return false;
		var id128 = (Id128)o;
		return low == id128.low && high == id128.high;
	}

	@Override
	public int compareTo(@NotNull Id128 o) {
		var c = Long.compareUnsigned(high, o.high);
		return c != 0 ? c : Long.compareUnsigned(low, o.low);
	}

	@Override
	public @NotNull String toString() {
		var bytes = new byte[24];
		ByteBuffer.longBeHandler.set(bytes, 8, high);
		ByteBuffer.longBeHandler.set(bytes, 16, low);
		return new BigInteger(bytes, 7, 17).toString();
	}

	public void buildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append(this);
	}
}
