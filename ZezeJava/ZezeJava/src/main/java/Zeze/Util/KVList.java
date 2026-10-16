package Zeze.Util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

// 平行数组的键值对列表（keys/values 同下标成对），可克隆
public class KVList<K extends @Nullable Object, V extends @Nullable Object> implements Cloneable {
	public static final @Nullable Object[] EMPTY = new Object[0];
	public static final int DEFAULT_SIZE = 8;

	private @Nullable Object [] keys = EMPTY;
	private @Nullable Object [] values = EMPTY;
	private int count;

	// Called only within count; nullable backing slots outside the live prefix do not constrain K or V.
	private K keyForPresentIndex(@Nullable K key) {
		return key;
	}

	private V valueForPresentIndex(@Nullable V value) {
		return value;
	}

	public static <K extends @Nullable Object, V extends @Nullable Object> KVList<K, V> wrap(@Nullable K [] keys, @Nullable V [] values, int count) {
		KVList<K, V> kvl = new KVList<>();
		kvl.keys = keys;
		kvl.values = values;
		kvl.count = Math.clamp(count, 0, Math.min(keys.length, values.length));
		return kvl;
	}

	public static <K extends @Nullable Object, V extends @Nullable Object> KVList<K, V> wrap(@Nullable K [] keys, @Nullable V [] values) {
		KVList<K, V> kvl = new KVList<>();
		kvl.keys = keys;
		kvl.values = values;
		kvl.count = Math.min(keys.length, values.length);
		return kvl;
	}

