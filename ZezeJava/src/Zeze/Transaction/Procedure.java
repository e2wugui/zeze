package Zeze.Transaction;

import java.util.concurrent.Callable;

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

	private Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}

	private Callable<Long> Action;
	public final Callable<Long> getAction() {
		return Action;
	}
	public final void setAction(Callable<Long> value) {
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

	public Procedure(Application app, Callable<Long> action, String actionName, Object userState) {
		Zeze = app;
		setAction(action);
		setActionName(actionName);
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
	 
	 @return 
	*/
	public final long Call() {
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
		catch (RedoException e) {
			currentT.Rollback();
			throw e;
		}
		catch (AbortException e) {
			currentT.Rollback();
			throw e;
			// 抛出这个异常，中断事务，跳过所有嵌套过程直到最外面。 e;
		}
		catch (RedoAndReleaseLockException e2) {
			currentT.Rollback();

			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(RedoAndRelease).incrementAndGet();

			throw e2; // 抛出这个异常，打断事务，跳过所有嵌套过程直到最外面。会重做。
		}
		catch (TaskCanceledException e3) {
			currentT.Rollback();
			logger.error("Procedure {} Exception UserState={}", this, getUserState(), e3);

			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(Excption).incrementAndGet();

			return CancelExcption; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
		}
		catch (Throwable e4) {
			currentT.Rollback();
			logger.error("Procedure {} Exception UserState={}", this, getUserState(), e4);

			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(Excption).incrementAndGet();

			// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
			if (e4.getClass().getSimpleName().equals("AssertFailedException")) {
				throw e4;
			}

			return Excption; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
		}
		finally {
			currentT.getProcedureStack().remove(currentT.getProcedureStack().size() - 1);
		}
	}

	protected long Process() {
		if (null != Action) {
			try {
				return Action.call();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		return NotImplement;
	}

	@Override
	public String toString() {
		// GetType().FullName 仅在用继承的方式实现 Procedure 才有意义。
		return (null != getAction()) ? getActionName() : this.getClass().getName();
	}
}