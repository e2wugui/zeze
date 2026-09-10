package Zeze.Util;

import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Application;
import Zeze.Config;
import Zeze.Hot.HotGuard;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Net.Service;
import Zeze.Raft.RaftRetryException;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("resource")
public final class Task {
	static final @NotNull Logger logger = LogManager.getLogger(Task.class);
	// 通常不建议开,事务并发量太大时并发冲突可能很高导致频繁redo
	private static final boolean USE_VIRTUAL_THREAD = PropertiesHelper.getBool("useVirtualThread", true);
	private static final boolean USE_UNLIMITED_VIRTUAL_THREAD = USE_VIRTUAL_THREAD
		&& PropertiesHelper.getBool("useUnlimitedVirtualThread", !inJUnitTest());

	// 默认不开启热更，这个实现希望能被优化掉，几乎不造成影响。
	// 开启热更时，由App.HotManager初始化的时候设置。
	@SuppressWarnings("CanBeFinal")
	public static volatile @NotNull Factory<HotGuard> hotGuard = () -> null;
	private static final FastLock taskLock = new FastLock();
	private static final TaskOneByOneByKey oneByOne = new TaskOneByOneByKey();

	@FunctionalInterface
	public interface ILogAction {
		void run(@Nullable Throwable ex, long result, @Nullable Protocol<?> p, @NotNull String actionName);
	}

	@SuppressWarnings("CanBeFinal")
	public static volatile long defaultTimeout = 120_000; // 2 minutes

	// volatile: shutdown 会将字段置 null，需保证对其他线程可见
	private static volatile ExecutorService threadPoolDefault;
	private static volatile ScheduledExecutorService threadPoolScheduled;
	private static volatile ExecutorService threadPoolCritical; // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
	@SuppressWarnings("CanBeFinal")
	public static @Nullable ILogAction logAction = Task::DefaultLogAction;

	private static volatile int systemOneByOneConcurrency;
	private static final AtomicLong systemExecuteCount = new AtomicLong();

	static {
		ShutdownHook.init();
		setSystemOneByOneConcurrency(Runtime.getRuntime().availableProcessors() / 2);
	}

	public static boolean isVirtualThreadEnabled() {
		return ThreadFactoryWithName.isVirtualThreadEnabled();
	}

