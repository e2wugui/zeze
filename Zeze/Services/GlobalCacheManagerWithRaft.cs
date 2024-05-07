
namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaft : AbstractGlobalCacheManagerWithRaft
    {
        protected override async System.Threading.Tasks.Task<long> ProcessAcquireRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessCleanupRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessKeepAliveRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessLoginRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessNormalCloseRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose;
            return Zeze.Util.ResultCode.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessReLoginRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
