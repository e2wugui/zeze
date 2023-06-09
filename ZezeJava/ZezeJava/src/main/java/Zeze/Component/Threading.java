package Zeze.Component;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Builtin.Threading.MutexUnlock;
import Zeze.Builtin.Threading.QueryLockInfo;
import Zeze.Builtin.Threading.SemaphoreCreate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.IModule;
import Zeze.Net.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Threading extends AbstractThreading {
	static final Logger logger = LogManager.getLogger(Threading.class);

	public Service service;
	private int serverId;

	public Threading(Service service, int progressId) {
		this.service = service;
		this.serverId = progressId;
	}

	@Override
	protected long ProcessQueryLockInfoRequest(QueryLockInfo r) throws Exception {
		for (var lockName : r.Argument.getLockNames())
			if (!acquired.containsKey(lockName))
				r.Result.getLockNames().add(lockName);
		r.SendResult();
		return 0;
	}

	public ConcurrentHashMap<BLockName, Concurrent> acquired = new ConcurrentHashMap<>();

	public interface Concurrent {
		// 所有Threading并发控制机制的基类。
		// 以后可能定义公共方法。
	}

	// 即使相同的名字，每个线程调用createMutex也是创建新的实例。
	// 多个线程共享一个实例也是可以的。
	public class Mutex implements Concurrent {
		private final String name;

		Mutex(String name) {
			this.name = name;
		}

		public boolean tryLock() {
			return tryLock(0);
		}

		public boolean tryLock(int timeoutMs) {
			var r = new MutexTryLock();
			var globalThreadId = new BGlobalThreadId(serverId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setTimeoutMs(timeoutMs);
			r.SendForWait(service.GetSocket(), timeoutMs + 1000).await();
			if (r.getResultCode() == 0) {
				acquired.put(lockName, this);
				return true;
			}
			return false;
		}

		public void unlock() {
			var globalThreadId = new BGlobalThreadId(serverId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);

			// 完美方案应该unlock成功以后才释放。这里先这样写了。
			if (!acquired.remove(lockName, this))
				return;

			var r = new MutexUnlock();
			r.Argument = lockName;
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.error("unlock error={}", IModule.getErrorCode(r.getResultCode()));
		}
	}

	public Mutex openMutex(String name) {
		return new Mutex(name);
	}

	public class Semaphore implements Concurrent {
		private final String name;

		Semaphore(String name) {
			this.name = name;
		}

		public boolean tryAcquire() {
			return tryAcquire(1, 0);
		}

		public boolean tryAcquire(int timeoutMs) {
			return tryAcquire(1, timeoutMs);
		}

		public boolean tryAcquire(int permits, int timeoutMs) {
			var r = new SemaphoreTryAcquire();
			var globalThreadId = new BGlobalThreadId(serverId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.Argument.setTimeoutMs(timeoutMs);
			r.SendForWait(service.GetSocket(), timeoutMs + 1000).await();
			if (r.getResultCode() == 0) {
				acquired.put(lockName, this);
				return true;
			}
			return false;
		}

		public void release() {
			release(1);
		}

		public void release(int permits) {
			var globalThreadId = new BGlobalThreadId(serverId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);

			// 完美方案应该unlock成功以后才释放。这里先这样写了。
			if (!acquired.remove(lockName, this))
				return;

			var r = new SemaphoreRelease();
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.error("unlock error={}", IModule.getErrorCode(r.getResultCode()));
		}

		void create(int permits) {
			var r = new SemaphoreCreate();
			var globalThreadId = new BGlobalThreadId(serverId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.error("create error={}", IModule.getErrorCode(r.getResultCode()));
		}
	}

	public Semaphore createSemaphore(String name, int permits) {
		var semaphore = new Semaphore(name);
		semaphore.create(permits);
		return semaphore;
	}

	public Semaphore openSemaphore(String name) {
		return new Semaphore(name);
	}
}
