using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Beans.LinkdBase;
using Zeze.Beans.Provider;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Arch
{
    public class LinkdService : Zeze.Services.HandshakeServer
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public LinkdApp LinkdApp { get; set; }

        public LinkdService(string name, Application zeze)
            : base(name, zeze)
        {
        }

        public void ReportError(long linkSid, int from, int code, string desc)
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
                Scheduler.Schedule((ThisTask) => this.GetSocket(linkSid)?.Dispose(), 2000);
            }
        }

        public override void DispatchUnknownProtocol(
            Zeze.Net.AsyncSocket so,
            int moduleId,
            int protocolId,
            Zeze.Serialize.ByteBuffer data)
        {
            var linkSession = so.UserState as LinkdUserSession;
            if (null == linkSession || null == linkSession.Account)
            {
                ReportError(so.SessionId, BReportError.FromLink, BReportError.CodeNotAuthed, "not authed.");
                return;
            }

            var dispatch = new Dispatch();
            dispatch.Argument.LinkSid = so.SessionId;
            dispatch.Argument.Account = linkSession.Account;
            dispatch.Argument.ProtocolType = Protocol.MakeTypeId(moduleId, protocolId);
            dispatch.Argument.ProtocolData = new Zeze.Net.Binary(data);
            dispatch.Argument.States.AddRange(linkSession.UserStates);
            dispatch.Argument.Statex = linkSession.UserStatex;

            long provider;
            if (linkSession.TryGetProvider(moduleId, out provider))
            {
                var socket = LinkdApp.LinkdProviderService.GetSocket(provider);
                if (null != socket)
                {
                    socket.Send(dispatch);
                    return;
                }
                // 原来绑定的provider找不到连接，尝试继续从静态绑定里面查找。
                // 此时应该处于 UnBind 过程中。
                //linkSession.UnBind(so, moduleId, null);
            }

            if (LinkdApp.LinkdProvider.ChoiceProviderAndBind(moduleId, so, out provider))
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

        public override void DispatchProtocol(Zeze.Net.Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                try
                {
                    var isRequestSaved = p.IsRequest;
                    var task = factoryHandle.Handle(p);
                    task.Wait();
                    var result = task.Result; // 不启用新的Task，直接在io-thread里面执行。
                    Mission.LogAndStatistics(null, result, p, isRequestSaved);
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
            sender.UserState = new LinkdUserSession(sender.SessionId);
            base.OnHandshakeDone(sender);
        }

        public override void OnSocketClose(Zeze.Net.AsyncSocket so, System.Exception e)
        {
            base.OnSocketClose(so, e);
            var linkSession = so.UserState as LinkdUserSession;
            linkSession?.OnClose(LinkdApp.LinkdProviderService);
        }
    }
}
