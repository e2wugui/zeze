package Zeze.Transaction;

import Zeze.Net.*;
import Zeze.Services.*;
import NLog.*;
import Zeze.*;

public final class GlobalClient extends Zeze.Net.Service {
	private GlobalAgent agent;

	public GlobalClient(GlobalAgent agent, Application zeze) {
		super(String.format("%1$s.GlobalClient", agent.getZeze().getSolutionName()), zeze);
		this.agent = agent;
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) {
		super.OnHandshakeDone(so);
		Object tempVar = so.getUserState();
		var agent = tempVar instanceof GlobalAgent.Agent ? (GlobalAgent.Agent)tempVar : null;
		if (agent.getLoginedTimes().Get() > 1) {
			var relogin = new GlobalCacheManager.ReLogin();
			relogin.getArgument().setServerId(getZeze().getConfig().getServerId());
			relogin.getArgument().setGlobalCacheManagerHashIndex(agent.getGlobalCacheManagerHashIndex());
			relogin.Send(so, (_) -> {
						if (relogin.isTimeout()) {
							agent.getLogined().TrySetException(new RuntimeException("GloalAgent.ReLogin Timeout"));
							;
						}
						else if (relogin.getResultCode() != 0) {
							agent.getLogined().TrySetException(new RuntimeException(String.format("GlobalAgent.ReLogoin Error %1$s", relogin.getResultCode())));
						}
						else {
							agent.getLoginedTimes().IncrementAndGet();
							agent.getLogined().SetResult(so);
						}
						return 0;
			});
		}
		else {
			var login = new GlobalCacheManager.Login();
			login.getArgument().setServerId(getZeze().getConfig().getServerId());
			login.getArgument().setGlobalCacheManagerHashIndex(agent.getGlobalCacheManagerHashIndex());
			login.Send(so, (_) -> {
						if (login.isTimeout()) {
							agent.getLogined().TrySetException(new RuntimeException("GloalAgent.Login Timeout"));
							;
						}
						else if (login.getResultCode() != 0) {
							agent.getLogined().TrySetException(new RuntimeException(String.format("GlobalAgent.Logoin Error %1$s", login.getResultCode())));
						}
						else {
							agent.getLoginedTimes().IncrementAndGet();
							agent.getLogined().SetResult(so);
						}
						return 0;
			});
		}
	}

	@Override
	public void OnSocketConnectError(AsyncSocket so, RuntimeException e) {
		super.OnSocketConnectError(so, e);
		Object tempVar = so.getUserState();
		var agent = tempVar instanceof GlobalAgent.Agent ? (GlobalAgent.Agent)tempVar : null;
		if (null == e) {
			e = new RuntimeException("Normal Connect Error???"); // ConnectError 应该 e != null 吧，懒得确认了。
		}
		agent.getLogined().TrySetException(e);
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		// Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
		if (null != factoryHandle.getHandle()) {
			agent.getZeze().InternalThreadPool.QueueUserWorkItem(() -> Util.Task.Call(() -> factoryHandle.Handle(p), p));
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, RuntimeException e) {
		super.OnSocketClose(so, e);
		Object tempVar = so.getUserState();
		var agent = tempVar instanceof GlobalAgent.Agent ? (GlobalAgent.Agent)tempVar : null;
		if (null == e) {
			e = new RuntimeException("Peer Normal Close.");
		}
		agent.OnSocketClose(this, e);
	}
}