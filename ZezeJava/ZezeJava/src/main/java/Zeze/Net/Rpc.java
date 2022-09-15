package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Rpc<TArgument extends Bean, TResult extends Bean> extends Protocol<TArgument> {
	private static final Logger logger = LogManager.getLogger(Rpc.class);

	public TResult Result;
	private Binary resultEncoded; // 如果设置了这个，发送结果的时候，优先使用这个编码过的。
	private long sessionId;
	private ProtocolHandle<Rpc<TArgument, TResult>> responseHandle;
	private TaskCompletionSource<TResult> future;
	private int timeout = 5000;
	private boolean isTimeout;
	private boolean isRequest = true;
	private boolean sendResultDone; // XXX ugly

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public ProtocolHandle<Rpc<TArgument, TResult>> getResponseHandle() {
		return responseHandle;
	}

	public void setResponseHandle(ProtocolHandle<Rpc<TArgument, TResult>> handle) {
		responseHandle = handle;
	}

	public TaskCompletionSource<TResult> getFuture() {
		return future;
	}

	public int getTimeout() {
		return timeout;
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

	private void schedule(Service service, long sessionId, int millisecondsTimeout) {
		long timeout = Math.max(millisecondsTimeout, 0);
		if (Reflect.inDebugMode)
			timeout += 10 * 60 * 1000; // 调试状态下RPC超时放宽到至少10分钟,方便调试时不容易超时

		Task.schedule(timeout, () -> {
			Rpc<TArgument, TResult> context = service.removeRpcContext(sessionId);
			if (context == null) // 一般来说，此时结果已经返回。
				return;

			context.isTimeout = true;
			context.setResultCode(Zeze.Transaction.Procedure.Timeout);

			if (context.future != null)
				context.future.TrySetException(RpcTimeoutException.getInstance());
			else if (context.responseHandle != null) {
				// 本来Schedule已经在Task中执行了，这里又派发一次。
				// 主要是为了让应用能拦截修改Response的处理方式。
				// Timeout 应该是少的，先这样了。
				var factoryHandle = service.findProtocolFactoryHandle(context.getTypeId());
				if (factoryHandle != null)
					service.DispatchRpcResponse(context, context.responseHandle, factoryHandle);
			}
		});
	}

	/**
	 * 使用当前 rpc 中设置的参数发送。
	 * 总是建立上下文，总是返回true。
	 * 这个方法是 Protocol 的重载。
	 * 用于不需要处理结果的请求
	 * 或者重新发送已经设置过 ResponseHandle 等的请求。
	 *
	 * @param so socket to sendTo
	 * @return true: success.
	 */
	@Override
	public boolean Send(AsyncSocket so) {
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
	public final boolean Send(AsyncSocket so, ProtocolHandle<Rpc<TArgument, TResult>> responseHandle) {
		return Send(so, responseHandle, timeout);
	}

	public final boolean Send(AsyncSocket so, ProtocolHandle<Rpc<TArgument, TResult>> responseHandle,
							  int millisecondsTimeout) {
		if (so == null)
			return false;
		Service service = so.getService();
		if (service == null)
			return false;

		// try remove. 只维护一个上下文。
		service.removeRpcContext(sessionId, this);
		sessionId = service.addRpcContext(this);
		this.responseHandle = responseHandle;
		timeout = millisecondsTimeout;
		isTimeout = false;
		isRequest = true;

		if (super.Send(so)) {
			schedule(service, sessionId, millisecondsTimeout);
			return true;
		}

		// 发送失败，一般是连接失效，此时删除上下文。
		// 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
		// 恢复最初的语义吧：如果ctx已经被并发的Remove，也就是被处理了，这里返回true。
		return !service.removeRpcContext(sessionId, this);
	}

	/**
	 * 不管发送是否成功，总是建立RpcContext。
	 * 连接(so)可以为null，此时Rpc请求将在Timeout后回调。
	 */
	public final void SendReturnVoid(Service service, AsyncSocket so,
									 ProtocolHandle<Rpc<TArgument, TResult>> responseHandle) {
		SendReturnVoid(service, so, responseHandle, 5000);
	}

	public final void SendReturnVoid(Service service, AsyncSocket so,
									 ProtocolHandle<Rpc<TArgument, TResult>> responseHandle, int millisecondsTimeout) {
		if (so != null && so.getService() != service)
			throw new IllegalStateException("so.Service != service");

		this.responseHandle = responseHandle;
		timeout = millisecondsTimeout;
		isTimeout = false;
		isRequest = true;
		sessionId = service.addRpcContext(this);
		super.Send(so);
		schedule(service, sessionId, millisecondsTimeout);
	}

	public final TaskCompletionSource<TResult> SendForWait(AsyncSocket so) {
		return SendForWait(so, 5000);
	}

	public final TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout) {
		future = new TaskCompletionSource<>();
		if (!Send(so, null, millisecondsTimeout))
			future.TrySetException(new IllegalStateException("Send Failed."));
		return future;
	}

	// 使用异步方式实现的同步等待版本

	public final void SendAndWaitCheckResultCode(AsyncSocket so) {
		SendAndWaitCheckResultCode(so, 5000);
	}

	public final void SendAndWaitCheckResultCode(AsyncSocket so, int millisecondsTimeout) {
		SendForWait(so, millisecondsTimeout).await();
		if (getResultCode() != 0)
			throw new IllegalStateException(String.format("Rpc Invalid ResultCode=%d %s", getResultCode(), this));
	}

	@Override
	public void SendResult(Binary result) {
		if (sendResultDone) {
			logger.error("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		sendResultDone = true;
		resultEncoded = result;
		isRequest = false;
		if (!super.Send(getSender()))
			logger.warn("Rpc.SendResult Failed: {} {}", getSender(), this);
	}

	@Override
	public boolean trySendResultCode(long code) {
		if (sendResultDone)
			return false;
		setResultCode(code);
		SendResult(null);
		return true;
	}

	@Override
	public <P extends Protocol<?>> void dispatch(Service service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Throwable {
		if (isRequest) {
			@SuppressWarnings("unchecked") P proto = (P)this;
			service.DispatchProtocol(proto, factoryHandle);
			return;
		}

		// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
		Rpc<TArgument, TResult> context = service.removeRpcContext(sessionId);
		if (context == null) {
			logger.warn("rpc response: lost context, maybe timeout. {}", this);
			return;
		}

		context.setSender(getSender());
		context.setUserState(getUserState());
		context.setResultCode(getResultCode());
		context.Result = Result;
		context.isTimeout = false; // not need
		context.isRequest = false;

		if (context.future != null)
			context.future.SetResult(context.Result); // SendForWait，设置结果唤醒等待者。
		else if (context.responseHandle != null)
			service.DispatchRpcResponse(context, context.responseHandle, factoryHandle);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBool(isRequest);
		bb.WriteLong(sessionId);
		bb.WriteLong(getResultCode());

		if (isRequest)
			Argument.encode(bb);
		else if (resultEncoded != null)
			bb.Append(resultEncoded.bytesUnsafe(), resultEncoded.getOffset(), resultEncoded.size());
		else
			Result.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		isRequest = bb.ReadBool();
		sessionId = bb.ReadLong();
		setResultCode(bb.ReadLong());

		(isRequest ? Argument : Result).decode(bb);
	}

	@Override
	public int preAllocSize() {
		return 1 + 9 + 9 + (isRequest ? Argument.preAllocSize() : Result.preAllocSize());
	}

	@Override
	public void preAllocSize(int size) {
		(isRequest ? Argument : Result).preAllocSize(size - 1 - 1 - 1);
	}

	@Override
	public String toString() {
		return String.format("%s IsRequest=%b SessionId=%d ResultCode=%d%n\tArgument=%s%n\tResult=%s",
				getClass().getName(), isRequest, sessionId, getResultCode(), Argument, Result);
	}
}
