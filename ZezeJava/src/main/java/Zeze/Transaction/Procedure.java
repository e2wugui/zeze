package Zeze.Transaction;

import Zeze.Util.TaskCanceledException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Zeze.*;

public class Procedure {
	public static final long Success = 0;
	public static final long Excption = -1;
	public static final long TooManyTry = -2;
	public static final long NotImplement = -3;
	public static final long Unknown = -4;
	public static final long ErrorSavepoint = -5;
	public static final long LogicError = -6;
	public static final long RedoAndRelease = -7;
	public static final long AbortException = -8;
	public static final long ProviderNotExist = -9;
	public static final long Timeout = -10;
	public static final long CancelExcption = -11;
	public static final long DuplicateRequest = -12;
	public static final long ErrorRequestId = -13;
	// >0 用户自定义。

	private static final Logger logger = LogManager.getLogger(Procedure.class);

	private final Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}

	private Zeze.Util.Func0<Long> Action;
	public final Zeze.Util.Func0<Long> getAction() {
		return Action;
	}
	public final void setAction(Zeze.Util.Func0<Long> value) {
		Action = value;
	}

	private String ActionName;
	public final String getActionName() {
		return ActionName;
	}
	public final void setActionName(String value) {
		ActionName = value;
	}

	// 用于继承方式实现 Procedure。
	public Procedure(Application app) {
		Zeze = app;
	}

	private Object UserState;
	public final Object getUserState() {
		return UserState;
	}
	public final void setUserState(Object value) {
		UserState = value;
	}
	private TransactionLevel Level;
	public final TransactionLevel getTransactionLevel() {
		return Level;
	}

	public Procedure(Application app, Zeze.Util.Func0<Long> action, String actionName,
					 TransactionLevel level, Object userState) {
		Zeze = app;
		setAction(action);
		setActionName(actionName);
		Level = level;
		setUserState(userState);
		if (null == getUserState()) { // 没指定，就从当前存储过程继承。嵌套时发生。
			setUserState(Transaction.getCurrent() == null
					? null : (Transaction.getCurrent().getTopProcedure() == null
					? null : Transaction.getCurrent().getTopProcedure().getUserState()));
		}
	}

	/**
	 创建 Savepoint 并执行。
	 嵌套 Procedure 实现，

	 @return 0 success; other means error.
	*/
	public final long Call() throws Throwable {
		if (null == Transaction.getCurrent()) {
			try {
				// 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
				return Transaction.Create(Zeze.getLocks()).Perform(this);
			}
			finally {
				Transaction.Destroy();
			}
		}

		Transaction currentT = Transaction.getCurrent();
		currentT.Begin();
		currentT.getProcedureStack().add(this);

		try {
			var result = Process();
			currentT.VerifyRunning(); // 防止应用抓住了异常，通过return方式返回。

			if (Success == result) {
				currentT.Commit();
				ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(result).incrementAndGet();
				return Success;
			}
			currentT.Rollback();
			var module = "";
			if (result > 0) {
				module = "@" + IModule.GetModuleId(result) + ":" + IModule.GetErrorCode(result);
			}
			logger.log(getZeze().getConfig().getProcessReturnErrorLogLevel(),
					"Procedure {} Return{}@{} UserState={}", this, result, module, getUserState());
			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(result).incrementAndGet();
			return result;
		}
		catch (GoBackZeze gobackzeze) {
			// 单独抓住这个异常，是为了能原样抛出，并且使用不同的级别记录日志。
			// 对状态正确性没有影响。
			currentT.Rollback();
			logger.debug(gobackzeze);
			throw gobackzeze;
		}
		catch (Throwable e) {
			currentT.Rollback();
			logger.error("Procedure {} Exception UserState={}", this, getUserState(), e);
			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(Excption).incrementAndGet();
			// 验证状态：Running状态将吃掉所有异常。
			currentT.VerifyRunning();
			// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
			if (e instanceof AssertionError) {
				throw e;
			}
			// 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
			return e instanceof TaskCanceledException ? CancelExcption : Excption;
		}
		finally {
			currentT.getProcedureStack().remove(currentT.getProcedureStack().size() - 1);
		}
	}

	protected long Process() throws Throwable{
		if (null != Action) {
			return Action.call();
		}
		return NotImplement;
	}

	@Override
	public String toString() {
		// GetType().FullName 仅在用继承的方式实现 Procedure 才有意义。
		return (null != getAction()) ? getActionName() : this.getClass().getName();
	}
}
