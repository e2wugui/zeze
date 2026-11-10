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
import Zeze.Collections.Persistents;

/** 事务 Map（2系）：Bean 值受管，原位修改同样记账。 */
public class PMap2<K, V extends Bean> extends PMap<K, V> {
	protected final Map2Meta<K, V> meta;

	public PMap2(Class<K> keyClass, Class<V> valueClass) {
		meta = Map2Meta.get(keyClass, valueClass);
	}

	public PMap2(Class<K> keyClass, Class<V> valueClass, Supplier<V> valueCtor) {
		meta = Map2Meta.create(keyClass, valueClass, valueCtor);
	}

	public PMap2(Map2Meta<K, V> meta) {
		this.meta = meta;
	}

	public Map2Meta<K, V> getMeta() {
		return meta;
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
		meta.checkValue(value);

		if (isManaged()) {
			// 先取写权限后挂接（理由同PList2.add）：拒绝路径不得污染输入bean归属。
			//noinspection unchecked
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			value.initRootInfoWithRedo(rootInfo, this);
			value.mapKey(key);
			return mapLog.put(key, value);
		}
		value.mapKey(key);
		V oldV = map.get(key);
		map = map.plus(key, value);
		return oldV;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return;
		if (m instanceof PMap2)
			m = ((PMap2<? extends K, ? extends V>)m).getMap(); // more stable

		// 整批通过校验后再写 mapKey 或挂接 Bean。已受管项由 initRootInfoWithRedo 拒绝并中止事务。
		for (var e : m.entrySet()) {
			if (e.getKey() == null)
				throw new IllegalArgumentException("null key");
			var value = e.getValue();
			if (value == null)
				throw new IllegalArgumentException("null value");
			meta.checkValue(value);
		}
		if (isManaged()) {
			//noinspection unchecked
			var mapLog = (LogMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			for (var e : m.entrySet()) {
				K k = e.getKey();
				V v = e.getValue();
				v.initRootInfoWithRedo(rootInfo, this);
				v.mapKey(k);
			}
			mapLog.putAll(m);
		} else {
			for (var e : m.entrySet())
				e.getValue().mapKey(e.getKey());
			map = map.plusAll(m);
		}
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
		var log = (LogMap2<K, V>)_log;
		DynamicField.checkTarget(log.dynamicField(), meta.dynamicField);
		var tmp = map;
		var modified = log.modifiedForRead();
		HashMap<K, V> puts = null;
		ArrayList<K> removes = null;
		for (var e : modified.entrySet()) {
			var v = e.getValue();
			if (v != null) {
				// 对齐put/decode/putAllData全部写路径：进map的bean必须带mapKey。
				// followerApply加入的replaced bean若不带，后续本地事务修改collect时
				// 后续提交收集仍需要由 mapKey 定位该值。
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
		// 单遍分区批量应用（同PMap1.followerApply）：等价旧 minusAll(removed).plusAll(replaced)。
		if (removes != null)
			tmp = tmp.minusAll(removes);
		if (puts != null)
			tmp = tmp.plusAll(puts);

		// apply changed
		for (var e : log.changedForRead().entrySet()) {
			// 原位修改只针对最终仍存在的键。键缺失说明回放状态已分歧，应中断应用。
			tmp.get(e.getKey()).followerApply(e.getValue());
		}
		map = tmp;
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		for (V v : map.values())
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		for (V v : map.values())
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public PMap2<K, V> copy() {
		return copy(meta);
	}

	private PMap2<K, V> copy(Map2Meta<K, V> targetMeta) {
		// 深拷贝值，避免副本继续引用原记录的受管 Bean。
		var copy = new PMap2<>(targetMeta);
		var newMap = Persistents.<K, V>map();
		for (var e : getMap().entrySet()) {
			var value = DynamicField.copyValue(targetMeta.dynamicField, e.getValue());
			targetMeta.checkValue(value);
			// mapKey是瞬态容器归属，不由Bean.copy/assign复制；后续原位changed仍需定位该键。
			value.mapKey(e.getKey());
			newMap = newMap.plus(e.getKey(), value);
		}
		copy.map = newMap;
		return copy;
	}

	/** 深拷贝元素赋值（区别于1系assign的引用共享：2系值可变，别名会让两个容器互相可见修改）。 */
	public void assign(PMap2<K, V> pmap) {
		if (this == pmap)
			return; // 自赋值：先clear后copy时源已空，原有内容会永久丢失
		// 使用目标映射校验和复制，成功后才清空。
		var copy = pmap.copy(meta);
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
		int n = bb.ReadUIntPositive();
		if (n == 0)
			return;
		// JDK暂存+单发批量（语义等价，见PMap1.decode注）；try只包valueFactory的invoke。
		var m = new HashMap<K, V>(((initialDecodeCapacity(n) + 2) / 3) * 4);
		try {
			for (int i = n; i > 0; i--) {
				K k = decoder.apply(bb);
				//noinspection unchecked
				V v = (V)meta.valueFactory.invoke();
				v.decode(bb);
				m.put(k, v);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		putAll(m);
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
		return mode == ChangeCaptureMode.NONE ? new LogMap<>(this, map)
			: new LogMapDelta.Map2<>(this, map);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogMap2<>(variableId(), this, meta, null);
		//noinspection unchecked
		var delta = (LogMapDelta.Map2<K, V>)finalLog;
		return new LogMap2<>(variableId(), this, meta, delta.getModified());
	}
}
