using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.StateMachines
{
    // 这个是 GlobalCacheManagerWithRaft 的 StateMachine 实现。
    // 第一个采用Raft的服务，把代码放在这里，可以参考。
    // 这个类由 Zeze.Services.GlobalCacheManagerWithRaft 使用。

    public class GlobalCacheManagerStateMachine : StateMachine
    {
        public ConcurrentMap<Services.GlobalCacheManager.GlobalTableKey, CacheState>
            Global { get; }
            = new ConcurrentMap<Services.GlobalCacheManager.GlobalTableKey, CacheState>
            (
                Services.GlobalCacheManager.DefaultConcurrencyLevel,
                Services.GlobalCacheManager.DefaultCapacity
            );

        public ConcurrentMap<int, CacheHolder>
            Sessions { get; }
            = new ConcurrentMap<int, CacheHolder>
            (
                Services.GlobalCacheManager.DefaultConcurrencyLevel,
                4096
            );

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

        /// <summary>
        /// 【优化】按顺序记录多个修改数据的操作，减少提交给Raft的日志数量。
        /// </summary>
        public sealed class OperatesLog : Log
        {
            public List<Operate> Operates { get; } = new List<Operate>();

            public override void Apply(StateMachine stateMachine)
            {
                var sm = stateMachine as GlobalCacheManagerStateMachine;
                foreach (var op in Operates)
                {
                    op.Apply(sm);
                }
            }

            public override void Decode(ByteBuffer bb)
            {
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    var opid = bb.ReadInt();
                    if (Services.GlobalCacheManagerWithRaft.Instance.StateMachine.OperateFactory.TryGetValue(opid, out var factory))
                    {
                        Operate op = factory();
                        op.Decode(bb);
                    }
                    else
                    {
                        throw new Exception($"Unknown Operate With Id={opid}");
                    }
                }
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Operates.Count);
                foreach (var op in Operates)
                {
                    bb.WriteInt(op.TypeId);
                    op.Encode(bb);
                }
            }
        }

        public ConcurrentDictionary<int, Func<Operate>>
            OperateFactory { get; }
            = new ConcurrentDictionary<int, Func<Operate>>();

        public interface Operate : Serializable
        {
            public virtual int TypeId => (int)Zeze.Transaction.Bean.Hash32(GetType().FullName);
            // 这里不管多线程，由调用者决定并发。
            public void Apply(GlobalCacheManagerStateMachine sm);
        }

        public sealed class PutCacheHolderAcquired : Operate
        {
            public int Id { get; set; }
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int AcquireState { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Sessions.Update(Id, (v) => v.Acquired[Key] = AcquireState);
            }

            public void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                AcquireState = bb.ReadInt();
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                Key.Encode(bb);
                bb.WriteInt(AcquireState);
            }
        }

        public sealed class RemoveCacheHolderAcquired : Operate
        {
            public int Id { get; set; }
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Sessions.Update(Id, (v) => v.Acquired.TryRemove(Key, out var _));
            }

            public void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                Key.Encode(bb);
            }
        }

        public sealed class RemoveCacheState : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Global.Remove(Key);
            }

            public void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
            }

            public void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
            }
        }

        public sealed class SetCacheStateAcquireStatePending : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int AcquireStatePending { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Global.Update(Key, (v) => v.AcquireStatePending = AcquireStatePending);
            }

            public void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                AcquireStatePending = bb.ReadInt();
            }

            public void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(AcquireStatePending);
            }
        }

        public sealed class SetCacheStateModify : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int Modify { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Global.Update(Key, (v) => v.Modify = Modify);
            }

            public void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                Modify = bb.ReadInt();
            }

            public void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(Modify);
            }
        }

        public sealed class AddCacheStateShare : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int Share { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Global.Update(Key, (v) => v.Share.Add(Share));
            }

            public void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                Share = bb.ReadInt();
            }

            public void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(Share);
            }
        }

        public sealed class RemoveCacheStateShare : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int Share { get; set; }

            public void Apply(GlobalCacheManagerStateMachine sm)
            {
                sm.Global.Update(Key, (v) => v.Share.Remove(Share));
            }

            public void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                Share = bb.ReadInt();
            }

            public void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(Share);
            }
        }

        public sealed class CacheState : Copyable<CacheState>
        {
            internal int AcquireStatePending { get; set; } = Services.GlobalCacheManager.StateInvalid;
            internal int Modify { get; set; } // AutoKeyLocalId
            internal HashSet<int> Share { get; } = new HashSet<int>(); // AutoKeyLocalIds

            public override string ToString()
            {
                StringBuilder sb = new StringBuilder();
                ByteBuffer.BuildString(sb, Share);
                return $"P{AcquireStatePending} M{Modify} S{sb}";
            }

            public CacheState()
            {
            }

            private CacheState(CacheState other)
            {
                AcquireStatePending = other.AcquireStatePending;
                Modify = other.Modify;
                foreach (var e in other.Share)
                {
                    Share.Add(e);
                }
            }

            public CacheState Copy()
            {
                return new CacheState(this);
            }

            public void Decode(ByteBuffer bb)
            {
                AcquireStatePending = bb.ReadInt();
                Modify = bb.ReadInt();
                Share.Clear();
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    Share.Add(bb.ReadInt());
                }
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(AcquireStatePending);
                bb.WriteInt(Modify);
                bb.WriteInt(Share.Count);
                foreach (var e in Share)
                {
                    bb.WriteInt(e);
                }
            }
        }

        public sealed class CacheHolder : Copyable<CacheHolder>
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            // local only. 每一个Raft服务器独立设置。【不会系列化】。
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; } // UnBind 的时候不会重置，会一直保留到下一次Bind。

            // 已分配给这个cache的记录。【需要系列化】。
            public ConcurrentDictionary<Services.GlobalCacheManager.GlobalTableKey, int>
                Acquired { get; }
                = new ConcurrentDictionary<Services.GlobalCacheManager.GlobalTableKey, int>
                (
                    Services.GlobalCacheManager.DefaultConcurrencyLevel,
                    1000000
                );

            public int Id { get; }

            public CacheHolder(int key)
            {
                Id = key;
            }

            public bool TryBindSocket(Zeze.Net.AsyncSocket newSocket,
                int _GlobalCacheManagerHashIndex)
            {
                lock (this)
                {
                    if (newSocket.UserState != null)
                        return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

                    var socket = Services.GlobalCacheManagerWithRaft.Instance.Raft.Server.GetSocket(SessionId);
                    if (null == socket)
                    {
                        // old socket not exist or has lost.
                        SessionId = newSocket.SessionId;
                        newSocket.UserState = this;
                        GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
                        return true;
                    }
                    // 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
                    return false;
                }
            }

            public bool TryUnBindSocket(Zeze.Net.AsyncSocket oldSocket)
            {
                lock (this)
                {
                    // 这里检查比较严格，但是这些检查应该都不会出现。

                    if (oldSocket.UserState != this)
                        return false; // not bind to this

                    var socket = Services.GlobalCacheManagerWithRaft.Instance.Raft.Server.GetSocket(SessionId);
                    if (socket != oldSocket)
                        return false; // not same socket

                    SessionId = 0;
                    return true;
                }
            }
            public override string ToString()
            {
                return "" + SessionId;
            }

            public int Reduce(Services.GlobalCacheManager.GlobalTableKey gkey, int state)
            {
                try
                {
                    var reduce = ReduceWaitLater(gkey, state);
                    if (null != reduce)
                    {
                        reduce.Future.Task.Wait();
                        // 如果rpc返回错误的值，外面能处理。
                        return reduce.Result.State;
                    }
                    return Services.GlobalCacheManager.StateReduceNetError;
                }
                catch (Zeze.Net.RpcTimeoutException timeoutex)
                {
                    // 等待超时，应该报告错误。
                    logger.Error(timeoutex, "Reduce RpcTimeoutException {0} target={1}", state, SessionId);
                    return Services.GlobalCacheManager.StateReduceRpcTimeout;
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "Reduce Exception {0} target={1}", state, SessionId);
                    return Services.GlobalCacheManager.StateReduceException;
                }
            }

            public const long ForbitPeriod = 10 * 1000; // 10 seconds
            private long LastErrorTime = 0; // 本地变量，【不会系列化】。

            public void SetError()
            {
                lock (this)
                {
                    long now = global::Zeze.Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > ForbitPeriod)
                        LastErrorTime = now;
                }
            }
            /// <summary>
            /// 返回null表示发生了网络错误，或者应用服务器已经关闭。
            /// </summary>
            /// <param name="gkey"></param>
            /// <param name="state"></param>
            /// <returns></returns>
            public Services.GlobalCacheManager.Reduce ReduceWaitLater(
                Services.GlobalCacheManager.GlobalTableKey gkey, int state)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < ForbitPeriod)
                            return null;
                    }
                    Zeze.Net.AsyncSocket peer = Services.GlobalCacheManagerWithRaft.Instance.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Services.GlobalCacheManager.Reduce(gkey, state);
                        reduce.SendForWait(peer, 10000);
                        return reduce;
                    }
                }
                catch (Exception ex)
                {
                    // 这里的异常只应该是网络发送异常。
                    logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
                }
                SetError();
                return null;
            }

            public CacheHolder()
            {
            }

            private CacheHolder(CacheHolder other)
            {
                SessionId = other.SessionId;
                GlobalCacheManagerHashIndex = other.GlobalCacheManagerHashIndex;
                foreach (var e in other.Acquired)
                {
                    Acquired.TryAdd(e.Key, e.Value);
                }
            }

            public CacheHolder Copy()
            {
                return new CacheHolder(this);
            }

            public void Decode(ByteBuffer bb)
            {
                Acquired.Clear();
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    var key = new Services.GlobalCacheManager.GlobalTableKey();
                    key.Decode(bb);
                    int value = bb.ReadInt();
                    Acquired.TryAdd(key, value);
                }
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Acquired.Count);
                foreach (var e in Acquired)
                {
                    e.Key.Encode(bb);
                    bb.WriteInt(e.Value);
                }
            }
        }
    }
}
