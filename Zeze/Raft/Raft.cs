using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Raft
{
    /// <summary>
    /// Raft Core
    /// </summary>
    public sealed class Raft
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public string Name => RaftConfig.Name;
        public string LeaderId { get; internal set; }
        public RaftConfig RaftConfig { get; }
        public LogSequence LogSequence { get; }
        public bool IsLeader => LeaderId.Equals(Name);
        public bool HasLeader => false == string.IsNullOrEmpty(LeaderId);
        public Server Server { get; }

        public StateMachine StateMachine { get; }

        public void AppendLog(Log log, bool ApplySync = true)
        {
            LogSequence.AppendLog(log, ApplySync);
        }

        public Raft(StateMachine sm,
            RaftConfig raftconf = null,
            Zeze.Config config = null,
            string name = "Zeze.Raft.Server")
        {
            if (null == raftconf)
                raftconf = RaftConfig.Load();

            if (null == config)
                config = Zeze.Config.Load();

            Server = new Server(this, name, config);
            LogSequence = new LogSequence(this);

            if (Server.Config.AcceptorCount() != 0)
                throw new Exception("Acceptor Found!");

            if (Server.Config.ConnectorCount() != 0)
                throw new Exception("Connector Found!");

            RaftConfig = raftconf;
            if (RaftConfig.Nodes.Count < 3)
                throw new Exception("Startup Nodes.Count Must >= 3.");

            Server.CreateAcceptor(Server, raftconf);
            Server.CreateConnector(Server, raftconf);

            sm.Raft = this;
            StateMachine = sm;
            RegisterInternalRpc();

            // 刚启动时，马上开始一次选举，里面有随机延迟。
            // 这里不等待LeaderLostTimeout，如果已经存在Leader，
            ConvertStateTo(RaftState.Candidate);
        }

        private int ProcessAppendEntries(Protocol p)
        {
            var r = p as AppendEntries;
            lock (this)
            {
                LogSequence.TrySetTerm(r.Argument.Term);
                // 只有Leader会发送AppendEntries，总是转到Follower，不管当前状态。
                // raft.pdf 文档描述仅在 Candidate 才转。
                ConvertStateTo(RaftState.Follower);
                LeaderId = r.Argument.LeaderId; // always replace
                return LogSequence.FollowerOnAppendEntries(r);
            }
        }

        private int ProcessInstallSnapshot(Protocol p)
        {
            var r = p as InstallSnapshot;
            lock (this)
            {
                if (LogSequence.TrySetTerm(r.Argument.Term))
                {
                    LeaderId = r.Argument.LeaderId;
                    // new term found.
                    ConvertStateTo(RaftState.Follower);
                }
            }
            r.SendResultCode(0);
            return Procedure.Success;
        }

        public enum RaftState
        { 
            Follower,
            Candidate,
            Leader,
        }

        public RaftState State { get; private set; } = RaftState.Follower;

        // Candidate
        private SchedulerTask StartRequestVoteDelayTask;
        private SchedulerTask WaitMajorityVoteTimoutTask;
        private ConcurrentDictionary<string, Connector> VoteSuccess
            = new ConcurrentDictionary<string, Connector>();
        // Leader
        private SchedulerTask HearbeatTimerTask;
        internal ManualResetEvent LeaderReadyEvent = new ManualResetEvent(false);
        // Follower
        private SchedulerTask LeaderLostTimerTask;

        // Must IsLeader
        internal void RunWhenLeaderReady(Action action)
        {
            if (LeaderReadyEvent.WaitOne(0))
            {
                action();
                return;
            }
            Util.Task.Run(
                () =>
                {
                    LeaderReadyEvent.WaitOne();
                    action();
                }, "Zeze.Raft.DispatchUserProtocol");
        }

        private bool IsLogUpToDate(long term, long index)
        {
            var last = LogSequence.LastRaftLog();
            if (term > last.Term)
                return true;
            if (term < last.Term)
                return false;
            return index >= last.Index;
        }

        private int ProcessRequestVote(Protocol p)
        {
            lock (this)
            {
                var r = p as RequestVote;
                if (LogSequence.TrySetTerm(r.Argument.Term))
                {
                    // new term found.
                    ConvertStateTo(RaftState.Follower);
                }
                // else continue process

                r.Result.Term = LogSequence.Term;
                // RequestVote RPC
                // Receiver implementation:
                // 1.Reply false if term < currentTerm(§5.1)
                // 2.If votedFor is null or candidateId, and candidate’s log is at
                // least as up - to - date as receiver’s log, grant vote(§5.2, §5.4)
                r.Result.VoteGranted = (false == IsLeader) // 这个检查必须的。
                    && (r.Argument.Term >= LogSequence.Term)
                    && LogSequence.CanVoteFor(r.Argument.CandidateId)
                    && IsLogUpToDate(r.Argument.LastLogTerm, r.Argument.LastLogIndex);
                if (r.Result.VoteGranted)
                    LogSequence.SetVoteFor(r.Argument.CandidateId);
                r.SendResultCode(0);

                if (IsLeader)
                {
                    // 如果Leader还活着，马上通告一次。这个比Hearbeat快。
                    var leaderIs = new LeaderIs();
                    leaderIs.Argument.LeaderId = LeaderId;
                    leaderIs.Send(r.Sender); // skip response.
                }
                return Procedure.Success;
            }
        }

        private int ProcessLeaderIs(Protocol p)
        {
            var r = p as LeaderIs;

            lock (this)
            {
                LeaderId = r.Argument.LeaderId;
                ConvertStateTo(RaftState.Follower);
            }

            return Procedure.Success;
        }

        private int ProcessRequestVoteResult(RequestVote rpc, Connector c)
        {
            lock (this)
            {
                if (LogSequence.TrySetTerm(rpc.Result.Term))
                {
                    // new term found
                    ConvertStateTo(RaftState.Follower);
                    return Procedure.Success;
                }
            }

            if (rpc.Result.VoteGranted && VoteSuccess.TryAdd(c.Name, c))
            {
                lock (this)
                {
                    if (VoteSuccess.Count >= RaftConfig.HalfCount)
                    {
                        // 加上自己就是多数派了。
                        ConvertStateTo(RaftState.Leader);
                    }
                }
            }
            return Procedure.Success;
        }

        private void SendRequestVote(SchedulerTask ThisTask)
        {
            lock (this)
            {
                var arg = new RequestVoteArgument();

                arg.Term = LogSequence.Term;
                arg.CandidateId = Name;
                LogSequence.SetVoteFor(Name);
                var log = LogSequence.LastRaftLog();
                arg.LastLogIndex = log.Index;
                arg.LastLogTerm = log.Term;

                Server.Config.ForEachConnector(
                    (c) =>
                    {
                        if (false == c.IsHandshakeDone)
                            return;

                        var rpc = new RequestVote() { Argument = arg };
                        rpc.Send(c.Socket, (p) => ProcessRequestVoteResult(rpc, c));
                    });

                // 定时，如果超时选举还未完成，再次发起选举。
                WaitMajorityVoteTimoutTask?.Cancel();
                WaitMajorityVoteTimoutTask = Scheduler.Instance.Schedule(
                    (ThisTask) =>
                    {
                        lock (this)
                        {
                            StartRequestVoteDelayTask = null;
                            ConvertStateTo(RaftState.Candidate);
                        }
                    },
                    RaftConfig.AppendEntriesTimeout + 1000);
            }
        }

        private void ConvertStateFromFollwerTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info("RaftState: Follower->Follower");
                    return;

                case RaftState.Candidate:
                    logger.Info("RaftState: Follower->Candidate");
                    State = RaftState.Candidate;
                    LeaderLostTimerTask?.Cancel();
                    LeaderLostTimerTask = null;
                    StartRequestVote();
                    return;

                case RaftState.Leader:
                    logger.Error("RaftState Impossible! Follower->Leader");
                    return;
            }
        }

        private void ConvertStateFromCandidateTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info("RaftState: Candidate->Follower");
                    State = RaftState.Follower;
                    VoteSuccess.Clear(); // 选举结束清除。

                    LogSequence.SetVoteFor(string.Empty);
                    StartRequestVoteDelayTask?.Cancel();
                    StartRequestVoteDelayTask = null;
                    WaitMajorityVoteTimoutTask?.Cancel();
                    WaitMajorityVoteTimoutTask = null;
                    return;

                case RaftState.Candidate:
                    // Normal: Vote Timeout. Restart.
                    // 如果确实需要重启，需要在调用前：
                    // StartRequestVoteDelayTask = null;
                    // 否则不会真正开始新的选举。
                    logger.Info("RaftState: Candidate->Candidate");
                    StartRequestVote();
                    return;

                case RaftState.Leader:
                    StartRequestVoteDelayTask?.Cancel();
                    StartRequestVoteDelayTask = null;
                    WaitMajorityVoteTimoutTask?.Cancel();
                    WaitMajorityVoteTimoutTask = null;
                    VoteSuccess.Clear(); // 选举结束清除。

                    logger.Info("RaftState: Candidate->Leader");
                    State = RaftState.Leader;
                    LogSequence.SetVoteFor(string.Empty);
                    LeaderId = Name; // set to self

                    // (Reinitialized after election)
                    var nextIndex = LogSequence.LastRaftLog().Index + 1;
                    Server.Config.ForEachConnector(
                        (c) =>
                        {
                            var cex = c as Server.ConnectorEx;
                            cex.NextIndex = nextIndex;
                            cex.MatchIndex = 0;
                        });

                    // Upon election:
                    // send initial empty AppendEntries RPCs
                    // (heartbeat)to each server; repeat during
                    // idle periods to prevent election timeouts(§5.2)
                    LogSequence.AppendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent), false);
                    HearbeatTimerTask = Scheduler.Instance.Schedule(
                        (ThisTask) =>
                        {
                            if (LogSequence.AppendLogActive)
                            {
                                LogSequence.AppendLogActive = false;
                                return;
                            }
                            LogSequence.AppendLog(new HeartbeatLog(), false);
                        },
                        RaftConfig.LeaderHeartbeatTimer,
                        RaftConfig.LeaderHeartbeatTimer);
                    return;
            }
        }

        private void ConvertStateFromLeaderTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info("RaftState: Leader->Follower");
                    State = RaftState.Follower;
                    LeaderReadyEvent.Reset();

                    HearbeatTimerTask?.Cancel();
                    HearbeatTimerTask = null;

                    // 每次LeaderActive启动一个Timer会很精确，但需要创建很多Task。
                    // 下面这种定时检测的方法在精度方面也可以。只是需要定时check。
                    LeaderLostTimerTask = Scheduler.Instance.Schedule(
                        (ThisTask) =>
                        {
                            var elapse = Time.NowUnixMillis - LogSequence.LeaderActiveTime;
                            if (elapse > RaftConfig.LeaderLostTimeout)
                            {
                                ConvertStateTo(RaftState.Candidate);
                            }
                        },
                        2000,
                        2000);
                    return;

                case RaftState.Candidate:
                    logger.Error("RaftState Impossible! Leader->Candidate");
                    return;

                case RaftState.Leader:
                    logger.Error("RaftState Impossible! Leader->Leader");
                    return;
            }
        }

        internal void ConvertStateTo(RaftState newState)
        {
            // 按真值表处理所有情况。
            switch (State)
            {
                case RaftState.Follower:
                    ConvertStateFromFollwerTo(newState);
                    return;

                case RaftState.Candidate:
                    ConvertStateFromCandidateTo(newState);
                    return;

                case RaftState.Leader:
                    ConvertStateFromLeaderTo(newState);
                    return;
            }
        }

        private void StartRequestVote()
        {
            if (null != StartRequestVoteDelayTask)
                return;

            VoteSuccess.Clear(); // 每次选举开始清除。

            LeaderId = string.Empty;
            LogSequence.TrySetTerm(LogSequence.Term + 1);
            WaitMajorityVoteTimoutTask?.Cancel();
            WaitMajorityVoteTimoutTask = null;
            StartRequestVoteDelayTask = Scheduler.Instance.Schedule(
                SendRequestVote, Util.Random.Instance.Next(2000));
        }

        private void RegisterInternalRpc()
        {
            Server.AddFactoryHandle(
                new RequestVote().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new RequestVote(),
                    Handle = ProcessRequestVote,
                });

            Server.AddFactoryHandle(
                new AppendEntries().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new AppendEntries(),
                    Handle = ProcessAppendEntries,
                });

            Server.AddFactoryHandle(
                new InstallSnapshot().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new InstallSnapshot(),
                    Handle = ProcessInstallSnapshot,
                });

            Server.AddFactoryHandle(
                new LeaderIs().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new LeaderIs(),
                    Handle = ProcessLeaderIs,
                });
        }

    }
}
