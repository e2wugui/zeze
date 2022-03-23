// auto generate
namespace Zeze.Services
{
    public abstract class AbstractGlobalCacheManagerWithRaft
    {

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47251758877516, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.Acquire(),
                Handle = ProcessAcquireRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAcquireRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47249689802603, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup(),
                Handle = ProcessCleanupRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCleanupRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47250139303472, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive(),
                Handle = ProcessKeepAliveRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47251605578232, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.Login(),
                Handle = ProcessLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47250988461421, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose(),
                Handle = ProcessNormalCloseRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47252602373450, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.Reduce(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47251661990773, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin(),
                Handle = ProcessReLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47251758877516, out var _);
            service.Factorys.TryRemove(47249689802603, out var _);
            service.Factorys.TryRemove(47250139303472, out var _);
            service.Factorys.TryRemove(47251605578232, out var _);
            service.Factorys.TryRemove(47250988461421, out var _);
            service.Factorys.TryRemove(47252602373450, out var _);
            service.Factorys.TryRemove(47251661990773, out var _);
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
            rocks.RegisterTableTemplate<Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Beans.GlobalCacheManagerWithRaft.CacheState>("Global");
            rocks.RegisterTableTemplate<Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Beans.GlobalCacheManagerWithRaft.AcquiredState>("Session");
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<int>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<long>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogSet1<int>>();
        }


        protected abstract System.Threading.Tasks.Task<long>  ProcessAcquireRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessCleanupRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessKeepAliveRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessLoginRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessNormalCloseRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessReLoginRequest(Zeze.Net.Protocol p);
    }
}
