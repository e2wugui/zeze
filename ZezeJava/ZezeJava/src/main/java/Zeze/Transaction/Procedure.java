package Zeze.Transaction;

import Zeze.Application;
import Zeze.IModule;
import Zeze.Util.FuncLong;
import Zeze.Util.Macro;
import Zeze.Util.TaskCanceledException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	// >0 用户自定义。

	public interface ILogAction {
		void run(Throwable ex, long result, Procedure p, String message);
	}

	private static final Logger logger = LogManager.getLogger(Procedure.class);
	@SuppressWarnings("CanBeFinal")
	public static ILogAction logAction = Procedure::defaultLogAction;

	public static void defaultLogAction(Throwable ex, long result, Procedure p, String message) {
		Level level;
		if (ex != null)
			level = Level.ERROR;
		else if (result != 0)
			level = p.zeze.getConfig().getProcessReturnErrorLogLevel();
		else if (logger.isTraceEnabled())
			level = Level.TRACE;
		else
			return;

		String module = result > 0 ? "@" + IModule.getModuleId(result) + ":" + IModule.getErrorCode(result) : "";
		logger.log(level, "Procedure={} Return={}{}{} UserState={}", p, result, module, message, p.userState, ex);
	}

	private final Application zeze;
	private final TransactionLevel level;
	private FuncLong action;
	private String actionName;
	private Object userState;
	// public Runnable runWhileCommit;

	// 用于继承方式实现 Procedure。
	public Procedure(Application app) {
		zeze = app;
		level = null;
		actionName = getClass().getName();
	}

	public Procedure(Application app, FuncLong action, String actionName, TransactionLevel level, Object userState) {
		zeze = app;
		this.level = level;
		this.action = action;
		setActionName(actionName);
		if (userState != null)
			this.userState = userState;
		else { // 没指定，就从当前存储过程继承。嵌套时发生。
			Transaction currentT = Transaction.getCurrent();
			if (currentT != null) {
				Procedure proc = currentT.getTopProcedure();
				if (proc != null)
					this.userState = proc.userState;
			}
		}
	}

	public final Application getZeze() {
		return zeze;
	}

	public final TransactionLevel getTransactionLevel() {
		return level;
	}

	public final FuncLong getAction() {
		return action;
	}

	public final void setAction(FuncLong action) {
		this.action = action;
	}

	public final String getActionName() {
		return actionName;
	}

	public final void setActionName(String actionName) {
		this.actionName = actionName != null ? actionName : (action != null ? action : this).getClass().getName();
	}

	public final Object getUserState() {
		return userState;
	}

	public final void setUserState(Object value) {
		userState = value;
	}

	/**
	 * 创建 Savepoint 并执行。
	 * 嵌套 Procedure 实现，
	 *
	 * @return 0 success; other means error.
	 */
	public final long call() {
		Transaction currentT = Transaction.getCurrent();
		if (currentT == null) {
			try {
				// 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
				return Transaction.create(zeze.getLocks()).perform(this);
			} finally {
				Transaction.destroy();
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
			long result = process();
			currentT.verifyRunning(); // 防止应用抓住了异常，通过return方式返回。

			if (result == Success) {
				currentT.commit();
				if (Macro.enableStatistics) {
					ProcedureStatistics.getInstance().getOrAdd(actionName).getOrAdd(result).increment();
				}
				return Success;
			}
			currentT.rollback();
			var tmpLogAction = logAction;
			if (tmpLogAction != null)
				tmpLogAction.run(null, result, this, "");
			if (Macro.enableStatistics) {
				ProcedureStatistics.getInstance().getOrAdd(actionName).getOrAdd(result).increment();
			}
			return result;
		} catch (GoBackZeze gobackzeze) {
			// 单独抓住这个异常，是为了能原样抛出，并且使用不同的级别记录日志。
			// 对状态正确性没有影响。
			currentT.rollback();
			logger.debug("", gobackzeze);
			throw gobackzeze;
		} catch (Throwable e) { // logger, rethrow AssertionError
			// rollback.
			currentT.rollback();
			currentT.logActions.add(() -> {
				var tmpLogAction = logAction;
				if (tmpLogAction != null)
					tmpLogAction.run(e, Exception, this, "");
			});
			if (Macro.enableStatistics) {
				ProcedureStatistics.getInstance().getOrAdd(actionName).getOrAdd(Exception).increment();
			}
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
	public String toString() {
		return actionName;
	}
}
