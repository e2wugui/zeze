package Zeze.Transaction;

import Zeze.Net.*;
import Zeze.*;
import Zeze.Services.GlobalCacheManager.*;

public final class GlobalClient extends Zeze.Net.Service {
	public GlobalClient(GlobalAgent agent, Application zeze) throws Throwable {
		super(agent.getZeze().getSolutionName() + ".GlobalClient", zeze);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) {
		// HandshakeDone 在 login|reLogin 完成以后才设置。

		var agent = (GlobalAgent.Agent)so.getUserState();
		if (agent.getLoginTimes().get() > 0) {
			var reLogin = new ReLogin();
			reLogin.Argument.ServerId = getZeze().getConfig().getServerId();
			reLogin.Argument.GlobalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			reLogin.Send(so, (ThisRpc) -> {
				if (reLogin.isTimeout() || reLogin.getResultCode() != 0) {
					so.close();
				}
				else {
					agent.getLoginTimes().incrementAndGet();
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		}
		else {
			var login = new Login();
			login.Argument.ServerId = getZeze().getConfig().getServerId();
			login.Argument.GlobalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			login.Send(so, (ThisRpc) -> {
				if (login.isTimeout() || login.getResultCode() != 0) {
					so.close();
				}
				else {
					agent.getLoginTimes().incrementAndGet();
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		}
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
		// Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
		if (null != factoryHandle.Handle) {
			getZeze().__GetInternalThreadPoolUnsafe().execute(
					() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p));
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		super.OnSocketClose(so, e);
		var agent = (GlobalAgent.Agent)so.getUserState();
		if (null == e) {
			e = new RuntimeException("Peer Normal Close.");
		}
		agent.OnSocketClose(this, e);
	}
}
