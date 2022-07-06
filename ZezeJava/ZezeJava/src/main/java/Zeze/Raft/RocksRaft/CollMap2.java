package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;

public class CollMap2<K, V extends Bean> extends CollMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	private final MethodHandle valueFactory;
	private final int logTypeId;

	public CollMap2(Class<K> keyClass, Class<V> valueClass) {
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.LogMap2<"
				+ Reflect.GetStableName(keyClass) + ", " + Reflect.GetStableName(valueClass) + '>');
	}

	private CollMap2(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, MethodHandle valueFactory) {
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@Override
	public void add(K key, V value) {
		put(key, value);
	}

	@Override
	public void put(K key, V value) {
		value.setMapKey(key);
		if (isManaged()) {
			value.InitRootInfo(getRootInfo(), this);
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Put(key, value);
		} else
			_map = _map.plus(key, value);
	}

	@Override
	public void remove(K key) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Remove(key);
		} else
			_map = _map.minus(key);
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap2<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Clear();
		} else
			_map = org.pcollections.Empty.map();
	}

	@Override
	public void FollowerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap2<K, V>)_log;
		var tmp = _map;
		for (var put : log.getPutted().values())
			put.InitRootInfo(getRootInfo(), this);
		tmp = tmp.plusAll(log.getPutted()).minusAll(log.getRemoved());

		// apply changed
		for (var e : log.getChangedWithKey().entrySet()) {
			Bean value = tmp.get(e.getKey());
			if (value != null)
				value.FollowerApply(e.getValue());
			else
				Rocks.logger.error("Not Exist! Key={} Value={}", e.getKey(), e.getValue());
		}
		_map = tmp;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void LeaderApplyNoRecursive(Log _log) {
		_map = ((LogMap2<K, V>)_log).getValue();
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogMap2<K, V>(logTypeId, keyCodecFuncs, valueFactory);
		log.setBelong(getParent());
		log.setThis(this);
		log.setVariableId(getVariableId());
		log.setValue(_map);
		return log;
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		for (var v : _map.values())
			v.InitRootInfo(root, this);
	}

	@Override
	public Bean CopyBean() {
		var copy = new CollMap2<K, V>(logTypeId, keyCodecFuncs, valueFactory);
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
