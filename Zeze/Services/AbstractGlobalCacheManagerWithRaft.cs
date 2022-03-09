// auto generate
namespace Zeze.Services
{
    public abstract class AbstractGlobalCacheManagerWithRaft
    {

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47249952964310, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.Acquire(),
                Handle = ProcessAcquireRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAcquireRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47250415716701, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.Cleanup(),
                Handle = ProcessCleanupRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCleanupRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47249217743128, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.KeepAlive(),
                Handle = ProcessKeepAliveRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47249332792819, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.Login(),
                Handle = ProcessLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47252127184652, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.NormalClose(),
                Handle = ProcessNormalCloseRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47249266974153, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.Reduce(),
                Handle = ProcessReduceRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
            service.AddFactoryHandle(47249634595374, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Component.GlobalCacheManagerWithRaft.ReLogin(),
                Handle = ProcessReLoginRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47249952964310, out var _);
            service.Factorys.TryRemove(47250415716701, out var _);
            service.Factorys.TryRemove(47249217743128, out var _);
            service.Factorys.TryRemove(47249332792819, out var _);
            service.Factorys.TryRemove(47252127184652, out var _);
            service.Factorys.TryRemove(47249266974153, out var _);
            service.Factorys.TryRemove(47249634595374, out var _);
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
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogSet1<int>>();
            rocks.RegisterTableTemplate<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Component.GlobalCacheManagerWithRaft.CacheState>("Global");
            rocks.RegisterTableTemplate<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Component.GlobalCacheManagerWithRaft.AcquiredState>("Session");
        }


        protected abstract long ProcessAcquireRequest(Zeze.Net.Protocol p);

        protected abstract long ProcessCleanupRequest(Zeze.Net.Protocol p);

        protected abstract long ProcessKeepAliveRequest(Zeze.Net.Protocol p);

        protected abstract long ProcessLoginRequest(Zeze.Net.Protocol p);

        protected abstract long ProcessNormalCloseRequest(Zeze.Net.Protocol p);

        protected abstract long ProcessReduceRequest(Zeze.Net.Protocol p);

        protected abstract long ProcessReLoginRequest(Zeze.Net.Protocol p);
    }
}
