package Zeze.Arch;

import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.Provider.*;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.SubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;

/**
 * Linkd上处理Provider协议的模块。
 */
public class LinkdProvider extends AbstractLinkdProvider {
//	private static final Logger logger = LogManager.getLogger(LinkdProvider.class);

	public LinkdApp LinkdApp;
	public ProviderDistribute Distribute;
	private int FirstModuleWithConfigTypeDefault;

	// 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
	// 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
	// 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
	// 不建议在一个项目里面使用多个Prefix。
	private String ServerServiceNamePrefix = "";

	public LinkdProvider() {
	}

	public int getFirstModuleWithConfigTypeDefault() {
		return FirstModuleWithConfigTypeDefault;
	}

	private void setFirstModuleWithConfigTypeDefault(int value) {
		FirstModuleWithConfigTypeDefault = value;
	}

	public String getServerServiceNamePrefix() {
		return ServerServiceNamePrefix;
	}

	private void setServerServiceNamePrefix(String value) {
		ServerServiceNamePrefix = value;
	}

	public boolean ChoiceProvider(AsyncSocket link, int moduleId, Zeze.Util.Func1<AsyncSocket, Boolean> OnSend) throws Throwable {
		var linkSession = (LinkdUserSession)link.getUserState();
		var provider = new OutLong();
		if (linkSession.TryGetProvider(moduleId, provider)) {
			var socket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
			if (null != socket) {
				if (OnSend.call(socket))
					return true; // done
			}
			// 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
			// 此时应该处于 UnBind 过程中。
		}

		if (ChoiceProviderAndBind(moduleId, link, provider)) {
			var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
			if (null != providerSocket) {
				// ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
				//noinspection RedundantIfStatement
				if (OnSend.call(providerSocket))
					return true;
			}
			// else
			// 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
		}
		return false;
	}

	public boolean ChoiceHashWithoutBind(int moduleId, int hash, OutLong provider) {
		var serviceName = ProviderDistribute.MakeServiceName(getServerServiceNamePrefix(), moduleId);
		provider.Value = 0L;
		var providers = Distribute.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (providers == null)
			return false;
		return Distribute.ChoiceHash(providers, hash, provider);
	}

