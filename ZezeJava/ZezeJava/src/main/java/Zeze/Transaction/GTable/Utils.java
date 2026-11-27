package Zeze.Transaction.GTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

import static java.lang.Math.ceil;
import static java.util.logging.Level.WARNING;

/** {@code GTable} 内部集合视图、断言与迭代器工具类。 */
public class Utils {
	public static <T> boolean addAll(Collection<T> addTo, Iterator<? extends T> iterator) {
		checkNotNull(addTo);
		checkNotNull(iterator);
		boolean wasModified = false;
		while (iterator.hasNext()) {
			wasModified |= addTo.add(iterator.next());
		}
		return wasModified;
	}

	public static <E> ArrayList<E> newArrayList() {
		return new ArrayList<>();
	}

	public static <E> ArrayList<E> newArrayList(
			Iterator<? extends E> elements) {
		ArrayList<E> list = newArrayList();
		addAll(list, elements);
		return list;
	}

	public static boolean removeAll(Iterator<?> removeFrom, Collection<?> elementsToRemove) {
		checkNotNull(elementsToRemove);
		boolean result = false;
		while (removeFrom.hasNext()) {
			if (elementsToRemove.contains(removeFrom.next())) {
				removeFrom.remove();
				result = true;
			}
		}
		return result;
	}

	public static int size(Iterator<?> iterator) {
		long count = 0L;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return saturatedCast(count);
	}

	public static int saturatedCast(long value) {
		if (value > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (value < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) value;
	}

	public static <T> T requireNonNull(@Nullable T obj) {
		if (obj == null)
			throw new NullPointerException();
		return obj;
	}
	// Guava 的空数组迭代器哨兵也走此强制转换；不能把它改成运行时非空检查。
	@SuppressWarnings("DataFlowIssue")
	static <T> T uncheckedCastNullableTToT(@Nullable T t) {
		return t;
	}

	public static void checkState(boolean expression, @Nullable Object errorMessage) {
		if (!expression) {
			throw new IllegalStateException(String.valueOf(errorMessage));
		}
	}

	public static void checkState(boolean expression) {
		if (!expression) {
			throw new IllegalStateException();
		}
	}

	private static class InPredicate<T> implements Predicate<T>, Serializable {
		private final Collection<?> target;

		private InPredicate(Collection<?> target) {
			this.target = checkNotNull(target);
		}

		@Override
		public boolean apply(T t) {
			try {
				return target.contains(t);
			} catch (NullPointerException | ClassCastException e) {
				return false;
			}
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (obj instanceof InPredicate<?> that) {
				return target.equals(that.target);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return target.hashCode();
		}

		@Override
		public String toString() {
			return "Predicates.in(" + target + ")";
		}

		@Serial private static final long serialVersionUID = 0;
	}

	public static <T> Predicate<T> in(Collection<? extends T> target) {
		return new InPredicate<>(target);
	}

	private static class IsEqualToPredicate implements Predicate<Object>, Serializable {
		private final Object target;

		private IsEqualToPredicate(Object target) {
			this.target = target;
		}

		@Override
		public boolean apply(@Nullable Object o) {
			return target.equals(o);
		}

		@Override
		public int hashCode() {
			return target.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (obj instanceof IsEqualToPredicate that) {
				return target.equals(that.target);
			}
			return false;
		}

		@Override
		public String toString() {
			return "Predicates.equalTo(" + target + ")";
		}

		@Serial private static final long serialVersionUID = 0;

		@SuppressWarnings("unchecked")
		<T> Predicate<T> withNarrowedType() {
			return (Predicate<T>) this;
		}
	}

	public static <T> Predicate<T> isNull() {
		return ObjectPredicate.IS_NULL.withNarrowedType();
	}

	public static <T> Predicate<T> equalTo(@Nullable T target) {
		return (target == null)
				? isNull()
				: new IsEqualToPredicate(target).withNarrowedType();
	}

	private static class NotPredicate<T> implements Predicate<T>, Serializable {
		final Predicate<T> predicate;

		NotPredicate(Predicate<T> predicate) {
			this.predicate = checkNotNull(predicate);
		}

		@Override
		public boolean apply(T t) {
			return !predicate.apply(t);
		}

		@Override
		public int hashCode() {
			return ~predicate.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (obj instanceof NotPredicate<?> that) {
				return predicate.equals(that.predicate);
			}
			return false;
		}

		@Override
		public String toString() {
			return "Predicates.not(" + predicate + ")";
		}

		@Serial private static final long serialVersionUID = 0;
	}

	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return new NotPredicate<>(predicate);
	}

	enum ObjectPredicate implements Predicate<Object> {
		ALWAYS_TRUE {
			@Override
			public boolean apply(@Nullable Object o) {
				return true;
			}

			@Override
			public String toString() {
				return "Predicates.alwaysTrue()";
			}
		},
		ALWAYS_FALSE {
			@Override
			public boolean apply(@Nullable Object o) {
				return false;
			}

			@Override
			public String toString() {
				return "Predicates.alwaysFalse()";
			}
		},
		IS_NULL {
			@Override
			public boolean apply(@Nullable Object o) {
				return o == null;
			}

			@Override
			public String toString() {
				return "Predicates.isNull()";
			}
		},
		NOT_NULL {
			@Override
			public boolean apply(@Nullable Object o) {
				return o != null;
			}

			@Override
			public String toString() {
				return "Predicates.notNull()";
			}
		};

		@SuppressWarnings("unchecked")
		<T> Predicate<T> withNarrowedType() {
			return (Predicate<T>) this;
		}
	}

	public static <T> Predicate<T> alwaysTrue() {
		return ObjectPredicate.ALWAYS_TRUE.withNarrowedType();
	}

	public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
		if (!expression) {
			throw new IllegalArgumentException(String.valueOf(errorMessage));
		}
	}
	private static String lenientToString(@Nullable Object o) {
		if (o == null) {
			return "null";
		}
		try {
			return o.toString();
		} catch (Exception e) {
			String objectToString =
					o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
			Logger.getLogger("com.google.common.base.Strings")
					.log(WARNING, "Exception during lenientFormat for " + objectToString, e);
			return "<" + objectToString + " threw " + e.getClass().getName() + ">";
		}
	}

