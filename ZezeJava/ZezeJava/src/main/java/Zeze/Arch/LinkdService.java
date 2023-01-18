package Zeze.Arch;

import java.net.ServerSocket;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.LinkdBase.ReportError;
import Zeze.Builtin.Provider.BDispatch;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdService extends Zeze.Services.HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdService.class);

	private static final class StableLinkSidKey {
		// 同一个账号同一个ClientId只允许一个登录。
		// ClientId 可能的分配方式：每个手机Client分配一个，所有电脑Client分配一个。
		public final String account;
		public final String clientId;

		public StableLinkSidKey(String account, String clientId) {
			this.account = account;
			this.clientId = clientId;
		}

		@Override
		public int hashCode() {
			final int _prime_ = 31;
			int _h_ = 0;
			_h_ = _h_ * _prime_ + account.hashCode();
			_h_ = _h_ * _prime_ + clientId.hashCode();
			return _h_;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof StableLinkSidKey) {
				var other = (StableLinkSidKey)obj;
				return account.equals(other.account) && clientId.equals(other.clientId);
			}
			return false;
		}
	}

	private static final class StableLinkSid {
		public boolean removed;
		public long linkSid;
		public AsyncSocket authedSocket;
	}

	protected LinkdApp linkdApp;
	protected ConcurrentLruLike<StableLinkSidKey, StableLinkSid> stableLinkSids;

	public LinkdService(String name, Zeze.Application zeze) {
		super(name, zeze);
	}

	@Override
	public void start() throws Exception {
		stableLinkSids = new ConcurrentLruLike<>(getName(), 1_000_000, this::tryLruRemove);
		super.start();
	}

	private void reportError(Dispatch dispatch) {
		// 如果是 rpc.request 直接返回Procedure.Busy错误。
		// see Zeze.Net.Rpc.decode/encode
		var bb = ByteBuffer.Wrap(dispatch.Argument.getProtocolData());
		var compress = bb.ReadInt();
		var familyClass = compress & FamilyClass.FamilyClassMask;
		var isRequest = familyClass == FamilyClass.Request;
		var so = GetSocket(dispatch.Argument.getLinkSid());
		if (isRequest && so != null) {
			if ((compress & FamilyClass.BitResultCode) != 0)
				bb.ReadLong();
			var sessionId = bb.ReadLong();
			// argument 忽略，必须要解析出来，也不知道是什么。

			// 开始响应rpc.response.
			// 【注意】复用了上面的变量 bb，compress。
			bb = ByteBuffer.Allocate(12);
			bb.WriteInt(FamilyClass.Response | FamilyClass.BitResultCode);
			bb.WriteLong(Procedure.Busy);
			bb.WriteLong(sessionId);
			EmptyBean.instance.encode(bb); // emptyBean对应任意bean的默认值状态。
			so.Send(bb);
		}
		// 报告服务器繁忙，但不关闭连接。
		reportError(dispatch.Argument.getLinkSid(), BReportError.FromLink, BReportError.CodeProviderBusy, "provider is busy.", false);
	}

	public void reportError(long linkSid, int from, int code, String desc) {
		reportError(linkSid, from, code, desc, true);
	}

	private void reportError(long linkSid, int from, int code, String desc, boolean closeLink) {
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
			if (closeLink) {
				Task.schedule(2000, () -> {
					var so = GetSocket(linkSid);
					if (so != null)
						so.closeGracefully();
				});
			}
		}
	}

	private boolean tryLruRemove(StableLinkSidKey key, StableLinkSid value) {
		var exist = stableLinkSids.remove(key);
		if (exist != null)
			exist.removed = true;
		return true;
	}

	private void setStableLinkSid(String account, String clientId, AsyncSocket client) {
		var key = new StableLinkSidKey(account, clientId);
		while (true) {
			var stable = stableLinkSids.getOrAdd(key, StableLinkSid::new);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (stable) {
				if (stable.removed)
					continue;

				if (stable.authedSocket == client) // same client
					return;

				// Must Close Before Reuse LinkSid
				if (stable.authedSocket != null)
					stable.authedSocket.close();
				if (stable.linkSid != 0) {
					// Reuse Old LinkSid
					client.setSessionId(stable.linkSid);
				} else {
					// first client
					stable.linkSid = client.getSessionId();
				}
				stable.authedSocket = client;
				//(client.UserState as LinkSession).StableLinkSid = stable;
			}
		}
	}

	public LinkdUserSession getAuthedSession(AsyncSocket socket) {
		var linkSession = (LinkdUserSession)socket.getUserState();
		if (linkSession == null || !linkSession.isAuthed()) {
			reportError(socket.getSessionId(), BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
			return null;
		}
		return linkSession;
	}

	public void setStableLinkSid(LinkdUserSession linkSession, AsyncSocket so,
								 int moduleId, int protocolId, ByteBuffer data) {
		var typeId = Protocol.makeTypeId(moduleId, protocolId);
		if (typeId == Zeze.Builtin.Game.Online.Login.TypeId_) {
			var login = new Zeze.Builtin.Game.Online.Login();
			var beginIndex = data.ReadIndex;
			login.decode(data);
			data.ReadIndex = beginIndex;
			setStableLinkSid(linkSession.getAccount(), String.valueOf(login.Argument.getRoleId()), so);
		} else if (typeId == Zeze.Builtin.Online.Login.TypeId_) {
			var login = new Zeze.Builtin.Online.Login();
			var beginIndex = data.ReadIndex;
			login.decode(data);
			data.ReadIndex = beginIndex;
			setStableLinkSid(linkSession.getAccount(), login.Argument.getClientId(), so);
		}
	}

	// 注意这里为了优化拷贝开销,返回的Dispatch引用了参数data中的byte数组,调用者要确保Dispatch用完之前不能修改data数据,否则应该传入data.Copy()
	public static Dispatch createDispatch(LinkdUserSession linkSession, AsyncSocket so,
										  int moduleId, int protocolId, ByteBuffer data) {
		return new Dispatch(new BDispatch(so.getSessionId(), linkSession.getAccount(),
				Protocol.makeTypeId(moduleId, protocolId), new Binary(data),
				linkSession.getContext(), linkSession.getContextx()));
	}

	private boolean tryReportError(LinkdUserSession linkSession, int moduleId, Dispatch dispatch) {
		var pms = linkdApp.linkdProvider.getProviderModuleState(moduleId);
		if (null == pms)
			return false;
		if (pms.configType == BModule.ConfigTypeDynamic) {
			reportError(linkSession.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider,
					"no provider: " + moduleId + ", " + dispatch.getProtocolId());
			// 此后断开连接，不再继续搜索，返回true
			return true;
		}
		return false;
	}

	public boolean findSend(LinkdUserSession linkSession, int moduleId, Dispatch dispatch) {
		var providerSessionId = linkSession.tryGetProvider(moduleId);
		if (providerSessionId != null) {
			var socket = linkdApp.linkdProviderService.GetSocket(providerSessionId);
			if (socket == null)
				return tryReportError(linkSession, moduleId, dispatch);

			var ps = (LinkdProviderSession)socket.getUserState();
			if (ps.load.getOverload() == BLoad.eOverload) {
				// 过载时会直接拒绝请求以及报告错误。
				reportError(dispatch);
				// 但是不能继续派发了。所以这里返回true，表示处理完成。
				return true;
			}

			if (socket.Send(dispatch))
				return true;

			return tryReportError(linkSession, moduleId, dispatch);
		}
		return false;
	}

	public boolean choiceBindSend(AsyncSocket so, int moduleId, Dispatch dispatch) {
		var provider = new OutLong();
		if (linkdApp.linkdProvider.choiceProviderAndBind(moduleId, so, provider)) {
			var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
			if (providerSocket == null)
				return false;

			var ps = (LinkdProviderSession)providerSocket.getUserState();
			if (ps.load.getOverload() == BLoad.eOverload) {
				// 过载时会直接拒绝请求以及报告错误。
				reportError(dispatch);
				// 但是不能继续派发了。所以这里返回true，表示处理完成。
				return true;
			}

			// ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
			return providerSocket.Send(dispatch);
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		return false;
	}

	@Override
	public void dispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) {
		var linkSession = getAuthedSession(so);
		setStableLinkSid(linkSession, so, moduleId, protocolId, data);
		var dispatch = createDispatch(linkSession, so, moduleId, protocolId, data);
		if (findSend(linkSession, moduleId, dispatch))
			return;
		if (choiceBindSend(so, moduleId, dispatch))
			return;
		reportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider,
				"no provider: " + moduleId + ", " + protocolId);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, Service.ProtocolFactoryHandle<P> factoryHandle) {
		if (factoryHandle.Handle != null) {
			try {
				var isRequestSaved = p.isRequest();
				var result = factoryHandle.Handle.handle(p); // 不启用新的Task，直接在io-thread里面执行。
				Task.logAndStatistics(null, result, p, isRequestSaved);
			} catch (Exception ex) {
				p.getSender().close(ex); // link 在异常时关闭连接。
			}
		} else {
			logger.warn("Protocol Handle Not Found: {}", p);
			p.getSender().close();
		}
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	@Override
	public void OnSocketAccept(AsyncSocket sender) throws Exception {
		sender.setUserState(new LinkdUserSession(sender.getSessionId()));
		super.OnSocketAccept(sender);
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
		super.OnSocketClose(so, e);
		if (so.getUserState() != null)
			((LinkdUserSession)so.getUserState()).onClose(linkdApp.linkdProviderService);
	}

	@Override
	public void onServerSocketBind(ServerSocket ss) {
		// 需要LinkdService实现自己的查询服务器，在这里把实际绑定的地址和端口注册到名字服务器。
		try {
			if (linkdApp.onServerSocketBindAction != null)
				linkdApp.onServerSocketBindAction.run(ss);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
