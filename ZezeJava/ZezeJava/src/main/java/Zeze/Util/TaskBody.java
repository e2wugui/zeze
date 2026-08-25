package Zeze.Util;

import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 任务载荷的统一抽象，{@link TaskSpec} 四种载荷（Action0/FuncLong/Procedure/Func0）的密封实现。
 * 载荷类型之间的差异只有三处，全部收敛到本接口：
 * <ul>
 * <li>异常/结果策略：见 {@link #call(String)}；</li>
 * <li>日志名解析：OfProcedure 固定 getActionName()，其余 name 优先、空则载荷类名，见 {@link #logName(String)}；</li>
 * <li>ZezeCounter 统计位置：OfFunc/OfProcedure 在 call 内部完成（statsKey 返回 null，外层不再计数），
 *     OfAction/OfFunc0 由外层 core 按 {@link #statsKey(String)} 计数。</li>
 * </ul>
 * Task 的各执行家族（call/submit/execute/schedule/scheduleAt）与 OneByOne 队列因此用一份代码处理所有载荷。
 *
 * @param <R> 结果类型：OfAction/OfFunc/OfProcedure 归一为 Long；OfFunc0 携带真实返回值类型
 */
public sealed interface TaskBody<R> {
	/**
	 * 带策略调用，供 Task 的 call/submit/execute/schedule 家族使用：
	 * OfAction 吞异常记日志返回 0；OfFunc/OfProcedure 翻错误码并走 logAndStatistics；
	 * OfFunc0 返回值与异常原样传播（sneaky throw）。本方法不做 ZezeCounter 计数（由外层 core 负责）。
	 */
	R call(@Nullable String name);

	/**
	 * 原始调用：不带日志/统计/异常翻译，供 OneByOne 队列等自行包装的场合使用。
	 */
	R callRaw() throws Exception;

	/**
	 * submit 的 Direct 语义（结果/异常都进 Future）：默认等同 {@link #callRaw()}（OfAction/OfFunc0 的异常进 Future）；
	 * OfFunc/OfProcedure 覆写为 {@link #call(String)}（异常翻错误码进结果，Future 不携带异常）。
	 */
	default R callForFuture(@Nullable String name) throws Exception {
		return callRaw();
	}

	/**
	 * 异常日志与任务名解析：OfProcedure 固定 getActionName()（name 参数无效）；
	 * 其余 name 优先，空则载荷类名。
	 */
	@NotNull String logName(@Nullable String name);

	/**
	 * ZezeCounter 计数 key（name 优先，空则载荷 Class）；
	 * null 表示统计已在 call 内部完成（OfFunc/OfProcedure），外层 core 不再计数。
	 */
	@Nullable Object statsKey(@Nullable String name);

	/**
	 * Action0 载荷：吞异常记日志，结果归一为 Long(0)。
	 */
	record OfAction(@NotNull Action0 action) implements TaskBody<Long> {
		@Override
		public Long call(@Nullable String name) {
			try {
				action.run();
			} catch (Throwable e) { // logger.error
				Task.logger.error("{} exception:", logName(name), e);
			}
			return 0L;
		}

		@Override
		public Long callRaw() throws Exception {
			action.run();
			return 0L;
		}

		@Override
		public @NotNull String logName(@Nullable String name) {
			return name != null ? name : action.getClass().getName();
		}

		@Override
		public @NotNull Object statsKey(@Nullable String name) {
			return name != null ? name : action.getClass();
		}
	}

	/**
	 * FuncLong 载荷：异常翻错误码并走 logAndStatistics（日志与统计在 callFuncCore 内完成）。
	 */
	record OfFunc(@NotNull FuncLong func) implements TaskBody<Long> {
		@Override
		public Long call(@Nullable String name) {
			try {
				return Task.callFuncCore(func, null, null, name);
			} catch (Throwable e) { // logger.error callFuncCore 自身异常的兜底（正常路径已翻错误码，不会到这里）
				Task.logger.error("{} exception:", logName(name), e);
				return Procedure.Exception;
			}
		}

		@Override
		public Long callRaw() throws Exception {
			return func.call();
		}

		@Override
		public Long callForFuture(@Nullable String name) {
			return call(name); // 翻错误码进结果，Future 不携带异常
		}

		@Override
		public @NotNull String logName(@Nullable String name) {
			return name != null ? name : func.getClass().getName();
		}

		@Override
		public @Nullable Object statsKey(@Nullable String name) {
			return null; // 统计在 callFuncCore 内完成，外层不再计数
		}
	}

	/**
	 * Procedure 载荷：同 OfFunc，日志名固定使用 getActionName()（name 参数无效）。
	 */
	record OfProcedure(@NotNull Procedure procedure) implements TaskBody<Long> {
		@Override
		public Long call(@Nullable String name) {
			try {
				return Task.callProcCore(procedure, null, null);
			} catch (Throwable e) { // logger.error callProcCore 自身异常的兜底（正常路径已翻错误码，不会到这里）
				Task.logger.error("{} exception:", procedure, e);
				return Procedure.Exception;
			}
		}

		@Override
		public Long callRaw() {
			return procedure.call();
		}

		@Override
		public Long callForFuture(@Nullable String name) {
			return call(name); // 翻错误码进结果，Future 不携带异常
		}

		@Override
		public @NotNull String logName(@Nullable String name) {
			return procedure.getActionName();
		}

		@Override
		public @Nullable Object statsKey(@Nullable String name) {
			return null; // 统计在 procedure.call 内完成，外层不再计数
		}
	}

	/**
	 * Func0 载荷：返回值与异常都原样传播（异常 sneaky throw，由外层 core 记日志并决定是否经 Future 传播）。
	 */
	record OfFunc0<R>(@NotNull Func0<R> func) implements TaskBody<R> {
		@Override
		public R call(@Nullable String name) {
			try {
				return func.call();
			} catch (Throwable e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public R callRaw() throws Exception {
			return func.call();
		}

		@Override
		public @NotNull String logName(@Nullable String name) {
			return name != null ? name : func.getClass().getName();
		}

		@Override
		public @NotNull Object statsKey(@Nullable String name) {
			return name != null ? name : func.getClass();
		}
	}
}
