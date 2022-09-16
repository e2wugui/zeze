package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdProviderService extends Zeze.Services.HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdProviderService.class);

	protected LinkdApp linkdApp;
	protected final ConcurrentHashMap<String, ProviderSession> providerSessions = new ConcurrentHashMap<>();

	public LinkdProviderService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) {
		Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	// 重载需要的方法。
	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, Service.ProtocolFactoryHandle<P> factoryHandle) {
		if (factoryHandle.Handle != null) {
			if (p.getTypeId() == Bind.TypeId_ || p.getTypeId() == Subscribe.TypeId_) {
				// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
				// 不要直接在io-thread里面执行。
				Task.runUnsafe(() -> factoryHandle.Handle.handle(p), p, null, null, factoryHandle.Mode);
			} else {
				// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
				// 直接执行，少一次线程切换。
				try {
					var isRequestSaved = p.isRequest();
					var result = factoryHandle.Handle.handle(p);
					Task.logAndStatistics(null, result, p, isRequestSaved);
				} catch (Throwable ex) {
					logger.error("Protocol.Handle Exception: {}", p, ex);
				}
			}
		} else
			logger.warn("Protocol Handle Not Found: {}", p);
	}

	@Override
	public void OnSocketAccept(AsyncSocket sender) throws Throwable {
		sender.setUserState(new LinkdProviderSession(sender.getSessionId()));
		super.OnSocketAccept(sender);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);

		var announce = new AnnounceLinkInfo();
		sender.Send(announce);
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		// 先unbind。这样避免有时间窗口。
		linkdApp.linkdProvider.onProviderClose(so);
		super.OnSocketClose(so, e);
	}
}
