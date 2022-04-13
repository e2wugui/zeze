// auto generate
namespace Zeze.Arch
{
    public abstract class AbstractProviderDirect : Zeze.IModule 
    {
        public const int ModuleId = 11009;
        public override string FullName => "Zeze.Beans.ProviderDirect";
        public override string Name => "ProviderDirect";
        public override int Id => ModuleId;


        public const int ErrorTransmitParameterFactoryNotFound = 1;

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47283356221296, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.ProviderDirect.AnnounceProviderInfo(),
                Handle = ProcessAnnounceProviderInfoRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAnnounceProviderInfoRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47286708377899, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.ProviderDirect.ModuleRedirect(),
                Handle = ProcessModuleRedirectRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessModuleRedirectRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47286357293504, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest(),
                Handle = ProcessModuleRedirectAllRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessModuleRedirectAllRequestp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47286982651743, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.ProviderDirect.ModuleRedirectAllResult(),
                Handle = ProcessModuleRedirectAllResult,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessModuleRedirectAllResultp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47284548257601, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.ProviderDirect.Transmit(),
                Handle = ProcessTransmit,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessTransmitp", Zeze.Transaction.TransactionLevel.None),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47283356221296, out var _);
            service.Factorys.TryRemove(47286708377899, out var _);
            service.Factorys.TryRemove(47286357293504, out var _);
            service.Factorys.TryRemove(47286982651743, out var _);
            service.Factorys.TryRemove(47284548257601, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }


        protected abstract System.Threading.Tasks.Task<long>  ProcessAnnounceProviderInfoRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessModuleRedirectRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessModuleRedirectAllRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessModuleRedirectAllResult(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessTransmit(Zeze.Net.Protocol p);
    }
}
