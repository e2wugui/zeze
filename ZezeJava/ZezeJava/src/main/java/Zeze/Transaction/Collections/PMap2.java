package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.Map;
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
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.LogMap2<"
				+ Reflect.GetStableName(keyClass) + ", " + Reflect.GetStableName(valueClass) + '>');
	}

	private PMap2(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, MethodHandle valueFactory) {
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@SuppressWarnings("unchecked")
	public V getOrAdd(K key) throws Throwable {
		var exist = get(key);
		if (null == exist) {
			exist = (V)valueFactory.invoke();
			put(key, exist);
		}
		return exist;
	}

	@Override
	public V put(K key, V value) {
		if (key == null) {
			throw new NullPointerException();
		}
		if (value == null) {
			throw new NullPointerException();
		}

		value.mapKey(key);
		if (isManaged()) {
			value.InitRootInfoWithRedo(RootInfo, this);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return mapLog.Put(key, value);
		}
		var oldV = _map.get(key);
		_map = _map.plus(key, value);
		return oldV;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (var p : m.entrySet()) {
			if (p.getKey() == null) {
				throw new NullPointerException();
			}
			if (p.getValue() == null) {
				throw new NullPointerException();
			}
		}

		if (isManaged()) {
			for (var v : m.values()) {
				v.InitRootInfoWithRedo(RootInfo, this);
			}
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			mapLog.PutAll(m);
		}
		else {
			_map = _map.plusAll(m);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		if (isManaged()) {
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return mapLog.Remove((K)key);
		}
		//noinspection SuspiciousMethodCalls
		var exist = _map.get(key);
		_map = _map.minus(key);
		return exist;
	}

	@Override
	public boolean remove(Entry<K, V> item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return mapLog.Remove(item.getKey(), item.getValue());
		}
		var old = _map;
		var exist = old.get(item.getKey());
		if (null != exist && exist.equals(item.getValue())) {
			_map = _map.minus(item.getKey());
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			mapLog.Clear();
		} else
			_map = org.pcollections.Empty.map();
	}

	private static final Logger logger = LogManager.getLogger(PMap2.class);

	@Override
	public void FollowerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap2<K, V>)_log;
		var tmp = _map;
		for (var put : log.getReplaced().values())
			put.InitRootInfo(RootInfo, this);
		tmp = tmp.plusAll(log.getReplaced()).minusAll(log.getRemoved());

		// apply changed
		for (var e : log.getChangedWithKey().entrySet()) {
			Bean value = tmp.get(e.getKey());
			if (value != null)
				value.FollowerApply(e.getValue());
			else
				logger.error("Not Exist! Key={} Value={}", e.getKey(), e.getValue());
		}
		_map = tmp;
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogMap2<K, V>(logTypeId, keyCodecFuncs, valueFactory);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(_map);
		return log;
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		for (var v : _map.values())
			v.InitRootInfo(root, this);
	}

	@Override
	protected void ResetChildrenRootInfo() {
		for (var v : _map.values())
			v.ResetRootInfo();
	}

	@Override
	public PMap2<K, V> CopyBean() {
		var copy = new PMap2<K, V>(logTypeId, keyCodecFuncs, valueFactory);
		copy._map = _map;
		return copy;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var encoder = keyCodecFuncs.encoder;
		for (var e : tmp.entrySet()) {
			encoder.accept(bb, e.getKey());
			e.getValue().Encode(bb);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void Decode(ByteBuffer bb) {
		clear();
		var decoder = keyCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = decoder.apply(bb);
			V value;
			try {
				value = (V)valueFactory.invoke();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			value.Decode(bb);
			put(key, value);
		}
	}
}
