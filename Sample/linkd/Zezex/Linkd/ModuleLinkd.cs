
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Net;
using Zeze.Util;

namespace Zezex.Linkd
{
    public sealed partial class ModuleLinkd : AbstractModule
    {
        public void Start(Zezex.App app)
        {
        }

        public void Stop(Zezex.App app)
        {
        }

        protected override Task<long> ProcessAuthRequest(Protocol p)
        {
            var rpc = p as Auth;
            /*
            BAccount account = _taccount.Get(protocol.Argument.Account);
            if (null == account || false == account.Token.Equals(protocol.Argument.Token))
            {
                result.Send(protocol.Sender);
                return ResultCode.LogicError;
            }

            Game.App.Instance.LinkdService.GetSocket(account.SocketSessionId)?.Dispose(); // kick, 最好发个协议再踢。如果允许多个连接，去掉这行。
            account.SocketSessionId = protocol.Sender.SessionId;
            */
            var linkSession = rpc.Sender.UserState as LinkdUserSession;
            linkSession.Account = rpc.Argument.Account;
            rpc.SendResultCode(Auth.Success);

            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessKeepAlive(Protocol p)
        {
            var protocol = p as KeepAlive;
            if (protocol.Sender.UserState is not LinkdUserSession linkSession)
            {
                // handshake 完成之前不可能回收得到 keepalive，先这样处理吧。
                protocol.Sender.Close(null);
                return Task.FromResult(ResultCode.LogicError);
            }
            linkSession.KeepAlive(App.Instance.LinkdService);
            protocol.Sender.Send(protocol); // send back;
            return Task.FromResult(ResultCode.Success);
        }
    }
}
