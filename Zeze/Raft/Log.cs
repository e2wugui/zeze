﻿using System;
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
        private int _TypeId;
        public virtual int TypeId => _TypeId;

        // 当前这个Log是哪个应用的Rpc请求引起的。
        // 【Raft用来检测重复的请求】。
        // RaftConfig里面配置AutoKeyLocalStep开启这个功能。
        // 启用这个功能要求应用的RpcSessionId持久化，并且全局唯一，对每个AutoKeyLocalStep递增。
        // 【注意】应用生成的Id必须大于0；0保留给内部；小于0未使用。
        public long UniqueRequestId { get; set; }

        public Log(long requestId)
        {
            UniqueRequestId = requestId;
            _TypeId = (int)Bean.Hash32(GetType().FullName);
        }

        /// <summary>
        /// 最主要的实现接口。
        /// </summary>
        /// <param name="stateMachine"></param>
        public abstract void Apply(StateMachine stateMachine);

        public virtual void Decode(ByteBuffer bb)
        {
            UniqueRequestId = bb.ReadLong();
        }

        public virtual void Encode(ByteBuffer bb)
        {
            bb.WriteLong(UniqueRequestId);
        }
    }

    public sealed class HeartbeatLog : Log
    {
        public const int SetLeaderReadyEvent = 1;

        public int Operate { get; private set; }

        public HeartbeatLog(int operate = 0) : base(0)
        {
            Operate = operate;
        }

        public override void Apply(StateMachine stateMachine)
        {
            switch (Operate)
            {
                case SetLeaderReadyEvent:
                    stateMachine.Raft.SetLeaderReady();
                    break;
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
            Operate = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
            bb.WriteInt(Operate);
        }
    }

    public sealed class RaftLog : Serializable
    {
        public long Term { get; private set; }
        public long Index { get; private set; }
        public Log Log { get; private set; }

        // 不会被系列化。Local Only.
        public Func<int, Log> LogFactory { get; }

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
                var key = ByteBuffer.Allocate();
                key.WriteLong8(index);
                Logs.Remove(key.Bytes, key.Size);
            }
        }

        // Leader
        public long AppendLogActiveTime { get; internal set; } = Zeze.Util.Time.NowUnixMillis;
        // Follower
        public long LeaderActiveTime { get; private set; } = Zeze.Util.Time.NowUnixMillis;

        private RocksDb Logs { get; set; }
        private RocksDb Rafts { get; set; }

        internal void Close()
        {
            lock (Raft)
            {
                Logs?.Dispose();
                Logs = null;
                Rafts?.Dispose();
                Rafts = null;
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

        private byte[] RaftsTermKey;
        private byte[] RaftsVoteForKey;

        private void SaveLog(RaftLog log)
        {
            LastIndex = log.Index; // 记住最后一个Index，用来下一次生成。

            var key = ByteBuffer.Allocate();
            key.WriteLong8(log.Index);
            var value = log.Encode();

            // key,value offset must 0
            Logs.Put(
                key.Bytes, key.Size,
                value.Bytes, value.Size,
                null, new WriteOptions().SetSync(true)
                );
        }

        private RaftLog ReadLog(long index)
        {
            var key = ByteBuffer.Allocate();
            key.WriteLong8(index);
            var value = Logs.Get(key.Bytes, key.Size);
            if (null == value)
                return null;
            return RaftLog.Decode(new Binary(value), Raft.StateMachine.LogFactory);
        }

        internal bool TrySetTerm(long term)
        {
            if (term > Term)
            {
                Term = term;
                var termValue = ByteBuffer.Allocate();
                termValue.WriteLong(term);
                Rafts.Put(
                    RaftsTermKey, RaftsTermKey.Length,
                    termValue.Bytes, termValue.Size,
                    null, new WriteOptions().SetSync(true)
                    );
                return true;
            }
            return false;
        }

        internal bool CanVoteFor(string voteFor)
        {
            return string.IsNullOrEmpty(VoteFor) || VoteFor.Equals(voteFor);
        }

        internal void SetVoteFor(string voteFor)
        {
            VoteFor = voteFor;
            var voteForValue = ByteBuffer.Allocate();
            voteForValue.WriteString(voteFor);
            Rafts.Put(
                RaftsVoteForKey, RaftsVoteForKey.Length,
                voteForValue.Bytes, voteForValue.Size,
                null, new WriteOptions().SetSync(true)
                );
        }

        /// <summary>
        /// 从startIndex开始，直到找到一个存在的日志。
        /// 最多找到结束Index。这是为了能处理Index不连续。
        /// 虽然算法上不可能，但花几行代码这样处理一下吧。
        /// 这个方法看起来也有可能返回null，实际上应该不会发生。
        /// </summary>
        /// <param name="startIndex"></param>
        /// <returns></returns>
        private RaftLog ReadLogStart(long startIndex)
        {
            for (long index = startIndex; index <= LastIndex; ++index)
            {
                var raftLog = ReadLog(index);
                if (null != raftLog)
                    return raftLog;
            }
            return null;
        }

        private RaftLog FindMaxMajorityLog(long startIndex)
        {
            RaftLog lastMajorityLog = null;
            for (long index = startIndex; index <= LastIndex; /**/)
            {
                var raftLog = ReadLogStart(index);
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

            // 已经提交的，旧的 AppendEntries 的结果，不用继续处理了。
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
                var raftLog = ReadLogStart(index);
                if (null == raftLog)
                {
                    logger.Warn("What Happened! index={0} lastApplyable={1} LastApplied={2}",
                        index, lastApplyableLog.Index, LastApplied);
                    return; // end?
                }

                index = raftLog.Index + 1;

                if (raftLog.Log.UniqueRequestId > 0)
                {
                    // 这是防止请求重复执行用的。
                    // 需要对每个Raft.Agent的请求排队处理。
                    // see Net.cs Server.DispatchProtocol

                    long appInstance = 0;
                    if (Raft.RaftConfig.AutoKeyLocalStep > 0)
                        appInstance = raftLog.Log.UniqueRequestId % Raft.RaftConfig.AutoKeyLocalStep;
                    // 这里不需要递增判断：由于请求是按网络传过来的顺序处理的，到达这里肯定是递增的。
                    // 如果来自客户端的请求Id不是递增的，在 Net.cs::Server 处理时会拒绝掉。
                    LastAppliedAppRpcUniqueRequestId[appInstance] = raftLog.Log.UniqueRequestId;
                }
                raftLog.Log.Apply(Raft.StateMachine);
                LastApplied = raftLog.Index; // 循环可能退出，在这里修改。

                if (WaitApplyFutures.TryRemove(raftLog.Index, out var future))
                    future.SetResult(0);
            }
        }

        public ConcurrentDictionary<long, long> LastAppliedAppRpcUniqueRequestId { get; }
            = new ConcurrentDictionary<long, long>();

        internal ConcurrentDictionary<long, TaskCompletionSource<int>> WaitApplyFutures { get; }
            = new ConcurrentDictionary<long, TaskCompletionSource<int>>();

        public void AppendLog(Log log, bool WaitApply = true)
        {
            if (false == Raft.IsLeader)
                throw new TaskCanceledException(); // 快速失败

            TaskCompletionSource<int> future = null;
            lock (Raft)
            {
                ++LastIndex;
                var raftLog = new RaftLog(Term, LastIndex, log);
                if (WaitApply)
                {
                    future = new TaskCompletionSource<int>();
                    if (false == WaitApplyFutures.TryAdd(raftLog.Index, future))
                        throw new Exception("Impossible");
                }
                SaveLog(raftLog);
            }

            // 广播给followers并异步等待多数确认
            Raft.Server.Config.ForEachConnector(
                (connector) => TrySendAppendEntries(connector as Server.ConnectorEx, null));

            if (WaitApply)
            {
                future.Task.Wait();
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
                        (ThisTask) => StartSnapshot(false), delay, 20 * 3600 * 1000);
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
                        connector.HandshakeDoneEvent.WaitOne();
                        var future = new TaskCompletionSource<int>();
                        var r = new InstallSnapshot() { Argument = trunkArg };
                        if (!r.Send(connector.Socket,
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
                            if (this.TrySetTerm(r.Result.Term))
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
                        // = FirstIndex + 1，防止Index跳着分配，使用ReadLogStart。
                        var next = ReadLogStart(FirstIndex + 1);
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

        private int ProcessAppendEntriesResult(Server.ConnectorEx connector, Protocol p)
        {
            // 这个rpc处理流程总是返回 Success，需要统计观察不同的分支的发生情况，再来定义不同的返回值。

            if (false == Raft.IsLeader)
                return Procedure.Success; // maybe close.

            var r = p as AppendEntries;
            if (r.IsTimeout)
            {
                TrySendAppendEntries(connector, r);  //resend
                return Procedure.Success;
            }

            lock (Raft)
            {
                if (Raft.LogSequence.TrySetTerm(r.Result.Term))
                {
                    Raft.LeaderId = string.Empty; // 此时不知道谁是Leader。
                                                  // new term found.
                    Raft.ConvertStateTo(Raft.RaftState.Follower);
                    // 发现新的 Term，已经不是Leader，不能继续处理了。
                    // 直接返回。
                    connector.Pending = null;
                    return Procedure.Success;
                }

                if (Raft.State != Raft.RaftState.Leader)
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

        private void TrySendAppendEntries(Server.ConnectorEx connector, AppendEntries pending)
        {
            lock (Raft)
            {
                // 按理说，多个Follower设置一次就够了，这里就不做这个处理了。
                AppendLogActiveTime = Util.Time.NowUnixMillis;

                if (connector.Pending != pending)
                    return;

                // 先清除，下面中断(return)不用每次自己清除。
                connector.Pending = null;
                if (false == connector.IsHandshakeDone)
                {
                    // Hearbeat Will Retry
                    return;
                }

                // 【注意】
                // 正在安装Snapshot，此时不复制日志，肯定失败。
                // 不做这个判断也是可以工作的，算是优化。
                if (connector.InstallSnapshotting)
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
                    copyLog = ReadLogStart(copyLog.Index + 1), --maxCount
                    )
                {
                    lastCopyLog = copyLog;
                    connector.Pending.Argument.Entries.Add(new Binary(copyLog.Encode()));
                }
                connector.Pending.Argument.LastEntryIndex = lastCopyLog.Index;
                if (false == connector.Pending.Send(
                    connector.Socket,
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
                    future.SetCanceled();
                }

                var key = ByteBuffer.Allocate();
                key.WriteLong8(index);
                Logs.Remove(key.Bytes, key.Size);
            }
        }

        internal int FollowerOnAppendEntries(AppendEntries r)
        {
            LeaderActiveTime = Zeze.Util.Time.NowUnixMillis;
            r.Result.Term = Term;
            r.Result.Success = false; // set default false

            if (r.Argument.Term < Term)
            {
                // 1. Reply false if term < currentTerm (§5.1)
                r.SendResult();
                return Procedure.LogicError;
            }

            var prevLog = ReadLog(r.Argument.PrevLogIndex);
            if (prevLog == null || prevLog.Term != r.Argument.PrevLogTerm)
            {
                // 2. Reply false if log doesn’t contain an entry
                // at prevLogIndex whose term matches prevLogTerm(§5.3)
                r.SendResult();
                return Procedure.LogicError;
            }

            foreach (var raftLogData in r.Argument.Entries)
            {
                var copyLog = RaftLog.Decode(raftLogData, Raft.StateMachine.LogFactory);
                var conflictCheck = ReadLog(copyLog.Index);
                if (null != conflictCheck)
                {
                    if (conflictCheck.Term != copyLog.Term)
                    {
                        // 3. If an existing entry conflicts
                        // with a new one (same index but different terms),
                        // delete the existing entry and all that follow it(§5.3)
                        // raft.pdf 5.3
                        RemoveLogAndCancelStart(conflictCheck.Index, LastIndex);
                        LastIndex = prevLog.Index;
                    }
                }
                else
                {
                    // 4. Append any new entries not already in the log
                    SaveLog(copyLog);
                }
                // 复用这个变量。当冲突需要删除时，精确指到前一个日志。
                prevLog = copyLog;
            }
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
    }
}
