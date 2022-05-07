package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;

public class CollMap1<K, V> extends CollMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public CollMap1(Class<K> keyClass, Class<V> valueClass) {
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.LogMap1<"
				+ Reflect.GetStableName(keyClass) + ", " + Reflect.GetStableName(valueClass) + '>');
	}

	private CollMap1(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs,
					 SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public void add(K key, V value) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Add(key, value);
		} else
			_map = _map.plus(key, value);
	}

	@Override
	public void put(K key, V value) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Put(key, value);
		} else
			_map = _map.plus(key, value);
	}

	@Override
	public void remove(K key) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Remove(key);
		} else
			_map = _map.minus(key);
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)Transaction.getCurrent().LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			mapLog.Clear();
		} else
			_map = org.pcollections.Empty.map();
	}

	@Override
	public void FollowerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogMap1<K, V>)_log;
		_map = _map.plusAll(log.getPutted()).minusAll(log.getRemoved());
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogMap1<>(logTypeId, keyCodecFuncs, valueCodecFuncs);
		log.setBelong(getParent());
		log.setThis(this);
		log.setVariableId(getVariableId());
		log.setValue(_map);
		return log;
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	public Bean CopyBean() {
		var copy = new CollMap1<>(logTypeId, keyCodecFuncs, valueCodecFuncs);
		copy._map = _map;
		return copy;
	}

	@Override
	public void Encode(ByteBuffer bb) {
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
	public void Decode(ByteBuffer bb) {
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
