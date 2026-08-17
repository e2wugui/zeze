package Zeze.Util;

import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;

/**
 * 任务提交参数的统一描述，用来替代 {@link Task} 的大量重载。
 * 通过静态工厂按任务类型构造：ofAction=普通 Action0, ofFunc=返回 long 的 FuncLong,
 * ofProcedure=存储过程 Procedure, ofFunc0=带返回值调度的 Func0。
 *
 * <pre>
 * TaskSpec.ofAction(() -> doSomething()).name("MyTask").mode(DispatchMode.Critical).executeUnsafe();
 * TaskSpec.ofFunc(() -> p.handle(service, factoryHandle)).protocol(p).errorHandle(Protocol::trySendResultCode).call();
 * </pre>
 *
 * spec 实例是一次性的、非线程安全：设置好参数后调用终结方法，不要在多个线程间共享或复用同一个实例
 * （与 TimerSpec 的 build 约定一致）。
 */
public sealed interface TaskSpec permits ActionTaskSpec, FuncLongTaskSpec, ProcedureTaskSpec, Func0TaskSpec {
	/**
	 * 普通无返回值任务。
	 *
	 * @param action 不能为空
	 */
	static @NotNull ActionTaskSpec ofAction(@NotNull Action0 action) {
		return new ActionTaskSpec(action);
	}

	/**
	 * 返回 long 结果的任务（如返回错误码的处理器）。
	 *
	 * @param func 不能为空
	 */
	static @NotNull FuncLongTaskSpec ofFunc(@NotNull FuncLong func) {
		return new FuncLongTaskSpec(func);
	}

	/**
	 * 存储过程任务。日志名固定使用 procedure.getActionName()。
	 *
	 * @param procedure 不能为空
	 */
	static @NotNull ProcedureTaskSpec ofProcedure(@NotNull Procedure procedure) {
		return new ProcedureTaskSpec(procedure);
	}

	/**
	 * 带返回值的延迟任务，返回值与异常都经 Future 传播。
	 *
	 * @param func 不能为空
	 */
	static <R> @NotNull Func0TaskSpec<R> ofFunc0(@NotNull Func0<R> func) {
		return new Func0TaskSpec<>(func);
	}
}
