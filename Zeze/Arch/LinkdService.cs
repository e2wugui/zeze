using Zeze.Builtin.LinkdBase;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Arch
{
    public class LinkdService : HandshakeServer
    {
        private new static readonly ILogger logger = LogManager.GetLogger(typeof(LinkdService));

        public LinkdApp LinkdApp { get; set; }

        public LinkdService(string name, Application zeze)
            : base(name, zeze)
        {
            StableLinkSids = new ConcurrentLruLike<StableLinkSidKey, StableLinkSid>(1000000, TryLruRemove);
        }

        private void ReportError(Dispatch dispatch)
        {
            // 如果是 rpc.request 直接返回Procedure.Busy错误。
            // see Zeze.Net.Rpc.decode/encode
            var bb = ByteBuffer.Wrap(dispatch.Argument.ProtocolData);
            var compress = bb.ReadInt();
            var familyClass = compress & FamilyClass.FamilyClassMask;
            var isRequest = familyClass == FamilyClass.Request;
            if (isRequest)
            {
                if ((compress & FamilyClass.BitResultCode) != 0)
                    bb.SkipLong();
                var sessionId = bb.ReadLong();
                // argument 忽略，必须要解析出来，也不知道是什么。

                // 开始响应rpc.response.
                // 【注意】复用了上面的变量 bb，compress。
                compress = FamilyClass.Response;
                compress |= FamilyClass.BitResultCode;
                bb = ByteBuffer.Allocate();
                bb.WriteInt(compress);
                bb.WriteLong(ResultCode.Busy);
                bb.WriteLong(sessionId);
                EmptyBean.Instance.Encode(bb); // emptyBean对应任意bean的默认值状态。
                var so = GetSocket(dispatch.Argument.LinkSid);
                if (null != so)
                    so.Send(bb.Bytes, bb.ReadIndex, bb.Size);
            }
            // 报告服务器繁忙，但不关闭连接。
            ReportError(dispatch.Argument.LinkSid, BReportError.FromLink, BReportError.CodeProviderBusy, "provider is busy.", false);
        }

        public void ReportError(long linkSid, int from, int code, string desc)
        {
            ReportError(linkSid, from, code, desc, true);
        }

        public void ReportError(long linkSid, int from, int code, string desc, bool closeLink)
        {
            var link = this.GetSocket(linkSid);
            if (null != link)
            {
                var error = new ReportError();
                error.Argument.From = from;
                error.Argument.Code = code;
                error.Argument.Desc = desc;
                error.Send(link);

                switch (from)
                {
                    case BReportError.FromLink:
                        switch (code)
                        {
                            case BReportError.CodeNoProvider:
                                // 没有服务时，不断开连接，允许客户端重试。
                                return;
                        }
                        break;

                    case BReportError.FromProvider:
                        break;
                }
                // 延迟关闭。等待客户端收到错误以后主动关闭，或者超时。
                // 虽然使用了写完关闭(CloseGracefully)方法，但是等待一下，尽量让客户端主动关闭，有利于减少 TCP_TIME_WAIT?
                if (closeLink)
                    Scheduler.Schedule(_ => GetSocket(linkSid)?.CloseGracefully(), 2000);
            }
        }

        class StableLinkSidKey
        {
            // 同一个账号同一个ClientId只允许一个登录。
            // ClientId 可能的分配方式：每个手机Client分配一个，所有电脑Client分配一个。
            public string Account { get; }
            public string ClientId { get; }

            public StableLinkSidKey(string account, string clientId)
            {
                Account = account;
                ClientId = clientId;
            }

            public override int GetHashCode()
            {
                const int _prime_ = 31;
                int _h_ = 0;
                _h_ = _h_ * _prime_ + Account.GetHashCode();
                _h_ = _h_ * _prime_ + ClientId.GetHashCode();
                return _h_;
            }

            public override bool Equals(object obj)
            {
                if (obj == this)
                    return true;
                if (obj is StableLinkSidKey other)
                {
                    return Account.Equals(other.Account) && ClientId.Equals(other.ClientId);
                }
                return false;
            }
        }

        public class StableLinkSid
        {
            public bool Removed { get; set; }
            public long LinkSid { get; set; }
            public AsyncSocket AuthedSocket { get; set; }
        }

        private ConcurrentLruLike<StableLinkSidKey, StableLinkSid> StableLinkSids { get; set; }

        private bool TryLruRemove(StableLinkSidKey key, StableLinkSid value)
        {
            if (StableLinkSids.TryRemove(key, out var exist))
            {
                exist.Removed = true;
            }
            return true;
        }

        private void SetStableLinkSid(string account, string clientId, AsyncSocket client)
        {
            var key = new StableLinkSidKey(account, clientId);
            while (true)
            {
                var stable = StableLinkSids.GetOrAdd(key, (_) => new StableLinkSid());
                lock (stable)
                {
                    if (stable.Removed)
                        continue;

                    if (stable.AuthedSocket == client) // same client
                        return;

                    // Must Close Before Reuse LinkSid
                    stable.AuthedSocket?.Close(null);
                    if (stable.LinkSid != 0)
                    {
                        // Reuse Old LinkSid
                        client.SetSessionId(stable.LinkSid);
                    }
                    else
                    {
                        // first client
                        stable.LinkSid = client.SessionId;
                    }
                    stable.AuthedSocket = client;
                    //(client.UserState as LinkSession).StableLinkSid = stable;
                }
            }
        }

        private bool TryReportError(LinkdUserSession linkSession, int moduleId, Dispatch dispatch)
        {
            var pms = LinkdApp.LinkdProvider.GetProviderModuleState(moduleId);
            if (null == pms)
                return false;
            if (pms.ConfigType == BModule.ConfigTypeDynamic)
            {
                ReportError(linkSession.SessionId, BReportError.FromLink, BReportError.CodeNoProvider,
                        "no provider: " + moduleId + ", " + dispatch.ProtocolId);
                // 此后断开连接，不再继续搜索，返回true
                return true;
            }
            return false;
        }

        public bool FindSend(LinkdUserSession linkSession, int moduleId, Dispatch dispatch)
        {
            if (linkSession.TryGetProvider(moduleId, out var provider))
            {
                var socket = LinkdApp.LinkdProviderService.GetSocket(provider);
                if (null == socket)
                    return TryReportError(linkSession, moduleId, dispatch);

                var ps = (LinkdProviderSession)socket.UserState;
                if (ps.Load.Overload == BLoad.eOverload)
                {
                    // 过载时会直接拒绝请求以及报告错误。
                    ReportError(dispatch);
                    // 但是不能继续派发了。所以这里返回true，表示处理完成。
                    return true;
                }

                if (socket.Send(dispatch))
                    return true;

                return TryReportError(linkSession, moduleId, dispatch);
            }
            return false;
        }

        public override void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
        {
            var linkSession = so.UserState as LinkdUserSession;
            if (null == linkSession || string.IsNullOrEmpty(linkSession.Account))
            {
                ReportError(so.SessionId, BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
                return;
            }

            if (moduleId == Game.AbstractOnline.ModuleId && protocolId == Builtin.Game.Online.Login.ProtocolId_)
            {
                var login = new global::Zeze.Builtin.Game.Online.Login();
                login.Decode(ByteBuffer.Wrap(data));
                SetStableLinkSid(linkSession.Account, login.Argument.RoleId.ToString(), so);
            }
            else if (moduleId == AbstractOnline.ModuleId && protocolId == Builtin.Online.Login.ProtocolId_)
            {
                var login = new global::Zeze.Builtin.Online.Login();
                login.Decode(ByteBuffer.Wrap(data));
                SetStableLinkSid(linkSession.Account, login.Argument.ClientId, so);
            }

            var dispatch = new Dispatch();
            dispatch.Argument.LinkSid = so.SessionId;
            dispatch.Argument.Account = linkSession.Account;
            dispatch.Argument.ProtocolType = Protocol.MakeTypeId(moduleId, protocolId);
            dispatch.Argument.ProtocolData = new Binary(data);
            dispatch.Argument.Context = linkSession.Context;
            dispatch.Argument.Contextx = linkSession.Contextx;

            if (FindSend(linkSession, moduleId, dispatch))
                return;

            if (LinkdApp.LinkdProvider.ChoiceProviderAndBind(moduleId, so, out var provider))
            {
                var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider);
                if (null != providerSocket)
                {
                    // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
                    providerSocket.Send(dispatch);
                    return;
                }
                // 找到provider但是发送之前连接关闭，当作没有找到处理。这个窗口很小，再次查找意义不大。
            }
            ReportError(so.SessionId, BReportError.FromLink, BReportError.CodeNoProvider, "no provider.");
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                _ = Mission.CallAsync(factoryHandle.Handle, p);
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
                p.Sender.Close(null);
            }
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            sender.UserState = new LinkdUserSession(sender.SessionId);
            base.OnHandshakeDone(sender);
        }

        public override void OnSocketClose(AsyncSocket so, System.Exception e)
        {
            base.OnSocketClose(so, e);
            var linkSession = so.UserState as LinkdUserSession;
            linkSession?.OnClose(LinkdApp.LinkdProviderService);
        }

        public override bool Discard(AsyncSocket sender, int moduleId, int protocolId, int size)
        {
            /*
            【新修订：实现成忽略ProviderService的带宽过载配置，
            因为ProviderService的输入最终也会反映到LinkdService的输出。
            否则这里应该是max(LinkdService.Rate, ProviderService.Rate)】
            */
            var opt = SocketOptions.OverBandwidth;
            if (opt == null)
                return false; // disable
            var rate = 0.0; // todo 统计得到带宽。(double)this.getBandwidth() / opt;

            // 总控
            if (rate > SocketOptions.OverBandwidthFusingRate) // 1.0
                return true; // 熔断: discard all，其他级别在回调中处理。
            if (rate < SocketOptions.OverBandwidthNormalRate) // 0.7
                return false; // 整体负载小于0.6,全部不丢弃

            /*
            对于游戏可以针对【Move协议】使用下面的策略.
            if (moduleId == Map.ModuleId && protocolId == Map.Move.ProtocolId)
                return Zeze.Util.Random.getInstance().nextInt(100) < (int)((rate - 0.7) / (1.0 - 0.7) * 100);
            return false; // 其他协议全部不丢弃，除非达到熔断。
            */
            if (LinkdApp.DiscardAction != null)
                return LinkdApp.DiscardAction(sender, moduleId, protocolId, size, rate);

            // 应用没有定制丢弃策略，那么熔断前都不丢弃。
            return false;
        }
    }
}
