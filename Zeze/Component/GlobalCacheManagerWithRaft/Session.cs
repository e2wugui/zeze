// auto-generated
using Zeze.Serialize;

namespace Zeze.Component.GlobalCacheManagerWithRaft
{
    public sealed class Session
    {
        public Zeze.Raft.RocksRaft.Table<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Component.GlobalCacheManagerWithRaft.AcquiredState> Open(Zeze.Raft.RocksRaft.Rocks rocks, int family)
        {
            return rocks.OpenTable<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Component.GlobalCacheManagerWithRaft.AcquiredState>("Session", 10000);
        }
    }
}
