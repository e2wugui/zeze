using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Net;
using Zeze.Transaction;

namespace Game.Login
{
    public class Session
    {
        public string Account { get; }
        public long? RoleId { get; }

        public string LinkName { get; }
        public long SessionId { get; } // 客户端在linkd上的SessionId

        public AsyncSocket Link { get; set; }

        public Session(string account, IList<long> states, AsyncSocket link, long linkSid)
        {
            Account = account;
            RoleId = states.Count == 0 ? null : states[0];
            SessionId = linkSid;
            Link = link;
            LinkName = Game.App.Instance.Server.GetLinkName(link);
        }

        public void SendResponse(Zeze.Net.Binary fullEncodedProtocol)
        {
            SendResponse(Zeze.Serialize.ByteBuffer.Wrap(fullEncodedProtocol).ReadInt4(), fullEncodedProtocol);
        }

        public void SendResponse(long typeId, Zeze.Net.Binary fullEncodedProtocol)
        {
            var send = new Zezex.Provider.Send();
            send.Argument.LinkSids.Add(SessionId);
            send.Argument.ProtocolType = typeId;
            send.Argument.ProtocolWholeData = fullEncodedProtocol;

            if (null != Link && null != Link.Socket)
            {
                Link.Send(send);
                return;
            }
            // 可能发生了重连，尝试再次查找发送。网络断开以后，已经不可靠了，先这样写着吧。
            if (Game.App.Instance.Server.Links.TryGetValue(LinkName, out var link))
            {
                if (link.IsHandshakeDone)
                {
                    Link = link.Socket;
                    link.Socket.Send(send);
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
            Transaction.Current.RunWhileCommit(() => SendResponse(typeId, fullEncodedProtocol));
        }

        public void SendResponseWhileCommit(Binary fullEncodedProtocol)
        {
            Transaction.Current.RunWhileCommit(() => SendResponse(fullEncodedProtocol));
        }

        public void SendResponseWhileCommit(Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => SendResponse(p));
        }

        // 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
        private void SendResponseWhileRollback(int typeId, Binary fullEncodedProtocol)
        {
            Transaction.Current.RunWhileRollback(() => SendResponse(typeId, fullEncodedProtocol));
        }

        private void SendResponseWhileRollback(Binary fullEncodedProtocol)
        {
            Transaction.Current.RunWhileRollback(() => SendResponse(fullEncodedProtocol));
        }

        public void SendResponseWhileRollback(Protocol p)
        {
            Transaction.Current.RunWhileRollback(() => SendResponse(p));
        }

        public static Session Get(Protocol context)
        {
            if (null == context.UserState)
                throw new Exception("not auth");
            return (Session)context.UserState;
        }
    }
}
