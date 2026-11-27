package Zeze.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * 分布式线程同步组件客户端：跨进程的 Mutex/Semaphore/ReadWriteLock（服务端为 ThreadingServer）。
 */
public class Threading extends AbstractThreading {
	static final Logger logger = LogManager.getLogger(Threading.class);

	public final Service service;
	private final int serverId;
	private final long appSerialId;
	private final Future<?> keepAliveTask;
	private final ServiceDomain serviceDomain;

	private record ServiceDomain(String name, List<String> targets) {
		static ServiceDomain of(Service service) {
			// 使用配置目标而非当前socket：同配置的本地包装共享，重连不改变域。
			// 调用者应在构造Threading前配置服务；手动连接服务沿用服务名作为命名空间。
			var targets = new ArrayList<String>();
			service.getConfig().forEachConnector(connector -> targets.add(connector.getName()));
			targets.sort(String::compareTo);
			return new ServiceDomain(service.getName(), List.copyOf(targets));
		}
	}

	private record SemaphoreHoldKey(ServiceDomain domain, int serverId, long threadId, String name) {
	}

	public Threading(Service service, int serverId) {
		this.service = service;
		this.serverId = serverId;
		serviceDomain = ServiceDomain.of(service);
		// 这里使用Agent.ServiceName联合serverId当作进程名字，目前足够区分不同的进程。
		this.appSerialId = PersistentAtomicLong.getOrAdd(service.getName() + "." + serverId).next();

		keepAlive();
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

	// 信号量本地持有计数：仅在计数为 0 时对超时 tryAcquire 补发补偿 release，避免误释放先前已持有的许可
	private static final ConcurrentHashMap<SemaphoreHoldKey, AtomicInteger> semaphoreLocalHolds = new ConcurrentHashMap<>();

	private SemaphoreHoldKey semaphoreHoldKey(String name) {
		return new SemaphoreHoldKey(serviceDomain, serverId, curThreadId(), name);
	}

	/** 客户端 RPC 超时：使用 long 计算 max(timeoutMs + 1000, 5000) 并钳制到 int 上限防溢出回绕。 */
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
				// RPC 超时时服务端可能已授锁；同连接补发幂等 unlock 防止进程存活期间锁永久悬挂
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

			// 完美方案应该unlock成功以后才释放。
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
				// RPC 超时时服务端可能已发放许可；仅 permits>0 且本地持有计数为 0 时补发幂等 release，避免误释已有许可
				if (!r.isTimeout())
					throw e;
				if (permits > 0) {
					var holds = semaphoreLocalHolds.get(semaphoreHoldKey(name));
					if (holds == null || holds.get() == 0) {
						var un = new SemaphoreRelease();
						un.Argument.setLockName(lockName);
						un.Argument.setPermits(permits);
						if (!un.Send(service.GetSocket()))
							logger.warn("compensating semaphore release send fail, {}", lockName);
					} else {
						logger.warn("skip compensating semaphore release: would release held permits, "
								+ "lockName={}, held={}, permits={}", lockName, holds.get(), permits);
					}
				}
				return false;
			}
			var acquired = r.getResultCode() == 0;
			if (acquired && permits > 0)
				semaphoreLocalHolds.computeIfAbsent(semaphoreHoldKey(name),
						__ -> new AtomicInteger()).addAndGet(permits);
			return acquired;
		}

		public void release() {
			release(1);
		}

		public void release(int permits) {
			var globalThreadId = new BGlobalThreadId(serverId, curThreadId());
			var lockName = new BLockName(globalThreadId, name);

			// 完美方案应该unlock成功以后才释放。
			var r = new SemaphoreRelease();
			r.Argument.setLockName(lockName);
			r.Argument.setPermits(permits);
			r.SendForWait(service.GetSocket()).await();
			// 结果码约定：0=成功且无持有者，>0=剩余持有量，-1=参数非法
			var rc = r.getResultCode();
			if (rc < 0)
				logger.error("release error={}", IModule.getErrorCode(rc));
			else {
				// 服务端确认释放后才扣减本地计数；归零时条件删除防静态 map 累积泄漏
				var key = semaphoreHoldKey(name);
				var holds = semaphoreLocalHolds.get(key);
				if (holds != null && holds.updateAndGet(v -> Math.max(0, v - permits)) == 0)
					semaphoreLocalHolds.remove(key, holds);
				if (rc == 0)
					logger.info("release success, {} permits=0", lockName);
				else
					logger.info("release success, {} permits={}", lockName, rc);
			}
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
	 * 创建信号量（permits 仅首次创建生效）；建议创建后共享引用，否则后续使用 openSemaphore 访问。
	 *
	 * @param name    信号量名称
	 * @param permits 初始许可数
	 * @return 信号量句柄
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
				// RPC 超时时服务端可能已加锁；同连接补发对称的幂等 exit 防止锁悬挂
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
				// 结果码：>0 表示剩余持有计数，-1 表示参数或模式不匹配
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
