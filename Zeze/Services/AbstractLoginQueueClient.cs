// auto generate

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Services
{
    public abstract class AbstractLoginQueueClient : Zeze.IModule 
    {
        public const int ModuleId = 11043;
        public override string FullName => "Zeze.Services.LoginQueueClient";
        public override string Name => "LoginQueueClient";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            service.AddFactoryHandle(47431216900463, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.LoginQueue.PutLoginToken(),
                Handle = ProcessPutLoginToken,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessPutLoginTokenp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessPutLoginToken", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47432287199628, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.LoginQueue.PutQueueFull(),
                Handle = ProcessPutQueueFull,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessPutQueueFullp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessPutQueueFull", Zeze.Transaction.DispatchMode.Normal),
            });
            service.AddFactoryHandle(47432615378605, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.LoginQueue.PutQueuePosition(),
                Handle = ProcessPutQueuePosition,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessPutQueuePositionp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessPutQueuePosition", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47431216900463, out var _);
            service.Factorys.TryRemove(47432287199628, out var _);
            service.Factorys.TryRemove(47432615378605, out var _);
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


        protected abstract System.Threading.Tasks.Task<long>  ProcessPutLoginToken(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessPutQueueFull(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessPutQueuePosition(Zeze.Net.Protocol p);
    }
}
