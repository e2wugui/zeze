package Zeze.Transaction;

import java.util.*;

/** 
 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
*/
public final class ChangeListenerMap {
	private final HashMap<Integer, HashSet<ChangeListener>> map = new HashMap<>();
	public volatile HashMap<Integer, HashSet<ChangeListener>> mapCopy = new HashMap<>();

	public void AddListener(int variableId, ChangeListener listener) {
		synchronized (this) {
			var set = map.computeIfAbsent(variableId, k -> new HashSet<>());
			if (!set.add(listener)) {
				throw new IllegalArgumentException();
			}
			MapCopyDeep();
		}
	}

	public void RemoveListener(int variableId, ChangeListener listener) {
		synchronized (this) {
			var set = map.remove(variableId);
			if (null != set) {
				boolean changed = set.remove(listener);
				if (set.size() == 0) {
					map.remove(variableId);
				}
				if (changed) {
					MapCopyDeep();
				}
			}
		}
	}

	// under lock
	private void MapCopyDeep() {
		HashMap<Integer, HashSet<ChangeListener>> copy = new HashMap<>();
		for (var e : map.entrySet()) {
			copy.put(e.getKey(), new HashSet<>(e.getValue()));
		}
		mapCopy = copy;
	}

	public boolean HasListener() {
		HashMap<Integer, HashSet<ChangeListener>> tmp = mapCopy;
		return !tmp.isEmpty();
	}

	public boolean HasListener(int variableId) {
		HashMap<Integer, HashSet<ChangeListener>> tmp = mapCopy;
		return tmp.containsKey(variableId);
	}
}