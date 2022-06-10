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
			logger.debug("GlobalClient Send ReLogin: {}", reLogin.Argument);
			reLogin.Send(so, (ThisRpc) -> {
				logger.debug("GlobalClient Recv Login. isTimeout={}, resultCode={}",
						reLogin.isTimeout(), reLogin.getResultCode());
				if (reLogin.isTimeout()) {
					so.close();
				} else if (reLogin.getResultCode() != 0) {
					// 清理本地已经分配的记录锁。
					// 1. 关闭网络。下面两行有点重复，就这样了。
					so.Close(new Exception("GlobalAgent.ReLogin Fail code=" + reLogin.getResultCode()));
					so.getConnector().Stop();
					// 2. 开始清理，由守护线程保护，必须成功。
					agent.startRelease(getZeze(), agent.getGlobalCacheManagerHashIndex(), () -> {
						// 3. 重置登录次数，下一次连接成功，会发送Login。
						agent.getLoginTimes().getAndSet(0);
						// 4. 开始网络连接。
						so.getConnector().Start();
					});
				}
				else {
					agent.getLoginTimes().incrementAndGet();
					agent.setActiveTime(System.currentTimeMillis());
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		}
		else {
			var login = new Login();
			login.Argument.ServerId = getZeze().getConfig().getServerId();
			login.Argument.GlobalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			logger.debug("GlobalClient Send Login: {}", login.Argument);
			login.Send(so, (ThisRpc) -> {
				logger.debug("GlobalClient Recv Login. isTimeout={}, resultCode={}",
						login.isTimeout(), login.getResultCode());
				if (login.isTimeout() || login.getResultCode() != 0) {
					so.close();
				}
				else {
					agent.setActiveTime(System.currentTimeMillis());
					agent.getLoginTimes().incrementAndGet();
					agent.initialize(login.Result.MaxNetPing, login.Result.ServerProcessTime, login.Result.ServerReleaseTimeout);
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
}
