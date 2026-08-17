package Zeze.Util;

import java.util.concurrent.Future;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 存储过程任务的提交参数，通过 {@link TaskSpec#ofProcedure} 构造。
 * 不提供 name 设置：日志名固定使用 procedure.getActionName()，与旧 API 一致。
 *
 * <pre>
 * TaskSpec.ofProcedure(zeze.newProcedure(() -> p.handle(this, factoryHandle), name, level))
 *         .outProtocol(outProtocol).errorHandle(Protocol::trySendResultCode)
 *         .mode(factoryHandle.Mode).executeUnsafe();
 * </pre>
 */
public final class ProcedureTaskSpec extends AbstractTaskSpec implements TaskSpec {
	private final @NotNull Procedure procedure;
	private @Nullable Protocol<?> from;
	private @Nullable OutObject<Protocol<?>> outProtocol; // 设置后走 outProtocol 分支，from 被忽略
	private @Nullable ProtocolErrorHandle errorHandle;

	ProcedureTaskSpec(@NotNull Procedure procedure) {
		this.procedure = procedure;
	}

	/**
	 * @param from 触发本过程的原协议，用于日志与错误处理，可为空
	 */
	public @NotNull ProcedureTaskSpec from(@Nullable Protocol<?> from) {
		this.from = from;
		return this;
	}

	/**
	 * @param outProtocol 过程内部解码出的协议通过它带出（支持 redo 时重新解码），设置后 {@link #from} 被忽略
	 */
	public @NotNull ProcedureTaskSpec outProtocol(@NotNull OutObject<Protocol<?>> outProtocol) {
		this.outProtocol = outProtocol;
		return this;
	}

	/**
	 * @param errorHandle 结果非0且协议是请求时执行的错误处理
	 */
	public @NotNull ProcedureTaskSpec errorHandle(@Nullable ProtocolErrorHandle errorHandle) {
		this.errorHandle = errorHandle;
		return this;
	}

	/**
	 * @param mode 调度模式，null 等同 Normal
	 */
	public @NotNull ProcedureTaskSpec mode(@Nullable DispatchMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * @param timeout 任务超时(毫秒)，不设置或小于0时，终结方法执行时取 Task.defaultTimeout
	 */
	public @NotNull ProcedureTaskSpec timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 等价 {@link Task#call(Procedure, Protocol, ProtocolErrorHandle)} 或设置了 outProtocol 时等价
	 * {@link Task#call(Procedure, OutObject, ProtocolErrorHandle)}。
	 */
	public long call() {
		return outProtocol != null
				? Task.callProcOutCore(procedure, outProtocol, errorHandle)
				: Task.callProcCore(procedure, from, errorHandle);
	}

	/**
	 * 事务感知：mode != Direct 且当前在运行中的事务内时延迟到事务提交后执行，否则立即异步执行。
	 * 等价 {@link Task#run(Procedure, Protocol, ProtocolErrorHandle, DispatchMode, long)}。
	 */
	public void run() {
		Task.runTxnAware(mode, outProtocol != null
				? () -> Task.executeUnsafeProcOutCore(procedure, outProtocol, errorHandle, mode, timeoutOrDefault())
				: () -> Task.executeUnsafeProcCore(procedure, from, errorHandle, mode, timeoutOrDefault()));
	}

	/**
	 * 注意: Unsafe 在事务中也会立即异步执行，即使之后该事务 redo 或 rollback 也无法撤销。等价
	 * {@link Task#runUnsafe(Procedure, Protocol, ProtocolErrorHandle, DispatchMode, long)}。
	 */
	public @NotNull Future<Long> runUnsafe() {
		return outProtocol != null
				? Task.runUnsafeProcOutCore(procedure, outProtocol, errorHandle, mode, timeoutOrDefault())
				: Task.runUnsafeProcCore(procedure, from, errorHandle, mode, timeoutOrDefault());
	}

	/**
	 * 注意: Unsafe 在事务中也会立即异步执行，即使之后该事务 redo 或 rollback 也无法撤销。等价
	 * {@link Task#executeUnsafe(Procedure, Protocol, ProtocolErrorHandle, DispatchMode, long)}。
	 */
	public void executeUnsafe() {
		if (outProtocol != null)
			Task.executeUnsafeProcOutCore(procedure, outProtocol, errorHandle, mode, timeoutOrDefault());
		else
			Task.executeUnsafeProcCore(procedure, from, errorHandle, mode, timeoutOrDefault());
	}
}
