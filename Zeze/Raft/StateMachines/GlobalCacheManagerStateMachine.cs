using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.StateMachines
{
    // 这个是 GlobalCacheManager 的 StateMachine 实现。
    // 作为第一个 Raft 用例，放在，也作为一个例子。

    public class GlobalCacheManagerStateMachine : StateMachine
    {
        public ConcurrentMap<Services.GlobalCacheManager.GlobalTableKey, CacheState> Global { get; }
            = new ConcurrentMap<Services.GlobalCacheManager.GlobalTableKey, CacheState>();

        public ConcurrentMap<Int, CacheHolder> Sessions { get; }
            = new ConcurrentMap<Int, CacheHolder>();

        public override void LoadFromSnapshot(string path)
        {
            lock (Raft)
            {
                using var file = new System.IO.FileStream(path, System.IO.FileMode.Open);
                Global.UnSerializeFrom(file);
                Sessions.UnSerializeFrom(file);
            }
        }

        public override bool Snapshot(string path, out long LastIncludedIndex, out long LastIncludedTerm)
        {
            using var file = new System.IO.FileStream(path, System.IO.FileMode.Create);
            lock (Raft)
            {
                LastIncludedIndex = Raft.LogSequence.Index;
                LastIncludedTerm = Raft.LogSequence.Term;
                if (!Global.StartSerialize())
                    return false;
                if (!Sessions.StartSerialize())
                    return false;
            }
            Global.ConcurrentSerializeTo(file);
            Sessions.ConcurrentSerializeTo(file);
            lock (Raft)
            {
                Global.EndSerialize();
                Sessions.EndSerialize();
            }
            return true;
        }
    }

    public sealed class CacheState : Copyable<CacheState>
    {
        public CacheState Copy()
        {
            throw new NotImplementedException();
        }

        public void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }
    }

    public sealed class CacheHolder : Copyable<CacheHolder>
    {
        public CacheHolder Copy()
        {
            throw new NotImplementedException();
        }

        public void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }
    }
}
