package Zeze.Transaction.Collections;

import java.util.Map;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

@SuppressWarnings("DataFlowIssue")
public class PMap1<K, V> extends PMap<K, V> {
	protected final @NotNull Meta2<K, V> meta;

	public PMap1(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		meta = Meta2.getMap1Meta(keyClass, valueClass);
	}

	private PMap1(@NotNull Meta2<K, V> meta) {
		this.meta = meta;
	}

	@Override
	public @Nullable V put(@NotNull K key, @NotNull V value) {
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("null key");
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("null value");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.put(key, value);
		}
		var exist = map.get(key);
		map = map.plus(key, value);
		return exist;
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		if (m instanceof PMap1)
			m = ((PMap1<? extends K, ? extends V>)m).getMap();
		for (var e : m.entrySet()) {
			if (e.getKey() == null)
				throw new IllegalArgumentException("null key");
			if (e.getValue() == null)
				throw new IllegalArgumentException("null value");
		}

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.putAll(m);
		} else
			map = map.plusAll(m);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable V remove(@NotNull Object key) {
		if (isManaged()) {
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.remove((K)key);
		}
		//noinspection SuspiciousMethodCalls
		var exist = map.get(key);
		map = map.minus(key);
		return exist;
	}

	@Override
	public boolean remove(@NotNull Entry<K, V> item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.remove(item.getKey(), item.getValue());
		}
		var old = map;
		var exist = old.get(item.getKey());
		if (exist != null && exist.equals(item.getValue())) {
			map = map.minus(item.getKey());
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = Empty.map();
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap1<K, V>)_log;
		map = map.minusAll(log.getRemoved()).plusAll(log.getReplaced());
	}

	@Override
	public @NotNull LogBean createLogBean() {
		var log = new LogMap1<>(meta, map);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		return log;
	}

	public void assign(@NotNull PMap1<K, V> pmap) {
		var items = pmap.getMap();
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
			mapLog.putAll(items);
		} else
			map = items;
	}

	@Override
	public @NotNull PMap1<K, V> copy() {
		var copy = new PMap1<>(meta);
		copy.map = getMap();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		for (var e : tmp.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			valueEncoder.accept(bb, e.getValue());
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		clear();
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = valueDecoder.apply(bb);
			put(key, value);
		}
	}
}
