package Zeze.Net;

import java.util.Objects;
import java.util.concurrent.Future;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.FuncLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 带协议上下文的任务提交门面：承载 protocol/from/outProtocol/errorHandle 这些仅框架 dispatch 层
 * 需要的属性，让 {@link Zeze.Util.TaskSpec} 保持对 Zeze.Net 零依赖。动词约定与 TaskSpec 一致：
 *
 * <pre>
 * ProtocolDispatch.ofFunc(() -> p.handle(this, factoryHandle), p)  // 载荷 + 关联协议
 *         .onError(Protocol::trySendResultCode)                    // 结果非0且是请求时的错误回发
 *         .dispatchMode(factoryHandle.Mode).runNow();
 * ProtocolDispatch.ofProcedure(proc)
 *         .outProtocol(out)                                        // redo 时重新解码出的协议经它带出
 *         .onError(Protocol::trySendResultCode).runNow();
 * </pre>
 *
 * 协议版完整日志（含 AsyncSocket 上下文、UserState、ProcessReturnErrorLogLevel 配置）由本路径保留，
 * 保证单次调用只记一次日志、只做一次统计。
 *
 * <p>实例是一次性的、非线程安全：任何终结方法执行后实例失效，再调 setter/终结方法抛
 * {@link IllegalStateException}。
 */
@SuppressWarnings("deprecation")
public final class ProtocolDispatch {
	private final @Nullable FuncLong func;
	private final @Nullable Procedure procedure;

	private boolean consumed;
	private @Nullable Protocol<?> protocol; // func 的关联协议 / procedure 的触发协议(from)
	private @Nullable OutObject<Protocol<?>> outProtocol; // 设置后走 outProtocol 分支，from 被忽略；仅 procedure
	private @Nullable ProtocolErrorHandle errorHandle;
	private @Nullable String name; // 仅 func 有效；procedure 固定使用 getActionName()
	private @Nullable DispatchMode dispatchMode; // null 等同 Normal
	private long timeout = -1; // 哨兵值：<0 表示未设置，终结方法执行时取 Task.defaultTimeout
	private boolean dispatchModeSet;
	private boolean timeoutSet;

	private ProtocolDispatch(@Nullable FuncLong func, @Nullable Procedure procedure, @Nullable Protocol<?> protocol) {
		this.func = func;
		this.procedure = procedure;
		this.protocol = protocol;
	}

	/**
	 * FuncLong 载荷 + 关联协议（用于日志与错误处理，可为空）。
	 */
	public static @NotNull ProtocolDispatch ofFunc(@NotNull FuncLong func, @Nullable Protocol<?> protocol) {
		return new ProtocolDispatch(Objects.requireNonNull(func), null, protocol);
	}

	/**
	 * Procedure 载荷。日志名固定使用 procedure.getActionName()（{@link #name} 对它无效）。
	 */
	public static @NotNull ProtocolDispatch ofProcedure(@NotNull Procedure procedure) {
		return new ProtocolDispatch(null, Objects.requireNonNull(procedure), null);
	}

	/**
	 * @param from 触发本过程的原协议，用于日志与错误处理，可为空；仅 ofProcedure 载荷有效
	 */
	public @NotNull ProtocolDispatch from(@Nullable Protocol<?> from) {
		checkNotConsumed();
		if (procedure == null)
			throw new IllegalArgumentException("from is only supported by ofProcedure payload");
		this.protocol = from;
		return this;
	}

	/**
	 * @param outProtocol 过程内部解码出的协议通过它带出（支持 redo 时重新解码），设置后 {@link #from} 被忽略；
	 *                    仅 ofProcedure 载荷有效
	 */
	public @NotNull ProtocolDispatch outProtocol(@NotNull OutObject<Protocol<?>> outProtocol) {
		checkNotConsumed();
		if (procedure == null)
			throw new IllegalArgumentException("outProtocol is only supported by ofProcedure payload");
		this.outProtocol = Objects.requireNonNull(outProtocol);
		return this;
	}

