// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Arch
{
    public abstract class AbstractProviderDirect : Zeze.IModule 
    {
        public const int ModuleId = 11009;
        public override string FullName => "Zeze.Arch.ProviderDirect";
        public override string Name => "ProviderDirect";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public const int ErrorTransmitParameterFactoryNotFound = 1;

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47286041114986, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.ProviderDirect.AnnounceProviderInfo(),
                Handle = ProcessAnnounceProviderInfoRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAnnounceProviderInfoRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAnnounceProviderInfoRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47284402955566, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.ProviderDirect.ModuleRedirect(),
                Handle = ProcessModuleRedirectRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessModuleRedirectRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessModuleRedirectRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47286816262188, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest(),
                Handle = ProcessModuleRedirectAllRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessModuleRedirectAllRequestp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessModuleRedirectAllRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47283400371444, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult(),
                Handle = ProcessModuleRedirectAllResult,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessModuleRedirectAllResultp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessModuleRedirectAllResult", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47284197108752, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.ProviderDirect.Transmit(),
                Handle = ProcessTransmit,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessTransmitp", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessTransmit", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47284247217006, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.ProviderDirect.TransmitAccount(),
                Handle = ProcessTransmitAccount,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessTransmitAccountp", Zeze.Transaction.TransactionLevel.None),
                Mode = _reflect.GetDispatchMode("ProcessTransmitAccount", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47286041114986, out var _);
            service.Factorys.TryRemove(47284402955566, out var _);
            service.Factorys.TryRemove(47286816262188, out var _);
            service.Factorys.TryRemove(47283400371444, out var _);
            service.Factorys.TryRemove(47284197108752, out var _);
            service.Factorys.TryRemove(47284247217006, out var _);
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

        protected abstract System.Threading.Tasks.Task<long>  ProcessTransmitAccount(Zeze.Net.Protocol p);
    }
}