	public static boolean inJUnitTest() {
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if (element.getClassName().startsWith("org.junit.") ||
				element.getClassName().startsWith("junit.")) {
				logger.info("inJUnitTest = true");
				return true;
			}
		}
		return false;
		//return System.getProperty("sun.java.command").split(" ")[0].endsWith(".JUnitStarter");
	}

	public static @NotNull TaskOneByOneByKey getOneByOne() {
		return oneByOne;
	}

	/**
	 * 设置系统队列数量。
	 * 默认是Runtime.getRuntime().availableProcessors() / 2。
	 *
	 * @param n concurrency
	 */
	public static void setSystemOneByOneConcurrency(int n) {
		if (n < 1)
			n = 1;
		systemOneByOneConcurrency = n;
	}

	public static int getSystemOneByOneConcurrency() {
		return systemOneByOneConcurrency;
	}

	private static String nextSystemOneByOneConcurrencyName() {
		return "SystemOneByOne_" + (systemExecuteCount.incrementAndGet() % systemOneByOneConcurrency);
	}

	/**
	 * 执行一个系统任务。
	 * 放入系统队列，挨个执行。系统队列有systemOneByOneConcurrency个。
	 *
	 * @param action0 action
	 */
	public static void executeSystemOneByOne(Action0 action0, String name) {
		TaskSpec.ofAction(action0).name(name).executeOneByOne(nextSystemOneByOneConcurrencyName(), oneByOne);
	}

	/**
	 * 执行一个系统任务。
	 * 放入系统队列，挨个执行。系统队列有systemOneByOneConcurrency个。
	 *
	 * @param proc proc
	 */
	public static void executeSystemOneByOne(Procedure proc) {
		TaskSpec.ofProcedure(proc).executeOneByOne(nextSystemOneByOneConcurrencyName(), oneByOne);
	}

	public static ExecutorService getThreadPool() {
		return threadPoolDefault;
	}

	public static ScheduledExecutorService getScheduledThreadPool() {
		return threadPoolScheduled;
	}

	// 注意：shutdown 后返回 null（与 getThreadPool/getScheduledThreadPool 一致），调用方需判空
	public static ExecutorService getCriticalThreadPool() {
		return threadPoolCritical;
	}

	/**
	 * 停止Task里面包含的默认的三个线程池,default,scheduled,critical。
	 * 幂等：池未初始化或已停止（字段为null）时跳过，不抛异常。
	 *
	 * @param maxAwait 等待任务结束的毫秒数。
	 * @throws InterruptedException await被中断异常
	 * @throws TimeoutException     await超时异常
	 */
	public static void shutdownNow(long maxAwait) throws InterruptedException, TimeoutException {
		shutdownPools(true, maxAwait);
	}

	/**
	 * 停止Task里面包含的默认的三个线程池,default,scheduled,critical。
	 * 幂等：池未初始化或已停止（字段为null）时跳过，不抛异常。
	 *
	 * @param maxAwait 等待任务结束的毫秒数。
	 * @throws InterruptedException await被中断异常
	 * @throws TimeoutException     await超时异常
	 */
	public static void shutdown(long maxAwait) throws InterruptedException, TimeoutException {
		shutdownPools(false, maxAwait);
	}

	// 与 tryInitThreadPool 同锁互斥：避免"读到旧池→并发初始化建新池→置null"竞态把新池引用抹掉却不关闭。
	// 先置null再等待：等待期间新提交立即走 poolOrThrow 的明确失败路径，而不是进入已 shutdown 的池被静默拒绝。
	private static void shutdownPools(boolean now, long maxAwait) throws InterruptedException, TimeoutException {
		ScheduledExecutorService scheduledTmp;
		ExecutorService defaultTmp;
		ExecutorService criticalTmp;
		taskLock.lock();
		try {
			scheduledTmp = threadPoolScheduled;
			defaultTmp = threadPoolDefault;
			criticalTmp = threadPoolCritical;
			if (scheduledTmp != null) {
				threadPoolScheduled = null;
				if (now)
					scheduledTmp.shutdownNow();
				else
					scheduledTmp.shutdown();
			}
			if (defaultTmp != null) {
				threadPoolDefault = null;
				if (now)
					defaultTmp.shutdownNow();
				else
					defaultTmp.shutdown();
			}
			if (criticalTmp != null) {
				threadPoolCritical = null;
				if (now)
					criticalTmp.shutdownNow();
				else
					criticalTmp.shutdown();
			}
		} finally {
			taskLock.unlock();
		}

		var timeout = "";
		if (scheduledTmp != null && !scheduledTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolScheduled timeout,";
		if (defaultTmp != null && !defaultTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolDefault timeout,";
		if (criticalTmp != null && !criticalTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolCritical timeout,";
		if (!timeout.isEmpty())
			throw new TimeoutException(timeout);
	}

	// 提交/执行/调度路径的池选择：池未初始化或已 shutdown（字段为null）时抛明确异常，替代裸NPE。
	// 不自动重建池：停机后的提交应显式失败，静默复活可能吞掉停机语义（如 ShutdownHook 内的 flush 任务派发）。
	// 包内可见：one-by-one 队列引擎的入队前校验共用（FND3-14——入队后派发失败会把队列永久卡死）。
	static @NotNull ExecutorService poolOrThrow(boolean critical) {
		var pool = critical ? threadPoolCritical : threadPoolDefault;
		if (pool == null)
			throw new IllegalStateException("Task thread pools not initialized or shut down: "
				+ (critical ? "critical" : "default") + " pool is null");
		return pool;
	}

	private static @NotNull ScheduledExecutorService scheduledPoolOrThrow() {
		var pool = threadPoolScheduled;
		if (pool == null)
			throw new IllegalStateException("Task thread pools not initialized or shut down: scheduled pool is null");
		return pool;
	}

	// 固定数量的线程池, 普通优先级, 自动优先使用支持虚拟线程(不限制数量), 用于处理普通任务
	public static @NotNull ExecutorService newFixedThreadPool(int threadCount, @NotNull String threadNamePrefix) {
		if (USE_UNLIMITED_VIRTUAL_THREAD && isVirtualThreadEnabled()) {
			logger.info("newFixedThreadPool({},{}) use unlimited virtual thread pool", threadCount, threadNamePrefix);
			return Executors.newThreadPerTaskExecutor(new ThreadFactoryWithName(threadNamePrefix));
		}
		return Executors.newFixedThreadPool(threadCount,
			new ThreadFactoryWithName(threadNamePrefix, Thread.NORM_PRIORITY, USE_VIRTUAL_THREAD));
	}

	// 关键线程池, 不使用虚拟线程时设为普通优先级+2, 线程数按需增长, 用于处理关键任务, 比普通任务的处理更及时
	public static @NotNull ExecutorService newCriticalThreadPool(@NotNull String threadNamePrefix) {
		if (USE_UNLIMITED_VIRTUAL_THREAD && isVirtualThreadEnabled()) {
			logger.info("newCriticalThreadPool({}) use unlimited virtual thread pool", threadNamePrefix);
			return Executors.newThreadPerTaskExecutor(new ThreadFactoryWithName(threadNamePrefix));
		}
		return Executors.newCachedThreadPool(new ThreadFactoryWithName(threadNamePrefix, Thread.NORM_PRIORITY + 2));
	}

	public static void initThreadPool(@NotNull ExecutorService pool,
									  @NotNull ScheduledExecutorService scheduled) {
		taskLock.lock();
		try {
			//noinspection ConstantValue
			if (pool == null || scheduled == null)
				throw new IllegalArgumentException();

			if (threadPoolDefault != null || threadPoolScheduled != null)
				throw new IllegalStateException("ThreadPool Has Initialized.");
			threadPoolDefault = pool;
			threadPoolScheduled = scheduled;
			threadPoolCritical = newCriticalThreadPool("ZezeCriticalPool");
			ThreadDiagnosable.startDiagnose(30_000);
		} finally {
			taskLock.unlock();
		}
	}

	public static boolean tryInitThreadPool() {
		return tryInitThreadPool((Config)null, null, null);
	}

	public static boolean tryInitThreadPool(@Nullable Application app) {
		return tryInitThreadPool(app != null ? app.getConfig() : null, null, null);
	}

	// 不叫 tryInitThreadPool(Config)：避免与 Application 单参重载对裸 null 调用产生歧义
	// （tryInitThreadPool(null) 在两个引用类型重载间无法确定绑定，编译报错）。
	public static boolean tryInitThreadPoolWithConfig(@Nullable Config config) {
		return tryInitThreadPool(config, null, null);
	}

	public static boolean tryInitThreadPool(@Nullable Application app, @Nullable ExecutorService pool,
											@Nullable ScheduledExecutorService scheduled) {
		return tryInitThreadPool(app != null ? app.getConfig() : null, pool, scheduled);
	}

	// 注意：第一个参数传字面量 null 时需强转 (Config)null，以区别于 Application 三参重载。
	public static boolean tryInitThreadPool(@Nullable Config config, @Nullable ExecutorService pool,
											@Nullable ScheduledExecutorService scheduled) {
		taskLock.lock();
		try {
			if (threadPoolDefault != null || threadPoolScheduled != null)
				return false;

			if (pool == null) {
				int workerThreads;
				if (config != null && config.getWorkerThreads() > 0)
					workerThreads = config.getWorkerThreads();
				else
					workerThreads = Runtime.getRuntime().availableProcessors() * 30;
				threadPoolDefault = newFixedThreadPool(workerThreads, "ZezeTaskPool");
			} else
				threadPoolDefault = pool;

			if (scheduled == null) {
				int workerThreads;
				if (config != null && config.getScheduledThreads() > 0)
					workerThreads = config.getScheduledThreads();
				else
					workerThreads = Runtime.getRuntime().availableProcessors();
				threadPoolScheduled = Executors.newScheduledThreadPool(workerThreads,
					new ThreadFactoryWithName("ZezeScheduledPool", Thread.NORM_PRIORITY, USE_VIRTUAL_THREAD));
			} else
				threadPoolScheduled = scheduled;
			threadPoolCritical = newCriticalThreadPool("ZezeCriticalPool");
			ThreadDiagnosable.startDiagnose(30_000);
			return true;
		} finally {
			taskLock.unlock();
		}
	}

	// 注意必须使用try包装,确保create和close配对
	public static @NotNull ThreadDiagnosable.Timeout createTimeout(long timeout) {
		return new ThreadDiagnosable.Timeout(timeout);
	}

	// 注意必须使用try包装,确保create和close配对
	public static @NotNull ThreadDiagnosable.Critical enterCritical(boolean critical) {
		return new ThreadDiagnosable.Critical(critical);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).call()}。 */
	@Deprecated
	public static void call(@NotNull Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).call();
	}

	// ZezeCounter 计数辅助：key 为 null 表示统计已在 body.call 内部完成（OfFunc/OfProcedure），外层不再计数。
	private static void addTaskRunTime(@Nullable Object key, long timeBegin) {
		if (key != null && ZezeCounter.instance != null)
			ZezeCounter.instance.addTaskRunTime(key, System.nanoTime() - timeBegin);
	}

	// TaskBody 统一核心：载荷间差异（异常/结果策略、日志名、统计位置）由 TaskBody 实现封装，
	// 各执行家族只保留一份样板（池选择/hotGuard/timeout/计数）。
	static <R> R callCore(@NotNull TaskBody<R> body, @Nullable String name) {
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try {
			return body.call(name);
		} finally {
			addTaskRunTime(body.statsKey(name), timeBegin);
		}
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).call()}。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable String name) {
		return TaskSpec.ofFunc(func).name(name).call();
	}

	/**
	 * 事务感知执行：当前在运行中的事务内时延迟到事务提交后执行
	 * （rollback 不执行、redo 由新一轮重新注册），否则立即执行。
	 * 注意被延迟的 action 在事务的 commit 回调中同步执行，此时事务已 Completed，
	 * 不能再访问表或开新事务（需要事务的工作应在 action 内转入线程池执行）。
	 * 供 TaskSpec 与 Arch/Game 等框架层复用。
	 */
	public static void runTxnAware(@NotNull Runnable action) {
		Transaction t;
		if ((t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(action);
		else
			action.run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).run()}。 */
	@Deprecated
	public static void run(@NotNull Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）。 */
	@Deprecated
	public static void run(@NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofAction(action).name(name).dispatchMode(mode).runNow();
			return;
		}
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）。 */
	@Deprecated
	public static void run(@NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).runNow();
			return;
		}
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).run();
	}

	// 注意: 以Unsafe结尾的方法在事务中也会立即异步执行,即使之后该事务redo或rollback也无法撤销,很可能不是想要的结果,所以小心使用

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name) {
		return TaskSpec.ofAction(action).name(name).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name,
											   @Nullable DispatchMode mode) {
		return TaskSpec.ofAction(action).name(name).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name,
											   @Nullable DispatchMode mode, long timeout) {
		return TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).submitNow();
	}

	static <R> @NotNull Future<R> submitCore(@NotNull TaskBody<R> body, @Nullable String name,
											 @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var future = new TaskCompletionSource<R>();
			try {
				future.setResult(body.callForFuture(name));
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", body.logName(name), e);
				future.setException(e);
			} finally {
				addTaskRunTime(body.statsKey(name), timeBegin);
			}
			return future;
		}

		return poolOrThrow(mode == DispatchMode.Critical).submit(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return body.call(name);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", body.logName(name), e);
				throw forceThrow(e);
			} finally {
				addTaskRunTime(body.statsKey(name), timeBegin);
			}
		});
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name,
									 @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name,
									 @Nullable DispatchMode mode, long timeout) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).timeout(timeout).runNow();
	}

	// 无 Future 消费者的入池执行：异常只记日志（body.call 的策略已先处理一轮，这里兜住 OfFunc0 的传播）
	static void executeCore(@NotNull TaskBody<?> body, @Nullable String name,
							@Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try {
				body.call(name);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", body.logName(name), e);
			} finally {
				addTaskRunTime(body.statsKey(name), timeBegin);
			}
			return;
		}

		poolOrThrow(mode == DispatchMode.Critical).execute(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				body.call(name);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", body.logName(name), e);
			} finally {
				addTaskRunTime(body.statsKey(name), timeBegin);
			}
		});
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).schedule(initialDelay)}。 */
	@Deprecated
	public static void schedule(long initialDelay, @NotNull Action0 action) {
		TaskSpec.ofAction(action).schedule(initialDelay);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).timeout(timeout).schedule(initialDelay)}。 */
	@Deprecated
	public static void schedule(long initialDelay, @NotNull Action0 action, long timeout) {
		TaskSpec.ofAction(action).timeout(timeout).schedule(initialDelay);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).scheduleNow(initialDelay)}。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleUnsafe(long initialDelay, @NotNull Action0 action) {
		return TaskSpec.ofAction(action).scheduleNow(initialDelay);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).timeout(timeout).scheduleNow(initialDelay)}。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleUnsafe(long initialDelay, @NotNull Action0 action, long timeout) {
		return TaskSpec.ofAction(action).timeout(timeout).scheduleNow(initialDelay);
	}

	static <R> @NotNull ScheduledFuture<R> scheduleCore(long initialDelay, @NotNull TaskBody<R> body,
														@Nullable String name, long timeout) {
		return scheduledPoolOrThrow().schedule(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return body.call(name);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", body.logName(name), e);
				throw forceThrow(e);
			} finally {
				addTaskRunTime(body.statsKey(name), timeBegin);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).scheduleNow(initialDelay)}。 */
	@Deprecated
	public static <R> @NotNull Future<R> scheduleUnsafe(long initialDelay, @NotNull Func0<R> func) {
		return TaskSpec.ofFunc0(func).scheduleNow(initialDelay);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc0(func).timeout(timeout).scheduleNow(initialDelay)}。 */
	@Deprecated
	public static <R> @NotNull Future<R> scheduleUnsafe(long initialDelay, @NotNull Func0<R> func, long timeout) {
		return TaskSpec.ofFunc0(func).timeout(timeout).scheduleNow(initialDelay);
	}

	// 周期调度：周期任务无法携带返回值，结果丢弃，异常只记日志（不 rethrow，否则 ScheduledExecutor 会停掉后续周期）。
	static <R> @NotNull TimerFuture<R> schedulePeriodCore(long initialDelay, long period, @NotNull TaskBody<R> body,
														  @Nullable String name, long timeout) {
		var future = new TimerFuture<R>();
		future.setFuture(scheduledPoolOrThrow().scheduleWithFixedDelay(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			future.lock();
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				if (future.isCancelled())
					return;
				body.call(name);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", body.logName(name), e);
			} finally {
				future.unlock();
				addTaskRunTime(body.statsKey(name), timeBegin);
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS));
		return future;
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).scheduleAt(hour, minute)}。 */
	@Deprecated
	public static void scheduleAt(int hour, int minute, @NotNull Action0 action) {
		TaskSpec.ofAction(action).scheduleAt(hour, minute);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).scheduleAtPeriod(hour, minute, period)}。 */
	@Deprecated
	public static void scheduleAt(int hour, int minute, long period, @NotNull Action0 action) {
		TaskSpec.ofAction(action).scheduleAtPeriod(hour, minute, period);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).timeout(timeout).scheduleAtPeriod(hour, minute, period)}。 */
	@Deprecated
	public static void scheduleAt(int hour, int minute, long period, @NotNull Action0 action, long timeout) {
		TaskSpec.ofAction(action).timeout(timeout).scheduleAtPeriod(hour, minute, period);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).scheduleAtNow(hour, minute)}。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, @NotNull Action0 action) {
		return TaskSpec.ofAction(action).scheduleAtNow(hour, minute);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).scheduleAtPeriodNow(hour, minute, period)}。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, long period,
															   @NotNull Action0 action) {
		return TaskSpec.ofAction(action).scheduleAtPeriodNow(hour, minute, period);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).timeout(timeout).scheduleAtPeriodNow(hour, minute, period)}。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, long period,
															   @NotNull Action0 action, long timeout) {
		return TaskSpec.ofAction(action).timeout(timeout).scheduleAtPeriodNow(hour, minute, period);
	}

	static long delayUntilNextDaily(int hour, int minute) {
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, hour);
		firstTime.set(Calendar.MINUTE, minute);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);
		if (firstTime.before(Calendar.getInstance())) // 如果第一次的时间比当前时间早，推到明天。
			firstTime.add(Calendar.DAY_OF_MONTH, 1); // tomorrow!
		return firstTime.getTime().getTime() - System.currentTimeMillis();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).schedulePeriod(initialDelay, period)}。 */
	@Deprecated
	public static void schedule(long initialDelay, long period, @NotNull Action0 action) {
		TaskSpec.ofAction(action).schedulePeriod(initialDelay, period);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).timeout(timeout).schedulePeriod(initialDelay, period)}。 */
	@Deprecated
	public static void schedule(long initialDelay, long period, @NotNull Action0 action, long timeout) {
		TaskSpec.ofAction(action).timeout(timeout).schedulePeriod(initialDelay, period);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).schedulePeriodNow(initialDelay, period)}。 */
	@Deprecated
	public static @NotNull TimerFuture<?> scheduleUnsafe(long initialDelay, long period, @NotNull Action0 action) {
		return TaskSpec.ofAction(action).schedulePeriodNow(initialDelay, period);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).timeout(timeout).schedulePeriodNow(initialDelay, period)}。 */
	@Deprecated
	public static @NotNull TimerFuture<?> scheduleUnsafe(long initialDelay, long period, @NotNull Action0 action,
														 long timeout) {
		return TaskSpec.ofAction(action).timeout(timeout).schedulePeriodNow(initialDelay, period);
	}

	public static void DefaultLogAction(@Nullable Throwable ex, long result, @Nullable Protocol<?> p,
										@NotNull String actionName) {
		// exception -> Error
		// 0 != result -> level from p or Info
		// others -> Trace
		Level level;
		if (ex != null)
			level = Level.ERROR;
		else if (result != 0) {
			Service s;
			Application zeze;
			if (p != null && (s = p.getService()) != null && (zeze = s.getZeze()) != null)
				level = zeze.getConfig().getProcessReturnErrorLogLevel();
			else
				level = Level.INFO;
		} else {
			if (!logger.isTraceEnabled())
				return;
			level = Level.TRACE;
		}
		Object userState;
		String userStateStr = p != null && (userState = p.getUserState()) != null ? " UserState=" + userState : "";

		var moduleId = 0;
		var errCode = result;
		if (result > 0) {
			moduleId = IModule.getModuleId(result);
			errCode = IModule.getErrorCode(result);
		}

		if (null == ex) {
			logger.log(level, "Action={}{} Return={}:{} Arg={}",
				actionName, userStateStr, moduleId, errCode,
				p != null ? AsyncSocket.toStr(p.Argument) : "");
		} else {
			logger.log(level, "Action={}{} Return={}:{} Arg={}",
				actionName, userStateStr, moduleId, errCode,
				p != null ? AsyncSocket.toStr(p.Argument) : "", ex);
		}
	}

	public static void logAndStatistics(long result, @Nullable Protocol<?> p, boolean isRequestSaved) {
		logAndStatistics(null, result, p, isRequestSaved, null);
	}

	public static void logAndStatistics(@Nullable Throwable ex, long result, @Nullable Protocol<?> p,
										boolean isRequestSaved) {
		logAndStatistics(ex, result, p, isRequestSaved, null);
	}

	public static void logAndStatistics(@Nullable Throwable ex, long result, @Nullable Protocol<?> p,
										boolean isRequestSaved, @Nullable String aName) {
		var protocolName = p != null ? p.getClass().getName() : "?";
		var actionName = aName != null ? aName : isRequestSaved ? protocolName : protocolName + ":Response";
		var tmpVolatile = logAction;
		if (tmpVolatile != null) {
			try {
				tmpVolatile.run(ex, result, p, actionName);
			} catch (Exception e) {
				logger.error("logAndStatistics exception:", e);
			}
		}
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).call()}。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return TaskSpec.ofFunc(func, p).call();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).call()}。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p,
							@Nullable ProtocolErrorHandle actionWhenError) {
		return TaskSpec.ofFunc(func, p, actionWhenError).call();
	}

	public static @NotNull Throwable getRootCause(@NotNull Throwable e) {
		for (; ; ) {
			var c = e.getCause();
			if (c == null)
				return e;
			e = c;
		}
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).call()}。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p,
							@Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		return TaskSpec.ofFunc(func, p, actionWhenError).name(aName).call();
	}

	/** 协议版 Func 载荷的叶子核心（TaskBody.OfProtocolFunc 调用）：异常翻错误码、错误回发、协议日志与统计。 */
	static long callFuncCore(@NotNull FuncLong func, @Nullable Protocol<?> p,
							 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		boolean isRequestSaved = p == null || p.isRequest(); // 记住这个，以后可能会被改变。
		try {
			var result = func.call();
			if (result != 0 && isRequestSaved && actionWhenError != null)
				actionWhenError.handle(p, result);
			logAndStatistics(null, result, p, isRequestSaved, aName);
			return result;
		} catch (Exception ex) {
			long errorCode;
			var rootEx = getRootCause(ex);
			if (rootEx instanceof TaskCanceledException)
				errorCode = Procedure.CancelException;
			else if (rootEx instanceof RaftRetryException)
				errorCode = Procedure.RaftRetry;
			else
				errorCode = Procedure.Exception;

			logAndStatistics(ex, errorCode, p, isRequestSaved, aName);
			if (isRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.handle(p, errorCode);
				} catch (Exception e) {
					logger.error("{} exception:", aName != null ? aName
						: (p != null ? p.getClass().getName() : actionWhenError.getClass().getName()), e);
				}
			}
			return errorCode;
		} finally {
			//noinspection ConstantValue
			if (ZezeCounter.instance != null && func != null) {
				ZezeCounter.instance.addTaskRunTime(aName != null ? aName : (p != null ? p : func).getClass(),
					System.nanoTime() - timeBegin);
			}
		}
	}

	// 无协议路径已由 TaskBody.OfFunc/OfProcedure 与统一 core（callCore/submitCore/executeCore）覆盖；
	// 协议感知路径同样由 TaskBody 协议版载荷路由到下方的 call core，不再需要独立的 submit/execute 协议 core。

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).run()}。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		TaskSpec.ofFunc(func, p).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).run()}。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError) {
		TaskSpec.ofFunc(func, p, actionWhenError).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).run()}。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		TaskSpec.ofFunc(func, p, actionWhenError).name(aName).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
						   @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).runNow();
			return;
		}
		TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).run();
	}

	/** @deprecated
	 * 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
						   @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).runNow();
			return;
		}
		TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return TaskSpec.ofFunc(func, p).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError) {
		return TaskSpec.ofFunc(func, p, actionWhenError).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError, String aName) {
		return TaskSpec.ofFunc(func, p, actionWhenError).name(aName).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
												  @Nullable DispatchMode mode) {
		return TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												  @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
												  @Nullable DispatchMode mode, long timeout) {
		return TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		TaskSpec.ofFunc(func, p).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError) {
		TaskSpec.ofFunc(func, p, actionWhenError).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError, String aName) {
		TaskSpec.ofFunc(func, p, actionWhenError).name(aName).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
									 @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
									 @Nullable DispatchMode mode, long timeout) {
		TaskSpec.ofFunc(func, p, actionWhenError).name(aName).dispatchMode(mode).timeout(timeout).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).call()}。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure) {
		return TaskSpec.ofProcedure(procedure).call();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from).call()}。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		return TaskSpec.ofProcedure(procedure, from).call();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).call()}。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure, @Nullable Protocol<?> from,
							@Nullable ProtocolErrorHandle actionWhenError) {
		return TaskSpec.ofProcedure(procedure, from, actionWhenError).call();
	}

	/** 协议版 Procedure 载荷的叶子核心（TaskBody.OfProtocolProcedure 调用）。 */
	static long callProcCore(@NotNull Procedure procedure, @Nullable Protocol<?> from,
							 @Nullable ProtocolErrorHandle actionWhenError) {
		boolean isRequestSaved = from == null || from.isRequest();
		try {
			// 日志在call里面记录。因为要支持嵌套。
			// 统计在call里面实现。
			long result = procedure.call();
			if (result != 0 && isRequestSaved && actionWhenError != null)
				actionWhenError.handle(from, result);
			logAndStatistics(null, result, from, isRequestSaved, procedure.getActionName());
			return result;
		} catch (Exception ex) {
			// Procedure.call处理了所有错误。应该不会到这里。除非内部错误。
			if (isRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.handle(from, Procedure.Exception);
				} catch (Exception e) {
					logger.error("actionWhenError exception:", e);
				}
			}
			logger.error("{} exception:", procedure, ex);
			return Procedure.Exception;
		}
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).call()}。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
							@Nullable ProtocolErrorHandle actionWhenError) {
		return TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).call();
	}

	/** 过程内解码协议载荷的叶子核心（TaskBody.OfProcedureOut 调用）。 */
	static long callProcOutCore(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
								@Nullable ProtocolErrorHandle actionWhenError) {
		Protocol<?> from = null;
		try {
			// 日志在call里面记录。因为要支持嵌套。
			// 统计在call里面实现。
			long result = procedure.call();
			from = outProtocol.value;
			if (result != 0 && (from == null || from.isRequest()) && actionWhenError != null)
				actionWhenError.handle(from, result);
			logAndStatistics(null, result, from, from == null || from.isRequest(), procedure.getActionName());
			return result;
		} catch (Exception ex) {
			// Procedure.call处理了所有错误。应该不会到这里。除非内部错误。
			if ((from == null || from.isRequest()) && actionWhenError != null) {
				try {
					actionWhenError.handle(from, Procedure.Exception);
				} catch (Exception e) {
					logger.error("actionWhenError exception:", e);
				}
			}
			logger.error("{} exception:", procedure, ex);
			return Procedure.Exception;
		}
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).run()}。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from).run()}。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		TaskSpec.ofProcedure(procedure, from).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).run()}。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
						   @Nullable ProtocolErrorHandle actionWhenError) {
		TaskSpec.ofProcedure(procedure, from, actionWhenError).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).runNow();
			return;
		}
		TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).run();
	}

	/** @deprecated
	 * 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
						   @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).runNow();
			return;
		}
		TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure) {
		return TaskSpec.ofProcedure(procedure).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		return TaskSpec.ofProcedure(procedure, from).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
												  @Nullable ProtocolErrorHandle actionWhenError) {
		return TaskSpec.ofProcedure(procedure, from, actionWhenError).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		return TaskSpec.ofProcedure(procedure).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
												  @Nullable ProtocolErrorHandle actionWhenError,
												  @Nullable DispatchMode mode) {
		return TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
												  @Nullable ProtocolErrorHandle actionWhenError,
												  @Nullable DispatchMode mode, long timeout) {
		return TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure,
												  @NotNull OutObject<Protocol<?>> outProtocol,
												  @Nullable ProtocolErrorHandle actionWhenError,
												  @Nullable DispatchMode mode) {
		return TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).timeout(timeout).submitNow()}。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure,
												  @NotNull OutObject<Protocol<?>> outProtocol,
												  @Nullable ProtocolErrorHandle actionWhenError,
												  @Nullable DispatchMode mode, long timeout) {
		return TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).timeout(timeout).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		TaskSpec.ofProcedure(procedure, from).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
									 @Nullable ProtocolErrorHandle actionWhenError) {
		TaskSpec.ofProcedure(procedure, from, actionWhenError).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode,
									 long timeout) {
		TaskSpec.ofProcedure(procedure, from, actionWhenError).dispatchMode(mode).timeout(timeout).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).timeout(timeout).runNow()}。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
									 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode,
									 long timeout) {
		TaskSpec.ofProcedureOut(procedure, outProtocol, actionWhenError).dispatchMode(mode).timeout(timeout).runNow();
	}

	// RpcResponse 族与普通族（call 核参数为 null 时）完全等价，这里直接委托普通族。

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).run()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void runRpcResponse(@NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void runRpcResponse(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofProcedure(procedure).dispatchMode(mode).runNow();
			return;
		}
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void runRpcResponse(@NotNull Procedure procedure, @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).runNow();
			return;
		}
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).run()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		TaskSpec.ofFunc(func, p).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).dispatchMode(mode).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p, @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofFunc(func, p).dispatchMode(mode).runNow();
			return;
		}
		TaskSpec.ofFunc(func, p).dispatchMode(mode).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).run()}；mode=Direct 时用 .runNow()（保留旧行为立即执行，跳过事务延迟）；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p, @Nullable DispatchMode mode,
									  long timeout) {
		if (mode == DispatchMode.Direct) {
			TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).runNow();
			return;
		}
		TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).submitNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure) {
		return TaskSpec.ofProcedure(procedure).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).submitNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure,
															 @Nullable DispatchMode mode) {
		return TaskSpec.ofProcedure(procedure).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).submitNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure,
															 @Nullable DispatchMode mode, long timeout) {
		return TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).submitNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return TaskSpec.ofFunc(func, p).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).dispatchMode(mode).submitNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
															 @Nullable DispatchMode mode) {
		return TaskSpec.ofFunc(func, p).dispatchMode(mode).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).submitNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
															 @Nullable DispatchMode mode, long timeout) {
		return TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).submitNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).runNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).runNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).runNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode,
												long timeout) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).timeout(timeout).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).runNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		TaskSpec.ofFunc(func, p).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).dispatchMode(mode).runNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func, p).dispatchMode(mode).runNow();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).runNow()}；RpcResponse 族与普通族等价。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
												@Nullable DispatchMode mode, long timeout) {
		TaskSpec.ofFunc(func, p).dispatchMode(mode).timeout(timeout).runNow();
	}

	public static void waitAll(@NotNull Collection<Future<?>> tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw forceThrow(e);
			}
		}
	}

	public static void waitAll(Future<?> @NotNull [] tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw forceThrow(e);
			}
		}
	}

	// 利用编译器的漏洞(?)强制抛出任何异常,调用者不必声明throws或包装成RuntimeException,建议只在必要时使用
	@SuppressWarnings("unchecked")
	@Contract("_ -> fail")
	public static <E extends Throwable> RuntimeException forceThrow(@NotNull Throwable e) throws E {
		throw (E)e;
	}

	private Task() {
	}
}
