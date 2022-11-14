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
        zeze.addTable(zeze.getConfig().getTableConf(_tLinkedMapNodes.getName()).getDatabaseName(), _tLinkedMapNodes);
        zeze.addTable(zeze.getConfig().getTableConf(_tLinkedMaps.getName()).getDatabaseName(), _tLinkedMaps);
        zeze.addTable(zeze.getConfig().getTableConf(_tValueIdToNodeId.getName()).getDatabaseName(), _tValueIdToNodeId);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tLinkedMapNodes.getName()).getDatabaseName(), _tLinkedMapNodes);
        zeze.removeTable(zeze.getConfig().getTableConf(_tLinkedMaps.getName()).getDatabaseName(), _tLinkedMaps);
        zeze.removeTable(zeze.getConfig().getTableConf(_tValueIdToNodeId.getName()).getDatabaseName(), _tValueIdToNodeId);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
