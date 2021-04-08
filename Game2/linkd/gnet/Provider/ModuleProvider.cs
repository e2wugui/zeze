
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

        /// <summary>
        /// under lock (StaticBinds) or readonly
        /// </summary>
        public class Providers
        {
            // HashSet改成List，新增的机器放在后面，负载均衡时，从后往前搜索，会更快一些。
            // 后台服务不会随便增删，不需要高效Add,Remove。
            private List<long> ProviderSessionIds { get; } = new List<long>();
            public int ChoiceType { get; }

            public Providers DeepCopy()
            {
                var copy = new Providers(ChoiceType);
                copy.ProviderSessionIds.AddRange(ProviderSessionIds);
                return copy;
            }

            public Providers(int choiceType)
            {
                ChoiceType = choiceType;
            }

            public void AddProvider(long provider)
            {
                if (ProviderSessionIds.Contains(provider))
                    return;
                ProviderSessionIds.Add(provider);
            }

            public int RemoveProvider(long provider)
            {
                ProviderSessionIds.Remove(provider);
                return ProviderSessionIds.Count;
            }

            /// <summary>
            /// 加权负载均衡
            /// </summary>
            /// <param name="provider"></param>
            /// <returns></returns>
            public bool Choice(out long provider)
            {
                var frees = new List<ProviderSession>(ProviderSessionIds.Count);
                var all = new List<ProviderSession>(ProviderSessionIds.Count);
                int TotalWeight = 0;

                // 新的provider在后面，从后面开始搜索。
                for (int i = ProviderSessionIds.Count - 1; i >= 0; --i)
                {
                    var ps = App.Instance.ProviderService.GetSocket(ProviderSessionIds[i])?.UserState as ProviderSession;
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
                provider = 0;
                return false;
            }

            public bool Choice(int hash, out long provider)
            {
                if (ProviderSessionIds.Count == 0)
                {
                    provider = 0;
                    return false;
                }
                provider = ProviderSessionIds[hash % ProviderSessionIds.Count];
                return true;
            }
        }

        private Dictionary<int, Providers> StaticBinds { get; } = new Dictionary<int, Providers>();
        private volatile Dictionary<int, Providers> StaticBindsCopy = new Dictionary<int, Providers>();

        // under lock (StaticBinds)
        private void StaticBindsCopyNow()
        {
            var tmp = new Dictionary<int, Providers>();
            foreach (var sb in StaticBinds)
            {
                tmp.Add(sb.Key, sb.Value.DeepCopy());
            }
            StaticBindsCopy = tmp;
        }

        public bool ChoiceProvider(int moduleId, int hash, out long provider)
        {
            // avoid lock
            var tmp = StaticBindsCopy;
            if (tmp.TryGetValue(moduleId, out var providers))
            {
                if (providers.Choice(hash, out provider))
                {
                    return true;
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
                    switch (providers.ChoiceType)
                    {
                        case BBind.ChoiceTypeHashUserId:
                            return providers.Choice(Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.UserId), out provider);

                        case BBind.ChoiceTypeHashRoleId:
                            if (linkSession.UserStates.Count > 0)
                            {
                                return providers.Choice(Zeze.Serialize.ByteBuffer.calc_hashnr(linkSession.UserStates[0]), out provider);
                            }
                            else
                            {
                                provider = 0;
                                return false;
                            }
                    }
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

        public int FirstStaticBindModuleId()
        { 
            lock (StaticBinds)
            {
                foreach (var moduleId in StaticBinds.Keys)
                {
                    return moduleId;
                }
                return 0;
            }
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
                        binds.AddProvider(rpc.Sender.SessionId);
                    }
                    StaticBindsCopyNow();
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
                        if (binds.RemoveProvider(provider.SessionId) == 0)
                            StaticBinds.Remove(moduleId);
                    }
                }
                StaticBindsCopyNow();
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
                if (ChoiceProvider(protocol.Argument.ModuleId, i, out provider))
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

            // 随便找一个静态绑定的ModuleId，用来查找玩家所在的Provider。
            var firstModuleId = FirstStaticBindModuleId();
            foreach (var target in protocol.Argument.Role2LinkSid)
            {
                var linkSession = App.LinkdService.GetSocket(target.Value)?.UserState as LinkSession;
                if (null == linkSession
                    || false == linkSession.TryGetProvider(firstModuleId, out var provider))
                {
                    var hash = target.Key.GetHashCode();
                    if (false == transmitsHash.TryGetValue(hash, out var transmitHash))
                    {
                        transmitHash = new Transmit();
                        transmitHash.Argument.ActionName = protocol.Argument.ActionName;
                        transmitHash.Argument.Sender = protocol.Argument.Sender;
                        transmitsHash.Add(hash, transmitHash);
                    }
                    transmitHash.Argument.Role2LinkSid.Add(target.Key, target.Value);
                    continue;
                }
                if (false == transmits.TryGetValue(provider, out var transmit))
                {
                    transmit = new Transmit();
                    transmit.Argument.ActionName = protocol.Argument.ActionName;
                    transmit.Argument.Sender = protocol.Argument.Sender;
                    transmits.Add(provider, transmit);
                }
                transmit.Argument.Role2LinkSid.Add(target.Key, target.Value);
            }

            // 已经绑定的会话，查找连接并转发，忽略连接查找错误。
            foreach (var transmit in transmits)
            {
                App.ProviderService.GetSocket(transmit.Key)?.Send(transmit.Value);
            }

            // 会话不存在，根据hash选择Provider并转发，忽略连接查找错误。
            foreach (var transmitHash in transmitsHash)
            {
                if (App.gnet_Provider.ChoiceProvider(firstModuleId, transmitHash.Key, out var provider))
                    App.ProviderService.GetSocket(provider)?.Send(transmitHash.Value);
            }

            return Zeze.Transaction.Procedure.Success;
        }
    }
}
