﻿
using System.Collections.Generic;
using Zeze.Services;

namespace gnet.Provider
{
    public sealed partial class ModuleProvider : AbstractModule
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public void Start(gnet.App app)
        {
        }

        public void Stop(gnet.App app)
        {
        }

        private string MakeServiceName(string serviceNamePrefix, int moduleId)
        {
            return $"{serviceNamePrefix}{moduleId}";
        }

        public bool ChoiceHash(ServiceManager.Agent.SubscribeState providers,
            int hash, out long provider)
        {
            provider = 0;

            var list = providers.ServiceInfos.ServiceInfoListSortedByIdentity;
            if (list.Count == 0)
                return false;

            var providerModuleState = list[hash % list.Count].LocalState as ProviderModuleState;
            if (null == providerModuleState)
                return false;

            provider = providerModuleState.SessionId;
            return true;
        }

        public bool ChoiceLoad(ServiceManager.Agent.SubscribeState providers, out long provider)
        {
            provider = 0;

            var list = providers.ServiceInfos.ServiceInfoListSortedByIdentity;
            var frees = new List<ProviderSession>(list.Count);
            var all = new List<ProviderSession>(list.Count);
            int TotalWeight = 0;

            // 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
            for (int i = list.Count - 1; i >= 0; --i)
            {
                var providerModuleState = list[i].LocalState as ProviderModuleState;
                if (null == providerModuleState)
                    continue;
                var ps = App.Instance.ProviderService.GetSocket(providerModuleState.SessionId)?.UserState as ProviderSession;
                if (null == ps)
                    continue; // 这里发现关闭的服务，仅仅忽略.
                all.Add(ps);
                if (ps.OnlineNew > App.Instance.Config.MaxOnlineNew)
                    continue;
                int weight = ps.ProposeMaxOnline - ps.Online;
                if (weight <= 0)
                    continue;
                frees.Add(ps);
                TotalWeight += weight;
            }
            if (TotalWeight > 0)
            {
                int randweight = Zeze.Util.Random.Instance.Next(TotalWeight);
                foreach (var ps in frees)
                {
                    int weight = ps.ProposeMaxOnline - ps.Online;
                    if (randweight < weight)
                    {
                        provider = ps.SessionId;
                        return true;
                    }
                    randweight -= weight;
                }
            }
            // 选择失败，一般是都满载了，随机选择一个。
            if (all.Count > 0)
            {
                provider = all[Zeze.Util.Random.Instance.Next(all.Count)].SessionId;
                return true;
            }
            // no providers
            return false;
        }

        public bool ChoiceProvider(string serviceNamePrefix, int moduleId, int hash, out long provider)
        {
            var serviceName = MakeServiceName(serviceNamePrefix, moduleId);
            if (false == App.Instance.ServiceManagerAgent.SubscribeStates.TryGetValue(
                serviceName, out var volatileProviders))
            {
                provider = 0;
                return false;
            }
            return ChoiceHash(volatileProviders, hash, out provider);
        }

