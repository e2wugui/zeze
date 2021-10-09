package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;
import java.util.*;

public class PMapReadOnly<K, V, P extends V> implements IReadOnlyDictionary<K, V> {
	private final PMap<K, P> _origin;

	public PMapReadOnly(PMap<K, P> origin) {
		_origin = origin;
	}

	public final V get(K key) {
		return _origin.get(key);
	}

	public final java.lang.Iterable<K> getKeys() {
		return _origin.keySet();
	}

	public final java.lang.Iterable<V> getValues() {
		return (java.lang.Iterable<V>)_origin.values();
	}

	public final int getCount() {
		return _origin.size();
	}

	public final boolean ContainsKey(K key) {
		return _origin.containsKey(key);
	}

	public final Iterator<Map.Entry<K, V>> GetEnumerator() {
		for (var e : _origin) {
//C# TO JAVA CONVERTER TODO TASK: Java does not have an equivalent to the C# 'yield' keyword:
			yield return new Map.Entry<K, V>(e.Key, e.Value);
		}
	}

	public final Iterator GetEnumerator() {
		return ((java.lang.Iterable)_origin).iterator();
	}

	public final boolean TryGetValue(K key, tangible.OutObject<V> value) {
		V cur;
		tangible.OutObject<P> tempOut_cur = new tangible.OutObject<P>();
		if (_origin.TryGetValue(key, tempOut_cur)) {
		cur = tempOut_cur.outArgValue;
			value.outArgValue = cur;
			return true;
		}
	else {
		cur = tempOut_cur.outArgValue;
	}
		value.outArgValue = null;
		return false;
	}
}