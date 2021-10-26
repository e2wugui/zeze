package Zezex.Provider;

import java.util.*;
import Zeze.Net.Protocol;
import Zeze.Services.ServiceManager.Agent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleProvider extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(ModuleProvider.class);

    public void Start(Zezex.App app) {
    }

    public void Stop(Zezex.App app) {
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
        var providerModuleState = tempVar instanceof ProviderModuleState ? (ProviderModuleState) tempVar : null;
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
            Object tempVar = list.get(i).getLocalState();
            var providerModuleState = tempVar instanceof ProviderModuleState ? (ProviderModuleState) tempVar : null;
            if (null == providerModuleState) {
                continue;
            }
            Object tempVar2 = App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
            var ps = (Zezex.ProviderSession)App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
            if (null == ps) {
                continue; // 这里发现关闭的服务，仅仅忽略.
            }
            all.add(ps);
            if (ps.getOnlineNew() > App.Instance.getLinkConfig().getMaxOnlineNew()) {
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

    public boolean ChoiceProvider(String serviceNamePrefix, int moduleId, int hash, Zeze.Util.OutObject<Long> provider) {
        var serviceName = MakeServiceName(serviceNamePrefix, moduleId);

        var volatileProviders = App.getServiceManagerAgent().getSubscribeStates().get(serviceName);
        if (null == volatileProviders) {
            provider.Value = 0L;
            return false;
        }
        return ChoiceHash(volatileProviders, hash, provider);
    }

    public boolean ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, Zeze.Util.OutObject<Long> provider) {
        var serviceName = MakeServiceName(getServerServiceNamePrefix(), moduleId);
        var linkSession = (Zezex.LinkSession) link.getUserState();
        provider.Value = 0L;
        var volatileProviders = App.getServiceManagerAgent().getSubscribeStates().get(serviceName);
        if (null == volatileProviders)
            return false;

        // 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
        // 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
        // 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
        var providerModuleState = (ProviderModuleState) volatileProviders.getSubscribeInfo().getLocalState();
        switch (providerModuleState.getChoiceType()) {
            case BModule.ChoiceTypeHashAccount:
                return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.getAccount()), provider);

            case BModule.ChoiceTypeHashRoleId:
                if (!linkSession.getUserStates().isEmpty()) {
                    return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.getUserStates().get(0)), provider);
                } else {
                    return false;
                }
        }
        if (ChoiceLoad(volatileProviders, provider)) {
            // 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
            var providerSocket = App.ProviderService.GetSocket(provider.Value);
            var providerSession = (Zezex.ProviderSession) providerSocket.getUserState();
            linkSession.Bind(link, providerSession.getStaticBinds().keySet(), providerSocket);
            return true;
        }

        return false;
    }

    public void OnProviderClose(Zeze.Net.AsyncSocket provider) {
        var providerSession = (Zezex.ProviderSession) provider.getUserState();
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
                        var linkSession = (Zezex.LinkSession) link.getUserState();
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
    public int ProcessBindRequest(Protocol _rpc) {
        var rpc = (Bind) _rpc;
        if (rpc.Argument.getLinkSids().size() == 0) {
            var providerSession = (Zezex.ProviderSession) rpc.getSender().getUserState();
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
                    var linkSession = (Zezex.LinkSession) link.getUserState();
                    linkSession.Bind(link, rpc.Argument.getModules().keySet(), rpc.getSender());
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
        var providerSession = (Zezex.ProviderSession) provider.getUserState();
        for (var moduleId : modules) {
            if (false == isOnProviderClose) {
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
    public int ProcessUnBindRequest(Protocol _rpc) {
        var rpc = (UnBind) _rpc;
        if (rpc.Argument.getLinkSids().size() == 0) {
            UnBindModules(rpc.getSender(), rpc.Argument.getModules().keySet());
        } else {
            // 动态绑定
            for (var linkSid : rpc.Argument.getLinkSids()) {
                var link = App.LinkdService.GetSocket(linkSid);
                if (null != link) {
                    var linkSession = (Zezex.LinkSession) link.getUserState();
                    linkSession.UnBind(link, rpc.Argument.getModules().keySet(), rpc.getSender());
                }
            }
        }
        rpc.SendResultCode(BBind.ResultSuccess);
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessSend(Protocol _p) {
        var protocol = (Send) _p;
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
    public int ProcessBroadcast(Protocol _protocol) {
        var protocol = (Broadcast) _protocol;
        if (protocol.Argument.getConfirmSerialId() != 0) {
            var confirm = new SendConfirm();
            confirm.Argument.setConfirmSerialId(protocol.Argument.getConfirmSerialId());
            protocol.getSender().Send(confirm);
        }

        App.Instance.LinkdService.Foreach((socket) -> {
            // auth 通过就允许发送广播。
            // 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
            // 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
            var linkSession = (Zezex.LinkSession) socket.getUserState();
            if (null != linkSession && !linkSession.getAccount().equals(null) && !linkSession.getUserStates().isEmpty()) {
                socket.Send(protocol.Argument.getProtocolWholeData());
            }
        });
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessKick(Protocol _p) {
        var protocol = (Kick) _p;
        App.Instance.LinkdService.ReportError(
                protocol.Argument.getLinksid(),
                Zezex.Linkd.BReportError.FromProvider,
                protocol.Argument.getCode(),
                protocol.Argument.getDesc());
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessSetUserState(Protocol _p) {
        var protocol = (SetUserState) _p;
        var socket = App.LinkdService.GetSocket(protocol.Argument.getLinkSid());
        var linkSession = (Zezex.LinkSession) socket.getUserState();
        if (linkSession != null) {
            linkSession.SetUserState(protocol.Argument.getStates(), protocol.Argument.getStatex());
        }
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessModuleRedirectRequest(Protocol _rpc) {
        var rpc = (ModuleRedirect) _rpc;
        long SourceProvider = rpc.getSender().getSessionId();
        var provider = new Zeze.Util.OutObject<Long>();
        if (ChoiceProvider(rpc.Argument.getServiceNamePrefix(), rpc.Argument.getModuleId(),
                rpc.Argument.getHashCode(), provider)) {
            rpc.Send(App.ProviderService.GetSocket(provider.Value), (context) -> {
                // process result。context == rpc
                if (rpc.isTimeout()) {
                    rpc.setResultCode(ModuleRedirect.ResultCodeLinkdTimeout);
                }

                rpc.Send(App.ProviderService.GetSocket(SourceProvider)); // send back to src provider
                return Zeze.Transaction.Procedure.Success;
            });
            // async mode
        } else {
            rpc.SendResultCode(ModuleRedirect.ResultCodeLinkdNoProvider); // send back direct
        }
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessModuleRedirectAllRequest(Protocol _protocol) {
        var protocol = (ModuleRedirectAllRequest) _protocol;
        HashMap<Long, ModuleRedirectAllRequest> transmits = new HashMap<Long, ModuleRedirectAllRequest>();

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
            var socket = App.ProviderService.GetSocket(transmit.getKey());
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
    public int ProcessModuleRedirectAllResult(Protocol _protocol) {
        var protocol = (ModuleRedirectAllResult) _protocol;
        var sourcerProvider = App.ProviderService.GetSocket(protocol.Argument.getSourceProvider());
        if (null != sourcerProvider) {
            protocol.Send(sourcerProvider);
        }
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessReportLoad(Protocol _protocol) {
        var protocol = (ReportLoad) _protocol;
        var providerSession = (Zezex.ProviderSession) protocol.getSender().getUserState();
        if (providerSession != null) {
            providerSession.SetLoad(protocol.Argument);
        }
        return Zeze.Transaction.Procedure.Success;
    }

    @Override
    public int ProcessTransmit(Protocol _protocol) {
        var protocol = (Transmit) _protocol;
        // 查询 role 所在的 provider 并转发。
        var transmits = new HashMap<Long, Transmit>();
        // 如果 role 不在线，就根据 hash(roleId) 选择 provider 转发。
        var transmitsHash = new HashMap<Integer, Transmit>();

        for (var target : protocol.Argument.getRoles().entrySet()) {
            var provider = App.ProviderService.GetSocket(target.getValue().getProviderSessionId());
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
            if (null == transmit){
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
            var tsocket = App.ProviderService.GetSocket(transmit.getKey());
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
                var tsocket = App.ProviderService.GetSocket(provider.Value);
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
    public int ProcessAnnounceProviderInfo(Protocol _protocol) {
        var protocol = (AnnounceProviderInfo) _protocol;
        var session = (Zezex.ProviderSession) protocol.getSender().getUserState();
        session.setInfo(protocol.Argument);
        setServerServiceNamePrefix(protocol.Argument.getServiceNamePrefix());
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
            factoryHandle.NoProcedure = true;
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
            factoryHandle.NoProcedure = true;
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
