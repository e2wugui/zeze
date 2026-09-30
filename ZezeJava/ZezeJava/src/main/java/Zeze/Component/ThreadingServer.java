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

/**
 * 分布式线程同步组件服务端：为每个全局线程维护 SimulateThread，仲裁 Mutex/Semaphore/ReadWriteLock。
 */
public class ThreadingServer extends AbstractThreadingServer {
	private static final Logger logger = LogManager.getLogger(ThreadingServer.class);

	// 锁操作rpc的结果码约定为非负小整数（0=成功，1=失败/未持有，2=超时未获取）。
	// 参数非法或操作类型未知时用-1应答：客户端getResultCode()!=0按false处理（exit类走debug日志），
	// 不再无应答挂满超时（≥5s后以CompletionException抛出，错误形态不可诊断）。
	public static final int ResultCodeInvalidArgument = -1;

	private final Service service;
	private final ServiceManagerServer.Conf conf;
	private final HashMap<BGlobalThreadId, SimulateThread> simulateThreads = new HashMap<>();
	private final ConcurrentHashMap<Integer, SimulateThreads> simulateThreadsByServerId = new ConcurrentHashMap<>();

	// 每种锁一个命名空间。
	// 其他可做的优化【暂不考虑】。
	// WeakRef SimulateThread记住自己拥有的所有资源，当SimulateThread退出时，这里自动回收。
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
			thread.interrupt(); // Interrupt timed acquisitions; release must run on the owning thread.
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

	// 动作队列元素：携带发起该动作的rpc句柄，供SimulateThread.runAction()在动作异常时统一补应答。
	// rpc允许为null：内部动作（如超时release）没有对应的客户端请求，异常时只记日志不补应答。
	private record SimulateThreadAction(Rpc<?, ?> rpc, Action1<SimulateThread> action) {
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
			// 增加其他类型的同步机制，需要修改这里。
			// 漏检会导致持锁的模拟线程被判空闲退出：锁悬挂且无法释放（release只能offer给已退出的线程）。
			return mutexRefs.isEmpty() && semaphoreRefs.isEmpty() && rwLockRefs.isEmpty();
		}

		public ReentrantLock getMutex(String name) {
			// ref cache
			var mutex = mutexRefs.get(name);
			if (null != mutex)
				return mutex;

			// alloc
			return mutexes.computeIfAbsent(name, (k) -> new ReentrantLock());
		}

		SemaphoreAcquired getSemaphore(String name) {
			// ref cache
			var semaphoreAcq = semaphoreRefs.get(name);
			if (null != semaphoreAcq)
				return semaphoreAcq;

			// ref global
			var semaphore = semaphores.get(name);
			if (null != semaphore)
				return new SemaphoreAcquired(semaphore);
			return null;
		}

		public ReentrantReadWriteLock getReadWriteLock(String name) {
			// ref cache
			var rwLock = rwLockRefs.get(name);
			if (null != rwLock)
				return rwLock;

			// getOrAdd global
			return rwLocks.computeIfAbsent(name, (key) -> new ReentrantReadWriteLock());
		}

