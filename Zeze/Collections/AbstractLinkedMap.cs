// auto generate
namespace Zeze.Collections
{
    public abstract class AbstractLinkedMap : Zeze.IModule 
    {
        public const int ModuleId = 11005;
        public override string FullName => "Zeze.Collections.LinkedMap";
        public override string Name => "LinkedMap";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;

        internal Zeze.Builtin.Collections.LinkedMap.tLinkedMapNodes _tLinkedMapNodes = new Zeze.Builtin.Collections.LinkedMap.tLinkedMapNodes();
        internal Zeze.Builtin.Collections.LinkedMap.tLinkedMaps _tLinkedMaps = new Zeze.Builtin.Collections.LinkedMap.tLinkedMaps();
        internal Zeze.Builtin.Collections.LinkedMap.tValueIdToNodeId _tValueIdToNodeId = new Zeze.Builtin.Collections.LinkedMap.tValueIdToNodeId();

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
            zeze.AddTable(zeze.Config.GetTableConf(_tLinkedMapNodes.Name).DatabaseName, _tLinkedMapNodes);
            zeze.AddTable(zeze.Config.GetTableConf(_tLinkedMaps.Name).DatabaseName, _tLinkedMaps);
            zeze.AddTable(zeze.Config.GetTableConf(_tValueIdToNodeId.Name).DatabaseName, _tValueIdToNodeId);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tLinkedMapNodes.Name).DatabaseName, _tLinkedMapNodes);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tLinkedMaps.Name).DatabaseName, _tLinkedMaps);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tValueIdToNodeId.Name).DatabaseName, _tValueIdToNodeId);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }

    }
}
