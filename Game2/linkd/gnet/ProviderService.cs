

using System.Collections.Concurrent;
using System.Collections.Generic;

namespace gnet
{
    public sealed partial class ProviderService
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        // 重载需要的方法。
        public override void DispatchProtocol(Zeze.Net.Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                try
                {
                    factoryHandle.Handle(p); // 不启用新的Task，直接在io-thread里面执行。
                }
                catch (System.Exception ex)
                {
                    logger.Log(SocketOptions.SocketLogLevel, ex, "Protocol.Handle. {0}", p);
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
            }
        }

        public override void OnHandshakeDone(Zeze.Net.AsyncSocket sender)
        {
            base.OnHandshakeDone(sender);
            sender.UserState = new ProviderSession(sender.SessionId);
        }

        public override void OnSocketClose(Zeze.Net.AsyncSocket so, System.Exception e)
        {
            // 先unbind。这样避免有时间窗口。
            gnet.App.Instance.gnet_Provider.OnProviderClose(so);
            base.OnSocketClose(so, e);
        }
    }

    public class ProviderSession
    {
        /// <summary>
        /// 维护此Provider上绑定的LinkSession，用来在Provider关闭的时候，进行 UnBind。
        /// moduleId -> LinkSids
        /// 多线程：主要由LinkSession回调.  需要保护。
        /// 
        /// </summary>
        public Dictionary<int, HashSet<long>> LinkSessionIds { get; } = new Dictionary<int, HashSet<long>>();

        /// <summary>
        /// 维护此Provider上绑定的StaticBinds，用来在Provider关闭的时候，进行 UnBind。
        /// 同时，当此Provider第一次被选中时，所有的StaticBinds都会一起被绑定到LinkSession上，
        /// 多线程：这里面的数据访问都处于 lock (gnet.App.Instance.gnet_Provider_Module.StaticBinds) 下
        /// see gnet.Provider.ModuleProvider
        /// </summary>
        public HashSet<int> StaticBinds { get; } = new HashSet<int>();
        private Provider.BLoad Load { get; set; }
        public int ProposeMaxOnline => Load.ProposeMaxOnline;
        public int Online => Load.Online;
        public int OnlineNew => Load.OnlineNew;

        public long SessionId { get; }

        public ProviderSession(long ssid)
        {
            SessionId = ssid;
        }

        public void SetLoad(Provider.BLoad load)
        {
            lock (LinkSessionIds)
            {
                Load.Assign(load); // 复制一次吧。
            }
        }

        public void AddLinkSession(int moduleId, long linkSessionId)
        {
            lock (LinkSessionIds)
            {
                if (false == LinkSessionIds.TryGetValue(moduleId, out var linkSids))
                {
                    linkSids = new HashSet<long>();
                    LinkSessionIds.Add(moduleId, linkSids);
                }
                if (linkSids.Add(linkSessionId))
                {
                    ++Load.Online;
                    // 在真正的数据报告回来之前，临时增加统计。仅包括本linkd分配的。
                    // 本来Load应该总是由Provider报告的。
                    // linkd 的临时增加是为了能快速反应出报告间隔期间的分配。
                }
            }
        }

        public void RemoveLinkSession(int moduleId, long linkSessionId)
        {
            lock (LinkSessionIds)
            {
                if (LinkSessionIds.TryGetValue(moduleId, out var linkSids))
                {
                    if (linkSids.Remove(linkSessionId))
                    {
                        // 下线时Provider会进行统计，这里避免二次计数，
                        // 没有扣除不会有问题，本来Load应该总是由Provider报告的。
                        //--Load.Online;
                        if (linkSids.Count == 0)
                            LinkSessionIds.Remove(moduleId);
                    }
                }
            }
        }
    }
}
