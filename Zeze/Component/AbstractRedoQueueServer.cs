// auto generate
namespace Zeze.Component
{
    public abstract class AbstractRedoQueueServer : Zeze.IModule 
    {
        public const int ModuleId = 11010;
        public override string FullName => "Zeze.Component.RedoQueueServer";
        public override string Name => "RedoQueueServer";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;

        internal Zeze.Builtin.RedoQueue.tQueueLastTaskId _tQueueLastTaskId = new();

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47289120801215, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Builtin.RedoQueue.RunTask(),
                Handle = ProcessRunTaskRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessRunTaskRequest", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessRunTaskRequest", Zeze.Transaction.DispatchMode.Normal),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47289120801215, out var _);
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
            zeze.AddTable(zeze.Config.GetTableConf(_tQueueLastTaskId.Name).DatabaseName, _tQueueLastTaskId);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tQueueLastTaskId.Name).DatabaseName, _tQueueLastTaskId);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }


        protected abstract System.Threading.Tasks.Task<long>  ProcessRunTaskRequest(Zeze.Net.Protocol p);
    }
}
