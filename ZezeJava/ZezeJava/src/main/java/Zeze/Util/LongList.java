package Zeze.Util;

import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class LongList implements Comparable<LongList>, Cloneable, Serializable {
	public static final long[] EMPTY = new long[0];
	public static final int DEFAULT_SIZE = 8;

	private long[] _buffer = EMPTY;
	private int _count;

	public static LongList wrap(long[] data, int count) {
		LongList o = new LongList();
		o._buffer = data;
		o._count = count > data.length ? data.length : Math.max(count, 0);
		return o;
	}

	public static LongList wrap(long[] data) {
		if (data == null)
			throw new IllegalArgumentException("null data");
		LongList o = new LongList();
		o._buffer = data;
		o._count = data.length;
		return o;
	}

	public static LongList createSpace(int count) {
		LongList o = new LongList();
		if (count > 0) {
			o._buffer = new long[count];
			o._count = count;
		}
		return o;
	}

	public LongList() {
	}

	public LongList(int count) {
		reserveSpace(count);
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public LongList(LongList o) {
		replace(o);
	}

	public LongList(long[] data) {
		replace(data);
	}

	public LongList(long[] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public long[] array() {
		return _buffer;
	}

	public boolean empty() {
		return _count <= 0;
	}

	public int size() {
		return _count;
	}

	public int capacity() {
		return _buffer.length;
	}

	public int remain() {
		return _count;
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

	public long[] toArray() {
		int n = _count;
		if (n <= 0)
			return EMPTY;
		long[] buf = new long[n];
		System.arraycopy(_buffer, 0, buf, 0, n);
		return buf;
	}

	public long[] toArray(int fromIdx, int count) {
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

	public LongList wraps(long[] data, int count) {
		_buffer = data;
		_count = count > data.length ? data.length : Math.max(count, 0);
		return this;
	}

	public LongList wraps(long[] data) {
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
			for (cap = 8; count > cap; cap <<= 1) {
				// empty
			}
			long[] buf = new long[cap];
			int n = _count;
			if (n > 0)
				System.arraycopy(buffer, 0, buf, 0, n);
			_buffer = buf;
		}
	}

	public final void reserveSpace(int count) {
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

	public final void replace(long[] data, int fromIdx, int count) {
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

	public final void replace(long[] data) {
		replace(data, 0, data.length);
	}

	public final void replace(LongList o) {
		replace(o._buffer, 0, o._count);
	}

	public void swap(LongList o) {
		int count = _count;
		_count = o._count;
		o._count = count;
		long[] buf = o._buffer;
		o._buffer = _buffer;
		_buffer = buf;
	}

	public LongList add(long value) {
		int n = _count;
		int nNew = n + 1;
		reserve(nNew);
		_buffer[n] = value;
		_count = nNew;
		return this;
	}

	public LongList add(long[] data, int fromIdx, int count) {
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

	public LongList add(long[] data) {
		return add(data, 0, data.length);
	}

	public LongList add(LongList o) {
		return add(o._buffer, 0, o._count);
	}

	public LongList insert(int fromIdx, long data) {
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

	public LongList insert(int fromIdx, long[] data, int idx, int count) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(data, idx, count);
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

	public LongList insert(int fromIdx, long[] data) {
		return insert(fromIdx, data, 0, data.length);
	}

	public LongList insert(int fromIdx, LongList o) {
		return insert(fromIdx, o._buffer, 0, o._count);
	}

	public LongList remove(int idx) {
		int lastIdx = _count - 1;
		if (idx < 0 || idx > lastIdx)
			return this;
		_count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(_buffer, idx + 1, _buffer, idx, lastIdx - idx);
		return this;
	}

	public LongList removeAndExchangeLast(int idx) {
		int lastIdx = _count - 1;
		if (idx >= 0 && idx <= lastIdx) {
			_count = lastIdx;
			_buffer[idx] = _buffer[lastIdx];
		}
		return this;
	}

	public LongList erase(int fromIdx, int toIdx) {
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

	public LongList eraseFront(int count) {
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
		int n = _count;
		for (int i = fromIdx; i < n; ++i) {
			if (_buffer[i] != value)
				continue;
			return i;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public LongList clone() {
		return new LongList(this);
	}

	@Override
	public int hashCode() {
		long[] buf = _buffer;
		int n = _count;
		long result = n;
		if (n <= 32) {
			for (int i = 0; i < n; ++i)
				result = 31L * result + buf[i];
		} else {
			int i;
			for (i = 0; i < 16; ++i)
				result = 31L * result + buf[i];
			for (i = n - 16; i < n; ++i)
				result = 31L * result + buf[i];
		}
		return (int)result;
	}

	@Override
	public int compareTo(LongList o) {
		if (o == null)
			return 1;
		int n0 = _count;
		int n1 = o._count;
		int n = Math.min(n0, n1);
		long[] buf = _buffer;
		long[] data = o._buffer;
		for (int i = 0; i < n; ++i) {
			long c = buf[i] - data[i];
			if (c == 0L)
				continue;
			return c < 0L ? -1 : 1;
		}
		return n0 - n1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof LongList))
			return false;
		LongList oct = (LongList)o;
		int n = _count;
		if (n != oct._count)
			return false;
		long[] buf = _buffer;
		long[] data = oct._buffer;
		for (int i = 0; i < n; ++i) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public final boolean equals(LongList oct) {
		if (this == oct)
			return true;
		if (oct == null)
			return false;
		int n = _count;
		if (n != oct._count)
			return false;
		long[] buf = _buffer;
		long[] data = oct._buffer;
		for (int i = 0; i < n; ++i) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public void foreach(LongConsumer consumer) {
		int n = _count;
		for (int i = 0; i < n; ++i)
			consumer.accept(_buffer[i]);
	}

	public boolean foreachPred(LongPredicate predicate) {
		int n = _count;
		for (int i = 0; i < n; ++i) {
			if (!predicate.test(_buffer[i]))
				return false;
		}
		return true;
	}

	public StringBuilder dump(StringBuilder sb) {
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

	public String dump() {
		int n = _count;
		return n > 0 ? dump(new StringBuilder(n * 4)).toString() : "[]";
	}

	@Override
	public void encode(ByteBuffer bb) {
		int count = _count;
		if (count <= 0)
			return;
		long[] buf = _buffer;
		bb.WriteUInt(count);
		for (int i = 0; i < count; ++i)
			bb.WriteLong(buf[i]);
	}

	@Override
	public void decode(ByteBuffer s) {
		int count = s.ReadUInt();
		reserveSpace(count);
		long[] buf = _buffer;
		for (int i = 0; i < count; ++i)
			buf[i] = s.ReadLong();
		_count = count;
	}

	@Override
	public String toString() {
		return "[" + _count + "/" + _buffer.length + "]";
	}
}
