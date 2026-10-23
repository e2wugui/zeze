package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/** PSet1 的变更日志：以 added/removed 两个增量集合记录事务内对持久化 Set 的修改。 */
final class LogSet1Delta<V> extends LogSet<V> {

	private @Nullable Set<V> added;
	private @Nullable Set<V> removed;

	LogSet1Delta(PSet<V> source, org.pcollections.PSet<V> value) {
		super(source, value);
	}

	@Nullable Set<V> getAdded() {
		return added;
	}

	@Nullable Set<V> getRemoved() {
		return removed;
	}

	private Set<V> addedForWrite(int expectedSize) {
		return added != null ? added : (added = HashSet.newHashSet(expectedSize));
	}

	private Set<V> removedForWrite(int expectedSize) {
		return removed != null ? removed : (removed = HashSet.newHashSet(expectedSize));
	}

	@Override
	public boolean add(V item) {
		if (!super.add(item))
			return false;
		if (removed != null) removed.remove(item);
		addedForWrite(1).add(item);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		var old = getValue();
		if (!super.addAll(c))
			return false;
		for (V item : c) {
			if (!old.contains(item)) {
				if (removed != null) removed.remove(item);
				addedForWrite(c.size()).add(item);
			}
		}
		return true;
	}

	@Override
	public boolean remove(V item) {
		if (!super.remove(item))
			return false;
		if (added != null) added.remove(item);
		removedForWrite(1).add(item);
		return true;
	}

	@Override
	public boolean removeAll(Collection<? extends V> c) {
		var old = getValue();
		if (!super.removeAll(c))
			return false;
		for (V item : c) {
			if (old.contains(item)) {
				if (added != null) added.remove(item);
				removedForWrite(Math.min(c.size(), old.size())).add(item);
			}
		}
		return true;
	}

	public void clear() {
		var exist = getValue();
		if (added != null) added.removeAll(exist);
		if (!exist.isEmpty()) removedForWrite(exist.size()).addAll(exist);
		super.clear();
	}

	private void merge(LogSet1Delta<V> from) {
		if (from.added != null && !from.added.isEmpty()) {
			var target = addedForWrite(from.added.size());
			for (V v : from.added) {
				if (removed != null) removed.remove(v);
				target.add(v);
			}
		}
		if (from.removed != null && !from.removed.isEmpty()) {
			var target = removedForWrite(from.removed.size());
			for (V v : from.removed) {
				if (added != null) added.remove(v);
				target.add(v);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection child) {
		super.mergeFrom(child);
		merge((LogSet1Delta<V>)child);
	}

	@Override
	public boolean hasDelta() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogSet1Delta<V> beginSavepoint() {
		return new LogSet1Delta<>((PSet<V>)source, getValue());
	}
}
