package Zeze.Util;

import java.util.Objects;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 带返回值任务的 OneByOne 提交参数，通过 {@link OneByOneSpec#ofFunc0} 构造。
 * 仅支持提交到 {@link TaskOneByOneByKey2}（其旧 API 使用 Func0，base 家族使用 FuncLong）；
 * 不提供 cancel 设置：Key2 不支持 shutdown/cancel。
 *
 * <pre>
 * OneByOneSpec.ofFunc0(1, () -> doSomething()).name("MyTask")
 *         .mode(DispatchMode.Critical).execute(oneByOne2);
 * </pre>
 */
public final class Func0OneByOneSpec<R> extends AbstractOneByOneSpec implements OneByOneSpec {
	private final @NotNull Func0<R> func;

	Func0OneByOneSpec(@NotNull Object key, @NotNull Func0<R> func) {
		super(key);
		this.func = Objects.requireNonNull(func);
	}

	Func0OneByOneSpec(int key, @NotNull Func0<R> func) {
		super(key);
		this.func = Objects.requireNonNull(func);
	}

	Func0OneByOneSpec(long key, @NotNull Func0<R> func) {
		super(key);
		this.func = Objects.requireNonNull(func);
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用 func 的类名
	 */
	public @NotNull Func0OneByOneSpec<R> name(@Nullable String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal
	 */
	public @NotNull Func0OneByOneSpec<R> mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}
}
