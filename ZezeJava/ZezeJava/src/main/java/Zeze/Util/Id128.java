package Zeze.Util;

import java.math.BigInteger;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

public class Id128 implements Comparable<Id128>, Serializable, Cloneable {
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
	public Id128 clone() {
		try {
			return (Id128)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public void encode(byte @NotNull [] bytes16) {
		ByteBuffer.longLeHandler.set(bytes16, 0, low);
		ByteBuffer.longLeHandler.set(bytes16, 8, high);
	}

	public void decode(byte @NotNull [] bytes16) {
		low = ByteBuffer.ToLong(bytes16, 0);
		high = ByteBuffer.ToLong(bytes16, 8);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteLong8(low);
		bb.WriteLong8(high);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		low = bb.ReadLong8();
		high = bb.ReadLong8();
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
}
