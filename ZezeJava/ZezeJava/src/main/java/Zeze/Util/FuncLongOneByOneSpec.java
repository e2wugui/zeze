package Zeze.Util;

import java.util.Objects;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 返回 long 结果任务（如返回错误码的处理器）的 OneByOne 提交参数，通过 {@link OneByOneSpec#ofFunc} 构造。
 *
 * <pre>
 * OneByOneSpec.ofFunc(account, () -> p.handle(this, factoryHandle))
 *         .mode(factoryHandle.Mode).execute(oneByOne);
 * </pre>
 */
public final class FuncLongOneByOneSpec extends AbstractOneByOneSpec implements OneByOneSpec {
	private final @NotNull FuncLong func;

	FuncLongOneByOneSpec(@NotNull Object key, @NotNull FuncLong func) {
		super(key);
		this.func = Objects.requireNonNull(func);
	}

	FuncLongOneByOneSpec(int key, @NotNull FuncLong func) {
		super(key);
		this.func = Objects.requireNonNull(func);
	}

	FuncLongOneByOneSpec(long key, @NotNull FuncLong func) {
		super(key);
		this.func = Objects.requireNonNull(func);
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用 func 的类名
	 */
	public @NotNull FuncLongOneByOneSpec name(@Nullable String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param cancel 队列 shutdown(true) 时对未执行任务的回调，可为空
	 */
	public @NotNull FuncLongOneByOneSpec cancel(@Nullable Action0 cancel) {
		this.cancel = cancel;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal
	 */
	public @NotNull FuncLongOneByOneSpec mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * 提交到 {@link TaskOneByOneBase}（TaskOneByOneByKey / TaskOneByOneByKeyLru 等）。
	 * 等价 {@link TaskOneByOneBase#Execute(Object, FuncLong, String, Action0, DispatchMode)}
	 * 及其 int/long key 版本的最长重载。
	 * <p>
	 * 不提供 {@link TaskOneByOneByKey2} 版本：Key2 旧 API 使用 Func0 而非 FuncLong，
	 * func::call 适配会引入装箱语义混淆。
	 */
	public void execute(@NotNull TaskOneByOneBase oneByOne) {
		executeByKey(oneByOne, new TaskOneByOneQueue.TaskFunc(func, name, cancel, modeOrDefault()));
	}
}
