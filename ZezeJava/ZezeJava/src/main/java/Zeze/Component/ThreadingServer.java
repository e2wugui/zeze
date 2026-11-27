package Zeze.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BKeepAlive;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.ReadWriteLockOperate;
import Zeze.Builtin.Threading.SemaphoreCreate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Action1;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 分布式线程同步组件服务端：为每个全局线程维护 SimulateThread，仲裁 Mutex/Semaphore/ReadWriteLock。
 */
public class ThreadingServer extends AbstractThreadingServer {
	private static final Logger logger = LogManager.getLogger(ThreadingServer.class);

	// 锁操作 RPC 非法参数或未知操作类型统一返回 -1（正常结果码为非负整数）
	public static final int ResultCodeInvalidArgument = -1;

	private final Service service;
	private final ServiceManagerServer.Conf conf;
	private final HashMap<BGlobalThreadId, SimulateThread> simulateThreads = new HashMap<>();
	private final ConcurrentHashMap<Integer, SimulateThreads> simulateThreadsByServerId = new ConcurrentHashMap<>();

	// 每种锁一个命名空间；SimulateThread 持有自身资源引用并在退出时释放
	private final ConcurrentHashMap<String, ReentrantLock> mutexes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ReentrantReadWriteLock> rwLocks = new ConcurrentHashMap<>();

	private final Future<?> timeoutReleaseTask;
	private volatile boolean closed;

	public ThreadingServer(Service service, ServiceManagerServer.Conf conf) {
		this.service = service;
		this.conf = conf;
		this.timeoutReleaseTask = TaskSpec.ofAction(this::timeoutRelease).schedulePeriodNow(60_000, 60_000);
	}

	public void close() {
		timeoutReleaseTask.cancel(false);
		SimulateThread[] threads;
		lock();
		try {
			closed = true;
			threads = simulateThreads.values().toArray(SimulateThread[]::new);
		} finally {
			unlock();
		}
		for (var thread : threads)
			thread.interrupt(); // 中断带超时的等待获取；锁释放由所属模拟线程自身执行
		awaitTermination(threads);
	}

