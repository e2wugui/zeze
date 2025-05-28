using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;

namespace Zeze.Arch
{
    public class ProviderUserSession
    {
        public ProviderService Service { get; }
        public string Account { get; }
        public string Context { get; }
        public long? RoleId => string.IsNullOrEmpty(Context) ? null : long.Parse(Context);

        public string LinkName { get; }
        public long LinkSid { get; } // 客户端在linkd上的SessionId

        public AsyncSocket Link { get; set; }

        public ProviderUserSession(ProviderService service, string account, string context, AsyncSocket link, long linkSid)
        {
            Service = service;

            Account = account;
            Context = context;
            LinkSid = linkSid;
            Link = link;
            LinkName = ProviderService.GetLinkName(link);
        }

        public void Kick(int code, string desc)
        {
            ProviderImplement.SendKick(Link, LinkSid, code, desc);
        }

        public void SendResponse(Zeze.Net.Binary fullEncodedProtocol)
        {
            SendResponse(Zeze.Serialize.ByteBuffer.Wrap(fullEncodedProtocol).ReadInt4(), fullEncodedProtocol);
        }

        private void SendOnline(AsyncSocket link, Send send)
        {
            if (Service.ProviderApp.ProviderImplement is ProviderImplementWithOnline arch)
            {
                var context = new Dictionary<long, (string, string)>();
                context.Add(LinkSid, (Account, Context));
                arch.Online.Send(link, context, send);
            }
            else if (Service.ProviderApp.ProviderImplement is Game.ProviderImplementWithOnline game)
            {
                var context = new Dictionary<long, long>();
                context.Add(LinkSid, RoleId.Value);
                game.Online.Send(link, context, send);
            }
            else
            {
                link.Send(send);
            }
        }

        public void SendResponse(long typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            var send = new Send();
            send.Argument.LinkSids.Add(LinkSid);
            send.Argument.ProtocolType = typeId;
            send.Argument.ProtocolWholeData = fullEncodedProtocol;

            if (null != Link && !Link.Closed)
            {
                SendOnline(Link, send);
                return;
            }
            // 可能发生了重连，尝试再次查找发送。网络断开以后，已经不可靠了，先这样写着吧。
            if (Service.Links.TryGetValue(LinkName, out var link))
            {
                if (link.IsHandshakeDone)
                {
                    Link = link.Socket;
                    SendOnline(Link, send);
                }
            }
        }

        public void SendResponse(Protocol p)
        {
            p.IsRequest = false;
            SendResponse(p.TypeId, new Zeze.Net.Binary(p.Encode()));
        }

        public void SendResponseWhileCommit(int typeId, Binary fullEncodedProtocol)
        {
            Zeze.Transaction.Transaction.Current.RunWhileCommit(() => SendResponse(typeId, fullEncodedProtocol));
        }

        public void SendResponseWhileCommit(Binary fullEncodedProtocol)
        {
            Zeze.Transaction.Transaction.Current.RunWhileCommit(() => SendResponse(fullEncodedProtocol));
        }

        public void SendResponseWhileCommit(Protocol p)
        {
            Zeze.Transaction.Transaction.Current.RunWhileCommit(() => SendResponse(p));
        }

        // 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
        private void SendResponseWhileRollback(int typeId, Binary fullEncodedProtocol)
        {
            Zeze.Transaction.Transaction.Current.RunWhileRollback(() => SendResponse(typeId, fullEncodedProtocol));
        }

        private void SendResponseWhileRollback(Binary fullEncodedProtocol)
        {
            Zeze.Transaction.Transaction.Current.RunWhileRollback(() => SendResponse(fullEncodedProtocol));
        }

        public void SendResponseWhileRollback(Protocol p)
        {
            Zeze.Transaction.Transaction.Current.RunWhileRollback(() => SendResponse(p));
        }

        public static ProviderUserSession Get(Protocol context)
        {
            if (null == context.UserState)
                throw new Exception("not auth");
            return (ProviderUserSession)context.UserState;
        }
    }
}
