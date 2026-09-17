package Zeze.Component;

import java.util.concurrent.CompletionException;
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
import Zeze.Util.TaskSpec;
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
		keepAliveTask = TaskSpec.ofAction(this::keepAlive).schedulePeriodNow(10_000, 10_000);
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

	/** 客户端rpc超时=max(timeoutMs+1000, 5000)（FND5-22）：long运算防timeoutMs+1000
	 * 回绕为负被max取走下限（客户端5秒假超时，服务端仍持锁30分钟）；SendForWait超时
	 * 参数为int，钳制上限。 */
	private static int rpcTimeoutMs(int timeoutMs) {
		//noinspection MathClampMigration
		return (int)Math.min(Math.max((long)timeoutMs + 1000, 5000), Integer.MAX_VALUE);
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
			var timeout = rpcTimeoutMs(timeoutMs);
			try {
				r.SendForWait(service.GetSocket(), timeout).await();
			} catch (CompletionException e) {
				// FND7-64：客户端rpc超时＝应答迟到或丢失，服务端可能已授予该锁。只要客户端进程
				// 活着，keepAlive每10s刷新服务端activeTime，timeoutRelease永不触发——授予的锁
				// 无人unlock，无限期悬挂（同globalThreadId重试还会holdCount累积）。按未获锁继续，
				// 并对同lockName补发unlock（fire-and-forget）：补偿与tryLock同连接，服务端
				// SimulateThread串行处理，必在tryLock决策之后执行；未真获锁时服务端对无条目
				// 幂等应答0（ThreadingServer.ProcessMutexUnlockRequest），无害。
				if (!r.isTimeout())
					throw e;
				var un = new MutexUnlock();
				un.Argument.setLockName(lockName);
				if (!un.Send(service.GetSocket()))
					logger.warn("compensating unlock send fail, {}", lockName);
				return false;
			}
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
			var timeout = rpcTimeoutMs(timeoutMs);
			try {
				r.SendForWait(service.GetSocket(), timeout).await();
			} catch (CompletionException e) {
				// VB①（FND7-64同型）：客户端rpc超时＝应答迟到或丢失，服务端可能已发放permits
				// （semaphoreRefs记账+信号量真实扣减）。只要客户端进程活着，keepAlive每10s刷新
				// 服务端activeTime，timeoutRelease永不触发——已发放的许可无人release，永久短缺。
				// 按未获取继续，并对同lockName补发release（fire-and-forget）：与tryAcquire同连接，
				// 服务端SimulateThread串行处理必在acquire决策之后；未真获取时服务端对无条目
				// 幂等应答0（ThreadingServer.ProcessSemaphoreReleaseRequest），无害。
				// permits<=0被服务端入队前校验立即拒绝（ResultCodeInvalidArgument），不存在
				// "已发放"形态，且release(<=0)本身非法，不补偿。
				if (!r.isTimeout())
					throw e;
				if (permits > 0) {
					var un = new SemaphoreRelease();
					un.Argument.setLockName(lockName);
					un.Argument.setPermits(permits);
					if (!un.Send(service.GetSocket()))
						logger.warn("compensating semaphore release send fail, {}", lockName);
				}
				return false;
			}
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
			// 结果码约定：0=成功且无持有者，>0=成功后剩余持有量，-1=参数非法（重复release也可能得到负持有量）。
			var rc = r.getResultCode();
			if (rc < 0)
				logger.error("release error={}", IModule.getErrorCode(rc));
			else if (rc == 0)
				logger.info("release success, {} permits=0", lockName); // 无持有者
			else
				logger.info("release success, {} permits={}", lockName, rc);
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
			var timeout = rpcTimeoutMs(timeoutMs);
			try {
				r.SendForWait(service.GetSocket(), timeout).await();
			} catch (CompletionException e) {
				// VB①（FND7-64同型）：客户端rpc超时＝应答迟到或丢失，服务端可能已enter成功
				// （rwLockRefs记账+读写锁真实持有）。只要客户端进程活着，keepAlive每10s刷新
				// 服务端activeTime，timeoutRelease永不触发——持有的读/写锁无人exit，永久悬挂。
				// 按未进入继续，并对同lockName补发同模式exit（fire-and-forget）：与enter同连接，
				// 服务端SimulateThread串行处理必在enter决策之后；未真进入时服务端对无条目
				// 幂等应答0（ThreadingServer.ProcessReadWriteLockOperateRequest），无害。
				// tryOperate仅由tryEnterRead/tryEnterWrite调用，补偿按进入模式对称映射exit。
				if (!r.isTimeout())
					throw e;
				var un = new ReadWriteLockOperate();
				un.Argument.setLockName(lockName);
				un.Argument.setOperateType(operateType == eEnterRead ? eExitRead : eExitWrite);
				if (!un.Send(service.GetSocket()))
					logger.warn("compensating rwlock exit send fail, {}", lockName);
				return false;
			}
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
				// 如实标注为结果码（残留P3：原日志把结果码误标注成hold字段）。
				// 结果码语义见ThreadingServer：>0=exit后剩余hold计数，-1=参数/模式不匹配等错误。
				logger.debug("exit {} result code={}", operateType, IModule.getErrorCode(r.getResultCode()));
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
