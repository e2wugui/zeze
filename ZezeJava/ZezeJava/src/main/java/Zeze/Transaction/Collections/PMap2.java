package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PMap2<K, V extends Bean> extends PMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	private final MethodHandle valueFactory;
	private final int logTypeId;

	public PMap2(Class<K> keyClass, Class<V> valueClass) {
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Collections.LogMap2<"
				+ Reflect.getStableName(keyClass) + ", " + Reflect.getStableName(valueClass) + '>');
	}

	public PMap2(Class<K> keyClass, ToLongFunction<Bean> get, LongFunction<Bean> create) { // only for DynamicBean value
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueFactory = SerializeHelper.createDynamicFactory(get, create);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Collections.LogMap2<"
				+ Reflect.getStableName(keyClass) + ", Zeze.Transaction.DynamicBean>");
	}

	private PMap2(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, MethodHandle valueFactory) {
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@SuppressWarnings("unchecked")
	public V createValue() {
		try {
			return (V)valueFactory.invoke();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public V getOrAdd(K key) throws Throwable {
		var exist = get(key);
		if (exist == null) {
			exist = (V)valueFactory.invoke();
			put(key, exist);
		}
		return exist;
	}

	@Override
	public V put(K key, V value) {
		if (key == null)
			throw new IllegalArgumentException("null key");
		if (value == null)
			throw new IllegalArgumentException("null value");

		if (!value.isManaged())
			value.mapKey(key);
		if (isManaged()) {
			value.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return mapLog.put(key, value);
		}
		var oldV = map.get(key);
		map = map.plus(key, value);
		return oldV;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (var p : m.entrySet()) {
			var k = p.getKey();
			if (k == null)
				throw new IllegalArgumentException("null key");
			var v = p.getValue();
			if (v == null)
				throw new IllegalArgumentException("null value");
			if (!v.isManaged())
				v.mapKey(k);
		}

		if (isManaged()) {
			for (var v : m.values())
				v.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.putAll(m);
		} else
			map = map.plusAll(m);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		if (isManaged()) {
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
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
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = org.pcollections.Empty.map();
	}

	private static final Logger logger = LogManager.getLogger(PMap2.class);

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap2<K, V>)_log;
		var tmp = map;
		for (var put : log.getReplaced().values())
			put.initRootInfo(rootInfo, this);
		tmp = tmp.plusAll(log.getReplaced()).minusAll(log.getRemoved());

		// apply changed
		for (var e : log.getChangedWithKey().entrySet()) {
			Bean value = tmp.get(e.getKey());
			if (value != null)
				value.followerApply(e.getValue());
			else
				logger.error("Not Exist! Key={} Value={}", e.getKey(), e.getValue());
		}
		map = tmp;
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogMap2<K, V>(logTypeId, keyCodecFuncs, valueFactory);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(map);
		return log;
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		for (var v : map.values())
			v.initRootInfo(root, this);
	}

	@Override
	public PMap2<K, V> copy() {
		var copy = new PMap2<K, V>(logTypeId, keyCodecFuncs, valueFactory);
		copy.map = map;
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var encoder = keyCodecFuncs.encoder;
		for (var e : tmp.entrySet()) {
			encoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(ByteBuffer bb) {
		clear();
		var decoder = keyCodecFuncs.decoder;
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				var key = decoder.apply(bb);
				V value = (V)valueFactory.invoke();
				value.decode(bb);
				put(key, value);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
