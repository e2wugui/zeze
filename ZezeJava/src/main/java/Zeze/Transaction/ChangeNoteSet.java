package Zeze.Transaction;

import java.util.*;

public final class ChangeNoteSet<K> extends ChangeNote {
	private HashSet<K> Added = new HashSet<K> ();
	public HashSet<K> getAdded() {
		return Added;
	}
	private HashSet<K> Removed = new HashSet<K> ();
	public HashSet<K> getRemoved() {
		return Removed;
	}

	private Zeze.Transaction.Collections.PSet<K> Set;
	@Override
	public Bean getBean() {
		return Set;
	}

	public ChangeNoteSet(Zeze.Transaction.Collections.PSet<K> set) {
		Set = set;
	}

	public void LogAdd(K key) {
		getAdded().add(key);
		getRemoved().remove(key);
	}

	public void LogRemove(K key) {
		getRemoved().add(key);
		getAdded().remove(key);
	}

	@Override
	public void Merge(ChangeNote other) {
		@SuppressWarnings("unchecked")
		ChangeNoteSet<K> another = (ChangeNoteSet<K>)other;
		// Put,Remove 需要确认有没有顺序问题
		// this: add 1,3 remove 2,4 nest: add 2 remove 1
		for (var e : another.getAdded()) {
			LogAdd(e); // replace 1,2,3 remove 4
		}
		for (var e : another.getRemoved()) {
			LogRemove(e); // replace 2,3 remove 1,4
		}
	}
}