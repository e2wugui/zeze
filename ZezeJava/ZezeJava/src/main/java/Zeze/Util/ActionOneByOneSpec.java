package Zeze.Util;

import java.util.Objects;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 普通 Action0 任务的 OneByOne 提交参数，通过 {@link OneByOneSpec#ofAction} 构造。
 *
 * <pre>
 * OneByOneSpec.ofAction("Account#1", this::logout).name("Logout")
 *         .mode(DispatchMode.Critical).execute(oneByOne);
 * </pre>
 */
public final class ActionOneByOneSpec extends AbstractOneByOneSpec implements OneByOneSpec {
	private final @NotNull Action0 action;

	ActionOneByOneSpec(@NotNull Object key, @NotNull Action0 action) {
		super(key);
		this.action = Objects.requireNonNull(action);
	}

	ActionOneByOneSpec(int key, @NotNull Action0 action) {
		super(key);
		this.action = Objects.requireNonNull(action);
	}

	ActionOneByOneSpec(long key, @NotNull Action0 action) {
		super(key);
		this.action = Objects.requireNonNull(action);
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用 action 的类名
	 */
	public @NotNull ActionOneByOneSpec name(@Nullable String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param cancel 队列 shutdown(true) 时对未执行任务的回调，可为空；TaskOneByOneByKey2 不支持
	 */
	public @NotNull ActionOneByOneSpec cancel(@Nullable Action0 cancel) {
		this.cancel = cancel;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal
	 */
	public @NotNull ActionOneByOneSpec mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * 提交到 {@link TaskOneByOneBase}（TaskOneByOneByKey / TaskOneByOneByKeyLru 等）。
	 * 等价 {@link TaskOneByOneBase#Execute(Object, Action0, String, Action0, DispatchMode)}
	 * 及其 int/long key 版本的最长重载。
	 */
	public void execute(@NotNull TaskOneByOneBase oneByOne) {
		executeByKey(oneByOne, new TaskOneByOneQueue.TaskAction(action, name, cancel, modeOrDefault()));
	}

	/**
	 * 提交到全局静态 {@link Task#getOneByOne()}，等价 {@code execute(Task.getOneByOne())}。
	 */
	public void execute() {
		execute(Task.getOneByOne());
	}

	/**
	 * 提交到 {@link TaskOneByOneByKey2}。
	 * 等价 {@link TaskOneByOneByKey2#Execute(int, Action0, String, DispatchMode)}
	 * （long key 转 Long.hashCode、Object key 转 hashCode 后委托 int 版，与旧重载一致）。
	 *
	 * @throws IllegalArgumentException 已设置 {@link #cancel} 时（Key2 不支持 shutdown/cancel）
	 */
	public void execute(@NotNull TaskOneByOneByKey2 oneByOne) {
		if (cancel != null)
			throw new IllegalArgumentException("cancel is not supported by TaskOneByOneByKey2");
		oneByOne.executeActionCore(hashKey(), action, name, modeOrDefault());
	}
}
