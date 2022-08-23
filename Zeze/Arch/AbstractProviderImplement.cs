// auto generate
namespace Zeze.Arch
{
    public abstract class AbstractProviderImplement : Zeze.IModule 
    {
        public const int ModuleId = 11008;
        public override string FullName => "Zeze.Arch.ProviderImplement";
        public override string Name => "ProviderImplement";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47281374674071, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.AnnounceLinkInfo(),
                Handle = ProcessAnnounceLinkInfo,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAnnounceLinkInfop", Zeze.Transaction.TransactionLevel.None),
            });
            service.AddFactoryHandle(47279114253990, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Bind(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47280285301785, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Dispatch(),
                Handle = ProcessDispatch,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDispatchp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47281652939086, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.LinkBroken(),
                Handle = ProcessLinkBroken,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLinkBrokenp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47281226998238, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Send(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47280110454586, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.Subscribe(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47281107578964, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.Provider.UnBind(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47281374674071, out var _);
            service.Factorys.TryRemove(47279114253990, out var _);
            service.Factorys.TryRemove(47280285301785, out var _);
            service.Factorys.TryRemove(47281652939086, out var _);
            service.Factorys.TryRemove(47281226998238, out var _);
            service.Factorys.TryRemove(47280110454586, out var _);
            service.Factorys.TryRemove(47281107578964, out var _);
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


        protected abstract System.Threading.Tasks.Task<long>  ProcessAnnounceLinkInfo(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessDispatch(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessLinkBroken(Zeze.Net.Protocol p);
    }
}
