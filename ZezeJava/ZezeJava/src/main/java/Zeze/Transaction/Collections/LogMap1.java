package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;

import org.jspecify.annotations.Nullable;
import Zeze.Collections.Persistents;

/** PMap1 的变更日志：以 replaced/removed 两个增量集合记录事务内对持久化 Map 的修改。 */
public class LogMap1<K, V> extends LogMap<K, V> {
	protected final Meta2<K, V> meta;
	private final HashMap<K, V> replaced = new HashMap<>();
	private final Set<K> removed = new HashSet<>();

	public LogMap1(@Nullable Bean belong, int varId, @Nullable Bean self, org.pcollections.PMap<K, V> value,
				   Meta2<K, V> meta) {
		super(belong, varId, self, value);
		this.meta = meta;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final HashMap<K, V> getReplaced() {
		return replaced;
	}

	public final Set<K> getRemoved() {
		return removed;
	}

	public final @Nullable V get(K key) {
		return getValue().get(key);
	}

	public final void add(K key, V value) {
		put(key, value);
	}

	public final @Nullable V put(K key, V value) {
		V exist = getValue().get(key);
		setValue(getValue().plus(key, value));
		removed.remove(key);
		replaced.put(key, value);
		return exist;
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		var old = getValue();
		var newMap = old.plusAll(m);
		if (newMap != old) {
			setValue(newMap);
			// 只记值真实变化的键：值未变的键混入replaced是幻影增量，误导增量驱动的监听器。
			// 变化判定按系覆写：1系值不可变用equals；2系值可变须用身份（见LogMap2）。
			for (var e : m.entrySet()) {
				K k = e.getKey();
				if (isValueChanged(old.get(k), e.getValue())) {
					removed.remove(k);
					replaced.put(k, e.getValue());
				}
			}
		}
	}

	/** putAll的值变化判定：1系值不可变，equals相等即无变化。 */
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		return !java.util.Objects.equals(oldValue, newValue);
	}

	public final @Nullable V remove(K key) {
		V old = getValue().get(key);
		if (old != null) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
		}
		return old;
	}

	public final boolean remove(K key, V value) {
		V old = getValue().get(key);
		if (value.equals(old)) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
			return true;
		}
		return false;
	}

	public final void clear() {
		for (var ks : getValue().keySet()) {
			replaced.remove(ks);
			removed.add(ks);
		}
		setValue(Persistents.map());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		bb.WriteUInt(replaced.size());
		for (var e : replaced.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			//noinspection DataFlowIssue
			valueEncoder.accept(bb, e.getValue());
		}

		bb.WriteUInt(removed.size());
		for (K k : removed)
			keyEncoder.accept(bb, k);
	}

	@Override
	public void decode(IByteBuffer bb) {
		replaced.clear();
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			K k = keyDecoder.apply(bb);
			//noinspection DataFlowIssue
			V v = valueDecoder.apply(bb);
			replaced.put(k, v);
		}

		removed.clear();
		for (int i = bb.ReadUInt(); i > 0; --i)
			removed.add(keyDecoder.apply(bb));
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		Log log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(LogMap1<K, V> another) {
		for (var e : another.replaced.entrySet()) {
			K k = e.getKey();
			removed.remove(k);
			replaced.put(k, e.getValue());
		}
		for (K k : another.removed) {
			replaced.remove(k);
			removed.add(k);
		}
	}

	@Override
	public Log beginSavepoint() {
		return new LogMap1<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" replaced:");
		ByteBuffer.BuildSortedString(sb, replaced);
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
