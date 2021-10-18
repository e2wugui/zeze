package Zeze.Net;

import Zeze.Serialize.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Rpc<TArgument extends Zeze.Transaction.Bean, TResult extends Zeze.Transaction.Bean>
	extends Protocol1<TArgument> {
	private static final Logger logger = LogManager.getLogger(Rpc.class);

	public TResult Result;

	private boolean IsTimeout = false;
	public boolean isTimeout() {
		return IsTimeout;
	}

	public long SessionId;
	public long getSessionId() {
		return SessionId;
	}
	public void setSessionId(long sessionId) {
		SessionId = sessionId;
	}

	private ProtocolHandle ResponseHandle;
	public ProtocolHandle getResponseHandle() {
		return ResponseHandle;
	}
	public void setResponseHandle(ProtocolHandle handle) {
		ResponseHandle = handle;
	}

	private int Timeout = 5000;
	public int getTimeout() {
		return Timeout;
	}
	public void setTimeout(int timeout) {
		Timeout = timeout;
	}

	private Zeze.Util.TaskCompletionSource<TResult> Future;

	/** 
	 使用当前 rpc 中设置的参数发送。
	 总是建立上下文，总是返回true。
	 这个方法是 Protocol 的重载。
	 用于不需要处理结果的请求
	 或者重新发送已经设置过 ResponseHandle 等的请求。
	 
	 @param so
	 @return 
	*/
	@Override
	public boolean Send(AsyncSocket so) {
		return Send(so, ResponseHandle, Timeout);
	}

	private Zeze.Util.Task Schedule(Service service, long sessionId, int millisecondsTimeout) {
		return Zeze.Util.Task.schedule((ThisTask) -> {
					Rpc<TArgument, TResult> context = service.<Rpc<TArgument, TResult>>RemoveRpcContext(sessionId);
					if (null == context) { // 一般来说，此时结果已经返回。
						return;
					}

					context.IsTimeout = true;
					context.setResultCode(Zeze.Transaction.Procedure.Timeout);

					if (null != context.Future) {
						context.Future.TrySetException(new RpcTimeoutException());
					}
					else {
						if (this.ResponseHandle != null) {
							this.ResponseHandle.handle(context);
						}
					}
		}, millisecondsTimeout, millisecondsTimeout);
	}

	/** 
	 异步发送rpc请求。
	 1. 如果返回true，表示请求已经发送，并且建立好了上下文。
	 2. 如果返回false，请求没有发送成功，上下文也没有保留。
	 
	 @param so
	 @param responseHandle
	 @param millisecondsTimeout
	 @return 
	*/

	public final boolean Send(AsyncSocket so, ProtocolHandle responseHandle) {
		return Send(so, responseHandle, 5000);
	}

	public final boolean Send(AsyncSocket so, ProtocolHandle responseHandle, int millisecondsTimeout) {
		if (so == null || so.getService() == null) {
			return false;
		}

		this.setRequest(true);
		this.ResponseHandle = responseHandle;
		this.Timeout = millisecondsTimeout;
		this.SessionId = so.getService().AddRpcContext(this);
		if (super.getUniqueRequestId() == 0) {
			super.setUniqueRequestId(this.SessionId);
		}

		var timeoutTask = Schedule(so.getService(), SessionId, millisecondsTimeout);

		if (super.Send(so)) {
			return true;
		}

		// 发送失败，一般是连接失效，此时删除上下文。
		// 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
		// Cancel不是必要的。
		timeoutTask.Cancel();
		// 【注意】当上下文已经其他并发过程删除（得到了处理），那么这里就返回成功。
		// see OnSocketDisposed
		// 这里返回 false 表示真的没有发送成功，外面根据自己需要决定是否重连并再次发送。
		Rpc<TArgument, TResult> context = so.getService().<Rpc<TArgument, TResult>>RemoveRpcContext(this.SessionId);
		return context == null;
	}

	/** 
	 不管发送是否成功，总是建立RpcContext。
	 连接(so)可以为null，此时Rpc请求将在Timeout后回调。
	 
	 @param service
	 @param so
	 @param responseHandle
	 @param millisecondsTimeout
	*/

	public final void SendReturnVoid(Service service, AsyncSocket so, ProtocolHandle responseHandle) {
		SendReturnVoid(service, so, responseHandle, 5000);
	}

	public final void SendReturnVoid(Service service, AsyncSocket so, ProtocolHandle responseHandle, int millisecondsTimeout) {
		if (null != so && so.getService() != service) {
			throw new RuntimeException("so.Service != service");
		}

		this.setRequest(true);
		this.ResponseHandle = responseHandle;
		this.Timeout = millisecondsTimeout;
		this.SessionId = service.AddRpcContext(this);
		if (super.getUniqueRequestId() == 0) {
			super.setUniqueRequestId(this.SessionId);
		}
		Schedule(service, SessionId, millisecondsTimeout);
		super.Send(so);
	}


	public final Zeze.Util.TaskCompletionSource<TResult> SendForWait(AsyncSocket so) {
		return SendForWait(so, 5000);
	}

	public final Zeze.Util.TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout) {
		Future = new Zeze.Util.TaskCompletionSource<TResult>();
		if (false == Send(so, null, millisecondsTimeout)) {
			Future.TrySetException(new RuntimeException("Send Failed."));
		}
		return Future;
	}

	// 使用异步方式实现的同步等待版本

	public final void SendAndWaitCheckResultCode(AsyncSocket so) {
		SendAndWaitCheckResultCode(so, 5000);
	}

	public final void SendAndWaitCheckResultCode(AsyncSocket so, int millisecondsTimeout) {
		var tmpFuture = new Zeze.Util.TaskCompletionSource<Integer>();
		if (false == Send(so,
			(rpc) -> {
				if (IsTimeout) {
					tmpFuture.TrySetException(new RpcTimeoutException(String.format("RpcTimeout %1$s", this)));
				} else if (getResultCode() != 0) {
					tmpFuture.TrySetException(new RuntimeException(String.format("Rpc Invalid ResultCode=%1$s %2$s", getResultCode(), this)));
				} else {
					tmpFuture.SetResult(0);
				}
				return Zeze.Transaction.Procedure.Success;
			}, millisecondsTimeout)) {
			throw new RuntimeException("Send Failed.");
		}
		tmpFuture.Wait();
	}
	private boolean SendResultDone = false; // XXX ugly

	public final void SendResult() {
		if (SendResultDone) {
			return;
		}
		SendResultDone = true;

		setRequest(false);
		super.Send(Sender);
	}

	@Override
	public void SendResultCode(int code) {
		if (SendResultDone) {
			return;
		}
		SendResultDone = true;

		setResultCode(code);
		setRequest(false);
		super.Send(Sender);
	}

	@Override
	public void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle) {
		if (isRequest()) {
			service.DispatchProtocol(this, factoryHandle);
			return;
		}

		// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
		Rpc<TArgument, TResult> context = service.<Rpc<TArgument, TResult>>RemoveRpcContext(SessionId);
		if (null == context) {
			logger.info("rpc response: lost context, maybe timeout. {}", this);
			return;
		}

		context.setRequest(false);
		context.Result = Result;
		context.Sender = Sender;
		context.setResultCode(getResultCode());
		context.UserState = UserState;

		if (context.Future != null) {
			context.Future.SetResult(context.Result);
			return; // SendForWait，设置结果唤醒等待者。
		}
		context.IsTimeout = false; // not need
		if (null != context.ResponseHandle) {
			service.DispatchRpcResponse(context, context.ResponseHandle, factoryHandle);
		}
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setRequest(bb.ReadBool());
		SessionId = bb.ReadLong();
		setResultCode(bb.ReadInt());
		setUniqueRequestId(bb.ReadLong());

		if (isRequest()) {
			Argument.Decode(bb);
		}
		else {
			Result.Decode(bb);
		}
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBool(isRequest());
		bb.WriteLong(SessionId);
		bb.WriteInt(getResultCode());
		bb.WriteLong(getUniqueRequestId());

		if (isRequest()) {
			Argument.Encode(bb);
		}
		else {
			Result.Encode(bb);
		}
	}

	@Override
	public String toString() {
		return String.format("%1$s SessionId=%2$s UniqueRequestId=%3$s ResultCode=%4$s%5$s\tArgument=%6$s%7$s\tResult=%8$s", this.getClass().getName(), SessionId, getUniqueRequestId(), getResultCode(), System.lineSeparator(), Argument, System.lineSeparator(), Result);
	}
}
