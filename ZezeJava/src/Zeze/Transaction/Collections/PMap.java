package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;
import java.util.*;

//C# TO JAVA CONVERTER TODO TASK: The interface type was changed to the closest equivalent Java type, but the methods implemented will need adjustment:
public abstract class PMap<K, V> extends PCollection implements Map<K, V> {
	private final tangible.Func1Param<ImmutableDictionary<K, V>, Log> _logFactory;
	protected ImmutableDictionary<K, V> map;

	public PMap(long logKey, tangible.Func1Param<ImmutableDictionary<K, V>, Log> logFactory) {
		super(logKey);
		this._logFactory = ::logFactory;
		map = ImmutableDictionary<K, V>.Empty;
	}

	public final Log NewLog(ImmutableDictionary<K, V> value) {
		return _logFactory.invoke(value);
	}

	public abstract static class LogV extends Log {
		public ImmutableDictionary<K, V> Value;
		protected LogV(Bean bean, ImmutableDictionary<K, V> value) {
			super(bean);
			Value = value;
		}

		protected final void Commit(PMap<K, V> variable) {
			variable.map = Value;
		}
	}

	protected final ImmutableDictionary<K, V> getData() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return map;
			}
			txn.VerifyRecordAccessed(this, true);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			return tempVar ? log.Value : map;
		}
		else {
			return map;
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Throw expressions are not converted by C# to Java Converter:
//ORIGINAL LINE: [Obsolete("Don't use this, please use Keys2", true)] ICollection<K> IDictionary<K, V>.Keys => throw new NotImplementedException();
    @Deprecated
	private Collection<K> IDictionary<K, V>.Keys -> throw new UnsupportedOperationException();
//C# TO JAVA CONVERTER TODO TASK: Throw expressions are not converted by C# to Java Converter:
//ORIGINAL LINE: [Obsolete("Don't use this, please use Values2", true)] ICollection<V> IDictionary<K, V>.Values => throw new NotImplementedException();
    @Deprecated
	private Collection<V> IDictionary<K, V>.Values -> throw new UnsupportedOperationException();

	public final java.lang.Iterable<K> getKeys() {
		return getData().keySet();
	}

	public final java.lang.Iterable<V> values() {
		return getData().Values;
	}

	public final int size() {
		return getData().Count;
	}

	@Override
	public String toString() {
		return String.format("PMap%1$s", getData());
	}
	public final boolean isReadOnly() {
		return false;
	}

	public abstract V get(K key);
	public abstract void set(K key, V value);
	public abstract void Add(K key, V value);
	public abstract void Add(Map.Entry<K, V> item);
	public abstract void AddRange(java.lang.Iterable<Map.Entry<K, V>> pairs);
	public abstract void SetItem(K key, V value);
	public abstract void SetItems(java.lang.Iterable<Map.Entry<K, V>> items);
	public abstract void Clear();
	public abstract boolean Remove(K key);
	public abstract boolean Remove(Map.Entry<K, V> item);

	public final void CopyTo(Map.Entry<K, V>[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : getData()) {
			array[index++] = e;
		}
	}

	public final boolean contains(Object objectValue) {
		Map.Entry<K, V> item = (Map.Entry<K, V>)objectValue;
		return getData().Contains(item);
	}

	public final boolean containsKey(Object objectKey) {
		K key = (K)objectKey;
		return getData().ContainsKey(key);
	}

	public final boolean TryGetValue(K key, tangible.OutObject<V> value) {
		return getData().TryGetValue(key, value);

	}

	public final Iterator GetEnumerator() {
		if (this instanceof IEnumerable)
			return IEnumerable_GetEnumerator();
		else if (this instanceof IEnumerable)
			return IEnumerable_GetEnumerator();
		else
			throw new UnsupportedOperationException("No interface found.");
	}

	private Iterator IEnumerable_GetEnumerator() {
		return getData().iterator();
	}

	private Iterator<Map.Entry<K, V>> IEnumerable_GetEnumerator() {
		return getData().iterator();
	}

	public final ImmutableDictionary<K, V>.Enumerator GetEnumerator() {
		return getData().iterator();
	}
}