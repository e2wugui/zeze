using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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

        // 仅用于Leader。
        public ConcurrentDictionary<Zeze.Net.Connector, int> SuccessFollowers { get; }
            = new ConcurrentDictionary<Net.Connector, int>();

        // 对于Leader
        // 0. 初始WaitMajorityConfirmation，
        // 1. 多数确认以后Committable，
        // 2. 提交以后设置为Commtted
        //
        // 对于Follower
        // 1. 处理AppendEntries时返回Success时即设为Committable，以后等待Leader推进CommitIndex。
        // 2. 提交以后设置为Commtted
        public enum State
        {
            WaitMajorityConfirmation,
            Committable,
            Commtted,
        }
        // 线程，under lock(LogSequence)
        public State LogState { get; set; } = State.WaitMajorityConfirmation;

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

        private List<RaftLog> Logs { get; } = new List<RaftLog>();

        /// <summary>
        /// 复制日志超时，以及发送失败重试超时。
        /// </summary>
        public const int AppendEntriesTimeout = 5000;
        // 不精确 Heartbeat Idle 算法：
        // 如果 AppendLogActive 则设为 false，然后等待下一次timer。
        // 否则发送 AppendLog。
        public const int LeaderHeartbeatTimer = 6000;
        // > LeaderHeartbeatTimer + AppendEntriesTimeout
        public const int LeaderLostTimeout = 12000; 

        public bool AppendLogActive = false;

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
                    AppendLog(new HeartbeatLog());
                },
                LeaderHeartbeatTimer, LeaderHeartbeatTimer);
        }

        /// <summary>
        /// 从 last 开始往前查找最早的未提交的日志。
        /// 如果中间发现WaitMajorityConfirmation返回-1。
        /// 由于tcp流以及日志Append模式，这里一般只需要回溯一次，然后直接返回last。
        /// 只有在异常的情况下，可能返回前一个（上一次Term中后来多数确认成功的一个，共2个）。
        /// </summary>
        /// <param name="current"></param>
        /// <returns></returns>
        private int FindFirstCommittableIndex(int current)
        {
            for (int i = current; i >= 0; --i)
            {
                switch (Logs[i].LogState)
                {
                    case RaftLog.State.Committable:
                        continue;

                    case RaftLog.State.Commtted:
                        if (Logs[i].Index != CommitIndex)
                        {
                            // 原则上不可能，加上这个检查纠错。
                            logger.Fatal("RaftLog.Committed but Index Is Not CommitIndex.");
                        }
                        return i + 1;

                    case RaftLog.State.WaitMajorityConfirmation:
                        return -1;
                }
            }
            return 0;
        }

        private int FindLastCommittableIndex(int current)
        {
            for (int i = current; i < Logs.Count; ++i)
            {
                switch (Logs[i].LogState)
                {
                    case RaftLog.State.Committable:
                        continue;

                    case RaftLog.State.Commtted:
                        // 原则上不可能。
                        // 已经记过logger，这个发生了，应该停止服务吧。
                        logger.Fatal("RaftLog.Committed Found After Committable.");
                        Environment.Exit(101010);
                        return -1;

                    case RaftLog.State.WaitMajorityConfirmation:
                        return i - 1;
                }
            }
            return Logs.Count - 1;
        }

        private void TryCommit(int index)
        {
            lock (this)
            {
                var raftLog = Logs[index];
                if (raftLog.LogState == RaftLog.State.Commtted)
                {
                    return; // 已经提交（多数确认过的）日志，不需要额外处理。
                }

                raftLog.LogState = RaftLog.State.Committable;

                if (raftLog.SuccessFollowers.Count < Raft.RaftConfig.HalfCount)
                {
                    return; // 没有达到多数确认。
                }

                if (raftLog.Term != Term)
                {
                    // 如果是上一个 Term 未提交的日志，不自动提交。
                    // 总是等待当前 Term 推进时，随便提交它。
                    return;
                }

                var tryCommitIndex = raftLog.Index;
                var firstArrayIndex = FindFirstCommittableIndex(index);
                if (firstArrayIndex < 0)
                    return;
                var lastArrayIndex = FindLastCommittableIndex(index);
                if (lastArrayIndex < 0)
                    return; // 这种情况发生，会导致程序退出。

                for (int i = firstArrayIndex; i <= lastArrayIndex; ++i)
                {
                    var log = Logs[i];
                    log.Log.Apply(Raft.StateMachine);
                    log.LogState = RaftLog.State.Commtted;
                }

                CommitIndex = Logs[lastArrayIndex].Index;
                // Leader只管自己推进CommitIndex。
                // 不需要广播一次让followers推进CommitIndex。
                // 持续的日志复制会导致followers推进CommitIndex。
                // 日志复制停止以后，心跳会导致followers推进最后一条的CommitIndex.
            }
        }

        private (RaftLog, AppendEntriesArgument, int) AppendLogAtomic(Log log)
        {
            var arg = new AppendEntriesArgument();
            lock (this)
            {
                arg.Term = Term;
                arg.LeaderId = Raft.Name;

                arg.PrevLogIndex = Index;
                // TODO 第一个Log的前一个Term写什么？
                arg.PrevLogTerm = Logs.Count == 0 ? Term : Logs[Logs.Count - 1].Term;

                ++Index;
                var raftLog = new RaftLog(Term, Index, log);
                Logs.Add(raftLog);

                arg.Entries.Add(raftLog.Encode());
                arg.LeaderCommit = CommitIndex;

                return (raftLog, arg, Logs.Count - 1);
            }
        }

        public void AppendLog(Log log)
        {
            var (raftLog, arg, index) = AppendLogAtomic(log);
            // 广播给followers并异步等待多数确认
            Raft.Server.Config.ForEachConnector(
                (c) =>
                {
                    if (raftLog.SuccessFollowers.TryGetValue(c, out var _))
                    {
                        // 已经复制成功过。忽略。
                        // 由于RaftNodes.Count不会很多，现在保存复制成功过的。
                        // 使用循环检测方式，没有成功的持续广播等待确认。
                        // 这种方式，当 RaftNodes.Count 发生变更时，可以继续通知新的node。
                        // TODO，Count变更时，需要确认这样做是否正确。
                        return;
                    }
                    SendAppendEntries(raftLog, index, arg, c);
                });
        }

        private void SendAppendEntries(RaftLog raftLog, int index,
            AppendEntriesArgument arg, Zeze.Net.Connector connector)
        {
            // 按理说，多个Follower设置一次就够了，这里就不做这个处理了。
            AppendLogActive = true;

            if (false == connector.IsHandshakeDone)
            {
                Zeze.Util.Scheduler.Instance.Schedule(
                    (ThisTask) => SendAppendEntries(raftLog, index, arg, connector),
                    AppendEntriesTimeout);
            }

            var sendResultLocal = new AppendEntries() { Argument = arg }.Send(connector.Socket,
                (p) =>
                {
                    var r = p as AppendEntries;
                    if (r.IsTimeout)
                    {
                        SendAppendEntries(raftLog, index, arg, connector);  //resend
                    }
                    else if (r.Result.Success)
                    {
                        if (raftLog.SuccessFollowers.TryAdd(connector, 1))
                        {
                            TryCommit(index);
                        }
                        else
                        {
                            logger.Fatal("RaftLog.SuccessFollowers.TryAdd false. Imposible!");
                        }
                        // TODO
                        // r.Result.Term 这个拿来干嘛用
                    }
                    else
                    {
                        SendAppendEntries(raftLog, index, arg, connector);  //resend
                    }
                    return Procedure.Success;
                },
                AppendEntriesTimeout);

            if (false == sendResultLocal)
            {
                Zeze.Util.Scheduler.Instance.Schedule(
                    (ThisTask) => SendAppendEntries(raftLog, index, arg, connector),
                    AppendEntriesTimeout);
                return;
            }
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
            if (r.Argument.Term < Term)
            {
                // 1. Reply false if term < currentTerm (§5.1)
                r.Result.Success = false;
                r.SendResult();
                return Procedure.LogicError;
            }

            lock (this)
            {
                var (prevLog, prevIndex) = FindPrevLog(r.Argument.PrevLogTerm, r.Argument.PrevLogIndex);
                if (prevLog == null)
                {
                    // 2. Reply false if log doesn’t contain an entry
                    // at prevLogIndex whose term matches prevLogTerm(§5.3)
                    // TODO 初始化系统或者snapshot以后，此时prev肯定找不到，怎么允许。
                    r.Result.Success = false;
                    r.SendResult();
                    return Procedure.LogicError;
                }

                // 【确认】Raft 协议描述允许一次发送多个日志，
                // 协议中的关于3、4的说明应该是针对每一个日志的。
                foreach (var raftLogData in r.Argument.Entries)
                {
                    var raftLog = RaftLog.Decode(raftLogData, Raft.StateMachine.LogFactory);
                    raftLog.LogState = RaftLog.State.Committable;

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
                CommitIndex = Math.Min(
                    r.Argument.LeaderCommit,
                    Logs[Logs.Count - 1].Index);
                var currentArrayIndex = FindCurrentCommitIndex(CommitIndex);
                if (currentArrayIndex >= 0)
                {
                    var firstIndex = FindFirstCommittableIndex(currentArrayIndex);
                    for (int i = firstIndex; i < currentArrayIndex; ++i)
                    {
                        var log = Logs[i];
                        log.Log.Apply(Raft.StateMachine);
                        log.LogState = RaftLog.State.Commtted;
                    }
                }
            }
            r.SendResultCode(0);
            return Procedure.Success;
        }

        private int FindCurrentCommitIndex(long commitIndex)
        {
            for (int i = Logs.Count - 1; i >= 0; --i)
            {
                if (Logs[i].Index == commitIndex)
                    return i;
            }
            return -1;
        }
    }
}