	public static String lenientFormat(
			@Nullable String template, @Nullable Object @Nullable ... args) {
		template = String.valueOf(template);

		if (args == null) {
			args = new Object[] {"(Object[])null"};
		} else {
			// 复制数组，避免修改调用方数组或向协变数组写入 String 抛出 ArrayStoreException。
			args = Arrays.copyOf(args, args.length, Object[].class);
			for (int i = 0; i < args.length; i++) {
				args[i] = lenientToString(args[i]);
			}
		}

		StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
		int templateStart = 0;
		int i = 0;
		while (i < args.length) {
			int placeholderStart = template.indexOf("%s", templateStart);
			if (placeholderStart == -1) {
				break;
			}
			builder.append(template, templateStart, placeholderStart);
			builder.append(args[i++]);
			templateStart = placeholderStart + 2;
		}
		builder.append(template, templateStart, template.length());

		if (i < args.length) {
			builder.append(" [");
			builder.append(args[i++]);
			while (i < args.length) {
				builder.append(", ");
				builder.append(args[i++]);
			}
			builder.append(']');
		}

		return builder.toString();
	}

	public static <K, V> Map.Entry<K, V> immutableEntry(K key, V value) {
		return new ImmutableEntry<>(key, value);
	}

	public static boolean equal(@Nullable Object a, @Nullable Object b) {
		return Objects.equals(a, b);
	}

