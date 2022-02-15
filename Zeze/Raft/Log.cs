using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;
using RocksDbSharp;
using Zeze.Net;

namespace Zeze.Raft
{
    /// <summary>
    /// 用户接口。
    /// </summary>
    public abstract class Log : Serializable
    {
        /// <summary>
        /// 用于系列化，外部使用，实现类不用 Decode、Encode 这个变量。
        /// 只读，常量即可。
        /// 在一个StateMachine实现中唯一。
        /// 不冲突的时候使用默认实现即可。
        /// 【注意】
        /// 如果实现类的FullName发生了改变，需要更新所有的Raft-Node。
        /// 如果不想跟名字相关，重载并提供一个编号。
        /// </summary>
        private readonly int _TypeId;
        public virtual int TypeId => _TypeId;

        // 当前这个Log是哪个应用的Rpc请求引起的。
        // 【Raft用来检测重复的请求】。
        // RaftConfig里面配置AutoKeyLocalStep开启这个功能。
        // 启用这个功能要求应用的RpcSessionId持久化，并且全局唯一，对每个AutoKeyLocalStep递增。
        // 【注意】应用生成的Id必须大于0；0保留给内部；小于0未使用。
        public UniqueRequestId Unique { get; } = new UniqueRequestId();
        public long CreateTime { get; set; }

        public Log(IRaftRpc req)
        {
            if (null != req)
            {
                Unique = req.Unique;
                CreateTime = req.CreateTime;
            }
            _TypeId = (int)Bean.Hash32(GetType().FullName);
        }

        /// <summary>
        /// 最主要的实现接口。
        /// </summary>
        /// <param name="stateMachine"></param>
        public abstract void Apply(RaftLog holder, StateMachine stateMachine);

        public virtual void Decode(ByteBuffer bb)
        {
            Unique.Decode(bb);
            CreateTime = bb.ReadLong();
        }

        public virtual void Encode(ByteBuffer bb)
        {
            Unique.Encode(bb);
            bb.WriteLong(CreateTime);
        }
    }

    public sealed class HeartbeatLog : Log
    {
        public const int SetLeaderReadyEvent = 1;

        public int Operate { get; private set; }
        public string Info { get; private set; }

        public HeartbeatLog(int operate = 0, string info = null)
            : base(null)
        {
            Operate = operate;
            Info = null == info ? string.Empty : info;
        }

        public override void Apply(RaftLog holder, StateMachine stateMachine)
        {
            switch (Operate)
            {
                case SetLeaderReadyEvent:
                    stateMachine.Raft.SetLeaderReady(holder);
                    break;
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
            Operate = bb.ReadInt();
            Info = bb.ReadString();
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
            bb.WriteInt(Operate);
            bb.WriteString(Info);
        }

        public override string ToString()
        {
            return $"{GetType().FullName}:{Info}";
        }
    }

    public sealed class RaftLog : Serializable
    {
        public long Term { get; private set; }
        public long Index { get; private set; }
        public Log Log { get; private set; }

        // 不会被系列化。Local Only.
        public Func<int, Log> LogFactory { get; }

        public override string ToString()
        {
            return $"Term={Term} Index={Index} Log={Log}";
        }
        public RaftLog(long term, long index, Log log)
        {
            Term = term;
            Index = index;
            Log = log;
        }

        public RaftLog(Func<int, Log> logFactory)
        {
            LogFactory = logFactory;
        }

        public void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            Index = bb.ReadLong();
            int logTypeId = bb.ReadInt4();
            Log = LogFactory(logTypeId);
            Log.Decode(bb);
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteLong(Index);
            bb.WriteInt4(Log.TypeId);
            Log.Encode(bb);
        }

        public ByteBuffer Encode()
        {
            var bb = ByteBuffer.Allocate();
            Encode(bb);
            return bb;
        }

        public static RaftLog Decode(Binary data, Func<int, Log> logFactory)
        {
            var raftLog = new RaftLog(logFactory);
            data.Decode(raftLog);
            return raftLog;
        }
    }

    public class LogSequence
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Raft Raft { get; }

        public long Term { get; private set; }
        public long LastIndex { get; private set; }
        // 用来处理NextIndex回溯时限制搜索。snapshot需要修订这个值。
        public long FirstIndex { get; private set; }
        public long CommitIndex { get; private set; }
        public long LastApplied { get; private set; }

        // 这个不是日志需要的，因为持久化，所以就定义在这里吧。
        internal string VoteFor { get; set; }

        // 初始化的时候会加入一条日志(Index=0，不需要真正apply)，
        // 以后Snapshot时，会保留LastApplied的。
        // 所以下面方法不会返回空。
        // 除非什么例外发生。那就抛空指针异常吧。
        public RaftLog LastAppliedLog()
        {
            return ReadLog(LastApplied);
        }

        public long GetAndSetFirstIndex(long newFirstIndex)
        {
            lock (Raft)
            {
                long tmp = FirstIndex;
                FirstIndex = newFirstIndex;
                return tmp;
            }
        }

        public void RemoveLogBeforeLastApplied(long oldFirstIndex)
        {
            RemoveLogReverse(LastApplied - 1, oldFirstIndex);
        }

