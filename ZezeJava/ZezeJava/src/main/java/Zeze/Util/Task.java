package Zeze.Util;

import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import Zeze.Application;
import Zeze.IModule;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Raft.RaftRetryException;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ProcedureStatistics;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Task implements Future<Long> {
	static final Logger logger = LogManager.getLogger(Task.class);
	private static ThreadPoolExecutor threadPoolDefault;
	private static ScheduledExecutorService threadPoolScheduled;
	//	private static final ThreadPoolExecutor rpcResponseThreadPool
	//			= (ThreadPoolExecutor)Executors.newCachedThreadPool(new ThreadFactoryWithName("ZezeRespPool"));
	public static volatile Action4<Level, Throwable, Long, String> LogAction = Task::DefaultLogAction;

	public static ThreadPoolExecutor getThreadPool() {
		return threadPoolDefault;
	}

	public static ScheduledExecutorService getScheduledThreadPool() {
		return threadPoolScheduled;
	}

	public static synchronized void initThreadPool(ThreadPoolExecutor pool, ScheduledExecutorService scheduled) {
		if (pool == null || scheduled == null)
			throw new IllegalArgumentException();

		if (threadPoolDefault != null || threadPoolScheduled != null)
			throw new IllegalStateException("ThreadPool Has Inited.");
		threadPoolDefault = pool;
		threadPoolScheduled = scheduled;
	}

	public static synchronized boolean tryInitThreadPool(Application app, ThreadPoolExecutor pool,
														 ScheduledThreadPoolExecutor scheduled) {
		if (threadPoolDefault != null || threadPoolScheduled != null)
			return false;

		if (pool == null) {
			int workerThreads = app == null ? 240 : (app.getConfig().getWorkerThreads() > 0
					? app.getConfig().getWorkerThreads() : Runtime.getRuntime().availableProcessors() * 30);
			threadPoolDefault = new ThreadPoolExecutor(workerThreads, workerThreads, 0, TimeUnit.NANOSECONDS,
					new LinkedBlockingQueue<>(), new ThreadFactoryWithName("ZezeTaskPool"));
		} else
			threadPoolDefault = pool;

		if (scheduled == null) {
			int workerThreads = app == null ? 120 : (app.getConfig().getScheduledThreads() > 0
					? app.getConfig().getScheduledThreads() : Runtime.getRuntime().availableProcessors() * 15);
			threadPoolScheduled = new ScheduledThreadPoolExecutor(workerThreads,
					new ThreadFactoryWithName("ZezeScheduledPool"));
		} else
			threadPoolScheduled = scheduled;
		return true;
	}

	public static void Call(Action0 action, String name) {
		try {
			action.run();
		} catch (Throwable ex) {
			logger.error(name != null ? name : action != null ? action.getClass().getName() : "", ex);
		}
	}

	public static long Call(FuncLong func, String name) {
		try {
			return func.call();
		} catch (Throwable ex) {
			logger.error(name != null ? name : func != null ? func.getClass().getName() : "", ex);
			return Procedure.Exception;
		}
	}

	Future<?> future;

	@Deprecated
	public void Cancel() {
		future.cancel(false);
	}

	@Deprecated
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Deprecated
	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Deprecated
	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Deprecated
	@Override
	public Long get() throws InterruptedException, ExecutionException {
		return (Long)future.get();
	}

	@Deprecated
	@Override
	public Long get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return (Long)future.get(timeout, unit);
	}

	private Task(Future<?> future) {
		this.future = future;
	}

	public static Future<?> run(Action0 action, String actionName) {
		return threadPoolDefault.submit(() -> Call(action, actionName));
	}

	@Deprecated
	public static Task Run(Action0 action, String actionName) {
		return new Task(threadPoolDefault.submit(() -> {
			Call(action, actionName);
			return 0L;
		}));
	}

	public static Future<?> schedule(long initialDelay, Action0 action) {
		return threadPoolScheduled.schedule(() -> {
			try {
				action.run();
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
			} catch (Throwable e) {
				logger.error("schedule", e);
			}
		}, initialDelay, period, TimeUnit.MILLISECONDS);
	}

	public static <R> Future<R> schedule(long initialDelay, Func0<R> func) {
		return threadPoolScheduled.schedule(() -> {
			try {
				return func.call();
			} catch (Exception | Error e) {
				logger.error("schedule", e);
				throw e;
			} catch (Throwable e) {
				logger.error("schedule", e);
				throw new RuntimeException(e);
			}
		}, initialDelay, TimeUnit.MILLISECONDS);
	}

	private static class SchedulerTask extends Task implements Callable<Integer>, Runnable {
		private final SchedulerHandle SchedulerHandle;

		SchedulerTask(SchedulerHandle handle) {
			super(null);
			SchedulerHandle = handle;
		}

		@Override
		public void run() {
			call();
		}

		@Override
		public Integer call() {
			try {
				SchedulerHandle.handle(this);
			} catch (Throwable ex) {
				logger.error("SchedulerTask", ex);
			}
			return 0;
		}
	}

	@Deprecated
	public static Task schedule(SchedulerHandle s, long initialDelay) {
		var task = new SchedulerTask(s);
		task.future = threadPoolScheduled.schedule((Callable<Integer>)task, initialDelay, TimeUnit.MILLISECONDS);
		return task;
	}

	@Deprecated
	public static Task schedule(SchedulerHandle s, long initialDelay, long period) {
		var task = new SchedulerTask(s);
		task.future = threadPoolScheduled.scheduleWithFixedDelay(task, initialDelay, period, TimeUnit.MILLISECONDS);
		return task;
	}

	public static void DefaultLogAction(Level level, Throwable ex, Long result, String msg) {
		// exception -> Error
		// 0 != result -> level from parameter
		// others -> Trace
		Level ll = (ex != null) ? Level.ERROR : (result != 0) ? level : Level.TRACE;
		final var module = result > 0 ? "@" + IModule.GetModuleId(result) + ":" + IModule.GetErrorCode(result) : "";
		logger.log(ll, () -> msg + " Return=" + result + module, ex);
	}

	public static void LogAndStatistics(long result, Protocol<?> p, boolean IsRequestSaved) {
		LogAndStatistics(null, result, p, IsRequestSaved);
	}

	public static void LogAndStatistics(Throwable ex, long result, Protocol<?> p, boolean IsRequestSaved) {
		LogAndStatistics(ex, result, p, IsRequestSaved, null);
	}

	public static void LogAndStatistics(Throwable ex, long result, Protocol<?> p, boolean IsRequestSaved, String aName) {
		final var actionName = null != aName ? aName : IsRequestSaved ? p.getClass().getName() : p.getClass().getName() + ":Response";
		var logLevel = p.getService() != null && p.getService().getZeze() != null
				? p.getService().getZeze().getConfig().getProcessReturnErrorLogLevel()
				: Level.TRACE;
		var tmpVolatile = LogAction;
		if (tmpVolatile != null) {
			try {
				tmpVolatile.run(logLevel, ex, result, "Action=" + actionName + " UserState=" + p.getUserState());
			} catch (Throwable e) {
				logger.error("LogAction Exception", e);
			}
		}
		ProcedureStatistics.getInstance().GetOrAdd(actionName).GetOrAdd(result).incrementAndGet();
	}

	public static long Call(FuncLong func, Protocol<?> p) {
		return Call(func, p, null);
	}

	public static long Call(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		return Call(func, p, actionWhenError, null);
	}

	public static long Call(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError, String aName) {
		boolean IsRequestSaved = p.isRequest(); // 记住这个，以后可能会被改变。
		try {
			var result = func.call();
			if (result != 0 && IsRequestSaved && actionWhenError != null)
				actionWhenError.handle(p, result);
			LogAndStatistics(null, result, p, IsRequestSaved, aName);
			return result;
		} catch (Throwable ex) {
			long errorCode;
			for (Throwable rootEx = ex, cause; ; rootEx = cause) {
				if ((cause = rootEx.getCause()) == null) {
					if (rootEx instanceof TaskCanceledException)
						errorCode = Procedure.CancelException;
					else if (rootEx instanceof RaftRetryException)
						errorCode = Procedure.RaftRetry;
					else
						errorCode = Procedure.Exception;
					break;
				}
			}

			if (IsRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.handle(p, errorCode);
				} catch (Throwable e) {
					logger.error(e);
				}
			}
			LogAndStatistics(ex, errorCode, p, IsRequestSaved, aName);
			return errorCode;
		}
	}

	public static Future<Long> run(FuncLong func, Protocol<?> p) {
		return threadPoolDefault.submit(() -> Call(func, p, null));
	}

	public static Future<Long> run(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		return threadPoolDefault.submit(() -> Call(func, p, actionWhenError));
	}

	@Deprecated
	public static Task Run(FuncLong func, Protocol<?> p) {
		return new Task(threadPoolDefault.submit(() -> Call(func, p, null)));
	}

	@Deprecated
	public static Task Run(FuncLong func, Protocol<?> p, ProtocolErrorHandle actionWhenError) {
		return new Task(threadPoolDefault.submit(() -> Call(func, p, actionWhenError)));
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
			return result;
		} catch (Throwable ex) {
			// Procedure.Call处理了所有错误。应该不会到这里。除非内部错误。
			if (isRequestSaved && actionWhenError != null) {
				try {
					actionWhenError.run(from, Procedure.Exception);
				} catch (Throwable e) {
					logger.error("ActionWhenError Exception", e);
				}
			}
			logger.error(procedure.getActionName(), ex);
			return Procedure.Exception;
		}
	}

	public static Future<Long> run(Procedure procedure) {
		return threadPoolDefault.submit(() -> Call(procedure, null, null));
	}

	public static Future<Long> run(Procedure procedure, Protocol<?> from) {
		return threadPoolDefault.submit(() -> Call(procedure, from, null));
	}

	public static Future<Long> run(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError) {
		return threadPoolDefault.submit(() -> Call(procedure, from, actionWhenError));
	}

	public static Future<Long> runRpcResponse(Procedure procedure) {
		return threadPoolDefault.submit(() -> Call(procedure, null, null)); // rpcResponseThreadPool
	}

	public static Future<Long> runRpcResponse(FuncLong func, Protocol<?> p) {
		return threadPoolDefault.submit(() -> Call(func, p, null)); // rpcResponseThreadPool
	}

	@Deprecated
	public static Task Run(Procedure procedure) {
		return new Task(threadPoolDefault.submit(() -> Call(procedure, null, null)));
	}

	@Deprecated
	public static Task Run(Procedure procedure, Protocol<?> from) {
		return new Task(threadPoolDefault.submit(() -> Call(procedure, from, null)));
	}

	@Deprecated
	public static Task Run(Procedure procedure, Protocol<?> from, Action2<Protocol<?>, Long> actionWhenError) {
		return new Task(threadPoolDefault.submit(() -> Call(procedure, from, actionWhenError)));
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

	@Deprecated
	public static void WaitAll(Collection<Task> tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Deprecated
	public static void WaitAll(Task[] tasks) {
		for (var task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
