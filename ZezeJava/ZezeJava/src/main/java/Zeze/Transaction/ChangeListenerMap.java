package Zeze.Transaction;

import java.util.HashSet;
import Zeze.Util.IntHashMap;

/**
 * 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
 */
public final class ChangeListenerMap {
	private final IntHashMap<HashSet<ChangeListener>> map = new IntHashMap<>();
	public volatile IntHashMap<HashSet<ChangeListener>> mapCopy = new IntHashMap<>();

	public synchronized void AddListener(int variableId, ChangeListener listener) {
		var set = map.computeIfAbsent(variableId, k -> new HashSet<>());
		if (!set.add(listener))
			throw new IllegalArgumentException();
		MapCopyDeep();
	}

	public synchronized void RemoveListener(int variableId, ChangeListener listener) {
		var set = map.remove(variableId);
		if (set != null) {
			boolean changed = set.remove(listener);
			if (set.isEmpty())
				map.remove(variableId);
			if (changed)
				MapCopyDeep();
		}
	}

	// under lock
	private void MapCopyDeep() {
		var copy = new IntHashMap<HashSet<ChangeListener>>();
		map.foreach((k, v) -> copy.put(k, new HashSet<>(v)));
		mapCopy = copy;
	}

	public boolean HasListener() {
		return !mapCopy.isEmpty();
	}

	public boolean HasListener(int variableId) {
		return mapCopy.containsKey(variableId);
	}
}
