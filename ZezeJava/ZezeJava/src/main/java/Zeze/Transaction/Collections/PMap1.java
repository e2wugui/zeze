package Zeze.Transaction.Collections;

import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;

public class PMap1<K, V> extends PMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public PMap1(Class<K> keyClass, Class<V> valueClass) {
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Collections.LogMap1<"
				+ Reflect.getStableName(keyClass) + ", " + Reflect.getStableName(valueClass) + '>');
	}

	@SuppressWarnings("unchecked")
	public PMap1(Class<K> keyClass, ToLongFunction<Bean> get, LongFunction<Bean> create) { // only for DynamicBean value
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = (SerializeHelper.CodecFuncs<V>)SerializeHelper.createCodec(get, create);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Collections.LogMap1<"
				+ Reflect.getStableName(keyClass) + ", Zeze.Transaction.DynamicBean>");
	}

	private PMap1(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs,
				  SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	public SerializeHelper.CodecFuncs<V> getValueCodecFuncs() {
		return valueCodecFuncs;
	}

	@Override
	public V put(K key, V value) {
		if (key == null)
			throw new IllegalArgumentException("null key");
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
	public void putAll(Map<? extends K, ? extends V> m) {
		for (var p : m.entrySet()) {
			if (p.getKey() == null)
				throw new IllegalArgumentException("null key");
			if (p.getValue() == null)
				throw new IllegalArgumentException("null value");
		}

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.putAll(m);
		} else {
			map = map.plusAll(m);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
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
	public boolean remove(Entry<K, V> item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
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
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = org.pcollections.Empty.map();
	}

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap1<K, V>)_log;
		map = map.plusAll(log.getReplaced()).minusAll(log.getRemoved());
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogMap1<>(logTypeId, keyCodecFuncs, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(map);
		return log;
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	protected void resetChildrenRootInfo() {
	}

	@Override
	public PMap1<K, V> copy() {
		var copy = new PMap1<>(logTypeId, keyCodecFuncs, valueCodecFuncs);
		copy.map = map;
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var keyEncoder = keyCodecFuncs.encoder;
		var valueEncoder = valueCodecFuncs.encoder;
		for (var e : tmp.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			valueEncoder.accept(bb, e.getValue());
		}
	}

	@Override
	public void decode(ByteBuffer bb) {
		clear();
		var keyDecoder = keyCodecFuncs.decoder;
		var valueDecoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = valueDecoder.apply(bb);
			put(key, value);
		}
	}
}
