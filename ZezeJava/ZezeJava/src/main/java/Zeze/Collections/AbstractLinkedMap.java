// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractLinkedMap implements Zeze.IModule {
    public static final int ModuleId = 11005;
    public static final String ModuleName = "LinkedMap";
    public static final String ModuleFullName = "Zeze.Collections.LinkedMap";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

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
}
