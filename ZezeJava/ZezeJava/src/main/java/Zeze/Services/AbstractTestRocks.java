// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractTestRocks {
    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.RegisterTableTemplate("tRocks", int.class, Zeze.Beans.TestRocks.Value.class);
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Integer.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(Integer.class, Integer.class));
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogMap1<>(Integer.class, Zeze.Beans.TestRocks.Value.class));
    }
}
