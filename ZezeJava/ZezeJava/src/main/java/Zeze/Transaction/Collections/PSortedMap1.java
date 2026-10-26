package Zeze.Transaction.Collections;

import java.util.Map;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;
import org.jspecify.annotations.Nullable;
import org.pcollections.Empty;
import org.pcollections.TreePMap;

/** 事务 SortedMap（1系）：Comparable 键，不可变值按值拷贝记账。 */
public class PSortedMap1<K extends Comparable<K>, V> extends PSortedMap<K, V> {
	protected final SortedMap1Meta<K, V> meta;

	public PSortedMap1(Class<K> keyClass, Class<V> valueClass) {
		meta = SortedMap1Meta.get(keyClass, valueClass);
	}

	public PSortedMap1(SortedMap1Meta<K, V> meta) {
		this.meta = meta;
	}

	public SortedMap1Meta<K, V> getMeta() {
		return meta;
	}

	@Override
	public ByteBuffer encodeKey(K key) {
		var bb = ByteBuffer.Allocate();
		meta.keyEncoder.accept(bb, key);
		return bb;
	}

	@Override
	public K decodeKey(ByteBuffer bb) {
		return meta.keyDecoder.apply(bb);
	}

	@Override
	public @Nullable V put(K key, V value) {
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("null key");
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("null value");

		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			return mapLog.put(key, value);
		}
		var exist = map.get(key);
		map = map.plus(key, value);
		return exist;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return;
		if (m instanceof PSortedMap1)
			m = ((PSortedMap1<? extends K, ? extends V>)m).getMap(); // more stable

		for (var e : m.entrySet()) {
			if (e.getKey() == null)
				throw new IllegalArgumentException("null key");
			if (e.getValue() == null)
				throw new IllegalArgumentException("null value");
		}

		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.putAll(m);
		} else
			map = map.plusAll(m);
	}

	@Override
	public @Nullable V remove(Object key) {
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			//noinspection unchecked
			return mapLog.remove((K)key);
		}
		// noinspection unchecked
		V exist = map.get((K)key);
		map = map.minus(key);
		return exist;
	}

	@Override
	public boolean remove(Entry<K, V> item) {
		K k = item.getKey();
		V v = item.getValue();
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			return mapLog.remove(k, v);
		}
		V exist = map.get(k);
		if (exist != null && exist.equals(v)) {
			map = map.minus(k);
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		if (isEmpty())
			return;
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.clear();
		} else {
			var comparator = map.comparator();
			map = comparator != null ? TreePMap.empty(comparator) : Empty.sortedMap();
		}
	}

	@Override
	public void followerApply(Log _log) {
		//noinspection unchecked
		var log = (LogSortedMap1<K, V>)_log;
		map = map.minusAll(log.getRemoved()).plusAll(log.getReplaced());
	}

	public void assign(PSortedMap1<K, V> pmap) {
		var items = pmap.getMap();
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.clear();
			mapLog.putAll(items);
		} else
			map = items;
	}

	@Override
	public PSortedMap1<K, V> copy() {
		var copy = new PSortedMap1<>(meta);
		copy.map = getMap();
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		for (var e : tmp.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			//noinspection DataFlowIssue
			valueEncoder.accept(bb, e.getValue());
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		clear();
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
		for (int i = bb.ReadUIntPositive(); i > 0; i--) {
			K k = keyDecoder.apply(bb);
			//noinspection DataFlowIssue
			V v = valueDecoder.apply(bb);
			put(k, v);
		}
	}

	@Override
	public TransactionLog createLog(ChangeCaptureMode mode) {
		return mode == ChangeCaptureMode.NONE ? new LogSortedMap<>(this, map)
			: new LogSortedMapDelta.Map1<>(this, map);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogSortedMap1<>(variableId(), this, map.comparator(), meta, null, null);
		//noinspection unchecked
		var delta = (LogSortedMapDelta.Map1<K, V>)finalLog;
		return new LogSortedMap1<>(variableId(), this, map.comparator(), meta, delta.getReplaced(), delta.getRemoved());
	}
}
