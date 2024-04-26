using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Builtin.Provider;
using Zeze.Services.ServiceManager;

namespace Zeze.Arch
{
    public class ProviderService : Zeze.Services.HandshakeClient
    {
        // private static readonly ILogger logger = LogManager.GetLogger(typeof(ProviderService));

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
        public static string GetLinkName(AsyncSocket sender)
        {
            return sender.Connector.Name;
        }

        public static string GetLinkName(Zeze.Services.ServiceManager.ServiceInfo serviceInfo)
        {
            return serviceInfo.PassiveIp + ":" + serviceInfo.PassivePort;
        }

        public void Kick(string linkName, long linkSid, int code, string desc)
        {
            if (linkSid != 0 && Links.TryGetValue(linkName, out var link))
                ProviderImplement.SendKick(link.TryGetReadySocket(), linkSid, code, desc);
        }

        public override void Start()
        {
            // copy Config.Connector to Links
            Config.ForEachConnector((c) => Links.TryAdd(c.Name, c));
            base.Start();
            LinkConnectors = Links.ToArray();
        }

        public bool ApplyPut(ServiceInfo link)
        {
            var linkName = GetLinkName(link);
            var isNew = false;
            Links.GetOrAdd(linkName, __ =>
            {
                if (Config.TryGetOrAddConnector(link.PassiveIp, link.PassivePort, true, out var outC))
                {
                    outC.Start();
                    isNew = true;
                }
                return outC;
            });
            return isNew;
        }

        public bool ApplyRemove(ServiceInfo link)
        {
            var linkName = GetLinkName(link);
            if (Links.TryRemove(linkName, out var removed))
            {
                Config.RemoveConnector(removed);
                removed.Stop();
                return true;
            }
            return false;
        }

        public void RefreshLinkConnectors()
        {
            LinkConnectors = Links.ToArray();
        }

        public class LinkSession
        {
            public string Name { get; }
            public long SessionId { get; }

            public LinkSession(string name, long sid)
            {
                Name = name;
                SessionId = sid;
            }
        }

        public ConcurrentDictionary<string, Connector> Links { get; } = new();

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
            rpc.Send(sender, (protocol) => { ProviderStaticBindCompleted.SetResult(true); return Task.FromResult(0L); });
            var sub = new Builtin.Provider.Subscribe();
            sub.Argument.Modules.AddRange(ProviderApp.DynamicModules);
            sub.Send(sender, (protocol) => { ProviderDynamicSubscribeCompleted.SetResult(true); return Task.FromResult(0L); });
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
