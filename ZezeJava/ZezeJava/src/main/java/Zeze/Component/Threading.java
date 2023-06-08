package Zeze.Component;

import java.util.concurrent.ExecutionException;
import Zeze.Application;
import Zeze.Builtin.Threading.BSemaphore;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.OutInt;
import Zeze.Util.Task;

public class Threading extends AbstractThreading {
	private final Application zeze;

	public Threading(Application zeze) {
		this.zeze = zeze;
	}

	public class Mutex {
		private final String name;

		Mutex(String name) {
			this.name = name;
		}

		@SuppressWarnings("BusyWait")
		public void lock() {
			while (true) {
				if (tryLock())
					return;

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@SuppressWarnings("BusyWait")
		public boolean tryLock(long timeoutMs) {
			while (true) {
				if (tryLock())
					return true;

				if (timeoutMs <= 0)
					return false;

				try {
					Thread.sleep(500);
					timeoutMs -= 500; // 低精度
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public boolean tryLock() {
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var mutex = _tMutex.getOrAdd(name);
							if (mutex.isLocked())
								return 1;
							mutex.setLocked(true);
							mutex.setServerId(zeze.getConfig().getServerId());
							mutex.setLockTime(System.currentTimeMillis());
							return 0;
						}, "Threading.Mutex.tryLock"), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public void unlock() {
			try {
				// skip call result! silence!
				Task.runUnsafe(zeze.newProcedure(
						() -> {
							var mutex = _tMutex.get(name);
							if (null != mutex && mutex.isLocked()) {
								mutex.setLocked(false);
							}
							return 0;
						}, "Threading.Mutex.unlock"), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean destroy() {
			// 删除锁。
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var mutex = _tMutex.get(name);
							if (null != mutex && !mutex.isLocked()) {
								_tMutex.remove(name);
								return 0;
							}
							return 1;
						}, "Threading.Mutex.destroy"), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public Mutex createMutex(String name) {
		return new Mutex(name);
	}

	public class Semaphore {
		private final String name;

		Semaphore(String name) {
			this.name = name;
		}

		public void acquire() {
			acquire(1);
		}

		public void release() {
			release(1);
		}

		@SuppressWarnings("BusyWait")
		public void acquire(int permits) {
			while (true) {
				if (tryAcquire(permits))
					return;

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@SuppressWarnings("BusyWait")
		public boolean tryAcquire(int permits, int timeoutMs) {
			while (true) {
				if (tryAcquire(permits))
					return true;

				if (timeoutMs <= 0)
					return false;

				try {
					Thread.sleep(500);
					timeoutMs -= 500;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public boolean tryAcquire() {
			return tryAcquire(1);
		}

		public boolean tryAcquire(int permits) {
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var sem = _tSemaphore.get(name);
							if (null == sem)
								return 1;

							if (sem.getPermits() >= permits) {
								sem.setPermits(sem.getPermits() - permits);
								return 0;
							}

							return 2;
						}, "Threading.Semaphore.tryAcquire"), DispatchMode.Critical).get();

			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public void release(int permits) {
			try {
				// skip result!
				Task.runUnsafe(zeze.newProcedure(
						() -> {
							var sem = _tSemaphore.get(name);
							if (null == sem)
								return 1;

							sem.setPermits(sem.getPermits() + permits);
							return 0;
						}, "Threading.Semaphore.release"),DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean destroy() {
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var sem = _tSemaphore.get(name);
							if (null == sem && sem.getPermits() == initialPermits)
								return
							return 0;
						}, "Threading.Semaphore.destroy"), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 创建信号量
	 * @param name semaphore name
	 * @return semaphore
	 */
	public Semaphore createSemaphore(String name) {
		return new Semaphore(name);
	}

	/**
	 * permits 只有第一次创建的时候才会被初始化到信号量里面。
	 * 这个参数比较讨厌，每个使用的地方最好统一。
	 * api设计成两步了。
	 * @param name name
	 * @param permits permits
	 */
	public void initializeSemaphore(String name, int permits) {
		try {
			if (0 != Task.runUnsafe(zeze.newProcedure(
					() -> {
						_tSemaphore.getOrAdd(name).setPermits(permits);
						return 0;
					}, "Threading.Semaphore.initialize"), DispatchMode.Critical).get())
				throw new RuntimeException("Threading.Semaphore.initialize fail.");
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}


	public class ReadWriteLock {
		private final String name;

		ReadWriteLock(String name) {
			this.name = name;
		}

		/**
		 * 尝试进入读锁，必须匹配exitRead释放。
		 * @return boolean
		 */

		public boolean tryEnterRead() {
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var rw = _tReadWriteLock.getOrAdd(name);
							if (rw.isInWriting())
								return 1;
							rw.setReadingCount(rw.getReadingCount() + 1);
							return 0;
						}, ""), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean tryEnterWrite() {
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var rw = _tReadWriteLock.getOrAdd(name);
							if (rw.isInWriting())
								return 1;
							if (rw.getReadingCount() > 0)
								return 2;
							rw.setInWriting(true);
							return 0;
						}, ""), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public void exitRead() {
			try {
				// skip result.
				Task.runUnsafe(zeze.newProcedure(
						() -> {
							var rw = _tReadWriteLock.getOrAdd(name);
							if (rw.isInWriting())
								return 1;
							if (rw.getReadingCount() <= 0)
								return 2;
							rw.setReadingCount(rw.getReadingCount() - 1);
							return 0;
						}, ""), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public void exitWrite() {
			try {
				// skip result.
				Task.runUnsafe(zeze.newProcedure(
						() -> {
							var rw = _tReadWriteLock.getOrAdd(name);
							if (!rw.isInWriting())
								return 1;
							if (rw.getReadingCount() > 0)
								return 2;
							rw.setInWriting(false);
							return 0;
						}, ""), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean destroy() {
			try {
				return 0 == Task.runUnsafe(zeze.newProcedure(
						() -> {
							var rw = _tReadWriteLock.get(name);
							if (null != rw && !rw.isInWriting() && rw.getReadingCount() == 0) {
								_tReadWriteLock.remove(name);
								return 0;
							}
							return 1;
						}, ""), DispatchMode.Critical).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public ReadWriteLock createReadWriteLock(String name) {
		return new ReadWriteLock(name);
	}
}
