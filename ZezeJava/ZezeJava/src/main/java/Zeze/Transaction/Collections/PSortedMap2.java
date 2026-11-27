package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicField;
import Zeze.Transaction.Data;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;
import org.pcollections.Empty;
import org.pcollections.TreePMap;

/**
 * 事务 SortedMap（2系）：Comparable 键，Bean 值受管。
 * 未受管 Map 可暂存受管值的引用；值仍归原容器所有，不能随临时 Map 再次挂接受管。
 */
public class PSortedMap2<K extends Comparable<K>, V extends Bean> extends PSortedMap<K, V> {
	protected final SortedMap2Meta<K, V> meta;

	public PSortedMap2(Class<K> keyClass, Class<V> valueClass) {
		meta = SortedMap2Meta.get(keyClass, valueClass);
	}

	public PSortedMap2(Class<K> keyClass, Class<V> valueClass, Supplier<V> valueCtor) {
		meta = SortedMap2Meta.create(keyClass, valueClass, valueCtor);
	}

	public PSortedMap2(SortedMap2Meta<K, V> meta) {
		this.meta = meta;
	}

	public SortedMap2Meta<K, V> getMeta() {
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

	public V createValue() {
		try {
			//noinspection unchecked
			return (V)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	public V getOrAdd(K key) {
		V exist = get(key);
		if (exist == null) {
			exist = createValue();
			put(key, exist);
		}
		return exist;
	}

	@Override
	public @Nullable V put(K key, V value) {
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("null key");
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("null value");
		DynamicField.checkValueOwner(meta.dynamicField, value);

		if (isManaged()) {
			// 先取写权限再挂接，避免拒绝路径污染输入 Bean 归属。
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			value.initRootInfoWithRedo(rootInfo, this);
			value.mapKey(key);
			assert parent() != null;
			return mapLog.put(key, value);
		}
		// 未受管容器可以暂存引用，但不能改写其他受管容器拥有的定位信息。
		if (!value.isManaged())
			value.mapKey(key);
		V oldV = map.get(key);
		map = map.plus(key, value);
		return oldV;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return;
		if (m instanceof PSortedMap2)
			m = ((PSortedMap2<? extends K, ? extends V>)m).getMap(); // 捕获不可变快照

		// 整批通过校验后再写 mapKey 或挂接 Bean。
		for (var e : m.entrySet()) {
			if (e.getKey() == null)
				throw new IllegalArgumentException("null key");
			var value = e.getValue();
			if (value == null)
				throw new IllegalArgumentException("null value");
			DynamicField.checkValueOwner(meta.dynamicField, value);
		}

		if (isManaged()) {
			assert parent() != null;
			//noinspection unchecked
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			for (var e : m.entrySet()) {
				K k = e.getKey();
				V v = e.getValue();
				// 已受管项由 initRootInfoWithRedo 拒绝并中止事务。
				v.initRootInfoWithRedo(rootInfo, this);
				v.mapKey(k);
			}
			mapLog.putAll(m);
		} else {
			var newMap = map.plusAll(m);
			for (var e : m.entrySet())
				if (!e.getValue().isManaged())
					e.getValue().mapKey(e.getKey());
			map = newMap;
		}
	}

	@Override
	public @Nullable V remove(Object key) {
		if (isManaged()) {
			assert parent() != null;
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
			assert parent() != null;
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
			assert parent() != null;
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
		var log = (LogSortedMap2<K, V>)_log;
		DynamicField.checkTarget(log.dynamicField(), meta.dynamicField);
		var tmp = map;
		var modified = log.modifiedForRead();
		HashMap<K, V> puts = null;
		ArrayList<K> removes = null;
		for (var e : modified.entrySet()) {
			var v = e.getValue();
			if (v != null) {
				// 写入容器的 Bean 须绑定 mapKey，供后续本地修改收集定位。
				v.initRootInfo(rootInfo, this);
				v.mapKey(e.getKey());
				if (puts == null)
					puts = new HashMap<>(modified.size() * 2);
				puts.put(e.getKey(), v);
			} else {
				if (removes == null)
					removes = new ArrayList<>(modified.size());
				removes.add(e.getKey());
			}
		}
		if (removes != null)
			tmp = tmp.minusAll(removes);
		if (puts != null)
			tmp = tmp.plusAll(puts);

		for (var e : log.changedForRead().entrySet()) {
			// 原位修改只针对最终仍存在的键。键缺失说明回放状态已分歧，应中断应用。
			tmp.get(e.getKey()).followerApply(e.getValue());
		}
		map = tmp;
	}

	@Override
	protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		for (var e : map.entrySet()) {
			var value = e.getValue();
			value.initRootInfo(root, this);
			// 挂接前可能经过多个未受管容器，定位键以最终取得所有权的容器为准。
			value.mapKey(e.getKey());
		}
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		for (var e : map.entrySet()) {
			var value = e.getValue();
			value.initRootInfoWithRedo(root, this);
			value.mapKey(e.getKey());
		}
	}

	@Override
	public PSortedMap2<K, V> copy() {
		return copy(meta);
	}

	private PSortedMap2<K, V> copy(SortedMap2Meta<K, V> targetMeta) {
		// 深拷贝值，避免副本继续引用原记录的受管 Bean。
		var copy = new PSortedMap2<>(targetMeta);
		var source = getMap();
		var comparator = source.comparator();
		org.pcollections.PSortedMap<K, V> newMap = comparator != null
			? TreePMap.empty(comparator) : Empty.sortedMap();
		for (var e : source.entrySet()) {
			var value = DynamicField.copyValue(targetMeta.dynamicField, e.getValue());
			value.mapKey(e.getKey());
			newMap = newMap.plus(e.getKey(), value);
		}
		copy.map = newMap;
		return copy;
	}

	/** 深拷贝元素赋值：2 系值可变，避免两个容器共享同一 Bean 引用。 */
	public void assign(PSortedMap2<K, V> pmap) {
		if (this == pmap)
			return; // 避免自赋值先 clear 导致原内容丢失
		// 使用目标映射校验和复制，成功后才清空。
		var copy = meta.dynamicField != null ? pmap.copy(meta) : pmap.copy();
		clear();
		putAll(copy);
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var encoder = meta.keyEncoder;
		for (var e : tmp.entrySet()) {
			encoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		clear();
		var decoder = meta.keyDecoder;
		try {
			for (int i = bb.ReadUIntPositive(); i > 0; i--) {
				K k = decoder.apply(bb);
				//noinspection unchecked
				V v = (V)meta.valueFactory.invoke();
				v.decode(bb);
				put(k, v);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	public <D extends Data> void putAllData(Map<K, D> dataMap) {
		Bean.toBeanMap(dataMap, this);
	}

	public <D extends Data> void toDataMap(Map<K, D> dataMap) {
		Bean.toDataMap(getMap(), dataMap);
	}

	public <D extends Data> HashMap<K, D> toDataMap() {
		var beanMap = getMap();
		var dataMap = new HashMap<K, D>(((beanMap.size() + 2) / 3) * 4);
		Bean.toDataMap(beanMap, dataMap);
		return dataMap;
	}

	@Override
	public TransactionLog createLog(ChangeCaptureMode mode) {
		return mode == ChangeCaptureMode.NONE ? new LogSortedMap<>(this, map)
			: new LogSortedMapDelta.Map2<>(this, map);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogSortedMap2<>(variableId(), this, map.comparator(), meta, null);
		//noinspection unchecked
		var delta = (LogSortedMapDelta.Map2<K, V>)finalLog;
		return new LogSortedMap2<>(variableId(), this, map.comparator(), meta, delta.getModified());
	}
}
