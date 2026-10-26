package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
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

/** 事务 SortedMap（2系）：Comparable 键，Bean 值受管。 */
@SuppressWarnings({"unchecked"})
public class PSortedMap2<K extends Comparable<K>, V extends Bean> extends PSortedMap<K, V> {
	protected final SortedMap2Meta<K, V> meta;

	public PSortedMap2(Class<K> keyClass, Class<V> valueClass) {
		meta = SortedMap2Meta.get(keyClass, valueClass);
	}

	public PSortedMap2(Class<K> keyClass, Class<V> valueClass, Supplier<V> valueCtor) {
		meta = SortedMap2Meta.create(keyClass, valueClass, valueCtor);
	}

	public PSortedMap2(Class<K> keyClass, ToLongFunction<@Nullable Bean> get, LongFunction<@Nullable Bean> create) { // only for DynamicBean value
		// 必须用 sortedMap2 家族头哈希：写端 typeId 与读端（Helper.registerLogSortedMap2Dynamic）
		// 的注册键对称；用错 map2 家族会借道同 keyClass 的 map<K,dynamic> 注册解码成 LogMap2。
		meta = SortedMap2Meta.createDynamic(keyClass, get, create);
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
			//noinspection DataFlowIssue
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

		if (isManaged()) {
			// 先取写权限后挂接（理由同PList2.add）：拒绝路径不得污染输入bean归属。
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			value.initRootInfoWithRedo(rootInfo, this);
			value.mapKey(key);
			assert parent() != null;
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
		if (m instanceof PSortedMap2)
			m = ((PSortedMap2<? extends K, ? extends V>)m).getMap(); // more stable

		if (isManaged()) {
			// 双循环（对齐PSortedMap1.putAll"先全量校验、后入日志"）：单循环"边验边改"时，靠后条目
			// null抛出前靠前bean的initRootInfoWithRedo/mapKey已改写——普通字段写不受事务回滚保护，
			// 调用方catch后复用bean即携带脏归属。
			for (var e : m.entrySet()) {
				K k = e.getKey();
				if (k == null)
					throw new IllegalArgumentException("null key");
				if (e.getValue() == null) // 对齐put与PMap1.putAll：null在托管分支的initRootInfoWithRedo处解引用NPE，先验拒绝
					throw new IllegalArgumentException("null value");
				// 已受管项不预检：挂接循环内经initRootInfoWithRedo→throwAbort整体中止（277e6e501 Abort契约）
			}
			assert parent() != null;
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			for (var e : m.entrySet()) {
				K k = e.getKey();
				V v = e.getValue();
				v.initRootInfoWithRedo(rootInfo, this);
				v.mapKey(k);
			}
			mapLog.putAll(m);
		} else {
			for (var e : m.entrySet()) {
				K k = e.getKey();
				if (k == null)
					throw new IllegalArgumentException("null key");
				if (e.getValue() == null) // null在非托管分支的mapKey处解引用NPE，先验拒绝
					throw new IllegalArgumentException("null value");
				e.getValue().mapKey(k);
			}
			map = map.plusAll(m);
		}
	}

	@Override
	public @Nullable V remove(Object key) {
		if (isManaged()) {
			assert parent() != null;
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			return mapLog.remove((K)key);
		}
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
			var mapLog = (LogSortedMap<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			mapLog.clear();
		} else {
			var comparator = map.comparator();
			map = comparator != null ? TreePMap.empty(comparator) : Empty.sortedMap();
		}
	}

	@Override
	public void followerApply(Log _log) {
		var log = (LogSortedMap2<K, V>)_log;
		var tmp = map;
		for (var e : log.replacedForRead().entrySet()) {
			// 对齐put/decode/putAllData全部写路径：进map的bean必须带mapKey（同PMap2.followerApply）。
			e.getValue().initRootInfo(rootInfo, this);
			e.getValue().mapKey(e.getKey());
		}
		tmp = tmp.minusAll(log.removedForRead()).plusAll(log.replacedForRead());

		// apply changed
		for (var e : log.changedForRead().entrySet()) {
			// 正常重放下changed只含最终map存在的key（收集时已过滤），follower侧null即先行分歧
			// （可能是编辑了又被删等任何来源）：直接递归抛NPE，由驱动方裁决——raft路径
			// Rocks.followerApply统一catch+fatalKill；History回放路径批中断。抛出只证明应用
			// 路径无硬分歧，不证明最终数值一致（错位应用/历史缺失不抛异常），仍由
			// Verify.verifyAndClear全量对账兜底。抛出时map未提交。
			tmp.get(e.getKey()).followerApply(e.getValue());
		}
		map = tmp;
	}

	@Override
	protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		for (V v : map.values())
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		for (V v : map.values())
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public PSortedMap2<K, V> copy() {
		// 深拷贝值Bean：浅拷贝共享原记录树内的受管活Bean，经ReadOnly.copy()交出的“副本”
		// 拿到的是可变后门（copy.get(k).setField改的是原记录），且副本元素再put进受管容器
		// 抛HasManagedException——与CollOne.copy()/生成代码Bean.assign的深拷贝语义对齐。
		var copy = new PSortedMap2<>(meta);
		var source = getMap();
		var comparator = source.comparator();
		org.pcollections.PSortedMap<K, V> newMap = comparator != null
			? TreePMap.empty(comparator) : Empty.sortedMap();
		for (var e : source.entrySet()) {
			V value = (V)e.getValue().copy();
			value.mapKey(e.getKey());
			newMap = newMap.plus(e.getKey(), value);
		}
		copy.map = newMap;
		return copy;
	}

	/** 深拷贝元素赋值（区别于1系assign的引用共享：2系值可变，别名会让两个容器互相可见修改）。 */
	public void assign(PSortedMap2<K, V> pmap) {
		if (this == pmap)
			return; // 自赋值：先clear后copy时源已空，原有内容会永久丢失
		var copy = pmap.copy(); // 先备输入再发布：copy抛出时目标保持原值
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
				//noinspection DataFlowIssue
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

	@SuppressWarnings("unchecked")
	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogSortedMap2<>(variableId(), this, map.comparator(), meta, null, null);
		if (!(finalLog instanceof LogSortedMapDelta.Map2<?, ?>))
			throw new IllegalStateException("incomplete or incompatible collection transaction state");
		var delta = (LogSortedMapDelta.Map2<K, V>)finalLog;
		return new LogSortedMap2<>(variableId(), this, map.comparator(), meta, delta.getReplaced(), delta.getRemoved());
	}
}
