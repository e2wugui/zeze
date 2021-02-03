
using System.Collections.Generic;

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

        public class Providers
        {
            // 保留计数，可以知道moduleId的访问次数。
            public HashSet<long> ProviderSessionIds { get; } = new HashSet<long>();
            public int ChoiceType { get; }
            public Zeze.Util.AtomicLong AccessCount { get; } = new Zeze.Util.AtomicLong();
            private long[] ProviderSessionIdArray;

            public Providers(int choiceType)
            {
                ChoiceType = choiceType;
            }

            public void SetupChoice()
            {
                ProviderSessionIdArray = new long[ProviderSessionIds.Count];
                ProviderSessionIds.CopyTo(ProviderSessionIdArray);
            }

            public bool Choice(out long provider)
            {
                // 现在是顺序轮转。
                // 根据ProviderSession.Count选择。但是又不能一下子都选它。
                if (ProviderSessionIdArray.Length == 0)
                {
                    provider = 0;
                    return false;
                }
                provider = ProviderSessionIdArray[(int)(AccessCount.IncrementAndGet() % ProviderSessionIdArray.Length)];
                return true;
            }

            public bool Choice(int hash, out long provider)
            {
                if (ProviderSessionIdArray.Length == 0)
                {
                    provider = 0;
                    return false;
                }
                provider = ProviderSessionIdArray[hash % ProviderSessionIdArray.Length];
                return true;
            }
        }

        private Dictionary<int, Providers> StaticBinds { get; } = new Dictionary<int, Providers>();

        public bool ChoiceProvider(int moduleId, int hash, out long provider)
        {
            // TODO 这个用于provider之间转发请求，需要优化并发。考虑实现方案：volatile StaticBindsCopy。不锁。
            lock (StaticBinds)
            {
                if (StaticBinds.TryGetValue(moduleId, out var providers))
                {
                    if (providers.Choice(hash, out provider))
                    {
                        return true;
                    }
                }
            }
            provider = 0;
            return false;
        }

        public bool ChoiceProviderAndBind(int moduleId, Zeze.Net.AsyncSocket link, out long provider)
        {
            var linkSession = link.UserState as LinkSession;
            lock (StaticBinds)
            {
                if (StaticBinds.TryGetValue(moduleId, out var providers))
                {
                    if (providers.Choice(out provider))
                    {
                        var providerSocket = gnet.App.Instance.ProviderService.GetSocket(provider);
                        var providerSession = providerSocket.UserState as ProviderSession;
                        linkSession.Bind(link, providerSession.StaticBinds, providerSocket);
                        return true;
                    }
                }
            }
            provider = 0;
            return false;
        }

        public void OnProviderClose(Zeze.Net.AsyncSocket provider)
        {
            ProviderSession providerSession = provider.UserState as ProviderSession;
            if (null == providerSession)
                return;

            lock (StaticBinds)
            {
                // unbind module
                UnBindModules(provider, providerSession.StaticBinds, true);
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
        }

        public override int ProcessBindRequest(Bind rpc)
        {
            if (rpc.Argument.LinkSids.Count == 0)
            {
                lock (StaticBinds)
                {
                    var providerSession = rpc.Sender.UserState as ProviderSession;
                    foreach (var module in rpc.Argument.Modules)
                    {
                        providerSession.StaticBinds.Add(module.Key);
                        if (false == StaticBinds.TryGetValue(module.Key, out var binds))
                        {
                            binds = new Providers(module.Value);
                            StaticBinds.Add(module.Key, binds);
                        }
                        binds.ProviderSessionIds.Add(rpc.Sender.SessionId);
                        binds.SetupChoice();
                    }
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
                        linkSession.Bind(link, rpc.Argument.Modules.Keys2, rpc.Sender);
                    }
                }
            }
            rpc.SendResultCode(BBind.ResultSuccess);
            return Zeze.Transaction.Procedure.Success;
        }

        private void UnBindModules(Zeze.Net.AsyncSocket provider, IEnumerable<int> modules, bool isOnProviderClose = false)
        {
            lock (StaticBinds)
            {
                var providerSession = provider.UserState as ProviderSession;
                foreach (var moduleId in modules)
                {
                    if (false == isOnProviderClose)
                        providerSession.StaticBinds.Remove(moduleId);
                    if (StaticBinds.TryGetValue(moduleId, out var binds))
                    {
                        binds.ProviderSessionIds.Remove(provider.SessionId);
                        binds.SetupChoice();
                        if (binds.ProviderSessionIds.Count == 0)
                            StaticBinds.Remove(moduleId);
                    }
                }
            }
        }
        public override int ProcessUnBindRequest(UnBind rpc)
        {
            if (rpc.Argument.LinkSids.Count == 0)
            {
                UnBindModules(rpc.Sender, rpc.Argument.Modules.Keys2);
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
                        linkSession.UnBind(link, rpc.Argument.Modules.Keys2, rpc.Sender);
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
            App.Instance.LinkdService.Foreach((socket)=>
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

            if (ChoiceProvider(rpc.Argument.ModuleId, rpc.Argument.HashCode, out provider))
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
    }
}
