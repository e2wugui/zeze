// auto generate
namespace Zeze.Services
{
    public abstract class AbstractGlobalCacheManagerWithRaftAgent : Zeze.IModule 
    {
        public const int ModuleId = 11001;
        public override string FullName => "Zeze.Services.GlobalCacheManagerWithRaftAgent";
        public override string Name => "GlobalCacheManagerWithRaftAgent";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47251404755902, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAcquireResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAcquireResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47253156226169, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCleanupResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessCleanupResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47249886857671, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessKeepAliveResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessKeepAliveResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47251261574418, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.Login(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLoginResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessLoginResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47249192987366, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessNormalCloseResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessNormalCloseResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47250386526035, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce(),
                Handle = ProcessReduceRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReduceRequest", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47251807618150, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReLoginResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessReLoginResponse", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47251404755902, out var _);
            service.Factorys.TryRemove(47253156226169, out var _);
            service.Factorys.TryRemove(47249886857671, out var _);
            service.Factorys.TryRemove(47251261574418, out var _);
            service.Factorys.TryRemove(47249192987366, out var _);
            service.Factorys.TryRemove(47250386526035, out var _);
            service.Factorys.TryRemove(47251807618150, out var _);
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


        protected abstract System.Threading.Tasks.Task<long>  ProcessReduceRequest(Zeze.Net.Protocol p);
    }
}
