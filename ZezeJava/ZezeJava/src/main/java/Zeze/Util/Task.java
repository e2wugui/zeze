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

	private static ExecutorService threadPoolDefault;
	private static ScheduledExecutorService threadPoolScheduled;
	private static ExecutorService threadPoolCritical; // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
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

	public static @NotNull ExecutorService getCriticalThreadPool() {
		return threadPoolCritical;
	}

	/**
	 * 停止Task里面包含的默认的三个线程池,default,scheduled,critical。
	 *
	 * @param maxAwait 等待任务结束的毫秒数。
	 * @throws InterruptedException await被中断异常
	 * @throws TimeoutException     await超时异常
	 */
	public static void shutdownNow(long maxAwait) throws InterruptedException, TimeoutException {

		threadPoolScheduled.shutdownNow();
		threadPoolDefault.shutdownNow();
		threadPoolCritical.shutdownNow();

		var threadPoolScheduledTmp = threadPoolScheduled;
		var threadPoolDefaultTmp = threadPoolDefault;
		var threadPoolCriticalTmp = threadPoolCritical;
		threadPoolScheduled = null;
		threadPoolDefault = null;
		threadPoolCritical = null;

		var timeout = "";
		if (!threadPoolScheduledTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolScheduled timeout,";
		if (!threadPoolDefaultTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolDefault timeout,";
		if (!threadPoolCriticalTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolCritical timeout,";
		if (!timeout.isEmpty())
			throw new TimeoutException(timeout);
	}

	/**
	 * 停止Task里面包含的默认的三个线程池,default,scheduled,critical。
	 *
	 * @param maxAwait 等待任务结束的毫秒数。
	 * @throws InterruptedException await被中断异常
	 * @throws TimeoutException     await超时异常
	 */
	public static void shutdown(long maxAwait) throws InterruptedException, TimeoutException {
		threadPoolScheduled.shutdown();
		threadPoolDefault.shutdown();
		threadPoolCritical.shutdown();

		var threadPoolScheduledTmp = threadPoolScheduled;
		var threadPoolDefaultTmp = threadPoolDefault;
		var threadPoolCriticalTmp = threadPoolCritical;
		threadPoolScheduled = null;
		threadPoolDefault = null;
		threadPoolCritical = null;

		var timeout = "";
		if (!threadPoolScheduledTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolScheduled timeout,";
		if (!threadPoolDefaultTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolDefault timeout,";
		if (!threadPoolCriticalTmp.awaitTermination(maxAwait, TimeUnit.MILLISECONDS))
			timeout += "await threadPoolCritical timeout,";
		if (!timeout.isEmpty())
			throw new TimeoutException(timeout);
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
				throw new IllegalStateException("ThreadPool Has Inited.");
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

	public static boolean tryInitThreadPool(@Nullable Config config) {
		return tryInitThreadPool(config, null, null);
	}

	public static boolean tryInitThreadPool(@Nullable Application app, @Nullable ExecutorService pool,
	                                        @Nullable ScheduledExecutorService scheduled) {
		return tryInitThreadPool(app != null ? app.getConfig() : null, pool, scheduled);
	}

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

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void call(@NotNull Action0 action, @Nullable String name) {
		callActionCore(action, name);
	}

	static void callActionCore(@NotNull Action0 action, @Nullable String name) {
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try {
			action.run();
		} catch (Exception ex) {
			//noinspection ConstantValue,UnreachableCode
			logger.error("{} exception:", name != null ? name : action != null ? action.getClass().getName() : "", ex);
		} finally {
			//noinspection ConstantValue
			if (ZezeCounter.instance != null && action != null)
				ZezeCounter.instance.addTaskRunTime(name != null ? name : action.getClass(), System.nanoTime() - timeBegin);
		}
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable String name) {
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try {
			return func.call();
		} catch (Exception ex) {
			//noinspection ConstantValue,UnreachableCode
			logger.error("{} exception:", name != null ? name : func != null ? func.getClass().getName() : "", ex);
			return Procedure.Exception;
		} finally {
			//noinspection ConstantValue
			if (ZezeCounter.instance != null && func != null)
				ZezeCounter.instance.addTaskRunTime(name != null ? name : func.getClass(), System.nanoTime() - timeBegin);
		}
	}

	/**
	 * 事务感知执行：当前在运行中的事务内时延迟到事务提交后执行
	 * （rollback 不执行、redo 由新一轮重新注册），否则立即执行。
	 * 注意被延迟的 action 在事务的 commit 回调中同步执行，此时事务已 Completed，
	 * 不能再访问表或开新事务（需要事务的工作应在 action 内转入线程池执行）。
	 * 供 Zeze.Net.ProtocolDispatch 等 Util 包外的框架层复用。
	 */
	public static void runTxnAware(@NotNull Runnable action) {
		Transaction t;
		if ((t = Transaction.getCurrent()) != null && t.isRunning())
			t.runWhileCommit(action);
		else
			action.run();
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull Action0 action, @Nullable String name) {
		runTxnAware(() -> executeActionCore(action, name, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。注意本方法保留老语义：mode=Direct 跳过事务检查立即执行；TaskSpec 的 run() 中 Direct 不再跳过事务延迟。 */
	@Deprecated
	public static void run(@NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct)
			executeActionCore(action, name, mode, defaultTimeout);
		else
			runTxnAware(() -> executeActionCore(action, name, mode, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。注意本方法保留老语义：mode=Direct 跳过事务检查立即执行；TaskSpec 的 run() 中 Direct 不再跳过事务延迟。 */
	@Deprecated
	public static void run(@NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct)
			executeActionCore(action, name, mode, timeout);
		else
			runTxnAware(() -> executeActionCore(action, name, mode, timeout));
	}

	// 注意: 以Unsafe结尾的方法在事务中也会立即异步执行,即使之后该事务redo或rollback也无法撤销,很可能不是想要的结果,所以小心使用
	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name) {
		return runUnsafe(action, name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name,
	                                           @Nullable DispatchMode mode) {
		return runUnsafe(action, name, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<?> runUnsafe(@NotNull Action0 action, @Nullable String name,
	                                           @Nullable DispatchMode mode, long timeout) {
		return submitActionCore(action, name, mode, timeout);
	}

	static @NotNull Future<?> submitActionCore(@NotNull Action0 action, @Nullable String name,
	                                              @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var future = new TaskCompletionSource<Long>();
			try {
				action.run();
				future.setResult(0L);
			} catch (Exception e) {
				//noinspection ConstantValue,UnreachableCode
				logger.error("{} exception:", name != null ? name : action != null ? action.getClass().getName() : "", e);
				future.setException(e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && action != null) {
					ZezeCounter.instance.addTaskRunTime(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				action.run();
			} catch (Throwable e) { // logger.error
				//noinspection ConstantValue,UnreachableCode
				logger.error("{} exception:", name != null ? name : action != null ? action.getClass().getName() : "", e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && action != null) {
					ZezeCounter.instance.addTaskRunTime(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name) {
		executeUnsafe(action, name, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name,
	                                 @Nullable DispatchMode mode) {
		executeUnsafe(action, name, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Action0 action, @Nullable String name,
	                                 @Nullable DispatchMode mode, long timeout) {
		executeActionCore(action, name, mode, timeout);
	}

	static void executeActionCore(@NotNull Action0 action, @Nullable String name,
	                                    @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try {
				action.run();
			} catch (Exception e) {
				//noinspection ConstantValue,UnreachableCode
				logger.error("{} exception:", name != null ? name : action != null ? action.getClass().getName() : "", e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && action != null) {
					ZezeCounter.instance.addTaskRunTime(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				action.run();
			} catch (Throwable e) { // logger.error
				//noinspection ConstantValue,UnreachableCode
				logger.error("{} exception:", name != null ? name : action != null ? action.getClass().getName() : "", e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && action != null) {
					ZezeCounter.instance.addTaskRunTime(name != null ? name : action.getClass(),
							System.nanoTime() - timeBegin);
				}
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void schedule(long initialDelay, @NotNull Action0 action) {
		runTxnAware(() -> scheduleActionCore(initialDelay, action, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void schedule(long initialDelay, @NotNull Action0 action, long timeout) {
		runTxnAware(() -> scheduleActionCore(initialDelay, action, null, timeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleUnsafe(long initialDelay, @NotNull Action0 action) {
		return scheduleActionCore(initialDelay, action, null, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleUnsafe(long initialDelay, @NotNull Action0 action, long timeout) {
		return scheduleActionCore(initialDelay, action, null, timeout);
	}

	static @NotNull ScheduledFuture<?> scheduleActionCore(long initialDelay, @NotNull Action0 action,
	                                                      @Nullable String aName, long timeout) {
		return threadPoolScheduled.schedule(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				action.run();
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : "schedule", e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && action != null)
					ZezeCounter.instance.addTaskRunTime(aName != null ? aName : action.getClass(),
							System.nanoTime() - timeBegin);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static <R> @NotNull Future<R> scheduleUnsafe(long initialDelay, @NotNull Func0<R> func) {
		return scheduleFunc0Core(initialDelay, func, null, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static <R> @NotNull Future<R> scheduleUnsafe(long initialDelay, @NotNull Func0<R> func, long timeout) {
		return scheduleFunc0Core(initialDelay, func, null, timeout);
	}

	// Func0 入池执行：返回值与异常经 Future 传播（异常同时记日志，与 scheduleFunc0Core 一致）。
	static <R> @NotNull Future<R> submitFunc0Core(@NotNull Func0<R> func, @Nullable String name,
	                                              @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var future = new TaskCompletionSource<R>();
			try {
				future.setResult(func.call());
			} catch (Exception e) {
				//noinspection ConstantValue,UnreachableCode
				logger.error("{} exception:", name != null ? name : func != null ? func.getClass().getName() : "", e);
				future.setException(e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && func != null)
					ZezeCounter.instance.addTaskRunTime(name != null ? name : func.getClass(),
							System.nanoTime() - timeBegin);
			}
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return func.call();
			} catch (Throwable e) { // logger.error
				//noinspection ConstantValue,UnreachableCode
				logger.error("{} exception:", name != null ? name : func != null ? func.getClass().getName() : "", e);
				throw forceThrow(e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && func != null)
					ZezeCounter.instance.addTaskRunTime(name != null ? name : func.getClass(),
							System.nanoTime() - timeBegin);
			}
		});
	}

	static <R> @NotNull ScheduledFuture<R> scheduleFunc0Core(long initialDelay, @NotNull Func0<R> func,
	                                                         @Nullable String aName, long timeout) {
		return threadPoolScheduled.schedule(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return func.call();
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : "schedule", e);
				throw forceThrow(e);
			} finally {
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && func != null)
					ZezeCounter.instance.addTaskRunTime(aName != null ? aName : func.getClass(),
							System.nanoTime() - timeBegin);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	// FuncLong 延迟调度：结果码经 Future 传播，日志/统计在 callFuncCore 内完成（不重复计数）。
	static @NotNull ScheduledFuture<Long> scheduleFuncCore(long initialDelay, @NotNull FuncLong func,
	                                                       @Nullable String aName, long timeout) {
		return threadPoolScheduled.schedule(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return callFuncCore(func, null, null, aName);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : func.getClass().getName(), e);
				return Procedure.Exception;
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	// Procedure 延迟调度：结果码经 Future 传播，日志名固定使用 getActionName()。
	static @NotNull ScheduledFuture<Long> scheduleProcCore(long initialDelay, @NotNull Procedure procedure,
	                                                       long timeout) {
		return threadPoolScheduled.schedule(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return callProcCore(procedure, null, null);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", procedure, e);
				return Procedure.Exception;
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	// Func0 周期调度：周期任务无法携带返回值，结果丢弃，异常只记日志（与 schedulePeriodCore 一致）。
	static <R> @NotNull TimerFuture<R> scheduleFunc0PeriodCore(long initialDelay, long period,
	                                                           @NotNull Func0<R> func, @Nullable String aName,
	                                                           long timeout) {
		var future = new TimerFuture<R>();
		future.setFuture(threadPoolScheduled.scheduleWithFixedDelay(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			future.lock();
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				if (future.isCancelled())
					return;
				func.call();
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : "schedule", e);
			} finally {
				future.unlock();
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && func != null)
					ZezeCounter.instance.addTaskRunTime(aName != null ? aName : func.getClass(),
							System.nanoTime() - timeBegin);
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS));
		return future;
	}

	// FuncLong 周期调度：周期任务结果码无法携带，丢弃，日志/统计照常在 callFuncCore 内完成。
	static @NotNull TimerFuture<Long> scheduleFuncPeriodCore(long initialDelay, long period,
	                                                         @NotNull FuncLong func, @Nullable String aName,
	                                                         long timeout) {
		var future = new TimerFuture<Long>();
		future.setFuture(threadPoolScheduled.scheduleWithFixedDelay(() -> {
			future.lock();
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				if (future.isCancelled())
					return;
				callFuncCore(func, null, null, aName);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : func.getClass().getName(), e);
			} finally {
				future.unlock();
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS));
		return future;
	}

	// Procedure 周期调度：周期任务结果码无法携带，丢弃，日志名固定使用 getActionName()。
	static @NotNull TimerFuture<Long> scheduleProcPeriodCore(long initialDelay, long period,
	                                                         @NotNull Procedure procedure, long timeout) {
		var future = new TimerFuture<Long>();
		future.setFuture(threadPoolScheduled.scheduleWithFixedDelay(() -> {
			future.lock();
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				if (future.isCancelled())
					return;
				callProcCore(procedure, null, null);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", procedure, e);
			} finally {
				future.unlock();
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS));
		return future;
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void scheduleAt(int hour, int minute, @NotNull Action0 action) {
		scheduleAt(hour, minute, -1, action);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void scheduleAt(int hour, int minute, long period, @NotNull Action0 action) {
		runTxnAware(() -> scheduleAtCore(hour, minute, period, action, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void scheduleAt(int hour, int minute, long period, @NotNull Action0 action, long timeout) {
		runTxnAware(() -> scheduleAtCore(hour, minute, period, action, null, timeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, @NotNull Action0 action) {
		return scheduleAtUnsafe(hour, minute, -1, action);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, long period,
	                                                           @NotNull Action0 action) {
		return scheduleAtCore(hour, minute, period, action, null, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull ScheduledFuture<?> scheduleAtUnsafe(int hour, int minute, long period,
	                                                           @NotNull Action0 action, long timeout) {
		return scheduleAtCore(hour, minute, period, action, null, timeout);
	}

	static long scheduleAtDelay(int hour, int minute) {
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, hour);
		firstTime.set(Calendar.MINUTE, minute);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);
		if (firstTime.before(Calendar.getInstance())) // 如果第一次的时间比当前时间早，推到明天。
			firstTime.add(Calendar.DAY_OF_MONTH, 1); // tomorrow!
		return firstTime.getTime().getTime() - System.currentTimeMillis();
	}

	static @NotNull ScheduledFuture<?> scheduleAtCore(int hour, int minute, long period,
	                                                  @NotNull Action0 action, @Nullable String aName,
	                                                  long timeout) {
		var delay = scheduleAtDelay(hour, minute);
		if (period > 0)
			return schedulePeriodCore(delay, period, action, aName, timeout);
		return scheduleActionCore(delay, action, aName, timeout);
	}

	static <R> @NotNull ScheduledFuture<R> scheduleAtFunc0Core(int hour, int minute, long period,
	                                                           @NotNull Func0<R> func, @Nullable String aName,
	                                                           long timeout) {
		var delay = scheduleAtDelay(hour, minute);
		if (period > 0)
			return scheduleFunc0PeriodCore(delay, period, func, aName, timeout);
		return scheduleFunc0Core(delay, func, aName, timeout);
	}

	static @NotNull ScheduledFuture<Long> scheduleAtFuncCore(int hour, int minute, long period,
	                                                         @NotNull FuncLong func, @Nullable String aName,
	                                                         long timeout) {
		var delay = scheduleAtDelay(hour, minute);
		if (period > 0)
			return scheduleFuncPeriodCore(delay, period, func, aName, timeout);
		return scheduleFuncCore(delay, func, aName, timeout);
	}

	static @NotNull ScheduledFuture<Long> scheduleAtProcCore(int hour, int minute, long period,
	                                                         @NotNull Procedure procedure, long timeout) {
		var delay = scheduleAtDelay(hour, minute);
		if (period > 0)
			return scheduleProcPeriodCore(delay, period, procedure, timeout);
		return scheduleProcCore(delay, procedure, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void schedule(long initialDelay, long period, @NotNull Action0 action) {
		runTxnAware(() -> schedulePeriodCore(initialDelay, period, action, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void schedule(long initialDelay, long period, @NotNull Action0 action, long timeout) {
		runTxnAware(() -> schedulePeriodCore(initialDelay, period, action, null, timeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull TimerFuture<?> scheduleUnsafe(long initialDelay, long period, @NotNull Action0 action) {
		return schedulePeriodCore(initialDelay, period, action, null, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull TimerFuture<?> scheduleUnsafe(long initialDelay, long period, @NotNull Action0 action,
	                                                     long timeout) {
		return schedulePeriodCore(initialDelay, period, action, null, timeout);
	}

	static @NotNull TimerFuture<?> schedulePeriodCore(long initialDelay, long period, @NotNull Action0 action,
	                                                  @Nullable String aName, long timeout) {
		var future = new TimerFuture<>();
		future.setFuture(threadPoolScheduled.scheduleWithFixedDelay(() -> {
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			future.lock();
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				//System.out.println(action);
				if (future.isCancelled())
					return;
				action.run();
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : "schedule", e);
			} finally {
				future.unlock();
				//noinspection ConstantValue
				if (ZezeCounter.instance != null && action != null)
					ZezeCounter.instance.addTaskRunTime(aName != null ? aName : action.getClass(),
							System.nanoTime() - timeBegin);
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS));
		return future;
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

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return call(func, p, null, null);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                        @Nullable ProtocolErrorHandle actionWhenError) {
		return call(func, p, actionWhenError, null);
	}

	public static @NotNull Throwable getRootCause(@NotNull Throwable e) {
		for (; ; ) {
			var c = e.getCause();
			if (c == null)
				return e;
			e = c;
		}
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                        @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		return callFuncCore(func, p, actionWhenError, aName);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static long callFuncCore(@NotNull FuncLong func, @Nullable Protocol<?> p,
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

	// 以下无协议重载供 TaskSpec 使用，避免 TaskSpec 依赖 Zeze.Net；协议感知路径见 Zeze.Net.ProtocolDispatch。
	static long callFuncCore(@NotNull FuncLong func, @Nullable String aName) {
		return callFuncCore(func, null, null, aName);
	}

	static long callProcCore(@NotNull Procedure procedure) {
		return callProcCore(procedure, null, null);
	}

	static @NotNull Future<Long> submitFuncCore(@NotNull FuncLong func, @Nullable String aName,
	                                            @Nullable DispatchMode mode, long timeout) {
		return submitFuncCore(func, null, null, aName, mode, timeout);
	}

	static void executeFuncCore(@NotNull FuncLong func, @Nullable String aName,
	                            @Nullable DispatchMode mode, long timeout) {
		executeFuncCore(func, null, null, aName, mode, timeout);
	}

	static @NotNull Future<Long> submitProcCore(@NotNull Procedure procedure,
	                                            @Nullable DispatchMode mode, long timeout) {
		return submitProcCore(procedure, null, null, mode, timeout);
	}

	static void executeProcCore(@NotNull Procedure procedure, @Nullable DispatchMode mode, long timeout) {
		executeProcCore(procedure, null, null, mode, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		runTxnAware(() -> executeFuncCore(func, p, null, null, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                       @Nullable ProtocolErrorHandle actionWhenError) {
		runTxnAware(() -> executeFuncCore(func, p, actionWhenError, null, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                       @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName) {
		runTxnAware(() -> executeFuncCore(func, p, actionWhenError, aName, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。注意本方法保留老语义：mode=Direct 跳过事务检查立即执行；TaskSpec 的 run() 中 Direct 不再跳过事务延迟。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                       @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                       @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct)
			executeFuncCore(func, p, actionWhenError, aName, mode, defaultTimeout);
		else
			runTxnAware(() -> executeFuncCore(func, p, actionWhenError, aName, mode, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。注意本方法保留老语义：mode=Direct 跳过事务检查立即执行；TaskSpec 的 run() 中 Direct 不再跳过事务延迟。 */
	@Deprecated
	public static void run(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                       @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                       @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct)
			executeFuncCore(func, p, actionWhenError, aName, mode, timeout);
		else
			runTxnAware(() -> executeFuncCore(func, p, actionWhenError, aName, mode, timeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return runUnsafe(func, p, null, null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                              @Nullable ProtocolErrorHandle actionWhenError) {
		return runUnsafe(func, p, actionWhenError, null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                              @Nullable ProtocolErrorHandle actionWhenError, String aName) {
		return runUnsafe(func, p, actionWhenError, aName, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                              @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                                              @Nullable DispatchMode mode) {
		return runUnsafe(func, p, actionWhenError, aName, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                              @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                                              @Nullable DispatchMode mode, long timeout) {
		return submitFuncCore(func, p, actionWhenError, aName, mode, timeout);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static @NotNull Future<Long> submitFuncCore(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                               @Nullable ProtocolErrorHandle actionWhenError,
	                                               @Nullable String aName, @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(callFuncCore(func, p, actionWhenError, aName));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return callFuncCore(func, p, actionWhenError, aName);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : (p != null ? p.getClass().getName() : null), e);
				return Procedure.Exception;
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		executeUnsafe(func, p, null, null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                 @Nullable ProtocolErrorHandle actionWhenError) {
		executeUnsafe(func, p, actionWhenError, null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                 @Nullable ProtocolErrorHandle actionWhenError, String aName) {
		executeUnsafe(func, p, actionWhenError, aName, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                                 @Nullable DispatchMode mode) {
		executeUnsafe(func, p, actionWhenError, aName, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                 @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                                 @Nullable DispatchMode mode, long timeout) {
		executeFuncCore(func, p, actionWhenError, aName, mode, timeout);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static void executeFuncCore(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                  @Nullable ProtocolErrorHandle actionWhenError, @Nullable String aName,
	                                  @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			callFuncCore(func, p, actionWhenError, aName);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				callFuncCore(func, p, actionWhenError, aName);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", aName != null ? aName : (p != null ? p.getClass().getName() : null), e);
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure) {
		return call(procedure, (Protocol<?>)null, null);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		return call(procedure, from, null);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                        @Nullable ProtocolErrorHandle actionWhenError) {
		return callProcCore(procedure, from, actionWhenError);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static long callProcCore(@NotNull Procedure procedure, @Nullable Protocol<?> from,
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

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static long call(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
	                        @Nullable ProtocolErrorHandle actionWhenError) {
		return callProcOutCore(procedure, outProtocol, actionWhenError);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static long callProcOutCore(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
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

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure) {
		runTxnAware(() -> executeProcCore(procedure, null, null, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		runTxnAware(() -> executeProcCore(procedure, from, null, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                       @Nullable ProtocolErrorHandle actionWhenError) {
		runTxnAware(() -> executeProcCore(procedure, from, actionWhenError, null, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。注意本方法保留老语义：mode=Direct 跳过事务检查立即执行；TaskSpec 的 run() 中 Direct 不再跳过事务延迟。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                       @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode) {
		if (mode == DispatchMode.Direct)
			executeProcCore(procedure, from, actionWhenError, mode, defaultTimeout);
		else
			runTxnAware(() -> executeProcCore(procedure, from, actionWhenError, mode, defaultTimeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。注意本方法保留老语义：mode=Direct 跳过事务检查立即执行；TaskSpec 的 run() 中 Direct 不再跳过事务延迟。 */
	@Deprecated
	public static void run(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                       @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct)
			executeProcCore(procedure, from, actionWhenError, mode, timeout);
		else
			runTxnAware(() -> executeProcCore(procedure, from, actionWhenError, mode, timeout));
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure) {
		return runUnsafe(procedure, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		return runUnsafe(procedure, from, null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                              @Nullable ProtocolErrorHandle actionWhenError) {
		return runUnsafe(procedure, from, actionWhenError, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		return runUnsafe(procedure, (Protocol<?>)null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                              @Nullable ProtocolErrorHandle actionWhenError,
	                                              @Nullable DispatchMode mode) {
		return runUnsafe(procedure, from, actionWhenError, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                              @Nullable ProtocolErrorHandle actionWhenError,
	                                              @Nullable DispatchMode mode, long timeout) {
		return submitProcCore(procedure, from, actionWhenError, mode, timeout);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static @NotNull Future<Long> submitProcCore(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                               @Nullable ProtocolErrorHandle actionWhenError,
	                                               @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(callProcCore(procedure, from, actionWhenError));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return callProcCore(procedure, from, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", procedure, e);
				return Procedure.Exception;
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure,
	                                              @NotNull OutObject<Protocol<?>> outProtocol,
	                                              @Nullable ProtocolErrorHandle actionWhenError,
	                                              @Nullable DispatchMode mode) {
		return runUnsafe(procedure, outProtocol, actionWhenError, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runUnsafe(@NotNull Procedure procedure,
	                                              @NotNull OutObject<Protocol<?>> outProtocol,
	                                              @Nullable ProtocolErrorHandle actionWhenError,
	                                              @Nullable DispatchMode mode, long timeout) {
		return submitProcOutCore(procedure, outProtocol, actionWhenError, mode, timeout);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static @NotNull Future<Long> submitProcOutCore(@NotNull Procedure procedure,
	                                                  @NotNull OutObject<Protocol<?>> outProtocol,
	                                                  @Nullable ProtocolErrorHandle actionWhenError,
	                                                  @Nullable DispatchMode mode, long timeout) {
		if (mode == DispatchMode.Direct) {
			var future = new TaskCompletionSource<Long>();
			future.setResult(callProcOutCore(procedure, outProtocol, actionWhenError));
			return future;
		}

		return (mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).submit(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				return callProcOutCore(procedure, outProtocol, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", procedure, e);
				return Procedure.Exception;
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure) {
		executeUnsafe(procedure, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from) {
		executeUnsafe(procedure, from, null, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                 @Nullable ProtocolErrorHandle actionWhenError) {
		executeUnsafe(procedure, from, actionWhenError, DispatchMode.Normal);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		executeUnsafe(procedure, (Protocol<?>)null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode) {
		executeUnsafe(procedure, from, actionWhenError, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode,
	                                 long timeout) {
		executeProcCore(procedure, from, actionWhenError, mode, timeout);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static void executeProcCore(@NotNull Procedure procedure, @Nullable Protocol<?> from,
	                                  @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode,
	                                  long timeout) {
		if (mode == DispatchMode.Direct) {
			callProcCore(procedure, from, actionWhenError);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				callProcCore(procedure, from, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", procedure, e);
			}
		});
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
	                                 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode) {
		executeUnsafe(procedure, outProtocol, actionWhenError, mode, defaultTimeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeUnsafe(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
	                                 @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode,
	                                 long timeout) {
		executeProcOutCore(procedure, outProtocol, actionWhenError, mode, timeout);
	}

	/** 框架层（Zeze.Net 等）复用的核心方法；应用层请使用 {@link TaskSpec}。 */
	public static void executeProcOutCore(@NotNull Procedure procedure, @NotNull OutObject<Protocol<?>> outProtocol,
	                                     @Nullable ProtocolErrorHandle actionWhenError, @Nullable DispatchMode mode,
	                                     long timeout) {
		if (mode == DispatchMode.Direct) {
			callProcOutCore(procedure, outProtocol, actionWhenError);
			return;
		}

		(mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault).execute(() -> {
			try (var ignoredHot = hotGuard.create(); var ignored = createTimeout(timeout)) {
				callProcOutCore(procedure, outProtocol, actionWhenError);
			} catch (Throwable e) { // logger.error
				logger.error("{} exception:", procedure, e);
			}
		});
	}

	// RpcResponse 族与普通族（call 核参数为 null 时）完全等价，这里直接委托普通族。
	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void runRpcResponse(@NotNull Procedure procedure) {
		run(procedure);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void runRpcResponse(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		run(procedure, null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void runRpcResponse(@NotNull Procedure procedure, @Nullable DispatchMode mode, long timeout) {
		run(procedure, null, null, mode, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		run(func, p);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p, @Nullable DispatchMode mode) {
		run(func, p, null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void runRpcResponse(@NotNull FuncLong func, @Nullable Protocol<?> p, @Nullable DispatchMode mode,
	                                  long timeout) {
		run(func, p, null, null, mode, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure) {
		return runUnsafe(procedure);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure,
	                                                         @Nullable DispatchMode mode) {
		return runUnsafe(procedure, (Protocol<?>)null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull Procedure procedure,
	                                                         @Nullable DispatchMode mode, long timeout) {
		return runUnsafe(procedure, (Protocol<?>)null, null, mode, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		return runUnsafe(func, p);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                                         @Nullable DispatchMode mode) {
		return runUnsafe(func, p, null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static @NotNull Future<Long> runRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                                         @Nullable DispatchMode mode, long timeout) {
		return runUnsafe(func, p, null, null, mode, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure) {
		executeUnsafe(procedure);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode) {
		executeUnsafe(procedure, (Protocol<?>)null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull Procedure procedure, @Nullable DispatchMode mode,
	                                            long timeout) {
		executeUnsafe(procedure, (Protocol<?>)null, null, mode, timeout);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p) {
		executeUnsafe(func, p);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                            @Nullable DispatchMode mode) {
		executeUnsafe(func, p, null, null, mode);
	}

	/** @deprecated 请使用 {@link TaskSpec}：ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter + 终结方法。 */
	@Deprecated
	public static void executeRpcResponseUnsafe(@NotNull FuncLong func, @Nullable Protocol<?> p,
	                                            @Nullable DispatchMode mode, long timeout) {
		executeUnsafe(func, p, null, null, mode, timeout);
	}

	public static void waitAll(@NotNull Collection<Future<?>> tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				forceThrow(e);
			}
		}
	}

	public static void waitAll(Future<?> @NotNull [] tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				forceThrow(e);
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
