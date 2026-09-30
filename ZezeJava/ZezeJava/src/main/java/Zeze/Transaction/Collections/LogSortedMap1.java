package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;
import org.pcollections.TreePMap;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** PSortedMap1 的变更日志：以 replaced/removed 增量记录事务内对持久化有序 Map 的修改。 */
public class LogSortedMap1<K extends Comparable<K>, V> extends LogSortedMap<K, V> {
	protected final @NotNull Meta2<K, V> meta;
	private final Map<K, V> replaced;
	private final Set<K> removed;

	public LogSortedMap1(Bean belong, int varId, Bean self, @NotNull org.pcollections.PSortedMap<K, V> value,
	               @NotNull Meta2<K, V> meta) {
		super(belong, varId, self, value);
		this.meta = meta;
		// 增量与底层有序映射必须使用同一键等价关系（如 Decimal 的 1.0 与 1.00）。
		replaced = new TreeMap<>(value.comparator());
		removed = new TreeSet<>(value.comparator());
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public @NotNull String getTypeName() {
		return meta.name;
	}

	public final @NotNull Map<K, V> getReplaced() {
		return replaced;
	}

	public final @NotNull Set<K> getRemoved() {
		return removed;
	}

	public final @Nullable V get(K key) {
		return getValue().get(key);
	}

	public final void add(@NotNull K key, @NotNull V value) {
		put(key, value);
	}

	public final @Nullable V put(@NotNull K key, @NotNull V value) {
		V exist = getValue().get(key);
		setValue(getValue().plus(key, value));
		removed.remove(key);
		replaced.put(key, value);
		return exist;
	}

	public final void putAll(@NotNull Map<? extends K, ? extends V> m) {
		var old = getValue();
		var newMap = old.plusAll(m);
		if (newMap != old) {
			setValue(newMap);
			// 输入可能包含多个比较器等价的键，必须记最终值而非中间输入值。
			for (K k : m.keySet()) {
				V v = newMap.get(k);
				if (!java.util.Objects.equals(old.get(k), v)) {
					removed.remove(k);
					replaced.put(k, v);
				}
			}
		}
	}

	public final @Nullable V remove(@NotNull K key) {
		V old = getValue().get(key);
		if (old != null) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
		}
		return old;
	}

	public final boolean remove(@NotNull K key, @NotNull V value) {
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
		var comparator = getValue().comparator();
		setValue(comparator != null ? TreePMap.empty(comparator) : Empty.sortedMap());
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
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
	public void decode(@NotNull IByteBuffer bb) {
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
	public void endSavepoint(@NotNull Savepoint currentSp) {
		Log log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogSortedMap1<K, V>)log;
			currentLog.setValue(getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(@NotNull LogSortedMap1<K, V> another) {
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
	public @NotNull Log beginSavepoint() {
		return new LogSortedMap1<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" replaced:");
		ByteBuffer.BuildSortedString(sb, replaced);
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
