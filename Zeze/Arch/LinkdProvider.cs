
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Builtin.LinkdBase;
using Zeze.Builtin.Provider;
using Zeze.Services.ServiceManager;

namespace Zeze.Arch
{
    public class LinkdProvider : AbstractLinkdProvider
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public LinkdApp LinkdApp { get; set; }
        public ProviderDistribute Distribute { get; set; }

        // 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
        // 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
        // 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
        // 不建议在一个项目里面使用多个Prefix。
        public string ServerServiceNamePrefix { get; private set; } = "";

        protected override async Task<long> ProcessAnnounceProviderInfo(Zeze.Net.Protocol _p)
        {
            var protocol = _p as AnnounceProviderInfo;
            var session = protocol.Sender.UserState as LinkdProviderSession;
            session.Info = protocol.Argument;
            ServerServiceNamePrefix = protocol.Argument.ServiceNamePrefix;
            return Zeze.Transaction.Procedure.Success;
        }

        public int FirstModuleWithConfigTypeDefault { get; private set; } = 0;

        protected override async Task<long> ProcessBindRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Bind;
            if (rpc.Argument.LinkSids.Count == 0)
            {
                var providerSession = rpc.Sender.UserState as LinkdProviderSession;
                foreach (var module in rpc.Argument.Modules)
                {
                    if (FirstModuleWithConfigTypeDefault == 0
                        && module.Value.ConfigType == BModule.ConfigTypeDefault)
                    {
                        FirstModuleWithConfigTypeDefault = module.Value.ConfigType;
                    }
                    var providerModuleState = new ProviderModuleState(providerSession.SessionId,
                        module.Key, module.Value.ChoiceType, module.Value.ConfigType);
                    var serviceName = LinkdApp.LinkdProvider.Distribute.MakeServiceName(providerSession.Info.ServiceNamePrefix, module.Key);
                    var subState = await LinkdApp.Zeze.ServiceManagerAgent.SubscribeService(serviceName,
                        SubscribeInfo.SubscribeTypeReadyCommit,
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
                    var link = LinkdApp.LinkdService.GetSocket(linkSid);
                    if (null != link)
                    {
                        var linkSession = link.UserState as LinkdUserSession;
                        linkSession.Bind(LinkdApp.LinkdProviderService, link, rpc.Argument.Modules.Keys, rpc.Sender);
                    }
                }
            }
            rpc.SendResultCode(BBind.ResultSuccess);
            return Zeze.Transaction.Procedure.Success;
        }

        protected override async Task<long> ProcessBroadcast(Zeze.Net.Protocol p)
        {
            var protocol = p as Broadcast;
            if (protocol.Argument.ConfirmSerialId != 0)
            {
                var confirm = new SendConfirm();
                confirm.Argument.ConfirmSerialId = protocol.Argument.ConfirmSerialId;
                protocol.Sender.Send(confirm);
            }

            LinkdApp.LinkdService.Foreach((socket) =>
            {
                // auth 通过就允许发送广播。
                // 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
                // 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
                var linkSession = socket.UserState as LinkdUserSession;
                if (null != linkSession && null != linkSession.Account
                    // 这个状态是内部服务在CLogin的时候设置的，判断一下，可能可以简化客户端实现。
                    // 当然这个定义不是很好。但比较符合一般的使用。
                    && linkSession.UserStates.Count > 0
                    )
                    socket.Send(protocol.Argument.ProtocolWholeData);
            });
            return Zeze.Transaction.Procedure.Success;
        }

        protected override async Task<long> ProcessKick(Zeze.Net.Protocol p)
        {
            var protocol = p as Kick;
            LinkdApp.LinkdService.ReportError(
                protocol.Argument.Linksid, BReportError.FromProvider,
                protocol.Argument.Code, protocol.Argument.Desc);
            return Zeze.Transaction.Procedure.Success;
        }

        protected override async Task<long> ProcessSend(Zeze.Net.Protocol _p)
        {
            var protocol = _p as Send;
            // 这个是拿来处理乱序问题的：多个逻辑服务器之间，给客户端发送协议排队。
            // 所以不用等待真正发送给客户端，收到就可以发送结果。
            if (protocol.Argument.ConfirmSerialId != 0)
            {
                var confirm = new SendConfirm();
                confirm.Argument.ConfirmSerialId = protocol.Argument.ConfirmSerialId;
                protocol.Sender.Send(confirm);
            }

            foreach (var linkSid in protocol.Argument.LinkSids)
            {
                var link = LinkdApp.LinkdService.GetSocket(linkSid);
                logger.Debug("Send {0} {1}", Zeze.Net.Protocol.GetModuleId(protocol.Argument.ProtocolType),
                    Zeze.Net.Protocol.GetProtocolId(protocol.Argument.ProtocolType));
                // ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
                link?.Send(protocol.Argument.ProtocolWholeData);
            }
            return Zeze.Transaction.Procedure.Success;
        }

        protected override async Task<long> ProcessSetUserState(Zeze.Net.Protocol p)
        {
            var protocol = p as SetUserState;
            var socket = LinkdApp.LinkdService.GetSocket(protocol.Argument.LinkSid);
            var linkSession = socket?.UserState as LinkdUserSession;
            linkSession?.SetUserState(protocol.Argument.States, protocol.Argument.Statex);
            return Zeze.Transaction.Procedure.Success;
        }

