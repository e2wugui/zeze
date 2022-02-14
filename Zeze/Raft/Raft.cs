using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
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
        public bool IsLeader => this.State == RaftState.Leader;
        public Server Server { get; }

        internal SimpleThreadPool ImportantThreadPool { get; }

        public StateMachine StateMachine { get; }

        public void AppendLog(Log log)
        {
            LogSequence.AppendLog(log, true);
        }

        private volatile bool IsShutdown = false;

        public void Shutdown()
        {
            lock (this)
            {
                // shutdown 只做一次。
                if (IsShutdown)
                    return;

                IsShutdown = true;
            }

            TimerTask?.Cancel();
            TimerTask = null;

            // 0 clear pending task if is leader
            if (IsLeader)
            {
                Server.TaskOneByOne.Shutdown(false, () =>
                {
                    foreach (var e in LogSequence.WaitApplyFutures)
                    {
                        e.Value.TrySetCanceled();
                        LogSequence.WaitApplyFutures.TryRemove(e.Key, out _);
                    }
                }, true);
            }
            ImportantThreadPool.Shutdown();

            // 1. close network.
            Server.Stop();

            lock (this)
            {
                // see WaitLeaderReady.
                // 可以避免状态设置不对的问题。关闭时转换成Follower也是对的。
                ConvertStateTo(RaftState.Follower);
                LeaderId = string.Empty;
            }

            // 3. close LogSequence (rocksdb)
            LogSequence.Close();
        }

        private void ProcessExit(object sender, EventArgs e)
        {
            Shutdown();
        }

        public Raft(StateMachine sm,
            string RaftName = null,
            RaftConfig raftconf = null,
            Zeze.Config config = null,
            string name = "Zeze.Raft.Server")
        {
            if (null == raftconf)
                raftconf = RaftConfig.Load();
            raftconf.Verify();

            RaftConfig = raftconf;
            sm.Raft = this;
            StateMachine = sm;

            if (false == string.IsNullOrEmpty(RaftName))
                raftconf.Name = RaftName;

            if (null == config)
                config = Zeze.Config.Load();

            Server = new Server(this, name, config);
            if (Server.Config.AcceptorCount() != 0)
                throw new Exception("Acceptor Found!");
            if (Server.Config.ConnectorCount() != 0)
                throw new Exception("Connector Found!");
            if (RaftConfig.Nodes.Count < 3)
                throw new Exception("Startup Nodes.Count Must >= 3.");

            ImportantThreadPool = new SimpleThreadPool(5, $"Raft.{Name}");
            Server.CreateAcceptor(Server, raftconf);
            Server.CreateConnector(Server, raftconf);

            LogSequence = new LogSequence(this);

            RegisterInternalRpc();
            LogSequence.StartSnapshotPerDayTimer();
            AppDomain.CurrentDomain.ProcessExit += ProcessExit;
            TimerTask = Scheduler.Instance.Schedule(OnTimer, 1000, 200);
        }

        private long ProcessAppendEntries(Protocol p)
        {
            var r = p as AppendEntries;
            lock (this)
            {
                return LogSequence.FollowerOnAppendEntries(r);
            }
        }

        private ConcurrentDictionary<long, FileStream> ReceiveSnapshotting
            = new ConcurrentDictionary<long, FileStream>();

        private long ProcessInstallSnapshot(Protocol p)
        {
            var r = p as InstallSnapshot;
            lock (this)
            {
                if (LogSequence.TrySetTerm(r.Argument.Term) == LogSequence.SetTermResult.Newer)
                {
                    LeaderId = r.Argument.LeaderId;
                    // new term found.
                    ConvertStateTo(RaftState.Follower);
                }
            }
            r.Result.Term = LogSequence.Term;
            if (r.Argument.Term < LogSequence.Term)
            {
                // 1. Reply immediately if term < currentTerm
                r.SendResultCode(InstallSnapshot.ResultCodeTermError);
                return Procedure.LogicError;
            }

            // 2. Create new snapshot file if first chunk(offset is 0)
            // 把 LastIncludedIndex 放到文件名中，
            // 新的InstallSnapshot不覆盖原来进行中或中断的。
            var path = Path.Combine(RaftConfig.DbHome,
                $"{LogSequence.SnapshotFileName}.{r.Argument.LastIncludedIndex}");

            FileStream outputFileStream = null;
            if (r.Argument.Offset == 0)
            {
                // GetOrAdd 允许重新开始。
                outputFileStream = ReceiveSnapshotting.GetOrAdd(
                    r.Argument.LastIncludedIndex,
                    (_) => new FileStream(path, FileMode.OpenOrCreate));
                outputFileStream.Seek(0, SeekOrigin.End);
            }
            else
            {
                // ignore return of TryGetValue here.
                ReceiveSnapshotting.TryGetValue(r.Argument.LastIncludedIndex, out outputFileStream);
            }

            if (null == outputFileStream)
            {
                // 肯定是旧的被丢弃的安装，Discard And Ignore。
                r.SendResultCode(InstallSnapshot.ResultCodeOldInstall);
                return Procedure.Success;
            }

            r.Result.Offset = -1; // 默认让Leader继续传输，不用重新定位。
            if (r.Argument.Offset > outputFileStream.Length)
            {
                // 数据块超出当前已经接收到的数据。
                // 填写当前长度，让Leader从该位置开始重新传输。
                r.Result.Offset = outputFileStream.Length;
                r.SendResultCode(InstallSnapshot.ResultCodeNewOffset);
                return Procedure.Success;
            }
            
            if (r.Argument.Offset == outputFileStream.Length)
            {
                // 正常的Append流程，直接写入。
                // 3. Write data into snapshot file at given offset
                outputFileStream.Write(r.Argument.Data.Bytes, r.Argument.Data.Offset, r.Argument.Data.Count);
            }
            else
            {
                // 数据块开始位置小于当前长度。
                var newEndPosition = r.Argument.Offset + r.Argument.Data.Count;
                if (newEndPosition > outputFileStream.Length)
                {
                    // 有新的数据需要写入文件。
                    outputFileStream.Seek(r.Argument.Offset, SeekOrigin.Begin);
                    outputFileStream.Write(r.Argument.Data.Bytes, r.Argument.Data.Offset, r.Argument.Data.Count);
                }
                r.Result.Offset = outputFileStream.Length;
            }

            // 4. Reply and wait for more data chunks if done is false
            if (r.Argument.Done)
            {
                // 5. Save snapshot file, discard any existing or partial snapshot with a smaller index
                outputFileStream.Close();
                foreach (var e in ReceiveSnapshotting)
                {
                    if (e.Key < r.Argument.LastIncludedIndex)
                    {
                        e.Value.Close();
                        var pathDelete = Path.Combine(RaftConfig.DbHome, $"{LogSequence.SnapshotFileName}.{e.Key}");
                        File.Delete(path);
                        ReceiveSnapshotting.TryRemove(e.Key, out var _);
                    }
                }
                // 剩下的处理流程在下面的函数里面。
                LogSequence.EndReceiveInstallSnapshot(outputFileStream, r);
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

        private volatile RaftState _State = RaftState.Follower;
        public RaftState State { get { return _State; } private set { _State = value; } }

        private SchedulerTask TimerTask;
        // Candidate
        private ConcurrentDictionary<string, Connector> VoteSuccess
            = new ConcurrentDictionary<string, Connector>();
        private long NextVoteTime;
        private long SendRequestVoteTime;
        // Leader
        private long LeaderWaitReadyTerm;
        private long LeaderWaitReadyIndex;
        internal volatile TaskCompletionSource<bool> LeaderReadyFuture = new TaskCompletionSource<bool>();
        // Follower

        // 重置 OnTimer 需要的所有时间。
        private void ResetTimerTime()
        {
            var now = Time.NowUnixMillis;
            LogSequence.LeaderActiveTime = now;
            //SendRequestVoteTime = now;
            Server.Config.ForEachConnector(
                (c) =>
                {
                    var cex = c as Server.ConnectorEx;
                    cex.AppendLogActiveTime = now;
                });
        }

        /// <summary>
        /// 每个Raft使用一个固定Timer，根据不同的状态执行相应操作。
        /// 【简化】不同状态下不管维护管理不同的Timer了。
        /// </summary>
        /// <param name="ThisTask"></param>
        private void OnTimer(SchedulerTask ThisTask)
        {
            lock (this)
            {
                switch (State)
                {
                    case RaftState.Follower:
                        var elapse = Time.NowUnixMillis - LogSequence.LeaderActiveTime;
                        if (elapse > RaftConfig.LeaderLostTimeout)
                        {
                            ConvertStateTo(RaftState.Candidate);
                        }
                        break;

                    case RaftState.Candidate:
                        if (SendRequestVoteTime > 0)
                        {
                            if (Time.NowUnixMillis > SendRequestVoteTime)
                                SendRequestVote();
                        }
                        else if (Time.NowUnixMillis > NextVoteTime)
                        {
                            // vote timeout. restart
                            ConvertStateTo(RaftState.Candidate);
                        }

                        break;

                    case RaftState.Leader:
                        var now = Time.NowUnixMillis;
                        Server.Config.ForEachConnector(
                            (c) =>
                            {
                                var cex = c as Server.ConnectorEx;
                                if (now - cex.AppendLogActiveTime > RaftConfig.LeaderHeartbeatTimer)
                                    LogSequence.SendHearbeatTo(cex);
                            });
                        break;
                }
            }
        }

       /// <summary>
        /// true，IsLeader && LeaderReady;
        /// false, !IsLeader
        /// </summary>
        /// <returns></returns>
        internal bool WaitLeaderReady()
        {
            lock (this)
            {
                var volatileTmp = LeaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
                while (IsLeader)
                {
                    if (volatileTmp.Task.Wait(0))
                        return volatileTmp.Task.Result;
                    Monitor.Wait(this);
                }
                return false;
            }
        }

        public bool IsReadyLeader
        {
            get
            {
                lock (this)
                {
                    var volatileTmp = LeaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
                    return IsLeader && volatileTmp.Task.Wait(0) && volatileTmp.Task.Result;
                }
            }
        }

        internal void ResetLeaderReadyAfterChangeState()
        {
            LeaderReadyFuture.TrySetResult(false);
            LeaderReadyFuture = new TaskCompletionSource<bool>(); // prepare for next leader
            Monitor.PulseAll(this); // has under lock(this)
        }

        internal void SetLeaderReady(RaftLog heart)
        {
            if (IsLeader)
            {
                // 是否过期First-Hearbeat。
                // 使用 LeaderReadyFuture 可以更加精确的识别。
                // 但是，由于RaftLog不是常驻内存的，保存不了进程级别的变量。
                if (heart.Term != LeaderWaitReadyTerm || heart.Index != LeaderWaitReadyIndex)
                    return;

                LeaderWaitReadyIndex = 0;
                LeaderWaitReadyTerm = 0;

                logger.Info($"{Name} {RaftConfig.DbHome} LastIndex={LogSequence.LastIndex} Count={LogSequence.GetTestStateMachineCount()}");

                LeaderReadyFuture.TrySetResult(true);
                Monitor.PulseAll(this); // has under lock(this)

                Server.Foreach(
                    (allsocket) =>
                    {
                        // 本来这个通告发给Agent(client)即可，
                        // 但是现在没有区分是来自Raft的连接还是来自Agent，
                        // 全部发送。
                        // 另外Raft之间有两个连接，会收到多次，Raft不处理这个通告。
                        // 由于Raft数量不多，不会造成大的浪费，不做处理了。
                        if (allsocket.IsHandshakeDone)
                        {
                            var r = new LeaderIs();
                            r.Argument.Term = LogSequence.Term;
                            r.Argument.LeaderId = LeaderId;
                            r.Argument.IsLeader = IsLeader;
                            r.Send(allsocket); // skip response.
                        }
                    });
            }
        }

        private bool IsCandidateLastLogUpToDate(long lastTerm, long lastIndex)
        {
            var last = LogSequence.LastRaftLog();
            logger.Info($"{Name}-{IsLeader} {RaftConfig.DbHome} CTerm={lastTerm} Term={last.Term} LastIndex={last.Index} Count={LogSequence.GetTestStateMachineCount()}");
            if (lastTerm > last.Term)
                return true;
            if (lastTerm < last.Term)
                return false;
            return lastIndex >= last.Index;
        }

        private long ProcessRequestVote(Protocol p)
        {
            lock (this)
            {
                var r = p as RequestVote;
                var newer = LogSequence.TrySetTerm(r.Argument.Term) == LogSequence.SetTermResult.Newer;
                if (newer)
                {
                    // new term found. 选举中的状态不改变。如果是Leader，马上切换到Follower。
                    if (State == RaftState.Leader)
                        ConvertStateTo(RaftState.Follower);
                }
                // else continue process

                r.Result.Term = LogSequence.Term;
                // RequestVote RPC
                // Receiver implementation:
                // 1.Reply false if term < currentTerm(§5.1)
                // 2.If votedFor is null or candidateId, and candidate’s log is at
                // least as up - to - date as receiver’s log, grant vote(§5.2, §5.4)
                r.Result.VoteGranted = newer // 1. 【change】term > currentTerm
                    && LogSequence.CanVoteFor(r.Argument.CandidateId)
                    && IsCandidateLastLogUpToDate(r.Argument.LastLogTerm, r.Argument.LastLogIndex);
                if (r.Result.VoteGranted)
                {
                    LogSequence.SetVoteFor(r.Argument.CandidateId);
                }
                logger.Info("{0}: VoteFor={1} Rpc={2}", Name, LogSequence.VoteFor, r);
                r.SendResultCode(0);

                return Procedure.Success;
            }
        }

        private long ProcessLeaderIs(Protocol p)
        {
            var r = p as LeaderIs;

            // 这个协议是发送给Agent(Client)的，
            // 为了简单，不做区分。
            // Raft也会收到，忽略。
            r.SendResultCode(0);

            return Procedure.Success;
        }

        private long ProcessRequestVoteResult(RequestVote rpc, Connector c)
        {
            if (rpc.IsTimeout || rpc.ResultCode != 0)
                return 0; // skip error. re-vote later. 

            lock (this)
            {
                if (LogSequence.TrySetTerm(rpc.Result.Term) == LogSequence.SetTermResult.Newer)
                {
                    // new term found
                    ConvertStateTo(RaftState.Follower);
                    return Procedure.Success;
                }

                if (rpc.Result.VoteGranted && VoteSuccess.TryAdd(c.Name, c))
                {
                    if (
                        // 确保当前状态是选举中。没有判断这个，
                        // 后面 ConvertStateTo 也会忽略不正确的状态转换。
                        State == RaftState.Candidate
                        // 加上自己就是多数派了。
                        && VoteSuccess.Count >= RaftConfig.HalfCount)
                    {
                        ConvertStateTo(RaftState.Leader);
                    }
                }
            }

            return Procedure.Success;
        }

        private void PrepareSendRequestVote()
        {
            SendRequestVoteTime = Time.NowUnixMillis + Util.Random.Instance.Next(RaftConfig.AppendEntriesTimeout*2);
            Server.Config.ForEachConnector((c) => c.Start());
        }

        private void SendRequestVote()
        {
            VoteSuccess.Clear(); // 每次选举开始清除。

            LeaderId = string.Empty;
            LogSequence.SetVoteFor(Name); // Vote Self First.
            LogSequence.TrySetTerm(LogSequence.Term + 1);

            var arg = new RequestVoteArgument();
            arg.Term = LogSequence.Term;
            arg.CandidateId = Name;
            var log = LogSequence.LastRaftLog();
            arg.LastLogIndex = log.Index;
            arg.LastLogTerm = log.Term;

            SendRequestVoteTime = 0;
            NextVoteTime = Time.NowUnixMillis + RaftConfig.AppendEntriesTimeout;
            Server.Config.ForEachConnector((c) =>
            {
                var rpc = new RequestVote() { Argument = arg };
                var sendresult = rpc.Send(c.TryGetReadySocket(), (p) => ProcessRequestVoteResult(rpc, c));
                logger.Info("{0}:{1}: SendRequestVote {2}", Name, sendresult, rpc);
            });
        }

        private void ConvertStateFromFollwerTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info($"RaftState {Name}: Follower->Follower");
                    return;

                case RaftState.Candidate:
                    logger.Info($"RaftState {Name}: Follower->Candidate");
                    State = RaftState.Candidate;
                    LogSequence.SetVoteFor(string.Empty); // 先清除，在真正自荐前可以给别人投票。
                    PrepareSendRequestVote();
                    return;

                case RaftState.Leader:
                    // 并发的RequestVote的结果如果没有判断当前状态，可能会到达这里。
                    // 不是什么大问题。see ProcessRequestVoteResult
                    logger.Info($"RaftState {Name} Impossible! Follower->Leader");
                    return;
            }
        }

        private void ConvertStateFromCandidateTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info($"RaftState {Name}: Candidate->Follower");
                    State = RaftState.Follower;
                    VoteSuccess.Clear(); // 选举结束清除。
                    LogSequence.SetVoteFor(string.Empty);
                    return;

                case RaftState.Candidate:
                    logger.Info($"RaftState {Name}: Candidate->Candidate");
                    LogSequence.SetVoteFor(string.Empty); // 先清除，在真正自荐前可以给别人投票。
                    PrepareSendRequestVote();
                    return;

                case RaftState.Leader:
                    VoteSuccess.Clear(); // 选举结束清除。

                    logger.Info($"RaftState {Name}: Candidate->Leader");
                    State = RaftState.Leader;
                    LogSequence.SetVoteFor(string.Empty);
                    LeaderId = Name; // set to self

                    // (Reinitialized after election)
                    var nextIndex = LogSequence.LastIndex + 1;

                    Server.Config.ForEachConnector(
                        (c) =>
                        {
                            var cex = c as Server.ConnectorEx;
                            cex.Start(); // 马上尝试连接。
                            cex.NextIndex = nextIndex;
                            cex.MatchIndex = 0;
                        });

                    // Upon election:
                    // send initial empty AppendEntries RPCs
                    // (heartbeat)to each server; repeat during
                    // idle periods to prevent election timeouts(§5.2)
                    LogSequence.AppendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent),
                        false, out LeaderWaitReadyTerm, out LeaderWaitReadyIndex);
                    return;
            }
        }

        private void ConvertStateFromLeaderTo(RaftState newState)
        {
            ResetLeaderReadyAfterChangeState(); // 本来 Leader -> Follower 需要，为了健壮性，全部改变都重置。
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info($"RaftState {Name}: Leader->Follower");
                    State = RaftState.Follower;
                    return;

                case RaftState.Candidate:
                    logger.Error($"RaftState {Name} Impossible! Leader->Candidate");
                    return;

                case RaftState.Leader:
                    logger.Error($"RaftState {Name} Impossible! Leader->Leader");
                    return;
            }
        }

        internal void ConvertStateTo(RaftState newState)
        {
            ResetTimerTime();
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