	private static String badPositionIndex(int index, int size, String desc) {
		if (index < 0) {
			return lenientFormat("%s (%s) must not be negative", desc, index);
		}
		if (size < 0) {
			throw new IllegalArgumentException("negative size: " + size);
		}
		return lenientFormat("%s (%s) must not be greater than size (%s)", desc, index, size);
	}

	public static int checkPositionIndex(int index, int size, String desc) {
		if (index < 0 || index > size) {
			throw new IndexOutOfBoundsException(badPositionIndex(index, size, desc));
		}
		return index;
	}

	public static int checkPositionIndex(int index, int size) {
		return checkPositionIndex(index, size, "index");
	}

	static boolean safeRemove(Collection<?> collection, Object object) {
		checkNotNull(collection);

		try {
			return collection.remove(object);
		} catch (NullPointerException | ClassCastException var3) {
			return false;
		}
	}

	@Nullable
	static <V> V safeRemove(Map<?, V> map, @Nullable Object key) {
		checkNotNull(map);
		try {
			return map.remove(key);
		} catch (ClassCastException | NullPointerException e) {
			return null;
		}
	}

	static void clear(Iterator<?> iterator) {
		checkNotNull(iterator);
		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	static boolean equalsImpl(Table<?, ?, ?> table, @Nullable Object obj) {
		if (obj == table) {
			return true;
		}
		if (obj instanceof Table<?, ?, ?> that) {
			return table.cellSet().equals(that.cellSet());
		}
		return false;
	}

	static boolean safeContainsKey(Map<?, ?> map, @Nullable Object key) {
		checkNotNull(map);

		try {
			return map.containsKey(key);
		} catch (NullPointerException | ClassCastException var3) {
			return false;
		}
	}

	static <V> @Nullable V safeGet(Map<?, V> map, @Nullable Object key) {
		checkNotNull(map);

		try {
			return map.get(key);
		} catch (NullPointerException | ClassCastException var3) {
			return null;
		}
	}

	static <K, V>
	Iterator<Map.Entry<K, V>> asMapEntryIterator(Set<K> set, final Function<? super K, V> function) {
		return new TransformedIterator<>(set.iterator()) {
			@Override
			Map.Entry<K, V> transform(final K key) {
				return immutableEntry(key, function.apply(key));
			}
		};
	}

	static boolean safeContains(Collection<?> collection, @Nullable Object object) {
		checkNotNull(collection);
		try {
			return collection.contains(object);
		} catch (ClassCastException | NullPointerException e) {
			return false;
		}
	}

	static <T> UnmodifiableIterator<T> emptyIterator() {
		return emptyListIterator();
	}

	@SuppressWarnings("unchecked")
	static <T> UnmodifiableListIterator<T> emptyListIterator() {
		return (UnmodifiableListIterator<T>) ArrayItr.EMPTY;
	}

	private static final class ArrayItr<T> extends AbstractIndexedListIterator<T> {
		static final UnmodifiableListIterator<Object> EMPTY = new ArrayItr<>(new Object[0], 0, 0, 0);

		private final T[] array;
		private final int offset;

		ArrayItr(T[] array, int offset, int length, int index) {
			super(length, index);
			this.array = array;
			this.offset = offset;
		}

		@Override
		protected T get(int index) {
			return array[offset + index];
		}
	}

	abstract static class ViewCachingAbstractMap<K, V> extends AbstractMap<K, V> {
		/** 首次调用 {@link #entrySet()} 时创建并缓存 entrySet 视图。 */
		abstract Set<Entry<K, V>> createEntrySet();

		@Nullable private transient Set<Entry<K, V>> entrySet;

		@Override
		public Set<Entry<K, V>> entrySet() {
			Set<Entry<K, V>> result = entrySet;
			return (result == null) ? entrySet = createEntrySet() : result;
		}

		@Nullable private transient Set<K> keySet;

		@Override
		public Set<K> keySet() {
			Set<K> result = keySet;
			return (result == null) ? keySet = createKeySet() : result;
		}

		Set<K> createKeySet() {
			return new KeySet<>(this);
		}

		@Nullable private transient Collection<V> values;

		@Override
		public Collection<V> values() {
			Collection<V> result = values;
			return (result == null) ? values = createValues() : result;
		}

		Collection<V> createValues() {
			return new Values<>(this);
		}
	}

	static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
		checkNotNull(collection);
		// 仅在 collection 为更大的 Set 时遍历本集合，避免 AbstractSet.removeAll(List) 退化为 O(n²)。
		if (collection instanceof Set && collection.size() > set.size()) {
			return removeAll(set.iterator(), collection);
		}
		return removeAllImpl(set, collection.iterator());
	}

