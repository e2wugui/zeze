package Zeze.Util;

import java.util.Collection;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LongList implements Comparable<LongList>, Cloneable, Serializable {
	public static final long[] EMPTY = new long[0];
	public static final int DEFAULT_SIZE = 8;

	private long @NotNull [] buffer = EMPTY;
	private int count;

	public static @NotNull LongList wrap(long @NotNull [] data, int count) {
		LongList ll = new LongList();
		ll.buffer = data;
		ll.count = count > data.length ? data.length : Math.max(count, 0);
		return ll;
	}

	public static @NotNull LongList wrap(long @NotNull [] data) {
		LongList ll = new LongList();
		ll.buffer = data;
		ll.count = data.length;
		return ll;
	}

	public static @NotNull LongList createSpace(int count) {
		LongList ll = new LongList();
		if (count > 0) {
			ll.buffer = new long[count];
			ll.count = count;
		}
		return ll;
	}

	public LongList() {
	}

	public LongList(int count) {
		reserveSpace(count);
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public LongList(@NotNull LongList ll) {
		replace(ll);
	}

	public LongList(long @NotNull [] data) {
		replace(data);
	}

	public LongList(long @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public long @NotNull [] array() {
		return buffer;
	}

	public boolean isEmpty() {
		return count <= 0;
	}

	public int size() {
		return count;
	}

	public int capacity() {
		return buffer.length;
	}

	public long get(int idx) {
		return buffer[idx];
	}

	public void set(int idx, long value) {
		buffer[idx] = value;
	}

	public long addValue(int idx, long value) {
		buffer[idx] = value += buffer[idx];
		return value;
	}

	public void clear() {
		count = 0;
	}

	public void reset() {
		buffer = EMPTY;
		count = 0;
	}

	public long @NotNull [] toArray() {
		int n = count;
		if (n <= 0)
			return EMPTY;
		long[] buf = new long[n];
		System.arraycopy(buffer, 0, buf, 0, n);
		return buf;
	}

	public long @NotNull [] toArray(int fromIdx, int count) {
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= this.count || count <= 0)
			return EMPTY;
		int n = fromIdx + count;
		n = Integer.compareUnsigned(n, this.count) > 0 ? this.count - fromIdx : count;
		long[] buf = new long[n];
		System.arraycopy(buffer, fromIdx, buf, 0, n);
		return buf;
	}

	public @NotNull LongList wraps(long @NotNull [] data, int count) {
		buffer = data;
		this.count = count > data.length ? data.length : Math.max(count, 0);
		return this;
	}

	public @NotNull LongList wraps(long @NotNull [] data) {
		buffer = data;
		count = data.length;
		return this;
	}

	public void shrink(int count) {
		long[] buffer;
		int n = this.count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count >= (buffer = this.buffer).length)
			return;
		long[] buf = new long[count];
		System.arraycopy(buffer, 0, buf, 0, n);
		this.buffer = buf;
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		long[] buffer = this.buffer;
		if (count > buffer.length) {
			int cap;
			for (cap = DEFAULT_SIZE; count > cap; cap <<= 1) {
				// empty
			}
			long[] buf = new long[cap];
			int n = this.count;
			if (n > 0)
				System.arraycopy(buffer, 0, buf, 0, n);
			this.buffer = buf;
		}
	}

	public void reserveSpace(int count) {
		if (count > buffer.length) {
			int cap;
			for (cap = 8; count > cap; cap <<= 1) {
				// empty
			}
			buffer = new long[cap];
		}
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		this.count = count;
	}

	public void replace(long @NotNull [] data, int fromIdx, int count) {
		if (count <= 0) {
			this.count = 0;
			return;
		}
		int len = data.length;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len) {
			this.count = 0;
			return;
		}
		if (count > (len -= fromIdx))
			count = len;
		reserveSpace(count);
		System.arraycopy(data, fromIdx, buffer, 0, count);
		this.count = count;
	}

	public void replace(long @NotNull [] data) {
		replace(data, 0, data.length);
	}

	public void replace(@NotNull LongList ll) {
		replace(ll.buffer, 0, ll.count);
	}

	public void swap(@NotNull LongList ll) {
		int count = this.count;
		this.count = ll.count;
		ll.count = count;
		long[] buf = ll.buffer;
		ll.buffer = buffer;
		buffer = buf;
	}

	public @NotNull LongList add(long value) {
		int n = count;
		int nNew = n + 1;
		reserve(nNew);
		buffer[n] = value;
		count = nNew;
		return this;
	}

	public @NotNull LongList addAll(long[] data, int fromIdx, int count) {
		if (count <= 0)
			return this;
		int len = data.length;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len)
			return this;
		if (count > (len -= fromIdx))
			count = len;
		int n = this.count;
		reserve(n + count);
		System.arraycopy(data, fromIdx, buffer, n, count);
		this.count = n + count;
		return this;
	}

	public @NotNull LongList addAll(long @NotNull [] data) {
		return addAll(data, 0, data.length);
	}

	public @NotNull LongList addAll(@NotNull LongList ll) {
		return addAll(ll.buffer, 0, ll.count);
	}

	public @NotNull LongList addAll(@NotNull Collection<Long> c) {
		int n = count;
		reserve(n + c.size());
		long[] buf = buffer;
		for (Long v : c)
			buf[n++] = v;
		count = n;
		return this;
	}

	public @NotNull void addAllTo(@NotNull Collection<Long> c) {
		long[] buf = buffer;
		for (int i = 0, n = count; i < n; i++)
			c.add(buf[i]);
	}

	public @NotNull LongList insert(int fromIdx, long data) {
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(data);
		reserve(n + 1);
		long[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = data;
		count = n + 1;
		return this;
	}

	public @NotNull LongList insert(int fromIdx, long @NotNull [] data, int idx, int count) {
		int n = this.count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return addAll(data, idx, count);
		if (count <= 0)
			return this;
		int len = data.length;
		if (idx < 0)
			idx = 0;
		if (idx >= len)
			return this;
		if (count > (len -= idx))
			count = len;
		reserve(n + count);
		long[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(data, idx, buf, fromIdx, count);
		this.count = n + count;
		return this;
	}

	public @NotNull LongList insert(int fromIdx, long @NotNull [] data) {
		return insert(fromIdx, data, 0, data.length);
	}

	public @NotNull LongList insert(int fromIdx, @NotNull LongList ll) {
		return insert(fromIdx, ll.buffer, 0, ll.count);
	}

	public @NotNull LongList remove(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			if (idx != lastIdx)
				System.arraycopy(buffer, idx + 1, buffer, idx, lastIdx - idx);
		}
		return this;
	}

	public @NotNull LongList removeAndExchangeLast(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			buffer[idx] = buffer[lastIdx];
		}
		return this;
	}

	public @NotNull LongList erase(int fromIdx, int toIdx) {
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n || fromIdx >= toIdx)
			return this;
		if (toIdx >= n)
			count = fromIdx;
		else {
			System.arraycopy(buffer, toIdx, buffer, fromIdx, n -= toIdx);
			count = n + fromIdx;
		}
		return this;
	}

	public @NotNull LongList eraseFront(int count) {
		int n = this.count;
		if (count >= n)
			this.count = 0;
		else if (count > 0) {
			System.arraycopy(buffer, count, buffer, 0, n -= count);
			this.count = n;
		}
		return this;
	}

	public int indexOf(long value) {
		return indexOf(value, 0);
	}

	public int indexOf(long value, int fromIdx) {
		long[] buf = buffer;
		int n = count;
		for (int i = fromIdx; i < n; i++) {
			if (buf[i] == value)
				return i;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull LongList clone() {
		return new LongList(this);
	}

	@Override
	public int hashCode() {
		long[] buf = buffer;
		int n = count;
		long result = n;
		if (n <= 32) {
			for (int i = 0; i < n; i++)
				result = 31L * result + buf[i];
		} else {
			int i;
			for (i = 0; i < 16; i++)
				result = 31L * result + buf[i];
			for (i = n - 16; i < n; i++)
				result = 31L * result + buf[i];
		}
		return (int)result;
	}

	@Override
	public int compareTo(@Nullable LongList ll) {
		if (ll == null)
			return 1;
		int n0 = count;
		int n1 = ll.count;
		int n = Math.min(n0, n1);
		long[] buf = buffer;
		long[] data = ll.buffer;
		for (int i = 0; i < n; i++) {
			long c = buf[i] - data[i];
			if (c != 0)
				return c < 0 ? -1 : 1;
		}
		return n0 - n1;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (!(o instanceof LongList))
			return false;
		LongList ll = (LongList)o;
		int n = count;
		if (n != ll.count)
			return false;
		long[] buf = buffer;
		long[] data = ll.buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] != data[i])
				return false;
		}
		return true;
	}

	public boolean equals(@Nullable LongList ll) {
		if (ll == this)
			return true;
		if (ll == null)
			return false;
		int n = count;
		if (n != ll.count)
			return false;
		long[] buf = buffer;
		long[] data = ll.buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] != data[i])
				return false;
		}
		return true;
	}

	public void foreach(@NotNull LongConsumer consumer) {
		long[] buf = buffer;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept(buf[i]);
	}

	public boolean foreachPred(@NotNull LongPredicate predicate) {
		long[] buf = buffer;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(buf[i]))
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder dump(@NotNull StringBuilder sb) {
		sb.append('[');
		long[] buf = buffer;
		int n = count;
		if (n > 0) {
			for (int i = 0; ; ) {
				sb.append(buf[i]);
				if (++i >= n)
					break;
				sb.append(',');
			}
		}
		return sb.append(']');
	}

	public @NotNull String dump() {
		int n = count;
		return n > 0 ? dump(new StringBuilder(n * 8)).toString() : "[]";
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = count;
		bb.WriteUInt(n);
		if (n > 0) {
			long[] buf = buffer;
			for (int i = 0; i < n; i++)
				bb.WriteLong(buf[i]);
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		decode(bb, bb.ReadUInt());
	}

	public void encode(@NotNull ByteBuffer bb, int n) {
		if (count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(count));
		if (n > 0) {
			long[] buf = buffer;
			for (int i = 0; i < n; i++)
				bb.WriteLong(buf[i]);
		}
	}

	public void decode(@NotNull IByteBuffer bb, int n) {
		reserveSpace(n);
		long[] buf = buffer;
		for (int i = 0; i < n; i++)
			buf[i] = bb.ReadLong();
		count = n;
	}

	@Override
	public @NotNull String toString() {
		return "[" + count + "/" + buffer.length + "]";
	}
}
