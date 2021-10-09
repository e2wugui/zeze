package Zeze.Transaction;

import Zeze.*;

public class Procedure {
	public static final int Success = 0;
	public static final int Excption = -1;
	public static final int TooManyTry = -2;
	public static final int NotImplement = -3;
	public static final int Unknown = -4;
	public static final int ErrorSavepoint = -5;
	public static final int LogicError = -6;
	public static final int RedoAndRelease = -7;
	public static final int AbortException = -8;
	public static final int ProviderNotExist = -9;
	public static final int Timeout = -10;
	public static final int CancelExcption = -11;
	public static final int DuplicateRequest = -12;
	public static final int ErrorRequestId = -13;
	// >0 用户自定义。

	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}

	private tangible.Func0Param<Integer> Action;
	public final tangible.Func0Param<Integer> getAction() {
		return Action;
	}
	public final void setAction(tangible.Func0Param<Integer> value) {
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

	public Procedure(Application app, tangible.Func0Param<Integer> action, String actionName, Object userState) {
		Zeze = app;
		setAction(::action);
		setActionName(actionName);
		setUserState(userState);
		if (null == getUserState()) { // 没指定，就从当前存储过程继承。嵌套时发生。
			setUserState(Transaction.getCurrent() == null ? null : (Transaction.getCurrent().getTopProcedure() == null ? null : Transaction.getCurrent().getTopProcedure().getUserState()));
		}
	}

	/** 
	 创建 Savepoint 并执行。
	 嵌套 Procedure 实现，
	 
	 @return 
	*/
	public final int Call() {
		if (null == Transaction.getCurrent()) {
			try {
				// 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
				return Transaction.Create().Perform(this);
			}
			finally {
				Transaction.Destroy();
			}
		}

		Transaction currentT = Transaction.getCurrent();
		currentT.Begin();
		currentT.getProcedureStack().add(this);

		try {
			int result = Process();
			if (Success == result) {
				currentT.Commit();
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
				ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(result).IncrementAndGet();
//#endif
				return Success;
			}
			currentT.Rollback();

			var module = "";
			if (result > 0) {
				module = "@" + Net.Protocol.GetModuleId(result) + ":" + Net.Protocol.GetProtocolId(result);
			}
			logger.Log(getZeze().getConfig().getProcessReturnErrorLogLevel(), "Procedure {0} Return{1}@{2} UserState={3}", ToString(), result, module, getUserState());
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(result).IncrementAndGet();
//#endif
			return result;
		}
		catch (AbortException e) {
			currentT.Rollback();
			throw; // 抛出这个异常，中断事务，跳过所有嵌套过程直到最外面。 e; // 抛出这个异常，中断事务，跳过所有嵌套过程直到最外面。
		}
		catch (RedoAndReleaseLockException e2) {
			currentT.Rollback();
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(RedoAndRelease).IncrementAndGet();
//#endif
			throw; // 抛出这个异常，打断事务，跳过所有嵌套过程直到最外面。会重做。 e2; // 抛出这个异常，打断事务，跳过所有嵌套过程直到最外面。会重做。
		}
		catch (TaskCanceledException ce) {
			currentT.Rollback();
			logger.Error(ce, "Procedure {0} Exception UserState={1}", ToString(), getUserState());
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(Excption).IncrementAndGet();
//#endif
			return CancelExcption; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
		}
		catch (RuntimeException e) {
			currentT.Rollback();
			logger.Error(e, "Procedure {0} Exception UserState={1}", ToString(), getUserState());
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
			ProcedureStatistics.getInstance().GetOrAdd(getActionName()).GetOrAdd(Excption).IncrementAndGet();
//#endif
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if DEBUG
			// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
			if (e.getClass().getSimpleName().equals("AssertFailedException")) {
				throw e;
			}
//#endif
			return Excption; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
		}
		finally {
			currentT.getProcedureStack().remove(currentT.getProcedureStack().size() - 1);
		}
	}

	protected int Process() {
		if (null != getAction()) {
			return tangible.Action0Param();
		}
		return NotImplement;
	}

	@Override
	public String toString() {
		// GetType().FullName 仅在用继承的方式实现 Procedure 才有意义。
		return (null != getAction()) ? getActionName() :this.getClass().getName();
	}
}