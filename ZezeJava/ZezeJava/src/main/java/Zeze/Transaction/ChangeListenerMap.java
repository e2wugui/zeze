package Zeze.Transaction;

import java.util.HashSet;

/**
 * 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
 */
public final class ChangeListenerMap {
	private final HashSet<ChangeListener> set = new HashSet<>();
	public volatile HashSet<ChangeListener> setCopy;

	public synchronized void AddListener(ChangeListener listener) {
		if (set.add(listener))
			setCopy = null;
	}

	public synchronized void RemoveListener(ChangeListener listener) {
		if (set.remove(listener))
			setCopy = null;
	}

	public HashSet<ChangeListener> getListeners() {
		var tmp = setCopy;
		if (tmp == null) {
			synchronized (this) {
				tmp = setCopy;
				if (tmp == null)
					setCopy = tmp = new HashSet<>(set);
			}
		}
		return tmp;
	}

	public boolean HasListener() {
		return !getListeners().isEmpty();
	}
}
