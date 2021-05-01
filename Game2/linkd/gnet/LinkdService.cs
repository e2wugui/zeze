
using System.Collections.Generic;

namespace gnet
{
    public sealed partial class LinkdService
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public void ReportError(long linkSid, int from, int code, string desc)
        {
            var link = this.GetSocket(linkSid);
            if (null != link)
            {
                var error = new Linkd.ReportError();
                error.Argument.From = from;
                error.Argument.Code = code;
                error.Argument.Desc = desc;
                error.Send(link);

                switch (from)
                {
                    case Linkd.BReportError.FromLink:
                        switch (code)
                        {
                            case Linkd.BReportError.CodeNoProvider:
                                // 没有服务时，不断开连接，允许客户端重试。
                                return;
                        }
                        break;

                    case Linkd.BReportError.FromProvider:
                        break;
                }
                // 延迟关闭。等待客户端收到错误以后主动关闭，或者超时。
                App.Instance.Scheduler.Schedule((ThisTask) => this.GetSocket(linkSid)?.Dispose(), 2000);
            }
        }

        public override void DispatchUnknownProtocol(Zeze.Net.AsyncSocket so, int type, Zeze.Serialize.ByteBuffer data)
        {
            var linkSession = so.UserState as LinkSession;
            if (null == linkSession || null == linkSession.UserId)
            {
                ReportError(so.SessionId, Linkd.BReportError.FromLink, Linkd.BReportError.CodeNotAuthed, "not authed.");
                return;
            }

            var moduleId = global::Zeze.Net.Protocol.GetModuleId(type);
            var dispatch = new Provider.Dispatch();
            dispatch.Argument.LinkSid = so.SessionId;
            dispatch.Argument.UserId = linkSession.UserId;
            dispatch.Argument.ProtocolType = type;
            dispatch.Argument.ProtocolData = new Zeze.Net.Binary(data);
            dispatch.Argument.States.AddRange(linkSession.UserStates);
            dispatch.Argument.Statex = linkSession.UserStatex;

            long provider;
            if (linkSession.TryGetProvider(moduleId, out provider))
            {
                var socket = App.Instance.ProviderService.GetSocket(provider);
                if (null != socket)
                {
                    socket.Send(dispatch);
                    return;
                }
                // 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
                // 此时应该处于 UnBind 过程中。
                //linkSession.UnBind(so, moduleId, null);
            }

            if (App.Instance.gnet_Provider.ChoiceProviderAndBind(moduleId, so, out provider))
            {
                var providerSocket = App.Instance.ProviderService.GetSocket(provider);
                if (null != providerSocket)
                {
                    // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
                    providerSocket.Send(dispatch);
                    return;
                }
                // 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
            }
            ReportError(so.SessionId, Linkd.BReportError.FromLink, Linkd.BReportError.CodeNoProvider, "no provider.");
        }

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
                    p.Sender.Close(ex); // link 在异常时关闭连接。
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
                p.Sender.Close(null);
            }
        }

        public override void OnHandshakeDone(Zeze.Net.AsyncSocket sender)
        {
            base.OnHandshakeDone(sender);
            sender.UserState = new LinkSession(sender.SessionId);
        }

        public override void OnSocketClose(Zeze.Net.AsyncSocket so, System.Exception e)
        {
            base.OnSocketClose(so, e);
            var linkSession = so.UserState as LinkSession;
            linkSession?.OnClose();
        }
    }

    public class LinkSession
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public string UserId { get; set; } // 为了接入更广泛的用户系统，使用string。
        public List<long> UserStates { get; } = new List<long>();
        public Zeze.Net.Binary UserStatex { get; private set; } = Zeze.Net.Binary.Empty;

        private Dictionary<int, long> Binds { get; set; } = new Dictionary<int, long>(); // moduleId -> InnerService.SessionId

        public long SessionId { get; }

        public LinkSession(long sessionId)
        {
            SessionId = sessionId;
        }

        public void SetUserState(ICollection<long> states, Zeze.Net.Binary statex)
        {
            lock (this) // 简单使用一下这个锁。
            {
                UserStates.Clear();
                UserStates.AddRange(states);
                UserStatex = statex;
            }
        }

        public bool TryGetProvider(int moduleId, out long provider)
        {
            lock (this)
            {
                return Binds.TryGetValue(moduleId, out provider);
            }
        }

        public void Bind(Zeze.Net.AsyncSocket link, IEnumerable<int> moduleIds, Zeze.Net.AsyncSocket provider)
        {
            lock (this)
            {
                foreach (var moduleId in moduleIds)
                {
                    if (Binds.TryGetValue(moduleId, out var exist))
                    {
                        var oldSocket = App.Instance.ProviderService.GetSocket(exist);
                        logger.Warn("LinkSession.Bind replace provider {0} {1} {2}", moduleId,
                            oldSocket.Socket.RemoteEndPoint, provider.Socket.RemoteEndPoint);
                    }
                    Binds[moduleId] = provider.SessionId;
                    (provider.UserState as ProviderSession).AddLinkSession(moduleId, link.SessionId);
                }
            }
        }

        public void UnBind(Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider, bool isOnProviderClose = false)
        {
            UnBind(link, new HashSet<int>() { moduleId }, provider, isOnProviderClose);
        }

        public void UnBind(Zeze.Net.AsyncSocket link, IEnumerable<int> moduleIds, Zeze.Net.AsyncSocket provider, bool isOnProviderClose = false)
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
                                (provider.UserState as ProviderSession)?.RemoveLinkSession(moduleId, link.SessionId);
                        }
                        else
                        {
                            var oldSocket = App.Instance.ProviderService.GetSocket(exist);
                            logger.Warn("LinkSession.UnBind not owner {0} {1} {2}", moduleId,
                                oldSocket.Socket.RemoteEndPoint, provider.Socket.RemoteEndPoint);
                        }
                    }
                }
            }
        }

        // 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
        private Zeze.Util.SchedulerTask KeepAliveTask;

        public void KeepAlive()
        {
            KeepAliveTask?.Cancel();
            KeepAliveTask = App.Instance.Scheduler.Schedule((ThisTask) =>
            {
                App.Instance.LinkdService.GetSocket(SessionId)?.Close(null);
            }, 3000000);
        }

        public void OnClose()
        {
            KeepAliveTask?.Cancel();

            if (null == UserId)
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

            var linkBroken = new gnet.Provider.LinkBroken();
            linkBroken.Argument.UserId = UserId;
            linkBroken.Argument.LinkSid = SessionId;
            linkBroken.Argument.States.AddRange(UserStates);
            linkBroken.Argument.Statex = UserStatex;
            linkBroken.Argument.Reason = Provider.BLinkBroken.REASON_PEERCLOSE; // 这个保留吧。现在没什么用。

            // 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
            foreach (var e in bindsSwap)
            {
                var provider = App.Instance.ProviderService.GetSocket(e.Value);
                if (null == provider)
                    continue;
                var providerSession = provider.UserState as ProviderSession;
                if (null == providerSession)
                    continue;

                provider.Send(linkBroken);
                providerSession.RemoveLinkSession(e.Key, SessionId);
            }
        }
    }
}
