package Zeze.Component;

import java.util.concurrent.Future;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Builtin.Threading.MutexUnlock;
import Zeze.Builtin.Threading.ReadWriteLockOperate;
import Zeze.Builtin.Threading.SemaphoreCreate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.IModule;
import Zeze.Net.Service;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Threading extends AbstractThreading {
	static final Logger logger = LogManager.getLogger(Threading.class);

	public final Service service;
	private final int serverId;
	private final long appSerialId;
	private final Future<?> keepAliveTask;

	public Threading(Service service, int serverId) {
		this.service = service;
		this.serverId = serverId;
		// 这里使用Agent.ServiceName联合serverId当作进程名字，目前足够区分不同的进程。
		this.appSerialId = PersistentAtomicLong.getOrAdd(service.getName() + "." + serverId).next();

		keepAlive(); // first keepAlive
		keepAliveTask = Task.scheduleUnsafe(10_000, 10_000, this::keepAlive);
	}

	public void close() {
		keepAliveTask.cancel(false);
	}

	private void keepAlive() {
		var p = new KeepAlive();
		p.Argument.setServerId(serverId);
		p.Argument.setAppSerialId(appSerialId);
		p.Send(service.GetSocket());
	}

	@SuppressWarnings("deprecation")
	private static long curThreadId() {
		return Thread.currentThread().getId();
	}

	// 即使相同的名字，每个线程调用createMutex也是创建新的实例。
	// 多个线程共享一个实例也是可以的。
	public class Mutex {
		private final String name;

		Mutex(String name) {
			this.name = name;
		}

		public boolean tryLock() {
			return tryLock(0);
		}

		public boolean tryLock(int timeoutMs) {
			var r = new MutexTryLock();
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setTimeoutMs(timeoutMs);
			var timeout = Math.max(timeoutMs + 1000, 5000);
			r.SendForWait(service.GetSocket(), timeout).await();
			return r.getResultCode() == 0;
		}

		public void unlock() {
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);

			// 完美方案应该unlock成功以后才释放。这里先这样写了。
			var r = new MutexUnlock();
			r.Argument.setLockName(lockName);
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() < 0)
				logger.error("unlock error={}", IModule.getErrorCode(r.getResultCode()));
			if (r.getResultCode() == 0)
				logger.debug("unlock success, {}", lockName);
		}
	}

	public Mutex openMutex(String name) {
		return new Mutex(name);
	}

	public class Semaphore {
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
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.Argument.setTimeoutMs(timeoutMs);
			var timeout = Math.max(timeoutMs + 1000, 5000);
			r.SendForWait(service.GetSocket(), timeout).await();
			return r.getResultCode() == 0;
		}

		public void release() {
			release(1);
		}

		public void release(int permits) {
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);

			// 完美方案应该unlock成功以后才释放。这里先这样写了。
			var r = new SemaphoreRelease();
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() <= 0)
				logger.info("release success, {} permits={}", lockName, r.getResultCode());
		}

		void create(int permits) {
			var r = new SemaphoreCreate();
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() != 0)
				throw new IllegalStateException("create error=" + IModule.getErrorCode(r.getResultCode()));
		}
	}

	/**
	 * 创建信号量。
	 * 其中参数permits只有第一次创建的时候才会被使用。
	 * 比较建议的使用方式是只使用 createSemaphore 初始化一次，然后共享返回的变量。
	 * 如果不保存返回值，后面建议使用 openSemaphore 继续访问这个信号量。
	 *
	 * @param name    semaphore name
	 * @param permits initial permits
	 * @return created semaphore
	 */
	public Semaphore createSemaphore(String name, int permits) {
		var semaphore = new Semaphore(name);
		semaphore.create(permits);
		return semaphore;
	}

	public Semaphore openSemaphore(String name) {
		return new Semaphore(name);
	}

	public class ReadWriteLock {
		private final String name;

		ReadWriteLock(String name) {
			this.name = name;
		}

		private boolean tryOperate(int timeoutMs, int operateType) {
			var r = new ReadWriteLockOperate();
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setOperateType(operateType);
			r.Argument.setTimeoutMs(timeoutMs);
			var timeout = Math.max(timeoutMs + 1000, 5000);
			r.SendForWait(service.GetSocket(), timeout).await();
			return r.getResultCode() == 0;
		}

		public boolean tryEnterRead() {
			return tryOperate(0, eEnterRead);
		}

		public boolean tryEnterRead(int timeoutMs) {
			return tryOperate(timeoutMs, eEnterRead);
		}

		public boolean tryEnterWrite() {
			return tryOperate(0, eEnterWrite);
		}

		public boolean tryEnterWrite(int timeoutMs) {
			return tryOperate(timeoutMs, eEnterWrite);
		}

		private void exitOperate(int operateType) {
			var r = new ReadWriteLockOperate();
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setOperateType(operateType);
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.debug("exit {} hold={}", operateType, IModule.getErrorCode(r.getResultCode()));
		}

		public void exitRead() {
			exitOperate(eExitRead);
		}

		public void exitWrite() {
			exitOperate(eExitWrite);
		}
	}

	public ReadWriteLock openReadWriteLock(String name) {
		return new ReadWriteLock(name);
	}
}