        public bool ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, out long provider)
        {
            var serviceName = MakeServiceName(GameServerServiceNamePrefix, moduleId);
            var linkSession = link.UserState as LinkSession;

            provider = 0;
            if (false == App.Instance.ServiceManagerAgent.SubscribeStates.TryGetValue(
                serviceName, out var volatileProviders))
                return false;

            // 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
            // 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
            // 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
            var providerModuleState = volatileProviders.SubscribeInfo.LocalState as ProviderModuleState;

            switch (providerModuleState.ChoiceType)
            {
                case BModule.ChoiceTypeHashUserId:
                    return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.UserId), out provider);

                case BModule.ChoiceTypeHashRoleId:
                    if (linkSession.UserStates.Count > 0)
                    {
                        return ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.UserStates[0]), out provider);
                    }
                    else
                    {
                        return false;
                    }
            }
            if (ChoiceLoad(volatileProviders, out provider))
            {
                // 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
                var providerSocket = gnet.App.Instance.ProviderService.GetSocket(provider);
                var providerSession = providerSocket.UserState as ProviderSession;
                linkSession.Bind(link, providerSession.StaticBinds.Keys, providerSocket);
                return true;
            }

            return false;
        }

        public void OnProviderClose(Zeze.Net.AsyncSocket provider)
        {
            ProviderSession providerSession = provider.UserState as ProviderSession;
            if (null == providerSession)
                return;

            // unbind module
            UnBindModules(provider, providerSession.StaticBinds.Keys, true);
            providerSession.StaticBinds.Clear();

            // unbind LinkSession
            lock (providerSession.LinkSessionIds)
            {
                foreach (var e in providerSession.LinkSessionIds)
                {
                    foreach (var linkSid in e.Value)
                    {
                        var link = App.Instance.LinkdService.GetSocket(linkSid);
                        if (null != link)
                        {
                            var linkSession = link.UserState as LinkSession;
                            linkSession?.UnBind(link, e.Key, provider, true);
                        }
                    }
                }
                providerSession.LinkSessionIds.Clear();
            }
        }

        public int FirstModuleWithConfigTypeDefault { get; private set; } = 0;

        public sealed class ProviderModuleState
        {
            public long SessionId { get; }
            public int ModuleId { get; }
            public int ChoiceType { get; }
            public int ConfigType { get; }

            public ProviderModuleState(long sessionId, int moduleId, int choiceType, int configType)
            {
                SessionId = sessionId;
                ModuleId = moduleId;
                ChoiceType = choiceType;
                ConfigType = configType;
            }
        }

        public override int ProcessBindRequest(Bind rpc)
        {
            if (rpc.Argument.LinkSids.Count == 0)
            {
                var providerSession = rpc.Sender.UserState as ProviderSession;
                foreach (var module in rpc.Argument.Modules)
                {
                    if (FirstModuleWithConfigTypeDefault == 0
                        && module.Value.ConfigType == BModule.ConfigTypeDefault)
                    {
                        FirstModuleWithConfigTypeDefault = module.Value.ConfigType;
                    }
                    var providerModuleState = new ProviderModuleState(providerSession.SessionId,
                        module.Key, module.Value.ChoiceType, module.Value.ConfigType);
                    var serviceName = MakeServiceName(providerSession.Info.ServiceNamePrefix, module.Key);
                    var subState = App.ServiceManagerAgent.SubscribeService(serviceName,
                        ServiceManager.SubscribeInfo.SubscribeTypeReadyCommit,
                        providerModuleState);
                    // 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
                    subState.SetServiceIdentityReadyState(providerSession.Info.ServiceIndentity, providerModuleState);
                    providerSession.StaticBinds.TryAdd(module.Key, module.Key);
                }
            }
            else
            {
                // 动态绑定
                foreach (var linkSid in rpc.Argument.LinkSids)
                {
                    var link = App.Instance.LinkdService.GetSocket(linkSid);
                    if (null != link)
                    {
                        var linkSession = link.UserState as LinkSession;
                        linkSession.Bind(link, rpc.Argument.Modules.Keys, rpc.Sender);
                    }
                }
            }
            rpc.SendResultCode(BBind.ResultSuccess);
            return Zeze.Transaction.Procedure.Success;
        }

        private void UnBindModules(Zeze.Net.AsyncSocket provider, IEnumerable<int> modules, bool isOnProviderClose = false)
        {
            var providerSession = provider.UserState as ProviderSession;
            foreach (var moduleId in modules)
            {
                if (false == isOnProviderClose)
                    providerSession.StaticBinds.TryRemove(moduleId, out var _);
                var serviceName = MakeServiceName(providerSession.Info.ServiceNamePrefix, moduleId);
                if (false == App.Instance.ServiceManagerAgent.SubscribeStates.TryGetValue(
                    serviceName, out var volatileProviders))
                {
                    continue;
                }
                // UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
                // 这里仅仅设置该moduleId对应的服务的状态不可用。
                volatileProviders.SetServiceIdentityReadyState(providerSession.Info.ServiceIndentity, null);
            }
        }
        public override int ProcessUnBindRequest(UnBind rpc)
        {
            if (rpc.Argument.LinkSids.Count == 0)
            {
                UnBindModules(rpc.Sender, rpc.Argument.Modules.Keys);
            }
            else
            {
                // 动态绑定
                foreach (var linkSid in rpc.Argument.LinkSids)
                {
                    var link = App.Instance.LinkdService.GetSocket(linkSid);
                    if (null != link)
                    {
                        var linkSession = link.UserState as LinkSession;
                        linkSession.UnBind(link, rpc.Argument.Modules.Keys, rpc.Sender);
                    }
                }
            }
            rpc.SendResultCode(BBind.ResultSuccess);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessSend(Send protocol)
        {
            foreach (var linkSid in protocol.Argument.LinkSids)
            {
                var link = App.Instance.LinkdService.GetSocket(linkSid);
                logger.Debug("Send {0} {1}", Zeze.Net.Protocol.GetModuleId(protocol.Argument.ProtocolType),
                    Zeze.Net.Protocol.GetProtocolId(protocol.Argument.ProtocolType));
                // ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
                link?.Send(protocol.Argument.ProtocolWholeData);
            }
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessBroadcast(Broadcast protocol)
        {
            App.Instance.LinkdService.Foreach((socket) =>
            {
                // auth 通过就允许发送广播。
                // 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
                // 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
                var linkSession = socket.UserState as LinkSession;
                if (null != linkSession && null != linkSession.UserId
                    // 这个状态是内部服务在CLogin的时候设置的，判断一下，可能可以简化客户端实现。
                    // 当然这个定义不是很好。但比较符合一般的使用。
                    && linkSession.UserStates.Count > 0
                    )
                    socket.Send(protocol.Argument.ProtocolWholeData);
            });
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessKick(Kick protocol)
        {
            App.Instance.LinkdService.ReportError(
                protocol.Argument.Linksid, Linkd.BReportError.FromProvider,
                protocol.Argument.Code, protocol.Argument.Desc);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessSetUserState(SetUserState protocol)
        {
            var socket = App.Instance.LinkdService.GetSocket(protocol.Argument.LinkSid);
            var linkSession = socket?.UserState as LinkSession;
            linkSession?.SetUserState(protocol.Argument.States, protocol.Argument.Statex);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessModuleRedirectRequest(ModuleRedirect rpc)
        {
            long SourceProvider = rpc.Sender.SessionId;
            long provider;

            if (ChoiceProvider(rpc.Argument.ServiceNamePrefix, rpc.Argument.ModuleId, rpc.Argument.HashCode, out provider))
            {
                rpc.Send(App.ProviderService.GetSocket(provider), (context) =>
                {
                    // process result。context == rpc
                    if (rpc.IsTimeout)
                        rpc.ResultCode = ModuleRedirect.ResultCodeLinkdTimeout;

                    rpc.Send(App.ProviderService.GetSocket(SourceProvider)); // send back to src provider
                    return Zeze.Transaction.Procedure.Success;
                });
                // async mode
            }
            else
            {
                rpc.SendResultCode(ModuleRedirect.ResultCodeLinkdNoProvider); // send back direct
            }
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessModuleRedirectAllRequest(ModuleRedirectAllRequest protocol)
        {
            Dictionary<long, ModuleRedirectAllRequest> transmits = new Dictionary<long, ModuleRedirectAllRequest>();

            ModuleRedirectAllResult miss = new ModuleRedirectAllResult();
            miss.Argument.ModuleId = protocol.Argument.ModuleId;
            miss.Argument.MethodFullName = protocol.Argument.MethodFullName;
            miss.Argument.SourceProvider = protocol.Sender.SessionId; // not used
            miss.Argument.SessionId = protocol.Argument.SessionId;
            miss.Argument.AutoKeyLocalId = 0; // 在这里没法知道逻辑服务器id，错误报告就不提供这个了。
            miss.ResultCode = ModuleRedirect.ResultCodeLinkdNoProvider;

            for (int i = 0; i < protocol.Argument.HashCodeConcurrentLevel; ++i)
            {
                long provider;
                if (ChoiceProvider(protocol.Argument.ServiceNamePrefix, protocol.Argument.ModuleId, i, out provider))
                {
                    if (false == transmits.TryGetValue(provider, out var exist))
                    {
                        exist = new ModuleRedirectAllRequest();
                        exist.Argument.ModuleId = protocol.Argument.ModuleId;
                        exist.Argument.HashCodeConcurrentLevel = protocol.Argument.HashCodeConcurrentLevel;
                        exist.Argument.MethodFullName = protocol.Argument.MethodFullName;
                        exist.Argument.SourceProvider = protocol.Sender.SessionId;
                        exist.Argument.SessionId = protocol.Argument.SessionId;
                        exist.Argument.Params = protocol.Argument.Params;
                        transmits.Add(provider, exist);
                    }
                    exist.Argument.HashCodes.Add(i);
                }
                else
                {
                    miss.Argument.Hashs[i] = new BModuleRedirectAllHash()
                    {
                        ReturnCode = Zeze.Transaction.Procedure.ProviderNotExist
                    };
                }
            }

            // 转发给provider
            foreach (var transmit in transmits)
            {
                var socket = App.ProviderService.GetSocket(transmit.Key);
                if (null != socket)
                {
                    transmit.Value.Send(socket);
                }
                else
                {
                    foreach (var hashindex in transmit.Value.Argument.HashCodes)
                    {
                        miss.Argument.Hashs[hashindex] = new BModuleRedirectAllHash()
                        {
                            ReturnCode = Zeze.Transaction.Procedure.ProviderNotExist
                        };
                    }
                }
            }

            // 没有转发成功的provider的hash分组，马上发送结果报告错误。
            if (miss.Argument.Hashs.Count > 0)
            {
                miss.Send(protocol.Sender);
            }
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessModuleRedirectAllResult(ModuleRedirectAllResult protocol)
        {
            var sourcerProvider = App.ProviderService.GetSocket(protocol.Argument.SourceProvider);
            if (null != sourcerProvider)
            {
                protocol.Send(sourcerProvider);
            }
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessReportLoad(ReportLoad protocol)
        {
            var providerSession = protocol.Sender.UserState as ProviderSession;
            providerSession?.SetLoad(protocol.Argument);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessTransmit(Transmit protocol)
        {
            // 查询 role 所在的 provider 并转发。
            var transmits = new Dictionary<long, Transmit>();
            // 如果 role 不在线，就根据 hash(roleId) 选择 provider 转发。
            var transmitsHash = new Dictionary<int, Transmit>();

            foreach (var target in protocol.Argument.Roles)
            {
                var provider = App.ProviderService.GetSocket(target.Value.ProviderSessionId);
                if (null == provider)
                {
                    var hash = target.Key.GetHashCode();
                    if (false == transmitsHash.TryGetValue(hash, out var transmitHash))
                    {
                        transmitHash = new Transmit();
                        transmitHash.Argument.ActionName = protocol.Argument.ActionName;
                        transmitHash.Argument.Sender = protocol.Argument.Sender;
                        transmitHash.Argument.ServiceNamePrefix = protocol.Argument.ServiceNamePrefix;
                        transmitsHash.Add(hash, transmitHash);
                    }
                    transmitHash.Argument.Roles.Add(target.Key, target.Value);
                    continue;
                }
                if (false == transmits.TryGetValue(target.Value.ProviderSessionId, out var transmit))
                {
                    transmit = new Transmit();
                    transmit.Argument.ActionName = protocol.Argument.ActionName;
                    transmit.Argument.Sender = protocol.Argument.Sender;
                    transmit.Argument.ServiceNamePrefix = protocol.Argument.ServiceNamePrefix;
                    transmits.Add(target.Value.ProviderSessionId, transmit);
                }
                transmit.Argument.Roles.Add(target.Key, target.Value);
            }

            // 已经绑定的会话，查找连接并转发，忽略连接查找错误。
            foreach (var transmit in transmits)
            {
                App.ProviderService.GetSocket(transmit.Key)?.Send(transmit.Value);
            }

            // 会话不存在，根据hash选择Provider并转发，忽略连接查找错误。
            foreach (var transmitHash in transmitsHash)
            {
                if (App.gnet_Provider.ChoiceProvider(
                    protocol.Argument.ServiceNamePrefix, FirstModuleWithConfigTypeDefault,
                    transmitHash.Key, out var provider))
                {
                    App.ProviderService.GetSocket(provider)?.Send(transmitHash.Value);
                }
            }

            return Zeze.Transaction.Procedure.Success;
        }

        // 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
        // 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
        // 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
        // 不建议在一个项目里面使用多个Prefix。
        public string GameServerServiceNamePrefix { get; private set; } = "";

        public override int ProcessAnnounceProviderInfo(AnnounceProviderInfo protocol)
        {
            var session = protocol.Sender.UserState as ProviderSession;
            session.Info = protocol.Argument;
            GameServerServiceNamePrefix = protocol.Argument.ServiceNamePrefix;
            return Zeze.Transaction.Procedure.Success;
        }
    }
}
