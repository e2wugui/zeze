package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
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

	public LogMap1(@NotNull Meta2<K, V> meta, @NotNull org.pcollections.PMap<K, V> value) {
		super(value);
		this.meta = meta;
	}

	public LogMap1(@NotNull Class<K> keyClass, Class<V> valueClass) {
		super(Empty.map()); // not used
		this.meta = Meta2.getMap1Meta(keyClass, valueClass);
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
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
		var exist = getValue().get(key);
		setValue(getValue().plus(key, value));
		replaced.put(key, value);
		removed.remove(key);
		return exist;
	}

	public final void putAll(@NotNull Map<? extends K, ? extends V> m) {
		var newMap = getValue().plusAll(m);
		if (newMap != getValue()) {
			setValue(newMap);
			for (var e : m.entrySet()) {
				replaced.put(e.getKey(), e.getValue());
				removed.remove(e.getKey());
			}
		}
	}

	public final @Nullable V remove(@NotNull K key) {
		var old = getValue().get(key);
		if (null != old) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
		}
		return old;
	}

	public final boolean remove(@NotNull K key, @NotNull V value) {
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
		setValue(Empty.map());
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(replaced.size());
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		for (var p : replaced.entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			valueEncoder.accept(bb, p.getValue());
		}

		bb.WriteUInt(removed.size());
		for (var r : removed)
			keyEncoder.accept(bb, r);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		replaced.clear();
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
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
	public void endSavepoint(@NotNull Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(@NotNull LogMap1<K, V> another) {
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
	public @NotNull Log beginSavepoint() {
		var dup = new LogMap1<>(meta, getValue());
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		return dup;
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, replaced);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
