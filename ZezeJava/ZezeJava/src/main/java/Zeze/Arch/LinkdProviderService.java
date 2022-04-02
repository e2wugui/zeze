package Zeze.Arch;

import Zeze.Net.Protocol;
import Zeze.Net.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Beans.Provider.*;

public class LinkdProviderService extends Zeze.Services.HandshakeServer {
	public ProviderLinkd ProviderLinkd;

	public LinkdProviderService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	private static final Logger logger = LogManager.getLogger(LinkdProviderService.class);

	// 重载需要的方法。
	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, Service.ProtocolFactoryHandle<P> factoryHandle) {
		if (null != factoryHandle.Handle) {
			if (p.getTypeId() == Bind.TypeId_) {
				// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
				// 不要直接在io-thread里面执行。
				Zeze.Util.Task.run(() -> factoryHandle.Handle.handle(p), p);
			}
			else {
				// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
				// 直接执行，少一次线程切换。
				try {
					var isRequestSaved = p.isRequest();
					var result = factoryHandle.Handle.handle(p);
					Zeze.Util.Task.LogAndStatistics(null, result, p, isRequestSaved);
				} catch (Throwable ex) {
					logger.error("Protocol.Handle Exception: " + p, ex);
				}
			}
		} else
			logger.warn("Protocol Handle Not Found: {}", p);
	}

	@Override
	public void OnHandshakeDone(Zeze.Net.AsyncSocket sender) throws Throwable {
		sender.setUserState(new LinkdProviderSession(sender.getSessionId()));
		super.OnHandshakeDone(sender);

		var announce = new AnnounceLinkInfo();
		announce.Argument.setLinkId(0); // reserve
		announce.Argument.setProviderSessionId(sender.getSessionId());
		sender.Send(announce);
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, Throwable e) throws Throwable {
		// 先unbind。这样避免有时间窗口。
		ProviderLinkd.OnProviderClose(so);
		super.OnSocketClose(so, e);
	}
}
