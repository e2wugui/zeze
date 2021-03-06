﻿
namespace gnet.Linkd
{
    public sealed partial class ModuleLinkd : AbstractModule
    {
        public void Start(gnet.App app)
        {
        }

        public void Stop(gnet.App app)
        {
        }

        public override int ProcessAuthRequest(Auth rpc)
        {
            /*
            BAccount account = _taccount.Get(protocol.Argument.Account);
            if (null == account || false == account.Token.Equals(protocol.Argument.Token))
            {
                result.Send(protocol.Sender);
                return Zeze.Transaction.Procedure.LogicError;
            }

            Game.App.Instance.LinkdService.GetSocket(account.SocketSessionId)?.Dispose(); // kick, 最好发个协议再踢。如果允许多个连接，去掉这行。
            account.SocketSessionId = protocol.Sender.SessionId;
            */
            var linkSession = rpc.Sender.UserState as LinkSession;
            linkSession.UserId = rpc.Argument.Account;
            rpc.SendResultCode(Auth.Success);

            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessKeepAlive(KeepAlive protocol)
        {
            var linkSession = protocol.Sender.UserState as LinkSession;
            if (null == linkSession)
            {
                // handshake 完成之前不可能回收得到 keepalive，先这样处理吧。
                protocol.Sender.Close(null);
                return Zeze.Transaction.Procedure.LogicError;
            }
            linkSession.KeepAlive();
            protocol.Sender.Send(protocol); // send back;
            return Zeze.Transaction.Procedure.Success;
        }
    }
}
