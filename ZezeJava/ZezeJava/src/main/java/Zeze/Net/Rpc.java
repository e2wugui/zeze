package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Rpc<TArgument extends Bean, TResult extends Bean> extends Protocol<TArgument> {
	private static final Logger logger = LogManager.getLogger(Rpc.class);

	public TResult Result;
	private Binary ResultEncoded; // 如果设置了这个，发送结果的时候，优先使用这个编码过的。
	public long SessionId;
	private ProtocolHandle<Rpc<TArgument, TResult>> ResponseHandle;
	private TaskCompletionSource<TResult> Future;
	private int Timeout = 5000;
	private boolean IsTimeout;
	private boolean IsRequest = true;
	private boolean SendResultDone; // XXX ugly

	public long getSessionId() {
		return SessionId;
	}

	public void setSessionId(long sessionId) {
		SessionId = sessionId;
	}

	public ProtocolHandle<Rpc<TArgument, TResult>> getResponseHandle() {
		return ResponseHandle;
	}

	public void setResponseHandle(ProtocolHandle<Rpc<TArgument, TResult>> handle) {
		ResponseHandle = handle;
	}

	public TaskCompletionSource<TResult> getFuture() {
		return Future;
	}

	public int getTimeout() {
		return Timeout;
	}

	public void setTimeout(int timeout) {
		Timeout = timeout;
	}

	public boolean isTimeout() {
		return IsTimeout;
	}

	public void setIsTimeout(boolean isTimeout) {
		IsTimeout = isTimeout;
	}

	@Override
	public final boolean isRequest() {
		return IsRequest;
	}

	@Override
	public final void setRequest(boolean request) {
		IsRequest = request;
	}

	@Override
	public TResult getResultBean() {
		return Result;
	}

	private void Schedule(Service service, long sessionId, int millisecondsTimeout) {
		Task.schedule(millisecondsTimeout, () -> {
			Rpc<TArgument, TResult> context = service.RemoveRpcContext(sessionId);
			if (context == null) // 一般来说，此时结果已经返回。
				return;

			context.IsTimeout = true;
			context.setResultCode(Zeze.Transaction.Procedure.Timeout);

			if (context.Future != null)
				context.Future.TrySetException(RpcTimeoutException.getInstance());
			else if (context.ResponseHandle != null) {
				// 本来Schedule已经在Task中执行了，这里又派发一次。
				// 主要是为了让应用能拦截修改Response的处理方式。
				// Timeout 应该是少的，先这样了。
				var factoryHandle = service.FindProtocolFactoryHandle(context.getTypeId());
				if (factoryHandle != null)
					service.DispatchRpcResponse(context, context.ResponseHandle, factoryHandle);
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
		return Send(so, ResponseHandle, Timeout);
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
		return Send(so, responseHandle, Timeout);
	}

	public final boolean Send(AsyncSocket so, ProtocolHandle<Rpc<TArgument, TResult>> responseHandle,
							  int millisecondsTimeout) {
		if (so == null)
			return false;
		Service service = so.getService();
		if (service == null)
			return false;

		// try remove. 只维护一个上下文。
		service.RemoveRpcContext(SessionId, this);
		SessionId = service.AddRpcContext(this);
		ResponseHandle = responseHandle;
		Timeout = millisecondsTimeout;
		IsTimeout = false;
		IsRequest = true;

		if (super.Send(so)) {
			Schedule(service, SessionId, millisecondsTimeout);
			return true;
		}

		// 发送失败，一般是连接失效，此时删除上下文。
		// 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
		// 恢复最初的语义吧：如果ctx已经被并发的Remove，也就是被处理了，这里返回true。
		return !service.RemoveRpcContext(SessionId, this);
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

		ResponseHandle = responseHandle;
		Timeout = millisecondsTimeout;
		IsTimeout = false;
		IsRequest = true;
		SessionId = service.AddRpcContext(this);
		super.Send(so);
		Schedule(service, SessionId, millisecondsTimeout);
	}

	public final TaskCompletionSource<TResult> SendForWait(AsyncSocket so) {
		return SendForWait(so, 5000);
	}

	public final TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout) {
		Future = new TaskCompletionSource<>();
		if (!Send(so, null, millisecondsTimeout))
			Future.TrySetException(new IllegalStateException("Send Failed."));
		return Future;
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
		if (SendResultDone) {
			logger.error("Rpc.SendResult Already Done: " + getSender() + " " + this, new Exception());
			return;
		}
		SendResultDone = true;
		ResultEncoded = result;
		IsRequest = false;
		if (!super.Send(getSender()))
			logger.warn("Rpc.SendResult Failed: {} {}", getSender(), this);
	}

	@Override
	public boolean trySendResultCode(long code) {
		if (SendResultDone)
			return false;
		setResultCode(code);
		SendResult(null);
		return true;
	}

	@Override
	public <P extends Protocol<?>> void Dispatch(Service service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Throwable {
		if (IsRequest) {
			@SuppressWarnings("unchecked") P proto = (P)this;
			service.DispatchProtocol(proto, factoryHandle);
			return;
		}

		// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
		Rpc<TArgument, TResult> context = service.RemoveRpcContext(SessionId);
		if (context == null) {
			logger.warn("rpc response: lost context, maybe timeout. {}", this);
			return;
		}

		context.setSender(getSender());
		context.setUserState(getUserState());
		context.setResultCode(getResultCode());
		context.Result = Result;
		context.IsTimeout = false; // not need
		context.IsRequest = false;

		if (context.Future != null) {
			context.Future.SetResult(context.Result);
			return; // SendForWait，设置结果唤醒等待者。
		}
		if (context.ResponseHandle != null)
			service.DispatchRpcResponse(context, context.ResponseHandle, factoryHandle);
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBool(IsRequest);
		bb.WriteLong(SessionId);
		bb.WriteLong(getResultCode());

		if (IsRequest)
			Argument.Encode(bb);
		else if (ResultEncoded != null)
			bb.Append(ResultEncoded.InternalGetBytesUnsafe(), ResultEncoded.getOffset(), ResultEncoded.size());
		else
			Result.Encode(bb);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		IsRequest = bb.ReadBool();
		SessionId = bb.ReadLong();
		setResultCode(bb.ReadLong());

		(IsRequest ? Argument : Result).Decode(bb);
	}

	@Override
	public int getPreAllocSize() {
		return 1 + 9 + 9 + (IsRequest ? Argument.getPreAllocSize() : Result.getPreAllocSize());
	}

	@Override
	public void setPreAllocSize(int size) {
		(IsRequest ? Argument : Result).setPreAllocSize(size - 1 - 1 - 1);
	}

	@Override
	public String toString() {
		return String.format("%s IsRequest=%b SessionId=%d ResultCode=%d%n\tArgument=%s%n\tResult=%s",
				getClass().getName(), IsRequest, SessionId, getResultCode(), Argument, Result);
	}
}
