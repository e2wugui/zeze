// auto generate
namespace Zeze.Services
{
    public abstract class AbstractTestRocks : Zeze.IModule 
    {
    public const int ModuleId = 11002;
    public override string FullName => "Zeze.Beans.TestRocks";
    public override string Name => "TestRocks";
    public override int Id => ModuleId;


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
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<int>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<bool>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<float>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<double>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<string>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<Zeze.Net.Binary>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogSet1<int>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogSet1<Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.Log<Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogMap1<int, int>>();
            rocks.RegisterLog<Zeze.Raft.RocksRaft.LogMap2<int, Zeze.Beans.TestRocks.Value>>();
        }

    }
}
