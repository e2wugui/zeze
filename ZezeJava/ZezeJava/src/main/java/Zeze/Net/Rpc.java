package Zeze.Net;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import Zeze.Util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 请求-应答协议基类。发送侧实例为一次性：Send/SendForWait 只能进入一次，
 * 发送失败后同样不得复用——重试请新建实例（每次发送需要新的sessionId）。
 * 需要「可靠投递/超时重试」语义时，新建实例重发，并配合协议层幂等或服务端按请求标识去重
 */
public abstract class Rpc<TArgument extends Serializable, TResult extends Serializable> extends Protocol<TArgument> {
	protected static final Logger logger = LogManager.getLogger(Rpc.class);

	private static final VarHandle SEND_RESULT_DONE, REQUEST_SENT;
	static {
		try {
			SEND_RESULT_DONE = MethodHandles.lookup().findVarHandle(Rpc.class, "sendResultDone", boolean.class);
			REQUEST_SENT = MethodHandles.lookup().findVarHandle(Rpc.class, "requestSent", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("NotNullFieldNotInitialized") // 由具体 RPC 子类构造器初始化
	public TResult Result;
	protected transient @Nullable Binary resultEncoded; // 如果设置了这个，发送结果的时候，优先使用这个编码过的。
	private long sessionId;
	private transient @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle;
	private transient @Nullable TaskCompletionSource<TResult> future;
	// 超时兜底定时器，上下文被消费时经 cancelTimeout 取消以防滞留调度队列。
	private volatile transient @Nullable Timeout timeoutTask;
	private int timeout = 5000;
	private int timeoutBroken = 30_000;
	private boolean isTimeout;
	private boolean isRequest = true;
	protected volatile transient boolean sendResultDone;
	private volatile transient boolean requestSent;

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

	public void schedule(Service service, long sessionId, int millisecondsTimeout) {
		long timeout = Math.max(millisecondsTimeout, 0);
		if (Reflect.inDebugMode)
			timeout += 10 * 60 * 1000; // 调试状态下RPC超时放宽到至少10分钟,方便调试时不容易超时

		// 使用非事务感知的时间轮同步注册，避免事务回滚撤销超时兜底。
		timeoutTask = Task.getTimeoutManager().schedule(timeout, () -> {
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
				else
					logger.warn("rpc timeout: protocol factory not found, response handle skipped: {}", this);
			}
		});
		// 断连可能在 schedule 返回前已消费上下文，发布后复查补偿取消。
		if (service.getRpcContext(sessionId) != this)
			cancelTimeout();
	}

	/** 取消超时兜底定时器（上下文被消费时由 {@link Service#removeRpcContext} 调用，幂等）。 */
	public void cancelTimeout() {
		var task = timeoutTask;
		if (task != null) {
			timeoutTask = null;
			task.cancel();
		}
	}

	/**
	 * 使用当前 rpc 中设置的参数发送。
	 * 发送成功或请求已经被完成时返回 true，否则返回 false。
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
		return sendRequest(so, responseHandle, millisecondsTimeout, null);
	}

	private boolean sendRequest(@Nullable AsyncSocket so,
			@Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle, int millisecondsTimeout,
			@Nullable TaskCompletionSource<TResult> waitFuture) {
		if (so == null)
			return false;
		// 先 CAS 占住发送权，拒绝并发或重入发送。
		if (sessionId != 0 || !(boolean)REQUEST_SENT.compareAndSet(this, false, true))
			throw new IllegalStateException("Rpc already sent (sessionId=" + sessionId
					+ "); create a new instance to retry: " + this);
		Service service = so.getService();

		this.responseHandle = responseHandle;
		if (waitFuture != null)
			future = waitFuture;
		setSender(so);
		timeout = millisecondsTimeout;
		isTimeout = false;
		isRequest = true;

		// 发号在生命周期锁外执行；登记与连接置死互斥，确保销毁回调看到的请求字段已初始化。
		while (true) {
			//noinspection DataFlowIssue
			long sid = service.nextSessionId();
			var registered = so.runIfOpen(() -> {
				sessionId = sid;
				return service.tryAddRpcContext(sid, this);
			});
			if (registered == null)
				return false;
			if (registered)
				break;
		}

		// 先挂超时再编码/发送，避免编码或传输层抛异常导致上下文失去超时兜底。
		schedule(service, sessionId, millisecondsTimeout);
		if (super.Send(so))
			return true;

		// 发送失败删除上下文；若已被并发移除（已处理）则返回 true。
		return !service.removeRpcContext(sessionId, this);
	}

	/**
	 * 总是回调responseHandle，即使发送返回false。
	 * 回调前设置: this.setIsTimeout(true); this.setResultCode(Procedure.FailCallback);
	 *
	 * @param so socket
	 * @param responseHandle response handle
	 * @return true success, false fail.
	 */
	public boolean sendCallbackAlways(@Nullable AsyncSocket so,
									  @Nullable ProtocolHandle<Rpc<TArgument, TResult>> responseHandle) {
		if (responseHandle == null)
			throw new IllegalStateException("responseHandle is null"); // 调用这个函数不允许没有回调。

		if (Send(so, responseHandle, timeout))
			return true;

		this.setIsTimeout(true);
		this.setResultCode(Procedure.FailSend);
		// 无事务或者whileCommit中都需要立即回调(now）。事务中调用也是立即。另起线程避免whileCommit中调用这个函数，回调的时候不能启用事务。
		TaskSpec.ofFunc(() -> responseHandle.handle(this)).executeSystemOneByOne();
		return false;
	}

	public long sendCallbackBrokenTimeout(Connector c,
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
		this.setResultCode(Procedure.FailSend);
		TaskSpec.ofFunc(() -> responseHandle.handle(this)).executeSystemOneByOne();
		return Procedure.FailSend;
	}

	public final TaskCompletionSource<TResult> SendForWait(@Nullable AsyncSocket so) {
		return SendForWait(so, timeout);
	}

	public final TaskCompletionSource<TResult> SendForWait(@Nullable AsyncSocket so, int millisecondsTimeout) {
		var waitFuture = new TaskCompletionSource<TResult>();
		if (!sendRequest(so, null, millisecondsTimeout, waitFuture))
			waitFuture.setException(new IllegalStateException("Send Fail."));
		return waitFuture;
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

	// VarHandle CAS 保证 Result 最多发送一次；先占发送权再写 resultCode 等字段。
	protected final boolean tryMarkSendResultDone() {
		return (boolean)SEND_RESULT_DONE.compareAndSet(this, false, true);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isSendResultDone() {
		return sendResultDone;
	}

	@Override
	public boolean trySendResultCode(long code) {
		return sendResultCode(code, null);
	}

	@Override
	protected boolean sendResultCode(long code, @Nullable Binary result) {
		if (!tryMarkSendResultDone())
			return false;
		setResultCode(code);
		resultEncoded = result;
		isRequest = false;
		if (!super.Send(getSender()))
			logger.warn("Rpc.trySendResultCode Failed: {} {}", getSender(), this);
		return true;
	}

	@Override
	public void dispatch(Service service,
	                     Service.ProtocolFactoryHandle<?> factoryHandle) throws Exception {
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

	// 应答会合先校验 typeId 与原发送连接（ctx.sender != null 时），不匹配则不消费上下文，仅限频告警。
	private static volatile long lastResponseMismatchLogMs;

	public static @Nullable Rpc<?, ?> removeRpcContextChecked(
			Service service, long sid, Rpc<?, ?> response) {
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
		return service.removeRpcContext(sid, ctx) ? ctx : null;
	}

	public Rpc<TArgument, TResult> setupRpcResponseContext(Rpc<?, ?> ctx) {
		//noinspection unchecked
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
	public long handle(Service service,
	                   Service.ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isRequest)
			return super.handle(service, factoryHandle);

		// 事务 redo 时上下文只能消费一次：首轮解析并缓存到当前事务，重试直接复用。
		var ctx = Transaction.resolveOnceOrApply(service, sessionId,
				sid -> removeRpcContextChecked(service, sid, this));
		if (ctx == null) {
			service.onRpcLostContext(this);
			return Procedure.Unknown;
		}

		var context = setupRpcResponseContext(ctx);

		if (context.future != null)
			context.future.setResult(context.Result); // SendForWait，设置结果唤醒等待者。
		else if (context.responseHandle != null)
			return context.responseHandle.handle(context);

		return 0;
	}

	@Override
	public void encode(ByteBuffer bb) {
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
	public void decode(IByteBuffer bb) {
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
		// 扣最小头部字节，保留一定余量吸收变长字段抖动以减少扩容。
		(isRequest ? Argument : Result).preAllocSize(size - 1 - 1 - 1);
	}

	@Override
	public String toString() {
		return String.format("%s IsRequest=%b SessionId=%d ResultCode=%d\nArgument=%s\nResult=%s",
				getClass().getName(), isRequest, sessionId, getResultCode(), Argument, Result);
	}
}
