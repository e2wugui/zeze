package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.HashMap;

import org.jspecify.annotations.Nullable;

/**
 * PSet1 的变更日志：以单容器 modified 记录事务内对持久化 Set 的修改，
 * value 为 {@link LogSet1#PRESENT} 表示加入、null 表示删除（同 HashSet 底层 HashMap 惯例）。
 */
final class LogSet1Delta<V> extends LogSet<V> {
	private @Nullable HashMap<V, @Nullable Object> modified;

	LogSet1Delta(PSet<V> source, org.pcollections.PSet<V> value) {
		super(source, value);
	}

	@Nullable HashMap<V, @Nullable Object> getModified() {
		return modified;
	}

	private HashMap<V, @Nullable Object> modifiedForWrite(int expectedSize) {
		return modified != null ? modified : (modified = HashMap.newHashMap(expectedSize));
	}

	@Override
	public boolean add(V item) {
		if (!super.add(item))
			return false;
		modifiedForWrite(1).put(item, LogSet1.PRESENT);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		var old = getValue();
		if (!super.addAll(c))
			return false;
		var m = modifiedForWrite(c.size());
		for (V item : c)
			if (!old.contains(item))
				m.put(item, LogSet1.PRESENT);
		return true;
	}

	@Override
	public boolean remove(V item) {
		if (!super.remove(item))
			return false;
		modifiedForWrite(1).put(item, null);
		return true;
	}

	@Override
	public boolean removeAll(Collection<? extends V> c) {
		var old = getValue();
		if (!super.removeAll(c))
			return false;
		var m = modifiedForWrite(Math.min(c.size(), old.size()));
		for (V item : c)
			if (old.contains(item))
				m.put(item, null);
		return true;
	}

	public void clear() {
		var exist = getValue();
		if (!exist.isEmpty())
			for (V e : exist)
				modifiedForWrite(exist.size()).put(e, null);
		super.clear();
	}

	private void merge(LogSet1Delta<V> from) {
		if (from.modified != null && !from.modified.isEmpty())
			modifiedForWrite(from.modified.size()).putAll(from.modified);
	}

	@Override
	protected void mergeFrom(LogCollection<?> child) {
		super.mergeFrom(child);
		//noinspection unchecked
		merge((LogSet1Delta<V>)child);
	}

	@Override
	public boolean hasDelta() {
		return true;
	}

	@Override
	public LogSet1Delta<V> beginSavepoint() {
		return new LogSet1Delta<>(source, getValue());
	}
}
