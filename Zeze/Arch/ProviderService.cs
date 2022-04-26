using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Builtin.Provider;
using Zeze.Builtin.ProviderDirect;

namespace Zeze.Arch
{
    public class ProviderService : Zeze.Services.HandshakeClient
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public ProviderApp ProviderApp;

        public ProviderService(String name, Zeze.Application zeze)
            : base(name, zeze)
        {
        }

        /// <summary>
        /// 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
        /// 这里要求 linkName 在所有 provider 中都一样。
        /// 使用 Connector 配置得到名字，只要保证配置一样。
        /// </summary>
        /// <param name="sender"></param>
        /// <returns></returns>
        public string GetLinkName(AsyncSocket sender)
        {
            return sender.Connector.Name;
        }

        public string GetLinkName(Zeze.Services.ServiceManager.ServiceInfo serviceInfo)
        {
            return serviceInfo.PassiveIp + ":" + serviceInfo.PassivePort;
        }

        public override void Start()
        {
            // copy Config.Connector to Links
            Config.ForEachConnector((c) => Links.TryAdd(c.Name, c));
            base.Start();
            LinkConnectors = Links.ToArray();
        }

        public void ApplyLinksChanged(Zeze.Services.ServiceManager.ServiceInfos serviceInfos)
        {
            var current = new HashSet<string>();
            foreach (var link in serviceInfos.SortedIdentity)
            {
                var linkName = GetLinkName(link);
                current.Add(Links.GetOrAdd(linkName, (key) =>
                {
                    if (Config.TryGetOrAddConnector(link.PassiveIp, link.PassivePort, true, out var c))
                    {
                        c.Start();
                    }
                    return c;
                }).Name);
            }
            // 删除多余的连接器。
            foreach (var linkName in Links.Keys)
            {
                if (current.Contains(linkName))
                    continue;
                if (Links.TryRemove(linkName, out var removed))
                {
                    Config.RemoveConnector(removed);
                    removed.Stop();
                }
            }
            LinkConnectors = Links.ToArray();
        }

        public class LinkSession
        {
            public string Name { get; }
            public long SessionId { get; }

            // 在和linkd连接建立完成以后，由linkd发送通告协议时保存。
            public int LinkId { get; private set; } // reserve
            public long ProviderSessionId { get; private set; }

            public LinkSession(string name, long sid)
            {
                Name = name;
                SessionId = sid;
            }

            public void Setup(int linkId, long providerSessionId)
            {
                LinkId = linkId;
                ProviderSessionId = providerSessionId;
            }
        }

        public ConcurrentDictionary<string, Connector> Links { get; }
            = new ConcurrentDictionary<string, Connector>();
        private volatile KeyValuePair<string, Connector>[] LinkConnectors;
        private readonly Zeze.Util.AtomicInteger LinkRandomIndex = new();
        public AsyncSocket RandomLink()
        {
            var volatileTmp = LinkConnectors;
            if (volatileTmp.Length == 0)
                return null;
            var index = (uint)LinkRandomIndex.IncrementAndGet();
            var connector = volatileTmp[index % (uint)volatileTmp.Length].Value;
            // 如果只选择已经连上的Link，当所有的连接都没准备好时，仍然需要GetReadySocket，
            // 所以简单处理成总是等待连接完成。
            return connector.GetReadySocket();
        }

        // 用来同步等待Provider的静态绑定完成。
        public TaskCompletionSource<bool> ProviderStaticBindCompleted = new();
        public TaskCompletionSource<bool> ProviderDynamicSubscribeCompleted = new();

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            base.OnHandshakeDone(sender);
            var linkName = GetLinkName(sender);
            sender.UserState = new LinkSession(linkName, sender.SessionId);

            var announce = new Zeze.Builtin.Provider.AnnounceProviderInfo();
            announce.Argument.ServiceNamePrefix = ProviderApp.ServerServiceNamePrefix;
            announce.Argument.ServiceIndentity = Zeze.Config.ServerId.ToString();
            announce.Send(sender);

            // static binds
            var rpc = new Bind();
            rpc.Argument.Modules.AddRange(ProviderApp.StaticBinds);
            rpc.Send(sender, async (protocol) => { ProviderStaticBindCompleted.SetResult(true); return 0; });
            var sub = new Subscribe();
            sub.Argument.Modules.AddRange(ProviderApp.DynamicModules);
            sub.Send(sender, async (protocol) => { ProviderDynamicSubscribeCompleted.SetResult(true); return 0; });
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            // 防止Client不进入加密，直接发送用户协议。
            if (false == IsHandshakeProtocol(p.TypeId))
                p.Sender.VerifySecurity();

            base.DispatchProtocol(p, factoryHandle);
        }

        /*
        public void ReportLoad(int online, int proposeMaxOnline, int onlineNew)
        {
            var report = new Zezex.Provider.ReportLoad();

            report.Argument.Online = online;
            report.Argument.ProposeMaxOnline = proposeMaxOnline;
            report.Argument.OnlineNew = onlineNew;

            foreach (var link in Links.Values)
            {
                if (link.IsHandshakeDone)
                {
                    link.Socket.Send(report);
                }
            }
        }
        */
    }
}
