package Zeze.Transaction.Collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Transaction;

public abstract class CollMap<K, V> extends Collection implements Iterable<Map.Entry<K, V>> {
	public org.pcollections.PMap<K, V> _map = org.pcollections.Empty.map();

	public final V get(K key) {
		return getMap().get(key);
	}

	public abstract void add(K key, V value);

	public abstract void put(K key, V value);

	public abstract void remove(K key);

	public abstract void clear();

	public Set<Map.Entry<K, V>> entrySet() {
		return getMap().entrySet();
	}

	protected final org.pcollections.PMap<K, V> getMap() {
		if (isManaged()) {
			if (Transaction.getCurrent() == null)
				return _map;
			var log = Transaction.getCurrent().GetLog(getParent().getObjectId() + getVariableId());
			if (log == null)
				return _map;
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)log;
			return mapLog.getValue();
		}
		return _map;
	}

	public final int size() {
		return getMap().size();
	}

	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		return getMap().entrySet().iterator();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getMap());
		return sb.toString();
	}
}
