package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public abstract class Rpc<TArgument, TResult> : Protocol<TArgument> where TArgument: Zeze.Transaction.Bean, new() where TResult: Zeze.Transaction.Bean, new()
public abstract class Rpc<TArgument extends Zeze.Transaction.Bean, TResult extends Zeze.Transaction.Bean> extends Protocol<TArgument> {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private TResult Result = new TResult();
	public final TResult getResult() {
		return Result;
	}
	public final void setResult(TResult value) {
		Result = value;
	}

	private boolean IsTimeout;
	public final boolean isTimeout() {
		return IsTimeout;
	}
	private void setTimeout(boolean value) {
		IsTimeout = value;
	}
	private long SessionId;
	public final long getSessionId() {
		return SessionId;
	}
	public final void setSessionId(long value) {
		SessionId = value;
	}

	private tangible.Func1Param<Protocol, Integer> ResponseHandle;
	public final tangible.Func1Param<Protocol, Integer> getResponseHandle() {
		return ResponseHandle;
	}
	public final void setResponseHandle(tangible.Func1Param<Protocol, Integer> value) {
		ResponseHandle = value;
	}
	private int Timeout = 5000;
	public final int getTimeout() {
		return Timeout;
	}
	public final void setTimeout(int value) {
		Timeout = value;
	}

	private TaskCompletionSource<TResult> Future;
	public final TaskCompletionSource<TResult> getFuture() {
		return Future;
	}
	private void setFuture(TaskCompletionSource<TResult> value) {
		Future = value;
	}

