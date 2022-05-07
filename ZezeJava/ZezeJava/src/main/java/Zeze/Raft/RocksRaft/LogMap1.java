package Zeze.Raft.RocksRaft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;

public class LogMap1<K, V> extends LogMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private final HashMap<K, V> Putted = new HashMap<>();
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

	public final HashMap<K, V> getPutted() {
		return Putted;
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

	public final void Put(K key, V value) {
		setValue(getValue().plus(key, value));
		Putted.put(key, value);
		Removed.remove(key);
	}

	public final void Remove(K key) {
		setValue(getValue().minus(key));
		Putted.remove(key);
		Removed.add(key);
	}

	public final void Clear() {
		for (var key : getValue().keySet())
			Remove(key);
		setValue(org.pcollections.Empty.map());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteUInt(Putted.size());
		var keyEncoder = keyCodecFuncs.encoder;
		var valueEncoder = valueCodecFuncs.encoder;
		for (var p : Putted.entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			valueEncoder.accept(bb, p.getValue());
		}

		bb.WriteUInt(Removed.size());
		for (var r : Removed)
			keyEncoder.accept(bb, r);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Putted.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		var valueDecoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			var key = keyDecoder.apply(bb);
			var value = valueDecoder.apply(bb);
			Putted.put(key, value);
		}

		Removed.clear();
		for (int i = bb.ReadUInt(); i > 0; --i)
			Removed.add(keyDecoder.apply(bb));
	}

	@Override
	public void EndSavepoint(Savepoint currentSp) {
		var log = currentSp.getLogs().get(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(this.getValue());
			currentLog.MergeChangeNote(this);
		} else
			currentSp.getLogs().put(getLogKey(), this);
	}

	private void MergeChangeNote(LogMap1<K, V> another) {
		// Put,Remove 需要确认有没有顺序问题
		// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
		for (var e : another.Putted.entrySet()) {
			// replace 1,2,3 remove 4
			Putted.put(e.getKey(), e.getValue());
			Removed.remove(e.getKey());
		}
		for (var e : another.Removed) {
			// replace 2,3 remove 1,4
			Putted.remove(e);
			Removed.add(e);
		}
	}

	@Override
	public Log BeginSavepoint() {
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
		ByteBuffer.BuildSortedString(sb, Putted);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, Removed);
		return sb.toString();
	}
}
