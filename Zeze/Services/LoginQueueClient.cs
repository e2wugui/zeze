
namespace Zeze.Services
{
    public class LoginQueueClient : AbstractLoginQueueClient
    {
        protected override async System.Threading.Tasks.Task<long> ProcessPutLoginToken(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.LoginQueue.PutLoginToken;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessPutQueueFull(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.LoginQueue.PutQueueFull;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessPutQueuePosition(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.LoginQueue.PutQueuePosition;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
