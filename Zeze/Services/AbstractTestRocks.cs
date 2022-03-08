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
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogSet1<int>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogSet1<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogMap1<int, int>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogMap1<int, Zeze.Component.TestRocks.Value>>();
            rocks.OpenTable<int, Zeze.Component.TestRocks.Value>("tRocks", 10000);
        }

    }
}
