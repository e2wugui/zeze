package Zeze.Transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

/**
 * 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。
 * 所有监听器必须在任何业务事务开始前完成注册，业务运行期间不支持新增监听器。
 */
public final class ChangeListenerMap extends ReentrantLock {
	private final HashSet<ChangeListener> set = new HashSet<>();
	private volatile @Nullable Set<ChangeListener> setCopy; // unmodifiable copy

	/**
	 * 在应用初始化阶段、所有业务事务开始前注册监听器。
	 * 应用负责保证注册阶段与业务阶段互斥；本方法的锁只保护监听器集合。
	 */
	public void addListener(ChangeListener listener) {
		var current = Transaction.getCurrent();
		if (current != null && current.isRunning())
			throw new IllegalStateException("change listeners must be registered outside a running transaction");
		lock();
		try {
			if (set.add(listener))
				setCopy = null;
		} finally {
			unlock();
		}
	}

	public void removeListener(ChangeListener listener) {
		lock();
		try {
			if (set.remove(listener))
				setCopy = null;
		} finally {
			unlock();
		}
	}

	public Set<ChangeListener> getListeners() {
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
