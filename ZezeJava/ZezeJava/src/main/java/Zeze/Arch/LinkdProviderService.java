package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdProviderService extends Zeze.Services.HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdProviderService.class);

	public LinkdApp LinkdApp;
	public final ConcurrentHashMap<String, ProviderSession> ProviderSessions = new ConcurrentHashMap<>();

	public LinkdProviderService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	// 重载需要的方法。
	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, Service.ProtocolFactoryHandle<P> factoryHandle) {
		if (null != factoryHandle.Handle) {
			if (p.getTypeId() == Bind.TypeId_) {
				// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
				// 不要直接在io-thread里面执行。
				Task.run(() -> factoryHandle.Handle.handle(p), p);
			} else {
				// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
				// 直接执行，少一次线程切换。
				try {
					var isRequestSaved = p.isRequest();
					var result = factoryHandle.Handle.handle(p);
					Task.LogAndStatistics(null, result, p, isRequestSaved);
				} catch (Throwable ex) {
					logger.error("Protocol.Handle Exception: " + p, ex);
				}
			}
		} else
			logger.warn("Protocol Handle Not Found: {}", p);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		sender.setUserState(new LinkdProviderSession(sender.getSessionId()));
		super.OnHandshakeDone(sender);

		var announce = new AnnounceLinkInfo();
		sender.Send(announce);
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		// 先unbind。这样避免有时间窗口。
		LinkdApp.LinkdProvider.OnProviderClose(so);
		super.OnSocketClose(so, e);
	}
}
