package Zeze.Util;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloatList implements Comparable<FloatList>, Cloneable, Serializable {
	public static final float[] EMPTY = new float[0];
	public static final int DEFAULT_SIZE = 8;

	protected float @NotNull [] _buffer = EMPTY;
	protected int _count;

	public static @NotNull FloatList wrap(float @NotNull [] data, int count) {
		FloatList fl = new FloatList();
		fl._buffer = data;
		fl._count = count > data.length ? data.length : Math.max(count, 0);
		return fl;
	}

	public static @NotNull FloatList wrap(float @NotNull [] data) {
		FloatList fl = new FloatList();
		fl._buffer = data;
		fl._count = data.length;
		return fl;
	}

	public static @NotNull FloatList createSpace(int count) {
		FloatList fl = new FloatList();
		if (count > 0) {
			fl._buffer = new float[count];
			fl._count = count;
		}
		return fl;
	}

	public FloatList() {
	}

	public FloatList(int count) {
		reserveSpace(count);
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public FloatList(@NotNull FloatList fl) {
		replace(fl);
	}

	public FloatList(float @NotNull [] data) {
		replace(data);
	}

	public FloatList(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public float @NotNull [] array() {
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

	public float get(int idx) {
		return _buffer[idx];
	}

	public void set(int idx, float value) {
		_buffer[idx] = value;
	}

	public float addValue(int idx, float value) {
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

	public float @NotNull [] toArray() {
		int n = _count;
		if (n <= 0)
			return EMPTY;
		float[] buf = new float[n];
		System.arraycopy(_buffer, 0, buf, 0, n);
		return buf;
	}

	public float @NotNull [] toArray(int fromIdx, int count) {
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= _count || count <= 0)
			return EMPTY;
		int n = fromIdx + count;
		n = n < 0 || n > _count ? _count - fromIdx : count;
		float[] buf = new float[n];
		System.arraycopy(_buffer, fromIdx, buf, 0, n);
		return buf;
	}

	public @NotNull FloatList wraps(float @NotNull [] data, int count) {
		_buffer = data;
		_count = count > data.length ? data.length : Math.max(count, 0);
		return this;
	}

	public @NotNull FloatList wraps(float @NotNull [] data) {
		_buffer = data;
		_count = data.length;
		return this;
	}

	public void shrink(int count) {
		float[] buffer;
		int n = _count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count >= (buffer = _buffer).length)
			return;
		float[] buf = new float[count];
		System.arraycopy(buffer, 0, buf, 0, n);
		_buffer = buf;
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		float[] buffer = _buffer;
		if (count > buffer.length) {
			int cap;
			for (cap = DEFAULT_SIZE; count > cap; cap <<= 1) {
				// empty
			}
			float[] buf = new float[cap];
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
			_buffer = new float[cap];
		}
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		_count = count;
	}

	public void replace(float @NotNull [] data, int fromIdx, int count) {
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

	public void replace(float @NotNull [] data) {
		replace(data, 0, data.length);
	}

	public void replace(@NotNull FloatList fl) {
		replace(fl._buffer, 0, fl._count);
	}

	public void swap(@NotNull FloatList fl) {
		int count = _count;
		_count = fl._count;
		fl._count = count;
		float[] buf = fl._buffer;
		fl._buffer = _buffer;
		_buffer = buf;
	}

	public @NotNull FloatList add(float value) {
		int n = _count;
		int nNew = n + 1;
		reserve(nNew);
		_buffer[n] = value;
		_count = nNew;
		return this;
	}

	public @NotNull FloatList addAll(float[] data, int fromIdx, int count) {
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

	public @NotNull FloatList addAll(float @NotNull [] data) {
		return addAll(data, 0, data.length);
	}

	public @NotNull FloatList addAll(@NotNull FloatList fl) {
		return addAll(fl._buffer, 0, fl._count);
	}

	public @NotNull FloatList addAll(@NotNull Collection<Float> c) {
		int n = _count;
		reserve(n + c.size());
		float[] buf = _buffer;
		for (Float v : c)
			buf[n++] = v;
		_count = n;
		return this;
	}

	public @NotNull void addAllTo(@NotNull Collection<Float> c) {
		float[] buf = _buffer;
		for (int i = 0, n = _count; i < n; i++)
			c.add(buf[i]);
	}

	public @NotNull FloatList insert(int fromIdx, float data) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(data);
		reserve(n + 1);
		float[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = data;
		_count = n + 1;
		return this;
	}

	public @NotNull FloatList insert(int fromIdx, float @NotNull [] data, int idx, int count) {
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
		float[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(data, idx, buf, fromIdx, count);
		_count = n + count;
		return this;
	}

	public @NotNull FloatList insert(int fromIdx, float @NotNull [] data) {
		return insert(fromIdx, data, 0, data.length);
	}

	public @NotNull FloatList insert(int fromIdx, @NotNull FloatList fl) {
		return insert(fromIdx, fl._buffer, 0, fl._count);
	}

	public @NotNull FloatList remove(int idx) {
		int lastIdx = _count - 1;
		if (idx < 0 || idx > lastIdx)
			return this;
		_count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(_buffer, idx + 1, _buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull FloatList removeAndExchangeLast(int idx) {
		int lastIdx = _count - 1;
		if (idx >= 0 && idx <= lastIdx) {
			_count = lastIdx;
			_buffer[idx] = _buffer[lastIdx];
		}
		return this;
	}

	public @NotNull FloatList erase(int fromIdx, int toIdx) {
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

	public @NotNull FloatList eraseFront(int count) {
		int n = _count;
		if (count >= n)
			_count = 0;
		else if (count > 0) {
			System.arraycopy(_buffer, count, _buffer, 0, n -= count);
			_count = n;
		}
		return this;
	}

	public int indexOf(float value) {
		return indexOf(value, 0);
	}

	public int indexOf(float value, int fromIdx) {
		float[] buf = _buffer;
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
	public @NotNull FloatList clone() {
		return new FloatList(this);
	}

	@Override
	public int hashCode() {
		float[] buf = _buffer;
		int n = _count;
		float result = n;
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
	public int compareTo(@Nullable FloatList fl) {
		if (fl == null)
			return 1;
		int n0 = _count;
		int n1 = fl._count;
		int n = Math.min(n0, n1);
		float[] buf = _buffer;
		float[] data = fl._buffer;
		for (int i = 0; i < n; i++) {
			float c = buf[i] - data[i];
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
		if (!(o instanceof FloatList))
			return false;
		FloatList fl = (FloatList)o;
		int n = _count;
		if (n != fl._count)
			return false;
		float[] buf = _buffer;
		float[] data = fl._buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public boolean equals(@Nullable FloatList fl) {
		if (fl == this)
			return true;
		if (fl == null)
			return false;
		int n = _count;
		if (n != fl._count)
			return false;
		float[] buf = _buffer;
		float[] data = fl._buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public interface FloatConsumer {
		void accept(float value);
	}

	public void foreach(@NotNull FloatConsumer consumer) {
		float[] buf = _buffer;
		int n = _count;
		for (int i = 0; i < n; i++)
			consumer.accept(buf[i]);
	}

	public interface FloatPredicate {
		boolean test(float value);
	}

	public boolean foreachPred(@NotNull FloatPredicate predicate) {
		float[] buf = _buffer;
		int n = _count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(buf[i]))
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder dump(@NotNull StringBuilder sb) {
		sb.append('[');
		float[] buf = _buffer;
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
		if (n > 0)
			bb.WriteFloats(_buffer, 0, n);
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		decode(bb, bb.ReadUInt());
	}

	public void encode(@NotNull ByteBuffer bb, int n) {
		if (_count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(_count));
		if (n > 0)
			bb.WriteFloats(_buffer, 0, n);
	}

	public void decode(@NotNull ByteBuffer bb, int n) {
		reserveSpace(n);
		bb.ReadFloats(_buffer, 0, n);
		_count = n;
	}

	@Override
	public @NotNull String toString() {
		return "[" + _count + "/" + _buffer.length + "]";
	}
}
