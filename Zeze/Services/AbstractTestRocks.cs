// auto generate
namespace Zeze.Services
{
    public abstract class AbstractTestRocks
    {

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
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
            rocks.RegisterTableTemplate<int, Zeze.Beans.TestRocks.Value>("tRocks");
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<int>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<bool>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<float>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<double>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<string>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<Net.Binary>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.LogSet1<int>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.LogSet1<Beans.GlobalCacheManagerWithRaft.GlobalTableKey>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.Log<Beans.GlobalCacheManagerWithRaft.GlobalTableKey>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.LogMap1<int, int>>();
            Raft.RocksRaft.Rocks.RegisterLog<Raft.RocksRaft.LogMap2<int, Beans.TestRocks.Value>>();
        }

    }
}