	public static <K extends @Nullable Object, V extends @Nullable Object> KVList<K, V> createSpace(int count) {
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
	public KVList(KVList<K, V> kvl) {
		replace(kvl);
	}

	public KVList(@Nullable K [] keys, @Nullable V [] values) {
		replace(keys, values);
	}

	public KVList(@Nullable K [] keys, @Nullable V [] values, int fromIdx, int count) {
		replace(keys, values, fromIdx, count);
	}

	public @Nullable Object[] keys() {
		return keys;
	}

	public @Nullable Object[] values() {
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
		return keyForPresentIndex((K)keys[idx]);
	}

	@SuppressWarnings("unchecked")
	public V getValue(int idx) {
		return valueForPresentIndex((V)values[idx]);
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
	public K [] toKeyArray(K [] a) {
		if (a.length < count)
			return (K[])Arrays.copyOf(keys, count, a.getClass());
		System.arraycopy(keys, 0, a, 0, count);
		return a;
	}

	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public V [] toValueArray(V [] a) {
		if (a.length < count)
			return (V[])Arrays.copyOf(values, count, a.getClass());
		System.arraycopy(values, 0, a, 0, count);
		return a;
	}

	public KVList<K, V> wraps(@Nullable K [] keys, @Nullable V [] values, int count) {
		this.keys = keys;
		this.values = values;
		this.count = Math.clamp(count, 0, Math.min(keys.length, values.length));
		return this;
	}

	public KVList<K, V> wraps(@Nullable K [] keys, @Nullable V [] values) {
		this.keys = keys;
		this.values = values;
		this.count = Math.min(keys.length, values.length);
		return this;
	}

	public void shrink(int count) {
		@Nullable Object[] buffer;
		int n = this.count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count < (buffer = keys).length) {
			@Nullable Object[] buf = new Object[count];
			System.arraycopy(buffer, 0, buf, 0, n);
			keys = buf;
		}
		if (count < (buffer = values).length) {
			@Nullable Object[] buf = new Object[count];
			System.arraycopy(buffer, 0, buf, 0, n);
			values = buf;
		}
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		if (count > keys.length || count > values.length) {
			int cap;
			for (cap = DEFAULT_SIZE; count > cap && cap > 0; cap <<= 1) {
				// empty
			}
			if (cap < 0) // count > 2^30：倍增 cap 溢出为负，原实现死循环
				throw new OutOfMemoryError("KVList reserve count too large: " + count);
			@Nullable Object[] buffer = keys;
			if (count > buffer.length) {
				@Nullable Object[] buf = new Object[cap];
				int n = this.count;
				if (n > 0)
					System.arraycopy(buffer, 0, buf, 0, n);
				keys = buf;
			}
			buffer = values;
			if (count > buffer.length) {
				@Nullable Object[] buf = new Object[cap];
				int n = this.count;
				if (n > 0)
					System.arraycopy(buffer, 0, buf, 0, n);
				values = buf;
			}
		}
	}

	public void reserveSpace(int count) {
		if (count > keys.length || count > values.length) {
			int cap;
			for (cap = 8; count > cap && cap > 0; cap <<= 1) {
				// empty
			}
			if (cap < 0) // 同 reserve：防倍增溢出死循环
				throw new OutOfMemoryError("KVList reserveSpace count too large: " + count);
			if (count > keys.length)
				keys = new Object[cap];
			if (count > values.length)
				values = new Object[cap];
		}
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		this.count = count;
	}

	public void replace(@Nullable K [] keys, @Nullable V [] values, int fromIdx, int count) {
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

	public void replace(@Nullable K [] keys, @Nullable V [] values) {
		replace(keys, values, 0, Math.min(keys.length, values.length));
	}

	@SuppressWarnings("unchecked")
	public void replace(KVList<K, V> kvl) {
		replace((K[])kvl.keys, (V[])kvl.values, 0, kvl.count);
	}

	public void swap(KVList<K, V> kvl) {
		int count = this.count;
		this.count = kvl.count;
		kvl.count = count;
		@Nullable Object[] buf = kvl.keys;
		kvl.keys = keys;
		keys = buf;
		buf = kvl.values;
		kvl.values = values;
		values = buf;
	}

	public KVList<K, V> add(K key, V value) {
		int n = count;
		int nNew = n + 1;
		reserve(nNew);
		keys[n] = key;
		values[n] = value;
		count = nNew;
		return this;
	}

	public KVList<K, V> addAll(@Nullable K [] keys, @Nullable V [] values, int fromIdx, int count) {
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

	public KVList<K, V> addAll(@Nullable K [] keys, @Nullable V [] values) {
		return addAll(keys, values, 0, Math.min(keys.length, values.length));
	}

	@SuppressWarnings("unchecked")
	public KVList<K, V> addAll(KVList<K, V> kvl) {
		return addAll((K[])kvl.keys, (V[])kvl.values, 0, kvl.count);
	}

	public KVList<K, V> addAll(Collection<K> keys, Collection<V> values) {
		int n = count;
		int s = Math.min(keys.size(), values.size());
		reserve(n + s);
		int end = n + s; // 与数组版addAll的min截断语义对齐，集合大小不一致时按s截断，避免越界或错位
		@Nullable Object[] buf = this.keys;
		for (K k : keys) {
			if (n >= end)
				break;
			buf[n++] = k;
		}
		buf = this.values;
		n = count;
		for (V v : values) {
			if (n >= end)
				break;
			buf[n++] = v;
		}
		count = end;
		return this;
	}

	@SuppressWarnings("unchecked")
	public void addAllTo(Collection<K> keys, Collection<V> values) {
		@Nullable Object[] ks = this.keys;
		@Nullable Object[] vs = this.values;
		for (int i = 0, n = count; i < n; i++) {
			keys.add(keyForPresentIndex((K)ks[i]));
			values.add(valueForPresentIndex((V)vs[i]));
		}
	}

	public KVList<K, V> insert(int fromIdx, K key, V value) {
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(key, value);
		reserve(n + 1);
		@Nullable Object[] buf = keys;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = key;
		buf = values;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = value;
		count = n + 1;
		return this;
	}

	public KVList<K, V> insert(int fromIdx, @Nullable K [] keys, @Nullable V [] values, int idx, int count) {
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
		if (keys == this.keys || keys == this.values || values == this.keys || values == this.values) {
			keys = Arrays.copyOfRange(keys, idx, idx + count);
			values = Arrays.copyOfRange(values, idx, idx + count);
			idx = 0;
		}
		reserve(n + count);
		@Nullable Object[] buf = this.keys;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(keys, idx, buf, fromIdx, count);
		buf = this.values;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(values, idx, buf, fromIdx, count);
		this.count = n + count;
		return this;
	}

	public KVList<K, V> insert(int fromIdx, @Nullable K [] keys, @Nullable V [] values) {
		return insert(fromIdx, keys, values, 0, Math.min(keys.length, values.length));
	}

	@SuppressWarnings("unchecked")
	public KVList<K, V> insert(int fromIdx, KVList<K, V> kvl) {
		return insert(fromIdx, (K[])kvl.keys, (V[])kvl.values, 0, kvl.count);
	}

	public KVList<K, V> remove(int idx) {
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

	public KVList<K, V> removeAndExchangeLast(int idx) {
		int lastIdx = count;
		if (Integer.compareUnsigned(idx, lastIdx) < 0) {
			count = --lastIdx;
			keys[idx] = keys[lastIdx];
			values[idx] = values[lastIdx];
		}
		return this;
	}

	public KVList<K, V> erase(int fromIdx, int toIdx) {
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

	public KVList<K, V> eraseFront(int count) {
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
		@Nullable Object[] buf = keys;
		int n = count;
		for (int i = fromIdx; i < n; i++) {
			if (Objects.equals(buf[i], key))
				return i;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public KVList<K, V> clone() {
		return new KVList<>(this);
	}

	@Override
	public int hashCode() {
		@Nullable Object[] ks = keys;
		@Nullable Object[] vs = values;
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
		if (!(o instanceof KVList<?, ?> kvl))
			return false;
		int n = count;
		if (n != kvl.count)
			return false;
		@Nullable Object[] ks = keys;
		@Nullable Object[] vs = values;
		@Nullable Object[] oks = kvl.keys;
		@Nullable Object[] ovs = kvl.values;
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
		@Nullable Object[] ks = keys;
		@Nullable Object[] vs = values;
		@Nullable Object[] oks = kvl.keys;
		@Nullable Object[] ovs = kvl.values;
		for (int i = 0; i < n; i++) {
			if (!Objects.equals(ks[i], oks[i]) || !Objects.equals(vs[i], ovs[i]))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void foreach(BiConsumer<K, V> consumer) {
		@Nullable Object[] ks = keys;
		@Nullable Object[] vs = values;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept(keyForPresentIndex((K)ks[i]), valueForPresentIndex((V)vs[i]));
	}

	@SuppressWarnings("unchecked")
	public void foreachKey(Consumer<K> consumer) {
		@Nullable Object[] buf = keys;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept(keyForPresentIndex((K)buf[i]));
	}

	@SuppressWarnings("unchecked")
	public void foreachValue(Consumer<V> consumer) {
		@Nullable Object[] buf = values;
		int n = count;
		for (int i = 0; i < n; i++)
			consumer.accept(valueForPresentIndex((V)buf[i]));
	}

	@SuppressWarnings("unchecked")
	public boolean foreachPred(BiPredicate<K, V> predicate) {
		@Nullable Object[] ks = keys;
		@Nullable Object[] vs = values;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(keyForPresentIndex((K)ks[i]), valueForPresentIndex((V)vs[i])))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean foreachKeyPred(Predicate<K> predicate) {
		@Nullable Object[] buf = keys;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(keyForPresentIndex((K)buf[i])))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean foreachValuePred(Predicate<V> predicate) {
		@Nullable Object[] buf = values;
		int n = count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(valueForPresentIndex((V)buf[i])))
				return false;
		}
		return true;
	}

	public StringBuilder dump(StringBuilder sb) {
		sb.append('[');
		@Nullable Object[] ks = keys;
		@Nullable Object[] vs = values;
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

	public String dump() {
		int n = count;
		return n > 0 ? dump(new StringBuilder(n * 16)).toString() : "[]";
	}

	@Override
	public String toString() {
		return "[" + count + "/" + capacity() + "]";
	}
}
