package Zeze.Util;

import java.util.Objects;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 存储过程任务的 OneByOne 提交参数，通过 {@link OneByOneSpec#ofProcedure} 构造。
 * 不提供 name 设置：日志名固定使用 procedure.getActionName()，与旧 API 一致。
 *
 * <pre>
 * OneByOneSpec.ofProcedure(key, zeze.newProcedure(func, actionName))
 *         .mode(DispatchMode.Normal).execute(Task.getOneByOne());
 * </pre>
 */
public final class ProcedureOneByOneSpec extends AbstractOneByOneSpec implements OneByOneSpec {
	private final @NotNull Procedure procedure;

	ProcedureOneByOneSpec(@NotNull Object key, @NotNull Procedure procedure) {
		super(key);
		this.procedure = Objects.requireNonNull(procedure);
	}

	ProcedureOneByOneSpec(int key, @NotNull Procedure procedure) {
		super(key);
		this.procedure = Objects.requireNonNull(procedure);
	}

	ProcedureOneByOneSpec(long key, @NotNull Procedure procedure) {
		super(key);
		this.procedure = Objects.requireNonNull(procedure);
	}

	/**
	 * @param cancel 队列 shutdown(true) 时对未执行任务的回调，可为空；TaskOneByOneByKey2 不支持
	 */
	public @NotNull ProcedureOneByOneSpec cancel(@Nullable Action0 cancel) {
		this.cancel = cancel;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal
	 */
	public @NotNull ProcedureOneByOneSpec mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * 提交到 {@link TaskOneByOneBase}（TaskOneByOneByKey / TaskOneByOneByKeyLru 等）。
	 * 等价 {@link TaskOneByOneBase#Execute(Object, Procedure, Action0, DispatchMode)}
	 * 及其 int/long key 版本的最长重载。
	 */
	public void execute(@NotNull TaskOneByOneBase oneByOne) {
		executeByKey(oneByOne, new TaskOneByOneQueue.TaskFunc(procedure::call, procedure.getActionName(), cancel,
				modeOrDefault()));
	}

	/**
	 * 提交到 {@link TaskOneByOneByKey2}。
	 * 等价 {@link TaskOneByOneByKey2#Execute(int, Procedure, DispatchMode)}
	 * （long key 转 Long.hashCode、Object key 转 hashCode 后委托 int 版，与旧重载一致）。
	 *
	 * @throws IllegalArgumentException 已设置 {@link #cancel} 时（Key2 不支持 shutdown/cancel）
	 */
	public void execute(@NotNull TaskOneByOneByKey2 oneByOne) {
		if (cancel != null)
			throw new IllegalArgumentException("cancel is not supported by TaskOneByOneByKey2");
		oneByOne.executeProcedureCore(hashKey(), procedure, modeOrDefault());
	}
}
