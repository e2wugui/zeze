package Zeze.Util;

import java.util.Collection;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntList implements Comparable<IntList>, Cloneable, Serializable {
	public static final int[] EMPTY = new int[0];
	public static final int DEFAULT_SIZE = 8;

	protected int @NotNull [] buffer = EMPTY;
	protected int count;

	public static @NotNull IntList wrap(int @NotNull [] data, int count) {
		IntList il = new IntList();
		il.buffer = data;
		il.count = count > data.length ? data.length : Math.max(count, 0);
		return il;
	}

	public static @NotNull IntList wrap(int @NotNull [] data) {
		IntList il = new IntList();
		il.buffer = data;
		il.count = data.length;
		return il;
	}

	public static @NotNull IntList createSpace(int count) {
		IntList il = new IntList();
		if (count > 0) {
			il.buffer = new int[count];
			il.count = count;
		}
		return il;
	}

	public IntList() {
	}

	public IntList(int count) {
		reserveSpace(count);
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public IntList(@NotNull IntList il) {
		replace(il);
	}

	public IntList(int @NotNull [] data) {
		replace(data);
	}

	public IntList(int @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int @NotNull [] array() {
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

	public int get(int idx) {
		return buffer[idx];
	}

	public void set(int idx, int value) {
		buffer[idx] = value;
	}

	public int addValue(int idx, int value) {
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

	public int @NotNull [] toArray() {
		int n = count;
		if (n <= 0)
			return EMPTY;
		int[] buf = new int[n];
		System.arraycopy(buffer, 0, buf, 0, n);
		return buf;
	}

	public int @NotNull [] toArray(int fromIdx, int count) {
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= this.count || count <= 0)
			return EMPTY;
		int n = fromIdx + count;
		n = Integer.compareUnsigned(n, this.count) > 0 ? this.count - fromIdx : count;
		int[] buf = new int[n];
		System.arraycopy(buffer, fromIdx, buf, 0, n);
		return buf;
	}

	public @NotNull IntList wraps(int @NotNull [] data, int count) {
		buffer = data;
		this.count = count > data.length ? data.length : Math.max(count, 0);
		return this;
	}

	public @NotNull IntList wraps(int @NotNull [] data) {
		buffer = data;
		count = data.length;
		return this;
	}

	public void shrink(int count) {
		int[] buffer;
		int n = this.count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count >= (buffer = this.buffer).length)
			return;
		int[] buf = new int[count];
		System.arraycopy(buffer, 0, buf, 0, n);
		this.buffer = buf;
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		int[] buffer = this.buffer;
		if (count > buffer.length) {
			int cap;
			for (cap = DEFAULT_SIZE; count > cap; cap <<= 1) {
				// empty
			}
			int[] buf = new int[cap];
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
			buffer = new int[cap];
		}
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		this.count = count;
	}

	public void replace(int @NotNull [] data, int fromIdx, int count) {
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

	public void replace(int @NotNull [] data) {
		replace(data, 0, data.length);
	}

	public void replace(@NotNull IntList il) {
		replace(il.buffer, 0, il.count);
	}

	public void swap(@NotNull IntList il) {
		int count = this.count;
		this.count = il.count;
		il.count = count;
		int[] buf = il.buffer;
		il.buffer = buffer;
		buffer = buf;
	}

	public @NotNull IntList add(int value) {
		int n = count;
		int nNew = n + 1;
		reserve(nNew);
		buffer[n] = value;
		count = nNew;
		return this;
	}

	public @NotNull IntList addAll(int @NotNull [] data, int fromIdx, int count) {
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

	public @NotNull IntList addAll(int @NotNull [] data) {
		return addAll(data, 0, data.length);
	}

	public @NotNull IntList addAll(@NotNull IntList il) {
		return addAll(il.buffer, 0, il.count);
	}

	public @NotNull IntList addAll(@NotNull Collection<Integer> c) {
		int n = count;
		reserve(n + c.size());
		int[] buf = buffer;
		for (Integer v : c)
			buf[n++] = v;
		count = n;
		return this;
	}

	public @NotNull void addAllTo(@NotNull Collection<Integer> c) {
		int[] buf = buffer;
		for (int i = 0, n = count; i < n; i++)
			c.add(buf[i]);
	}

	public @NotNull IntList insert(int fromIdx, int data) {
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(data);
		reserve(n + 1);
		int[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = data;
		count = n + 1;
		return this;
	}

	public @NotNull IntList insert(int fromIdx, int @NotNull [] data, int idx, int count) {
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
		int[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(data, idx, buf, fromIdx, count);
		this.count = n + count;
		return this;
	}

	public @NotNull IntList insert(int fromIdx, int @NotNull [] data) {
		return insert(fromIdx, data, 0, data.length);
	}

	public @NotNull IntList insert(int fromIdx, @NotNull IntList il) {
		return insert(fromIdx, il.buffer, 0, il.count);
	}

	public @NotNull IntList remove(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			if (idx != lastIdx)
				System.arraycopy(buffer, idx + 1, buffer, idx, lastIdx - idx);
		}
		return this;
	}

	public @NotNull IntList removeAndExchangeLast(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			buffer[idx] = buffer[lastIdx];
		}
		return this;
	}

	public @NotNull IntList erase(int fromIdx, int toIdx) {
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

	public @NotNull IntList eraseFront(int count) {
		int n = this.count;
		if (count >= n)
			this.count = 0;
		else if (count > 0) {
			System.arraycopy(buffer, count, buffer, 0, n -= count);
			this.count = n;
		}
		return this;
	}

	public int indexOf(int value) {
		return indexOf(value, 0);
	}

	public int indexOf(int value, int fromIdx) {
		int[] buf = buffer;
		int n = count;
		for (int i = fromIdx; i < n; i++) {
			if (buf[i] == value)
				return i;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull IntList clone() {
		return new IntList(this);
	}

	@Override
	public int hashCode() {
		int[] buf = buffer;
		int n = count;
		int result = n;
		if (n <= 32) {
			for (int i = 0; i < n; i++)
				result = 31 * result + buf[i];
		} else {
			int i;
			for (i = 0; i < 16; i++)
				result = 31 * result + buf[i];
			for (i = n - 16; i < n; i++)
				result = 31 * result + buf[i];
		}
		return result;
	}

	@Override
	public int compareTo(@Nullable IntList il) {
		if (il == null)
			return 1;
		int n0 = count;
		int n1 = il.count;
		int n = Math.min(n0, n1);
		int[] buf = buffer;
		int[] data = il.buffer;
		for (int i = 0; i < n; i++) {
			int c = buf[i] - data[i];
			if (c != 0)
				return c;
		}
		return n0 - n1;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (!(o instanceof IntList))
			return false;
		IntList il = (IntList)o;
		int n = count;
		if (n != il.count)
			return false;
		int[] buf = buffer;
		int[] data = il.buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] != data[i])
				return false;
		}
		return true;
	}

	public boolean equals(@Nullable IntList il) {
		if (il == this)
			return true;
		if (il == null)
			return false;
		int n = count;
		if (n != il.count)
			return false;
		int[] buf = buffer;
		int[] data = il.buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] != data[i])
				return false;
		}
		return true;
	}

	public void foreach(@NotNull IntConsumer consumer) {
		int[] buf = buffer;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept(buf[i]);
	}

	public boolean foreachPred(@NotNull IntPredicate predicate) {
		int[] buf = buffer;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(buf[i]))
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder dump(@NotNull StringBuilder sb) {
		sb.append('[');
		int[] buf = buffer;
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
		return n > 0 ? dump(new StringBuilder(n * 2)).toString() : "[]";
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = count;
		bb.WriteUInt(n);
		if (n > 0) {
			int[] buf = buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
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
			int[] buf = buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	public void decode(@NotNull IByteBuffer bb, int n) {
		reserveSpace(n);
		int[] buf = buffer;
		for (int i = 0; i < n; i++)
			buf[i] = bb.ReadInt();
		count = n;
	}

	@Override
	public @NotNull String toString() {
		return "[" + count + "/" + buffer.length + "]";
	}
}
