package Zezex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LinkdService extends LinkdServiceBase {
	private static final Logger logger = LogManager.getLogger(LinkdService.class);

	public LinkdService(Zeze.Application zeze) {
		super(zeze);
	}

	public void ReportError(long linkSid, int from, int code, String desc) {
		var link = this.GetSocket(linkSid);
		if (null != link) {
			var error = new Zezex.Linkd.ReportError();
			error.Argument.setFrom(from);
			error.Argument.setCode(code);
			error.Argument.setDesc(desc);
			error.Send(link);

			switch (from) {
				case Zezex.Linkd.BReportError.FromLink:
					switch (code) {
						case Zezex.Linkd.BReportError.CodeNoProvider:
							// 没有服务时，不断开连接，允许客户端重试。
							return;
					}
					break;

				case Zezex.Linkd.BReportError.FromProvider:
					break;
			}
			// 延迟关闭。等待客户端收到错误以后主动关闭，或者超时。
			Zeze.Util.Task.schedule((ThisTask) -> {
				var so = this.GetSocket(linkSid);
				if (so != null)
					so.close();
			}, 2000, -1);
		}
	}

	@Override
	public void DispatchUnknownProtocol(Zeze.Net.AsyncSocket so, int type, Zeze.Serialize.ByteBuffer data) {
		var linkSession = (LinkSession)so.getUserState();
		if (null == linkSession || linkSession.getAccount().isEmpty()) {
			ReportError(
					so.getSessionId(),
					Zezex.Linkd.BReportError.FromLink,
					Zezex.Linkd.BReportError.CodeNotAuthed,
					"not authed."
					);
			return;
		}

		var moduleId = Zeze.Net.Protocol.GetModuleId(type);
		var dispatch = new Zezex.Provider.Dispatch();
		dispatch.Argument.setLinkSid(so.getSessionId());
		dispatch.Argument.setAccount(linkSession.getAccount());
		dispatch.Argument.setProtocolType(type);
		dispatch.Argument.setProtocolData(new Zeze.Net.Binary(data));
		dispatch.Argument.getStates().addAll(linkSession.getUserStates());
		dispatch.Argument.setStatex(linkSession.getUserStatex());

		var provider = new Zeze.Util.OutObject<Long>();
		if (linkSession.TryGetProvider(moduleId, provider)) {
			var socket = App.getInstance().ProviderService.GetSocket(provider.Value);
			if (null != socket) {
				socket.Send(dispatch);
				return;
			}
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
			//linkSession.UnBind(so, moduleId, null);
		}

		if (App.getInstance().Zezex_Provider.ChoiceProviderAndBind(moduleId, so, provider)) {
			var providerSocket = App.getInstance().ProviderService.GetSocket(provider.Value);
			if (null != providerSocket) {
				// ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				providerSocket.Send(dispatch);
				return;
			}
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		ReportError(so.getSessionId(), Zezex.Linkd.BReportError.FromLink, Zezex.Linkd.BReportError.CodeNoProvider, "no provider.");
	}

	@Override
	public void DispatchProtocol(Zeze.Net.Protocol p, ProtocolFactoryHandle factoryHandle) {
		if (null != factoryHandle.Handle) {
			try {
				var isRequestSaved = p.isRequest();
				int result = factoryHandle.Handle.handle(p); // 不启用新的Task，直接在io-thread里面执行。
				Zeze.Util.Task.LogAndStatistics(result, p, isRequestSaved);
			}
			catch (RuntimeException ex) {
				p.Sender.Close(ex); // link 在异常时关闭连接。
			}
		}
		else {
			logger.log(getSocketOptions().getSocketLogLevel(), "Protocol Handle Not Found. {}", p);
			p.Sender.Close(null);
		}
	}

	@Override
	public void OnHandshakeDone(Zeze.Net.AsyncSocket sender) {
		super.OnHandshakeDone(sender);
		sender.setUserState(new LinkSession(sender.getSessionId()));
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, Throwable e) {
		super.OnSocketClose(so, e);
		if (so.getUserState() != null) {
			((Zezex.LinkSession)so.getUserState()).OnClose();
		}
	}
}