	public boolean ChoiceProviderAndBind(int moduleId, AsyncSocket link, OutLong provider) {
		var serviceName = ProviderDistribute.MakeServiceName(getServerServiceNamePrefix(), moduleId);
		var linkSession = (LinkdUserSession)link.getUserState();
		provider.Value = 0L;
		var providers = Distribute.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (providers == null)
			return false;

		// 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
		// 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
		// 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
		var providerModuleState = (ProviderModuleState)providers.getSubscribeInfo().getLocalState();
		switch (providerModuleState.ChoiceType) {
		case BModule.ChoiceTypeHashAccount:
			if (!Distribute.ChoiceHash(providers, ByteBuffer.calc_hashnr(linkSession.getAccount()), provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeHashRoleId:
			var roleId = linkSession.getRoleId();
			if (roleId == null || !Distribute.ChoiceHash(providers, ByteBuffer.calc_hashnr(roleId), provider))
				return false;
			break; // bind static later

		case BModule.ChoiceTypeFeedFullOneByOne:
			if (!Distribute.ChoiceFeedFullOneByOne(providers, provider))
				return false;
			break; // bind static later

		default:
			if (!Distribute.ChoiceLoad(providers, provider))
				return false;
			break; // bind static later
		}

		// 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
		var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
		var providerSession = (LinkdProviderSession)providerSocket.getUserState();
		linkSession.Bind(LinkdApp.LinkdProviderService, link, providerSession.getStaticBinds().keySet(), providerSocket);
		return true;
	}

	public void OnProviderClose(AsyncSocket provider) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		if (providerSession == null)
			return;

		// unbind module
		UnBindModules(provider, providerSession.getStaticBinds().keySet(), true);
		providerSession.getStaticBinds().clear();

		// unbind LinkSession
		var linkSessionIds = providerSession.getLinkSessionIds();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (linkSessionIds) {
			for (var it = linkSessionIds.iterator(); it.moveToNext(); ) {
				for (var it2 = it.value().iterator(); it.moveToNext(); ) {
					var link = LinkdApp.LinkdService.GetSocket(it2.value());
					if (link != null) {
						var linkSession = (LinkdUserSession)link.getUserState();
						if (linkSession != null) {
							linkSession.UnBind(LinkdApp.LinkdProviderService, link, it.key(), provider, true);
						}
					}
				}
			}
			linkSessionIds.clear();
		}
	}

	@Override
	public long ProcessBindRequest(Bind rpc) {
		if (rpc.Argument.getLinkSids().isEmpty()) {
			var providerSession = (LinkdProviderSession)rpc.getSender().getUserState();
			for (var module : rpc.Argument.getModules().entrySet()) {
				if (getFirstModuleWithConfigTypeDefault() == 0 && module.getValue().getConfigType() == BModule.ConfigTypeDefault) {
					setFirstModuleWithConfigTypeDefault(module.getValue().getConfigType());
				}
				var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
						module.getKey(), module.getValue().getChoiceType(), module.getValue().getConfigType());
				var serviceName = ProviderDistribute.MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), module.getKey());
				var subState = Distribute.Zeze.getServiceManagerAgent().SubscribeService(
						serviceName, SubscribeInfo.SubscribeTypeSimple, providerModuleState);
				// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
				// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
				subState.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), providerModuleState);
				providerSession.getStaticBinds().putIfAbsent(module.getKey(), module.getKey());
			}
		} else {
			// 动态绑定
			for (var linkSid : rpc.Argument.getLinkSids()) {
				var link = LinkdApp.LinkdService.GetSocket(linkSid);
				if (link != null) {
					var linkSession = (LinkdUserSession)link.getUserState();
					linkSession.Bind(LinkdApp.LinkdProviderService, link, rpc.Argument.getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe rpc) {
		var providerSession = (LinkdProviderSession)rpc.getSender().getUserState();
		for (var module : rpc.Argument.getModules().entrySet()) {
			var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
					module.getKey(), module.getValue().getChoiceType(), module.getValue().getConfigType());
			var serviceName = ProviderDistribute.MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), module.getKey());
			var subState = Distribute.Zeze.getServiceManagerAgent().SubscribeService(
					serviceName, module.getValue().getSubscribeType(), providerModuleState);
			// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
			// 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
			subState.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), providerModuleState);
		}

		rpc.SendResult();
		return Procedure.Success;
	}

	private void UnBindModules(AsyncSocket provider, Iterable<Integer> modules) {
		UnBindModules(provider, modules, false);
	}

	private void UnBindModules(AsyncSocket provider, Iterable<Integer> modules, boolean isOnProviderClose) {
		var providerSession = (LinkdProviderSession)provider.getUserState();
		for (var moduleId : modules) {
			if (!isOnProviderClose) {
				providerSession.getStaticBinds().remove(moduleId);
			}
			var serviceName = ProviderDistribute.MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), moduleId);
			var volatileProviders = Distribute.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
			if (volatileProviders == null)
				continue;
			// UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
			// 这里仅仅设置该moduleId对应的服务的状态不可用。
			volatileProviders.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), null);
		}
	}

	@Override
	protected long ProcessUnBindRequest(UnBind rpc) {
		if (rpc.Argument.getLinkSids().size() == 0) {
			UnBindModules(rpc.getSender(), rpc.Argument.getModules().keySet());
		} else {
			// 动态绑定
			for (var linkSid : rpc.Argument.getLinkSids()) {
				var link = LinkdApp.LinkdService.GetSocket(linkSid);
				if (link != null) {
					var linkSession = (LinkdUserSession)link.getUserState();
					linkSession.UnBind(LinkdApp.LinkdProviderService, link, rpc.Argument.getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSend(Send protocol) {
		var ptype = protocol.Argument.getProtocolType();
		var pdata = protocol.Argument.getProtocolWholeData();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
			AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "SENT[{}]: {}:{} [{}]", protocol.Argument.getLinkSids(),
					Protocol.GetModuleId(ptype), Protocol.GetProtocolId(ptype), pdata.size());
		}
		for (var linkSid : protocol.Argument.getLinkSids()) {
			var link = LinkdApp.LinkdService.GetSocket(linkSid);
			// ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
			if (link != null) {
				link.Send(pdata);
			}
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessBroadcast(Broadcast protocol) throws Throwable {
		var ptype = protocol.Argument.getProtocolType();
		var pdata = protocol.Argument.getProtocolWholeData();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
			AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "BROC[{}]: {}:{} [{}]", LinkdApp.LinkdService.getSocketCount(),
					Protocol.GetModuleId(ptype), Protocol.GetProtocolId(ptype), pdata.size());
		}
		LinkdApp.LinkdService.Foreach((socket) -> {
			// auth 通过就允许发送广播。
			// 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
			// 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
			var linkSession = (LinkdUserSession)socket.getUserState();
			if (linkSession != null && linkSession.getAccount() == null && !linkSession.getContext().isEmpty()) {
				socket.Send(pdata);
			}
		});
		return Procedure.Success;
	}

	@Override
	protected long ProcessKick(Kick protocol) {
		LinkdApp.LinkdService.ReportError(
				protocol.Argument.getLinksid(),
				BReportError.FromProvider,
				protocol.Argument.getCode(),
				protocol.Argument.getDesc());
		return Procedure.Success;
	}

	@Override
	protected long ProcessSetUserState(SetUserState protocol) {
		var socket = LinkdApp.LinkdService.GetSocket(protocol.Argument.getLinkSid());
		if (null != socket) {
			var linkSession = (LinkdUserSession)socket.getUserState();
			if (linkSession != null) {
				linkSession.SetUserState(protocol.Argument.getContext(), protocol.Argument.getContextx());
				return Procedure.Success;
			}
		}
		return Procedure.Unknown;
	}

	@Override
	protected long ProcessAnnounceProviderInfo(AnnounceProviderInfo protocol) {
		var session = (LinkdProviderSession)protocol.getSender().getUserState();
		session.setInfo(protocol.Argument);
		setServerServiceNamePrefix(protocol.Argument.getServiceNamePrefix());
		session.ServerLoadIp = protocol.Argument.getProviderDirectIp();
		session.ServerLoadPort = protocol.Argument.getProviderDirectPort();
		LinkdApp.LinkdProviderService.ProviderSessions.put(session.getServerLoadName(), session);

		return Zeze.Transaction.Procedure.Success;
	}
}
