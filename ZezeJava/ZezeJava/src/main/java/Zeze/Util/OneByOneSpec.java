package Zeze.Util;

import org.jetbrains.annotations.NotNull;

/**
 * OneByOne（相同key串行）任务提交参数的统一描述，用来替代 {@link TaskOneByOneBase} 与
 * {@link TaskOneByOneByKey2} 的大量 Execute 重载。
 * 通过静态工厂按任务类型构造：ofAction=普通 Action0, ofFunc=返回 long 的 FuncLong,
 * ofProcedure=存储过程 Procedure, ofFunc0=带返回值的 Func0（仅 {@link TaskOneByOneByKey2} 支持）。
 * 工厂按 key 类型重载（Object/int/long），int/long 版本不装箱，重载绑定与旧 Execute 完全一致：
 * {@code ofAction(1, a)} 绑定 int 版；显式传 {@code Integer} 走 Object 版（hashCode）。
 *
 * <pre>
 * OneByOneSpec.ofAction("Account#1", () -> doSomething()).name("MyTask").execute(oneByOne);
 * OneByOneSpec.ofFunc(1, () -> p.handle(this, factoryHandle)).mode(factoryHandle.Mode).execute(oneByOne);
 * OneByOneSpec.ofProcedure(key, zeze.newProcedure(func, actionName))
 *         .mode(DispatchMode.Normal).execute(Task.getOneByOne());
 * </pre>
 *
 * 与旧 API 的两处无害微差：
 * 1. spec 使 mode 永不残留 null（未设置时终结方法使用 DispatchMode.Normal，与旧最短重载显式传
 *    Normal 一致），{@link TaskOneByOneQueue.BatchTask#prepare} 的 {@code mode != task.mode}
 *    批分组更连贯，线程池选择等价（仅 Critical 分支有区别）；
 * 2. 工厂对参数 requireNonNull，使 null 校验比旧 Object 版重载（延迟到 TaskAction/TaskFunc
 *    构造时才 NPE）提前。
 *
 * spec 实例是一次性的、非线程安全：设置好参数后调用终结方法，不要在多个线程间共享或复用同一个实例
 * （与 TaskSpec 的约定一致）。
 */
public sealed interface OneByOneSpec permits ActionOneByOneSpec, FuncLongOneByOneSpec, ProcedureOneByOneSpec,
		Func0OneByOneSpec {
	/**
	 * 普通无返回值任务。
	 *
	 * @param key    相同 key 的任务串行执行
	 * @param action 不能为空
	 */
	static @NotNull ActionOneByOneSpec ofAction(@NotNull Object key, @NotNull Action0 action) {
		return new ActionOneByOneSpec(key, action);
	}

	/**
	 * 普通无返回值任务，int key 不装箱。
	 *
	 * @param key    相同 key 的任务串行执行
	 * @param action 不能为空
	 */
	static @NotNull ActionOneByOneSpec ofAction(int key, @NotNull Action0 action) {
		return new ActionOneByOneSpec(key, action);
	}

	/**
	 * 普通无返回值任务，long key 不装箱。
	 *
	 * @param key    相同 key 的任务串行执行
	 * @param action 不能为空
	 */
	static @NotNull ActionOneByOneSpec ofAction(long key, @NotNull Action0 action) {
		return new ActionOneByOneSpec(key, action);
	}

	/**
	 * 返回 long 结果的任务（如返回错误码的处理器）。
	 *
	 * @param key  相同 key 的任务串行执行
	 * @param func 不能为空
	 */
	static @NotNull FuncLongOneByOneSpec ofFunc(@NotNull Object key, @NotNull FuncLong func) {
		return new FuncLongOneByOneSpec(key, func);
	}

	/**
	 * 返回 long 结果的任务（如返回错误码的处理器），int key 不装箱。
	 *
	 * @param key  相同 key 的任务串行执行
	 * @param func 不能为空
	 */
	static @NotNull FuncLongOneByOneSpec ofFunc(int key, @NotNull FuncLong func) {
		return new FuncLongOneByOneSpec(key, func);
	}

	/**
	 * 返回 long 结果的任务（如返回错误码的处理器），long key 不装箱。
	 *
	 * @param key  相同 key 的任务串行执行
	 * @param func 不能为空
	 */
	static @NotNull FuncLongOneByOneSpec ofFunc(long key, @NotNull FuncLong func) {
		return new FuncLongOneByOneSpec(key, func);
	}

	/**
	 * 存储过程任务。日志名固定使用 procedure.getActionName()。
	 *
	 * @param key       相同 key 的任务串行执行
	 * @param procedure 不能为空
	 */
	static @NotNull ProcedureOneByOneSpec ofProcedure(@NotNull Object key, @NotNull Zeze.Transaction.Procedure procedure) {
		return new ProcedureOneByOneSpec(key, procedure);
	}

	/**
	 * 存储过程任务，int key 不装箱。日志名固定使用 procedure.getActionName()。
	 *
	 * @param key       相同 key 的任务串行执行
	 * @param procedure 不能为空
	 */
	static @NotNull ProcedureOneByOneSpec ofProcedure(int key, @NotNull Zeze.Transaction.Procedure procedure) {
		return new ProcedureOneByOneSpec(key, procedure);
	}

	/**
	 * 存储过程任务，long key 不装箱。日志名固定使用 procedure.getActionName()。
	 *
	 * @param key       相同 key 的任务串行执行
	 * @param procedure 不能为空
	 */
	static @NotNull ProcedureOneByOneSpec ofProcedure(long key, @NotNull Zeze.Transaction.Procedure procedure) {
		return new ProcedureOneByOneSpec(key, procedure);
	}

	/**
	 * 带返回值的任务，仅支持提交到 {@link TaskOneByOneByKey2}。
	 *
	 * @param key  相同 key 的任务串行执行
	 * @param func 不能为空
	 */
	static <R> @NotNull Func0OneByOneSpec<R> ofFunc0(@NotNull Object key, @NotNull Func0<R> func) {
		return new Func0OneByOneSpec<>(key, func);
	}

	/**
	 * 带返回值的任务，仅支持提交到 {@link TaskOneByOneByKey2}，int key 不装箱。
	 *
	 * @param key  相同 key 的任务串行执行
	 * @param func 不能为空
	 */
	static <R> @NotNull Func0OneByOneSpec<R> ofFunc0(int key, @NotNull Func0<R> func) {
		return new Func0OneByOneSpec<>(key, func);
	}

	/**
	 * 带返回值的任务，仅支持提交到 {@link TaskOneByOneByKey2}，long key 不装箱。
	 *
	 * @param key  相同 key 的任务串行执行
	 * @param func 不能为空
	 */
	static <R> @NotNull Func0OneByOneSpec<R> ofFunc0(long key, @NotNull Func0<R> func) {
		return new Func0OneByOneSpec<>(key, func);
	}
}
