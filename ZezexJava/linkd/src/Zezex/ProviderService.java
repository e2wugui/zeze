package Zezex;

// auto-generated



public final class ProviderService extends Zeze.Services.HandshakeServer {
	public ProviderService(Zeze.Application zeze) {
		super("ProviderService", zeze);
	}



	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	// 重载需要的方法。
	@Override
	public void DispatchProtocol(Zeze.Net.Protocol p, ProtocolFactoryHandle factoryHandle) {
		if (null != factoryHandle.Handle) {
			if (p.TypeId == Zezex.Provider.Bind.TypeId_) {
				// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
				// 不要直接在io-thread里面执行。
				Zeze.Util.Task.Run(() -> factoryHandle.Handle(p), p);
			}
			else {
				// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
				// 直接执行，少一次线程切换。
				try {
					var isReqeustSaved = p.IsRequest;
					int result = factoryHandle.Handle(p);
					Zeze.Util.Task.LogAndStatistics(result, p, isReqeustSaved);
				}
				catch (RuntimeException ex) {
					logger.Log(getSocketOptions().SocketLogLevel, ex, "Protocol.Handle. {0}", p);
				}
			}
		}
		else {
			logger.Log(getSocketOptions().SocketLogLevel, "Protocol Handle Not Found. {0}", p);
		}
	}

	@Override
	public void OnHandshakeDone(Zeze.Net.AsyncSocket sender) {
		super.OnHandshakeDone(sender);
		sender.UserState = new ProviderSession(sender.SessionId);

		var announce = new Zezex.Provider.AnnounceLinkInfo();
		announce.getArgument().LinkId = 0; // reserve
		announce.getArgument().ProviderSessionId = sender.SessionId;
		sender.Send(announce);
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, RuntimeException e) {
		// 先unbind。这样避免有时间窗口。
		Zezex.App.getInstance().getZezexProvider().OnProviderClose(so);
		super.OnSocketClose(so, e);
	}
}