package Zeze.Transaction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
 */
public final class ChangeListenerMap {
	private final HashSet<ChangeListener> set = new HashSet<>();
	private volatile Set<ChangeListener> setCopy; // unmodifiable copy

	public synchronized void addListener(ChangeListener listener) {
		if (set.add(listener))
			setCopy = null;
	}

	public synchronized void removeListener(ChangeListener listener) {
		if (set.remove(listener))
			setCopy = null;
	}

	public Set<ChangeListener> getListeners() {
		var tmp = setCopy;
		if (tmp == null) {
			synchronized (this) {
				tmp = setCopy;
				if (tmp == null)
					setCopy = tmp = Set.copyOf(set);
			}
		}
		return tmp;
	}

	public boolean hasListener() {
		return !getListeners().isEmpty();
	}
}
