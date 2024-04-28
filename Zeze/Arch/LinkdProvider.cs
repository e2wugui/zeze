using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Builtin.LinkdBase;
using Zeze.Builtin.Provider;
using Zeze.Services.ServiceManager;
using Zeze.Util;

namespace Zeze.Arch
{
    public class LinkdProvider : AbstractLinkdProvider
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(LinkdProvider));
        public LinkdApp LinkdApp { get; set; }
        public ProviderDistributeVersion Distribute { get; set; }

        // 用于客户端选择Provider，只支持一种Provider。如果要支持多种，需要客户端增加参数，这个不考虑了。
        // 内部的ModuleRedirect ModuleRedirectAll Transmit都携带了ServiceNamePrefix参数，所以，
        // 内部的Provider可以支持完全不同的solution，不过这个仅仅保留给未来扩展用，
        // 不建议在一个项目里面使用多个Prefix。
        public string ServerServiceNamePrefix { get; private set; } = "";

        protected override Task<long> ProcessAnnounceProviderInfo(Zeze.Net.Protocol _p)
        {
            var protocol = _p as AnnounceProviderInfo;
            var session = protocol.Sender.UserState as LinkdProviderSession;
            session.Info = protocol.Argument;
            ServerServiceNamePrefix = protocol.Argument.ServiceNamePrefix;
            return Task.FromResult(ResultCode.Success);
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
                    var serviceName = ProviderDistribute.MakeServiceName(providerSession.Info.ServiceNamePrefix, module.Key);
                    var subState = await LinkdApp.Zeze.ServiceManager.SubscribeService(serviceName, providerModuleState);
                    // 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
                    // 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
                    subState.SetIdentityLocalState(providerSession.Info.ServiceIndentity, providerModuleState);
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
            return ResultCode.Success;
        }

        protected override Task<long> ProcessBroadcast(Zeze.Net.Protocol p)
        {
            var protocol = p as Broadcast;
            LinkdApp.LinkdService.Foreach((socket) =>
            {
                // auth 通过就允许发送广播。
                // 如果要实现 role.login 才允许，Provider 增加 SetLogin 协议给内部server调用。
                // 这些广播一般是重要通告，只要登录客户端就允许收到，然后进入世界的时候才显示。这样处理就不用这个状态了。
                if (socket.UserState is LinkdUserSession linkSession && null != linkSession.Account
                    // 这个状态是内部服务在CLogin的时候设置的，判断一下，可能可以简化客户端实现。
                    // 当然这个定义不是很好。但比较符合一般的使用。
                    && false == string.IsNullOrEmpty(linkSession.Context)
                    )
                    socket.Send(protocol.Argument.ProtocolWholeData);
            });
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessKick(Zeze.Net.Protocol p)
        {
            var protocol = p as Kick;
            LinkdApp.LinkdService.ReportError(
                protocol.Argument.Linksid, BReportError.FromProvider,
                protocol.Argument.Code, protocol.Argument.Desc);
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessSendRequest(Zeze.Net.Protocol _p)
        {
            var r = _p as Send;
            foreach (var linkSid in r.Argument.LinkSids)
            {
                var link = LinkdApp.LinkdService.GetSocket(linkSid);
                logger.Debug("Send {0} {1}", Zeze.Net.Protocol.GetModuleId(r.Argument.ProtocolType),
                    Zeze.Net.Protocol.GetProtocolId(r.Argument.ProtocolType));
                // ProtocolId现在是hash值，显示出来也不好看，以后加配置换成名字。
                if (null != link && link.Send(r.Argument.ProtocolWholeData))
                    continue;
                if (null != link)
                    link.Close(null);
                else
                    r.Result.ErrorLinkSids.Add(linkSid);
            }
            r.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessSetUserState(Zeze.Net.Protocol p)
        {
            var protocol = p as SetUserState;
            var socket = LinkdApp.LinkdService.GetSocket(protocol.Argument.LinkSid);
            var linkSession = socket?.UserState as LinkdUserSession;
            linkSession?.SetUserState(protocol.Argument.Context, protocol.Argument.Contextx);
            return Task.FromResult(ResultCode.Success);
        }

        protected override async Task<long> ProcessSubscribeRequest(Zeze.Net.Protocol _p)
        {
            var rpc = (Zeze.Builtin.Provider.Subscribe)_p;

            var ps = (LinkdProviderSession)rpc.Sender.UserState;
            foreach (var module in rpc.Argument.Modules)
            {
                var providerModuleState = new ProviderModuleState(ps.SessionId,
                        module.Key, module.Value.ChoiceType, module.Value.ConfigType);
                var serviceName = ProviderDistribute.MakeServiceName(ps.Info.ServiceNamePrefix, module.Key);
                var subState = await LinkdApp.Zeze.ServiceManager.SubscribeService(serviceName, providerModuleState);
                // 订阅成功以后，仅仅需要设置ready。service-list由Agent维护。
                // 即使 SubscribeTypeSimple 也需要设置 Ready，因为 providerModuleState 需要设置到ServiceInfo中，以后Choice的时候需要用。
                subState.SetIdentityLocalState(ps.Info.ServiceIndentity, providerModuleState);
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
                var serviceName = ProviderDistribute.MakeServiceName(ps.Info.ServiceNamePrefix, moduleId);
                if (false == LinkdApp.Zeze.ServiceManager.SubscribeStates.TryGetValue(
                    serviceName, out var volatileProviders))
                {
                    continue;
                }
                // UnBind 不删除provider-list，这个总是通过ServiceManager通告更新。
                // 这里仅仅设置该moduleId对应的服务的状态不可用。
                volatileProviders.SetIdentityLocalState(ps.Info.ServiceIndentity, null);
            }
        }
        protected override Task<long> ProcessUnBindRequest(Zeze.Net.Protocol p)
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
            return Task.FromResult(ResultCode.Success);
        }

        public string MakeServiceName(int moduleId)
        {
            return ProviderDistribute.MakeServiceName(ServerServiceNamePrefix, moduleId);
        }

        public ProviderModuleState GetProviderModuleState(int moduleId)
        {
            if (false == Distribute.Zeze.ServiceManager.SubscribeStates.TryGetValue(MakeServiceName(moduleId), out var providers))
                return null;
            return (ProviderModuleState)providers.SubscribeInfo.LocalState;
        }


        public bool ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, out long provider)
        {
            var serviceName = ProviderDistribute.MakeServiceName(ServerServiceNamePrefix, moduleId);
            var linkSession = link.UserState as LinkdUserSession;

            provider = 0;
            if (false == LinkdApp.Zeze.ServiceManager.SubscribeStates.TryGetValue(serviceName, out var providers))
                return false;

            // 这里保存的 ProviderModuleState 是该moduleId的第一个bind请求去订阅时记录下来的，
            // 这里仅使用里面的ChoiceType和ConfigType。这两个参数对于相同的moduleId都是一样的。
            // 如果需要某个provider.SessionId，需要查询 ServiceInfoListSortedByIdentity 里的ServiceInfo.LocalState。
            var providerModuleState = providers.SubscribeInfo.LocalState as ProviderModuleState;

            var distribute = LinkdApp.LinkdProvider.Distribute.SelectDistribute(0); // TODO version from client
            if (null == distribute)
                return false;

            switch (providerModuleState.ChoiceType)
            {
                case BModule.ChoiceTypeHashAccount:
                    return distribute.ChoiceHash(providers, FixedHash.Hash32(linkSession.Account), out provider);

                case BModule.ChoiceTypeHashRoleId:
                    var roleId = linkSession.RoleId;
                    if (null != roleId)
                    {
                        return distribute.ChoiceHash(providers, FixedHash.calc_hashnr(roleId.Value), out provider);
                    }
                    else
                    {
                        return false;
                    }

                case BModule.ChoiceTypeFeedFullOneByOne:
                    return distribute.ChoiceFeedFullOneByOne(providers, out provider);
            }

            // default
            if (distribute.ChoiceLoad(providers, out provider))
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
            if (provider.UserState is not LinkdProviderSession ps)
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
