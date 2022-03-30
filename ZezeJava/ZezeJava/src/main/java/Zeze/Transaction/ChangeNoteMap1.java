package Zeze.Transaction;

import java.util.*;

/**
 不精确的 Map 改变细节通告。改变分成两个部分访问：Replaced（add or put）Removed。
 直接改变 Map.Value 的数据细节保存在内部变量 ChangedValue 中，需要调用 MergeChangedToReplaced 合并到 Replaced 中。
 1. 由于添加以后再删除，Removed 这里可能存在一开始不存在的项。

 <typeparam name="K"></typeparam>
 <typeparam name="V"></typeparam>
*/
public class ChangeNoteMap1<K, V> extends ChangeNote {
	private final HashMap<K, V> Replaced = new HashMap<> ();
	public final HashMap<K, V> getReplaced() {
		return Replaced;
	}
	private final HashSet<K> Removed = new HashSet<> ();
	public final HashSet<K> getRemoved() {
		return Removed;
	}

	private Zeze.Transaction.Collections.PMap<K, V> Map;
	protected final Zeze.Transaction.Collections.PMap<K, V> getMap() {
		return Map;
	}
	protected final void setMap(Zeze.Transaction.Collections.PMap<K, V> value) {
		Map = value;
	}

	@Override
	public Bean getBean() {
		return getMap();
	}

	public ChangeNoteMap1(Zeze.Transaction.Collections.PMap<K, V> map) {
		setMap(map);
	}

	/** 由于不需要 Note 来支持回滚，这里只保留最新的改变即可。
	*/
	public final void LogPut(K key, V value) {
		getReplaced().put(key, value);
		getRemoved().remove(key);
	}

	/** 由于不需要 Note 来支持回滚，这里只保留最新的改变即可。
	*/
	public final void LogRemove(K key) {
		getRemoved().add(key);
		getReplaced().remove(key);
	}

	@Override
	public void Merge(ChangeNote note) {
		@SuppressWarnings("unchecked")
		ChangeNoteMap1<K, V> another = (ChangeNoteMap1<K, V>)note;
		// Put,Remove 需要确认有没有顺序问题
		// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
		for (var e : another.getReplaced().entrySet()) {
			LogPut(e.getKey(), e.getValue()); // replace 1,2,3 remove 4
		}
		for (var e : another.getRemoved()) {
			LogRemove(e); // replace 2,3 remove 1,4
		}
	}
}
