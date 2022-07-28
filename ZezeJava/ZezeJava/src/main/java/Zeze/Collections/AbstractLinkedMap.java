// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractLinkedMap extends Zeze.IModule {
    public static final int ModuleId = 11005;
    @Override public String getFullName() { return "Zeze.Collections.LinkedMap"; }
    @Override public String getName() { return "LinkedMap"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Collections.LinkedMap.tLinkedMapNodes _tLinkedMapNodes = new Zeze.Builtin.Collections.LinkedMap.tLinkedMapNodes();
    protected final Zeze.Builtin.Collections.LinkedMap.tLinkedMaps _tLinkedMaps = new Zeze.Builtin.Collections.LinkedMap.tLinkedMaps();
    protected final Zeze.Builtin.Collections.LinkedMap.tValueIdToNodeId _tValueIdToNodeId = new Zeze.Builtin.Collections.LinkedMap.tValueIdToNodeId();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
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

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
