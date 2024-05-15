using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using System.Collections.Concurrent;
using System.IO;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using System.Globalization;
using RocksDbSharp;
using Zeze.Util;
using DotNext.Threading;
using System.Threading;

namespace Zeze.Raft
{
    public sealed class HeartbeatLog : Log
    {
        public const int SetLeaderReadyEvent = 1;

        public int Operate { get; private set; }
        public string Info { get; private set; }

        public HeartbeatLog(int operate = 0, string info = null)
            : base(null)
        {
            Operate = operate;
            Info = info ?? string.Empty;
        }

        public override Task Apply(RaftLog holder, StateMachine stateMachine)
        {
            switch (Operate)
            {
                case SetLeaderReadyEvent:
                    stateMachine.Raft.SetLeaderReady(holder);
                    break;
            }

            return Task.CompletedTask;
        }

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
            Operate = bb.ReadUInt();
            Info = bb.ReadString();
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
            bb.WriteUInt(Operate);
            bb.WriteString(Info);
        }

        public override string ToString()
        {
            return $"{base.ToString()} Info={Info}";
        }
    }

    public sealed class RaftLog : Serializable
    {
        public long Term { get; private set; }
        public long Index { get; private set; }
        public Log Log { get; private set; }

        // 不会被系列化。Local Only.
        public Func<int, Log> LogFactory { get; }
        public TaskCompletionSource<int> LeaderFuture { get; internal set; }

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

        public static RaftLog DecodeTermIndex(byte[] data)
        {
            var bb = ByteBuffer.Wrap(data);
            var term = bb.ReadLong();
            var index = bb.ReadLong();
            return new RaftLog(term, index, null);
        }
    }

    public class LogSequence
    {
        internal static readonly ILogger logger = LogManager.GetLogger(typeof(LogSequence));

        public Raft Raft { get; }

        public long Term { get; private set; }
        public long LastIndex { get; private set; }
        // 用来处理NextIndex回溯时限制搜索。snapshot需要修订这个值。
        public long FirstIndex { get; private set; }
        public long CommitIndex { get; private set; }
        public long LastApplied { get; private set; }

        // 这个不是日志需要的，因为持久化，所以就定义在这里吧。
        internal string VoteFor { get; private set; }
        internal bool NodeReady { get; private set; }
        internal long LastLeaderCommitIndex { get; private set; }

        // 初始化的时候会加入一条日志(Index=0，不需要真正apply)，
        // 以后Snapshot时，会保留LastApplied的。
        // 所以下面方法不会返回空。
        // 除非什么例外发生。那就抛空指针异常吧。
        public async Task<RaftLog> LastAppliedLogTermIndex()
        {
            return RaftLog.DecodeTermIndex(await ReadLogBytes(LastApplied));
        }

        private async Task SaveFirstIndex(long newFirstIndex)
        {
            var firstIndexValue = ByteBuffer.Allocate();
            firstIndexValue.WriteLong(newFirstIndex);
            await Rafts.PutAsync(
                RaftsFirstIndexKey, RaftsFirstIndexKey.Length,
                firstIndexValue.Bytes, firstIndexValue.Size,
                null, WriteOptions
                );
            FirstIndex = newFirstIndex;
        }

        public async Task CommitSnapshot(string path, long newFirstIndex)
        {
            using (await Raft.Monitor.EnterAsync())
            {
                await CommitSnapshotNoLock(path, newFirstIndex);
            }
        }

        private async Task CommitSnapshotNoLock(string path, long newFirstIndex)
        {
            File.Move(path, SnapshotFullName, true);
            await SaveFirstIndex(newFirstIndex);
            StartRemoveLogOnlyBefore(newFirstIndex);
        }

        internal volatile TaskCompletionSource<bool> RemoveLogBeforeFuture;
        internal volatile bool LogsAvailable = false;

        private void StartRemoveLogOnlyBefore(long index)
        {
            // has under lockraft
            {
                if ((null != RemoveLogBeforeFuture && false == RemoveLogBeforeFuture.Task.IsCompleted)
                    || false == LogsAvailable || Raft.IsShutdown)
                {
                    return;
                }
                RemoveLogBeforeFuture = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            }

            // 直接对 RocksDb 多线程访问，这里就不做多线程保护了。
            _ = Task.Run(() =>
            {
                try
                {
                    using var it = Logs.RocksDb.NewIterator();
                    it.SeekToFirst();
                    while (LogsAvailable && false == Raft.IsShutdown && it.Valid())
                    {
                        if (ByteBuffer.Wrap(it.Key()).ReadLong() >= index)
                        {
                            RemoveLogBeforeFuture.TrySetResult(true);
                            return;
                        }

                        var key = it.Key();
                        Logs.RocksDb.Remove(key, key.Length, null, WriteOptions);

                        // 删除快照前的日志时，不删除唯一请求存根，否则快照建立时刻前面一点时间的请求无法保证唯一。
                        // 唯一请求存根自己管理删除，
                        // 【注意】
                        // 服务器完全奔溃（数据全部丢失）后，重新配置一台新的服务器，仍然又很小的机会存在无法判断唯一。
                        // 此时比较好的做法时，从工作节点的数据库(unique/)复制出一份，作为开始数据。
                        // 参考 RemoveLogAndCancelStart

                        //if (raftLog.Log.Unique.RequestId > 0)
                        //    OpenUniqueRequests(raftLog.Log.CreateTime).Remove(raftLog);
                        it.Next();
                    }
                }
                finally
                {
                    RemoveLogBeforeFuture.TrySetResult(false);
                }
            });
        }

        // Leader
        // Follower
        public long LeaderActiveTime { get; internal set; } = Zeze.Util.Time.NowUnixMillis;

        private Util.AsyncRocksDb Logs { get; set; }
        private Util.AsyncRocksDb Rafts { get; set; }

        internal sealed class UniqueRequestSet
        {
            internal Util.AsyncRocksDb Db { get; set; }
            public string DbName { get; set; }

            public LogSequence LogSequence { get; set; }
            public AsyncLock Mutex { get; } = new();

            public UniqueRequestSet(LogSequence lq, string dbName)
            {
                LogSequence = lq;
                DbName = dbName;
            }

            private async Task Put(RaftLog log, bool isApply)
            {
                var db = await OpenDb();
                var key = ByteBuffer.Allocate(100);
                log.Log.Unique.Encode(key);

                // 先读取并检查状态，减少写操作。

                var existBytes = await db.GetAsync(key.Bytes, key.Size);
                if (false == isApply && existBytes != null)
                    throw new RaftRetryException($"Duplicate Request Found = {log.Log.Unique}");

                if (existBytes != null)
                {
                    var existState = new UniqueRequestState();
                    existState.Decode(ByteBuffer.Wrap(existBytes));
                    if (existState.IsApplied)
                        return;
                }

                var value = ByteBuffer.Allocate(4096);
                new UniqueRequestState(log, isApply).Encode(value);
                await db.PutAsync(key.Bytes, key.Size, value.Bytes, value.Size, null, LogSequence.WriteOptions);
            }

            public async Task Save(RaftLog log)
            {
                await Put(log, false);
            }

            public async Task Apply(RaftLog log)
            {
                await Put(log, true);
            }

            public async Task Remove(RaftLog log)
            {
                var key = ByteBuffer.Allocate(100);
                log.Log.Unique.Encode(key);
                await (await OpenDb()).RemoveAsync(key.Bytes, key.Size, null, LogSequence.WriteOptions);
            }

            public async Task<UniqueRequestState> GetRequestState(IRaftRpc iraftrpc)
            {
                var key = ByteBuffer.Allocate(100);
                iraftrpc.Unique.Encode(key);
                var val = await (await OpenDb()).GetAsync(key.Bytes, key.Size);
                if (null == val)
                    return null;
                var bb = ByteBuffer.Wrap(val);
                var state = new UniqueRequestState();
                state.Decode(bb);
                return state;
            }

            private async Task<Util.AsyncRocksDb> OpenDb()
            {
                using (await Mutex.AcquireAsync(CancellationToken.None))
                {
                    if (null == Db)
                    {
                        var dir = Path.Combine(LogSequence.Raft.RaftConfig.DbHome, "unique");
                        Util.FileSystem.CreateDirectory(dir);
                        Db = await Util.AsyncRocksDb.OpenAsync(
                            new DbOptions().SetCreateIfMissing(true),
                            Path.Combine(dir, DbName),
                            LogSequence.Raft.AsyncExecutor);
                    }
                    return Db;
                }
            }

            public void Dispose()
            {
                Db?.RocksDb.Dispose();
                Db = null;
            }
        }
        private ConcurrentDictionary<string, UniqueRequestSet> UniqueRequestSets { get; } = new();

        internal void RemoveExpiredUniqueRequestSet()
        {
            var expired = DateTime.Now.AddDays(-(Raft.RaftConfig.UniqueRequestExpiredDays + 1));

            const string format = "yyyy.M.d";
            CultureInfo provider = CultureInfo.InvariantCulture;
            var uniqueHome = Path.Combine(Raft.RaftConfig.DbHome, "unique");

            // try close and delete
            foreach (var reqsets in UniqueRequestSets)
            {
                var db = DateTime.ParseExact(reqsets.Key, format, provider);
                if (db < expired)
                {
                    reqsets.Value.Dispose();
                    UniqueRequestSets.TryRemove(reqsets.Key, out _);
                    Util.FileSystem.DeleteDirectory(Path.Combine(uniqueHome, reqsets.Key));
                }
            }
            // try delete in dirs
            if (Directory.Exists(uniqueHome))
            {
                foreach (var dir in Directory.EnumerateDirectories(uniqueHome))
                {
                    var dirname = Path.GetFileName(dir);
                    var db = DateTime.ParseExact(dirname, format, provider);
                    if (db < expired)
                    {
                        Util.FileSystem.DeleteDirectory(Path.Combine(uniqueHome, dir));
                    }
                }
            }
        }

        internal void CancelPendingAppendLogFutures()
        {
            foreach (var job in LeaderAppendLogs.Values)
            {
                job.LeaderFuture.TrySetCanceled();
            }
            LeaderAppendLogs.Clear();
        }

        internal void Close()
        {
            // must after set Raft.IsShutdown = false;
            CancelPendingAppendLogFutures();

            Logs?.RocksDb.Dispose();
            Logs = null;
            Rafts?.RocksDb.Dispose();
            Rafts = null;
            foreach (var uniqreqsets in UniqueRequestSets.Values)
            {
                uniqreqsets.Db?.RocksDb.Dispose();
            }
            UniqueRequestSets.Clear();
        }

        public LogSequence(Raft raft)
        {
            Raft = raft;
        }

        public async Task OpenAsync()
        {
            var options = new DbOptions().SetCreateIfMissing(true);

            Rafts = await Util.AsyncRocksDb.OpenAsync(options,
                Path.Combine(Raft.RaftConfig.DbHome, "rafts"), Raft.AsyncExecutor);
            {
                // Read Term
                var termKey = ByteBuffer.Allocate();
                termKey.WriteInt(0);
                RaftsTermKey = termKey.Copy();
                var termValue = await Rafts.GetAsync(RaftsTermKey);
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
                var voteForvalue = await Rafts.GetAsync(RaftsVoteForKey);
                if (null != voteForvalue)
                {
                    var bb = ByteBuffer.Wrap(voteForvalue);
                    VoteFor = bb.ReadString();
                }
                else
                {
                    VoteFor = string.Empty;
                }
                // Read FirstIndex 由于snapshot并发，Logs中的第一条记录可能不是FirstIndex了。
                var firstIndexKey = ByteBuffer.Allocate();
                firstIndexKey.WriteInt(2);
                RaftsFirstIndexKey = firstIndexKey.Copy();
                var firstIndexValue = await Rafts.GetAsync(RaftsFirstIndexKey);
                if (null != firstIndexValue)
                {
                    var bb = ByteBuffer.Wrap(firstIndexValue);
                    FirstIndex = bb.ReadLong();
                }
                else
                {
                    FirstIndex = -1; // never snapshot. will re-initialize later.
                }
                // NodeReady
                // 节点第一次启动，包括机器毁坏后换了新机器再次启动时为 false。
                // 当满足以下条件之一：
                // 1. 成为Leader并且Ready
                // 2. 成为Follower并在处理AppendEntries时观察到LeaderCommit发生了变更
                // 满足条件以后设置 NodeReady 为 true。
                // 这个条件影响投票逻辑：NodeReady 为 true 以前，只允许给 Candidate.LastIndex == 0 的节点投票。
                var nodeReadyKey = ByteBuffer.Allocate();
                nodeReadyKey.WriteInt(3);
                RaftsNodeReadyKey = nodeReadyKey.Copy();
                var nodeReadyValue = await Rafts.GetAsync(RaftsNodeReadyKey);
                if (null != nodeReadyValue)
                {
                    var bb = ByteBuffer.Wrap(nodeReadyValue);
                    NodeReady = bb.ReadBool();
                }
            }


            Logs = await Util.AsyncRocksDb.OpenAsync(options,
                Path.Combine(Raft.RaftConfig.DbHome, "logs"), Raft.AsyncExecutor);
            await Raft.AsyncExecutor.RunAsync(() =>
            {
                // Read Last Log Index
                using var itLast = Logs.RocksDb.NewIterator();
                itLast.SeekToLast();
                if (itLast.Valid())
                {
                    LastIndex = RaftLog.DecodeTermIndex(itLast.Value()).Index;
                }
                else
                {
                    // empty. add one for prev.
                    SaveLogSynchronously(new RaftLog(Term, 0, new HeartbeatLog()));
                    LastIndex = 0;
                }
                logger.Info($"{Raft.Name}-{Raft.IsLeader} {Raft.RaftConfig.DbHome} LastIndex={LastIndex} Count={GetTestStateMachineCount()}");

                // 【注意】snapshot 以后 FirstIndex 会推进，不再是从0开始。
                if (FirstIndex == -1) // never snapshot
                {
                    using var itFirst = Logs.RocksDb.NewIterator();
                    itFirst.SeekToFirst();
                    FirstIndex = RaftLog.Decode(new Binary(itFirst.Value()), Raft.StateMachine.LogFactory).Index;
                }
                LastApplied = FirstIndex;
                CommitIndex = FirstIndex;
            });
            LogsAvailable = true;

            // 可能有没有被清除的日志存在。启动任务。
            StartRemoveLogOnlyBefore(FirstIndex);
        }

        private async Task TrySetNodeReady()
        {
            if (NodeReady)
                return;

            NodeReady = true;

            var value = ByteBuffer.Allocate();
            value.WriteBool(true);
            await Rafts.PutAsync(RaftsNodeReadyKey, RaftsNodeReadyKey.Length, value.Bytes, value.Size, null, WriteOptions);
        }

        internal async Task<(bool, UniqueRequestState)> TryGetRequestState(Protocol p)
        {
            var iraftrpc = p as IRaftRpc;

            var create = Util.Time.UnixMillisToDateTime(iraftrpc.CreateTime);
            var now = DateTime.Now;
            if ((now - create).Days >= Raft.RaftConfig.UniqueRequestExpiredDays)
                return (true, null);

            return (false, await OpenUniqueRequests(iraftrpc.CreateTime).GetRequestState(iraftrpc));
        }

        private UniqueRequestSet OpenUniqueRequests(long time)
        {
            var dateTime = Util.Time.UnixMillisToDateTime(time);
            var dbName = $"{dateTime.Year}.{dateTime.Month}.{dateTime.Day}";
            return UniqueRequestSets.GetOrAdd(dbName, (db) => new UniqueRequestSet(this, db));
        }

        private byte[] RaftsTermKey;
        private byte[] RaftsVoteForKey;
        private byte[] RaftsFirstIndexKey;
        private byte[] RaftsNodeReadyKey; // 只会被写一次，所以这个优化可以不做，统一形式吧。

        public WriteOptions WriteOptions { get; set; } = new WriteOptions().SetSync(true);

        private void SaveLogSynchronously(RaftLog log)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(log.Index);
            var value = log.Encode();
            // key,value offset must 0
            Logs.RocksDb.Put(
                key.Bytes, key.Size,
                value.Bytes, value.Size,
                null, WriteOptions
                );

            logger.Debug($"{Raft.Name}-{Raft.IsLeader} RequestId={log.Log.Unique.RequestId} Index={log.Index} Count={GetTestStateMachineCount()}");
        }

        private async Task SaveLog(RaftLog log)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(log.Index);
            var value = log.Encode();
            // key,value offset must 0
            await Logs.PutAsync(
                key.Bytes, key.Size,
                value.Bytes, value.Size,
                null, WriteOptions
                );

            logger.Info($"{Raft.Name}-{Raft.IsLeader} RequestId={log.Log.Unique.RequestId} Index={log.Index} Count={GetTestStateMachineCount()}");
        }

        private async Task SaveLogRaw(long index, byte[] rawValue)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(index);

            await Logs.PutAsync(
                key.Bytes, key.Size,
                rawValue, rawValue.Length,
                null, WriteOptions
                );

            logger.Debug($"{Raft.Name}-{Raft.IsLeader} RequestId=? Index={index} Count={GetTestStateMachineCount()}");
        }

        private async Task<byte[]> ReadLogBytes(long index)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong(index);
            return await Logs?.GetAsync(key.Bytes, key.Size);
        }

        private async Task<RaftLog> ReadLog(long index)
        {
            var value = await ReadLogBytes(index);
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
        internal async Task<SetTermResult> TrySetTerm(long term)
        {
            if (term > Term)
            {
                Term = term;
                var termValue = ByteBuffer.Allocate();
                termValue.WriteLong(term);
                await Rafts.PutAsync(RaftsTermKey, RaftsTermKey.Length, termValue.Bytes, termValue.Size, null, WriteOptions);
                Raft.LeaderId = string.Empty;
                await SetVoteFor(string.Empty);
                LastLeaderCommitIndex = 0;
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

        internal async Task SetVoteFor(string voteFor)
        {
            if (false == VoteFor.Equals(voteFor))
            {
                VoteFor = voteFor;
                var voteForValue = ByteBuffer.Allocate();
                voteForValue.WriteString(voteFor);
                await Rafts.PutAsync(RaftsVoteForKey, RaftsVoteForKey.Length, voteForValue.Bytes, voteForValue.Size, null, WriteOptions);
            }
        }

        private async Task TryCommit(AppendEntries rpc, Server.ConnectorEx connector)
        {
            connector.NextIndex = rpc.Argument.LastEntryIndex + 1;
            connector.MatchIndex = rpc.Argument.LastEntryIndex;

            // 旧的 AppendEntries 的结果，不用继续处理了。
            // 【注意】这个不是必要的，是一个小优化。
            if (rpc.Argument.LastEntryIndex <= CommitIndex)
                return;

            // find MaxMajorityLogIndex
            // Rules for Servers
            // If there exists an N such that N > commitIndex, a majority
            // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
            // set commitIndex = N(§5.3, §5.4).
            var followers = new List<Server.ConnectorEx>();
            Raft.Server.Config.ForEachConnector((c) => followers.Add(c as Server.ConnectorEx));
            followers.Sort((a, b) => b.MatchIndex.CompareTo(a.MatchIndex));
            var maxMajorityLogIndex = followers[Raft.RaftConfig.HalfCount - 1].MatchIndex;
            if (maxMajorityLogIndex > CommitIndex)
            {
                var maxMajorityLog = await ReadLog(maxMajorityLogIndex);
                if (maxMajorityLog.Term != Term)
                {
                    // 如果是上一个 Term 未提交的日志在这一次形成的多数派，
                    // 不自动提交。
                    // 总是等待当前 Term 推进时，顺便提交它。
                    return;
                }
                // 推进！
                CommitIndex = maxMajorityLogIndex;
                await TrySetNodeReady();
                await TryStartApplyTask(maxMajorityLog);
            }
        }

        internal volatile TaskCompletionSource<bool> ApplyFuture; // follower background apply task

        // under lock (Raft)
        private async Task TryStartApplyTask(RaftLog lastApplyableLog)
        {
            if ((null == ApplyFuture || ApplyFuture.Task.IsCompleted) && false == Raft.IsShutdown)
            {
                // 仅在没有 apply 进行中才尝试进行处理。
                if (CommitIndex - LastApplied < Raft.RaftConfig.BackgroundApplyCount)
                {
                    // apply immediately in current thread
                    await TryApply(lastApplyableLog, long.MaxValue);
                    return;
                }

                ApplyFuture = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
                _ = Task.Run(async () =>
                {
                    try
                    {
                        // 如果有人等待。
                        ApplyFuture.SetResult(await Util.Mission.CallAsync(BackgroundApply, "BackgroundApply") == 0);
                    }
                    finally
                    {
                        ApplyFuture.TrySetResult(false);
                    }
                });
            }
        }

        private async Task<long> BackgroundApply()
        {
            while (false == Raft.IsShutdown)
            {
                using (await Raft.Monitor.EnterAsync())
                {
                    // ReadLog Again，CommitIndex Maybe Grow.
                    var lastApplyableLog = await ReadLog(CommitIndex);
                    await TryApply(lastApplyableLog, Raft.RaftConfig.BackgroundApplyCount);
                    if (LastApplied == lastApplyableLog.Index)
                    {
                        return 0; // 本次Apply结束。
                    }
                }
                System.Threading.Thread.Yield();
            }
            return ResultCode.CancelException;
        }

        private async Task TryApply(RaftLog lastApplyableLog, long count)
        {
            if (null == lastApplyableLog)
            {
                logger.Error("lastApplyableLog is null.");
                return;
            }
            for (long index = LastApplied + 1;
                index <= lastApplyableLog.Index && count > 0;
                --count)
            {
                if (false == LeaderAppendLogs.TryRemove(index, out var raftLog))
                {
                    raftLog = await ReadLog(index);
                }
                if (null == raftLog)
                {
                    logger.Warn("What Happened! index={0} lastApplyable={1} LastApplied={2}",
                        index, lastApplyableLog.Index, LastApplied);
                    // trySnapshot(); // 错误的时候不做这个尝试了。
                    return; // end?
                }

                index = raftLog.Index + 1;
                await raftLog.Log.Apply(raftLog, Raft.StateMachine);
                if (raftLog.Log.Unique.RequestId > 0)
                    await OpenUniqueRequests(raftLog.Log.CreateTime).Apply(raftLog);
                LastApplied = raftLog.Index; // 循环可能退出，在这里修改。
                //*
                if (LastIndex - LastApplied < 5)
                    logger.Info($"{Raft.Name}-{Raft.IsLeader} {Raft.RaftConfig.DbHome} RequestId={raftLog.Log.Unique.RequestId} LastIndex={LastIndex} LastApplied={LastApplied} Count={GetTestStateMachineCount()}");
                // */
                raftLog.LeaderFuture?.TrySetResult(0);
            }
            trySnapshot();
        }

        private void trySnapshot()
        {
            var snapshotLogCount = Raft.RaftConfig.SnapshotLogCount;
            if (snapshotLogCount > 0)
            {
                if (LastApplied - PrevSnapshotIndex > snapshotLogCount)
                {
                    PrevSnapshotIndex = LastApplied;
                    _ = Util.Mission.CallAsync(Snapshot, "Snapshot");
                }
            }
            // else disable
        }

        internal long? GetTestStateMachineCount()
        {
            return (Raft.StateMachine as Test.TestStateMachine)?.Count;
        }

        internal ConcurrentDictionary<long, RaftLog> LeaderAppendLogs { get; } = new ConcurrentDictionary<long, RaftLog>();

        internal void SendHeartbeatTo(Server.ConnectorEx connector)
        {
            connector.AppendLogActiveTime = Util.Time.NowUnixMillis;

            if (false == Raft.IsLeader)
                return; // skip if is not a leader

            if (connector.Pending != null)
                return;

            if (InstallSnapshotting.ContainsKey(connector.Name))
                return;

            var socket = connector.TryGetReadySocket();
            if (null == socket)
            {
                // Heartbeat Will Retry
                return;
            }

            var heartbeat = new AppendEntries();
            heartbeat.Argument.Term = Term;
            heartbeat.Argument.LeaderId = Raft.Name;
            heartbeat.Send(socket, async (p) =>
            {
                if (heartbeat.IsTimeout)
                    return 0; // skip

                using (await Raft.Monitor.EnterAsync())
                {
                    if (await Raft.LogSequence.TrySetTerm(heartbeat.Result.Term) == SetTermResult.Newer)
                    {
                        // new term found.
                        await Raft.ConvertStateTo(Raft.RaftState.Follower);
                        return ResultCode.Success;
                    }
                    return 0;
                }
            }, Raft.RaftConfig.AppendEntriesTimeout);
        }

        internal async Task AppendLog(Log log)
        {
            if (false == Raft.IsLeader)
                throw new RaftRetryException(); // 快速失败

            long term;
            long index;
            TaskCompletionSource<int> future;

            using (await Raft.Monitor.EnterAsync())
            {
                (term, index, future) = await AppendLog(log, true);
            }

            try
            {
                await future.Task.WaitAsync(new TimeSpan(TimeSpan.TicksPerMillisecond * Raft.RaftConfig.AppendEntriesTimeout * 2));
            }
            catch (TimeoutException)
            {
                LeaderAppendLogs.TryRemove(index, out _);
                throw new RaftRetryException();
            }
        }

        internal async Task<(long, long, TaskCompletionSource<int>)> AppendLog(Log log, bool waitapplied)
        {
            TaskCompletionSource<int> future = null;
            // has under lock (Raft)
            {
                var raftLog = new RaftLog(Term, LastIndex + 1, log);
                if (raftLog.Log.Unique.RequestId > 0)
                    await OpenUniqueRequests(raftLog.Log.CreateTime).Save(raftLog);
                await SaveLog(raftLog);

                // 容易出错的放到前面。
                if (waitapplied)
                {
                    raftLog.LeaderFuture = new TaskCompletionSource<int>(TaskCreationOptions.RunContinuationsAsynchronously);
                    future = raftLog.LeaderFuture;
                    if (false == LeaderAppendLogs.TryAdd(raftLog.Index, raftLog))
                    {
                        logger.Fatal($"LeaderAppendLogs.TryAdd Fail. Index={raftLog.Index}");
                        Raft.FatalKill();
                    }
                }
                // 最后修改LastIndex。
                LastIndex = raftLog.Index;
                // 广播给followers并异步等待多数确认
                try
                {
                    await Raft.Server.Config.ForEachConnectorAsync(async (c) => await TrySendAppendEntries(c as Server.ConnectorEx, null));
                }
                catch (Exception)
                {
                    LastIndex--;
                    // 只有下面这个需要回滚，日志(SaveLog, OpenUniqueRequests(...).Save)以后根据LastIndex覆盖。
                    if (waitapplied)
                        LeaderAppendLogs.TryRemove(raftLog.Index, out _);
                    throw;
                }
            }
            return (Term, LastIndex, future);
        }

        // 是否正在创建Snapshot过程中，用来阻止新的创建请求。
        private bool Snapshotting { get; set; } = false;
        // 是否有安装进程正在进行中，用来阻止新的创建请求。
        internal ConcurrentDictionary<string, Server.ConnectorEx> InstallSnapshotting { get; }
            = new ConcurrentDictionary<string, Server.ConnectorEx>(); // key is Server.ConnectorEx.Name

        public const string SnapshotFileName = "snapshot.dat";
        public string SnapshotFullName => Path.Combine(Raft.RaftConfig.DbHome, SnapshotFileName);

        private long PrevSnapshotIndex;

        public async Task EndReceiveInstallSnapshot(FileStream s, InstallSnapshot r)
        {
            // cancel RemoveLogBefore
            LogsAvailable = false;
            await Util.Mission.AwaitNullableTask(RemoveLogBeforeFuture?.Task);

            using (await Raft.Monitor.EnterAsync())
            {
                try
                {
                    // 6. If existing log entry has same index and term as snapshot’s
                    // last included entry, retain log entries following it and reply
                    var last = await ReadLog(r.Argument.LastIncludedIndex);
                    if (null != last && last.Term == r.Argument.LastIncludedTerm)
                    {
                        // 【注意】没有错误处理：比如LastIncludedIndex是否超过CommitIndex之类的。
                        // 按照现在启动InstallSnapshot的逻辑，不会发生这种情况。
                        logger.Warn($"Exist Local Log. Do It Like A Local Snapshot!");
                        await CommitSnapshotNoLock(s.Name, r.Argument.LastIncludedIndex);
                        return;
                    }
                    // 7. Discard the entire log
                    // 整个删除，那么下一次AppendEntries又会找不到prev。不就xxx了吗?
                    // 我的想法是，InstallSnapshot 最后一个 trunk 带上 LastIncludedLog，
                    // 接收者清除log，并把这条日志插入（这个和系统初始化时插入的Index=0的日志道理差不多）。
                    // 【除了快照最后包含的日志，其他都删除。】
                    Logs.RocksDb.Dispose();
                    Logs = null;
                    CancelPendingAppendLogFutures();
                    var logsdir = Path.Combine(Raft.RaftConfig.DbHome, "logs");
                    Util.FileSystem.DeleteDirectory(logsdir);
                    var options = new DbOptions().SetCreateIfMissing(true);

                    Logs = await Util.AsyncRocksDb.OpenAsync(options, logsdir, Raft.AsyncExecutor);
                    var lastIncludedLog = RaftLog.Decode(r.Argument.LastIncludedLog, Raft.StateMachine.LogFactory);
                    await SaveLog(lastIncludedLog);
                    await CommitSnapshotNoLock(s.Name, lastIncludedLog.Index);

                    LastIndex = lastIncludedLog.Index;
                    CommitIndex = FirstIndex;
                    LastApplied = FirstIndex;

                    // 【关键】记录这个，放弃当前Term的投票。
                    await SetVoteFor(Raft.LeaderId);

                    // 8. Reset state machine using snapshot contents (and load
                    // snapshot’s cluster configuration)
                    await Raft.StateMachine.LoadSnapshot(SnapshotFullName);
                    logger.Info($"{Raft.Name} EndReceiveInstallSnapshot Path={s.Name}");
                }
                finally
                {
                    LogsAvailable = true;
                }
            }
        }

        public async Task Snapshot()
        {
            using (await Raft.Monitor.EnterAsync())
            {
                if (Snapshotting || !InstallSnapshotting.IsEmpty)
                {
                    return;
                }
                Snapshotting = true;
            }

            try
            {
                // 忽略Snapshot返回结果。肯定是重复调用导致的。
                // out 结果这里没有使用，定义在参数里面用来表示这个很重要。
                var path = Path.Combine(SnapshotFullName + ".tmp");
                var (result, lastIncludedTerm, lastIncludedIndex) = await Raft.StateMachine.Snapshot(path);
                logger.Info($"{Raft.Name} Snapshot Path={path} LastTerm={lastIncludedTerm} LastIndex={lastIncludedIndex}");
            }
            finally
            {
                using (await Raft.Monitor.EnterAsync())
                {
                    Snapshotting = false;
                }
            }
        }

        internal async Task CancelAllInstallSnapshot()
        {
            foreach (var installing in InstallSnapshotting.Values)
            {
                await EndInstallSnapshot(installing);
            }
        }

        internal async Task EndInstallSnapshot(Server.ConnectorEx c)
        {
            if (InstallSnapshotting.TryRemove(c.Name, out var cex))
            {
                var state = cex.InstallSnapshotState;
                logger.Info($"{Raft.Name} InstallSnapshot LastIncludedIndex={state.Pending.Argument.LastIncludedIndex} Done={state.Pending.Argument.Done} c={c.Name}");
                state.File.Close();
                if (state.Pending.Argument.Done && state.Pending.ResultCode == 0)
                {
                    cex.NextIndex = state.Pending.Argument.LastIncludedIndex + 1;

                    if (state.Pending.Argument.LastIncludedIndex > cex.MatchIndex) // see EndReceiveInstallSnapshot 6.
                        cex.MatchIndex = state.Pending.Argument.LastIncludedIndex;
                    // start log copy
                    await TrySendAppendEntries(c, null);
                }
            }
            c.InstallSnapshotState = null;
        }

        private async Task StartInstallSnapshot(Server.ConnectorEx c)
        {
            if (InstallSnapshotting.ContainsKey(c.Name))
            {
                return;
            }
            var path = SnapshotFullName;
            // 如果 Snapshotting，此时不启动安装。
            // 以后重试 AppendEntries 时会重新尝试 Install.
            if (File.Exists(path) && false == Snapshotting)
            {
                if (false == InstallSnapshotting.TryAdd(c.Name, c))
                    throw new Exception("Impossible");

                c.InstallSnapshotState = new InstallSnapshotState();
                var st = c.InstallSnapshotState;
                st.File = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read); ;
                st.FirstLog = await ReadLog(FirstIndex);
                st.Pending.Argument.Term = Term;
                st.Pending.Argument.LeaderId = Raft.Name;
                st.Pending.Argument.LastIncludedIndex = st.FirstLog.Index;
                st.Pending.Argument.LastIncludedTerm = st.FirstLog.Term;

                logger.Info($"{Raft.Name} InstallSnapshot Start... Path={path} c={c.Name}");
                await st.TrySend(this, c);
            }
            else
            {
                // 这情况一般是snapshot文件被删除了。【注意】这种情况也许报错更好？
                // 内部会判断，不会启动多个snapshot。
                // 需要在新的线程启动，原因: 1. 此时在Raft锁内，2. Raft锁现在不能重复。
                _ = Task.Run(async () => await Snapshot());
            }
        }

        private async Task<long> ProcessAppendEntriesResult(Server.ConnectorEx connector, Protocol p)
        {
            // 这个rpc处理流程总是返回 Success，需要统计观察不同的分支的发生情况，再来定义不同的返回值。
            var r = p as AppendEntries;

            using (await Raft.Monitor.EnterAsync())
            {
                if (r.IsTimeout && Raft.IsLeader)
                {
                    await TrySendAppendEntries(connector, r);  // timeout and resend
                    return ResultCode.Success;
                }

                if (await Raft.LogSequence.TrySetTerm(r.Result.Term) == SetTermResult.Newer)
                {
                    // new term found.
                    await Raft.ConvertStateTo(Raft.RaftState.Follower);
                    // 发现新的 Term，已经不是Leader，不能继续处理了。
                    // 直接返回。
                    connector.Pending = null;
                    return ResultCode.Success;
                }

                if (false == Raft.IsLeader)
                {
                    connector.Pending = null;
                    return ResultCode.Success;
                }

                if (r.Result.Success)
                {
                    await TryCommit(r, connector);
                    // TryCommit 推进了NextIndex，
                    // 可能日志没有复制完或者有新的AppendLog。
                    // 尝试继续复制日志。
                    // see TrySendAppendEntries 内的
                    // “限制一次发送的日志数量”
                    await TrySendAppendEntries(connector, r);
                    return ResultCode.Success;
                }

                // 日志同步失败，调整NextIndex，再次尝试。
                if (r.Result.NextIndex == 0)
                {
                    // 默认的回退模式。
                    --connector.NextIndex;
                }
                else if (r.Result.NextIndex <= FirstIndex)
                {
                    // leader snapshot，follower 完全没法匹配了，后续的 TrySendAppendEntries 将启动 InstallSnapshot。
                    connector.NextIndex = FirstIndex;
                }
                else if (r.Result.NextIndex >= LastIndex)
                {
                    logger.Fatal("Impossible r.Result.NextIndex >= LastIndex there must be a bug.");
                    Raft.FatalKill();
                }
                else
                {
                    // fast locate
                    connector.NextIndex = r.Result.NextIndex;
                }
                await TrySendAppendEntries(connector, r);  //resend. use new NextIndex。
                return ResultCode.Success;
            }
        }

        internal async Task TrySendAppendEntries(Server.ConnectorEx connector, AppendEntries pending)
        {
            // Pending 处理必须完成。
            connector.AppendLogActiveTime = Util.Time.NowUnixMillis;
            if (connector.Pending != pending)
                return;
            // 先清除，下面中断(return)不用每次自己清除。
            connector.Pending = null;

            if (false == Raft.IsLeader)
                return; // skip if is not a leader

            // 【注意】
            // 正在安装Snapshot，此时不复制日志，肯定失败。
            // 不做这个判断也是可以工作的，算是优化。
            if (InstallSnapshotting.ContainsKey(connector.Name))
                return;

            var socket = connector.TryGetReadySocket();
            if (null == socket)
                return;

            if (connector.NextIndex > LastIndex)
                return; // copy end.

            if (connector.NextIndex == FirstIndex)
            {
                // 已经到了日志开头，此时不会有prev-log，无法复制日志了。
                // 这一般发生在Leader进行了Snapshot，但是Follower的日志还更老。
                // 新起的Follower也一样。
                await StartInstallSnapshot(connector);
                return;
            }

            var nextLog = await ReadLog(connector.NextIndex);
            if (nextLog == null) // Logs可能已经变成null了, 小概率事件
                return;
            var prevLog = await ReadLog(nextLog.Index - 1);
            if (prevLog == null) // Logs可能已经变成null了, 小概率事件
                return;

            connector.Pending = new AppendEntries();
            connector.Pending.Argument.Term = Term;
            connector.Pending.Argument.LeaderId = Raft.Name;
            connector.Pending.Argument.LeaderCommit = CommitIndex;

            connector.Pending.Argument.PrevLogIndex = prevLog.Index;
            connector.Pending.Argument.PrevLogTerm = prevLog.Term;

            // 限制一次发送的日志数量，【注意】这个不是raft要求的。
            int maxCount = Raft.RaftConfig.MaxAppendEntriesCount;
            RaftLog lastCopyLog = nextLog;
            for (var copyLog = nextLog;
                maxCount > 0 && null != copyLog && copyLog.Index <= LastIndex;
                copyLog = await ReadLog(copyLog.Index + 1), --maxCount
                )
            {
                lastCopyLog = copyLog;
                connector.Pending.Argument.Entries.Add(new Binary(copyLog.Encode()));
            }
            connector.Pending.Argument.LastEntryIndex = lastCopyLog.Index;
            if (false == connector.Pending.Send(socket,
                async (p) => await ProcessAppendEntriesResult(connector, p),
                Raft.RaftConfig.AppendEntriesTimeout))
            {
                connector.Pending = null;
                // Heartbeat Will Retry
            }
        }

        internal async Task<RaftLog> LastRaftLogTermIndex()
        {
            return RaftLog.DecodeTermIndex(await ReadLogBytes(LastIndex));
        }

        private async Task RemoveLogAndCancelStart(long startIndex, long endIndex)
        {
            for (long index = startIndex; index <= endIndex; ++index)
            {
                if (index > LastApplied && LeaderAppendLogs.TryRemove(index, out var raftlog))
                {
                    // 还没有applied的日志被删除，
                    // 当发生在重新选举，但是旧的leader上还有一些没有提交的请求时，
                    // 需要取消。
                    // 其中判断：index > LastApplied 不是必要的。
                    // Apply的时候已经TryRemove了，仅会成功一次。
                    raftlog.LeaderFuture?.TrySetCanceled();
                }
                await RemoveLog(index);
            }
        }

        private async Task RemoveLog(long index)
        {
            var raftLog = await ReadLog(index);
            if (null != raftLog)
            {
                var key = ByteBuffer.Allocate();
                key.WriteLong(index);
                await Logs.RemoveAsync(key.Bytes, key.Size, null, WriteOptions);
                if (raftLog.Log.Unique.RequestId > 0)
                    await OpenUniqueRequests(raftLog.Log.CreateTime).Remove(raftLog);
            }
        }

        internal async Task<long> FollowerOnAppendEntries(AppendEntries r)
        {
            LeaderActiveTime = Zeze.Util.Time.NowUnixMillis;
            r.Result.Term = Term; // maybe rewrite later
            r.Result.Success = false; // set default false

            if (r.Argument.Term < Term)
            {
                // 1. Reply false if term < currentTerm (§5.1)
                r.SendResult();
                logger.Info($"this={Raft.Name} Leader={r.Argument.LeaderId} PrevLogIndex={r.Argument.PrevLogIndex} term < currentTerm");
                return ResultCode.Success;
            }

            switch (await TrySetTerm(r.Argument.Term))
            {
                case SetTermResult.Newer:
                    await Raft.ConvertStateTo(Raft.RaftState.Follower);
                    r.Result.Term = Term; // new term
                    break;

                case SetTermResult.Same:
                    switch (Raft.State)
                    {
                        case Raft.RaftState.Candidate:
                            // see raft.pdf 文档. 仅在 Candidate 才转。【找不到在文档哪里了，需要确认这点】
                            await Raft.ConvertStateTo(Raft.RaftState.Follower);
                            break;

                        case Raft.RaftState.Leader:
                            logger.Fatal($"Receive AppendEntries from another leader={r.Argument.LeaderId} with same term={Term}, there must be a bug. this={Raft.LeaderId}");
                            Raft.FatalKill();
                            return 0;
                    }
                    break;
            }

            Raft.LeaderId = r.Argument.LeaderId;

            // is Heartbeat(KeepAlive)
            if (r.Argument.Entries.Count == 0)
            {
                r.Result.Success = true;
                r.SendResult();
                return ResultCode.Success;
            }

            // check and copy log ...
            var prevLog = await ReadLog(r.Argument.PrevLogIndex);
            if (prevLog == null || prevLog.Term != r.Argument.PrevLogTerm)
            {
                // 2. Reply false if log doesn’t contain an entry
                // at prevLogIndex whose term matches prevLogTerm(§5.3)


                // fast locate when mismatch
                r.Result.NextIndex = r.Argument.PrevLogIndex > LastIndex ? LastIndex + 1 : 0;

                r.SendResult();
                logger.Debug("this={0} Leader={1} Index={2} prevLog mismatch", Raft.Name, r.Argument.LeaderId, r.Argument.PrevLogIndex);
                return ResultCode.Success;
            }

            // NodeReady 严格点，仅在正常复制时才检测。
            if (LastLeaderCommitIndex == 0)
            {
                // Term 增加时会重置为0，see TrySetTerm。严格点？
                LastLeaderCommitIndex = r.Argument.LeaderCommit;
            }
            else if (r.Argument.LeaderCommit > LastLeaderCommitIndex)
            {
                // 这里只要LeaderCommit推进就行，不需要自己的CommitIndex变更。
                // LeaderCommit推进，意味着，已经达成了多数，自己此时可能处于少数派。
                // 本结点CommitIndex是否还处于更早的时期，是没有关系的。
                await TrySetNodeReady();
            }

            int entryIndex = 0;
            var copyLogIndex = prevLog.Index + 1;
            for (; entryIndex < r.Argument.Entries.Count; ++entryIndex, ++copyLogIndex)
            {
                var copyLog = RaftLog.Decode(r.Argument.Entries[entryIndex], Raft.StateMachine.LogFactory);
                if (copyLog.Index != copyLogIndex)
                {
                    logger.Fatal($"copyLog.Index != copyLogIndex Leader={r.Argument.LeaderId} this={Raft.Name}");
                    Raft.FatalKill();
                }
                if (copyLog.Index < FirstIndex)
                    continue; // 快照以前的日志忽略。

                // 本地已经存在日志。
                if (copyLog.Index <= LastIndex)
                {
                    var conflictCheck = await ReadLog(copyLog.Index);
                    if (conflictCheck.Term == copyLog.Term)
                        continue;

                    // 3. If an existing entry conflicts
                    // with a new one (same index but different terms),
                    // delete the existing entry and all that follow it(§5.3)
                    // raft.pdf 5.3
                    if (conflictCheck.Index <= CommitIndex)
                    {
                        logger.Fatal($"{Raft.Name} truncate committed entries");
                        Raft.FatalKill();
                    }
                    await RemoveLogAndCancelStart(conflictCheck.Index, LastIndex);
                    LastIndex = conflictCheck.Index - 1;
                }
                break;
            }
            // Append this and all following entries.
            // 4. Append any new entries not already in the log
            for (; entryIndex < r.Argument.Entries.Count; ++entryIndex, ++copyLogIndex)
            {
                await SaveLogRaw(copyLogIndex, r.Argument.Entries[entryIndex].Bytes);
            }

            copyLogIndex--;
            // 必须判断，防止本次AppendEntries都是旧的。
            if (copyLogIndex > LastIndex)
                LastIndex = copyLogIndex;

            //CheckDump(prevLog.Index, copyLogIndex, r.Argument.Entries);

            // 5. If leaderCommit > commitIndex,
            // set commitIndex = min(leaderCommit, index of last new entry)
            if (r.Argument.LeaderCommit > CommitIndex)
            {
                CommitIndex = Math.Min(r.Argument.LeaderCommit, (await LastRaftLogTermIndex()).Index);
                await TryStartApplyTask(await ReadLog(CommitIndex));
            }
            r.Result.Success = true;
            logger.Debug("{0}: {1}", Raft.Name, r);
            r.SendResultCode(0);

            return ResultCode.Success;
        }

        internal void CheckDump(long prevLogIndex, long lastIndex, List<Binary> entries)
        {
            var logs = new StringBuilder();
            for (var index = prevLogIndex + 1; index <= lastIndex; ++index)
            {
                logs.Append(ReadLog(index).ToString()).Append('\n');
            }
            var copies = new StringBuilder();
            foreach (var entry in entries)
            {
                copies.Append(RaftLog.Decode(entry, Raft.StateMachine.LogFactory).ToString()).Append('\n');
            }

            if (logs.ToString().Equals(copies.ToString()))
                return;

            logger.Fatal("================= logs ======================");
            logger.Fatal(logs.ToString());
            logger.Fatal("================= copies ======================");
            logger.Fatal(copies.ToString());
            Raft.FatalKill();
        }
    }
}
