package Zeze.Net;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 请求-应答协议基类。发送侧实例为一次性：Send/SendForWait 只能进入一次，
 * 发送失败后同样不得复用——重试请新建实例（每次发送需要新的sessionId）。
 * 需要「可靠投递/超时重试」语义时，新建实例重发，并配合协议层幂等或服务端按请求标识去重
 */
public abstract class Rpc<TArgument extends Serializable, TResult extends Serializable> extends Protocol<TArgument> {
	protected static final @NotNull Logger logger = LogManager.getLogger(Rpc.class);

	private static final @NotNull VarHandle SEND_RESULT_DONE;
	static {
		try {
			SEND_RESULT_DONE = MethodHandles.lookup().findVarHandle(Rpc.class, "sendResultDone", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public TResult Result;
	protected transient @Nullable Binary resultEncoded; // 如果设置了这个，发送结果的时候，优先使用这个编码过的。
	private long sessionId;
	private transient @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle;
	private transient @Nullable TaskCompletionSource<TResult> future;
	private int timeout = 5000;
	private int timeoutBroken = 30_000;
	private boolean isTimeout;
	private boolean isRequest = true;
	protected volatile transient boolean sendResultDone;

	@Override
	public int getFamilyClass() {
		return isRequest ? FamilyClass.Request : FamilyClass.Response;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public @Nullable ProtocolHandle<Rpc<TArgument, TResult>> getResponseHandle() {
		return responseHandle;
	}

	public void setResponseHandle(@Nullable ProtocolHandle<Rpc<TArgument, TResult>> handle) {
		responseHandle = handle;
	}

	public @Nullable TaskCompletionSource<TResult> getFuture() {
		return future;
	}

	public void setFuture(@Nullable TaskCompletionSource<TResult> future) {
		this.future = future;
	}

	public int getTimeout() {
		return timeout;
	}

	public int getTimeoutBroken() {
		return timeoutBroken;
	}

	public void setTimeoutBroken(int timeoutBroken) {
		this.timeoutBroken = timeoutBroken;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isTimeout() {
		return isTimeout;
	}

	public void setIsTimeout(boolean isTimeout) {
		this.isTimeout = isTimeout;
	}

	@Override
	public final boolean isRequest() {
		return isRequest;
	}

	@Override
	public final void setRequest(boolean request) {
		isRequest = request;
	}

	@Override
	public TResult getResultBean() {
		return Result;
	}

	public void schedule(@NotNull Service service, long sessionId, int millisecondsTimeout) {
		long timeout = Math.max(millisecondsTimeout, 0);
		if (Reflect.inDebugMode)
			timeout += 10 * 60 * 1000; // 调试状态下RPC超时放宽到至少10分钟,方便调试时不容易超时

		TaskSpec.ofAction(() -> {
			// 实例一次性（禁止同实例重发）后，本定时器唯一对应自己的那次发送：
			// 双参移除失败即条目已被应答消费，直接跳过。
			if (!service.removeRpcContext(sessionId, this))
				return; // 一般来说，此时结果已经返回。

			isTimeout = true;
			setResultCode(Procedure.Timeout);

			if (future != null)
				future.setException(RpcTimeoutException.getInstance());
			else if (responseHandle != null) {
				// 本来Schedule已经在Task中执行了，这里又派发一次。
				// 主要是为了让应用能拦截修改Response的处理方式。
				var factoryHandle = service.findProtocolFactoryHandle(getTypeId());
				if (factoryHandle != null)
					service.dispatchRpcResponse(this, responseHandle, factoryHandle);
				else // 工厂缺失时静默丢弃responseHandle排障无线索，warn对齐onRpcLostContext
					logger.warn("rpc timeout: protocol factory not found, response handle skipped: {}", this);
			}
		// 超时清理必须立即注册（scheduleNow）：此刻请求字节已发出，
		// 即使所在事务随后回滚，应答仍会到来或永不到来，上下文必须有超时兜底；
		// 事务感知的 schedule 会随回滚丢弃注册，导致 rpcContexts 条目永驻、SendForWait 永久挂起。
		}).scheduleNow(timeout);
	}

	/**
	 * 使用当前 rpc 中设置的参数发送。
	 * 总是建立上下文，总是返回true。
	 * 这个方法是 Protocol 的重载。
	 * 用于不需要处理结果的请求。
	 * 本实例只能发送一次（含发送失败）；重试请新建实例（参见类注释）。
	 *
	 * @param so socket to sendTo
	 * @return true: success.
	 */
	@Override
	public boolean Send(@Nullable AsyncSocket so) {
		return Send(so, responseHandle, timeout);
	}

	/**
	 * 异步发送rpc请求。
	 * 1. 如果返回true，表示请求已经发送，并且建立好了上下文。
	 * 2. 如果返回false，请求没有发送成功，上下文也没有保留。
	 *
	 * @param so             socket to sendTo
	 * @param responseHandle response handle for this rpc
	 * @return true: success
	 */
	public final boolean Send(@Nullable AsyncSocket so,
	                          @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle) {
		return Send(so, responseHandle, timeout);
	}

	public final boolean Send(@Nullable AsyncSocket so,
	                          @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle,
	                          int millisecondsTimeout) {
		if (so == null)
			return false;
		if (sessionId != 0)
			throw new IllegalStateException("Rpc already sent (sessionId=" + sessionId
					+ "); create a new instance to retry: " + this);
		Service service = so.getService();

		this.responseHandle = responseHandle;
		sessionId = service.addRpcContext(this);
		timeout = millisecondsTimeout;
		isTimeout = false;
		isRequest = true;

		// 超时兜底先于编码/发送挂好——super.Send(so)内编码（写完sessionId后Argument.encode，
		// 用户bean可抛）或传输层异常逃逸时，上下文仍有超时回收与回调，
		// 发送返回false时下方双参remove先赢，超时定时器到期时remove(sessionId,this)必失败跳过，
		// 无双重回调（LongConcurrentHashMap.remove(key,value)为原子条件删除）。
		schedule(service, sessionId, millisecondsTimeout);
		if (super.Send(so))
			return true;

		// 发送失败，一般是连接失效，此时删除上下文。
		// 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
		// 如果ctx已经被并发的Remove，也就是被处理了，这里返回true。
		// 实例不因失败解禁：失败重试同样请新建实例（保持一次性语义简单）。
		return !service.removeRpcContext(sessionId, this);
	}

	/**
	 * 总是回调responseHandle，即使发送返回false。
	 * 回调前设置: this.setIsTimeout(true); this.setResultCode(Procedure.FailCallback);
	 *
	 * @param so socket
	 * @param responseHandle response handle
\	 * @return true success, false fail.
	 */
	public boolean sendCallbackAlways(@Nullable AsyncSocket so,
									  @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle) {
		if (responseHandle == null)
			throw new IllegalStateException("responseHandle is null"); // 调用这个函数不允许没有回调。

		if (Send(so, responseHandle, timeout))
			return true;

		this.setIsTimeout(true);
		this.setResultCode(Procedure.FailCallback);
		// 无事务或者whileCommit中都需要立即回调(now）。事务中调用也是立即。另起线程避免whileCommit中调用这个函数，回调的时候不能启用事务。
		TaskSpec.ofFunc(() -> responseHandle.handle(this)).executeSystemOneByOne();
		return false;
	}

	public long sendCallbackBrokenTimeout(@NotNull Connector c,
										 @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle) {
		if (responseHandle == null)
			throw new IllegalStateException("responseHandle is null"); // 调用这个函数不允许没有回调。

		if (timeout + 1000 > timeoutBroken)
			throw new IllegalArgumentException("timeoutRpc + 1000 > timeoutBroken");

		if (Send(c.getSocket(), responseHandle, timeout))
			return Procedure.Success;

		if (!c.checkBrokenTimeout(timeoutBroken))
			return Procedure.FailDiscard; // 失败，仍然丢了callback

		this.setIsTimeout(true);
		this.setResultCode(Procedure.FailCallback);
		TaskSpec.ofFunc(() -> responseHandle.handle(this)).executeSystemOneByOne();
		return Procedure.FailCallback;
	}

	public final TaskCompletionSource<TResult> SendForWait(@Nullable AsyncSocket so) {
		return SendForWait(so, timeout);
	}

	// 注意这个同步发送方法会覆盖future,而且之后不会自动清除,除非再次调用同步发送
	// 如果接着调用异步发送,可能因为旧的future导致无法响应responseHandle
	public final TaskCompletionSource<TResult> SendForWait(@Nullable AsyncSocket so, int millisecondsTimeout) {
		future = new TaskCompletionSource<>();
		if (!Send(so, null, millisecondsTimeout))
			future.setException(new IllegalStateException("Send Fail."));
		return future;
	}

	// 使用异步方式实现的同步等待版本

	public final void SendAndWaitCheckResultCode(@Nullable AsyncSocket so) {
		SendAndWaitCheckResultCode(so, timeout);
	}

	public final void SendAndWaitCheckResultCode(@Nullable AsyncSocket so, int millisecondsTimeout) {
		SendForWait(so, millisecondsTimeout).await();
		if (resultCode != 0)
			throw new IllegalStateException(String.format("Rpc Invalid ResultCode=%d %s", resultCode, this));
	}

	@Override
	public void SendResult(@Nullable Binary result) {
		if (!tryMarkSendResultDone()) {
			logger.warn("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception("only for stack trace"));
			return;
		}
		resultEncoded = result;
		isRequest = false;
		if (!super.Send(getSender()))
			logger.warn("Rpc.SendResult Failed: {} {}", getSender(), this);
	}

	// sendResultDone的检查-设置必须原子：responseHandle回调线程与派发层onError兜底（trySendResultCode）
	// 可能并发应答，两线程都过检查会双重发送Result，破坏"最多一次"语义。VarHandle CAS仲裁。
	// mark先于resultCode等字段写：只有赢家写字段，输家不会污染赢家在途的encode；
	// 轮询isSendResultDone后读resultCode仅SendResultCode路径有可见性保证（它先写resultCode再mark）。
	private boolean tryMarkSendResultDone() {
		return (boolean)SEND_RESULT_DONE.compareAndSet(this, false, true);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isSendResultDone() {
		return sendResultDone;
	}

	@Override
	public boolean trySendResultCode(long code) {
		if (!tryMarkSendResultDone())
			return false;
		setResultCode(code);
		resultEncoded = null;
		isRequest = false;
		if (!super.Send(getSender()))
			logger.warn("Rpc.trySendResultCode Failed: {} {}", getSender(), this);
		return true;
	}

	@Override
	public void dispatch(@NotNull Service service,
	                     @NotNull Service.ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isRequest) {
			super.dispatch(service, factoryHandle);
			return;
		}

		// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
		var ctx = removeRpcContextChecked(service, sessionId, this);
		if (ctx == null) {
			service.onRpcLostContext(this);
			return;
		}
		var context = setupRpcResponseContext(ctx);
		if (context.future != null)
			context.future.setResult(context.Result); // SendForWait，设置结果唤醒等待者。
		else if (context.responseHandle != null)
			service.dispatchRpcResponse(context, context.responseHandle, factoryHandle);
	}

	// 应答会合先校验后消费。sessionId 发号流全 JVM 共享且明文入帧、
	// 顺序可枚举，而原会合仅按帧内号消费——任一同 Service 对端可伪造异协议 Response 帧
	// 劫持他人在飞上下文（注入任意Result/回调CCE，真实应答沦为lost）。校验两层：
	// ①typeId 一致（上下文协议==应答帧协议，伪造常携异协议 typeId）；②ctx.sender!=null
	// 时应答必须从原发送连接到达（Raft/SM/GCM 全家原连接应答）。sender==null 的上下文
	// （Online 经 linkd 转发的合法跨连接应答）仅受①保护——按 linkName 绑定的第三层
	// 属独立设计。校验失败不消费上下文（真实应答或超时仍
	// 可达），仅限频告警。
	private static volatile long lastResponseMismatchLogMs; // 告警限频（60秒一条，防日志刷屏DoS）

	public static <T extends Protocol<?>> @Nullable T removeRpcContextChecked(
			@NotNull Service service, long sid, @NotNull Rpc<?, ?> response) {
		var ctx = service.getRpcContext(sid);
		if (ctx == null)
			return null;
		if (ctx.getTypeId() != response.getTypeId()
				|| (ctx.getSender() != null && ctx.getSender() != response.getSender())) {
			var now = System.currentTimeMillis();
			var last = lastResponseMismatchLogMs;
			if (now - last >= 60_000) {
				lastResponseMismatchLogMs = now;
				logger.warn("rpc response rejected (typeId/connection mismatch, possible hijack):"
						+ " sessionId={}, expect typeId={}, sender={}, got typeId={}, sender={}",
						sid, ctx.getTypeId(), ctx.getSender(), response.getTypeId(), response.getSender());
			}
			return null;
		}
		return service.removeRpcContext(sid);
	}

	public Rpc<TArgument, TResult> setupRpcResponseContext(@NotNull Protocol<?> ctx) {
		@SuppressWarnings("unchecked")
		var context = (Rpc<TArgument, TResult>)ctx;
		context.setSender(getSender());
		context.resultCode = resultCode;
		context.Result = Result;
		context.isTimeout = false; // not need
		context.isRequest = false;
		return context;
	}

	public long setFutureResultOrCallHandle() throws Exception {
		if (future != null) {
			future.setResult(Result);
			return 0;
		}

		if (responseHandle != null)
			return responseHandle.handle(this);

		logger.debug("rpc response handle miss: {}", this);
		return 0;
	}

	@Override
	public long handle(@NotNull Service service,
	                   @NotNull Service.ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isRequest)
			return super.handle(service, factoryHandle);

		// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
		// 本方法随action在事务redo时整体重跑，会合消费（removeRpcContext）只能发生一次：
		// 首轮解析并缓存到当前事务，重试直接复用。
		Rpc<TArgument, TResult> context = Transaction.resolveOnceOrApply(service, sessionId,
				sid -> removeRpcContextChecked(service, sid, this));
		if (context == null) {
			service.onRpcLostContext(this);
			// 上下文丢失（一般已被超时消费）：立即失败终止，不空转剩余重试、不以Success提交空事务。
			return Procedure.Unknown;
		}

		context.setSender(getSender());
		context.resultCode = resultCode;
		context.Result = Result;
		context.isTimeout = false; // not need
		context.isRequest = false;

		if (context.future != null)
			context.future.setResult(context.Result); // SendForWait，设置结果唤醒等待者。
		else if (context.responseHandle != null)
			return context.responseHandle.handle(context);

		return 0;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var header = getFamilyClass();
		if (resultCode == 0)
			bb.WriteUInt(header);
		else {
			bb.WriteUInt(header | FamilyClass.BitResultCode);
			bb.WriteLong(resultCode);
		}
		bb.WriteLong(sessionId);
		if (isRequest)
			Argument.encode(bb);
		else if (resultEncoded != null)
			bb.Append(resultEncoded.bytesUnsafe(), resultEncoded.getOffset(), resultEncoded.size());
		else
			Result.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var header = bb.ReadUInt();
		var familyClass = header & FamilyClass.FamilyClassMask;
		if (!FamilyClass.isRpc(familyClass))
			throw new IllegalStateException("invalid header(" + header + ") for decoding rpc: " + getClass().getName());
		isRequest = familyClass == FamilyClass.Request;
		resultCode = (header & FamilyClass.BitResultCode) != 0 ? bb.ReadLong() : 0;
		sessionId = bb.ReadLong();
		if (isRequest)
			Argument.decode(bb);
		else
			Result.decode(bb);
	}

	@Override
	public int preAllocSize() {
		return 1 + 9 + 9 + (isRequest ? Argument.preAllocSize() : Result.preAllocSize());
	}

	@Override
	public void preAllocSize(int size) {
		// 扣最小保证字节（各变长字段按最少1B计），保留最坏-最小的16B差作稳态迟滞带：估算为静态
		// per-class的high-water mark，实例尺寸天然抖动（变长字段/resultCode有无），带宽吸收小抖动
		// 以免EnsureWrite扩容（toPower2跳变+全量arraycopy）；零冗余精确贴合反而每次尺寸上探都付
		// 一次pow2扩容。对齐Protocol.preAllocSize(int)惯例（正向留10、反推扣1、带宽9B）。
		(isRequest ? Argument : Result).preAllocSize(size - 1 - 1 - 1);
	}

	@Override
	public @NotNull String toString() {
		return String.format("%s IsRequest=%b SessionId=%d ResultCode=%d\nArgument=%s\nResult=%s",
				getClass().getName(), isRequest, sessionId, getResultCode(), Argument, Result);
	}
}
