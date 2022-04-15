// auto generate
namespace Zeze.Arch
{
    public abstract class AbstractLinkdProvider : Zeze.IModule 
    {
        public const int ModuleId = 11008;
        public override string FullName => "Zeze.Beans.Provider";
        public override string Name => "Provider";
        public override int Id => ModuleId;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47281670848105, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.AnnounceProviderInfo(),
                Handle = ProcessAnnounceProviderInfo,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAnnounceProviderInfop", Zeze.Transaction.TransactionLevel.None),
            });
            service.AddFactoryHandle(47282301515237, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Bind(),
                Handle = ProcessBindRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47282243906435, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Broadcast(),
                Handle = ProcessBroadcast,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessBroadcastp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47282516612067, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Kick(),
                Handle = ProcessKick,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessKickp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47280423652415, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Send(),
                Handle = ProcessSend,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSendp", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47281174282091, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.SetUserState(),
                Handle = ProcessSetUserState,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSetUserStatep", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47282665133980, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.Subscribe(),
                Handle = ProcessSubscribeRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47280773808911, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.Provider.UnBind(),
                Handle = ProcessUnBindRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47281670848105, out var _);
            service.Factorys.TryRemove(47282301515237, out var _);
            service.Factorys.TryRemove(47282243906435, out var _);
            service.Factorys.TryRemove(47282516612067, out var _);
            service.Factorys.TryRemove(47280423652415, out var _);
            service.Factorys.TryRemove(47281174282091, out var _);
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


        protected abstract System.Threading.Tasks.Task<long>  ProcessAnnounceProviderInfo(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessBindRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessBroadcast(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessKick(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessSend(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessSetUserState(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessSubscribeRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessUnBindRequest(Zeze.Net.Protocol p);
    }
}
