package Zezex;

import Zeze.Net.Protocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ProviderService extends ProviderServiceBase {
	public ProviderService(Zeze.Application zeze) throws Throwable {
		super(zeze);
	}

	private static final Logger logger = LogManager.getLogger(ProviderService.class);

	// 重载需要的方法。
	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
		if (null != factoryHandle.Handle) {
			if (p.getTypeId() == Zezex.Provider.Bind.TypeId_) {
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
		sender.setUserState(new ProviderSession(sender.getSessionId()));
		super.OnHandshakeDone(sender);

		var announce = new Zezex.Provider.AnnounceLinkInfo();
		announce.Argument.setLinkId(0); // reserve
		announce.Argument.setProviderSessionId(sender.getSessionId());
		sender.Send(announce);
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, Throwable e) throws Throwable {
		// 先unbind。这样避免有时间窗口。
		Zezex.App.getInstance().Zezex_Provider.OnProviderClose(so);
		super.OnSocketClose(so, e);
	}
}
