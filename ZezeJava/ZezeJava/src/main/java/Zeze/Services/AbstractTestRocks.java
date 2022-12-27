// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractTestRocks implements Zeze.IModule {
    public static final int ModuleId = 11002;
    public static final String ModuleName = "TestRocks";
    public static final String ModuleFullName = "Zeze.Services.TestRocks";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.registerTableTemplate("tRocks", int.class, Zeze.Builtin.TestRocks.BValue.class);
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Integer.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Zeze.Builtin.TestRocks.BeanKey.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Builtin.TestRocks.BeanKey.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(Integer.class, Integer.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap2<>(Integer.class, Zeze.Builtin.TestRocks.BValue.class));
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
