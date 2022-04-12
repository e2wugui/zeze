// auto generate
namespace Zeze.Collections
{
    public abstract class AbstractLinkedMap : Zeze.IModule 
    {
    public const int ModuleId = 11005;
    public override string FullName => "Zeze.Beans.Collections.LinkedMap";
    public override string Name => "LinkedMap";
    public override int Id => ModuleId;

        tLinkedMapNodes _tLinkedMapNodes = new tLinkedMapNodes();
        tLinkedMaps _tLinkedMaps = new tLinkedMaps();
        tValueIdToNodeId _tValueIdToNodeId = new tValueIdToNodeId();

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
