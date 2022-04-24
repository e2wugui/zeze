using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;

namespace Zeze.Arch
{
    public class LinkdProviderSession : ProviderSession
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
        /// 多线程：这里面的数据访问都处于 lock (Zezex.App.Instance.gnet_Provider_Module.StaticBinds) 下
        /// see Zezex.Provider.ModuleProvider
        /// </summary>
        public ConcurrentDictionary<int, int> StaticBinds { get; } = new ConcurrentDictionary<int, int>();
        public BAnnounceProviderInfo Info { get; set; }

        public LinkdProviderSession(long sid)
        {
            base.SessionId = sid;
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
                linkSids.Add(linkSessionId);
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
