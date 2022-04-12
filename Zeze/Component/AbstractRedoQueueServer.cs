// auto generate
namespace Zeze.Component
{
    public abstract class AbstractRedoQueueServer : Zeze.IModule 
    {
    public const int ModuleId = 11010;
    public override string FullName => "Zeze.Beans.RedoQueue";
    public override string Name => "RedoQueue";
    public override int Id => ModuleId;

        tQueueLastTaskId _tQueueLastTaskId = new tQueueLastTaskId();

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
            service.AddFactoryHandle(47289196145593, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zeze.Beans.RedoQueue.RunTask(),
                Handle = ProcessRunTaskRequest,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessRunTaskRequest", Zeze.Transaction.TransactionLevel.Serializable),
            });
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
            service.Factorys.TryRemove(47289196145593, out var _);
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