	private static void awaitTermination(SimulateThread[] threads) {
		boolean interrupted = false;
		for (var thread : threads) {
			if (thread == Thread.currentThread())
				continue;
			while (thread.isAlive()) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	}

	public Service getService() {
		return service;
	}

	static class SemaphoreAcquired {
		final Semaphore semaphore;
		int permits;

		SemaphoreAcquired(Semaphore s) {
			this.semaphore = s;
		}
	}

	// 动作队列元素：rpc 非 null 时在异常或关闭兜底补发 ResultCodeInvalidArgument
	private record SimulateThreadAction(@Nullable Rpc<?, ?> rpc, Action1<SimulateThread> action) {
		void reject() {
			if (rpc != null)
				rpc.trySendResultCode(ResultCodeInvalidArgument);
		}
	}

	public class SimulateThread extends Thread {
		private final BGlobalThreadId id;
		private final LinkedBlockingQueue<SimulateThreadAction> actions = new LinkedBlockingQueue<>();
		private final HashMap<String, ReentrantLock> mutexRefs = new HashMap<>();

		private final HashMap<String, SemaphoreAcquired> semaphoreRefs = new HashMap<>();

		private final HashMap<String, ReentrantReadWriteLock> rwLockRefs = new HashMap<>();

		public SimulateThread(BGlobalThreadId id) {
			this.id = id;
		}

		private boolean acquireNothing() {
			// 新增同步原语须同步加入此处检查，避免持锁线程被误判空闲退出导致锁悬挂
			return mutexRefs.isEmpty() && semaphoreRefs.isEmpty() && rwLockRefs.isEmpty();
		}

		public ReentrantLock getMutex(String name) {
			var mutex = mutexRefs.get(name);
			if (null != mutex)
				return mutex;

			return mutexes.computeIfAbsent(name, (k) -> new ReentrantLock());
		}

		@Nullable SemaphoreAcquired getSemaphore(String name) {
			var semaphoreAcq = semaphoreRefs.get(name);
			if (null != semaphoreAcq)
				return semaphoreAcq;

			var semaphore = semaphores.get(name);
			if (null != semaphore)
				return new SemaphoreAcquired(semaphore);
			return null;
		}

		public ReentrantReadWriteLock getReadWriteLock(String name) {
			var rwLock = rwLockRefs.get(name);
			if (null != rwLock)
				return rwLock;

			return rwLocks.computeIfAbsent(name, (key) -> new ReentrantReadWriteLock());
		}

		@Override
		public void run() {
			try {
				while (!closed) {
					try {
						var action = actions.poll(200, TimeUnit.MILLISECONDS);
						if (null != action) {
							runAction(action);
						}

						if (acquireNothing()) {
							// 无已分配资源时延时 200ms 准备退出
							action = actions.poll(200, TimeUnit.MILLISECONDS);
							if (null != action) {
								runAction(action);
								continue;
							}
							if (simulateThreadExit(id, this))
								break;
						}
					} catch (Exception e) {
						if (!closed)
							logger.error("", e);
					}
				}
			} finally {
				if (!acquireNothing())
					release();
				SimulateThreadAction pending;
				while ((pending = actions.poll()) != null)
					pending.reject();
				simulateThreadExit(id, this);
			}
		}

		// 统一执行动作并在异常或关闭时补发错误码（Rpc.trySendResultCode 内部 CAS 防重复发送）
		private void runAction(SimulateThreadAction a) {
			try {
				if (closed) {
					a.reject();
					return;
				}
				a.action().run(this);
			} catch (Exception e) {
				logger.error("simulate action exception (thread=({}, {}))",
						id.getServerId(), id.getThreadId(), e);
				a.reject();
			}
		}

		public void release() {
			logger.info("timeout(thread=({}, {}))", id.getServerId(), id.getThreadId());
			for (var e : mutexRefs.entrySet()) {
				while (e.getValue().getHoldCount() > 0)
					e.getValue().unlock();
				logger.info("mutex timeout(name={})", e.getKey());
			}
			mutexRefs.clear();

			for (var e : semaphoreRefs.entrySet()) {
				e.getValue().semaphore.release(e.getValue().permits);
				logger.info("semaphore timeout(name={})", e.getKey());
			}
			semaphoreRefs.clear();

			for (var e : rwLockRefs.entrySet()) {
				while (e.getValue().getReadHoldCount() > 0)
					e.getValue().readLock().unlock();
				while (e.getValue().getWriteHoldCount() > 0)
					e.getValue().writeLock().unlock();
				logger.info("rwLock timeout(name={})", e.getKey());
			}
			rwLockRefs.clear();
		}
	}

	public class SimulateThreads {
		private final HashSet<SimulateThread> threads = new HashSet<>();
		// 由模块锁保护，超时判定与释放入队必须在同一临界区
		private long activeTime = System.currentTimeMillis();
		private final int serverId;
		private volatile BKeepAlive.@Nullable Data lastAppSerial;

		public SimulateThreads(int serverId) {
			this.serverId = serverId;
		}

		public int getServerId() {
			return serverId;
		}

		public void release() {
			lock();
			try {
				if (closed)
					return;
				for (var thread : threads)
					thread.actions.offer(new SimulateThreadAction(null, SimulateThread::release));
			} finally {
				unlock();
			}
		}
	}

		// Direct 模式保证同连接 KeepAlive 按接收序处理，配合模块锁保护 lastAppSerial 单调递增接管
		@Override
		@Zeze.Util.DispatchModeAnnotation(mode = Zeze.Transaction.DispatchMode.Direct)
		protected long ProcessKeepAlive(KeepAlive p) {
		var threads = simulateThreadsByServerId.computeIfAbsent(p.Argument.getServerId(), SimulateThreads::new);
		lock();
		try {
			if (null == threads.lastAppSerial) {
				threads.lastAppSerial = p.Argument;
				threads.activeTime = System.currentTimeMillis();
				return 0;
			}
			//noinspection DataFlowIssue
			if (threads.lastAppSerial.getAppSerialId() != p.Argument.getAppSerialId()) {
				// 仅更大 appSerialId 的新实例接管并释放旧资源，忽略迟到的旧实例心跳
				//noinspection DataFlowIssue
				if (p.Argument.getAppSerialId() > threads.lastAppSerial.getAppSerialId()) {
					threads.release();
					threads.lastAppSerial = p.Argument;
					threads.activeTime = System.currentTimeMillis();
				}
				return 0;
			}
			threads.activeTime = System.currentTimeMillis();
			return 0;
		} finally {
			unlock();
		}
	}

	private void timeoutRelease() {
		for (var threads : simulateThreadsByServerId.values()) {
			lock();
			try {
				// 与 KeepAlive 串行化。已经续活或被新实例接管的资源不能按旧时间释放。
				if (System.currentTimeMillis() - threads.activeTime > conf.threadingReleaseTimeout)
					threads.release();
			} finally {
				unlock();
			}
		}
	}

	private boolean simulateThreadExit(BGlobalThreadId id, SimulateThread expected) {
		lock();
		try {
			return null == simulateThreads.computeIfPresent(id, (key, This) -> {
				if (This == expected && This.actions.isEmpty()) {
					logger.info("simulate exit thread=({}, {})", id.getServerId(), id.getThreadId());
					var x = simulateThreadsByServerId.get(This.id.getServerId());
					if (null != x)
						x.threads.remove(This);
					return null;
				}
				return This;
			});
		} finally {
			unlock();
		}
	}

	private void simulateThreadOffer(Rpc<?, ?> rpc, BGlobalThreadId id, Action1<SimulateThread> action) {
		lock();
		try {
			if (closed) {
				rpc.trySendResultCode(ResultCodeInvalidArgument);
				return;
			}
			var st = simulateThreads.computeIfAbsent(
					id,
					(key) -> {
						var simulate = new SimulateThread(key);
						simulateThreadsByServerId.computeIfAbsent(id.getServerId(), SimulateThreads::new)
								.threads.add(simulate);
						simulate.start();
						logger.info("simulate new thread=({}, {})",
								key.getServerId(), key.getThreadId());
						return simulate;
					});
			st.actions.offer(new SimulateThreadAction(rpc, action));
		} finally {
			unlock();
		}
	}

	@Override
	protected long ProcessMutexTryLockRequest(Zeze.Builtin.Threading.MutexTryLock r) {
		logger.info("mutex.tryLock ININININ (thread=({}, {}), name={})",
				r.Argument.getLockName().getGlobalThreadId().getServerId(),
				r.Argument.getLockName().getGlobalThreadId().getThreadId(),
				r.Argument.getLockName().getName());
		if (r.Argument.getTimeoutMs() < 0) {
			r.SendResultCode(ResultCodeInvalidArgument);
			return 0;
		}
		simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
				(This) -> {
					var mutex = This.getMutex(r.Argument.getLockName().getName());
					var locked = mutex.tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
					logger.info("mutex.tryLock(thread=({}, {}), name={}) -> {}",
							r.Argument.getLockName().getGlobalThreadId().getServerId(),
							r.Argument.getLockName().getGlobalThreadId().getThreadId(),
							r.Argument.getLockName().getName(), locked);
					if (locked)
						This.mutexRefs.put(r.Argument.getLockName().getName(), mutex);
					r.SendResultCode(locked ? 0 : 1);
				});

		return 0;
	}

