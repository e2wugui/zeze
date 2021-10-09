package Zeze.Transaction;

import Zeze.*;
import java.util.*;

/** 
 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
*/
public final class ChangeListenerMap {
	private HashMap<Integer, HashSet<ChangeListener>> map = new HashMap<Integer, HashSet<ChangeListener>>();
	public volatile HashMap<Integer, HashSet<ChangeListener>> mapCopy = new HashMap<Integer, HashSet<ChangeListener>>();

	public void AddListener(int variableId, ChangeListener listener) {
		synchronized (this) {
			HashSet<ChangeListener> set;
			if (false == (map.containsKey(variableId) && (set = map.get(variableId)) == set)) {
				set = new HashSet<ChangeListener>();
				map.put(variableId, set);
			}
			if (false == set.add(listener)) {
				throw new IllegalArgumentException();
			}
			MapCopyDeep();
		}
	}

	public void RemoveListener(int variableId, ChangeListener listener) {
		synchronized (this) {
			TValue set;
			if (map.containsKey(variableId) && (set = map.get(variableId)) == set) {
				boolean changed = set.Remove(listener);
				if (set.Count == 0) {
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
		HashMap<Integer, HashSet<ChangeListener>> copy = new HashMap<Integer, HashSet<ChangeListener>>();
		for (var e : map.entrySet()) {
			HashSet<ChangeListener> set = new HashSet<ChangeListener>();
			for (var s : e.getValue()) {
				set.add(s);
			}
			copy.put(e.getKey(), set);
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