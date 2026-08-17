package Zeze.Util;

import java.util.concurrent.Future;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 返回 long 结果任务（如协议处理器）的提交参数，通过 {@link TaskSpec#ofFunc} 构造。
 *
 * <pre>
 * TaskSpec.ofFunc(() -> p.handle(this, factoryHandle))
 *         .protocol(p).errorHandle(Protocol::trySendResultCode)
 *         .mode(factoryHandle.Mode).executeUnsafe();
 * </pre>
 */
public final class FuncLongTaskSpec extends AbstractTaskSpec implements TaskSpec {
	private final @NotNull FuncLong func;
	private @Nullable Protocol<?> protocol;
	private @Nullable ProtocolErrorHandle errorHandle;

	FuncLongTaskSpec(@NotNull FuncLong func) {
		this.func = func;
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用协议类名
	 */
	public @NotNull FuncLongTaskSpec name(@Nullable String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param protocol 关联的协议，用于日志与错误处理，可为空
	 */
	public @NotNull FuncLongTaskSpec protocol(@Nullable Protocol<?> protocol) {
		this.protocol = protocol;
		return this;
	}

	/**
	 * @param errorHandle 结果非0且协议是请求时执行的错误处理
	 */
	public @NotNull FuncLongTaskSpec errorHandle(@Nullable ProtocolErrorHandle errorHandle) {
		this.errorHandle = errorHandle;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal
	 */
	public @NotNull FuncLongTaskSpec mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * @param timeout 任务超时(毫秒)，不设置或小于0时，终结方法执行时取 Task.defaultTimeout
	 */
	public @NotNull FuncLongTaskSpec timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 等价 {@link Task#call(FuncLong, Protocol, ProtocolErrorHandle, String)}。
	 */
	public long call() {
		return Task.callFuncCore(func, protocol, errorHandle, name);
	}

	/**
	 * 事务感知：mode != Direct 且当前在运行中的事务内时延迟到事务提交后执行，否则立即异步执行。
	 * 等价 {@link Task#run(FuncLong, Protocol, ProtocolErrorHandle, String, DispatchMode, long)}。
	 */
	public void run() {
		Task.runTxnAware(mode,
				() -> Task.executeUnsafeFuncCore(func, protocol, errorHandle, name, mode, timeoutOrDefault()));
	}

	/**
	 * 注意: Unsafe 在事务中也会立即异步执行，即使之后该事务 redo 或 rollback 也无法撤销。等价
	 * {@link Task#runUnsafe(FuncLong, Protocol, ProtocolErrorHandle, String, DispatchMode, long)}。
	 */
	public @NotNull Future<Long> runUnsafe() {
		return Task.runUnsafeFuncCore(func, protocol, errorHandle, name, mode, timeoutOrDefault());
	}

	/**
	 * 注意: Unsafe 在事务中也会立即异步执行，即使之后该事务 redo 或 rollback 也无法撤销。等价
	 * {@link Task#executeUnsafe(FuncLong, Protocol, ProtocolErrorHandle, String, DispatchMode, long)}。
	 */
	public void executeUnsafe() {
		Task.executeUnsafeFuncCore(func, protocol, errorHandle, name, mode, timeoutOrDefault());
	}
}
