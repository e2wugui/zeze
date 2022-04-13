// auto generate
namespace Zeze.Arch
{
    public abstract class AbstractProviderImplement : Zeze.IModule 
    {
        public const int ModuleId = 11008;
        public override string FullName => "Zeze.Beans.Provider";
        public override string Name => "Provider";
        public override int Id => ModuleId;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47282968534786, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.AnnounceLinkInfo(),
                Handle = ProcessAnnounceLinkInfo,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAnnounceLinkInfop", Zeze.Transaction.TransactionLevel.None),
            });
            service.AddFactoryHandle(47282301515237, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Bind(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47282067822559, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Dispatch(),
                Handle = ProcessDispatch,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDispatchp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47280680546638, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.LinkBroken(),
                Handle = ProcessLinkBroken,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLinkBrokenp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47281317762384, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.SendConfirm(),
                Handle = ProcessSendConfirm,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendConfirmp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47282665133980, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Subscribe(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47280773808911, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.UnBind(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47282968534786, out var _);
            service.Factorys.TryRemove(47282301515237, out var _);
            service.Factorys.TryRemove(47282067822559, out var _);
            service.Factorys.TryRemove(47280680546638, out var _);
            service.Factorys.TryRemove(47281317762384, out var _);
            service.Factorys.TryRemove(47282665133980, out var _);
            service.Factorys.TryRemove(47280773808911, out var _);
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

        protected abstract System.Threading.Tasks.Task<long>  ProcessSendConfirm(Zeze.Net.Protocol p);
    }
}
