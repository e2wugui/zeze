package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

import java.util.Map;

public class PSortedMap1<K extends Comparable<K>, V> extends PSortedMap<K, V> {
	protected final @NotNull Meta2<K, V> meta;

	// Bean key不支持（FND7-05，PMap1/PSet1判例姊妹）：排序map本体TreePMap按compareTo定序没问题，
	// 但日志簿记LogSortedMap1.replaced/removed是HashMap/HashSet——Bean值语义equals配身份
	// hashCode，等值bean落不同桶静默漏命中：mergeChangeNote漏合并，encode按身份哈希决定的
	// 迭代序写出重复条目，follower解码plusAll的终值依赖迭代序，可致静默主从分歧。显式失败优于静默错。
	private static <K> void checkBeanKey(@NotNull Class<K> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
					"PSortedMap1 does not support Bean key type (equals-without-hashCode misbehaves in hash map): "
							+ keyClass.getName());
	}

	public PSortedMap1(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		checkBeanKey(keyClass);
		// Bean值不支持（FND7-09姊妹缺口，PList1/PMap1判例同族）：排序map同为1系按值拷贝记账，
		// put不挂接rootInfo（对比PSortedMap2.put），装入的bean永不受管——原位修改不产生日志，
		// 提交后静默丢失。显式失败优于静默丢数据。
		if (Bean.class.isAssignableFrom(valueClass))
			throw new IllegalArgumentException(
					"PSortedMap1 does not support Bean value type (in-place modifications never managed, silently lost): "
							+ valueClass.getName());
		meta = Meta2.getSortedMap1Meta(keyClass, valueClass);
	}

	public PSortedMap1(@NotNull Meta2<K, V> meta) {
		this.meta = meta;
	}

	public @NotNull Meta2<K, V> getMeta() {
		return meta;
	}

	@Override
	public ByteBuffer encodeKey(K key) {
		var bb = ByteBuffer.Allocate();
		meta.keyEncoder.accept(bb, key);
		return bb;
	}

	@Override
	public K decodeKey(@NotNull ByteBuffer bb) {
		return meta.keyDecoder.apply(bb);
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
			var mapLog = (LogSortedMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.put(key, value);
		}
		var exist = map.get(key);
		map = map.plus(key, value);
		return exist;
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
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
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.putAll(m);
		} else
			map = map.plusAll(m);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable V remove(@NotNull Object key) {
		if (isManaged()) {
			var mapLog = (LogSortedMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.remove((K)key);
		}
		//noinspection SuspiciousMethodCalls
		V exist = map.get(key);
		map = map.minus(key);
		return exist;
	}

	@Override
	public boolean remove(@NotNull Entry<K, V> item) {
		K k = item.getKey();
		V v = item.getValue();
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
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
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = Empty.sortedMap();
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogSortedMap1<K, V>)_log;
		map = map.minusAll(log.getRemoved()).plusAll(log.getReplaced());
	}

	@Override
	public @NotNull LogBean createLogBean() {
		return new LogSortedMap1<>(parent(), variableId(), this, map, meta);
	}

	public void assign(@NotNull PSortedMap1<K, V> pmap) {
		var items = pmap.getMap();
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
			mapLog.putAll(items);
		} else
			map = items;
	}

	@Override
	public @NotNull PSortedMap1<K, V> copy() {
		var copy = new PSortedMap1<>(meta);
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
		for (int i = bb.ReadUIntPositive(); i > 0; i--) {
			K k = keyDecoder.apply(bb);
			V v = valueDecoder.apply(bb);
			put(k, v);
		}
	}
}
