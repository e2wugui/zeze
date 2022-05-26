// auto generate
namespace Zeze.Collections
{
    public abstract class AbstractQueue : Zeze.IModule 
    {
        public const int ModuleId = 11006;
        public override string FullName => "Zeze.Collections.Queue";
        public override string Name => "Queue";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;

        internal Zeze.Builtin.Collections.Queue.tQueueNodes _tQueueNodes = new();
        internal Zeze.Builtin.Collections.Queue.tQueues _tQueues = new();

        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
            zeze.AddTable(zeze.Config.GetTableConf(_tQueueNodes.Name).DatabaseName, _tQueueNodes);
            zeze.AddTable(zeze.Config.GetTableConf(_tQueues.Name).DatabaseName, _tQueues);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tQueueNodes.Name).DatabaseName, _tQueueNodes);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tQueues.Name).DatabaseName, _tQueues);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }

    }
}
