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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

public class LogMap1<K, V> extends LogMap<K, V> {
	protected final @NotNull Meta2<K, V> meta;
	private final HashMap<K, V> replaced = new HashMap<>();
	private final Set<K> removed = new HashSet<>();

	public LogMap1(Bean belong, int varId, Bean self, @NotNull org.pcollections.PMap<K, V> value,
				   @NotNull Meta2<K, V> meta) {
		super(belong, varId, self, value);
		this.meta = meta;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public @NotNull String getTypeName() {
		return meta.name;
	}

	public final @NotNull HashMap<K, V> getReplaced() {
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
		var newMap = getValue().plusAll(m);
		if (newMap != getValue()) {
			setValue(newMap);
			for (var e : m.entrySet()) {
				K k = e.getKey();
				removed.remove(k);
				replaced.put(k, e.getValue());
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
		setValue(Empty.map());
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(replaced.size());
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
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
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(@NotNull LogMap1<K, V> another) {
		// put,remove 需要确认有没有顺序问题
		// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
		for (var e : another.replaced.entrySet()) {
			// replace 1,2,3 remove 4
			K k = e.getKey();
			removed.remove(k);
			replaced.put(k, e.getValue());
		}
		for (K k : another.removed) {
			// replace 2,3 remove 1,4
			replaced.remove(k);
			removed.add(k);
		}
	}

	@Override
	public @NotNull Log beginSavepoint() {
		return new LogMap1<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
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