	static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
		boolean changed = false;
		while (iterator.hasNext()) {
			changed |= set.remove(iterator.next());
		}
		return changed;
	}

	@SuppressWarnings("unchecked")
	static <T> Iterator<T> emptyModifiableIterator() {
		return (Iterator<T>) EmptyModifiableIterator.INSTANCE;
	}

	public static <T> T checkNotNull(@Nullable T reference) {
		if (reference == null) {
			throw new NullPointerException();
		}
		return reference;
	}

	private static class CompositionPredicate<A, B> implements Predicate<A>, Serializable {
		final Predicate<B> p;
		final Function<A, ? extends B> f;

		private CompositionPredicate(Predicate<B> p, Function<A, ? extends B> f) {
			this.p = Utils.checkNotNull(p);
			this.f = Utils.checkNotNull(f);
		}

		@Override
		public boolean apply(A a) {
			return p.apply(f.apply(a));
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (obj instanceof CompositionPredicate<?, ?> that) {
				return f.equals(that.f) && p.equals(that.p);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return f.hashCode() ^ p.hashCode();
		}

		@Override
		public String toString() {
			return p + "(" + f + ")";
		}

		@Serial private static final long serialVersionUID = 0;
	}

	public static <A, B> Predicate<A> compose(Predicate<B> predicate, Function<A, ? extends B> function) {
		return new CompositionPredicate<>(predicate, function);
	}

	static <V> Predicate<Map.Entry<?, V>> valuePredicateOnEntries(Predicate<? super V> valuePredicate) {
		return compose(valuePredicate, Utils.valueFunction());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static <V> Function<Map.Entry<?, V>, V> valueFunction() {
		return (Function) EntryFunction.VALUE;
	}

	private enum EmptyModifiableIterator implements Iterator<Object> {
		INSTANCE;

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			//noinspection DataFlowIssue
			checkRemove(false);
		}
	}

	static void checkRemove(boolean canRemove) {
		checkState(canRemove, "no calls to next() since the last call to remove()");
	}

	abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
		@Override
		public boolean removeAll(Collection<?> c) {
			return removeAllImpl(this, c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return super.retainAll(checkNotNull(c));
		}
	}

	static <K, V> Iterator<K> keyIterator(Iterator<Map.Entry<K, V>> entryIterator) {
		return new TransformedIterator<>(entryIterator) {
			@Override
			K transform(Map.Entry<K, V> entry) {
				return entry.getKey();
			}
		};
	}

	static class KeySet<K, V>
			extends ImprovedAbstractSet<K> {
		final Map<K, V> map;

		KeySet(Map<K, V> map) {
			this.map = checkNotNull(map);
		}

		Map<K, V> map() {
			return map;
		}

		@Override
		public Iterator<K> iterator() {
			return keyIterator(map().entrySet().iterator());
		}

		@Override
		public void forEach(Consumer<? super K> action) {
			checkNotNull(action);
			// 直接走 map.forEach，避免为迭代分配临时 Entry。
			map.forEach((k, v) -> action.accept(k));
		}

		@Override
		public int size() {
			return map().size();
		}

		@Override
		public boolean isEmpty() {
			return map().isEmpty();
		}

		@Override
		public boolean contains(@Nullable Object o) {
			//noinspection SuspiciousMethodCalls
			return map().containsKey(o);
		}

		@Override
		public boolean remove(@Nullable Object o) {
			if (contains(o)) {
				map().remove(o);
				return true;
			}
			return false;
		}

		@Override
		public void clear() {
			map().clear();
		}
	}

	static <K, V> Iterator<V> valueIterator(Iterator<Map.Entry<K, V>> entryIterator) {
		return new TransformedIterator<>(entryIterator) {
			@Override
			V transform(Map.Entry<K, V> entry) {
				return entry.getValue();
			}
		};
	}

	public static <E> HashSet<E> newHashSet() {
		return new HashSet<>();
	}

	static int checkNonnegative(int value, String name) {
		if (value < 0) {
			throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
		}
		return value;
	}

	public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

	static int capacity(int expectedSize) {
		if (expectedSize < 3) {
			checkNonnegative(expectedSize, "expectedSize");
			return expectedSize + 1;
		}
		if (expectedSize < MAX_POWER_OF_TWO) {
			// 按默认负载因子 0.75 反算初始容量，避免插入 expectedSize 个元素时触发扩容。
			return (int) ceil(expectedSize / 0.75);
		}
		return Integer.MAX_VALUE;
	}

	public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
		return new HashSet<>(capacity(expectedSize));
	}

	static class Values<K, V> extends AbstractCollection<V> {
		final Map<K, V> map;

		Values(Map<K, V> map) {
			this.map = checkNotNull(map);
		}

		final Map<K, V> map() {
			return map;
		}

		@Override
		public Iterator<V> iterator() {
			return valueIterator(map().entrySet().iterator());
		}

		@Override
		public void forEach(Consumer<? super V> action) {
			checkNotNull(action);
			// 直接走 map.forEach，避免为迭代分配临时 Entry。
			map.forEach((k, v) -> action.accept(v));
		}

		@Override
		public boolean remove(@Nullable Object o) {
			try {
				return super.remove(o);
			} catch (UnsupportedOperationException e) {
				for (Map.Entry<K, V> entry : map().entrySet()) {
					if (equal(o, entry.getValue())) {
						map().remove(entry.getKey());
						return true;
					}
				}
				return false;
			}
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			try {
				return super.removeAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				Set<K> toRemove = newHashSet();
				for (Map.Entry<K, V> entry : map().entrySet()) {
					if (c.contains(entry.getValue())) {
						toRemove.add(entry.getKey());
					}
				}
				return map().keySet().removeAll(toRemove);
			}
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			try {
				return super.retainAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				Set<K> toRetain = newHashSet();
				for (Map.Entry<K, V> entry : map().entrySet()) {
					if (c.contains(entry.getValue())) {
						toRetain.add(entry.getKey());
					}
				}
				return map().keySet().retainAll(toRetain);
			}
		}

		@Override
		public int size() {
			return map().size();
		}

		@Override
		public boolean isEmpty() {
			return map().isEmpty();
		}

		@Override
		public boolean contains(@Nullable Object o) {
			//noinspection SuspiciousMethodCalls
			return map().containsValue(o);
		}

		@Override
		public void clear() {
			map().clear();
		}
	}

	abstract static class EntrySet<K, V> extends ImprovedAbstractSet<Map.Entry<K, V>> {
		abstract Map<K, V> map();

		@Override
		public int size() {
			return map().size();
		}

		@Override
		public void clear() {
			map().clear();
		}

		@Override
		public boolean contains(@Nullable Object o) {
			if (o instanceof Map.Entry<?, ?> entry) {
				Object key = entry.getKey();
				V value = Utils.safeGet(map(), key);
				//noinspection SuspiciousMethodCalls
				return equal(value, entry.getValue()) && (value != null || map().containsKey(key));
			}
			return false;
		}

		@Override
		public boolean isEmpty() {
			return map().isEmpty();
		}

		@Override
		public boolean remove(@Nullable Object o) {
			if (contains(o) && o instanceof Map.Entry<?, ?> entry) {
				return map().remove(entry.getKey()) != null;
			}
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			try {
				return super.removeAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				// 迭代器不支持 remove 时回退处理。
				return removeAllImpl(this, c.iterator());
			}
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			try {
				return super.retainAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				// 迭代器不支持 remove 时回退处理。
				Set<Object> keys = newHashSetWithExpectedSize(c.size());
				for (Object o : c) {
					if (contains(o) && o instanceof Map.Entry<?, ?> entry) {
						keys.add(entry.getKey());
					}
				}
				return map().keySet().retainAll(keys);
			}
		}
	}

	abstract static class IteratorBasedAbstractMap<K, V> extends AbstractMap<K, V> {
		@Override
		public abstract int size();

		abstract Iterator<Entry<K, V>> entryIterator();

		Spliterator<Entry<K, V>> entrySpliterator() {
			return Spliterators.spliterator(
					entryIterator(), size(), Spliterator.SIZED | Spliterator.DISTINCT);
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return new EntrySet<>() {
				@Override
				Map<K, V> map() {
					return IteratorBasedAbstractMap.this;
				}

				@Override
				public Iterator<Entry<K, V>> iterator() {
					return entryIterator();
				}

				@Override
				public Spliterator<Entry<K, V>> spliterator() {
					return entrySpliterator();
				}

				@Override
				public void forEach(Consumer<? super Entry<K, V>> action) {
					forEachEntry(action);
				}
			};
		}

		void forEachEntry(Consumer<? super Entry<K, V>> action) {
			entryIterator().forEachRemaining(action);
		}

		@Override
		public void clear() {
			Utils.clear(entryIterator());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static <K> Function<Map.Entry<K, ?>, K> keyFunction() {
		return (Function) EntryFunction.KEY;
	}

	private enum EntryFunction implements Function<Map.Entry<?, ?>, Object> {
		KEY {
			@Override
			@Nullable
			public Object apply(Map.Entry<?, ?> entry) {
				return entry.getKey();
			}
		},
		VALUE {
			@Override
			@Nullable
			public Object apply(Map.Entry<?, ?> entry) {
				return entry.getValue();
			}
		}
	}

	static <K> Predicate<Map.Entry<K, ?>> keyPredicateOnEntries(
			Predicate<? super K> keyPredicate) {
		return compose(keyPredicate, Utils.keyFunction());
	}

	abstract static class AbstractCell<R, C, V> implements Table.Cell<R, C, V> {
		// 供序列化使用。
		AbstractCell() {}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof Table.Cell<?, ?, ?> other) {
				return equal(getRowKey(), other.getRowKey())
						&& equal(getColumnKey(), other.getColumnKey())
						&& equal(getValue(), other.getValue());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Utils.hashCode(getRowKey(), getColumnKey(), getValue());
		}

		@Override
		public String toString() {
			return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
		}
	}

	public static int hashCode(@Nullable Object... objects) {
		return Arrays.hashCode(objects);
	}
	/** 返回指定行键、列键和值的不可变（可序列化）单元格。 */
	public static <R, C, V> Table.Cell<R, C, V> immutableCell(R rowKey, C columnKey, V value) {
		return new ImmutableCell<>(rowKey, columnKey, value);
	}

	static final class ImmutableCell<R, C, V> extends AbstractCell<R, C, V> implements Serializable {
		private final R rowKey;
		private final C columnKey;
		private final V value;

		ImmutableCell(R rowKey, C columnKey, V value) {
			this.rowKey = rowKey;
			this.columnKey = columnKey;
			this.value = value;
		}

		@Override
		public R getRowKey() {
			return rowKey;
		}

		@Override
		public C getColumnKey() {
			return columnKey;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Serial private static final long serialVersionUID = 0;
	}
}
