package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

@SuppressWarnings("DataFlowIssue")
public class PMap2<K, V extends Bean> extends PMap<K, V> {
	protected final @NotNull Meta2<K, V> meta;

	public PMap2(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		meta = Meta2.getMap2Meta(keyClass, valueClass);
	}

	public PMap2(@NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) { // only for DynamicBean value
		meta = Meta2.createDynamicMapMeta(keyClass, get, create);
	}

	private PMap2(@NotNull Meta2<K, V> meta) {
		this.meta = meta;
	}

	@SuppressWarnings("unchecked")
	public @NotNull V createValue() {
		try {
			return (V)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public @NotNull V getOrAdd(@NotNull K key) {
		var exist = get(key);
		if (exist == null) {
			exist = createValue();
			put(key, exist); // 里面会注册IZeroLogBean。
		}
		return exist;
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
			value.initRootInfoWithRedo(rootInfo, this);
			value.mapKey(key);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			Transaction.getCurrent().add(mapLog);
			return mapLog.put(key, value);
		}
		value.mapKey(key);
		var oldV = map.get(key);
		map = map.plus(key, value);
		return oldV;
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		for (var p : m.entrySet()) {
			var k = p.getKey();
			if (k == null)
				throw new IllegalArgumentException("null key");
			var v = p.getValue();
			if (v == null)
				throw new IllegalArgumentException("null value");
		}

		if (isManaged()) {
			for (var p : m.entrySet()) {
				var v = p.getValue();
				v.initRootInfoWithRedo(rootInfo, this);
				v.mapKey(p.getKey());
			}
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			Transaction.getCurrent().add(mapLog);
			mapLog.putAll(m);
		} else {
			for (var p : m.entrySet())
				p.getValue().mapKey(p.getKey());
			map = map.plusAll(m);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable V remove(@NotNull Object key) {
		if (isManaged()) {
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			Transaction.getCurrent().add(mapLog);
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
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			Transaction.getCurrent().add(mapLog);
			return mapLog.remove(item.getKey(), item.getValue());
		}
		var old = map;
		var exist = old.get(item.getKey());
		if (null != exist && exist.equals(item.getValue())) {
			map = map.minus(item.getKey());
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
			Transaction.getCurrent().add(mapLog);
		} else
			map = Empty.map();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap2<K, V>)_log;
		var tmp = map;
		// new protocol: LogMap2.decode已经构造好getReplaced()，所以下面的代码保持不变，不需要修改。

		for (var put : log.getReplaced().values())
			put.initRootInfo(rootInfo, this);
		tmp = tmp.plusAll(log.getReplaced()).minusAll(log.getRemoved());

		// apply changed
		for (var e : log.getChangedWithKey().entrySet()) {
			Bean value = tmp.get(e.getKey());
			value.followerApply(e.getValue()); // value NullPointerException if not exist.
		}
		map = tmp;
	}

	@Override
	public @NotNull LogBean createLogBean() {
		var log = new LogMap2<>(meta, map);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		return log;
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		for (var v : map.values())
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		for (var v : map.values())
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public @NotNull PMap2<K, V> copy() {
		var copy = new PMap2<>(meta);
		copy.map = getMap();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var encoder = meta.keyEncoder;
		for (var e : tmp.entrySet()) {
			encoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		clear();
		var decoder = meta.keyDecoder;
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				var key = decoder.apply(bb);
				V value = (V)meta.valueFactory.invoke();
				value.decode(bb);
				put(key, value);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
		}
	}

	public <D extends Data> void putAllData(@NotNull Map<K, D> dataMap) {
		Bean.toBeanMap(dataMap, this);
	}

	public <D extends Data> void toDataMap(@NotNull Map<K, D> dataList) {
		Bean.toDataMap(getMap(), dataList);
	}

	public <D extends Data> @NotNull HashMap<K, D> toDataMap() {
		var beanMap = getMap();
		var dataMap = new HashMap<K, D>(((beanMap.size() + 2) / 3) * 4);
		Bean.toDataMap(beanMap, dataMap);
		return dataMap;
	}
}
