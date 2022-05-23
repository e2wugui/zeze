package Zeze.Arch;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.ConcurrentLruLike;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Builtin.LinkdBase.*;
import Zeze.Builtin.Provider.*;

public class LinkdService extends Zeze.Services.HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdService.class);

	public LinkdApp LinkdApp;

	public LinkdService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
		StableLinkSids = new ConcurrentLruLike<>(1000000, this::TryLruRemove);
	}

	public void ReportError(long linkSid, int from, int code, String desc) {
		var link = this.GetSocket(linkSid);
		if (null != link) {
			var error = new ReportError();
			error.Argument.setFrom(from);
			error.Argument.setCode(code);
			error.Argument.setDesc(desc);
			error.Send(link);

			switch (from) {
				case BReportError.FromLink:
					//noinspection SwitchStatementWithTooFewBranches
					switch (code) {
						case BReportError.CodeNoProvider:
							// 没有服务时，不断开连接，允许客户端重试。
							return;
					}
					break;

				case BReportError.FromProvider:
					break;
			}
			// 延迟关闭。等待客户端收到错误以后主动关闭，或者超时。
			Zeze.Util.Task.schedule(2000, () -> {
				var so = this.GetSocket(linkSid);
				if (so != null)
					so.close();
			});
		}
	}

	static class StableLinkSidKey
	{
		// 同一个账号同一个ClientId只允许一个登录。
		// ClientId 可能的分配方式：每个手机Client分配一个，所有电脑Client分配一个。
		public final String Account;
		public final String ClientId;

		public StableLinkSidKey(String account, String clientId)
		{
			Account = account;
			ClientId = clientId;
		}

		@Override
		public int hashCode() {
			final int _prime_ = 31;
			int _h_ = 0;
			_h_ = _h_ * _prime_ + Account.hashCode();
			_h_ = _h_ * _prime_ + ClientId.hashCode();
			return _h_;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof StableLinkSidKey) {
				var other = (StableLinkSidKey)obj;
				return Account.equals(other.Account) && ClientId.equals(other.ClientId);
			}
			return false;
		}
	}

	public static class StableLinkSid {
		public boolean Removed = false;
		public long LinkSid;
		public AsyncSocket AuthedSocket;
	}

	private final ConcurrentLruLike<StableLinkSidKey, StableLinkSid> StableLinkSids;

	private boolean TryLruRemove(StableLinkSidKey key, StableLinkSid value) {
		var exist = StableLinkSids.remove(key);
		if (null != exist) {
			exist.Removed = true;
		}
		return true;
	}

	private void SetStableLinkSid(String account, String clientId, AsyncSocket client) {
		var key = new StableLinkSidKey(account, clientId);
		while (true) {
			var stable = StableLinkSids.GetOrAdd(key, StableLinkSid::new);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (stable) {
				if (stable.Removed)
					continue;

				if (stable.AuthedSocket == client) // same client
					return;

				// Must Close Before Reuse LinkSid
				if (null != stable.AuthedSocket)
					stable.AuthedSocket.Close(null);
				if (stable.LinkSid != 0) {
					// Reuse Old LinkSid
					client.setSessionId(stable.LinkSid);
				} else {
					// first client
					stable.LinkSid = client.getSessionId();
				}
				stable.AuthedSocket = client;
				//(client.UserState as LinkSession).StableLinkSid = stable;
			}
		}
	}

	@Override
	public void DispatchUnknownProtocol(Zeze.Net.AsyncSocket so, int moduleId, int protocolId, Zeze.Serialize.ByteBuffer data) {
		var linkSession = (LinkdUserSession)so.getUserState();
		if (null == linkSession || false == linkSession.isAuthed()) {
			ReportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
			return;
		}

		if (moduleId == Zeze.Game.Online.ModuleId && protocolId == Zeze.Builtin.Game.Online.Login.ProtocolId_) {
			var login = new Zeze.Builtin.Game.Online.Login();
			login.Decode(Zeze.Serialize.ByteBuffer.Wrap(data));
			SetStableLinkSid(linkSession.getAccount(), String.valueOf(login.Argument.getRoleId()), so);
		} else if (moduleId == Zeze.Arch.Online.ModuleId && protocolId == Zeze.Builtin.Online.Login.ProtocolId_) {
			var login = new Zeze.Builtin.Online.Login();
			login.Decode(Zeze.Serialize.ByteBuffer.Wrap(data));
			SetStableLinkSid(linkSession.getAccount(), login.Argument.getClientId(), so);
		}

		var dispatch = new Dispatch();
		dispatch.Argument.setLinkSid(so.getSessionId());
		dispatch.Argument.setAccount(linkSession.getAccount());
		dispatch.Argument.setProtocolType(Protocol.MakeTypeId(moduleId, protocolId));
		dispatch.Argument.setProtocolData(new Zeze.Net.Binary(data.Copy()));
		dispatch.Argument.setContext(linkSession.getContext());
		dispatch.Argument.setContextx(linkSession.getContextx());

		var provider = new Zeze.Util.OutLong();
		if (linkSession.TryGetProvider(moduleId, provider)) {
			var socket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
			if (null != socket) {
				socket.Send(dispatch);
				return;
			}
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
			//linkSession.UnBind(so, moduleId, null);
		}

		if (LinkdApp.LinkdProvider.ChoiceProviderAndBind(moduleId, so, provider)) {
			var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
			if (null != providerSocket) {
				// ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				providerSocket.Send(dispatch);
				return;
			}
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		ReportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider, "no provider.");
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, Service.ProtocolFactoryHandle<P> factoryHandle) {
		if (null != factoryHandle.Handle) {
			try {
				var isRequestSaved = p.isRequest();
				var result = factoryHandle.Handle.handle(p); // 不启用新的Task，直接在io-thread里面执行。
				Zeze.Util.Task.LogAndStatistics(null, result, p, isRequestSaved);
			}
			catch (Throwable ex) {
				p.getSender().Close(ex); // link 在异常时关闭连接。
			}
		} else {
			logger.warn("Protocol Handle Not Found: {}", p);
			p.getSender().Close(null);
		}
	}

	@Override
	public void OnSocketAccept(AsyncSocket sender) throws Throwable {
		sender.setUserState(new LinkdUserSession(sender.getSessionId()));
		super.OnSocketAccept(sender);
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, Throwable e) throws Throwable {
		super.OnSocketClose(so, e);
		if (so.getUserState() != null) {
			((LinkdUserSession)so.getUserState()).OnClose(LinkdApp.LinkdProviderService);
		}
	}
}