		@Override
		public void run() {
			try {
				while (!closed) {
					try {
						// 持有资源期间也必须带超时等待，否则无超时poll会空转占满CPU。
						var action = actions.poll(200, TimeUnit.MILLISECONDS);
						if (null != action) {
							runAction(action);
						}

						if (acquireNothing()) {
							// 没有已分配资源的时候，延时200ms，准备退出。
							action = actions.poll(200, TimeUnit.MILLISECONDS);
							if (null != action) {
								runAction(action);
								continue; // 发现新任务，继续工作，中断退出。
							}
							if (simulateThreadExit(id, this))
								break; // 真正退出...
							// else continue
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

		// 【通用兜底】动作统一在此包装执行：动作内任何新增可抛路径都会补发结果码-1，
		// 不再重演"catch(Exception)只记日志、客户端挂满rpc超时（≥5s以CompletionException呈现）"，
		// 也不再依赖逐点补丁（各处catch保留为快速失败路径）。
		// 双发防护：handler正常路径已自行SendResultCode应答、之后动作又抛异常时，兜底不能二次发送。
		// Rpc.trySendResultCode（Zeze.Net.Rpc）先经tryMarkSendResultDone做VarHandle CAS仲裁，
		// 已应答（sendResultDone=true）则CAS失败直接返回false不发送，天然防双发，无需额外AtomicBoolean。
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
		// activeTime由KeepAlive处理线程写、timeoutRelease定时器线程读，需要volatile保证可见性。
		private volatile long activeTime = System.currentTimeMillis();
		private final int serverId;
		// lastAppSerial依赖"同连接的KeepAlive按接收序串行处理"（见ProcessKeepAlive的Direct注解）：
		// IO线程内联执行保证同连接有序；volatile兜不同连接各自IO线程间的可见性。
		private volatile BKeepAlive.Data lastAppSerial;

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

		// Direct：lastAppSerial的"同连接KeepAlive有序处理"前提（见字段注释）要求IO线程按TCP
		// 接收序内联串行；Normal经共享线程池派发既不保证同连接顺序也不保证跨连接顺序——迟到的
			// 旧实例KeepAlive可在新实例接管并持锁后再次触发release，强制释放新实例持有的全部锁。
			// 处理体轻量（CHM查改+volatile写+非阻塞offer），内联执行不阻塞IO线程。
		@Override
		@Zeze.Util.DispatchModeAnnotation(mode = Zeze.Transaction.DispatchMode.Direct)
		protected long ProcessKeepAlive(KeepAlive p) {
		var threads = simulateThreadsByServerId.computeIfAbsent(p.Argument.getServerId(), SimulateThreads::new);
		threads.activeTime = System.currentTimeMillis();
		// 串行化serial的check-then-act：多selector线程部署下不同连接的KeepAlive在各自IO线程
		// 并发到达，"读lastAppSerial→比较→release→写回"非原子——同serverId双实例交错时双双通过
		// 单调判据、双release（合法新owner的锁被误释放），且低serial后写覆盖高serial触发下轮再释放。
		// 模块锁可重入（SimulateThreads.release同锁），KeepAlive频率低（10s周期）无争用顾虑。
		lock();
		try {
			if (null == threads.lastAppSerial) {
				threads.lastAppSerial = p.Argument;
				return 0; // first keepAlive。record only。
			}
			if (threads.lastAppSerial.getAppSerialId() != p.Argument.getAppSerialId()) {
				// serial无单调性判据——同serverId双实例（重建窗口内旧keepAliveTask在途、
				// 或错误配置双客户端）时两个appSerialId交替到达，10秒一轮互解，正常持锁者被持续
				// 强制释放。appSerialId为PersistentAtomicLong单调递增：仅更高的serial接管（release
				// 旧资源），更低的视为旧实例迟到，忽略不release。
				if (p.Argument.getAppSerialId() > threads.lastAppSerial.getAppSerialId()) {
					threads.release();
					threads.lastAppSerial = p.Argument;
				}
				return 0;
			}
			// same app serialId. done.
			return 0;
		} finally {
			unlock();
		}
	}

	private void timeoutRelease() {
		var now = System.currentTimeMillis();
		for (var threads : simulateThreadsByServerId.values()) {
			// 【30 minutes 没有联系】强制释放获得的所有资源。
			// 这个时间可以看作异常情况下，Agent获得锁后的最长安全工作时间。
			// 再超出，就没有锁定保证了。
			if (now - threads.activeTime > conf.threadingReleaseTimeout)
				threads.release();
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
		// tryLock(timeoutMs<0)按JDK契约抛IllegalArgumentException，若在SimulateThread
		// 动作内抛出会被run()吞掉且不再补发结果码（客户端挂满超时后以异常呈现）。入队前校验
		// 直接应答（结果码ResultCodeInvalidArgument）。
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
		// eEnterRead/eEnterWrite的动作内tryLock(timeoutMs<0)抛IllegalArgumentException
		// 会被SimulateThread.run()吞掉且不补发结果码。入队前校验直接应答。
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
								// enter/exit模式不对称（如enterWrite后exitRead）时unlock抛
								// IllegalMonitorStateException，动作在SimulateThread内抛出会被run()
								// 吞掉且不补发结果码——客户端挂满rpc超时。对齐参数校验系列
								// 立即应答错误码。
								logger.error("RWLock.exitRead mode mismatch (thread=({}, {}), name={})",
										r.Argument.getLockName().getGlobalThreadId().getServerId(),
										r.Argument.getLockName().getGlobalThreadId().getThreadId(),
										r.Argument.getLockName().getName(), e);
								r.SendResultCode(ResultCodeInvalidArgument);
								return;
							}
							var hold = rwLock.getReadHoldCount();
							// rwLockRefs按锁名共享一个条目，读写计数分开持有
							// （JDK支持写→读降级）。只看本模式计数清零即删条目会让另一模式的持有
							// 脱离跟踪：之后exit无应答即返回、模拟线程持锁被判空闲退出、
							// timeoutRelease也遍历不到——锁永久悬挂直到进程重启。双计数都为零才删。
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
								// 对称场景（enterRead后exitWrite），同eExitRead。
								logger.error("RWLock.exitWrite mode mismatch (thread=({}, {}), name={})",
										r.Argument.getLockName().getGlobalThreadId().getServerId(),
										r.Argument.getLockName().getGlobalThreadId().getThreadId(),
										r.Argument.getLockName().getName(), e);
								r.SendResultCode(ResultCodeInvalidArgument);
								return;
							}
							var hold = rwLock.getWriteHoldCount();
							// 双计数都为零才删（对称场景：先exitRead时writeHold仍>0，
							// 提前删条目=写锁悬挂、所有写者永久饥饿）。
							if (hold == 0 && rwLock.getReadHoldCount() == 0)
								This.rwLockRefs.remove(r.Argument.getLockName().getName());

							logger.info("RWLock.exitWrite(thread=({}, {}), name={}) hold={}",
									r.Argument.getLockName().getGlobalThreadId().getServerId(),
									r.Argument.getLockName().getGlobalThreadId().getThreadId(),
									r.Argument.getLockName().getName(),
									hold);
							r.SendResultCode(hold);
							return; // done
						}
						r.SendResultCode(0);
					});
			break;

		default:
			// 未知OperateType（buggy客户端/异版本/恶意包）不能无应答，
			// 否则客户端等满rpc超时后以CompletionException抛出。
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
		// release(permits<=0)按JDK契约抛IllegalArgumentException，动作在SimulateThread内
		// 抛出会被run()吞掉且不补发结果码（客户端挂满rpc超时后以CompletionException呈现）。
		// 入队前校验直接应答（TryAcquire家族已有同型防护）。
		if (r.Argument.getPermits() <= 0) {
			r.SendResultCode(ResultCodeInvalidArgument);
			return 0;
		}
		simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
				(This) -> {
					var semaphoreAcq = This.semaphoreRefs.get(r.Argument.getLockName().getName());
					if (null != semaphoreAcq) {
						// 钳制到入账持有量：客户端bug/恶意输入的超量release会直接膨胀JDK信号量
						// 真实容量（账面负值删条目、膨胀永久保留），信号量的并发约束被静默击穿。
						int actual = Math.min(r.Argument.getPermits(), semaphoreAcq.permits);
						semaphoreAcq.semaphore.release(actual);
						semaphoreAcq.permits -= actual;
						if (semaphoreAcq.permits <= 0) {
							// 马上要删除了，这个值本来不需要重置。如果下一次申请继续使用这个对象，必须设为0。
							// 现在不清除它（permits = 0），让后面的日志和结果能反应更多信息。
							This.semaphoreRefs.remove(r.Argument.getLockName().getName());
						}
						logger.info("semaphore.release(thread=({}, {}), name={}) permits={}",
								r.Argument.getLockName().getGlobalThreadId().getServerId(),
								r.Argument.getLockName().getGlobalThreadId().getThreadId(),
								r.Argument.getLockName().getName(),
								semaphoreAcq.permits);
						r.SendResultCode(semaphoreAcq.permits);
						return; // done
					}
					r.SendResultCode(0);
				});
		return 0;
	}

	@Override
	protected long ProcessSemaphoreTryAcquireRequest(SemaphoreTryAcquire r) {
		// tryAcquire(permits<=0)按JDK契约抛IllegalArgumentException，动作内抛出会被
		// SimulateThread.run()吞掉且不补发结果码。入队前校验直接应答。
		if (r.Argument.getPermits() <= 0 || r.Argument.getTimeoutMs() < 0) {
			r.SendResultCode(ResultCodeInvalidArgument);
			return 0;
		}
		simulateThreadOffer(r, r.Argument.getLockName().getGlobalThreadId(),
				(This) -> {
					var semaphoreAcq = This.getSemaphore(r.Argument.getLockName().getName());
					if (null == semaphoreAcq) {
						r.SendResultCode(1);
						return; // done
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
