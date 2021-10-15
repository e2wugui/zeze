package Zezex.Provider;

import Zeze.Services.*;
import Zezex.*;
import java.util.*;
import Zeze.Services.ServiceManager.Agent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleProvider extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleProvider.class);

	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	private String MakeServiceName(String serviceNamePrefix, int moduleId) {
		return String.format("%1$s%2$s", serviceNamePrefix, moduleId);
	}

	public boolean ChoiceHash(Agent.SubscribeState providers, int hash, Zeze.Util.OutObject<Long> provider) {
		provider.Value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		if (list.size() == 0) {
			return false;
		}

		Object tempVar = list.get(Integer.remainderUnsigned(hash, list.size())).getLocalState();
		var providerModuleState = tempVar instanceof ProviderModuleState ? (ProviderModuleState)tempVar : null;
		if (null == providerModuleState) {
			return false;
		}

		provider.Value = providerModuleState.getSessionId();
		return true;
	}

	public boolean ChoiceLoad(Agent.SubscribeState providers, Zeze.Util.OutObject<Long> provider) {
		provider.Value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		var frees = new ArrayList<ProviderSession>(list.size());
		var all = new ArrayList<ProviderSession>(list.size());
		int TotalWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.size() - 1; i >= 0; --i) {
			Object tempVar = list.get(i).getLocalState();
			var providerModuleState = tempVar instanceof ProviderModuleState ? (ProviderModuleState)tempVar : null;
			if (null == providerModuleState) {
				continue;
			}
			Object tempVar2 = App.Instance.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			var ps = App.Instance.ProviderService.GetSocket(providerModuleState.getSessionId()) == null
					? null : tempVar2 instanceof ProviderSession ? (ProviderSession)tempVar2 : null;
			if (null == ps) {
				continue; // 这里发现关闭的服务，仅仅忽略.
			}
			all.add(ps);
			if (ps.getOnlineNew() > App.Instance.getConfig().getMaxOnlineNew()) {
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

	public boolean ChoiceProvider(String serviceNamePrefix, int moduleId, int hash, tangible.OutObject<Long> provider) {
		var serviceName = MakeServiceName(serviceNamePrefix, moduleId);
		Object volatileProviders;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
		if (false == getApp().Instance.ServiceManagerAgent.SubscribeStates.TryGetValue(serviceName, out volatileProviders)) {
			provider.outArgValue = 0;
			return false;
		}
		return ChoiceHash(volatileProviders, hash, provider);
	}

	public boolean ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, tangible.OutObject<Long> provider) {
		var serviceName = MakeServiceName(getServerServiceNamePrefix(), moduleId);
		Object tempVar = link.UserState;
		var linkSession = tempVar instanceof LinkSession ? (LinkSession)tempVar : null;

		provider.outArgValue = 0;
		Object volatileProviders;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
		if (false == getApp().Instance.ServiceManagerAgent.SubscribeStates.TryGetValue(serviceName, out volatileProviders)) {
			return false;
		}

		// 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
		// 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
		// 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
		var providerModuleState = volatileProviders.SubscribeInfo.LocalState instanceof ProviderModuleState ? (ProviderModuleState)volatileProviders.SubscribeInfo.LocalState : null;

		switch (providerModuleState.getChoiceType()) {
			case BModule.ChoiceTypeHashAccount:
				return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.getAccount()), provider);

			case BModule.ChoiceTypeHashRoleId:
				if (!linkSession.getUserStates().isEmpty()) {
					return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.getUserStates().get(0)), provider);
				}
				else {
					return false;
				}
		}
		if (ChoiceLoad(volatileProviders, provider)) {
			// 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
			var providerSocket = App.getInstance().getProviderService().GetSocket(provider.outArgValue);
			Object tempVar2 = providerSocket.UserState;
			var providerSession = tempVar2 instanceof ProviderSession ? (ProviderSession)tempVar2 : null;
			linkSession.Bind(link, providerSession.getStaticBinds().keySet(), providerSocket);
			return true;
		}

		return false;
	}

	public void OnProviderClose(Zeze.Net.AsyncSocket provider) {
		Object tempVar = provider.UserState;
		ProviderSession providerSession = tempVar instanceof ProviderSession ? (ProviderSession)tempVar : null;
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
					var link = getApp().Instance.LinkdService.GetSocket(linkSid);
					if (null != link) {
						Object tempVar2 = link.UserState;
						var linkSession = tempVar2 instanceof LinkSession ? (LinkSession)tempVar2 : null;
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
		private long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private int ModuleId;
		public int getModuleId() {
			return ModuleId;
		}
		private int ChoiceType;
		public int getChoiceType() {
			return ChoiceType;
		}
		private int ConfigType;
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
	public int ProcessBindRequest(Bind rpc) {
		if (rpc.getArgument().getLinkSids().Count == 0) {
			Object tempVar = rpc.getSender().UserState;
			var providerSession = tempVar instanceof ProviderSession ? (ProviderSession)tempVar : null;
			for (var module : rpc.getArgument().getModules()) {
				if (getFirstModuleWithConfigTypeDefault() == 0 && module.Value.ConfigType == BModule.ConfigTypeDefault) {
					setFirstModuleWithConfigTypeDefault(module.Value.ConfigType);
				}
				var providerModuleState = new ProviderModuleState(providerSession.getSessionId(), module.Key, module.Value.ChoiceType, module.Value.ConfigType);
				var serviceName = MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), module.Key);
				var subState = getApp().getServiceManagerAgent().SubscribeService(serviceName, ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit, providerModuleState);
				// 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
				subState.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), providerModuleState);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				providerSession.getStaticBinds().TryAdd(module.Key, module.Key);
			}
		}
		else {
			// 动态绑定
			for (var linkSid : rpc.getArgument().getLinkSids()) {
				var link = getApp().Instance.LinkdService.GetSocket(linkSid);
				if (null != link) {
					Object tempVar2 = link.UserState;
					var linkSession = tempVar2 instanceof LinkSession ? (LinkSession)tempVar2 : null;
					linkSession.Bind(link, rpc.getArgument().getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Zeze.Transaction.Procedure.Success;
	}


	private void UnBindModules(Zeze.Net.AsyncSocket provider, java.lang.Iterable<Integer> modules) {
		UnBindModules(provider, modules, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private void UnBindModules(Zeze.Net.AsyncSocket provider, IEnumerable<int> modules, bool isOnProviderClose = false)
	private void UnBindModules(Zeze.Net.AsyncSocket provider, java.lang.Iterable<Integer> modules, boolean isOnProviderClose) {
		Object tempVar = provider.UserState;
		var providerSession = tempVar instanceof ProviderSession ? (ProviderSession)tempVar : null;
		for (var moduleId : modules) {
			if (false == isOnProviderClose) {
				TValue _;
				tangible.OutObject<Integer> tempOut__ = new tangible.OutObject<Integer>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				providerSession.getStaticBinds().TryRemove(moduleId, tempOut__);
			_ = tempOut__.outArgValue;
			}
			var serviceName = MakeServiceName(providerSession.getInfo().getServiceNamePrefix(), moduleId);
			Object volatileProviders;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (false == getApp().Instance.ServiceManagerAgent.SubscribeStates.TryGetValue(serviceName, out volatileProviders)) {
				continue;
			}
			// UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
			// 这里仅仅设置该moduleId对应的服务的状态不可用。
			volatileProviders.SetServiceIdentityReadyState(providerSession.getInfo().getServiceIndentity(), null);
		}
	}
	@Override
	public int ProcessUnBindRequest(UnBind rpc) {
		if (rpc.getArgument().getLinkSids().Count == 0) {
			UnBindModules(rpc.getSender(), rpc.getArgument().getModules().keySet());
		}
		else {
			// 动态绑定
			for (var linkSid : rpc.getArgument().getLinkSids()) {
				var link = getApp().Instance.LinkdService.GetSocket(linkSid);
				if (null != link) {
					Object tempVar = link.UserState;
					var linkSession = tempVar instanceof LinkSession ? (LinkSession)tempVar : null;
					linkSession.UnBind(link, rpc.getArgument().getModules().keySet(), rpc.getSender());
				}
			}
		}
		rpc.SendResultCode(BBind.ResultSuccess);
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessSend(Send protocol) {
		// 这个是拿来处理乱序问题的：多个逻辑服务器之间，给客户端发送协议排队。
		// 所以不用等待真正发送给客户端，收到就可以发送结果。
		if (protocol.getArgument().getConfirmSerialId() != 0) {
			var confirm = new SendConfirm();
			confirm.getArgument().ConfirmSerialId = protocol.getArgument().getConfirmSerialId();
			protocol.Sender.Send(confirm);
		}

		for (var linkSid : protocol.getArgument().getLinkSids()) {
			var link = getApp().Instance.LinkdService.GetSocket(linkSid);
			logger.Debug("Send {0} {1}", Zeze.Net.Protocol.GetModuleId(protocol.getArgument().getProtocolType()), Zeze.Net.Protocol.GetProtocolId(protocol.getArgument().getProtocolType()));
			// ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
			if (link != null) {
				link.Send(protocol.getArgument().getProtocolWholeData());
			}
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessBroadcast(Broadcast protocol) {
		if (protocol.getArgument().getConfirmSerialId() != 0) {
			var confirm = new SendConfirm();
			confirm.getArgument().ConfirmSerialId = protocol.getArgument().getConfirmSerialId();
			protocol.Sender.Send(confirm);
		}

		getApp().Instance.LinkdService.Foreach((socket) -> {
				// auth 通过就允许发送广播。
				// 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
				// 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
				var linkSession = socket.UserState instanceof LinkSession ? (LinkSession)socket.UserState : null;
				if (null != linkSession && !linkSession.getAccount().equals(null) && !linkSession.getUserStates().isEmpty()) {
					socket.Send(protocol.getArgument().getProtocolWholeData());
				}
		});
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessKick(Kick protocol) {
		getApp().Instance.LinkdService.ReportError(protocol.getArgument().getLinksid(), Linkd.BReportError.FromProvider, protocol.getArgument().getCode(), protocol.getArgument().getDesc());
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessSetUserState(SetUserState protocol) {
		var socket = getApp().Instance.LinkdService.GetSocket(protocol.getArgument().getLinkSid());
		Object tempVar = socket.UserState;
		var linkSession = socket == null ? null : tempVar instanceof LinkSession ? (LinkSession)tempVar : null;
		if (linkSession != null) {
			linkSession.SetUserState(protocol.getArgument().getStates(), protocol.getArgument().getStatex());
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessModuleRedirectRequest(ModuleRedirect rpc) {
		long SourceProvider = rpc.getSender().SessionId;
		long provider;

		tangible.OutObject<Long> tempOut_provider = new tangible.OutObject<Long>();
		if (ChoiceProvider(rpc.getArgument().getServiceNamePrefix(), rpc.getArgument().getModuleId(), rpc.getArgument().getHashCode(), tempOut_provider)) {
		provider = tempOut_provider.outArgValue;
			rpc.Send(getApp().getProviderService().GetSocket(provider), (context) -> {
					// process result。context == rpc
					if (rpc.isTimeout()) {
						rpc.setResultCode(ModuleRedirect.ResultCodeLinkdTimeout);
					}

					rpc.Send(getApp().getProviderService().GetSocket(SourceProvider)); // send back to src provider
					return Zeze.Transaction.Procedure.Success;
			});
			// async mode
		}
		else {
		provider = tempOut_provider.outArgValue;
			rpc.SendResultCode(ModuleRedirect.ResultCodeLinkdNoProvider); // send back direct
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol) {
		HashMap<Long, ModuleRedirectAllRequest> transmits = new HashMap<Long, ModuleRedirectAllRequest>();

		ModuleRedirectAllResult miss = new ModuleRedirectAllResult();
		miss.getArgument().ModuleId = protocol.getArgument().getModuleId();
		miss.getArgument().MethodFullName = protocol.getArgument().getMethodFullName();
		miss.getArgument().SourceProvider = protocol.Sender.SessionId; // not used
		miss.getArgument().SessionId = protocol.getArgument().getSessionId();
		miss.getArgument().ServerId = 0; // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
		miss.ResultCode = ModuleRedirect.ResultCodeLinkdNoProvider;

		for (int i = 0; i < protocol.getArgument().getHashCodeConcurrentLevel(); ++i) {
			long provider;
			tangible.OutObject<Long> tempOut_provider = new tangible.OutObject<Long>();
			if (ChoiceProvider(protocol.getArgument().getServiceNamePrefix(), protocol.getArgument().getModuleId(), i, tempOut_provider)) {
			provider = tempOut_provider.outArgValue;
				TValue exist;
				if (false == (transmits.containsKey(provider) && (exist = transmits.get(provider)) == exist)) {
					exist = new ModuleRedirectAllRequest();
					exist.Argument.ModuleId = protocol.getArgument().getModuleId();
					exist.Argument.HashCodeConcurrentLevel = protocol.getArgument().getHashCodeConcurrentLevel();
					exist.Argument.MethodFullName = protocol.getArgument().getMethodFullName();
					exist.Argument.SourceProvider = protocol.Sender.SessionId;
					exist.Argument.SessionId = protocol.getArgument().getSessionId();
					exist.Argument.Params = protocol.getArgument().getParams();
					transmits.put(provider, exist);
				}
				exist.Argument.HashCodes.Add(i);
			}
			else {
			provider = tempOut_provider.outArgValue;
				BModuleRedirectAllHash tempVar = new BModuleRedirectAllHash();
				tempVar.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
				miss.getArgument().getHashs().set(i, tempVar);
			}
		}

		// 转发给provider
		for (var transmit : transmits.entrySet()) {
			var socket = getApp().getProviderService().GetSocket(transmit.getKey());
			if (null != socket) {
				transmit.getValue().Send(socket);
			}
			else {
				for (var hashindex : transmit.getValue().Argument.HashCodes) {
					BModuleRedirectAllHash tempVar2 = new BModuleRedirectAllHash();
					tempVar2.setReturnCode(Zeze.Transaction.Procedure.ProviderNotExist);
					miss.getArgument().getHashs().set(hashindex, tempVar2);
				}
			}
		}

		// 没有转发成功的provider的hash分组，马上发送结果报告错误。
		if (miss.getArgument().getHashs().Count > 0) {
			miss.Send(protocol.Sender);
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol) {
		var sourcerProvider = getApp().getProviderService().GetSocket(protocol.getArgument().getSourceProvider());
		if (null != sourcerProvider) {
			protocol.Send(sourcerProvider);
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessReportLoad(ReportLoad protocol) {
		var providerSession = protocol.Sender.UserState instanceof ProviderSession ? (ProviderSession)protocol.Sender.UserState : null;
		if (providerSession != null) {
			providerSession.SetLoad(protocol.getArgument());
		}
		return Zeze.Transaction.Procedure.Success;
	}

	@Override
	public int ProcessTransmit(Transmit protocol) {
		// 查询 role 所在的 provider 并转发。
		var transmits = new HashMap<Long, Transmit>();
		// 如果 role 不在线，就根据 hash(roleId) 选择 provider 转发。
		var transmitsHash = new HashMap<Integer, Transmit>();

		for (var target : protocol.getArgument().getRoles()) {
			var provider = getApp().getProviderService().GetSocket(target.Value.ProviderSessionId);
			if (null == provider) {
				var hash = target.Key.hashCode();
				TValue transmitHash;
				if (false == (transmitsHash.containsKey(hash) && (transmitHash = transmitsHash.get(hash)) == transmitHash)) {
					transmitHash = new Transmit();
					transmitHash.Argument.ActionName = protocol.getArgument().getActionName();
					transmitHash.Argument.Sender = protocol.getArgument().getSender();
					transmitHash.Argument.ServiceNamePrefix = protocol.getArgument().getServiceNamePrefix();
					transmitsHash.put(hash, transmitHash);
				}
				transmitHash.Argument.Roles.Add(target.Key, target.Value);
				continue;
			}
			TValue transmit;
			if (false == (transmits.containsKey(target.Value.ProviderSessionId) && (transmit = transmits.get(target.Value.ProviderSessionId)) == transmit)) {
				transmit = new Transmit();
				transmit.Argument.ActionName = protocol.getArgument().getActionName();
				transmit.Argument.Sender = protocol.getArgument().getSender();
				transmit.Argument.ServiceNamePrefix = protocol.getArgument().getServiceNamePrefix();
				transmits.put(target.Value.ProviderSessionId, transmit);
			}
			transmit.Argument.Roles.Add(target.Key, target.Value);
		}

		// 已经绑定的会话，查找连接并转发，忽略连接查找错误。
		for (var transmit : transmits.entrySet()) {
			if (getApp().getProviderService().GetSocket(transmit.getKey()) != null) {
				getApp().getProviderService().GetSocket(transmit.getKey()).Send(transmit.getValue());
			}
		}

		// 会话不存在，根据hash选择Provider并转发，忽略连接查找错误。
		for (var transmitHash : transmitsHash.entrySet()) {
			long provider;
			tangible.OutObject<Long> tempOut_provider = new tangible.OutObject<Long>();
			if (getApp().getZezexProvider().ChoiceProvider(protocol.getArgument().getServiceNamePrefix(), getFirstModuleWithConfigTypeDefault(), transmitHash.getKey(), tempOut_provider)) {
			provider = tempOut_provider.outArgValue;
				if (getApp().getProviderService().GetSocket(provider) != null) {
					getApp().getProviderService().GetSocket(provider).Send(transmitHash.getValue());
				}
			}
		else {
			provider = tempOut_provider.outArgValue;
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
	public int ProcessAnnounceProviderInfo(AnnounceProviderInfo protocol) {
		var session = protocol.Sender.UserState instanceof ProviderSession ? (ProviderSession)protocol.Sender.UserState : null;
		session.setInfo(protocol.getArgument());
		setServerServiceNamePrefix(protocol.getArgument().getServiceNamePrefix());
		return Zeze.Transaction.Procedure.Success;
	}

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 10001;


    public Zezex.App App;

    public ModuleProvider(Zezex.App app) {
        App = app;
        // register protocol factory and handles
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.AnnounceProviderInfo();
            factoryHandle.Handle = (_p) -> ProcessAnnounceProviderInfo(_p);
            factoryHandle.NoProcedure = true,
            App.ProviderService.AddFactoryHandle(655451039, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Bind();
            factoryHandle.Handle = (_p) -> ProcessBindRequest(_p);
            App.ProviderService.AddFactoryHandle(655479127, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Broadcast();
            factoryHandle.Handle = (_p) -> ProcessBroadcast(_p);
            App.ProviderService.AddFactoryHandle(655477884, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Kick();
            factoryHandle.Handle = (_p) -> ProcessKick(_p);
            App.ProviderService.AddFactoryHandle(655446121, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ModuleRedirect();
            factoryHandle.Handle = (_p) -> ProcessModuleRedirectRequest(_p);
            App.ProviderService.AddFactoryHandle(655455850, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ModuleRedirectAllRequest();
            factoryHandle.Handle = (_p) -> ProcessModuleRedirectAllRequest(_p);
            App.ProviderService.AddFactoryHandle(655479394, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ModuleRedirectAllResult();
            factoryHandle.Handle = (_p) -> ProcessModuleRedirectAllResult(_p);
            App.ProviderService.AddFactoryHandle(655465353, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.ReportLoad();
            factoryHandle.Handle = (_p) -> ProcessReportLoad(_p);
            App.ProviderService.AddFactoryHandle(655489496, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Send();
            factoryHandle.Handle = (_p) -> ProcessSend(_p);
            App.ProviderService.AddFactoryHandle(655456505, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.SetUserState();
            factoryHandle.Handle = (_p) -> ProcessSetUserState(_p);
            App.ProviderService.AddFactoryHandle(655480350, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.Transmit();
            factoryHandle.Handle = (_p) -> ProcessTransmit(_p);
            factoryHandle.NoProcedure = true,
            App.ProviderService.AddFactoryHandle(655453724, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Zezex.Provider.UnBind();
            factoryHandle.Handle = (_p) -> ProcessUnBindRequest(_p);
            App.ProviderService.AddFactoryHandle(655436306, factoryHandle);
        }
        // register table
    }

    public void UnRegister() {
        App.ProviderService.getFactorys().remove(655451039);
        App.ProviderService.getFactorys().remove(655479127);
        App.ProviderService.getFactorys().remove(655477884);
        App.ProviderService.getFactorys().remove(655446121);
        App.ProviderService.getFactorys().remove(655455850);
        App.ProviderService.getFactorys().remove(655479394);
        App.ProviderService.getFactorys().remove(655465353);
        App.ProviderService.getFactorys().remove(655489496);
        App.ProviderService.getFactorys().remove(655456505);
        App.ProviderService.getFactorys().remove(655480350);
        App.ProviderService.getFactorys().remove(655453724);
        App.ProviderService.getFactorys().remove(655436306);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
