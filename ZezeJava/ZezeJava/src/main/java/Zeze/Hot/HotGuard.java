package Zeze.Hot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.jetbrains.annotations.NotNull;

/**
 * 热更守卫锁句柄：构造加锁、close 解锁，用于热更期间阻塞普通任务派发。
 */
public class HotGuard implements AutoCloseable {
	private final Lock lock;

	/**
	 * 未启用热更守卫时的共享占位实例（Task.hotGuard 的默认产物）：
	 * Factory.create() 契约非空，"null=禁用"用无操作哨兵表达
	 * （同 Cache.NullCache 的模式：null 不当哨兵使）。NoopLock 无状态，共享单例并发安全。
	 */
	public static final HotGuard None = new HotGuard(new NoopLock());

	public HotGuard(Lock lock) {
		lock.lock();
		this.lock = lock;
	}

	@Override
	public void close() {
		lock.unlock();
	}

	private static final class NoopLock implements Lock {
		@Override
		public void lock() {
		}

		@Override
		public void lockInterruptibly() {
		}

		@Override
		public boolean tryLock() {
			return true;
		}

		@Override
		public boolean tryLock(long time, @NotNull TimeUnit unit) {
			return true;
		}

		@Override
		public void unlock() {
		}

		@NotNull
		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException();
		}
	}
}
