package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.Reflect;

public class LogMap1<K, V> extends LogMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private final HashMap<K, V> Replaced = new HashMap<>();
	private final Set<K> Removed = new HashSet<>();

	public LogMap1(Class<K> keyClass, Class<V> valueClass) {
		this("Zeze.Raft.RocksRaft.LogMap1<" + Reflect.GetStableName(keyClass) + ", "
				+ Reflect.GetStableName(valueClass) + '>', keyClass, valueClass);
	}

	LogMap1(String typeName, Class<K> keyClass, Class<V> valueClass) {
		super(typeName);
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogMap1(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final HashMap<K, V> getReplaced() {
		return Replaced;
	}

	public final Set<K> getRemoved() {
		return Removed;
	}

	public final V Get(K key) {
		return getValue().get(key);
	}

	public final void Add(K key, V value) {
		Put(key, value);
	}

	public final V Put(K key, V value) {
		var exist = getValue().get(key);
		setValue(getValue().plus(key, value));
		Replaced.put(key, value);
		Removed.remove(key);
		return exist;
	}

	public final void PutAll(Map<? extends K, ? extends V> m) {
		var newmap = getValue().plusAll(m);
		if (newmap != getValue()) {
			setValue(newmap);
			for (var e : m.entrySet()) {
				Replaced.put(e.getKey(), e.getValue());
				Removed.remove(e.getKey());
			}
		}
	}

	public final V Remove(K key) {
		var old = getValue().get(key);
		if (null != old) {
			setValue(getValue().minus(key));
			Replaced.remove(key);
			Removed.add(key);
		}
		return old;
	}

	public final boolean Remove(K key, V value) {
		var old = getValue().get(key);
		if (null != old && old.equals((value))) {
			setValue(getValue().minus(key));
			Replaced.remove(key);
			Removed.add(key);
			return true;
		}
		return false;
	}

	public final void Clear() {
		for (var key : getValue().keySet())
			Remove(key);
		setValue(org.pcollections.Empty.map());
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteUInt(Replaced.size());
		var keyEncoder = keyCodecFuncs.encoder;
		var valueEncoder = valueCodecFuncs.encoder;
		for (var p : Replaced.entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			valueEncoder.accept(bb, p.getValue());
		}

		bb.WriteUInt(Removed.size());
		for (var r : Removed)
			keyEncoder.accept(bb, r);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Replaced.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		var valueDecoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			var key = keyDecoder.apply(bb);
			var value = valueDecoder.apply(bb);
			Replaced.put(key, value);
		}

		Removed.clear();
		for (int i = bb.ReadUInt(); i > 0; --i)
			Removed.add(keyDecoder.apply(bb));
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(getValue());
			currentLog.MergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void MergeChangeNote(LogMap1<K, V> another) {
		// Put,Remove 需要确认有没有顺序问题
		// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
		for (var e : another.Replaced.entrySet()) {
			// replace 1,2,3 remove 4
			Replaced.put(e.getKey(), e.getValue());
			Removed.remove(e.getKey());
		}
		for (var e : another.Removed) {
			// replace 2,3 remove 1,4
			Replaced.remove(e);
			Removed.add(e);
		}
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogMap1<>(getTypeId(), keyCodecFuncs, valueCodecFuncs);
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, Replaced);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, Removed);
		return sb.toString();
	}
}
