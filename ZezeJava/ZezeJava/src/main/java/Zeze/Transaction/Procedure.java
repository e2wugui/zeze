package Zeze.Transaction;

import Zeze.Application;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Util.FuncLong;
import Zeze.Util.PerfCounter;
import Zeze.Util.TaskCanceledException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Procedure {
	public static final long Success = 0;
	public static final long Exception = -1;
	public static final long TooManyTry = -2;
	public static final long NotImplement = -3;
	public static final long Unknown = -4;
	public static final long ErrorSavepoint = -5;
	public static final long LogicError = -6;
	public static final long RedoAndRelease = -7;
	public static final long AbortException = -8;
	public static final long ProviderNotExist = -9;
	public static final long Timeout = -10;
	public static final long CancelException = -11;
	public static final long DuplicateRequest = -12;
	public static final long ErrorRequestId = -13;
	public static final long ErrorSendFail = -14;
	public static final long RaftRetry = -15;
	public static final long RaftApplied = -16;
	public static final long RaftExpired = -17;
	public static final long Closed = -18;
	public static final long Busy = -19;
	public static final long AuthFail = -20;
	// >0 用户自定义。

	private @Nullable String protocolClassName;
	private @Nullable Binary protocolRawArgument;

	public @Nullable Binary getProtocolRawArgument() {
		return protocolRawArgument;
	}

	public @Nullable String getProtocolClassName() {
		return protocolClassName;
	}

	public void setProtocolClassName(String protocolClassName) {
		this.protocolClassName = protocolClassName;
	}

	public void setProtocolRawArgument(Binary protocolRawArgument) {
		this.protocolRawArgument = protocolRawArgument;
	}

	public interface ILogAction {
		void run(@Nullable Throwable ex, long result, @NotNull Procedure p, @NotNull String message);
	}

	private static final Logger logger = LogManager.getLogger(Procedure.class);
	public static final boolean ENABLE_DEBUG_LOG = logger.isDebugEnabled();
	public static final boolean ENABLE_TRACE_LOG = logger.isTraceEnabled();
	@SuppressWarnings("CanBeFinal")
	public static ILogAction logAction = Procedure::defaultLogAction;

	public static void defaultLogAction(@Nullable Throwable ex, long result, @NotNull Procedure p,
										@NotNull String message) {
		Level level;
		if (ex != null)
			level = Level.ERROR;
		else if (result != 0)
			level = p.zeze.getConfig().getProcessReturnErrorLogLevel();
		else if (ENABLE_TRACE_LOG)
			level = Level.TRACE;
		else
			return;

		String module = result > 0 ? "@" + IModule.getModuleId(result) + ":" + IModule.getErrorCode(result) : "";
		logger.log(level, "Procedure={} Return={}{}:{}", p, result, module, message, ex);
	}

	private final @NotNull Application zeze;
	private final @Nullable TransactionLevel level;
	private @Nullable FuncLong action;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private @NotNull String actionName;
	private @Nullable Object userState;
	// public Runnable runWhileCommit;

	// 用于继承方式实现 Procedure。
	public Procedure(@NotNull Application app) {
		zeze = app;
		level = null;
		actionName = getClass().getName();
	}

	public Procedure(@NotNull Application app, @Nullable FuncLong action, @Nullable String actionName,
					 @Nullable TransactionLevel level, @Nullable Object userState) {
		zeze = app;
		this.level = level;
		this.action = action;
		setActionName(actionName);
		this.userState = userState;
	}

	public final @NotNull Application getZeze() {
		return zeze;
	}

	public final @Nullable TransactionLevel getTransactionLevel() {
		return level;
	}

	public final @Nullable FuncLong getAction() {
		return action;
	}

	public final void setAction(@Nullable FuncLong action) {
		this.action = action;
	}

	public final @NotNull String getActionName() {
		return actionName;
	}

	public final void setActionName(@Nullable String actionName) {
		this.actionName = actionName != null ? actionName : (action != null ? action : this).getClass().getName();
	}

	/**
	 * 创建 Savepoint 并执行。
	 * 嵌套 Procedure 实现，
	 *
	 * @return 0 success; other means error.
	 */
	public final long call() {
		long result = Exception;
		Transaction currentT = Transaction.getCurrent();
		if (currentT == null) {
			long timeBegin = System.nanoTime();
			try {
				currentT = Transaction.create(zeze.getLocks(), userState);
				currentT.profiler.onProcedureBegin(actionName, timeBegin);
				// 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
				return result = currentT.perform(this);
			} finally {
				var curTime = System.nanoTime();
				var runTime = curTime - timeBegin;
				if (currentT != null) {
					currentT.profiler.onProcedureEnd(actionName, curTime, runTime);
					currentT.reuseTransaction();
				}
				if (PerfCounter.ENABLE_PERF) {
					PerfCounter.instance.addProcedureInfo(actionName, result);
					PerfCounter.instance.addRunInfo(actionName, runTime);
				}
			}
		}

		currentT.begin();
		currentT.getProcedureStack().add(this);
		try {
//			var r = runWhileCommit;
//			if (r != null) {
//				runWhileCommit = null;
//				currentT.runWhileCommit(r);
//			}
			result = process();
			currentT.verifyRunning(); // 防止应用抓住了异常，通过return方式返回。

			if (result == Success) {
				currentT.commit();
				return Success;
			}
			currentT.rollback();
			var tmpLogAction = logAction;
			if (tmpLogAction != null)
				tmpLogAction.run(null, result, this, "");
			return result;
		} catch (GoBackZeze goBackZeze) {
			// 单独抓住这个异常，是为了能原样抛出，并且使用不同的级别记录日志。
			// 对状态正确性没有影响。
			currentT.rollback();
			logger.info("GoBackZeze({}): {}", goBackZeze.getMessage(), this);
			throw goBackZeze;
		} catch (Throwable e) { // logger, rethrow AssertionError
			// rollback.
			currentT.rollback();
			currentT.logActions.add(() -> {
				var tmpLogAction = logAction;
				if (tmpLogAction != null)
					tmpLogAction.run(e, Exception, this, "");
			});
			// 验证状态：Running状态将吃掉所有异常。
			currentT.verifyRunning();
			// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
			if (e instanceof AssertionError)
				throw (AssertionError)e;
			// 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
			return e instanceof TaskCanceledException ? CancelException : Exception;
		} finally {
			currentT.getProcedureStack().remove(currentT.getProcedureStack().size() - 1);
		}
	}

	protected long process() throws Exception {
		return action != null ? action.call() : NotImplement;
	}

	@Override
	public @NotNull String toString() {
		return actionName;
	}
}
