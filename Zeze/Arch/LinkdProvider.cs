
namespace Zeze.Arch
{
    public class LinkdProvider : AbstractLinkdProvider
    {
        protected override System.Threading.Tasks.Task<long> ProcessAnnounceProviderInfo(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.AnnounceProviderInfo;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessBindRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.Bind;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessBroadcast(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.Broadcast;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessKick(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.Kick;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessSend(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.Send;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessSetUserState(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.SetUserState;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessSubscribeRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.Subscribe;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessUnBindRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.UnBind;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