        protected override async Task<long> ProcessSubscribeRequest(Zeze.Net.Protocol _p)
        {
            var rpc = (Zeze.Builtin.Provider.Subscribe)_p;

            var ps = (LinkdProviderSession)rpc.Sender.UserState;
            foreach (var module in rpc.Argument.Modules)
            {
                var providerModuleState = new ProviderModuleState(ps.SessionId,
                        module.Key, module.Value.ChoiceType, module.Value.ConfigType);
                var serviceName = LinkdApp.LinkdProvider.Distribute.MakeServiceName(ps.Info.ServiceNamePrefix, module.Key);
                var subState = await LinkdApp.Zeze.ServiceManagerAgent.SubscribeService(
                        serviceName, module.Value.SubscribeType, providerModuleState);
                // 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
                if (SubscribeInfo.SubscribeTypeReadyCommit == module.Value.SubscribeType)
                    subState.SetServiceIdentityReadyState(ps.Info.ServiceIndentity, providerModuleState);
            }

            rpc.SendResult();
            return 0;
        }

        private void UnBindModules(Zeze.Net.AsyncSocket provider, IEnumerable<int> modules, bool isOnProviderClose = false)
        {
            var ps = provider.UserState as LinkdProviderSession;
            foreach (var moduleId in modules)
            {
                if (false == isOnProviderClose)
                    ps.StaticBinds.TryRemove(moduleId, out var _);
                var serviceName = LinkdApp.LinkdProvider.Distribute.MakeServiceName(ps.Info.ServiceNamePrefix, moduleId);
                if (false == LinkdApp.Zeze.ServiceManagerAgent.SubscribeStates.TryGetValue(
                    serviceName, out var volatileProviders))
                {
                    continue;
                }
                // UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
                // 这里仅仅设置该moduleId对应的服务的状态不可用。
                volatileProviders.SetServiceIdentityReadyState(ps.Info.ServiceIndentity, null);
            }
        }
        protected override async Task<long> ProcessUnBindRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as UnBind;
            if (rpc.Argument.LinkSids.Count == 0)
            {
                UnBindModules(rpc.Sender, rpc.Argument.Modules.Keys);
            }
            else
            {
                // 动态绑定
                foreach (var linkSid in rpc.Argument.LinkSids)
                {
                    var link = LinkdApp.LinkdService.GetSocket(linkSid);
                    if (null != link)
                    {
                        var linkSession = link.UserState as LinkdUserSession;
                        linkSession.UnBind(LinkdApp.LinkdProviderService, link, rpc.Argument.Modules.Keys, rpc.Sender);
                    }
                }
            }
            rpc.SendResultCode(BBind.ResultSuccess);
            return Zeze.Transaction.Procedure.Success;
        }


        public bool ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, out long provider)
        {
            var serviceName = LinkdApp.LinkdProvider.Distribute.MakeServiceName(ServerServiceNamePrefix, moduleId);
            var linkSession = link.UserState as LinkdUserSession;

            provider = 0;
            if (false == LinkdApp.Zeze.ServiceManagerAgent.SubscribeStates.TryGetValue(
                serviceName, out var volatileProviders))
                return false;

            // 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
            // 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
            // 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
            var providerModuleState = volatileProviders.SubscribeInfo.LocalState as ProviderModuleState;

            switch (providerModuleState.ChoiceType)
            {
                case BModule.ChoiceTypeHashAccount:
                    return LinkdApp.LinkdProvider.Distribute.ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.Account), out provider);

                case BModule.ChoiceTypeHashRoleId:
                    if (linkSession.UserStates.Count > 0)
                    {
                        return LinkdApp.LinkdProvider.Distribute.ChoiceHash(volatileProviders, Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.UserStates[0]), out provider);
                    }
                    else
                    {
                        return false;
                    }

                case BModule.ChoiceTypeFeedFullOneByOne:
                    return LinkdApp.LinkdProvider.Distribute.ChoiceFeedFullOneByOne(volatileProviders, out provider);
            }

            // default
            if (LinkdApp.LinkdProvider.Distribute.ChoiceLoad(volatileProviders, out provider))
            {
                // 这里不判断null，如果失败让这次选择失败，否则选中了，又没有Bind以后更不好处理。
                var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider);
                var providerSession = providerSocket.UserState as LinkdProviderSession;
                linkSession.Bind(LinkdApp.LinkdProviderService, link, providerSession.StaticBinds.Keys, providerSocket);
                return true;
            }

            return false;
        }

        public void OnProviderClose(Zeze.Net.AsyncSocket provider)
        {
            var ps = provider.UserState as LinkdProviderSession;
            if (null == ps)
                return;

            // unbind module
            UnBindModules(provider, ps.StaticBinds.Keys, true);
            ps.StaticBinds.Clear();

            // unbind LinkSession
            lock (ps.LinkSessionIds)
            {
                foreach (var e in ps.LinkSessionIds)
                {
                    foreach (var linkSid in e.Value)
                    {
                        var link = LinkdApp.LinkdService.GetSocket(linkSid);
                        if (null != link)
                        {
                            var linkSession = link.UserState as LinkdUserSession;
                            linkSession?.UnBind(LinkdApp.LinkdProviderService, link, e.Key, provider, true);
                        }
                    }
                }
                ps.LinkSessionIds.Clear();
            }
        }

    }
}
