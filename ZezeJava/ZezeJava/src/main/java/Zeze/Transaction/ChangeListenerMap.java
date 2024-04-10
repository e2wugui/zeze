package Zeze.Transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
 */
public final class ChangeListenerMap extends ReentrantLock {
	private final HashSet<@NotNull ChangeListener> set = new HashSet<>();
	private volatile @Nullable Set<@NotNull ChangeListener> setCopy; // unmodifiable copy

	public void addListener(@NotNull ChangeListener listener) {
		lock();
		try {
			if (set.add(listener))
				setCopy = null;
		} finally {
			unlock();
		}
	}

	public void removeListener(@NotNull ChangeListener listener) {
		lock();
		try {
			if (set.remove(listener))
				setCopy = null;
		} finally {
			unlock();
		}
	}

	public @NotNull Set<@NotNull ChangeListener> getListeners() {
		var tmp = setCopy;
		if (tmp == null) {
			lock();
			try {
				tmp = setCopy;
				if (tmp == null)
					setCopy = tmp = Set.copyOf(set);
			} finally {
				unlock();
			}
		}
		return tmp;
	}

	public boolean hasListener() {
		return !getListeners().isEmpty();
	}
}
