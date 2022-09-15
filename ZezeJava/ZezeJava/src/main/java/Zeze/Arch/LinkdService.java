package Zeze.Arch;

import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.LinkdBase.ReportError;
import Zeze.Builtin.Provider.BDispatch;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import Zeze.Web.AbstractWeb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdService extends Zeze.Services.HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdService.class);

	private static final class StableLinkSidKey {
		// 同一个账号同一个ClientId只允许一个登录。
		// ClientId 可能的分配方式：每个手机Client分配一个，所有电脑Client分配一个。
		public final String Account;
		public final String ClientId;

		public StableLinkSidKey(String account, String clientId) {
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

	private static final class StableLinkSid {
		public boolean Removed;
		public long LinkSid;
		public AsyncSocket AuthedSocket;
	}

	protected LinkdApp LinkdApp;
	protected ConcurrentLruLike<StableLinkSidKey, StableLinkSid> StableLinkSids;

	public LinkdService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	@Override
	public void Start() throws Throwable {
		StableLinkSids = new ConcurrentLruLike<>(getName(), 1_000_000, this::TryLruRemove);
		super.Start();
	}

	public void ReportError(long linkSid, int from, int code, String desc) {
		var link = GetSocket(linkSid);
		if (link != null) {
			new ReportError(new BReportError(from, code, desc)).Send(link);

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
			// 虽然使用了写完关闭(CloseGracefully)方法，但是等待一下，尽量让客户端主动关闭，有利于减少 TCP_TIME_WAIT?
			Task.schedule(2000, () -> {
				var so = GetSocket(linkSid);
				if (so != null)
					so.closeGracefully();
			});
		}
	}

	private boolean TryLruRemove(StableLinkSidKey key, StableLinkSid value) {
		var exist = StableLinkSids.remove(key);
		if (exist != null)
			exist.Removed = true;
		return true;
	}

	private void SetStableLinkSid(String account, String clientId, AsyncSocket client) {
		var key = new StableLinkSidKey(account, clientId);
		while (true) {
			var stable = StableLinkSids.getOrAdd(key, StableLinkSid::new);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (stable) {
				if (stable.Removed)
					continue;

				if (stable.AuthedSocket == client) // same client
					return;

				// Must Close Before Reuse LinkSid
				if (stable.AuthedSocket != null)
					stable.AuthedSocket.close();
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

	public LinkdUserSession getAuthedSession(AsyncSocket socket) {
		var linkSession = (LinkdUserSession)socket.getUserState();
		if (linkSession == null || !linkSession.isAuthed()) {
			ReportError(socket.getSessionId(), BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
			return null;
		}
		return linkSession;
	}

	public void setStableLinkSid(LinkdUserSession linkSession, AsyncSocket so,
								 int moduleId, int protocolId, ByteBuffer data) {
		var typeId = Protocol.MakeTypeId(moduleId, protocolId);
		if (typeId == Zeze.Builtin.Game.Online.Login.TypeId_) {
			var login = new Zeze.Builtin.Game.Online.Login();
			var beginIndex = data.ReadIndex;
			login.decode(data);
			data.ReadIndex = beginIndex;
			SetStableLinkSid(linkSession.getAccount(), String.valueOf(login.Argument.getRoleId()), so);
		} else if (typeId == Zeze.Builtin.Online.Login.TypeId_) {
			var login = new Zeze.Builtin.Online.Login();
			var beginIndex = data.ReadIndex;
			login.decode(data);
			data.ReadIndex = beginIndex;
			SetStableLinkSid(linkSession.getAccount(), login.Argument.getClientId(), so);
		}
	}

	// 注意这里为了优化拷贝开销,返回的Dispatch引用了参数data中的byte数组,调用者要确保Dispatch用完之前不能修改data数据,否则应该传入data.Copy()
	public static Dispatch createDispatch(LinkdUserSession linkSession, AsyncSocket so,
										  int moduleId, int protocolId, ByteBuffer data) {
		return new Dispatch(new BDispatch(so.getSessionId(), linkSession.getAccount(),
				Protocol.MakeTypeId(moduleId, protocolId), new Binary(data),
				linkSession.getContext(), linkSession.getContextx()));
	}

	public boolean findSend(LinkdUserSession linkSession, int moduleId, Dispatch dispatch) {
		var providerSessionId = linkSession.TryGetProvider(moduleId);
		if (providerSessionId != null) {
			var socket = LinkdApp.LinkdProviderService.GetSocket(providerSessionId);
			if (socket != null)
				return socket.Send(dispatch);
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
			//linkSession.UnBind(so, moduleId, null);
		}
		return false;
	}

	public boolean choiceBindSend(AsyncSocket so, int moduleId, Dispatch dispatch) {
		var provider = new OutLong();
		if (LinkdApp.LinkdProvider.ChoiceProviderAndBind(moduleId, so, provider)) {
			var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
			if (providerSocket != null) {
				// ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				return providerSocket.Send(dispatch);
			}
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		return false;
	}

	@Override
	public void dispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) {
		if (moduleId == AbstractWeb.ModuleId) {
			ReportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider,
					"not a public provider: " + moduleId);
			return;
		}
		var linkSession = getAuthedSession(so);
		setStableLinkSid(linkSession, so, moduleId, protocolId, data);
		var dispatch = createDispatch(linkSession, so, moduleId, protocolId, data);
		if (findSend(linkSession, moduleId, dispatch))
			return;
		if (choiceBindSend(so, moduleId, dispatch))
			return;
		ReportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider,
				"no provider: " + moduleId + ", " + protocolId);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, Service.ProtocolFactoryHandle<P> factoryHandle) {
		if (factoryHandle.Handle != null) {
			try {
				var isRequestSaved = p.isRequest();
				var result = factoryHandle.Handle.handle(p); // 不启用新的Task，直接在io-thread里面执行。
				Task.LogAndStatistics(null, result, p, isRequestSaved);
			} catch (Throwable ex) {
				p.getSender().close(ex); // link 在异常时关闭连接。
			}
		} else {
			logger.warn("Protocol Handle Not Found: {}", p);
			p.getSender().close();
		}
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Throwable {
		Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	@Override
	public void OnSocketAccept(AsyncSocket sender) throws Throwable {
		sender.setUserState(new LinkdUserSession(sender.getSessionId()));
		super.OnSocketAccept(sender);
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		super.OnSocketClose(so, e);
		if (so.getUserState() != null)
			((LinkdUserSession)so.getUserState()).OnClose(LinkdApp.LinkdProviderService);
	}
}
