// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractLinkedMap {
    protected final tLinkedMapNodes _tLinkedMapNodes = new tLinkedMapNodes();
    protected final tLinkedMaps _tLinkedMaps = new tLinkedMaps();
    protected final tValueIdToNodeId _tValueIdToNodeId = new tValueIdToNodeId();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tLinkedMapNodes.getName()).getDatabaseName(), _tLinkedMapNodes);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tLinkedMaps.getName()).getDatabaseName(), _tLinkedMaps);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tValueIdToNodeId.getName()).getDatabaseName(), _tValueIdToNodeId);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tLinkedMapNodes.getName()).getDatabaseName(), _tLinkedMapNodes);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tLinkedMaps.getName()).getDatabaseName(), _tLinkedMaps);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tValueIdToNodeId.getName()).getDatabaseName(), _tValueIdToNodeId);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
