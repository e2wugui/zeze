
namespace Zeze.Arch
{
    public class Online : AbstractOnline
    {
        protected override async System.Threading.Tasks.Task<long> ProcessLoginRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.Online.Login;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessLogoutRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.Online.Logout;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.Online.ReliableNotifyConfirm;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessReLoginRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.Online.ReLogin;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
