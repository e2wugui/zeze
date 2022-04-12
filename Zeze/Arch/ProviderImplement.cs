
namespace Zeze.Arch
{
    public class ProviderImplement : AbstractProviderImplement
    {
        protected override System.Threading.Tasks.Task<long> ProcessAnnounceLinkInfo(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.AnnounceLinkInfo;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessDispatch(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.Dispatch;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessLinkBroken(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.LinkBroken;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override System.Threading.Tasks.Task<long> ProcessSendConfirm(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.Provider.SendConfirm;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
