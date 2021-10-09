package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class Agent {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private RaftConfig RaftConfig;
	public RaftConfig getRaftConfig() {
		return RaftConfig;
	}
	private void setRaftConfig(RaftConfig value) {
		RaftConfig = value;
	}
	private NetClient Client;
	public NetClient getClient() {
		return Client;
	}
	private void setClient(NetClient value) {
		Client = value;
	}
	public String getName() {
		return getClient().getName();
	}

	public ConnectorEx getLeader() {
		return _Leader;
	}

	private volatile ConnectorEx _Leader;

	private Util.IdentityHashMap<Protocol, Protocol> NotAutoResend = new Util.IdentityHashMap<Protocol, Protocol> ();
	private Util.IdentityHashMap<Protocol, Protocol> getNotAutoResend() {
		return NotAutoResend;
	}

	private Util.IdentityHashMap<Protocol, Protocol> Pending = new Util.IdentityHashMap<Protocol, Protocol> ();
	private Util.IdentityHashMap<Protocol, Protocol> getPending() {
		return Pending;
	}

	private tangible.Action2Param<Agent, tangible.Action0Param> OnLeaderChanged;
	public tangible.Action2Param<Agent, tangible.Action0Param> getOnLeaderChanged() {
		return OnLeaderChanged;
	}
	private void setOnLeaderChanged(tangible.Action2Param<Agent, tangible.Action0Param> value) {
		OnLeaderChanged = value;
	}

	/** 
	 发送Rpc请求。
	 如果 autoResend == true，那么总是返回成功。内部会在需要的时候重发请求。
	 如果 autoResend == false，那么返回结果表示是否成功。
	 
	 <typeparam name="TArgument"></typeparam>
	 <typeparam name="TResult"></typeparam>
	 @param rpc
	 @param handle
	 @param autoResend
	 @param timeout
	 @return 
	*/

	public <TArgument extends Bean, TResult extends Bean> boolean Send(Rpc<TArgument, TResult> rpc, Func<Protocol, Integer> handle, boolean autoResend) {
		return Send(rpc, handle, autoResend, -1);
	}

	public <TArgument extends Bean, TResult extends Bean> boolean Send(Rpc<TArgument, TResult> rpc, Func<Protocol, Integer> handle) {
		return Send(rpc, handle, true, -1);
	}

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public bool Send<TArgument, TResult>(Rpc<TArgument, TResult> rpc, Func<Protocol, int> handle, bool autoResend = true, int timeout = -1) where TArgument : Bean, new() where TResult : Bean, new()
//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
	public <TArgument extends Bean, TResult extends Bean> boolean Send(Rpc<TArgument, TResult> rpc, tangible.Func1Param<Protocol, Integer> handle, boolean autoResend, int timeout) {
		if (timeout < 0) {
			timeout = getRaftConfig().getAppendEntriesTimeout() + 1000;
		}

		if (autoResend) {
			var tmp = _Leader;
			if (null != tmp && tmp.isHandshakeDone() && rpc.Send(tmp.getSocket(), handle, timeout)) {
				return true;
			}

			rpc.setResponseHandle(::handle);
			rpc.setTimeout(timeout);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			getPending().TryAdd(rpc, rpc);
			return true;
		}

		// 记录不要自动发送的请求。
		getNotAutoResend().put(rpc, rpc);
		return rpc.Send(_Leader == null ? null : _Leader.getSocket(), (p) -> {
				V _;
				tangible.OutObject<V> tempOut__ = new tangible.OutObject<V>();
				getNotAutoResend().TryRemove(p, tempOut__);
				_ = tempOut__.outArgValue;
				return handle.invoke(p);
		}, timeout);
	}

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: private int SendForWaitHandle<TArgument, TResult>(TaskCompletionSource<Rpc<TArgument, TResult>> future, Rpc<TArgument, TResult> rpc) where TArgument : Bean, new() where TResult : Bean, new()
	private <TArgument extends Bean, TResult extends Bean> int SendForWaitHandle(TaskCompletionSource<Rpc<TArgument, TResult>> future, Rpc<TArgument, TResult> rpc) {
		if (rpc.isTimeout()) {
			future.TrySetException(new RpcTimeoutException("RaftRpcTimeout"));
		}
		else {
			future.SetResult(rpc);
		}
		return Procedure.Success;
	}


	public <TArgument extends Bean, TResult extends Bean> TaskCompletionSource<Rpc<TArgument, TResult>> SendForWait(Rpc<TArgument, TResult> rpc, boolean autoResend) {
		return SendForWait(rpc, autoResend, -1);
	}

	public <TArgument extends Bean, TResult extends Bean> TaskCompletionSource<Rpc<TArgument, TResult>> SendForWait(Rpc<TArgument, TResult> rpc) {
		return SendForWait(rpc, true, -1);
	}

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public TaskCompletionSource<Rpc<TArgument, TResult>> SendForWait<TArgument, TResult>(Rpc<TArgument, TResult> rpc, bool autoResend = true, int timeout = -1) where TArgument : Bean, new() where TResult : Bean, new()
//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
	public <TArgument extends Bean, TResult extends Bean> TaskCompletionSource<Rpc<TArgument, TResult>> SendForWait(Rpc<TArgument, TResult> rpc, boolean autoResend, int timeout) {
		if (timeout < 0) {
			timeout = getRaftConfig().getAppendEntriesTimeout() + 1000;
		}

		var future = new TaskCompletionSource<Rpc<TArgument, TResult>>();
		if (autoResend) {
			var tmp = _Leader;
			if (null != tmp && tmp.isHandshakeDone() && rpc.Send(tmp.getSocket(), (p) -> SendForWaitHandle(future, rpc), timeout)) {
				return future;
			}

			rpc.setResponseHandle((Protocol p) -> SendForWaitHandle(future, rpc));
			rpc.setTimeout(timeout);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			getPending().TryAdd(rpc, rpc);
			return future;
		}

		// 记录不要自动发送的请求。
		getNotAutoResend().put(rpc, rpc);
		if (false == rpc.Send(_Leader == null ? null : _Leader.getSocket(), (p) -> {
			V _;
			tangible.OutObject<V> tempOut__ = new tangible.OutObject<V>();
			getNotAutoResend().TryRemove(p, tempOut__);
			_ = tempOut__.outArgValue;
			return SendForWaitHandle(future, rpc);
		}, timeout)) {
			future.TrySetException(new RuntimeException("Send Failed."));
		};
		return future;
	}