package Zeze.Util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KVList<K, V> implements Cloneable {
	public static final Object[] EMPTY = new Object[0];
	public static final int DEFAULT_SIZE = 8;

	private Object @NotNull [] keys = EMPTY;
	private Object @NotNull [] values = EMPTY;
	private int count;

	public static <K, V> @NotNull KVList<K, V> wrap(K @NotNull [] keys, V @NotNull [] values, int count) {
		KVList<K, V> kvl = new KVList<>();
		kvl.keys = keys;
		kvl.values = values;
		kvl.count = Math.min(Math.min(keys.length, values.length), Math.max(count, 0));
		return kvl;
	}

	public static <K, V> @NotNull KVList<K, V> wrap(K @NotNull [] keys, V @NotNull [] values) {
		KVList<K, V> kvl = new KVList<>();
		kvl.keys = keys;
		kvl.values = values;
		kvl.count = Math.min(keys.length, values.length);
		return kvl;
	}

	public static <K, V> @NotNull KVList<K, V> createSpace(int count) {
		KVList<K, V> kvl = new KVList<>();
		if (count > 0) {
			kvl.keys = new Object[count];
			kvl.values = new Object[count];
			kvl.count = count;
		}
		return kvl;
	}

	public KVList() {
	}

	public KVList(int count) {
		reserveSpace(count);
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public KVList(@NotNull KVList<K, V> kvl) {
		replace(kvl);
	}

	public KVList(K @NotNull [] keys, V @NotNull [] values) {
		replace(keys, values);
	}

	public KVList(K @NotNull [] keys, V @NotNull [] values, int fromIdx, int count) {
		replace(keys, values, fromIdx, count);
	}

	public Object @NotNull [] keys() {
		return keys;
	}

	public Object @NotNull [] values() {
		return values;
	}

	public boolean isEmpty() {
		return count <= 0;
	}

	public int size() {
		return count;
	}

	public int capacity() {
		return Math.min(keys.length, values.length);
	}

	@SuppressWarnings("unchecked")
	public K getKey(int idx) {
		return (K)keys[idx];
	}

	@SuppressWarnings("unchecked")
	public V getValue(int idx) {
		return (V)values[idx];
	}

	public void setKey(int idx, K key) {
		keys[idx] = key;
	}

	public void setValue(int idx, V value) {
		values[idx] = value;
	}

	public void clear() {
		count = 0;
	}

	public void reset() {
		keys = EMPTY;
		values = EMPTY;
		count = 0;
	}

	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public K @NotNull [] toKeyArray(K @NotNull [] a) {
		if (a.length < count)
			return (K[])Arrays.copyOf(keys, count, a.getClass());
		System.arraycopy(keys, 0, a, 0, count);
		return a;
	}

	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public V @NotNull [] toValueArray(V @NotNull [] a) {
		if (a.length < count)
			return (V[])Arrays.copyOf(values, count, a.getClass());
		System.arraycopy(values, 0, a, 0, count);
		return a;
	}

	public @NotNull KVList<K, V> wraps(K @NotNull [] keys, V @NotNull [] values, int count) {
		this.keys = keys;
		this.values = values;
		this.count = Math.min(Math.min(keys.length, values.length), Math.max(count, 0));
		return this;
	}

	public @NotNull KVList<K, V> wraps(K @NotNull [] keys, V @NotNull [] values) {
		this.keys = keys;
		this.values = values;
		this.count = Math.min(keys.length, values.length);
		return this;
	}

	public void shrink(int count) {
		Object[] buffer;
		int n = this.count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count < (buffer = keys).length) {
			Object[] buf = new Object[count];
			System.arraycopy(buffer, 0, buf, 0, n);
			keys = buf;
		}
		if (count < (buffer = values).length) {
			Object[] buf = new Object[count];
			System.arraycopy(buffer, 0, buf, 0, n);
			values = buf;
		}
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		int cap;
		for (cap = DEFAULT_SIZE; count > cap; cap <<= 1) {
			// empty
		}
		Object[] buffer = keys;
		if (count > buffer.length) {
			Object[] buf = new Object[cap];
			int n = this.count;
			if (n > 0)
				System.arraycopy(buffer, 0, buf, 0, n);
			keys = buf;
		}
		buffer = values;
		if (count > buffer.length) {
			Object[] buf = new Object[cap];
			int n = this.count;
			if (n > 0)
				System.arraycopy(buffer, 0, buf, 0, n);
			values = buf;
		}
	}

	public void reserveSpace(int count) {
		int cap;
		for (cap = 8; count > cap; cap <<= 1) {
			// empty
		}
		if (count > keys.length)
			keys = new Object[cap];
		if (count > values.length)
			values = new Object[cap];
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		this.count = count;
	}

	public void replace(K @NotNull [] keys, V @NotNull [] values, int fromIdx, int count) {
		if (count <= 0) {
			this.count = 0;
			return;
		}
		int len = Math.min(keys.length, values.length);
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len) {
			this.count = 0;
			return;
		}
		if (count > (len -= fromIdx))
			count = len;
		reserveSpace(count);
		System.arraycopy(keys, fromIdx, this.keys, 0, count);
		System.arraycopy(values, fromIdx, this.values, 0, count);
		this.count = count;
	}

	public void replace(K @NotNull [] keys, V @NotNull [] values) {
		replace(keys, values, 0, Math.min(keys.length, values.length));
	}

	@SuppressWarnings("unchecked")
	public void replace(@NotNull KVList<K, V> kvl) {
		replace((K[])kvl.keys, (V[])kvl.values, 0, kvl.count);
	}

	public void swap(@NotNull KVList<K, V> kvl) {
		int count = this.count;
		this.count = kvl.count;
		kvl.count = count;
		Object[] buf = kvl.keys;
		kvl.keys = keys;
		keys = buf;
		buf = kvl.values;
		kvl.values = values;
		values = buf;
	}

	public @NotNull KVList<K, V> add(K key, V value) {
		int n = count;
		int nNew = n + 1;
		reserve(nNew);
		keys[n] = key;
		values[n] = value;
		count = nNew;
		return this;
	}

	public @NotNull KVList<K, V> addAll(K @NotNull [] keys, V @NotNull [] values, int fromIdx, int count) {
		if (count <= 0)
			return this;
		int len = Math.min(keys.length, values.length);
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len)
			return this;
		if (count > (len -= fromIdx))
			count = len;
		int n = this.count;
		reserve(n + count);
		System.arraycopy(keys, fromIdx, this.keys, n, count);
		System.arraycopy(values, fromIdx, this.values, n, count);
		this.count = n + count;
		return this;
	}

	public @NotNull KVList<K, V> addAll(K @NotNull [] keys, V @NotNull [] values) {
		return addAll(keys, values, 0, Math.min(keys.length, values.length));
	}

	@SuppressWarnings("unchecked")
	public @NotNull KVList<K, V> addAll(@NotNull KVList<K, V> kvl) {
		return addAll((K[])kvl.keys, (V[])kvl.values, 0, kvl.count);
	}

	public @NotNull KVList<K, V> addAll(@NotNull Collection<K> keys, @NotNull Collection<V> values) {
		int n = count;
		int s = Math.min(keys.size(), values.size());
		reserve(n + s);
		Object[] buf = this.keys;
		for (K k : keys)
			buf[n++] = k;
		buf = this.values;
		n = count;
		for (V v : values)
			buf[n++] = v;
		count = n;
		return this;
	}

	@SuppressWarnings("unchecked")
	public @NotNull void addAllTo(@NotNull Collection<K> keys, @NotNull Collection<V> values) {
		Object[] ks = this.keys;
		Object[] vs = this.values;
		for (int i = 0, n = count; i < n; i++) {
			keys.add((K)ks[i]);
			values.add((V)vs[i]);
		}
	}

	public @NotNull KVList<K, V> insert(int fromIdx, K key, V value) {
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(key, value);
		reserve(n + 1);
		Object[] buf = keys;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = key;
		buf = values;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = value;
		count = n + 1;
		return this;
	}

	public @NotNull KVList<K, V> insert(int fromIdx, K @NotNull [] keys, V @NotNull [] values, int idx, int count) {
		int n = this.count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return addAll(keys, values, idx, count);
		if (count <= 0)
			return this;
		int len = Math.min(keys.length, values.length);
		if (idx < 0)
			idx = 0;
		if (idx >= len)
			return this;
		if (count > (len -= idx))
			count = len;
		reserve(n + count);
		Object[] buf = this.keys;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(keys, idx, buf, fromIdx, count);
		buf = this.values;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(values, idx, buf, fromIdx, count);
		this.count = n + count;
		return this;
	}

	public @NotNull KVList<K, V> insert(int fromIdx, K @NotNull [] keys, V @NotNull [] values) {
		return insert(fromIdx, keys, values, 0, Math.min(keys.length, values.length));
	}

	@SuppressWarnings("unchecked")
	public @NotNull KVList<K, V> insert(int fromIdx, @NotNull KVList<K, V> kvl) {
		return insert(fromIdx, (K[])kvl.keys, (V[])kvl.values, 0, kvl.count);
	}

	public @NotNull KVList<K, V> remove(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			if (idx != lastIdx) {
				System.arraycopy(keys, idx + 1, keys, idx, lastIdx - idx);
				System.arraycopy(values, idx + 1, values, idx, lastIdx - idx);
			}
		}
		return this;
	}

	public @NotNull KVList<K, V> removeAndExchangeLast(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			keys[idx] = keys[lastIdx];
			values[idx] = values[lastIdx];
		}
		return this;
	}

	public @NotNull KVList<K, V> erase(int fromIdx, int toIdx) {
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n || fromIdx >= toIdx)
			return this;
		if (toIdx >= n)
			count = fromIdx;
		else {
			System.arraycopy(keys, toIdx, keys, fromIdx, n -= toIdx);
			System.arraycopy(values, toIdx, values, fromIdx, n);
			count = n + fromIdx;
		}
		return this;
	}

	public @NotNull KVList<K, V> eraseFront(int count) {
		int n = this.count;
		if (count >= n)
			this.count = 0;
		else if (count > 0) {
			System.arraycopy(keys, count, keys, 0, n -= count);
			System.arraycopy(values, count, values, 0, n);
			this.count = n;
		}
		return this;
	}

	public int indexOfKey(K key) {
		return indexOfKey(key, 0);
	}

	public int indexOfKey(K key, int fromIdx) {
		Object[] buf = keys;
		int n = count;
		for (int i = fromIdx; i < n; i++) {
			if (Objects.equals(buf[i], key))
				return i;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull KVList<K, V> clone() {
		return new KVList<>(this);
	}

	@Override
	public int hashCode() {
		Object[] ks = keys;
		Object[] vs = values;
		int n = count;
		long result = n;
		if (n <= 32) {
			for (int i = 0; i < n; i++)
				result ^= Objects.hashCode(ks[i]) ^ Objects.hashCode(vs[i]);
		} else {
			int i;
			for (i = 0; i < 16; i++)
				result ^= Objects.hashCode(ks[i]) ^ Objects.hashCode(vs[i]);
			for (i = n - 16; i < n; i++)
				result ^= Objects.hashCode(ks[i]) ^ Objects.hashCode(vs[i]);
		}
		return (int)result;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (!(o instanceof KVList))
			return false;
		KVList<?, ?> kvl = (KVList<?, ?>)o;
		int n = count;
		if (n != kvl.count)
			return false;
		Object[] ks = keys;
		Object[] vs = values;
		Object[] oks = kvl.keys;
		Object[] ovs = kvl.values;
		for (int i = 0; i < n; i++) {
			if (!Objects.equals(ks[i], oks[i]) || !Objects.equals(vs[i], ovs[i]))
				return false;
		}
		return true;
	}

	public boolean equals(@Nullable KVList<K, V> kvl) {
		if (kvl == this)
			return true;
		if (kvl == null)
			return false;
		int n = count;
		if (n != kvl.count)
			return false;
		Object[] ks = keys;
		Object[] vs = values;
		Object[] oks = kvl.keys;
		Object[] ovs = kvl.values;
		for (int i = 0; i < n; i++) {
			if (!Objects.equals(ks[i], oks[i]) || !Objects.equals(vs[i], ovs[i]))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void foreach(@NotNull BiConsumer<K, V> consumer) {
		Object[] ks = keys;
		Object[] vs = values;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept((K)ks[i], (V)vs[i]);
	}

	@SuppressWarnings("unchecked")
	public void foreachKey(@NotNull Consumer<K> consumer) {
		Object[] buf = keys;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept((K)buf[i]);
	}

	@SuppressWarnings("unchecked")
	public void foreachValue(@NotNull Consumer<V> consumer) {
		Object[] buf = values;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept((V)buf[i]);
	}

	@SuppressWarnings("unchecked")
	public boolean foreachPred(@NotNull BiPredicate<K, V> predicate) {
		Object[] ks = keys;
		Object[] vs = values;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test((K)ks[i], (V)vs[i]))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean foreachKeyPred(@NotNull Predicate<K> predicate) {
		Object[] buf = keys;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test((K)buf[i]))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean foreachValuePred(@NotNull Predicate<V> predicate) {
		Object[] buf = values;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test((V)buf[i]))
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder dump(@NotNull StringBuilder sb) {
		sb.append('[');
		Object[] ks = keys;
		Object[] vs = values;
		int n = count;
		if (n > 0) {
			for (int i = 0; ; ) {
				sb.append('(').append(ks[i]).append(',').append(vs[i]).append(')');
				if (++i >= n)
					break;
				sb.append(',');
			}
		}
		return sb.append(']');
	}

	public @NotNull String dump() {
		int n = count;
		return n > 0 ? dump(new StringBuilder(n * 16)).toString() : "[]";
	}

	@Override
	public @NotNull String toString() {
		return "[" + count + "/" + capacity() + "]";
	}
}
