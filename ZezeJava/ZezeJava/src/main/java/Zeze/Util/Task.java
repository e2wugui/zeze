package Zeze.Util;

import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.IModule;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Net.Service;
import Zeze.Raft.RaftRetryException;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Task {
	public interface ILogAction {
		void run(Throwable ex, long result, Protocol<?> p, String actionName);
	}

	static final Logger logger = LogManager.getLogger(Task.class);
	private static ExecutorService threadPoolDefault;
	private static ScheduledExecutorService threadPoolScheduled;
	private static ExecutorService threadPoolCritical; // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
	// private static final ThreadPoolExecutor rpcResponseThreadPool
	//			= (ThreadPoolExecutor)Executors.newCachedThreadPool(new ThreadFactoryWithName("ZezeRespPool"));
	public static ILogAction LogAction = Task::DefaultLogAction;

	static {
		ShutdownHook.init();
	}

	public static boolean isVirtualThreadEnabled() {
		return ThreadFactoryWithName.isVirtualThreadEnabled();
	}

	public static ExecutorService getThreadPool() {
		return threadPoolDefault;
	}

	public static ScheduledExecutorService getScheduledThreadPool() {
		return threadPoolScheduled;
	}

	public static ExecutorService getCriticalThreadPool() {
		return threadPoolCritical;
	}

	// 固定数量的线程池, 普通优先级, 自动支持虚拟线程, 用于处理普通任务
	public static ExecutorService newFixedThreadPool(int threadCount, String threadNamePrefix) {
		try {
			//noinspection JavaReflectionMemberAccess
			var r = (ExecutorService)Executors.class.getMethod("newVirtualThreadPerTaskExecutor", (Class<?>[])null).invoke(null);
			logger.info("newFixedThreadPool({},{}) use virtual thread pool", threadCount, threadNamePrefix);
			return r;
		} catch (ReflectiveOperationException ignored) {
		}

		return Executors.newFixedThreadPool(threadCount, new ThreadFactoryWithName(threadNamePrefix));
	}

	// 关键线程池, 普通优先级+1, 不使用虚拟线程, 线程数按需增长, 用于处理关键任务, 比普通任务的处理更及时
	public static ExecutorService newCriticalThreadPool(String threadNamePrefix) {
		return Executors.newCachedThreadPool(new ThreadFactoryWithName(threadNamePrefix) {
			@Override
			public Thread newThread(Runnable r) {
				var t = new Thread(null, r, namePrefix + threadNumber.getAndIncrement(), 0);
				t.setDaemon(true);
				t.setPriority(Thread.NORM_PRIORITY + 2);
				t.setUncaughtExceptionHandler((__, e) -> logger.error("fatal exception", e));
				return t;
			}
		});
	}

	public static synchronized void initThreadPool(ExecutorService pool, ScheduledExecutorService scheduled) {
		if (pool == null || scheduled == null)
			throw new IllegalArgumentException();

		if (threadPoolDefault != null || threadPoolScheduled != null)
			throw new IllegalStateException("ThreadPool Has Inited.");
		threadPoolDefault = pool;
		threadPoolScheduled = scheduled;
		threadPoolCritical = newCriticalThreadPool("ZezeCriticalPool");
	}

	public static synchronized boolean tryInitThreadPool(Application app, ExecutorService pool,
														 ScheduledExecutorService scheduled) {
		if (threadPoolDefault != null || threadPoolScheduled != null)
			return false;

		if (pool == null) {
			int workerThreads = app == null ? 240 : (app.getConfig().getWorkerThreads() > 0
					? app.getConfig().getWorkerThreads() : Runtime.getRuntime().availableProcessors() * 30);
			threadPoolDefault = newFixedThreadPool(workerThreads, "ZezeTaskPool");
		} else
			threadPoolDefault = pool;

		if (scheduled == null) {
			int workerThreads = app == null ? 8 : (app.getConfig().getScheduledThreads() > 0
					? app.getConfig().getScheduledThreads() : Runtime.getRuntime().availableProcessors());
			threadPoolScheduled = Executors.newScheduledThreadPool(workerThreads,
					new ThreadFactoryWithName("ZezeScheduledPool"));
		} else
			threadPoolScheduled = scheduled;
		threadPoolCritical = newCriticalThreadPool("ZezeCriticalPool");
		return true;
	}

	public static void Call(Action0 action, String name) {
		try {
			action.run();
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable ex) {
			logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", ex);
		}
	}

	public static long Call(FuncLong func, String name) {
		try {
			return func.call();
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable ex) {
			logger.error("{}", name != null ? name : func != null ? func.getClass().getName() : "", ex);
			return Procedure.Exception;
		}
	}

	public static void run(Action0 action, String name) {
		runUnsafe(action, name);
	}

	public static void runWhileCommit(Action0 action, String name) {
		Transaction.whileCommit(() -> runUnsafe(action, name));
	}

	public static Future<?> runUnsafe(Action0 action, String name) {
		return runUnsafe(action, name, DispatchMode.Normal);
	}

	public static void run(Action0 action, String name, DispatchMode mode) {
		runUnsafe(action, name, mode);
	}

	public static void runWhileCommit(Action0 action, String name, DispatchMode mode) {
		Transaction.whileCommit(() -> runUnsafe(action, name, mode));
	}

	public static Future<?> runUnsafe(Action0 action, String name, DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			final var future = new TaskCompletionSource<Long>();
			try {
				action.run();
				future.SetResult(0L);
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable ex) {
				future.SetException(ex);
				logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", ex);
			}
			return future;
		}

		var pool = mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault;
		return pool.submit(() -> {
			try {
				action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable ex) {
				logger.error("{}", name != null ? name : action != null ? action.getClass().getName() : "", ex);
			}
		});
	}

	public static Future<?> schedule(long initialDelay, Action0 action) {
		return threadPoolScheduled.schedule(() -> {
			try {
				action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				logger.error("schedule", e);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	public static Future<?> scheduleAt(int hour, int minute, Action0 action) {
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, hour);
		firstTime.set(Calendar.MINUTE, minute);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);
		if (firstTime.before(Calendar.getInstance())) // 如果第一次的时间比当前时间早，推到明天。
			firstTime.add(Calendar.DAY_OF_MONTH, 1); // tomorrow!
		var delay = firstTime.getTime().getTime() - System.currentTimeMillis();
		return schedule(delay, action);
	}

	public static Future<?> schedule(long initialDelay, long period, Action0 action) {
		return threadPoolScheduled.scheduleWithFixedDelay(() -> {
			try {
				action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				logger.error("schedule", e);
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS);
	}

	public static <R> Future<R> schedule(long initialDelay, Func0<R> func) {
		return threadPoolScheduled.schedule(() -> {
			try {
				return func.call();
			} catch (AssertionError e) {
				throw e;
			} catch (Exception | Error e) {
				logger.error("schedule", e);
				throw e;
			} catch (Throwable e) {
				logger.error("schedule", e);
				throw new RuntimeException(e);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	public static void DefaultLogAction(Throwable ex, long result, Protocol<?> p, String actionName) {
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
			else {
				if (!logger.isDebugEnabled())
					return;
				level = Level.DEBUG;
			}
		} else {
			if (!logger.isTraceEnabled())
				return;
			level = Level.TRACE;
		}
		Object userState;
		String userStateStr = p != null && (userState = p.getUserState()) != null ? " UserState=" + userState : "";

		if (result > 0) {
			logger.log(level, "Action={}{} Return={}@{}:{}", actionName, userStateStr, result,
					IModule.GetModuleId(result), IModule.GetErrorCode(result), ex);
		} else
			logger.log(level, "Action={}{} Return={}", actionName, userStateStr, result, ex);
	}

	public static void LogAndStatistics(long result, Protocol<?> p, boolean IsRequestSaved) {
		LogAndStatistics(null, result, p, IsRequestSaved, null);
	}

	public static void LogAndStatistics(Throwable ex, long result, Protocol<?> p, boolean IsRequestSaved) {
		LogAndStatistics(ex, result, p, IsRequestSaved, null);
	}

	public static void LogAndStatistics(Throwable ex, long result, Protocol<?> p, boolean IsRequestSaved, String aName) {
		var actionName = null != aName ? aName : IsRequestSaved ? p.getClass().getName() : p.getClass().getName() + ":Response";
		var tmpVolatile = LogAction;
		if (tmpVolatile != null) {
			try {
				tmpVolatile.run(ex, result, p, actionName);
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				logger.error("LogAndStatistics Exception", e);
			}
		}
		if (Macro.EnableStatistics) {
			ProcedureStatistics.getInstance().GetOrAdd(actionName).GetOrAdd(result).increment();
		}
	}

	public static long Call(FuncLong func, Protocol<?> p) {
		return Call(func, p, null, null);
	}

	public static long Call(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		return Call(func, p, actionWhenError, null);
	}

	public static Throwable getRootCause(Throwable e) {
		for (; ; ) {
			var c = e.getCause();
			if (c == null)
				return e;
			e = c;
		}
	}

	public static long Call(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String aName) {
		boolean IsRequestSaved = p.isRequest(); // 记住这个，以后可能会被改变。
		try {
			var result = func.call();
			if (result != 0 && IsRequestSaved && actionWhenError != null)
				actionWhenError.handle(p, result);
			LogAndStatistics(null, result, p, IsRequestSaved, aName);
			return result;
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable ex) {
			long errorCode;
			var rootEx = getRootCause(ex);
			if (rootEx instanceof TaskCanceledException)
				errorCode = Procedure.CancelException;
			else if (rootEx instanceof RaftRetryException)
				errorCode = Procedure.RaftRetry;
			else
				errorCode = Procedure.Exception;

			if (IsRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.handle(p, errorCode);
				} catch (AssertionError e) {
					throw e;
				} catch (Throwable e) {
					logger.error("", e);
				}
			}
			LogAndStatistics(ex, errorCode, p, IsRequestSaved, aName);
			return errorCode;
		}
	}

	public static void run(FuncLong func, Protocol<?> p) {
		runUnsafe(func, p);
	}

	public static void runWhileCommit(FuncLong func, Protocol<?> p) {
		Transaction.whileCommit(() -> runUnsafe(func, p));
	}

	public static Future<Long> runUnsafe(FuncLong func, Protocol<?> p) {
		return runUnsafe(func, p, null, null, DispatchMode.Normal);
	}

	public static void run(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		runUnsafe(func, p, actionWhenError);
	}

	public static void runWhileCommit(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		Transaction.whileCommit(() -> runUnsafe(func, p, actionWhenError));
	}

	public static Future<Long> runUnsafe(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		return runUnsafe(func, p, actionWhenError, null, DispatchMode.Normal);
	}

	public static void run(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String specialName) {
		runUnsafe(func, p, actionWhenError, specialName);
	}

	public static void runWhileCommit(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String specialName) {
		Transaction.whileCommit(() -> runUnsafe(func, p, actionWhenError, specialName));
	}

	public static Future<Long> runUnsafe(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String specialName) {
		return runUnsafe(func, p, actionWhenError, specialName, DispatchMode.Normal);
	}

	public static void run(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String specialName, DispatchMode mode) {
		runUnsafe(func, p, actionWhenError, specialName, mode);
	}

	public static void runWhileCommit(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String specialName, DispatchMode mode) {
		Transaction.whileCommit(() -> runUnsafe(func, p, actionWhenError, specialName, mode));
	}

	public static Future<Long> runUnsafe(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String specialName, DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			final var future = new TaskCompletionSource<Long>();
			future.SetResult(Call(func, p, actionWhenError, specialName));
			return future;
		}

		var pool = mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault;
		return pool.submit(() -> Call(func, p, actionWhenError, specialName));
	}

	public static long Call(Procedure procedure) {
		return Call(procedure, null, null);
	}

	public static long Call(Procedure procedure, Protocol<?> from) {
		return Call(procedure, from, null);
	}

	public static long Call(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError) {
		boolean isRequestSaved = from != null && from.isRequest();
		try {
			// 日志在Call里面记录。因为要支持嵌套。
			// 统计在Call里面实现。
			long result = procedure.Call();
			if (result != 0 && isRequestSaved && actionWhenError != null)
				actionWhenError.run(from, result);
			LogAndStatistics(null, result, from, isRequestSaved, procedure.getActionName());
			return result;
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable ex) {
			// Procedure.Call处理了所有错误。应该不会到这里。除非内部错误。
			if (isRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.run(from, Procedure.Exception);
				} catch (AssertionError e) {
					throw e;
				} catch (Throwable e) {
					logger.error("ActionWhenError Exception", e);
				}
			}
			logger.error("{}", procedure.getActionName(), ex);
			return Procedure.Exception;
		}
	}

	public static void run(Procedure procedure) {
		runUnsafe(procedure);
	}

	public static void runWhileCommit(Procedure procedure) {
		Transaction.whileCommit(() -> runUnsafe(procedure));
	}

	public static Future<Long> runUnsafe(Procedure procedure) {
		return runUnsafe(procedure, null, null, DispatchMode.Normal);
	}

	public static void run(Procedure procedure, Protocol<?> from) {
		runUnsafe(procedure, from);
	}

	public static void runWhileCommit(Procedure procedure, Protocol<?> from) {
		Transaction.whileCommit(() -> runUnsafe(procedure, from));
	}

	public static Future<Long> runUnsafe(Procedure procedure, Protocol<?> from) {
		return runUnsafe(procedure, from, null, DispatchMode.Normal);
	}

	public static void run(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError) {
		runUnsafe(procedure, from, actionWhenError);
	}

	public static void runWhileCommit(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError) {
		Transaction.whileCommit(() -> runUnsafe(procedure, from, actionWhenError));
	}

	public static Future<Long> runUnsafe(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError) {
		return runUnsafe(procedure, from, actionWhenError, DispatchMode.Normal);
	}

	public static void run(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError, DispatchMode mode) {
		runUnsafe(procedure, from, actionWhenError, mode);
	}

	public static void runWhileCommit(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError, DispatchMode mode) {
		Transaction.whileCommit(() -> runUnsafe(procedure, from, actionWhenError, mode));
	}

	public static Future<Long> runUnsafe(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError, DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			final var future = new TaskCompletionSource<Long>();
			future.SetResult(Call(procedure, from, actionWhenError));
			return future;
		}

		var pool = mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault;
		return pool.submit(() -> Call(procedure, from, actionWhenError));
	}

	public static void runRpcResponse(Procedure procedure) {
		runRpcResponseUnsafe(procedure);
	}

	public static void runRpcResponseWhileCommit(Procedure procedure) {
		Transaction.whileCommit(() -> runRpcResponseUnsafe(procedure));
	}

	public static Future<Long> runRpcResponseUnsafe(Procedure procedure) {
		return runRpcResponseUnsafe(procedure, DispatchMode.Normal);
	}

	public static void runRpcResponse(Procedure procedure, DispatchMode mode) {
		runRpcResponseUnsafe(procedure, mode);
	}

	public static void runRpcResponseWhileCommit(Procedure procedure, DispatchMode mode) {
		Transaction.whileCommit(() -> runRpcResponseUnsafe(procedure, mode));
	}

	public static Future<Long> runRpcResponseUnsafe(Procedure procedure, DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			final var future = new TaskCompletionSource<Long>();
			future.SetResult(Call(procedure, null, null));
			return future;
		}

		var pool = mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault;
		return pool.submit(() -> Call(procedure, null, null)); // rpcResponseThreadPool
	}

	public static void runRpcResponse(FuncLong func, Protocol<?> p) {
		runRpcResponseUnsafe(func, p);
	}

	public static void runRpcResponseWhileCommit(FuncLong func, Protocol<?> p) {
		Transaction.whileCommit(() -> runRpcResponseUnsafe(func, p));
	}

	public static Future<Long> runRpcResponseUnsafe(FuncLong func, Protocol<?> p) {
		return runRpcResponseUnsafe(func, p, DispatchMode.Normal);
	}

	public static void runRpcResponse(FuncLong func, Protocol<?> p, DispatchMode mode) {
		runRpcResponseUnsafe(func, p, mode);
	}

	public static void runRpcResponseWhileCommit(FuncLong func, Protocol<?> p, DispatchMode mode) {
		Transaction.whileCommit(() -> runRpcResponseUnsafe(func, p, mode));
	}

	public static Future<Long> runRpcResponseUnsafe(FuncLong func, Protocol<?> p, DispatchMode mode) {
		if (mode == DispatchMode.Direct) {
			final var future = new TaskCompletionSource<Long>();
			future.SetResult(Call(func, p, null, null));
			return future;
		}

		var pool = mode == DispatchMode.Critical ? threadPoolCritical : threadPoolDefault;
		return pool.submit(() -> Call(func, p, null, null)); // rpcResponseThreadPool
	}

	public static void waitAll(Collection<Future<?>> tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void waitAll(Future<?>[] tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Task() {
	}
}
