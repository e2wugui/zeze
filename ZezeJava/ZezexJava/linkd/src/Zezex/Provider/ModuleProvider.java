package Zezex.Provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.Protocol;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModuleProvider extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleProvider.class);

	@SuppressWarnings("RedundantThrows")
	public void Start(@SuppressWarnings("unused") Zezex.App app) throws Throwable {
	}

	@SuppressWarnings("RedundantThrows")
	public void Stop(@SuppressWarnings("unused") Zezex.App app) throws Throwable {
	}

	private String MakeServiceName(String serviceNamePrefix, int moduleId) {
		return serviceNamePrefix + moduleId;
	}

	public boolean ChoiceHash(Agent.SubscribeState providers, int hash, Zeze.Util.OutObject<Long> provider) {
		provider.Value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		if (list.size() == 0) {
			return false;
		}

		Object tempVar = list.get(Integer.remainderUnsigned(hash, list.size())).getLocalState();
		var providerModuleState = (ProviderModuleState)tempVar;
		if (null == providerModuleState) {
			return false;
		}

		provider.Value = providerModuleState.getSessionId();
		return true;
	}

	public boolean ChoiceLoad(Agent.SubscribeState providers, Zeze.Util.OutObject<Long> provider) {
		provider.Value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		var frees = new ArrayList<Zezex.ProviderSession>(list.size());
		var all = new ArrayList<Zezex.ProviderSession>(list.size());
		int TotalWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.size() - 1; i >= 0; --i) {
			var providerModuleState = (ProviderModuleState)list.get(i).getLocalState();
			if (null == providerModuleState) {
				continue;
			}
			// Object tempVar2 = App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			var ps = (Zezex.ProviderSession)App.LinkdProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			if (null == ps) {
				continue; // 这里发现关闭的服务，仅仅忽略.
			}
			all.add(ps);
			if (ps.getOnlineNew() > Zezex.App.Instance.getLinkConfig().getMaxOnlineNew()) {
				continue;
			}
			int weight = ps.getProposeMaxOnline() - ps.getOnline();
			if (weight <= 0) {
				continue;
			}
			frees.add(ps);
			TotalWeight += weight;
		}
		if (TotalWeight > 0) {
			int randweight = Zeze.Util.Random.getInstance().nextInt(TotalWeight);
			for (var ps : frees) {
				int weight = ps.getProposeMaxOnline() - ps.getOnline();
				if (randweight < weight) {
					provider.Value = ps.getSessionId();
					return true;
				}
				randweight -= weight;
			}
		}
		// 选择失败，一般是都满载了，随机选择一个。
		if (!all.isEmpty()) {
			provider.Value = all.get(Zeze.Util.Random.getInstance().nextInt(all.size())).getSessionId();
			return true;
		}
		// no providers
		return false;
	}

	private final AtomicInteger FeedFullOneByOneIndex = new AtomicInteger();

	public boolean ChoiceFeedFullOneByOne(Agent.SubscribeState providers, Zeze.Util.OutObject<Long> provider) {
		// 查找时增加索引，和喂饱时增加索引，需要原子化。提高并发以后慢慢想，这里应该足够快了。
		synchronized (this) {
			provider.Value = 0L;

			var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
			// 最多遍历一次。循环里面 continue 时，需要递增索引。
			for (int i = 0; i < list.size(); ++i, FeedFullOneByOneIndex.incrementAndGet()) {
				var index = Integer.remainderUnsigned(FeedFullOneByOneIndex.get(), list.size()); // current
				var serviceinfo = list.get(index);
				var providerModuleState = (ProviderModuleState)serviceinfo.getLocalState();
				if (providerModuleState == null)
					continue;
				var providerSocket = Zezex.App.Instance.LinkdProviderService.GetSocket(providerModuleState.SessionId);
				if (null == providerSocket)
					continue;
				var ps = (Zezex.ProviderSession)providerSocket.getUserState();
				// 这里发现关闭的服务，仅仅忽略.
				if (null == ps)
					continue;
				// 这个和一个一个喂饱冲突，但是一下子给一个服务分配太多用户，可能超载。如果不想让这个生效，把MaxOnlineNew设置的很大。
				if (ps.getOnlineNew() > Zezex.App.Instance.getLinkConfig().getMaxOnlineNew())
					continue;

				provider.Value = ps.getSessionId();
				if (ps.getOnline() >= ps.getProposeMaxOnline())
					FeedFullOneByOneIndex.incrementAndGet(); // 已经喂饱了一个，下一个。
				return true;
			}
			return false;
		}
	}

	public boolean ChoiceProvider(String serviceNamePrefix, int moduleId, int hash, Zeze.Util.OutObject<Long> provider) {
		var serviceName = MakeServiceName(serviceNamePrefix, moduleId);

		var volatileProviders = App.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (null == volatileProviders) {
			provider.Value = 0L;
			return false;
		}
		return ChoiceHash(volatileProviders, hash, provider);
	}

	public boolean ChoiceProviderByServerId(String serviceNamePrefix, int moduleId, int serverId, Zeze.Util.OutObject<Long> provider) {
		var serviceName = MakeServiceName(serviceNamePrefix, moduleId);

		var volatileProviders = App.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (null == volatileProviders) {
			provider.Value = 0L;
			return false;
		}
		var si = volatileProviders.getServiceInfos().findServiceInfoByServerId(serverId);
		if (null != si) {
			var state = (ProviderModuleState)si.getLocalState();
			provider.Value = state.getSessionId();
			return true;
		}
		return false;
	}

	public boolean ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, Zeze.Util.OutObject<Long> provider) {
		var serviceName = MakeServiceName(getServerServiceNamePrefix(), moduleId);
		var linkSession = (Zezex.LinkSession)link.getUserState();
		provider.Value = 0L;
		var volatileProviders = App.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (null == volatileProviders)
			return false;

		// 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
		// 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
		// 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
		var providerModuleState = (ProviderModuleState)volatileProviders.getSubscribeInfo().getLocalState();
		switch (providerModuleState.getChoiceType()) {
		case BModule.ChoiceTypeHashAccount:
			return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.getAccount()), provider);

		case BModule.ChoiceTypeHashRoleId:
			if (!linkSession.getUserStates().isEmpty()) {
				return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.getUserStates().get(0)), provider);
			} else {
				return false;
			}

		case BModule.ChoiceTypeFeedFullOneByOne:
			return ChoiceFeedFullOneByOne(volatileProviders, provider);
		}

		// default
		if (ChoiceLoad(volatileProviders, provider)) {
			// 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
			var providerSocket = App.LinkdProviderService.GetSocket(provider.Value);
			var providerSession = (Zezex.ProviderSession)providerSocket.getUserState();
			linkSession.Bind(link, providerSession.getStaticBinds().keySet(), providerSocket);
			return true;
		}

		return false;
	}

	public void OnProviderClose(Zeze.Net.AsyncSocket provider) {
		var providerSession = (Zezex.ProviderSession)provider.getUserState();
		if (null == providerSession) {
			return;
		}

		// unbind module
		UnBindModules(provider, providerSession.getStaticBinds().keySet(), true);
		providerSession.getStaticBinds().clear();

		// unbind LinkSession
		synchronized (providerSession.getLinkSessionIds()) {
			for (var e : providerSession.getLinkSessionIds().entrySet()) {
				for (var linkSid : e.getValue()) {
					var link = App.LinkdService.GetSocket(linkSid);
					if (null != link) {
						var linkSession = (Zezex.LinkSession)link.getUserState();
						if (linkSession != null) {
							linkSession.UnBind(link, e.getKey(), provider, true);
						}
					}
				}
			}
			providerSession.getLinkSessionIds().clear();
		}
	}

	private int FirstModuleWithConfigTypeDefault = 0;

	public int getFirstModuleWithConfigTypeDefault() {
		return FirstModuleWithConfigTypeDefault;
	}

	private void setFirstModuleWithConfigTypeDefault(int value) {
		FirstModuleWithConfigTypeDefault = value;
	}

	public final static class ProviderModuleState {
		private final long SessionId;

		public long getSessionId() {
			return SessionId;
		}

		private final int ModuleId;

		public int getModuleId() {
			return ModuleId;
		}

		private final int ChoiceType;

		public int getChoiceType() {
			return ChoiceType;
		}

		private final int ConfigType;

		public int getConfigType() {
			return ConfigType;
		}

		public ProviderModuleState(long sessionId, int moduleId, int choiceType, int configType) {
			SessionId = sessionId;
			ModuleId = moduleId;
			ChoiceType = choiceType;
			ConfigType = configType;
		}
	}

	@Override
	public long ProcessBindRequest(Bind rpc) throws Throwable {
		if (rpc.Argument.getLinkSids().isEmpty()) {
			var providerSession = (Zezex.ProviderSession)rpc.getSender().getUserState();
			for (var module : rpc.Argument.getModules().entrySet()) {
				if (getFirstModuleWithConfigTypeDefault() == 0 && module.getValue().getConfigType() == BModule.ConfigTypeDefault) {
					setFirstModuleWithConfigTypeDefault(module.getValue().getConfigType());
				}
				var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
						module.getKey(), module.getValue().getChoiceType(), module.getValue().getConfigType());
				var serviceName = MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), module.getKey());
				var subState = App.getServiceManagerAgent().SubscribeService(
						serviceName, Zeze.Services.ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit, providerModuleState);
				// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
				subState.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), providerModuleState);
				providerSession.getStaticBinds().putIfAbsent(module.getKey(), module.getKey());
			}
		} else {
			// 动态绑定
			for (var linkSid : rpc.Argument.getLinkSids()) {
				var link = App.LinkdService.GetSocket(linkSid);
				if (null != link) {
					var linkSession = (Zezex.LinkSession)link.getUserState();
					linkSession.Bind(link, rpc.Argument.getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe rpc) throws Throwable {

		var providerSession = (Zezex.ProviderSession)rpc.getSender().getUserState();
		for (var module : rpc.Argument.getModules().entrySet()) {
			var providerModuleState = new ProviderModuleState(providerSession.getSessionId(),
					module.getKey(), module.getValue().getChoiceType(), module.getValue().getConfigType());
			var serviceName = MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), module.getKey());
			var subState = App.getServiceManagerAgent().SubscribeService(
					serviceName, module.getValue().getSubscribeType(), providerModuleState);
			// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
			if (Zeze.Services.ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit == module.getValue().getSubscribeType())
				subState.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), providerModuleState);
		}

		rpc.SendResult();
		return Procedure.Success;
	}

	private void UnBindModules(Zeze.Net.AsyncSocket provider, java.lang.Iterable<Integer> modules) {
		UnBindModules(provider, modules, false);
	}

	private void UnBindModules(Zeze.Net.AsyncSocket provider, java.lang.Iterable<Integer> modules, boolean isOnProviderClose) {
		var providerSession = (Zezex.ProviderSession)provider.getUserState();
		for (var moduleId : modules) {
			if (!isOnProviderClose) {
				providerSession.getStaticBinds().remove(moduleId);
			}
			var serviceName = MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), moduleId);
			var volatileProviders = App.getServiceManagerAgent().getSubscribeStates().get(serviceName);
			if (null == volatileProviders)
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
				var link = App.LinkdService.GetSocket(linkSid);
				if (null != link) {
					var linkSession = (Zezex.LinkSession)link.getUserState();
					linkSession.UnBind(link, rpc.Argument.getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessSend(Send protocol) {
		// 这个是拿来处理乱序问题的：多个逻辑服务器之间，给客户端发送协议排队。
		// 所以不用等待真正发送给客户端，收到就可以发送结果。
		if (protocol.Argument.getConfirmSerialId() != 0) {
			var confirm = new SendConfirm();
			confirm.Argument.setConfirmSerialId(protocol.Argument.getConfirmSerialId());
			protocol.getSender().Send(confirm);
		}

		for (var linkSid : protocol.Argument.getLinkSids()) {
			var link = App.LinkdService.GetSocket(linkSid);
			var ptype = protocol.Argument.getProtocolType();
			logger.debug("Send {} {}", Protocol.GetModuleId(ptype), Protocol.GetProtocolId(ptype));
			// ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
			if (link != null) {
				link.Send(protocol.Argument.getProtocolWholeData());
			}
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessBroadcast(Broadcast protocol) throws Throwable {
		if (protocol.Argument.getConfirmSerialId() != 0) {
			var confirm = new SendConfirm();
			confirm.Argument.setConfirmSerialId(protocol.Argument.getConfirmSerialId());
			protocol.getSender().Send(confirm);
		}

		Zezex.App.Instance.LinkdService.Foreach((socket) -> {
			// auth 通过就允许发送广播。
			// 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
			// 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
			var linkSession = (Zezex.LinkSession)socket.getUserState();
			if (null != linkSession && linkSession.getAccount() == null && !linkSession.getUserStates().isEmpty()) {
				socket.Send(protocol.Argument.getProtocolWholeData());
			}
		});
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessKick(Kick protocol) {
		Zezex.App.Instance.LinkdService.ReportError(
				protocol.Argument.getLinksid(),
				Zezex.Linkd.BReportError.FromProvider,
				protocol.Argument.getCode(),
				protocol.Argument.getDesc());
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessSetUserState(SetUserState protocol) {
		var socket = App.LinkdService.GetSocket(protocol.Argument.getLinkSid());
		var linkSession = (Zezex.LinkSession)socket.getUserState();
		if (linkSession != null) {
			linkSession.SetUserState(protocol.Argument.getStates(), protocol.Argument.getStatex());
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectRequest(ModuleRedirect rpc) {
		final long SourceProvider = rpc.getSender().getSessionId();
		final long SourceRpcSessionId = rpc.getSessionId();
		var provider = new Zeze.Util.OutObject<Long>();

		// ModuleRedirectToServer
		if (rpc.Argument.getRedirectType() == ModuleRedirect.RedirectTypeToServer) {
			if (ChoiceProviderByServerId(rpc.Argument.getServiceNamePrefix(), rpc.Argument.getModuleId(),
					rpc.Argument.getHashCode(), provider)) {
				rpc.Send(App.LinkdProviderService.GetSocket(provider.Value), (context) -> {
					// process result。context == rpc
					if (rpc.isTimeout()) {
						rpc.setResultCode(ModuleRedirect.ResultCodeLinkdTimeout);
					}
					// send back to src provider
					rpc.setSender(App.LinkdProviderService.GetSocket(SourceProvider));
					rpc.setSessionId(SourceRpcSessionId);
					rpc.SendResult();
					return Zeze.Transaction.Procedure.Success;
				});
				// async mode
			} else {
				rpc.SendResultCode(ModuleRedirect.ResultCodeLinkdNoProvider); // send back direct
			}
			return 0;
		}
		// ModuleRedirect Or ModuleRedirectWithHash
		if (ChoiceProvider(rpc.Argument.getServiceNamePrefix(), rpc.Argument.getModuleId(),
				rpc.Argument.getHashCode(), provider)) {
			rpc.Send(App.LinkdProviderService.GetSocket(provider.Value), (context) -> {
				// process result。context == rpc
				if (rpc.isTimeout()) {
					rpc.setResultCode(ModuleRedirect.ResultCodeLinkdTimeout);
				}

				// send back to src provider
				rpc.setSender(App.LinkdProviderService.GetSocket(SourceProvider));
				rpc.setSessionId(SourceRpcSessionId);
				rpc.SendResult();
				return Zeze.Transaction.Procedure.Success;
			});
			// async mode
		} else {
			rpc.SendResultCode(ModuleRedirect.ResultCodeLinkdNoProvider); // send back direct
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol) {
		HashMap<Long, ModuleRedirectAllRequest> transmits = new HashMap<>();

		ModuleRedirectAllResult miss = new ModuleRedirectAllResult();
		miss.Argument.setModuleId(protocol.Argument.getModuleId());
		miss.Argument.setMethodFullName(protocol.Argument.getMethodFullName());
		miss.Argument.setSourceProvider(protocol.getSender().getSessionId()); // not used
		miss.Argument.setSessionId(protocol.Argument.getSessionId());
		miss.Argument.setServerId(0); // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
		miss.setResultCode(ModuleRedirect.ResultCodeLinkdNoProvider);

		for (int i = 0; i < protocol.Argument.getHashCodeConcurrentLevel(); ++i) {
			var provider = new Zeze.Util.OutObject<Long>();
			if (ChoiceProvider(protocol.Argument.getServiceNamePrefix(),
					protocol.Argument.getModuleId(), i, provider)) {
				var exist = transmits.get(provider.Value);
				if (null == exist) {
					exist = new ModuleRedirectAllRequest();
					exist.Argument.setModuleId(protocol.Argument.getModuleId());
					exist.Argument.setHashCodeConcurrentLevel(protocol.Argument.getHashCodeConcurrentLevel());
					exist.Argument.setMethodFullName(protocol.Argument.getMethodFullName());
					exist.Argument.setSourceProvider(protocol.getSender().getSessionId());
					exist.Argument.setSessionId(protocol.Argument.getSessionId());
					exist.Argument.setParams(protocol.Argument.getParams());
					transmits.put(provider.Value, exist);
				}
				exist.Argument.getHashCodes().add(i);
			} else {
				BModuleRedirectAllHash tempVar = new BModuleRedirectAllHash();
				tempVar.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
				miss.Argument.getHashs().put(i, tempVar);
			}
		}

		// 转发给provider
		for (var transmit : transmits.entrySet()) {
			var socket = App.LinkdProviderService.GetSocket(transmit.getKey());
			if (null != socket) {
				transmit.getValue().Send(socket);
			} else {
				for (var hashindex : transmit.getValue().Argument.getHashCodes()) {
					BModuleRedirectAllHash tempVar2 = new BModuleRedirectAllHash();
					tempVar2.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
					miss.Argument.getHashs().put(hashindex, tempVar2);
				}
			}
		}

		// 没有转发成功的provider的hash分组，马上发送结果报告错误。
		if (miss.Argument.getHashs().size() > 0) {
			miss.Send(protocol.getSender());
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) {
		var sourcerProvider = App.LinkdProviderService.GetSocket(protocol.Argument.getSourceProvider());
		if (null != sourcerProvider) {
			protocol.Send(sourcerProvider);
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessReportLoad(ReportLoad protocol) {
		var providerSession = (Zezex.ProviderSession)protocol.getSender().getUserState();
		if (providerSession != null) {
			providerSession.SetLoad(protocol.Argument);
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	protected long ProcessTransmit(Transmit protocol) {
		// 查询 role 所在的 provider 并转发。
		var transmits = new HashMap<Long, Transmit>();
		// 如果 role 不在线，就根据 hash(roleId) 选择 provider 转发。
		var transmitsHash = new HashMap<Integer, Transmit>();

		for (var target : protocol.Argument.getRoles().entrySet()) {
			var provider = App.LinkdProviderService.GetSocket(target.getValue().getProviderSessionId());
			if (null == provider) {
				var hash = target.getKey().hashCode();
				var transmitHash = transmitsHash.get(hash);
				if (null == transmitHash) {
					transmitHash = new Transmit();
					transmitHash.Argument.setActionName(protocol.Argument.getActionName());
					transmitHash.Argument.setSender(protocol.Argument.getSender());
					transmitHash.Argument.setServiceNamePrefix(protocol.Argument.getServiceNamePrefix());
					transmitsHash.put(hash, transmitHash);
				}
				transmitHash.Argument.getRoles().put(target.getKey(), target.getValue());
				continue;
			}
			var transmit = transmits.get(target.getValue().getProviderSessionId());
			if (null == transmit) {
				transmit = new Transmit();
				transmit.Argument.setActionName(protocol.Argument.getActionName());
				transmit.Argument.setSender(protocol.Argument.getSender());
				transmit.Argument.setServiceNamePrefix(protocol.Argument.getServiceNamePrefix());
				transmits.put(target.getValue().getProviderSessionId(), transmit);
			}
			transmit.Argument.getRoles().put(target.getKey(), target.getValue());
		}

		// 已经绑定的会话，查找连接并转发，忽略连接查找错误。
		for (var transmit : transmits.entrySet()) {
			var tsocket = App.LinkdProviderService.GetSocket(transmit.getKey());
			if (tsocket != null) {
				tsocket.Send(transmit.getValue());
			}
		}

		// 会话不存在，根据hash选择Provider并转发，忽略连接查找错误。
		for (var transmitHash : transmitsHash.entrySet()) {
			var provider = new Zeze.Util.OutObject<Long>();
			if (App.Zezex_Provider.ChoiceProvider(
					protocol.Argument.getServiceNamePrefix(),
					getFirstModuleWithConfigTypeDefault(),
					transmitHash.getKey(), provider)) {
				var tsocket = App.LinkdProviderService.GetSocket(provider.Value);
				if (tsocket != null) {
					tsocket.Send(transmitHash.getValue());
				}
			}
		}

		return Zeze.Transaction.Procedure.Success;
	}

	// 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
	// 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
	// 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
	// 不建议在一个项目里面使用多个Prefix。
	private String ServerServiceNamePrefix = "";

	public String getServerServiceNamePrefix() {
		return ServerServiceNamePrefix;
	}

	private void setServerServiceNamePrefix(String value) {
		ServerServiceNamePrefix = value;
	}

	@Override
	protected long ProcessAnnounceProviderInfo(AnnounceProviderInfo protocol) {
		var session = (Zezex.ProviderSession)protocol.getSender().getUserState();
		session.setInfo(protocol.Argument);
		setServerServiceNamePrefix(protocol.Argument.getServiceNamePrefix());
		return Zeze.Transaction.Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleProvider(Zezex.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
