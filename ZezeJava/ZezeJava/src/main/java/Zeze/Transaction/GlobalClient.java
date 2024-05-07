package Zeze.Transaction;

import java.io.IOException;
import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class GlobalClient extends Service {
	private static final IOException loginException = new IOException("login failed");
	private static final IOException reloginTimeoutException = new IOException("relogin timeout");

	public GlobalClient(GlobalAgent agent, Application zeze) {
		super(agent.getZeze().getProjectName() + ".GlobalClient", zeze);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) {
		// HandshakeDone 在 login|reLogin 完成以后才设置。

		var agent = (GlobalAgent.Agent)so.getUserState();
		if (agent.getLoginTimes().get() > 0) {
			var reLogin = new ReLogin();
			reLogin.Argument.serverId = getZeze().getConfig().getServerId();
			reLogin.Argument.globalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			logger.debug("GlobalClient Send ReLogin: {}", reLogin.Argument);
			reLogin.Send(so, (ThisRpc) -> {
				if (reLogin.isTimeout()) {
					so.close(reloginTimeoutException);
				} else if (reLogin.getResultCode() != 0) {
					// 清理本地已经分配的记录锁。
					// 1. 关闭网络。下面两行有点重复，就这样了。
					so.close(new IOException("GlobalAgent.ReLogin Fail code=" + reLogin.getResultCode()));
					//noinspection DataFlowIssue
					so.getConnector().stop();
					// 2. 开始清理，由守护线程保护，必须成功。
					agent.startRelease(getZeze(), () -> {
						// 3. 重置登录次数，下一次连接成功，会发送Login。
						agent.getLoginTimes().getAndSet(0);
						// 4. 开始网络连接。
						so.getConnector().start();
					});
				} else {
					logger.debug("GlobalClient Recv Login 1");
					agent.getLoginTimes().getAndIncrement();
					agent.setActiveTime(System.currentTimeMillis());
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		} else {
			var login = new Login();
			login.Argument.serverId = getZeze().getConfig().getServerId();
			login.Argument.globalCacheManagerHashIndex = agent.getGlobalCacheManagerHashIndex();
			login.Argument.debugMode = Reflect.inDebugMode;
			logger.debug("GlobalClient Send Login: {}", login.Argument);
			login.Send(so, (ThisRpc) -> {
				if (login.isTimeout() || login.getResultCode() != 0) {
					logger.error("GlobalClient Recv Login. isTimeout={}, resultCode={}",
							login.isTimeout(), login.getResultCode());
					so.close(loginException);
				} else {
					logger.debug("GlobalClient Recv Login 2");
					agent.setActiveTime(System.currentTimeMillis());
					agent.getLoginTimes().getAndIncrement();
					agent.initialize(login.Result.maxNetPing,
							login.Result.serverProcessTime, login.Result.serverReleaseTimeout);
					super.OnHandshakeDone(so);
				}
				return 0;
			});
		}
	}

	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
		// 不支持事务
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		// Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
		Task.getCriticalThreadPool().execute(() -> Task.call(() -> p.handle(this, factoryHandle), p));
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
															@NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		// global rpc 没有异步调用，仅仅future.setResult。直接io线程调用。
		try {
			responseHandle.handle(rpc);
		} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
			logger.error("Agent.NetClient.dispatchRpcResponse", e);
		}
	}

}
