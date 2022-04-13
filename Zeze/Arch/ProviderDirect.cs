
namespace Zeze.Arch
{
    public class ProviderDirect : AbstractProviderDirect
    {
        protected override async System.Threading.Tasks.Task<long> ProcessAnnounceProviderInfoRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessModuleRedirectRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.ProviderDirect.ModuleRedirect;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessModuleRedirectAllRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessModuleRedirectAllResult(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override async System.Threading.Tasks.Task<long> ProcessTransmit(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Beans.ProviderDirect.Transmit;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
