using System.Collections.Generic;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Arch
{
    public class LinkdUserSession
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(LinkdUserSession));

        public string Account { get; set; } // 为了接入更广泛的用户系统，使用string。
        public string Context { get; private set; } = "";
        public Zeze.Net.Binary Contextx { get; private set; } = Zeze.Net.Binary.Empty;

        private Dictionary<int, long> Binds { get; set; } = new Dictionary<int, long>(); // moduleId -> InnerService.SessionId

        public long SessionId { get; }

        public long? RoleId => string.IsNullOrEmpty(Context) ? null : long.Parse(Context);

        public LinkdUserSession(long sessionId)
        {
            SessionId = sessionId;
        }

        public void SetUserState(string context, Zeze.Net.Binary contextx)
        {
            Context = context;
            Contextx = contextx;
        }

        public bool TryGetProvider(int moduleId, out long provider)
        {
            lock (this)
            {
                return Binds.TryGetValue(moduleId, out provider);
            }
        }

        public void Bind(LinkdProviderService linkdProviderService,
            Zeze.Net.AsyncSocket link, IEnumerable<int> moduleIds, Zeze.Net.AsyncSocket provider)
        {
            lock (this)
            {
                foreach (var moduleId in moduleIds)
                {
                    if (Binds.TryGetValue(moduleId, out var exist))
                    {
                        var oldSocket = linkdProviderService.GetSocket(exist);
                        logger.Warn("LinkSession.Bind replace provider {0} {1} {2}", moduleId,
                            oldSocket.RemoteAddress, provider.RemoteAddress);
                    }
                    Binds[moduleId] = provider.SessionId;
                    (provider.UserState as LinkdProviderSession).AddLinkSession(moduleId, link.SessionId);
                }
            }
        }

        public void UnBind(LinkdProviderService linkdProviderService, 
            Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider, bool isOnProviderClose = false)
        {
            UnBind(linkdProviderService, link, new HashSet<int>() { moduleId }, provider, isOnProviderClose);
        }

        public void UnBind(LinkdProviderService linkdProviderService, 
            Zeze.Net.AsyncSocket link, IEnumerable<int> moduleIds, Zeze.Net.AsyncSocket provider, bool isOnProviderClose = false)
        {
            lock (this)
            {
                foreach (var moduleId in moduleIds)
                {
                    if (Binds.TryGetValue(moduleId, out var exist))
                    {
                        if (exist == provider.SessionId) // check owner? 也许不做这个检测更好？
                        {
                            Binds.Remove(moduleId);
                            if (false == isOnProviderClose)
                                (provider.UserState as LinkdProviderSession)?.RemoveLinkSession(moduleId, link.SessionId);
                        }
                        else
                        {
                            var oldSocket = linkdProviderService.GetSocket(exist);
                            logger.Warn("LinkSession.UnBind not owner {0} {1} {2}", moduleId,
                                oldSocket.RemoteAddress, provider.RemoteAddress);
                        }
                    }
                }
            }
        }

        // 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
        private Zeze.Util.SchedulerTask KeepAliveTask;

        public void KeepAlive(LinkdService linkdService)
        {
            KeepAliveTask?.Cancel();
            KeepAliveTask = Scheduler.Schedule((ThisTask) =>
            {
                linkdService.GetSocket(SessionId)?.Close(null);
            }, 300000);
        }

        public void OnClose(LinkdProviderService linkdProviderService)
        {
            KeepAliveTask?.Cancel();

            if (null == Account)
            {
                // 未验证通过的不通告。此时Binds肯定是空的。
                return;
            }

            Dictionary<int, long> bindsSwap = null;
            lock (this)
            {
                bindsSwap = Binds;
                Binds = new Dictionary<int, long>();
            }

            var linkBroken = new LinkBroken();
            linkBroken.Argument.Account = Account;
            linkBroken.Argument.LinkSid = SessionId;
            linkBroken.Argument.Context = Context;
            linkBroken.Argument.Contextx = Contextx;
            linkBroken.Argument.Reason = BLinkBroken.REASON_PEERCLOSE; // 这个保留吧。现在没什么用。

            // 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
            HashSet<AsyncSocket> bindProviders = new();
            foreach (var e in bindsSwap)
            {
                var provider = linkdProviderService.GetSocket(e.Value);
                if (null == provider)
                    continue;
                var ps = provider.UserState as LinkdProviderSession;
                if (null == ps)
                    continue;

                ps.RemoveLinkSession(e.Key, SessionId);
                bindProviders.Add(provider); // 先收集，去重。
            }
            foreach (var provider in bindProviders)
            {
                provider.Send(linkBroken);
            }
        }
    }
}
