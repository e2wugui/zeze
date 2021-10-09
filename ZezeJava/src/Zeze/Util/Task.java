package Zeze.Util;

import Zeze.Transaction.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Task implements Runnable {
	private static final Logger logger = LogManager.getLogger(Task.class);

	public static void Call(tangible.Action0Param action, String actionName) {
		try {
			action.invoke();
		}
		catch (Throwable ex) {
			logger.error(actionName, ex);
		}
	}

	private static Object lock = new Object();
	private static java.util.concurrent.ScheduledThreadPoolExecutor threadPool;

	public static java.util.concurrent.ScheduledThreadPoolExecutor getThreadPool() {
		return threadPool;
	}

	public static void initThreadPool(java.util.concurrent.ScheduledThreadPoolExecutor pool) {
		synchronized (lock) {
			if (null != threadPool)
				throw new RuntimeException("ThreadPool Has Inited.");
			threadPool = pool;
		}
	}

	public static boolean tryInitThreadPool(Zeze.Application app,
			java.util.concurrent.ScheduledThreadPoolExecutor pool) {
		synchronized (lock) {
			if (null != threadPool)
				return false;
			if (null == pool) {
				int workerThreads = app.getConfig().getWorkerThreads() > 0
						? app.getConfig().getWorkerThreads()
						: Runtime.getRuntime().availableProcessors() * 30;
				threadPool = new java.util.concurrent.ScheduledThreadPoolExecutor(workerThreads);
			}
			else {
				threadPool = pool;
			}
			return true;
		}
	}

	private tangible.Action0Param action;

	public Task(tangible.Action0Param action) {
		this.action = action;
	}

	@Override
	public void run() {
		action.invoke();
	}

	public static Task Run(tangible.Action0Param action, String actionName) {
		var task = new Task(() -> Call(action, actionName));
		threadPool.execute(task);
		return task;
	}
	
	public static void LogAndStatistics(int result, Zeze.Net.Protocol p, boolean IsRequestSaved) {
		final var actionName = IsRequestSaved ? p.getClass().getName() : p.getClass().getName() + ":Response";
		if (result != 0) {
			var logLevel = (null != p.getService().getZeze())
					? p.getService().getZeze().getConfig().getProcessReturnErrorLogLevel()
					: Level.INFO;
			final var module = result > 0
					? "@" + Zeze.Net.Protocol.GetModuleId(result) + ":" + Zeze.Net.Protocol.GetProtocolId(result)
					: "";
			logger.log(logLevel, () -> "Task " + actionName + " Return=" + result + module + " UserState=" + p.getUserState());
		}
		ProcedureStatistics.getInstance().GetOrAdd(actionName).GetOrAdd(result).incrementAndGet();
	}


	public static int Call(tangible.Func0Param<Integer> func, Zeze.Net.Protocol p) {
		return Call(func, p, null);
	}

	public static int Call(tangible.Func0Param<Integer> func, Zeze.Net.Protocol p,
			tangible.Action2Param<Zeze.Net.Protocol, Integer> actionWhenError) {
		boolean IsRequestSaved = p.isRequest(); // 记住这个，以后可能会被改变。
		try {
			int result = func.invoke();
			if (result != 0 && IsRequestSaved) {
				if (actionWhenError != null) {
					actionWhenError.invoke(p, result);
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

			var errorCode = ex instanceof TaskCanceledException ? Procedure.CancelExcption : Procedure.Excption;

			if (IsRequestSaved) {
				if (actionWhenError != null) {
					actionWhenError.invoke(p, errorCode);
				}
			}

			LogAndStatistics(errorCode, p, IsRequestSaved);
			// 使用 InnerException
			logger.error(() -> "Task " + p.getClass().getName() + " Exception UserState=" + p.getUserState(), ex);
			return errorCode;
		}
	}


	public static Task Run(tangible.Func0Param<Integer> func, Zeze.Net.Protocol p) {
		return Run(func, p, null);
	}

	public static Task Run(tangible.Func0Param<Integer> func, Zeze.Net.Protocol p,
			tangible.Action2Param<Zeze.Net.Protocol, Integer> actionWhenError) {
		var task = new Task(() -> Call(func, p, actionWhenError));
		threadPool.execute(task);
		return task;
	}


	public static int Call(Procedure procdure, Zeze.Net.Protocol from) {
		return Call(procdure, from, null);
	}

	public static int Call(Procedure procdure) {
		return Call(procdure, null, null);
	}

	public static int Call(Procedure procdure, Zeze.Net.Protocol from,
			tangible.Action2Param<Zeze.Net.Protocol, Integer> actionWhenError) {
		Boolean isRequestSaved = from == null ? null : from.isRequest();
		try {
			// 日志在Call里面记录。因为要支持嵌套。
			// 统计在Call里面实现。
			int result = procdure.Call();
			if (result != 0 && !isRequestSaved.equals(null) && isRequestSaved.booleanValue()) {
				if (actionWhenError != null) {
					actionWhenError.invoke(from, result);
				}
			}
			return result;
		}
		catch (RuntimeException ex) {
			// Procedure.Call处理了所有错误。应该不会到这里。除非内部错误。
			if (!isRequestSaved.equals(null) && isRequestSaved.booleanValue()) {
				if (actionWhenError != null) {
					actionWhenError.invoke(from, Procedure.Excption);
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
			tangible.Action2Param<Zeze.Net.Protocol, Integer> actionWhenError) {
		var task = new Task(() -> Call(procdure, from, actionWhenError));
		threadPool.execute(task);
		return task;
	}
}