	public Rpc() {
		this.setTimeout(false);
	}

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
		return Send(so, getResponseHandle(), getTimeout());
	}

	private Util.SchedulerTask Schedule(Service service, long sessionId, int millisecondsTimeout) {
		return Zeze.Util.Scheduler.getInstance().Schedule((ThisTask) -> {
					Rpc<TArgument, TResult> context = service.<Rpc<TArgument, TResult>>RemoveRpcContext(sessionId);
					if (null == context) { // 一般来说，此时结果已经返回。
						return;
					}

					context.setTimeout(true);
					context.setResultCode(Zeze.Transaction.Procedure.Timeout);

					if (null != context.getFuture()) {
						context.getFuture().TrySetException(new RpcTimeoutException());
					}
					else {
						if (this.getResponseHandle() != null) {
							this.getResponseHandle().Invoke(context);
						}
					}
		}, millisecondsTimeout, -1);
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

	public final boolean Send(AsyncSocket so, Func<Protocol, Integer> responseHandle) {
		return Send(so, responseHandle, 5000);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public bool Send(AsyncSocket so, Func<Protocol, int> responseHandle, int millisecondsTimeout = 5000)
	public final boolean Send(AsyncSocket so, tangible.Func1Param<Protocol, Integer> responseHandle, int millisecondsTimeout) {
		if (so == null || so.getService() == null) {
			return false;
		}

		this.setRequest(true);
		this.setResponseHandle(::responseHandle);
		this.setTimeout(millisecondsTimeout);
		this.setSessionId(so.getService().AddRpcContext(this));
		if (super.getUniqueRequestId() == 0) {
			super.setUniqueRequestId(this.getSessionId());
		}

		var timeoutTask = Schedule(so.getService(), getSessionId(), millisecondsTimeout);

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
		Rpc<TArgument, TResult> context = so.getService().<Rpc<TArgument, TResult>>RemoveRpcContext(this.getSessionId());
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

	public final void SendReturnVoid(Service service, AsyncSocket so, Func<Protocol, Integer> responseHandle) {
		SendReturnVoid(service, so, responseHandle, 5000);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendReturnVoid(Service service, AsyncSocket so, Func<Protocol, int> responseHandle, int millisecondsTimeout = 5000)
	public final void SendReturnVoid(Service service, AsyncSocket so, tangible.Func1Param<Protocol, Integer> responseHandle, int millisecondsTimeout) {
		if (null != so && so.getService() != service) {
			throw new RuntimeException("so.Service != service");
		}

		this.setRequest(true);
		this.setResponseHandle(::responseHandle);
		this.setTimeout(millisecondsTimeout);
		this.setSessionId(service.AddRpcContext(this));
		if (super.getUniqueRequestId() == 0) {
			super.setUniqueRequestId(this.getSessionId());
		}
		Schedule(service, getSessionId(), millisecondsTimeout);
		super.Send(so);
	}


	public final TaskCompletionSource<TResult> SendForWait(AsyncSocket so) {
		return SendForWait(so, 5000);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout = 5000)
	public final TaskCompletionSource<TResult> SendForWait(AsyncSocket so, int millisecondsTimeout) {
		setFuture(new TaskCompletionSource<TResult>());
		if (false == Send(so, null, millisecondsTimeout)) {
			getFuture().SetException(new RuntimeException("Send Failed."));
		}
		return getFuture();
	}

	// 使用异步方式实现的同步等待版本

	public final void SendAndWaitCheckResultCode(AsyncSocket so) {
		SendAndWaitCheckResultCode(so, 5000);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void SendAndWaitCheckResultCode(AsyncSocket so, int millisecondsTimeout = 5000)
	public final void SendAndWaitCheckResultCode(AsyncSocket so, int millisecondsTimeout) {
		var tmpFuture = new TaskCompletionSource<Integer>();
		if (false == Send(so, (_) -> {
			if (isTimeout()) {
				tmpFuture.TrySetException(new RpcTimeoutException(String.format("RpcTimeout %1$s", this)));
			}
			else if (getResultCode() != 0) {
				tmpFuture.TrySetException(new RuntimeException(String.format("Rpc Invalid ResultCode=%1$s %2$s", getResultCode(), this)));
			}
			else {
				tmpFuture.SetResult(0);
			}
			return Zeze.Transaction.Procedure.Success;
		}, millisecondsTimeout)) {
			throw new RuntimeException("Send Failed.");
		}
		tmpFuture.Task.Wait();
	}
	private boolean SendResultDone = false; // XXX ugly

	public final void SendResult() {
		if (SendResultDone) {
			return;
		}
		SendResultDone = true;

		setRequest(false);
		super.Send(getSender());
	}

	@Override
	public void SendResultCode(int code) {
		if (SendResultDone) {
			return;
		}
		SendResultDone = true;

		setResultCode(code);
		setRequest(false);
		super.Send(getSender());
	}

	@Override
	public void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle) {
		if (isRequest()) {
			service.DispatchProtocol(this, factoryHandle);
			return;
		}

		// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
		Rpc<TArgument, TResult> context = service.<Rpc<TArgument, TResult>>RemoveRpcContext(getSessionId());
		if (null == context) {
			logger.Info("rpc response: lost context, maybe timeout. {0}", this);
			return;
		}

		context.setRequest(false);
		context.setResult(getResult());
		context.setSender(getSender());
		context.setResultCode(getResultCode());
		context.setUserState(getUserState());

		if (context.getFuture() != null) {
			context.getFuture().SetResult(context.getResult());
			return; // SendForWait，设置结果唤醒等待者。
		}
		context.setTimeout(false); // not need
		if (null != context.getResponseHandle()) {
			service.DispatchRpcResponse(context, context.getResponseHandle(), factoryHandle);
		}
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setRequest(bb.ReadBool());
		setSessionId(bb.ReadLong());
		setResultCode(bb.ReadInt());
		setUniqueRequestId(bb.ReadLong());

		if (isRequest()) {
			getArgument().Decode(bb);
		}
		else {
			getResult().Decode(bb);
		}
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBool(isRequest());
		bb.WriteLong(getSessionId());
		bb.WriteInt(getResultCode());
		bb.WriteLong(getUniqueRequestId());

		if (isRequest()) {
			getArgument().Encode(bb);
		}
		else {
			getResult().Encode(bb);
		}
	}

	@Override
	public String toString() {
		return String.format("%1$s SessionId=%2$s UniqueRequestId=%3$s ResultCode=%4$s%5$s\tArgument=%6$s%7$s\tResult=%8$s", this.getClass().getName(), getSessionId(), getUniqueRequestId(), getResultCode(), System.lineSeparator(), getArgument(), System.lineSeparator(), getResult());
	}
}
