package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;

public class LogMap1<K, V> extends LogMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private final HashMap<K, V> replaced = new HashMap<>();
	private final Set<K> removed = new HashSet<>();

	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Transaction.Collections.LogMap1<");

	public LogMap1(Class<K> keyClass, Class<V> valueClass) {
		this(Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass), keyClass, valueClass);
	}

	LogMap1(int logTypeId, Class<K> keyClass, Class<V> valueClass) {
		super(logTypeId);
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogMap1(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final HashMap<K, V> getReplaced() {
		return replaced;
	}

	public final Set<K> getRemoved() {
		return removed;
	}

	public final V get(K key) {
		return getValue().get(key);
	}

	public final void add(K key, V value) {
		put(key, value);
	}

	public final V put(K key, V value) {
		var exist = getValue().get(key);
		setValue(getValue().plus(key, value));
		replaced.put(key, value);
		removed.remove(key);
		return exist;
	}

	public final void putAll(Map<? extends K, ? extends V> m) {
		var newmap = getValue().plusAll(m);
		if (newmap != getValue()) {
			setValue(newmap);
			for (var e : m.entrySet()) {
				replaced.put(e.getKey(), e.getValue());
				removed.remove(e.getKey());
			}
		}
	}

	public final V remove(K key) {
		var old = getValue().get(key);
		if (null != old) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
		}
		return old;
	}

	public final boolean remove(K key, V value) {
		var old = getValue().get(key);
		if (null != old && old.equals((value))) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
			return true;
		}
		return false;
	}

	public final void clear() {
		for (var key : getValue().keySet())
			remove(key);
		setValue(org.pcollections.Empty.map());
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteUInt(replaced.size());
		var keyEncoder = keyCodecFuncs.encoder;
		var valueEncoder = valueCodecFuncs.encoder;
		for (var p : replaced.entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			valueEncoder.accept(bb, p.getValue());
		}

		bb.WriteUInt(removed.size());
		for (var r : removed)
			keyEncoder.accept(bb, r);
	}

	@Override
	public void decode(ByteBuffer bb) {
		replaced.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		var valueDecoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			var key = keyDecoder.apply(bb);
			var value = valueDecoder.apply(bb);
			replaced.put(key, value);
		}

		removed.clear();
		for (int i = bb.ReadUInt(); i > 0; --i)
			removed.add(keyDecoder.apply(bb));
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(LogMap1<K, V> another) {
		// Put,Remove 需要确认有没有顺序问题
		// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
		for (var e : another.replaced.entrySet()) {
			// replace 1,2,3 remove 4
			replaced.put(e.getKey(), e.getValue());
			removed.remove(e.getKey());
		}
		for (var e : another.removed) {
			// replace 2,3 remove 1,4
			replaced.remove(e);
			removed.add(e);
		}
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogMap1<>(getTypeId(), keyCodecFuncs, valueCodecFuncs);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, replaced);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
