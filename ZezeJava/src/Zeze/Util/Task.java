package Zeze.Util;

import Zeze.Transaction.*;

import java.util.concurrent.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Task extends java.util.concurrent.FutureTask<Integer> {
	private static final Logger logger = LogManager.getLogger(Task.class);

	private static final Object lock = new Object();
	private static ThreadPoolExecutor threadPoolDefault;
	private static ScheduledThreadPoolExecutor threadPoolScheduled;

	public static ThreadPoolExecutor getThreadPool() {
		return threadPoolDefault;
	}

	public static ScheduledThreadPoolExecutor getScheduledThreadPool() {
		return threadPoolScheduled;
	}

	public static void initThreadPool(ThreadPoolExecutor pool, ScheduledThreadPoolExecutor scheduled) {
		if (pool == null || scheduled == null)
			throw new IllegalArgumentException();

		synchronized (lock) {
			if (null != threadPoolDefault || null != threadPoolScheduled)
				throw new RuntimeException("ThreadPool Has Inited.");
			threadPoolDefault = pool;
			threadPoolScheduled = scheduled;
		}
	}

	public static boolean tryInitThreadPool(Zeze.Application app,
											ThreadPoolExecutor pool,
											ScheduledThreadPoolExecutor scheduled) {
		synchronized (lock) {
			if (null != threadPoolDefault || null != threadPoolScheduled)
				return false;

			if (null == pool) {
				int workerThreads = null == app ? 240 : (app.getConfig().getWorkerThreads() > 0
						? app.getConfig().getWorkerThreads()
						: Runtime.getRuntime().availableProcessors() * 30);
				threadPoolDefault = new java.util.concurrent.ThreadPoolExecutor(
						workerThreads, workerThreads, 0,
						TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>());
			}
			else {
				threadPoolDefault = pool;
			}

			if (null == scheduled) {
				int workerThreads = null == app ? 240 : (app.getConfig().getWorkerThreads() > 0
						? app.getConfig().getWorkerThreads()
						: Runtime.getRuntime().availableProcessors() * 30);
				threadPoolScheduled = new ScheduledThreadPoolExecutor(workerThreads);
			} else {
				threadPoolScheduled = scheduled;
			}
			return true;
		}
	}

	public static void Call(Runnable action, String actionName) {
		try {
			action.run();
		}
		catch (Throwable ex) {
			logger.error(actionName, ex);
		}
	}

	public static long Call(Callable<Integer> action, String actionName) {
		try {
			return action.call();
		}
		catch (Throwable ex) {
			logger.error(actionName, ex);
			return Zeze.Transaction.Procedure.Excption;
		}
	}

	public void Cancel() {
		super.cancel(false);
	}

	public Task(Runnable action) {
		super(action, 0);
	}
	
	public Task(Callable<Integer> callable) {
		super(callable);
	}

	public static Task Run(Runnable action, String actionName) {
		var task = new Task(() -> Call(action, actionName));
		threadPoolDefault.execute(task);
		return task;
	}

	private static class SchedulerTask extends Task {
		private final SchedulerHandle SchedulerHandle;

		public SchedulerTask(SchedulerHandle handle) {
			super(SchedulerTask::fakecall);
			SchedulerHandle = handle;
		}
		
		private static int fakecall() {
			return 0;
		}

		@Override
		public void run() {
			try {
				SchedulerHandle.handle(this);
			} catch (Throwable ex) {
				logger.error("SchedulerTask", ex);
			} finally {
				super.run();
			}
		}
	}
	
	public static Task schedule(SchedulerHandle s, long initialDelay) {
		var task = new SchedulerTask(s);
		threadPoolScheduled.schedule(task, initialDelay, TimeUnit.MILLISECONDS);
		return task;
	}

	public static Task schedule(SchedulerHandle s, long initialDelay, long period) {
		var task = new SchedulerTask(s);
		threadPoolScheduled.scheduleWithFixedDelay(task, initialDelay, period, TimeUnit.MILLISECONDS);
		return task;
	}

	public static void LogAndStatistics(long result, Zeze.Net.Protocol p, boolean IsRequestSaved) {
		final var actionName = IsRequestSaved ? p.getClass().getName() : p.getClass().getName() + ":Response";
		if (result != 0) {
			var logLevel = (null != p.Service.getZeze())
					? p.Service.getZeze().getConfig().getProcessReturnErrorLogLevel()
					: Level.INFO;
			final var module = result > 0
					? "@" + Zeze.IModule.GetModuleId(result) + ":" + Zeze.IModule.GetErrorCode(result)
					: "";
			logger.log(logLevel, () -> "Task " + actionName + " Return=" + result + module + " UserState=" + p.getUserState());
		}
		ProcedureStatistics.getInstance().GetOrAdd(actionName).GetOrAdd(result).incrementAndGet();
	}


	public static long Call(Callable<Long> func, Zeze.Net.Protocol p) {
		return Call(func, p, null);
	}

	public static long Call(Callable<Long> func, Zeze.Net.Protocol p,
			Action2<Zeze.Net.Protocol, Long> actionWhenError) {
		boolean IsRequestSaved = p.isRequest(); // 记住这个，以后可能会被改变。
		try {
			var result = func.call();
			if (result != 0 && IsRequestSaved) {
				if (actionWhenError != null) {
					actionWhenError.run(p, result);
				}
			}
			LogAndStatistics(result, p, IsRequestSaved);
			return result;
		}
		catch (Throwable ex) {
			while (true) {
				var cause = ex.getCause();
				if (null == cause)
					break;
				ex = cause;
			}

			long errorCode = ex instanceof TaskCanceledException ? Procedure.CancelExcption : Procedure.Excption;

			if (IsRequestSaved) {
				if (actionWhenError != null) {
					actionWhenError.run(p, errorCode);
				}
			}

			LogAndStatistics(errorCode, p, IsRequestSaved);
			// 使用 InnerException
			logger.error(() -> "Task " + p.getClass().getName() + " Exception UserState=" + p.getUserState(), ex);
			return errorCode;
		}
	}


	public static Task Run(Callable<Long> func, Zeze.Net.Protocol p) {
		return Run(func, p, null);
	}

	public static Task Run(Callable<Long> func, Zeze.Net.Protocol p,
			Action2<Zeze.Net.Protocol, Long> actionWhenError) {
		var task = new Task(() -> Call(func, p, actionWhenError));
		threadPoolDefault.execute(task);
		return task;
	}


	public static long Call(Procedure procdure, Zeze.Net.Protocol from) {
		return Call(procdure, from, null);
	}

	public static long Call(Procedure procdure) {
		return Call(procdure, null, null);
	}

	public static long Call(Procedure procdure, Zeze.Net.Protocol from,
			Action2<Zeze.Net.Protocol, Long> actionWhenError) {
		Boolean isRequestSaved = from == null ? null : from.isRequest();
		try {
			// 日志在Call里面记录。因为要支持嵌套。
			// 统计在Call里面实现。
			long result = procdure.Call();
			if (result != 0 && isRequestSaved != null && isRequestSaved) {
				if (actionWhenError != null) {
					actionWhenError.run(from, result);
				}
			}
			return result;
		}
		catch (Throwable ex) {
			// Procedure.Call处理了所有错误。应该不会到这里。除非内部错误。
			if (isRequestSaved != null && isRequestSaved) {
				if (actionWhenError != null) {
					actionWhenError.run(from, (long)Procedure.Excption);
				}
			}
			logger.error(procdure.getActionName(), ex);
			return Procedure.Excption;
		}
	}


	public static Task Run(Procedure procdure, Zeze.Net.Protocol from) {
		return Run(procdure, from, null);
	}

	public static Task Run(Procedure procdure) {
		return Run(procdure, null, null);
	}

	public static Task Run(Procedure procdure,
			Zeze.Net.Protocol from,
			Action2<Zeze.Net.Protocol, Long> actionWhenError) {
		var task = new Task(() -> Call(procdure, from, actionWhenError));
		threadPoolDefault.execute(task);
		return task;
	}

	public static Task Create(Procedure procdure,
							  Zeze.Net.Protocol from,
							  Action2<Zeze.Net.Protocol, Long> actionWhenError) {
		return new Task(() -> Call(procdure, from, actionWhenError));
	}

	public static Task Run(Task task) {
		threadPoolDefault.execute(task);
		return task;
	}
}