package Zezex.Provider;

import Zeze.Services.*;
import Zezex.*;
import java.util.*;

// auto-generated



public final class ModuleProvider extends AbstractModule {
	public static final int ModuleId = 10001;


	private App App;
	public App getApp() {
		return App;
	}

	public ModuleProvider(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getProviderService().AddFactoryHandle(655451039, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.AnnounceProviderInfo(), Handle = Zeze.Net.Service.<AnnounceProviderInfo>MakeHandle(this, this.getClass().getMethod("ProcessAnnounceProviderInfo")), NoProcedure = true});
		getApp().getProviderService().AddFactoryHandle(655479127, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Bind(), Handle = Zeze.Net.Service.<Bind>MakeHandle(this, this.getClass().getMethod("ProcessBindRequest"))});
		getApp().getProviderService().AddFactoryHandle(655477884, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Broadcast(), Handle = Zeze.Net.Service.<Broadcast>MakeHandle(this, this.getClass().getMethod("ProcessBroadcast"))});
		getApp().getProviderService().AddFactoryHandle(655446121, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Kick(), Handle = Zeze.Net.Service.<Kick>MakeHandle(this, this.getClass().getMethod("ProcessKick"))});
		getApp().getProviderService().AddFactoryHandle(655455850, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ModuleRedirect(), Handle = Zeze.Net.Service.<ModuleRedirect>MakeHandle(this, this.getClass().getMethod("ProcessModuleRedirectRequest"))});
		getApp().getProviderService().AddFactoryHandle(655479394, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ModuleRedirectAllRequest(), Handle = Zeze.Net.Service.<ModuleRedirectAllRequest>MakeHandle(this, this.getClass().getMethod("ProcessModuleRedirectAllRequest"))});
		getApp().getProviderService().AddFactoryHandle(655465353, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ModuleRedirectAllResult(), Handle = Zeze.Net.Service.<ModuleRedirectAllResult>MakeHandle(this, this.getClass().getMethod("ProcessModuleRedirectAllResult"))});
		getApp().getProviderService().AddFactoryHandle(655489496, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.ReportLoad(), Handle = Zeze.Net.Service.<ReportLoad>MakeHandle(this, this.getClass().getMethod("ProcessReportLoad"))});
		getApp().getProviderService().AddFactoryHandle(655456505, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Send(), Handle = Zeze.Net.Service.<Send>MakeHandle(this, this.getClass().getMethod("ProcessSend"))});
		getApp().getProviderService().AddFactoryHandle(655480350, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.SetUserState(), Handle = Zeze.Net.Service.<SetUserState>MakeHandle(this, this.getClass().getMethod("ProcessSetUserState"))});
		getApp().getProviderService().AddFactoryHandle(655453724, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.Transmit(), Handle = Zeze.Net.Service.<Transmit>MakeHandle(this, this.getClass().getMethod("ProcessTransmit")), NoProcedure = true});
		getApp().getProviderService().AddFactoryHandle(655436306, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Zezex.Provider.UnBind(), Handle = Zeze.Net.Service.<UnBind>MakeHandle(this, this.getClass().getMethod("ProcessUnBindRequest"))});
		// register table
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655451039, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655479127, tempOut__2);
	_ = tempOut__2.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__3 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655477884, tempOut__3);
	_ = tempOut__3.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__4 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655446121, tempOut__4);
	_ = tempOut__4.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__5 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655455850, tempOut__5);
	_ = tempOut__5.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__6 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655479394, tempOut__6);
	_ = tempOut__6.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__7 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655465353, tempOut__7);
	_ = tempOut__7.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__8 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655489496, tempOut__8);
	_ = tempOut__8.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__9 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655456505, tempOut__9);
	_ = tempOut__9.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__10 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655480350, tempOut__10);
	_ = tempOut__10.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__11 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655453724, tempOut__11);
	_ = tempOut__11.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__12 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getProviderService().getFactorys().TryRemove(655436306, tempOut__12);
	_ = tempOut__12.outArgValue;
	}



	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	private String MakeServiceName(String serviceNamePrefix, int moduleId) {
		return String.format("%1$s%2$s", serviceNamePrefix, moduleId);
	}

	public boolean ChoiceHash(ServiceManager.Agent.SubscribeState providers, int hash, tangible.OutObject<Long> provider) {
		provider.outArgValue = 0;

		var list = providers.ServiceInfos.ServiceInfoListSortedByIdentity;
		if (list.Count == 0) {
			return false;
		}

		Object tempVar = list.get(hash % list.Count).LocalState;
		var providerModuleState = tempVar instanceof ProviderModuleState ? (ProviderModuleState)tempVar : null;
		if (null == providerModuleState) {
			return false;
		}

		provider.outArgValue = providerModuleState.getSessionId();
		return true;
	}

	public boolean ChoiceLoad(ServiceManager.Agent.SubscribeState providers, tangible.OutObject<Long> provider) {
		provider.outArgValue = 0;

		var list = providers.ServiceInfos.ServiceInfoListSortedByIdentity;
		var frees = new ArrayList<ProviderSession>(list.Count);
		var all = new ArrayList<ProviderSession>(list.Count);
		int TotalWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.Count - 1; i >= 0; --i) {
			Object tempVar = list.get(i).LocalState;
			var providerModuleState = tempVar instanceof ProviderModuleState ? (ProviderModuleState)tempVar : null;
			if (null == providerModuleState) {
				continue;
			}
			Object tempVar2 = getApp().Instance.ProviderService.GetSocket(providerModuleState.getSessionId()).UserState;
			var ps = getApp().Instance.ProviderService.GetSocket(providerModuleState.getSessionId()) == null ? null : tempVar2 instanceof ProviderSession ? (ProviderSession)tempVar2 : null;
			if (null == ps) {
				continue; // 这里发现关闭的服务，仅仅忽略.
			}
			all.add(ps);
			if (ps.OnlineNew > getApp().Instance.Config.MaxOnlineNew) {
				continue;
			}
			int weight = ps.ProposeMaxOnline - ps.Online;
			if (weight <= 0) {
				continue;
			}
			frees.add(ps);
			TotalWeight += weight;
		}
		if (TotalWeight > 0) {
			int randweight = Zeze.Util.Random.Instance.nextInt(TotalWeight);
			for (var ps : frees) {
				int weight = ps.getProposeMaxOnline() - ps.getOnline();
				if (randweight < weight) {
					provider.outArgValue = ps.getSessionId();
					return true;
				}
				randweight -= weight;
			}
		}
		// 选择失败，一般是都满载了，随机选择一个。
		if (!all.isEmpty()) {
			provider.outArgValue = all.get(Zeze.Util.Random.Instance.nextInt(all.size())).getSessionId();
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
}
