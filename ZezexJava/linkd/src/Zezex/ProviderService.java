package Zezex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ThrowableAttributeConverter;

public final class ProviderService extends ProviderServiceBase {
	public ProviderService(Zeze.Application zeze) {
		super(zeze);
	}

	private static final Logger logger = LogManager.getLogger(ProviderService.class);

	// 重载需要的方法。
	@Override
	public void DispatchProtocol(Zeze.Net.Protocol p, ProtocolFactoryHandle factoryHandle) {
		if (null != factoryHandle.Handle) {
			if (p.getTypeId() == Zezex.Provider.Bind.TypeId_) {
				// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
				// 不要直接在io-thread里面执行。
				Zeze.Util.Task.Run(() -> factoryHandle.Handle.handle(p), p);
			}
			else {
				// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
				// 直接执行，少一次线程切换。
				try {
					var isReqeustSaved = p.isRequest();
					int result = factoryHandle.Handle.handle(p);
					Zeze.Util.Task.LogAndStatistics(result, p, isReqeustSaved);
				}
				catch (RuntimeException ex) {
					logger.log(getSocketOptions().getSocketLogLevel(), () -> "Protocol.Handle. " + p, ex);
				}
			}
		}
		else {
			logger.log(getSocketOptions().getSocketLogLevel(), () -> "Protocol Handle Not Found. " + p);
		}
	}

	@Override
	public void OnHandshakeDone(Zeze.Net.AsyncSocket sender) {
		super.OnHandshakeDone(sender);
		sender.setUserState(new ProviderSession(sender.getSessionId()));

		var announce = new Zezex.Provider.AnnounceLinkInfo();
		announce.Argument.setLinkId(0); // reserve
		announce.Argument.setProviderSessionId(sender.getSessionId());
		sender.Send(announce);
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, Throwable e) {
		// 先unbind。这样避免有时间窗口。
		Zezex.App.getInstance().Zezex_Provider.OnProviderClose(so);
		super.OnSocketClose(so, e);
	}
}