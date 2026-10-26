package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.Map;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;

import org.jspecify.annotations.Nullable;
import Zeze.Collections.Persistents;

/** 事务 Map（1系）：不可变键值按值拷贝记账，不支持 Bean 值。 */
public class PMap1<K, V> extends PMap<K, V> {
	protected final Map1Meta<K, V> meta;

	public PMap1(Class<K> keyClass, Class<V> valueClass) {
		meta = Map1Meta.get(keyClass, valueClass);
	}

	public PMap1(Map1Meta<K, V> meta) {
		this.meta = meta;
	}

	public Map1Meta<K, V> getMeta() {
		return meta;
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
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
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
		if (m instanceof PMap1)
			m = ((PMap1<? extends K, ? extends V>)m).getMap(); // more stable
		for (var e : m.entrySet()) {
			if (e.getKey() == null)
				throw new IllegalArgumentException("null key");
			if (e.getValue() == null)
				throw new IllegalArgumentException("null value");
		}

		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.putAll(m);
		} else
			map = map.plusAll(m);
	}

	@Override
	public @Nullable V remove(Object key) {
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
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
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
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
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.clear();
		} else
			map = Persistents.map();
	}

	@Override
	public void followerApply(Log _log) {
		//noinspection unchecked
		var log = (LogMap1<K, V>)_log;
		map = map.minusAll(log.getRemoved()).plusAll(log.getReplaced());
	}

	public void assign(PMap1<K, V> pmap) {
		var items = pmap.getMap();
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.clear();
			mapLog.putAll(items);
		} else
			map = items;
	}

	@Override
	public PMap1<K, V> copy() {
		var copy = new PMap1<>(meta);
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
		int n = bb.ReadUIntPositive();
		if (n == 0)
			return;
		// JDK暂存+单发批量：clear后无中间可观测态，与逐条put等价（同键后写覆盖语义一致）；
		// ktt后端plusAll走builder批量构建。
		var m = new HashMap<K, V>(((initialDecodeCapacity(n) + 2) / 3) * 4);
		for (int i = n; i > 0; i--) {
			//noinspection DataFlowIssue
			m.put(keyDecoder.apply(bb), valueDecoder.apply(bb));
		}
		putAll(m);
	}

	@Override
	public TransactionLog createLog(ChangeCaptureMode mode) {
		return mode == ChangeCaptureMode.NONE ? new LogMap<>(this, map)
			: new LogMapDelta.Map1<>(this, map);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogMap1<>(variableId(), this, meta, null, null);
		//noinspection unchecked
		var delta = (LogMapDelta.Map1<K, V>)finalLog;
		return new LogMap1<>(variableId(), this, meta, delta.getReplaced(), delta.getRemoved());
	}
}