	/**
	 * @param errorHandle 结果非0且协议是请求时执行的错误处理，可为空
	 */
	public @NotNull ProtocolDispatch onError(@Nullable ProtocolErrorHandle errorHandle) {
		checkNotConsumed();
		this.errorHandle = errorHandle;
		return this;
	}

	/**
	 * @param name 任务名，用于日志与统计，默认使用协议类名；Procedure 载荷固定使用 getActionName()，此设置无效
	 */
	public @NotNull ProtocolDispatch name(@Nullable String name) {
		checkNotConsumed();
		this.name = name;
		return this;
	}

	/**
	 * @param dispatchMode 异步分发目标（Normal=默认池 / Critical=关键池 / Direct=调用线程），null 等同 Normal；
	 *                     call() 不消费它（显式设置会抛 IllegalArgumentException）
	 */
	public @NotNull ProtocolDispatch dispatchMode(@Nullable DispatchMode dispatchMode) {
		checkNotConsumed();
		this.dispatchMode = dispatchMode;
		this.dispatchModeSet = true;
		return this;
	}

	/**
	 * @param timeout 任务超时(毫秒)，不设置或小于0时，终结方法执行时取 Task.defaultTimeout；
	 *                call() 不消费它（显式设置会抛 IllegalArgumentException）
	 */
	public @NotNull ProtocolDispatch timeout(long timeout) {
		checkNotConsumed();
		this.timeout = timeout;
		this.timeoutSet = true;
		return this;
	}

	private void checkNotConsumed() {
		if (consumed)
			throw new IllegalStateException("ProtocolDispatch instance is single-use and has been consumed");
	}

	private void consume() {
		checkNotConsumed();
		consumed = true;
	}

	private long timeoutOrDefault() {
		return timeout < 0 ? Task.defaultTimeout : timeout;
	}

	/**
	 * 当前线程同步执行完，返回结果码。显式设置过 dispatchMode/timeout 时抛 IllegalArgumentException。
	 */
	public long call() {
		consume();
		if (dispatchModeSet || timeoutSet)
			throw new IllegalArgumentException("call() does not consume dispatchMode/timeout");
		if (func != null)
			return Task.call(func, protocol, errorHandle, name);
		assert procedure != null;
		if (outProtocol != null)
			return Task.call(procedure, outProtocol, errorHandle);
		return Task.call(procedure, protocol, errorHandle);
	}

	/**
	 * 异步·事务感知：dispatchMode != Direct 且当前在运行中的事务内时延迟到事务提交后执行
	 * （rollback 不执行、redo 由新一轮重新注册），否则立即入池；dispatchMode=Direct 时跳过事务检查，
	 * 当前线程立即同步执行。
	 */
	public void run() {
		consume();
		Task.runTxnAware(dispatchMode, this::runNowInternal);
	}

	/**
	 * 异步·立即入池，不等事务提交（即使在事务中，之后该事务 redo 或 rollback 也无法撤销）。
	 */
	public void runNow() {
		consume();
		runNowInternal();
	}

	private void runNowInternal() {
		var timeout = timeoutOrDefault();
		if (func != null) {
			Task.executeUnsafe(func, protocol, errorHandle, name, dispatchMode, timeout);
			return;
		}
		assert procedure != null;
		if (outProtocol != null)
			Task.executeUnsafe(procedure, outProtocol, errorHandle, dispatchMode, timeout);
		else
			Task.executeUnsafe(procedure, protocol, errorHandle, dispatchMode, timeout);
	}

	/**
	 * 同 {@link #runNow}，但返回结果码 Future。
	 */
	public @NotNull Future<Long> submitNow() {
		consume();
		var timeout = timeoutOrDefault();
		if (func != null)
			return Task.runUnsafe(func, protocol, errorHandle, name, dispatchMode, timeout);
		assert procedure != null;
		if (outProtocol != null)
			return Task.runUnsafe(procedure, outProtocol, errorHandle, dispatchMode, timeout);
		return Task.runUnsafe(procedure, protocol, errorHandle, dispatchMode, timeout);
	}
}
