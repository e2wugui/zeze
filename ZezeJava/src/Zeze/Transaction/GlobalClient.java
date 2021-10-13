package Zeze.Transaction;

import Zeze.Net.*;
import Zeze.Services.*;
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
		
		var agent = (GlobalAgent.Agent)so.getUserState();
		if (agent.getLoginedTimes().get() > 1) {
			var relogin = new GlobalCacheManager.ReLogin();
			relogin.Argument.ServerId = getZeze().getConfig().getServerId();
			relogin.Argument.GlobalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			relogin.Send(so, (ThisRpc) -> {
						if (relogin.isTimeout()) {
							agent.getLogined().TrySetException(new RuntimeException("GloalAgent.ReLogin Timeout"));
							;
						}
						else if (relogin.getResultCode() != 0) {
							agent.getLogined().TrySetException(new RuntimeException(String.format("GlobalAgent.ReLogoin Error %1$s", relogin.getResultCode())));
						}
						else {
							agent.getLoginedTimes().incrementAndGet();
							agent.getLogined().SetResult(so);
						}
						return 0;
			});
		}
		else {
			var login = new GlobalCacheManager.Login();
			login.Argument.ServerId = getZeze().getConfig().getServerId();
			login.Argument.GlobalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			login.Send(so, (ThisRpc) -> {
						if (login.isTimeout()) {
							agent.getLogined().TrySetException(new RuntimeException("GloalAgent.Login Timeout"));
							;
						}
						else if (login.getResultCode() != 0) {
							agent.getLogined().TrySetException(new RuntimeException(String.format("GlobalAgent.Logoin Error %1$s", login.getResultCode())));
						}
						else {
							agent.getLoginedTimes().incrementAndGet();
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
		if (null != factoryHandle.Handle) {
			agent.getZeze().__GetInternalThreadPoolUnsafe().execute(
					() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p));
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) {
		super.OnSocketClose(so, e);
		Object tempVar = so.getUserState();
		var agent = tempVar instanceof GlobalAgent.Agent ? (GlobalAgent.Agent)tempVar : null;
		if (null == e) {
			e = new RuntimeException("Peer Normal Close.");
		}
		agent.OnSocketClose(this, e);
	}
}