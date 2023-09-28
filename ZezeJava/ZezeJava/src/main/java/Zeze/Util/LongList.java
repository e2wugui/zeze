package Zeze.Util;

import java.util.Collection;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LongList implements Comparable<LongList>, Cloneable, Serializable {
	public static final long[] EMPTY = new long[0];
	public static final int DEFAULT_SIZE = 8;

	private long @NotNull [] _buffer = EMPTY;
	private int _count;

	public static @NotNull LongList wrap(long @NotNull [] data, int count) {
		LongList ll = new LongList();
		ll._buffer = data;
		ll._count = count > data.length ? data.length : Math.max(count, 0);
		return ll;
	}

	public static @NotNull LongList wrap(long @NotNull [] data) {
		LongList ll = new LongList();
		ll._buffer = data;
		ll._count = data.length;
		return ll;
	}

	public static @NotNull LongList createSpace(int count) {
		LongList ll = new LongList();
		if (count > 0) {
			ll._buffer = new long[count];
			ll._count = count;
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
		return _buffer;
	}

	public boolean isEmpty() {
		return _count <= 0;
	}

	public int size() {
		return _count;
	}

	public int capacity() {
		return _buffer.length;
	}

	public long get(int idx) {
		return _buffer[idx];
	}

	public void set(int idx, long value) {
		_buffer[idx] = value;
	}

	public long addValue(int idx, long value) {
		_buffer[idx] = value += _buffer[idx];
		return value;
	}

	public void clear() {
		_count = 0;
	}

	public void reset() {
		_buffer = EMPTY;
		_count = 0;
	}

	public long @NotNull [] toArray() {
		int n = _count;
		if (n <= 0)
			return EMPTY;
		long[] buf = new long[n];
		System.arraycopy(_buffer, 0, buf, 0, n);
		return buf;
	}

	public long @NotNull [] toArray(int fromIdx, int count) {
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= _count || count <= 0)
			return EMPTY;
		int n = fromIdx + count;
		n = n < 0 || n > _count ? _count - fromIdx : count;
		long[] buf = new long[n];
		System.arraycopy(_buffer, fromIdx, buf, 0, n);
		return buf;
	}

	public @NotNull LongList wraps(long @NotNull [] data, int count) {
		_buffer = data;
		_count = count > data.length ? data.length : Math.max(count, 0);
		return this;
	}

	public @NotNull LongList wraps(long @NotNull [] data) {
		_buffer = data;
		_count = data.length;
		return this;
	}

	public void shrink(int count) {
		long[] buffer;
		int n = _count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count >= (buffer = _buffer).length)
			return;
		long[] buf = new long[count];
		System.arraycopy(buffer, 0, buf, 0, n);
		_buffer = buf;
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		long[] buffer = _buffer;
		if (count > buffer.length) {
			int cap;
			for (cap = DEFAULT_SIZE; count > cap; cap <<= 1) {
				// empty
			}
			long[] buf = new long[cap];
			int n = _count;
			if (n > 0)
				System.arraycopy(buffer, 0, buf, 0, n);
			_buffer = buf;
		}
	}

	public void reserveSpace(int count) {
		if (count > _buffer.length) {
			int cap;
			for (cap = 8; count > cap; cap <<= 1) {
				// empty
			}
			_buffer = new long[cap];
		}
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		_count = count;
	}

	public void replace(long @NotNull [] data, int fromIdx, int count) {
		if (count <= 0) {
			_count = 0;
			return;
		}
		int len = data.length;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len) {
			_count = 0;
			return;
		}
		if (count > (len -= fromIdx))
			count = len;
		reserveSpace(count);
		System.arraycopy(data, fromIdx, _buffer, 0, count);
		_count = count;
	}

	public void replace(long @NotNull [] data) {
		replace(data, 0, data.length);
	}

	public void replace(@NotNull LongList ll) {
		replace(ll._buffer, 0, ll._count);
	}

	public void swap(@NotNull LongList ll) {
		int count = _count;
		_count = ll._count;
		ll._count = count;
		long[] buf = ll._buffer;
		ll._buffer = _buffer;
		_buffer = buf;
	}

	public @NotNull LongList add(long value) {
		int n = _count;
		int nNew = n + 1;
		reserve(nNew);
		_buffer[n] = value;
		_count = nNew;
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
		int n = _count;
		reserve(n + count);
		System.arraycopy(data, fromIdx, _buffer, n, count);
		_count = n + count;
		return this;
	}

	public @NotNull LongList addAll(long @NotNull [] data) {
		return addAll(data, 0, data.length);
	}

	public @NotNull LongList addAll(@NotNull LongList ll) {
		return addAll(ll._buffer, 0, ll._count);
	}

	public @NotNull LongList addAll(@NotNull Collection<Long> c) {
		int n = _count;
		reserve(n + c.size());
		long[] buf = _buffer;
		for (Long v : c)
			buf[n++] = v;
		_count = n;
		return this;
	}

	public @NotNull void addAllTo(@NotNull Collection<Long> c) {
		long[] buf = _buffer;
		for (int i = 0, n = _count; i < n; i++)
			c.add(buf[i]);
	}

	public @NotNull LongList insert(int fromIdx, long data) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(data);
		reserve(n + 1);
		long[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = data;
		_count = n + 1;
		return this;
	}

	public @NotNull LongList insert(int fromIdx, long @NotNull [] data, int idx, int count) {
		int n = _count;
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
		long[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(data, idx, buf, fromIdx, count);
		_count = n + count;
		return this;
	}

	public @NotNull LongList insert(int fromIdx, long @NotNull [] data) {
		return insert(fromIdx, data, 0, data.length);
	}

	public @NotNull LongList insert(int fromIdx, @NotNull LongList ll) {
		return insert(fromIdx, ll._buffer, 0, ll._count);
	}

	public @NotNull LongList remove(int idx) {
		int lastIdx = _count - 1;
		if (idx < 0 || idx > lastIdx)
			return this;
		_count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(_buffer, idx + 1, _buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull LongList removeAndExchangeLast(int idx) {
		int lastIdx = _count - 1;
		if (idx >= 0 && idx <= lastIdx) {
			_count = lastIdx;
			_buffer[idx] = _buffer[lastIdx];
		}
		return this;
	}

	public @NotNull LongList erase(int fromIdx, int toIdx) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n || fromIdx >= toIdx)
			return this;
		if (toIdx >= n)
			_count = fromIdx;
		else {
			System.arraycopy(_buffer, toIdx, _buffer, fromIdx, n -= toIdx);
			_count = n + fromIdx;
		}
		return this;
	}

	public @NotNull LongList eraseFront(int count) {
		int n = _count;
		if (count >= n)
			_count = 0;
		else if (count > 0) {
			System.arraycopy(_buffer, count, _buffer, 0, n -= count);
			_count = n;
		}
		return this;
	}

	public int indexOf(long value) {
		return indexOf(value, 0);
	}

	public int indexOf(long value, int fromIdx) {
		long[] buf = _buffer;
		int n = _count;
		for (int i = fromIdx; i < n; i++) {
			if (buf[i] != value)
				continue;
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
		long[] buf = _buffer;
		int n = _count;
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
		int n0 = _count;
		int n1 = ll._count;
		int n = Math.min(n0, n1);
		long[] buf = _buffer;
		long[] data = ll._buffer;
		for (int i = 0; i < n; i++) {
			long c = buf[i] - data[i];
			if (c == 0L)
				continue;
			return c < 0L ? -1 : 1;
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
		int n = _count;
		if (n != ll._count)
			return false;
		long[] buf = _buffer;
		long[] data = ll._buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public boolean equals(@Nullable LongList ll) {
		if (ll == this)
			return true;
		if (ll == null)
			return false;
		int n = _count;
		if (n != ll._count)
			return false;
		long[] buf = _buffer;
		long[] data = ll._buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public void foreach(@NotNull LongConsumer consumer) {
		long[] buf = _buffer;
		int n = _count;
		for (int i = 0; i < n; i++)
			consumer.accept(buf[i]);
	}

	public boolean foreachPred(@NotNull LongPredicate predicate) {
		long[] buf = _buffer;
		int n = _count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(buf[i]))
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder dump(@NotNull StringBuilder sb) {
		sb.append('[');
		long[] buf = _buffer;
		int n = _count;
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
		int n = _count;
		return n > 0 ? dump(new StringBuilder(n * 8)).toString() : "[]";
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = _count;
		bb.WriteUInt(n);
		if (n > 0) {
			long[] buf = _buffer;
			for (int i = 0; i < n; i++)
				bb.WriteLong(buf[i]);
		}
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		decode(bb, bb.ReadUInt());
	}

	public void encode(@NotNull ByteBuffer bb, int n) {
		if (_count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(_count));
		if (n > 0) {
			long[] buf = _buffer;
			for (int i = 0; i < n; i++)
				bb.WriteLong(buf[i]);
		}
	}

	public void decode(@NotNull ByteBuffer bb, int n) {
		reserveSpace(n);
		long[] buf = _buffer;
		for (int i = 0; i < n; i++)
			buf[i] = bb.ReadLong();
		_count = n;
	}

	@Override
	public @NotNull String toString() {
		return "[" + _count + "/" + _buffer.length + "]";
	}
}