	@Override
	protected long ProcessMutexUnlockRequest(Zeze.Builtin.Threading.MutexUnlock r) {
		simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
				(This) -> {
					var mutex = This.mutexRefs.get(r.Argument.getLockName().getName());
					if (null != mutex) {
						mutex.unlock();
						var hold = mutex.getHoldCount();
						if (0 == hold)
							This.mutexRefs.remove(r.Argument.getLockName().getName());
						logger.info("mutex.unlock(thread=({}, {}), name={}) hold={}",
								r.Argument.getLockName().getGlobalThreadId().getServerId(),
								r.Argument.getLockName().getGlobalThreadId().getThreadId(),
								r.Argument.getLockName().getName(),
								hold);
						r.SendResultCode(hold);
						return;
					}
					r.SendResultCode(0);
				});
		return 0;
	}

	@Override
	protected long ProcessReadWriteLockOperateRequest(ReadWriteLockOperate r) {
		if ((r.Argument.getOperateType() == Threading.eEnterRead
				|| r.Argument.getOperateType() == Threading.eEnterWrite)
				&& r.Argument.getTimeoutMs() < 0) {
			r.SendResultCode(ResultCodeInvalidArgument);
			return 0;
		}
		switch (r.Argument.getOperateType()) {
		case Threading.eEnterRead:
			simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
					(This) -> {
						var rwLock = This.getReadWriteLock(r.Argument.getLockName().getName());
						var locked = rwLock.readLock().tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
						if (locked)
							This.rwLockRefs.put(r.Argument.getLockName().getName(), rwLock);

						logger.info("RWLock.enterRead(thread=({}, {}), name={}) -> {}",
								r.Argument.getLockName().getGlobalThreadId().getServerId(),
								r.Argument.getLockName().getGlobalThreadId().getThreadId(),
								r.Argument.getLockName().getName(),
								locked);
						r.SendResultCode(locked ? 0 : 1);
					});
			break;

		case Threading.eEnterWrite:
			simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
					(This) -> {
						var rwLock = This.getReadWriteLock(r.Argument.getLockName().getName());
						var locked = rwLock.writeLock().tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
						if (locked)
							This.rwLockRefs.put(r.Argument.getLockName().getName(), rwLock);

						logger.info("RWLock.enterWrite(thread=({}, {}), name={}) -> {}",
								r.Argument.getLockName().getGlobalThreadId().getServerId(),
								r.Argument.getLockName().getGlobalThreadId().getThreadId(),
								r.Argument.getLockName().getName(),
								locked);
						r.SendResultCode(locked ? 0 : 1);
					});
			break;

		case Threading.eExitRead:
			simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
					(This) -> {
						var rwLock = This.rwLockRefs.get(r.Argument.getLockName().getName());
						if (null != rwLock) {
							try {
								rwLock.readLock().unlock();
							} catch (IllegalMonitorStateException e) {
								logger.error("RWLock.exitRead mode mismatch (thread=({}, {}), name={})",
										r.Argument.getLockName().getGlobalThreadId().getServerId(),
										r.Argument.getLockName().getGlobalThreadId().getThreadId(),
										r.Argument.getLockName().getName(), e);
								r.SendResultCode(ResultCodeInvalidArgument);
								return;
							}
							var hold = rwLock.getReadHoldCount();
							// 读写锁共享条目（支持写→读降级），须双计数均归零才移除引用
							if (hold == 0 && rwLock.getWriteHoldCount() == 0)
								This.rwLockRefs.remove(r.Argument.getLockName().getName());
							logger.info("RWLock.exitRead(thread=({}, {}), name={} hold={})",
									r.Argument.getLockName().getGlobalThreadId().getServerId(),
									r.Argument.getLockName().getGlobalThreadId().getThreadId(),
									r.Argument.getLockName().getName(),
									hold);

							r.SendResultCode(hold);
							return;
						}
						r.SendResultCode(0);
					});
			break;

		case Threading.eExitWrite:
			simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
					(This) -> {
						var rwLock = This.rwLockRefs.get(r.Argument.getLockName().getName());
						if (null != rwLock) {
							try {
								rwLock.writeLock().unlock();
							} catch (IllegalMonitorStateException e) {
								logger.error("RWLock.exitWrite mode mismatch (thread=({}, {}), name={})",
										r.Argument.getLockName().getGlobalThreadId().getServerId(),
										r.Argument.getLockName().getGlobalThreadId().getThreadId(),
										r.Argument.getLockName().getName(), e);
								r.SendResultCode(ResultCodeInvalidArgument);
								return;
							}
							var hold = rwLock.getWriteHoldCount();
							if (hold == 0 && rwLock.getReadHoldCount() == 0)
								This.rwLockRefs.remove(r.Argument.getLockName().getName());

							logger.info("RWLock.exitWrite(thread=({}, {}), name={}) hold={}",
									r.Argument.getLockName().getGlobalThreadId().getServerId(),
									r.Argument.getLockName().getGlobalThreadId().getThreadId(),
									r.Argument.getLockName().getName(),
									hold);
							r.SendResultCode(hold);
							return;
						}
						r.SendResultCode(0);
					});
			break;

		default:
			logger.error("ReadWriteLockOperate: unknown operateType={} (thread=({}, {}), name={})",
					r.Argument.getOperateType(),
					r.Argument.getLockName().getGlobalThreadId().getServerId(),
					r.Argument.getLockName().getGlobalThreadId().getThreadId(),
					r.Argument.getLockName().getName());
			r.SendResultCode(ResultCodeInvalidArgument);
			break;
		}
		return 0;
	}

	@Override
	protected long ProcessSemaphoreCreateRequest(SemaphoreCreate r) {
		semaphores.computeIfAbsent(r.Argument.getLockName().getName(),
				(key) -> new Semaphore(r.Argument.getPermits()));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSemaphoreReleaseRequest(SemaphoreRelease r) {
		if (r.Argument.getPermits() <= 0) {
			r.SendResultCode(ResultCodeInvalidArgument);
			return 0;
		}
		simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
				(This) -> {
					var semaphoreAcq = This.semaphoreRefs.get(r.Argument.getLockName().getName());
					if (null != semaphoreAcq) {
						// 钳制到本线程实际持有量，防止超量 release 虚增信号量容量
						int actual = Math.min(r.Argument.getPermits(), semaphoreAcq.permits);
						semaphoreAcq.semaphore.release(actual);
						semaphoreAcq.permits -= actual;
						if (semaphoreAcq.permits <= 0) {
							This.semaphoreRefs.remove(r.Argument.getLockName().getName());
						}
						logger.info("semaphore.release(thread=({}, {}), name={}) permits={}",
								r.Argument.getLockName().getGlobalThreadId().getServerId(),
								r.Argument.getLockName().getGlobalThreadId().getThreadId(),
								r.Argument.getLockName().getName(),
								semaphoreAcq.permits);
						r.SendResultCode(semaphoreAcq.permits);
						return;
					}
					r.SendResultCode(0);
				});
		return 0;
	}

	@Override
	protected long ProcessSemaphoreTryAcquireRequest(SemaphoreTryAcquire r) {
		if (r.Argument.getPermits() <= 0 || r.Argument.getTimeoutMs() < 0) {
			r.SendResultCode(ResultCodeInvalidArgument);
			return 0;
		}
		simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
				(This) -> {
					var semaphoreAcq = This.getSemaphore(r.Argument.getLockName().getName());
					if (null == semaphoreAcq) {
						r.SendResultCode(1);
						return;
					}
					var acquired = semaphoreAcq.semaphore.tryAcquire(r.Argument.getPermits(),
							r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
					if (acquired) {
						semaphoreAcq.permits += r.Argument.getPermits();
						This.semaphoreRefs.put(r.Argument.getLockName().getName(), semaphoreAcq);
					}
					logger.info("semaphore.tryAcquire(thread=({}, {}), name={}) -> {}",
							r.Argument.getLockName().getGlobalThreadId().getServerId(),
							r.Argument.getLockName().getGlobalThreadId().getThreadId(),
							r.Argument.getLockName().getName(),
							acquired);
					r.SendResultCode(acquired ? 0 : 2);
				});
		return 0;
	}
}