        private void RemoveLogReverse(long startIndex, long firstIndex)
        {
            if (startIndex >= LastApplied)
                throw new Exception("Error At Least Retain One Applied Log");

            for (var index = startIndex; index >= firstIndex; --index)
            {
                RemoveLog(index);
            }
        }

        // Leader
        // Follower
        public long LeaderActiveTime { get; internal set; } = Zeze.Util.Time.NowUnixMillis;

        private RocksDb Logs { get; set; }
        private RocksDb Rafts { get; set; }

        internal sealed class UniqueRequestSet
        { 
            private RocksDb Db { get; set; }
            public string DbName { get; set; }

            public LogSequence LogSequence { get; set; }

            public UniqueRequestSet(LogSequence lq, string dbName)
            {
                LogSequence = lq;
                DbName = dbName;
            }

            static ConcurrentDictionary<string, ConcurrentDictionary<long, long>> unique
                = new ConcurrentDictionary<string,ConcurrentDictionary<long, long>>(); 

            private void Put(RaftLog log, long value)
            {
                var db = OpenDb();
                var key = ByteBuffer.Allocate(100);
                log.Log.Unique.Encode(key);
                if (value > 0 && db.Get(key.Bytes, key.Size) != null)
                {
                    throw new RaftRetryException($"Duplicate Request Found = {log.Log.Unique}");
                }
                var val = ByteBuffer.Allocate();
                val.WriteLong(value);
                db.Put(key.Bytes, key.Size, val.Bytes, val.Size, null, LogSequence.WriteOptionsSync);
            }

            public void Save(RaftLog log)
            {
                Put(log, log.Index);
            }

            public void Apply(RaftLog log)
            {
                Put(log, -log.Index);
            }

            public void Remove(RaftLog log)
            {
                var key = ByteBuffer.Allocate(100);
                log.Log.Unique.Encode(key);
                OpenDb().Remove(key.Bytes, key.Size, null, LogSequence.WriteOptionsSync);
            }

            public long GetRequestState(IRaftRpc iraftrpc)
            {
                var key = ByteBuffer.Allocate(100);
                iraftrpc.Unique.Encode(key);
                var val = OpenDb().Get(key.Bytes, key.Size);
                if (null == val)
                    return 0;
                var bb = ByteBuffer.Wrap(val);
                return bb.ReadLong();
            }

            private RocksDb OpenDb()
            {
                lock (this)
                {
                    if (null == Db)
                    {
                        var dir = Path.Combine(LogSequence.Raft.RaftConfig.DbHome, "unique");
                        Directory.CreateDirectory(dir);
                        Db = RocksDb.Open(new DbOptions().SetCreateIfMissing(true), Path.Combine(dir, DbName));
                    }
                    return Db;
                }
            }

            public void Dispose()
            {
                Db?.Dispose();
            }
        }
        private ConcurrentDictionary<string, UniqueRequestSet> UniqueRequestSets { get; }
            = new ConcurrentDictionary<string, UniqueRequestSet>();

        internal void Close()
        {
            lock (Raft)
            {
                SnapshotTimer?.Cancel();
                Logs?.Dispose();
                Logs = null;
                Rafts?.Dispose();
                Rafts = null;
                foreach (var db in UniqueRequestSets.Values)
                {
                    db.Dispose();
                }
                UniqueRequestSets.Clear();
            }
        }

        public LogSequence(Raft raft)
        {
            Raft = raft;
            var options = new DbOptions().SetCreateIfMissing(true);

            Rafts = RocksDb.Open(options, Path.Combine(Raft.RaftConfig.DbHome, "rafts"));
            {
                // Read Term
                var termKey = ByteBuffer.Allocate();
                termKey.WriteInt(0);
                RaftsTermKey = termKey.Copy();
                var termValue = Rafts.Get(RaftsTermKey);
                if (null != termValue)
                {
                    var bb = ByteBuffer.Wrap(termValue);
                    Term = bb.ReadLong();
                }
                else
                {
                    Term = 0;
                }
                // Read VoteFor
                var voteForKey = ByteBuffer.Allocate();
                voteForKey.WriteInt(1);
                RaftsVoteForKey = voteForKey.Copy();
                var voteForvalue = Rafts.Get(RaftsVoteForKey);
                if (null != voteForvalue)
                {
                    var bb = ByteBuffer.Wrap(voteForvalue);
                    VoteFor = bb.ReadString();
                }
                else
                {
                    VoteFor = string.Empty;
                }
            }


            Logs = RocksDb.Open(options, Path.Combine(Raft.RaftConfig.DbHome, "logs"));
            {
                // Read Last Log Index
                using var itLast = Logs.NewIterator();
                itLast.SeekToLast();
                if (itLast.Valid())
                {
                    LastIndex = RaftLog.Decode(
                        new Binary(itLast.Value()),
                        Raft.StateMachine.LogFactory
                        ).Index;
                }
                else
                {
                    // empty. add one for prev.
                    SaveLog(new RaftLog(Term, 0, new HeartbeatLog()));
                    LastIndex = 0;
                }
                logger.Info($"{Raft.Name}-{Raft.IsLeader} {Raft.RaftConfig.DbHome} LastIndex={LastIndex} Count={GetTestStateMachineCount()}");

                using var itFirst = Logs.NewIterator();
                itFirst.SeekToFirst();
                FirstIndex = RaftLog.Decode(
                    new Binary(itFirst.Value()),
                    Raft.StateMachine.LogFactory
                    ).Index;
                // 【注意】snapshot 以后 FirstIndex 会推进，不再是从0开始。
                LastApplied = FirstIndex;
                CommitIndex = FirstIndex;
            }
        }

