using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Net;

namespace Game.Login
{
    public class Session
    {
        public string Account { get; }
        // 改成 Linkd(Gate 方式)，这里应该保存 linksid
        public long SessionId { get; }
        public long? LoginRoleId { get; set; } // warning: not thread safe

        public Session(string account, AsyncSocket socket)
        {
            Account = account;
            SessionId = socket.SessionId;
        }

        public void SendResponse(Protocol p)
        {
            AsyncSocket socket = Game.App.Instance.Server.GetSocket(SessionId);
            if (null != socket)
            {
                p.Send(socket);
            }
            else
            {
                // lost connection, log here.
            }
        }

        public static Session Get(Protocol context)
        {
            if (null == context.UserState)
                throw new Exception("not auth");
            return (Session)context.UserState;
        }
    }
}
