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
        public virtual int TypeId => (int)Bean.Hash32(GetType().FullName);

        /// <summary>
        /// 最主要的实现接口。
        /// </summary>
        /// <param name="stateMachine"></param>
        public abstract void Apply(StateMachine stateMachine);

        public abstract void Decode(ByteBuffer bb);
        public abstract void Encode(ByteBuffer bb);
    }

    public sealed class HeartbeatLog : Log
    {
        public const int SetLeaderReadyEvent = 1;

        public int Operate { get; private set; }

        public HeartbeatLog(int operate = 0)
        {
            Operate = operate;
        }

        public override void Apply(StateMachine stateMachine)
        {
            switch (Operate)
            {
                case SetLeaderReadyEvent:
                    if (stateMachine.Raft.IsLeader)
                    {
                        stateMachine.Raft.LeaderReadyEvent.Set();
                    }
                    break;
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            Operate = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
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
        public long Index { get; private set; }
        // 用来处理NextIndex回溯时限制搜索。snapshot需要修订这个值。
        public long FirstIndex { get; private set; }
        public long CommitIndex { get; private set; }
        public long LastApplied { get; private set; }

        // 这个不是日志需要的，因为持久化，所以就定义在这里吧。
        private string VoteFor { get; set; }

        // Leader
        public bool AppendLogActive { get; internal set; } = false;
        // Follower
        public long LeaderActiveTime { get; private set; } = Zeze.Util.Time.NowUnixMillis;

        private RocksDb Logs;
        private RocksDb Rafts;

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
                var it = Logs.NewIterator();
                it.SeekToLast();
                if (it.Valid())
                {
                    Index = RaftLog.Decode(
                        new Binary(it.Value()),
                        Raft.StateMachine.LogFactory
                        ).Index;
                }
                else
                {
                    // empty. add one for prev.
                    SaveLog(new RaftLog(Term, 0, new HeartbeatLog()));
                    Index = 0;
                }

                it.SeekToFirst();
                FirstIndex = RaftLog.Decode(
                    new Binary(it.Value()),
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
            Index = log.Index; // 记住最后一个Index，用来下一次生成。

            var key = ByteBuffer.Allocate();
            key.WriteLong8(log.Index);
            var value = log.Encode();

            // key,value offset must 0
            Logs.Put(key.Bytes, key.Size, value.Bytes, value.Size);
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
                Rafts.Put(RaftsTermKey, RaftsTermKey.Length, termValue.Bytes, termValue.Size);
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
            Rafts.Put(RaftsVoteForKey, RaftsVoteForKey.Length, voteForValue.Bytes, voteForValue.Size);
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
            for (long index = startIndex; index <= Index; ++index)
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
            for (long index = startIndex; index <= Index; /**/)
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
            }
            return lastMajorityLog;
        }

        private void TryCommit(AppendEntries rpc, Server.ConnectorEx connector)
        {
            connector.NextIndex = rpc.Argument.LastEntryIndex + 1;
            connector.MatchIndex = rpc.Argument.LastEntryIndex;

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
                    return; // end?

                index = raftLog.Index + 1;

                if (ApplySyncEvents.TryRemove(raftLog.Index, out var Event))
                {
                    Event.Set();
                }
                else
                {
                    raftLog.Log.Apply(Raft.StateMachine);
                }
                LastApplied = raftLog.Index; // 循环可能退出，在这里修改。
            }
        }

        internal ConcurrentDictionary<long, ManualResetEvent> ApplySyncEvents { get; }
            = new ConcurrentDictionary<long, ManualResetEvent>();

        public void AppendLog(Log log, bool ApplySync = true)
        {
            ManualResetEvent ApplySyncEvent = null;
            lock (Raft)
            {
                ++Index;
                var raftLog = new RaftLog(Term, Index, log);
                if (ApplySync)
                {
                    ApplySyncEvent = new ManualResetEvent(false);
                    if (ApplySyncEvents.TryAdd(raftLog.Index, ApplySyncEvent))
                        throw new Exception("Impossible");
                }
                SaveLog(raftLog);
            }

            // 广播给followers并异步等待多数确认
            Raft.Server.Config.ForEachConnector(
                (connector) => TrySendAppendEntries(connector as Server.ConnectorEx));

            if (ApplySync)
            {
                ApplySyncEvent.WaitOne();
                log.Apply(Raft.StateMachine);
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

        private void TrySendAppendEntries(Server.ConnectorEx connector)
        {
            if (false == connector.IsHandshakeDone)
            {
                Zeze.Util.Scheduler.Instance.Schedule(
                    (ThisTask) => TrySendAppendEntries(connector),
                    Raft.RaftConfig.AppendEntriesTimeout);
                return;
            }

            var rpc = new AppendEntries();
            lock (Raft)
            {
                var nextLog = ReadLogReverse(connector.NextIndex);
                if (null == nextLog)
                    return; // impossible

                if (nextLog.Index == FirstIndex)
                {
                    // 此时，没有了prev，而日志已经最老，无法安全复制日志。
                    // TODO start InstallSnapshot
                    return;
                }

                // 现在Index总是递增，但没有确认步长总是为1，这样能处理不为1的情况。
                connector.NextIndex = nextLog.Index;

                rpc.Argument.Term = Term;
                rpc.Argument.LeaderId = Raft.Name;
                rpc.Argument.LeaderCommit = CommitIndex;

                // 肯定能找到的。
                var prevLog = ReadLogReverse(nextLog.Index);
                rpc.Argument.PrevLogIndex = prevLog.Index;
                rpc.Argument.PrevLogTerm = prevLog.Term;

                // 限制一次发送的日志数量，【注意】这个不是raft要求的。
                int maxCount = Raft.RaftConfig.MaxAppendEntiresCount;
                RaftLog lastCopyLog = nextLog;
                for (var copyLog = nextLog;
                    maxCount > 0 && null != copyLog && copyLog.Index <= Index;
                    copyLog = ReadLogStart(copyLog.Index + 1), --maxCount
                    )
                {
                    lastCopyLog = copyLog;
                    rpc.Argument.Entries.Add(new Binary(copyLog.Encode()));
                }
                rpc.Argument.LastEntryIndex = lastCopyLog.Index;
            }

            var sendResultLocal = rpc.Send(connector.Socket,
                (p) =>
                {
                    var r = p as AppendEntries;
                    if (r.IsTimeout)
                    {
                        TrySendAppendEntries(connector);  //resend
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
                            return Procedure.Success;
                        }
                    }

                    if (r.Result.Success)
                    {
                        lock (Raft)
                        {
                            TryCommit(r, connector);
                        }
                        // TryCommit 推进了NextIndex，可能一次日志没有复制完。
                        // 尝试继续复制日志。see TrySendAppendEntries 内的
                        // “限制一次发送的日志数量”
                        TrySendAppendEntries(connector);
                        return Procedure.Success;
                    }

                    lock (Raft)
                    {
                        // TODO raft.pdf 提到一个优化
                        connector.NextIndex--;
                        TrySendAppendEntries(connector);  //resend
                        return Procedure.Success;
                    }
                },
                Raft.RaftConfig.AppendEntriesTimeout);

            // 按理说，多个Follower设置一次就够了，这里就不做这个处理了。
            AppendLogActive = sendResultLocal;

            if (false == sendResultLocal)
            {
                Zeze.Util.Scheduler.Instance.Schedule(
                    (ThisTask) => TrySendAppendEntries(connector),
                    Raft.RaftConfig.AppendEntriesTimeout);
                return;
            }
        }

        internal RaftLog LastRaftLog()
        {
            return ReadLog(Index);
        }

        private void RemoveLogUntilEnd(long startIndex, long prevIndex)
        {
            for (long index = startIndex; index <= Index; ++index)
            {
                var key = ByteBuffer.Allocate();
                key.WriteLong8(index);
                Logs.Remove(key.Bytes, key.Size);
            }
            Index = prevIndex;
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
                        RemoveLogUntilEnd(conflictCheck.Index, prevLog.Index);
                    }
                }
                else
                {
                    // 4. Append any new entries not already in the log
                    SaveLog(copyLog);
                }
                // 复用这个变量。当冲突需要删除时，精确指到前一个日志。
                // RemoveLogToEnd
                prevLog = copyLog;
            }
            // 5. If leaderCommit > commitIndex,
            // set commitIndex = min(leaderCommit, index of last new entry)
            CommitIndex = Math.Min(r.Argument.LeaderCommit, LastRaftLog().Index);
            TryApply(ReadLog(CommitIndex));

            r.Result.Success = true;
            r.SendResultCode(0);
            return Procedure.Success;
        }
    }
}