        internal long GetRequestState(Protocol p)
        {
            var iraftrpc = p as IRaftRpc;
            if (null == iraftrpc)
                return 0;
            return OpenUniqueRequests(iraftrpc.CreateTime).GetRequestState(iraftrpc);
        }

        private UniqueRequestSet OpenUniqueRequests(long time)
        {
            var dateTime = Util.Time.UnixMillisToDateTime(time);
            var dbName = $"{dateTime.Year}.{dateTime.Month}.{dateTime.Day}";
            return UniqueRequestSets.GetOrAdd(dbName, (db) => new UniqueRequestSet(this, db));
        }

        private readonly byte[] RaftsTermKey;
        private readonly byte[] RaftsVoteForKey;
        internal readonly WriteOptions WriteOptionsSync = new WriteOptions().SetSync(true);

        private void SaveLog(RaftLog log)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(log.Index);
            var value = log.Encode();
            // key,value offset must 0
            Logs.Put(
                key.Bytes, key.Size,
                value.Bytes, value.Size,
                null, WriteOptionsSync
                );

            logger.Info($"{Raft.Name}-{Raft.IsLeader} RequestId={log.Log.Unique.RequestId} Index={log.Index} Count={GetTestStateMachineCount()}");
        }

        private void SaveLogRaw(long index, byte[] rawValue)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(index);

            Logs.Put(
                key.Bytes, key.Size,
                rawValue, rawValue.Length,
                null, WriteOptionsSync
                );

            logger.Info($"{Raft.Name}-{Raft.IsLeader} RequestId=? Index={index} Count={GetTestStateMachineCount()}");
        }

        private RaftLog ReadLog(long index)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(index);
            var value = Logs.Get(key.Bytes, key.Size);
            if (null == value)
                return null;
            return RaftLog.Decode(new Binary(value), Raft.StateMachine.LogFactory);
        }

        internal enum SetTermResult
        {
            Newer,
            Same,
            Older
        }

        // Rules for Servers
        // All Servers:
        // If RPC request or response contains term T > currentTerm:
        // set currentTerm = T, convert to follower(§5.1)
        internal SetTermResult TrySetTerm(long term)
        {
            if (term > Term)
            {
                Term = term;
                var termValue = ByteBuffer.Allocate();
                termValue.WriteLong(term);
                Rafts.Put(
                    RaftsTermKey, RaftsTermKey.Length,
                    termValue.Bytes, termValue.Size,
                    null, WriteOptionsSync
                    );
                return SetTermResult.Newer;
            }
            if (term == Term)
                return SetTermResult.Same;
            return SetTermResult.Older;
        }

        internal bool CanVoteFor(string voteFor)
        {
            return string.IsNullOrEmpty(VoteFor) || VoteFor.Equals(voteFor);
        }

        internal void SetVoteFor(string voteFor)
        {
            if (false == VoteFor.Equals(voteFor))
            {
                VoteFor = voteFor;
                var voteForValue = ByteBuffer.Allocate();
                voteForValue.WriteString(voteFor);
                Rafts.Put(
                    RaftsVoteForKey, RaftsVoteForKey.Length,
                    voteForValue.Bytes, voteForValue.Size,
                    null, WriteOptionsSync
                    );
            }
        }

        private RaftLog FindMaxMajorityLog(long startIndex)
        {
            RaftLog lastMajorityLog = null;
            for (long index = startIndex; index <= LastIndex; /**/)
            {
                var raftLog = ReadLog(index);
                if (null == raftLog)
                    break;
                index = raftLog.Index + 1;
                int MajorityCount = 0;
                Raft.Server.Config.ForEachConnector(
                    (c) =>
                    {
                        var cex = c as Server.ConnectorEx;
                        if (cex.MatchIndex >= raftLog.Index)
                        {
                            ++MajorityCount;
                        }
                    });
                // 没有达成多数派，中断循环。后面返回上一个majority，仍可能为null。
                // 等于的时候加上自己就是多数派了。
                if (MajorityCount < Raft.RaftConfig.HalfCount)
                    break;
                lastMajorityLog = raftLog;
            }
            return lastMajorityLog;
        }

        private void TryCommit(AppendEntries rpc, Server.ConnectorEx connector)
        {
            connector.NextIndex = rpc.Argument.LastEntryIndex + 1;
            connector.MatchIndex = rpc.Argument.LastEntryIndex;

            // 旧的 AppendEntries 的结果，不用继续处理了。
            // 【注意】这个不是必要的，是一个小优化。
            if (rpc.Argument.LastEntryIndex <= CommitIndex)
                return;

            // Rules for Servers
            // If there exists an N such that N > commitIndex, a majority
            // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
            // set commitIndex = N(§5.3, §5.4).

            // TODO 对于 Leader CommitIndex 初始化问题。
            var raftLog = FindMaxMajorityLog(CommitIndex + 1);
            if (null == raftLog)
                return; // 一个多数派都没有找到。

            if (raftLog.Term != Term)
            {
                // 如果是上一个 Term 未提交的日志在这一次形成的多数派，
                // 不自动提交。
                // 总是等待当前 Term 推进时，顺便提交它。
                return;
            }
            CommitIndex = raftLog.Index;
            TryApply(raftLog);
        }

        private void TryApply(RaftLog lastApplyableLog)
        {
            if (null == lastApplyableLog)
            {
                logger.Error("lastApplyableLog is null.");
                return;
            }
            for (long index = LastApplied + 1;
                index <= lastApplyableLog.Index;
                /**/)
            {
                var raftLog = ReadLog(index);
                if (null == raftLog)
                {
                    logger.Warn("What Happened! index={0} lastApplyable={1} LastApplied={2}",
                        index, lastApplyableLog.Index, LastApplied);
                    return; // end?
                }

                index = raftLog.Index + 1;
                raftLog.Log.Apply(raftLog, Raft.StateMachine);
                if (raftLog.Log.Unique.RequestId > 0)
                    OpenUniqueRequests(raftLog.Log.CreateTime).Apply(raftLog);
                LastApplied = raftLog.Index; // 循环可能退出，在这里修改。
                //*
                if (LastIndex - LastApplied < 10)
                    logger.Info($"{Raft.Name}-{Raft.IsLeader} {Raft.RaftConfig.DbHome} RequestId={raftLog.Log.Unique.RequestId} LastIndex={LastIndex} LastApplied={LastApplied} Count={GetTestStateMachineCount()}");
                // */
                if (WaitApplyFutures.TryRemove(raftLog.Index, out var future))
                    future.SetResult(0);
            }
            //logger.Info($"{Raft.Name}-{Raft.IsLeader} CommitIndex={CommitIndex} RequestId={lastApplyableLog.Log.Unique.RequestId} LastIndex={LastIndex} LastApplied={LastApplied} Count={GetTestStateMachineCount()}");
        }

        internal long GetTestStateMachineCount()
        {
            return (Raft.StateMachine as Test.TestStateMachine).Count;
        }

        internal ConcurrentDictionary<long, TaskCompletionSource<int>> WaitApplyFutures { get; }
            = new ConcurrentDictionary<long, TaskCompletionSource<int>>();

        internal void SendHearbeatTo(Server.ConnectorEx connector)
        {
            lock (Raft)
            {
                connector.AppendLogActiveTime = Util.Time.NowUnixMillis;

                if (false == Raft.IsLeader)
                    return; // skip if is not a leader

                if (connector.Pending != null)
                    return;

                if (connector.InstallSnapshotting)
                    return;

                var socket = connector.TryGetReadySocket();
                if (null == socket)
                {
                    // Hearbeat Will Retry
                    return;
                }

                var hearbeat = new AppendEntries();
                hearbeat.Argument.Term = Term;
                hearbeat.Argument.LeaderId = Raft.Name;
                hearbeat.Send(socket, (p) =>
                {
                    if (hearbeat.IsTimeout)
                        return 0; // skip

                    lock (Raft)
                    {
                        if (Raft.LogSequence.TrySetTerm(hearbeat.Result.Term) == SetTermResult.Newer)
                        {
                            Raft.LeaderId = string.Empty; // 此时不知道谁是Leader。
                                                          // new term found.
                            Raft.ConvertStateTo(Raft.RaftState.Follower);
                            return Procedure.Success;
                        }
                    }
                    return 0;
                }, Raft.RaftConfig.AppendEntriesTimeout);
            }
        }

        internal void AppendLog(Log log, bool WaitApply)
        {
            AppendLog(log, WaitApply, out _, out _);
        }

        internal void AppendLog(Log log, bool WaitApply, out long term, out long index)
        {
            term = 0;
            index = 0;

            TaskCompletionSource<int> future = null;
            lock (Raft)
            {
                if (false == Raft.IsLeader)
                    throw new RaftRetryException(); // 快速失败

                var raftLog = new RaftLog(Term, LastIndex + 1, log);
                if (WaitApply)
                {
                    future = new TaskCompletionSource<int>();
                    if (false == WaitApplyFutures.TryAdd(raftLog.Index, future))
                        throw new Exception("Impossible");
                }
                if (raftLog.Log.Unique.RequestId > 0)
                    OpenUniqueRequests(raftLog.Log.CreateTime).Save(raftLog);
                SaveLog(raftLog);
                LastIndex = raftLog.Index;
                term = Term;
                index = LastIndex;
            }

            // 广播给followers并异步等待多数确认
            Raft.Server.Config.ForEachConnector(
                (connector) => TrySendAppendEntries(connector as Server.ConnectorEx, null));

            if (WaitApply)
            {
                if (false == future.Task.Wait(Raft.RaftConfig.AppendEntriesTimeout * 2 + 1000))
                {
                    WaitApplyFutures.TryRemove(index, out _);
                    throw new RaftRetryException();
                }
            }
        }

        /// <summary>
        /// see ReadLogStart
        /// </summary>
        /// <param name="startIndex"></param>
        /// <returns></returns>
        private RaftLog ReadLogReverse(long startIndex)
        {
            for (long index = startIndex; index >= FirstIndex; --index)
            {
                var raftLog = ReadLog(index);
                if (null != raftLog)
                    return raftLog;
            }
            logger.Error($"impossible"); // 日志列表肯定不会为空。
            return null;
        }

        // 是否正在创建Snapshot过程中，用来阻止新的创建请求。
        private bool Snapshotting { get; set; } = false;
        // 是否有安装进程正在进行中，用来阻止新的创建请求。
        internal Dictionary<string, Server.ConnectorEx> InstallSnapshotting { get; }
            = new Dictionary<string, Server.ConnectorEx>();

        public const string SnapshotFileName = "snapshot";
        private Util.SchedulerTask SnapshotTimer;

        public void StopSnapshotPerDayTimer()
        { 
            lock (Raft)
            {
                SnapshotTimer?.Cancel();
                SnapshotTimer = null;
            }
        }

        public void StartSnapshotPerDayTimer()
        {
            lock (Raft)
            {
                if (null != SnapshotTimer)
                    return;

                if (Raft.RaftConfig.SnapshotHourOfDay >= 0 && Raft.RaftConfig.SnapshotHourOfDay < 24)
                {
                    var now = DateTime.Now;
                    var firstTime = new DateTime(now.Year, now.Month, now.Day,
                        Raft.RaftConfig.SnapshotHourOfDay, Raft.RaftConfig.SnapshotMinute, 0);
                    if (firstTime.CompareTo(now) < 0)
                        firstTime = firstTime.AddDays(1);
                    var delay = Util.Time.DateTimeToUnixMillis(firstTime) - Util.Time.DateTimeToUnixMillis(now);
                    SnapshotTimer = Zeze.Util.Scheduler.Instance.Schedule(
                        (ThisTask) => StartSnapshot(false), delay, 24 * 3600 * 1000);
                }
            }
        }

        public void EndReceiveInstallSnapshot(FileStream s, InstallSnapshot r)
        {
            lock (Raft)
            {
                // 6. If existing log entry has same index and term as snapshot’s
                // last included entry, retain log entries following it and reply
                var last = ReadLog(r.Argument.LastIncludedIndex);
                if (null != last && last.Term == r.Argument.LastIncludedTerm)
                {
                    // 这里全部保留更简单吧，否则如果没有applied，那不就糟了吗？
                    // RemoveLogReverse(r.Argument.LastIncludedIndex - 1);
                    return;
                }
                // 7. Discard the entire log
                // 整个删除，那么下一次AppendEnties又会找不到prev。不就xxx了吗?
                // 我的想法是，InstallSnapshot 最后一个 trunk 带上 LastIncludedLog，
                // 接收者清除log，并把这条日志插入（这个和系统初始化时插入的Index=0的日志道理差不多）。
                // 【除了快照最后包含的日志，其他都删除。】
                var lastIncludedLog = RaftLog.Decode(r.Argument.LastIncludedLog, Raft.StateMachine.LogFactory);
                SaveLog(lastIncludedLog);
                // follower 没有并发请求需要处理，在锁内删除。
                RemoveLogReverse(lastIncludedLog.Index - 1, FirstIndex);
                RemoveLogAndCancelStart(lastIncludedLog.Index + 1, LastIndex);
                LastIndex = lastIncludedLog.Index;
                FirstIndex = lastIncludedLog.Index;
                CommitIndex = FirstIndex;
                LastApplied = FirstIndex;

                // 8. Reset state machine using snapshot contents (and load
                // snapshot’s cluster configuration)
                Raft.StateMachine.LoadFromSnapshot(s.Name);
                logger.Debug("{0} EndReceiveInstallSnapshot Path={1}", Raft.Name, s.Name);
            }
        }

        public void StartSnapshot(bool NeedNow = false)
        {
            lock (Raft)
            {
                if (Snapshotting || InstallSnapshotting.Count > 0)
                {
                    return;
                }

                if (LastApplied - FirstIndex < Raft.RaftConfig.SnapshotMinLogCount && false == NeedNow)
                {
                    return;
                }

                Snapshotting = true;
            }
            try
            {
                long LastIncludedIndex;
                long LastIncludedTerm;
                var path = Path.Combine(Raft.RaftConfig.DbHome, SnapshotFileName);

                // 忽略Snapshot返回结果。肯定是重复调用导致的。
                // out 结果这里没有使用，定义在参数里面用来表示这个很重要。
                Raft.StateMachine.Snapshot(path, out LastIncludedIndex, out LastIncludedTerm);
                logger.Debug("{0} Snapshot Path={1} LastIndex={2} LastTerm={3}",
                    Raft.Name, path, LastIncludedIndex, LastIncludedTerm);
            }
            finally
            {
                lock (Raft)
                {
                    Snapshotting = false;
                }
            }
        }

        private void InstallSnapshot(string path, Server.ConnectorEx connector)
        {
            // 整个安装成功结束时设置。中间Break(return)不设置。
            // 后面 finally 里面使用这个标志
            bool InstallSuccess = false;
            logger.Debug("{0} InstallSnapshot Start... Path={1} ToConnector={2}",
                Raft.Name, path, connector.Name);
            try
            {
                var snapshotFile = new FileStream(path, FileMode.Open);
                long offset = 0;
                var buffer = new byte[32 * 1024];
                var FirstLog = ReadLog(FirstIndex);
                var trunkArg = new InstallSnapshotArgument();
                trunkArg.Term = Term;
                trunkArg.LeaderId = Raft.LeaderId;
                trunkArg.LastIncludedIndex = FirstLog.Index;
                trunkArg.LastIncludedTerm = FirstLog.Term;

                while (!trunkArg.Done && Raft.IsLeader)
                {
                    int rc = snapshotFile.Read(buffer);
                    trunkArg.Offset = offset;
                    trunkArg.Data = new Binary(buffer, 0, rc);
                    trunkArg.Done = rc < buffer.Length;
                    offset += rc;

                    if (trunkArg.Done)
                        trunkArg.LastIncludedLog = new Binary(FirstLog.Encode());

                    while (Raft.IsLeader)
                    {
                        var socket = connector.WaitReady();
                        connector.AppendLogActiveTime = Util.Time.NowUnixMillis;
                        var future = new TaskCompletionSource<int>();
                        var r = new InstallSnapshot() { Argument = trunkArg };
                        if (!r.Send(socket,
                            (_) =>
                            {
                                future.SetResult(0);
                                return Procedure.Success;
                            }))
                        {
                            continue;
                        }
                        future.Task.Wait();
                        if (r.IsTimeout)
                            continue;

                        lock (Raft)
                        {
                            if (this.TrySetTerm(r.Result.Term) == SetTermResult.Newer)
                            {
                                // new term found.
                                Raft.ConvertStateTo(Raft.RaftState.Follower);
                                return;
                            }
                        }

                        switch (r.ResultCode)
                        {
                            case global::Zeze.Raft.InstallSnapshot.ResultCodeNewOffset:
                                break;

                            default:
                                logger.Warn($"InstallSnapshot Break ResultCode={r.ResultCode}");
                                return;
                        }

                        if (r.Result.Offset >= 0)
                        {
                            if (r.Result.Offset > snapshotFile.Length)
                            {
                                logger.Error($"InstallSnapshot.Result.Offset Too Big.{r.Result.Offset}/{snapshotFile.Length}");
                                return; // 中断安装。
                            }
                            offset = r.Result.Offset;
                            snapshotFile.Seek(offset, SeekOrigin.Begin);
                        }
                        break;
                    }
                }
                InstallSuccess = Raft.IsLeader;
                logger.Debug("{0} InstallSnapshot [SUCCESS] Path={1} ToConnector={2}",
                    Raft.Name, path, connector.Name);
            }
            finally
            {
                lock (Raft)
                {
                    connector.InstallSnapshotting = false;
                    InstallSnapshotting.Remove(connector.Name);
                    if (InstallSuccess)
                    {
                        // 安装完成，重新初始化，使得以后的AppendEnties能继续工作。
                        var next = ReadLog(FirstIndex + 1);
                        connector.NextIndex = next == null ? FirstIndex + 1 : next.Index;
                    }
                }
            }
        }

        private void StartInstallSnapshot(Server.ConnectorEx connector)
        {
            if (connector.InstallSnapshotting)
            {
                return;
            }
            var path = Path.Combine(Raft.RaftConfig.DbHome, SnapshotFileName);
            // 如果 Snapshotting，此时不启动安装。
            // 以后重试 AppendEntries 时会重新尝试 Install.
            if (File.Exists(path) && false == Snapshotting)
            {
                connector.InstallSnapshotting = true;
                InstallSnapshotting[connector.Name] = connector;
                Zeze.Util.Task.Run(() => InstallSnapshot(path, connector),
                    $"InstallSnapshot To '{connector.Name}'");
            }
            else
            {
                // 这一般的情况是snapshot文件被删除了。
                // 【注意】这种情况也许报错更好？
                // 内部会判断，不会启动多个snapshot。
                StartSnapshot(true);
            }
        }

        private long ProcessAppendEntriesResult(Server.ConnectorEx connector, Protocol p)
        {
            // 这个rpc处理流程总是返回 Success，需要统计观察不同的分支的发生情况，再来定义不同的返回值。

            var r = p as AppendEntries;
            bool resend = false;
            lock (Raft)
            {
                resend = r.IsTimeout && Raft.IsLeader;
            }
            if (resend)
            {
                TrySendAppendEntries(connector, r);  //resend
                return Procedure.Success;
            }

            lock (Raft)
            {
                if (Raft.LogSequence.TrySetTerm(r.Result.Term) == SetTermResult.Newer)
                {
                    Raft.LeaderId = string.Empty; // 此时不知道谁是Leader。
                                                  // new term found.
                    Raft.ConvertStateTo(Raft.RaftState.Follower);
                    // 发现新的 Term，已经不是Leader，不能继续处理了。
                    // 直接返回。
                    connector.Pending = null;
                    return Procedure.Success;
                }

                if (false == Raft.IsLeader)
                {
                    connector.Pending = null;
                    return Procedure.Success;
                }
            }

            if (r.Result.Success)
            {
                lock (Raft)
                {
                    TryCommit(r, connector);
                }
                // TryCommit 推进了NextIndex，
                // 可能日志没有复制完或者有新的AppendLog。
                // 尝试继续复制日志。
                // see TrySendAppendEntries 内的
                // “限制一次发送的日志数量”
                TrySendAppendEntries(connector, r);
                return Procedure.Success;
            }

            // 日志同步失败，调整NextIndex，再次尝试。
            lock (Raft)
            {
                // TODO raft.pdf 提到一个优化
                connector.NextIndex--;
                TrySendAppendEntries(connector, r);  //resend. use new NextIndex。
                return Procedure.Success;
            }
        }

        internal void TrySendAppendEntries(Server.ConnectorEx connector, AppendEntries pending)
        {
            lock (Raft)
            {
                connector.AppendLogActiveTime = Util.Time.NowUnixMillis;
                if (false == Raft.IsLeader)
                    return; // skip if is not a leader

                if (connector.Pending != pending)
                    return;

                // 先清除，下面中断(return)不用每次自己清除。
                connector.Pending = null;

                // 【注意】
                // 正在安装Snapshot，此时不复制日志，肯定失败。
                // 不做这个判断也是可以工作的，算是优化。
                if (connector.InstallSnapshotting)
                    return;

                var socket = connector.TryGetReadySocket();
                if (null == socket)
                    return;

                if (connector.NextIndex > LastIndex)
                    return;

                var nextLog = ReadLogReverse(connector.NextIndex);
                if (nextLog.Index == FirstIndex)
                {
                    // 已经到了日志开头，此时不会有prev-log，无法复制日志了。
                    // 这一般发生在Leader进行了Snapshot，但是Follower的日志还更老。
                    // 新起的Follower也一样。
                    StartInstallSnapshot(connector);
                    return;
                }

                // 现在Index总是递增，但没有确认步长总是为1，这样能处理不为1的情况。
                connector.NextIndex = nextLog.Index;

                connector.Pending = new AppendEntries();
                connector.Pending.Argument.Term = Term;
                connector.Pending.Argument.LeaderId = Raft.Name;
                connector.Pending.Argument.LeaderCommit = CommitIndex;

                // 肯定能找到的。
                var prevLog = ReadLogReverse(nextLog.Index - 1);
                connector.Pending.Argument.PrevLogIndex = prevLog.Index;
                connector.Pending.Argument.PrevLogTerm = prevLog.Term;

                // 限制一次发送的日志数量，【注意】这个不是raft要求的。
                int maxCount = Raft.RaftConfig.MaxAppendEntiresCount;
                RaftLog lastCopyLog = nextLog;
                for (var copyLog = nextLog;
                    maxCount > 0 && null != copyLog && copyLog.Index <= LastIndex;
                    copyLog = ReadLog(copyLog.Index + 1), --maxCount
                    )
                {
                    lastCopyLog = copyLog;
                    connector.Pending.Argument.Entries.Add(new Binary(copyLog.Encode()));
                }
                connector.Pending.Argument.LastEntryIndex = lastCopyLog.Index;
                if (false == connector.Pending.Send(socket,
                    (p) => ProcessAppendEntriesResult(connector, p),
                    Raft.RaftConfig.AppendEntriesTimeout))
                {
                    connector.Pending = null;
                    // Hearbeat Will Retry
                }
            }
        }

        internal RaftLog LastRaftLog()
        {
            return ReadLog(LastIndex);
        }

        private void RemoveLogAndCancelStart(long startIndex, long endIndex)
        {
            for (long index = startIndex; index <= endIndex; ++index)
            {
                if (index > LastApplied && WaitApplyFutures.TryRemove(index, out var future))
                {
                    // 还没有applied的日志被删除，
                    // 当发生在重新选举，但是旧的leader上还有一些没有提交的请求时，
                    // 需要取消。
                    // 其中判断：index > LastApplied 不是必要的。
                    // Apply的时候已经TryRemove了，仅会成功一次。
                    future.TrySetCanceled();
                }
                RemoveLog(index);
            }
        }

        private void RemoveLog(long index)
        {
            var raftLog = ReadLog(index);
            if (null != raftLog)
            {
                var key = ByteBuffer.Allocate();
                key.WriteLong(index);
                Logs.Remove(key.Bytes, key.Size, null, WriteOptionsSync);
                if (raftLog.Log.Unique.RequestId > 0)
                    OpenUniqueRequests(raftLog.Log.CreateTime).Remove(raftLog);
            }
        }

        internal long FollowerOnAppendEntries(AppendEntries r)
        {
            LeaderActiveTime = Zeze.Util.Time.NowUnixMillis;
            r.Result.Term = Term; // maybe rewrite later
            r.Result.Success = false; // set default false

            if (r.Argument.Term < Term)
            {
                // 1. Reply false if term < currentTerm (§5.1)
                r.SendResult();
                logger.Info("this={0} Leader={1} Index={2} term < currentTerm", Raft.Name, r.Argument.LeaderId, r.Argument.PrevLogIndex);
                return Procedure.Success;
            }

            switch (TrySetTerm(r.Argument.Term))
            {
                case SetTermResult.Newer:
                    Raft.ConvertStateTo(Raft.RaftState.Follower);
                    Raft.LeaderId = r.Argument.LeaderId;
                    r.Result.Term = Term; // new term

                    // 有Leader，清除一下上一次选举的投票。要不然可能下一次选举无法给别人投票。
                    // 这个不是必要的：因为要进行选举的时候，自己肯定也会尝试选自己，会重置，
                    // 但是清除一下，可以让选举更快进行。不用等待选举TimerTask。
                    SetVoteFor(string.Empty);
                    break;

                case SetTermResult.Same:
                    // 有Leader，清除一下上一次选举的投票。要不然可能下一次选举无法给别人投票。
                    // 这个不是必要的：因为要进行选举的时候，自己肯定也会尝试选自己，会重置，
                    // 但是清除一下，可以让选举更快进行。不用等待选举TimerTask。
                    SetVoteFor(string.Empty);

                    switch (Raft.State)
                    {
                        case Raft.RaftState.Candidate:
                            // see raft.pdf 文档. 仅在 Candidate 才转。【找不到在文档哪里了，需要确认这点】
                            Raft.ConvertStateTo(Raft.RaftState.Follower);
                            Raft.LeaderId = r.Argument.LeaderId;
                            break;

                        case Raft.RaftState.Leader:
                            logger.Fatal($"Receive AppendEntries from another leader={r.Argument.LeaderId} with same term={Term}, there must be a bug. this={Raft.LeaderId}");
                            Environment.Exit(0);
                            return 0;
                    }
                    break;
            }

            Raft.SetWithholdVotesUntil();

            // is Hearbeat(KeepAlive)
            if (r.Argument.Entries.Count == 0)
            {
                r.Result.Success = true;
                r.SendResult();
                return Procedure.Success;
            }

            // check and copy log ...
            var prevLog = ReadLog(r.Argument.PrevLogIndex);
            if (prevLog == null || prevLog.Term != r.Argument.PrevLogTerm)
            {
                // 2. Reply false if log doesn’t contain an entry
                // at prevLogIndex whose term matches prevLogTerm(§5.3)
                r.SendResult();
                logger.Debug("this={0} Leader={1} Index={2} prevLog mismatch", Raft.Name, r.Argument.LeaderId, r.Argument.PrevLogIndex);
                return Procedure.Success;
            }

            int entryIndex = 0;
            var copyLogIndex = prevLog.Index + 1;
            for (; entryIndex < r.Argument.Entries.Count; ++entryIndex, ++copyLogIndex)
            {
                var copyLog = RaftLog.Decode(r.Argument.Entries[entryIndex], Raft.StateMachine.LogFactory);
                if (copyLog.Index != copyLogIndex)
                {
                    logger.Fatal($"copyLog.Index != copyLogIndex Leader={r.Argument.LeaderId} this={Raft.Name}");
                    Environment.Exit(0);
                }
                if (copyLog.Index < FirstIndex)
                    continue; // 快照建立以前的日志忽略。

                // 本地已经存在日志。
                if (copyLog.Index <= LastIndex)
                {
                    var conflictCheck = ReadLog(copyLog.Index);
                    if (conflictCheck.Term == copyLog.Term)
                        continue;

                    // 3. If an existing entry conflicts
                    // with a new one (same index but different terms),
                    // delete the existing entry and all that follow it(§5.3)
                    // raft.pdf 5.3
                    if (conflictCheck.Index <= CommitIndex)
                    {
                        logger.Fatal("truncate committed entries");
                        Environment.Exit(0);
                    }
                    RemoveLogAndCancelStart(conflictCheck.Index, LastIndex);
                    LastIndex = conflictCheck.Index - 1;
                }
                break;
            }
            // Append this and all following entries.
            // 4. Append any new entries not already in the log
            for (; entryIndex < r.Argument.Entries.Count; ++entryIndex, ++copyLogIndex)
            {
                SaveLogRaw(copyLogIndex, r.Argument.Entries[entryIndex].Bytes);
            }

            copyLogIndex--;
            // 必须判断，防止本次AppendEntries都是旧的。
            if (copyLogIndex > LastIndex)
                LastIndex = copyLogIndex;

            CheckDump(prevLog.Index, copyLogIndex, r.Argument.Entries);

            // 5. If leaderCommit > commitIndex,
            // set commitIndex = min(leaderCommit, index of last new entry)
            if (r.Argument.LeaderCommit > CommitIndex)
            {
                CommitIndex = Math.Min(r.Argument.LeaderCommit, LastRaftLog().Index);
                TryApply(ReadLog(CommitIndex));
            }
            r.Result.Success = true;
            logger.Debug("{0}: {1}", Raft.Name, r);
            r.SendResultCode(0);

            return Procedure.Success;
        }

        private void CheckDump(long prevLogIndex, long lastIndex, List<Binary> entries)
        {
            var logs = new StringBuilder();
            for (var index = prevLogIndex + 1; index <= lastIndex; ++index)
            {
                logs.Append(ReadLog(index).ToString()).Append("\n");
            }
            var copies = new StringBuilder();
            foreach (var entry in entries)
            {
                copies.Append(RaftLog.Decode(entry, Raft.StateMachine.LogFactory).ToString()).Append("\n");
            }

            if (logs.ToString().Equals(copies.ToString()))
                return;

            logger.Fatal("================= logs ======================");
            logger.Fatal(logs);
            logger.Fatal("================= copies ======================");
            logger.Fatal(copies);
            Environment.Exit(0);
        }
    }
}
