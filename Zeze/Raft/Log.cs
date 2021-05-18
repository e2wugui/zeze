using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;

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
        internal ManualResetEvent ApplyEvent { get; set; }

        public virtual bool WaitAndApply(Raft raft, int millisecondsTimeout = -1)
        {
            if (ApplyEvent.WaitOne(millisecondsTimeout))
            {
                Apply(raft.StateMachine);
                ApplyEvent = null; // release trigger once.
                return true;
            }
            return false;
        }

        public abstract void Decode(ByteBuffer bb);
        public abstract void Encode(ByteBuffer bb);
    }

    public sealed class HeartbeatLog : Log
    {
        public override void Apply(StateMachine stateMachine)
        {
        }

        public override void Decode(ByteBuffer bb)
        {
        }

        public override void Encode(ByteBuffer bb)
        {
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

        public Zeze.Net.Binary Encode()
        {
            var bb = ByteBuffer.Allocate();
            Encode(bb);
            return new Zeze.Net.Binary(bb);
        }

        public static RaftLog Decode(Zeze.Net.Binary data, Func<int, Log> logFactory)
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

        public long Term => Raft.RaftConfig.Term;
        public long Index { get; private set; }

        public long CommitIndex { get; private set; }
        public long LastApplied { get; private set; }

        private List<RaftLog> Logs { get; } = new List<RaftLog>();

        private bool AppendLogActive = false;

        public LogSequence(Raft raft)
        {
            Raft = raft;
            Zeze.Util.Scheduler.Instance.Schedule(
                (ThisTask) =>
                {
                    if (AppendLogActive)
                    {
                        AppendLogActive = false;
                        return;
                    }
                    AppendLog(new HeartbeatLog(), false);
                },
                Raft.RaftConfig.LeaderHeartbeatTimer,
                Raft.RaftConfig.LeaderHeartbeatTimer);
        }

        private int FindMaxMajority(int startArrayIndex)
        {
            int lastMajorityArrayIndex = -1;
            for (int i = startArrayIndex; i < Logs.Count; ++i)
            {
                var log = Logs[i];
                int MajorityCount = 0;
                Raft.Server.Config.ForEachConnector(
                    (c) =>
                    {
                        var cex = c as Server.ConnectorEx;
                        if (cex.MatchIndex >= log.Index)
                        {
                            ++MajorityCount;
                        }
                    });

                if (MajorityCount <= Raft.RaftConfig.HalfCount)
                    break; // 没有达成多数派，中断搜索。

                lastMajorityArrayIndex = i;
            }
            return lastMajorityArrayIndex;
        }

        private void TryCommit(AppendEntries rpc, Server.ConnectorEx connector)
        {
            lock (this)
            {
                connector.NextIndex = rpc.Argument.LastEntryIndex + 1;
                connector.MatchIndex = rpc.Argument.LastEntryIndex;

                // Rules for Servers
                // If there exists an N such that N > commitIndex, a majority
                // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
                // set commitIndex = N(§5.3, §5.4).

                long majorityIndex = CommitIndex + 1;
                var majorityArrayIndex = ReverseFind(majorityIndex);
                if (majorityArrayIndex < 0 || majorityArrayIndex >= Logs.Count)
                    return;

                var maxMajorityArrayIndex = FindMaxMajority(majorityArrayIndex);
                if (maxMajorityArrayIndex < 0)
                    return; // 一个多数派都没有找到。

                var raftLog = Logs[maxMajorityArrayIndex];
                if (raftLog.Term != Term)
                {
                    // 如果是上一个 Term 未提交的日志，不自动提交。
                    // 总是等待当前 Term 推进时，随便提交它。
                    return;
                }
                CommitIndex = raftLog.Index;
                TryApply(maxMajorityArrayIndex);
            }
        }

        private void TryApply(int lastApplyArrayIndex)
        {
            // 不考虑效率，实现持久化后再来调整。
            lock (this)
            {
                var applyArrayIndexStart = ReverseFind(LastApplied);
                if (applyArrayIndexStart >= Logs.Count)
                    return;

                if (applyArrayIndexStart < 0)
                    applyArrayIndexStart = 0;

                for (int i = applyArrayIndexStart; i <= lastApplyArrayIndex; ++i)
                {
                    var log = Logs[i];
                    if (null != log.Log.ApplyEvent)
                    {
                        log.Log.ApplyEvent.Set();
                    }
                    else
                    {
                        log.Log.Apply(Raft.StateMachine);
                    }
                }
                LastApplied = Logs[lastApplyArrayIndex].Index;
            }
        }

        public Log AppendLog(Log log, bool ApplyMyself = true)
        {
            if (ApplyMyself)
            {
                if (null != log.ApplyEvent)
                    throw new Exception("Not A Fresh Log.");
                log.ApplyEvent = new ManualResetEvent(false);
            }

            lock (this)
            {
                ++Index;
                var raftLog = new RaftLog(Term, Index, log);
                Logs.Add(raftLog);
            }

            // 广播给followers并异步等待多数确认
            Raft.Server.Config.ForEachConnector(
                (connector) => TrySendAppendEntries(connector as Server.ConnectorEx));

            return log;
        }

        // LogIndex To ArrayIndex。
        // 以后持久化再来调整。
        private int ReverseFind(long nextIndex)
        {
            int i = Logs.Count - 1;
            for (; i >= 0; --i)
            {
                // 3 4 5 7+
                var log = Logs[i];
                if (log.Index < nextIndex)
                    break;
            }
            var n = i + 1;
            if (n >= Logs.Count)
            {
                // 如果是最后一个日志，检查一下是否刚好是要找的。
                if (Logs[^1].Index == nextIndex)
                    return i;
                return n; // UpperOutOfIndex。外面需要检查。
            }
            var logNext = Logs[n];
            if (logNext.Index == nextIndex)
                return n;
            if (i >= 0)
                return i;
            return -1; // LowerOutOfIndex。外面需要检查。
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
            var nextIndex = connector.NextIndex;

            lock (this)
            {
                var nextArrayIndex = ReverseFind(nextIndex);
                if (nextArrayIndex >= Logs.Count)
                {
                    return; // 没有日志需要同步。
                }

                if (nextArrayIndex < 0)
                {
                    // TODO LowerOutOfIndex Start InstallSnapshot
                    return;
                }
                rpc.Argument.Term = Term;
                rpc.Argument.LeaderId = Raft.Name;
                rpc.Argument.LeaderCommit = CommitIndex;

                // TODO 第一个Log的前一个Term写什么？
                // raft.pdf好像是在系统初始化时自动添加一条Index=0的日志。
                // 以后（包括Snapshot，会保留最后一个日志）都不会找不到prev。
                var logPrev = Logs[nextArrayIndex - 1];
                rpc.Argument.PrevLogIndex = logPrev.Index;
                rpc.Argument.PrevLogTerm = logPrev.Term;

                // TODO 限制一次发送的日志数量。
                for (int i = nextArrayIndex; i < Logs.Count; ++i)
                {
                    rpc.Argument.Entries.Add(Logs[i].Encode());
                }
                rpc.Argument.LastEntryIndex = Logs[^1].Index;
            }

            var sendResultLocal = rpc.Send(connector.Socket,
                (p) =>
                {
                    var r = p as AppendEntries;
                    if (r.IsTimeout)
                    {
                        TrySendAppendEntries(connector);  //resend
                    }
                    else if (r.Result.Success)
                    {
                        TryCommit(r, connector);
                    }
                    else
                    {
                        ReduceNextIndexAndTrySendAppendEntries(r.Result.Term, connector);
                    }
                    return Procedure.Success;
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

        private void ReduceNextIndexAndTrySendAppendEntries(long resultTerm, Server.ConnectorEx connector)
        {
            lock (this)
            {
                connector.NextIndex--;
            }
            TrySendAppendEntries(connector);  //resend
        }

        private (RaftLog, int) FindPrevLog(long prevTerm, long prevIndex)
        {
            for (int i = Logs.Count - 1; i >= 0; --i)
            {
                var log = Logs[i];
                if (log.Term == prevTerm && log.Index == prevIndex)
                    return (log, i);
            }
            return (null, -1);
        }

        internal int FollowerOnAppendEntries(AppendEntries r)
        {
            r.Result.Term = r.Argument.Term;
            r.Result.Success = false; // set default false

            if (r.Argument.Term < Term)
            {
                // 1. Reply false if term < currentTerm (§5.1)
                r.SendResult();
                return Procedure.LogicError;
            }

            lock (this)
            {
                var (prevLog, prevIndex) = FindPrevLog(
                    r.Argument.PrevLogTerm, r.Argument.PrevLogIndex);

                if (prevLog == null)
                {
                    // 2. Reply false if log doesn’t contain an entry
                    // at prevLogIndex whose term matches prevLogTerm(§5.3)
                    // TODO 初始化系统或者snapshot以后，此时prev肯定找不到，怎么允许。
                    r.SendResult();
                    return Procedure.LogicError;
                }

                // 【确认】Raft 协议描述允许一次发送多个日志，
                // 协议中的关于3、4的说明应该是针对每一个日志的。
                foreach (var raftLogData in r.Argument.Entries)
                {
                    var raftLog = RaftLog.Decode(raftLogData, Raft.StateMachine.LogFactory);

                    for (int i = Logs.Count - 1; i > prevIndex; --i)
                    {
                        // 3. If an existing entry conflicts
                        // with a new one (same index but different terms),
                        // delete the existing entry and all that follow it(§5.3)
                        // raft.pdf 5.3
                        // TODO 先简单删除之后所有的。这是错误的！！！
                        Logs.RemoveAt(i);
                    }
                    // 4. Append any new entries not already in the log
                    Logs.Add(raftLog);
                }
                // 5. If leaderCommit > commitIndex,
                // set commitIndex = min(leaderCommit, index of last new entry)
                CommitIndex = Math.Min(r.Argument.LeaderCommit,Logs[^1].Index);
                TryApply(ReverseFind(CommitIndex));
            }
            r.Result.Success = true;
            r.SendResultCode(0);
            return Procedure.Success;
        }
    }
}
