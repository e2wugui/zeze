// auto-generated
using Zeze.Serialize;

namespace Zeze.Component.GlobalCacheManagerWithRaft
{
    public sealed class Global
    {
        public Zeze.Raft.RocksRaft.Table<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Component.GlobalCacheManagerWithRaft.CacheState> Open(Zeze.Raft.RocksRaft.Rocks rocks, int family)
        {
            return rocks.OpenTable<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey, Zeze.Component.GlobalCacheManagerWithRaft.CacheState>("Global", 10000);
        }
    }
}
