using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Serialize;
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
        public RaftConfig RaftConfig { get; private set; }
        private volatile LogSequence LogSequencePrivate;
        public LogSequence LogSequence { get { return LogSequencePrivate; } }
        public bool IsLeader => this.State == RaftState.Leader;
        public Server Server { get; private set; }
        public bool IsWorkingLeader => IsLeader && false == IsShutdown;

        public StateMachine StateMachine { get; }
        public AsyncExecutor AsyncExecutor { get; }

        public delegate void AtFatalKill();

        public AtFatalKill AtFatalKills { get; set; } = () => { };
        public Nito.AsyncEx.AsyncMonitor Monitor { get; } = new();

        public void FatalKill()
        {
            IsShutdown = true;
            AtFatalKills?.Invoke();
            LogSequence.Close();
            NLog.LogManager.Shutdown();
            System.Diagnostics.Process.GetCurrentProcess().Kill();
        }

        public async Task AppendLog(Log log, Bean result = null)
        {
            if (result != null)
            {
                var bb = ByteBuffer.Allocate(1024);
                result.Encode(bb);
                log.RpcResult = new Binary(bb);
            }
            try
            {
                await LogSequence.AppendLog(log);
            }
            catch (Exception ex)
            {
                // 内部错误通通转化成Retry。
                throw new RaftRetryException($"Inner Error: {ex}");
            }
        }

        internal volatile bool IsShutdown = false;

        private void CancelAllReceiveSnapshotting()
        {
            foreach (var e in ReceiveSnapshotting)
            {
                if (ReceiveSnapshotting.TryRemove(e.Key, out var _))
                    e.Value.Close();
            }
        }

        public async Task Shutdown()
        {
            using (await Monitor.EnterAsync())
            {
                // shutdown 只做一次。
                if (IsShutdown)
                    return;

                AppDomain.CurrentDomain.ProcessExit -= ProcessExit;
                IsShutdown = true;
            }

            await Mission.AwaitNullableTask(LogSequencePrivate.RemoveLogBeforeFuture?.Task);
            await Mission.AwaitNullableTask(LogSequencePrivate.ApplyFuture?.Task);

            Server.Stop();
            AsyncExecutor.Shutdown();

            using (await Monitor.EnterAsync())
            {
                await LogSequence.CancelAllInstallSnapshot();
                CancelAllReceiveSnapshotting();

                TimerTask?.Cancel();
                TimerTask = null;
                await ConvertStateTo(RaftState.Follower);
                LogSequencePrivate.Close();
                LogSequencePrivate = null;
            }
        }

        private void ProcessExit(object sender, EventArgs e)
        {
            Shutdown().Wait();
        }

        public Raft(StateMachine sm, int executorPoolSize = 100)
        {
            StateMachine = sm;
            StateMachine.Raft = this;
            AsyncExecutor = new (() => executorPoolSize);
        }

        public async Task<Raft> OpenAsync(
            string RaftName = null,
            RaftConfig raftconf = null,
            Zeze.Config config = null,
            string name = "Zeze.Raft.Server")
        {
            using (await Monitor.EnterAsync())
            {
                // 打开互斥保护一下。
                if (RaftConfig != null)
                    throw new InvalidOperationException($"{RaftName} Has Opened.");

                if (null == raftconf)
                    raftconf = RaftConfig.Load();

                raftconf.Verify();
                RaftConfig = raftconf;
            }

            if (false == string.IsNullOrEmpty(RaftName))
            {
                // 如果 DbHome 和 Name 相关，一般表示没有特别配置。
                // 此处特别设置 Raft.Name 时，需要一起更新。
                if (raftconf.DbHome.Equals(raftconf.Name.Replace(":", "_")))
                    raftconf.DbHome = RaftName.Replace(":", "_");
                raftconf.Name = RaftName;
            }

            if (null == config)
                config = Zeze.Config.Load();

            Server = new Server(this, name, config);
            if (Server.Config.AcceptorCount() != 0)
                throw new Exception("Acceptor Found!");
            if (Server.Config.ConnectorCount() != 0)
                throw new Exception("Connector Found!");
            if (RaftConfig.Nodes.Count < 3)
                throw new Exception("Startup Nodes.Count Must >= 3.");

            Server.CreateAcceptor(Server, raftconf);
            Server.CreateConnector(Server, raftconf);

            Directory.CreateDirectory(RaftConfig.DbHome);

            LogSequencePrivate = new LogSequence(this);
            await LogSequencePrivate.OpenAsync();

            RegisterInternalRpc();

            var snapshot = LogSequence.SnapshotFullName;
            if (File.Exists(snapshot))
                await StateMachine.LoadSnapshot(snapshot);

            LogSequence.StartSnapshotTimer();

            AppDomain.CurrentDomain.ProcessExit += ProcessExit;
            TimerTask = Scheduler.Schedule(OnTimer, 10);
            return this;
        }

        private async Task<long> ProcessAppendEntries(Protocol p)
        {
            var r = p as AppendEntries;
            using (await Monitor.EnterAsync())
            {
                return await LogSequence.FollowerOnAppendEntries(r);
            }
        }

        private readonly ConcurrentDictionary<long, FileStream> ReceiveSnapshotting = new();

        private async Task<long> ProcessInstallSnapshot(Protocol p)
        {
            var r = p as InstallSnapshot;

            using (await Monitor.EnterAsync())
            {
                r.Result.Term = LogSequence.Term;
                if (r.Argument.Term < LogSequence.Term)
                {
                    // 1. Reply immediately if term < currentTerm
                    r.SendResultCode(InstallSnapshot.ResultCodeTermError);
                    return 0;
                }

                if (await LogSequence.TrySetTerm(r.Argument.Term) == LogSequence.SetTermResult.Newer)
                {
                    r.Result.Term = LogSequence.Term;
                    // new term found.
                    await ConvertStateTo(RaftState.Follower);
                }
                LeaderId = r.Argument.LeaderId;
                LogSequence.LeaderActiveTime = Zeze.Util.Time.NowUnixMillis;
            }

            // 2. Create new snapshot file if first chunk(offset is 0)
            // 把 LastIncludedIndex 放到文件名中，
            // 新的InstallSnapshot不覆盖原来进行中或中断的。
            var path = Path.Combine(RaftConfig.DbHome,
                $"{LogSequence.SnapshotFileName}.installing.{r.Argument.LastIncludedIndex}");

            FileStream outputFileStream = null;
            if (r.Argument.Offset == 0)
            {
                // GetOrAdd 允许重新开始。
                outputFileStream = ReceiveSnapshotting.GetOrAdd(
                    r.Argument.LastIncludedIndex,
                    (_) => new FileStream(path, FileMode.OpenOrCreate));
                outputFileStream.Seek(0, SeekOrigin.Begin);
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
                ReceiveSnapshotting.TryRemove(r.Argument.LastIncludedIndex, out _);
                outputFileStream.Close();
                foreach (var e in ReceiveSnapshotting)
                {
                    if (e.Key < r.Argument.LastIncludedIndex)
                    {
                        if (ReceiveSnapshotting.TryRemove(e.Key, out var _))
                        {
                            e.Value.Close();
                            var pathDelete = Path.Combine(RaftConfig.DbHome, $"{LogSequence.SnapshotFileName}.installing.{e.Key}");
                            File.Delete(pathDelete);
                        }
                    }
                }
                // 剩下的处理流程在下面的函数里面。
                await LogSequence.EndReceiveInstallSnapshot(outputFileStream, r);
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
        private readonly IdentityHashSet<RequestVote> RequestVotes = new();
        private long NextVoteTime;       // 等待当前轮选举结果超时；用来启动下一次选举。

        // Leader
        private long LeaderWaitReadyTerm;
        private long LeaderWaitReadyIndex;
        internal volatile TaskCompletionSource<bool> LeaderReadyFuture = new(TaskCreationOptions.RunContinuationsAsynchronously);

        // Follower
        private long LeaderLostTimeout;

        // 重置 OnTimer 需要的所有时间。
        private void ResetTimerTime()
        {
            var now = Time.NowUnixMillis;
            LogSequence.LeaderActiveTime = now;
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
        private async Task OnTimer(SchedulerTask ThisTask)
        {
            if (IsShutdown)
                return;

            using (await Monitor.EnterAsync())
            {
                try
                {
                    switch (State)
                    {
                        case RaftState.Follower:
                            if (Time.NowUnixMillis - LogSequence.LeaderActiveTime > LeaderLostTimeout)
                            {
                                await ConvertStateTo(RaftState.Candidate);
                            }
                            break;

                        case RaftState.Candidate:
                            if (Time.NowUnixMillis > NextVoteTime)
                            {
                                // vote timeout. restart
                                await ConvertStateTo(RaftState.Candidate);
                            }

                            break;

                        case RaftState.Leader:
                            var now = Time.NowUnixMillis;
                            Server.Config.ForEachConnector(
                                (c) =>
                                {
                                    var cex = c as Server.ConnectorEx;
                                    if (now - cex.AppendLogActiveTime > RaftConfig.LeaderHeartbeatTimer)
                                        LogSequence.SendHeartbeatTo(cex);
                                });
                            break;
                    }
                    if (++LowPrecisionTimer > 1000) // 10s
                    {
                        LowPrecisionTimer = 0;
                        await OnLowPrecisionTimer();
                    }
                }
                finally
                {
                    TimerTask = Scheduler.Schedule(OnTimer, 10);
                }
            }
        }

        private long LowPrecisionTimer;

        private async Task OnLowPrecisionTimer()
        {
            Server.Config.ForEachConnector((c) => c.Start()); // Connector Reconnect Bug?
            await LogSequence.RemoveExpiredUniqueRequestSet();
        }

        /// <summary>
        /// true，IsLeader && LeaderReady;
        /// false, !IsLeader
        /// </summary>
        /// <returns></returns>
        internal async Task<bool> WaitLeaderReady()
        {
            using (await Monitor.EnterAsync())
            {
                var volatileTmp = LeaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
                while (IsLeader)
                {
                    if (volatileTmp.Task.IsCompleted)
                        return volatileTmp.Task.Result;
                    await Monitor.WaitAsync();
                }
                return false;
            }
        }

        public bool IsReadyLeader
        {
            get
            {
                var volatileTmp = LeaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
                return IsLeader && volatileTmp.Task.Wait(0) && volatileTmp.Task.Result;
            }
        }

        internal void ResetLeaderReadyAfterChangeState()
        {
            LeaderReadyFuture.TrySetResult(false);
            // prepare for next leader
            LeaderReadyFuture = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Monitor.PulseAll();
        }

        internal void SetLeaderReady(RaftLog heart)
        {
            if (IsLeader)
            {
                // 是否过期First-Heartbeat。
                // 使用 LeaderReadyFuture 可以更加精确的识别。
                // 但是，由于RaftLog不是常驻内存的，保存不了进程级别的变量。
                if (heart.Term != LeaderWaitReadyTerm || heart.Index != LeaderWaitReadyIndex)
                    return;

                LeaderWaitReadyIndex = 0;
                LeaderWaitReadyTerm = 0;

                logger.Info($"{Name} {RaftConfig.DbHome} LastIndex={LogSequence.LastIndex} Count={LogSequence.GetTestStateMachineCount()}");

                LeaderReadyFuture.TrySetResult(true);
                Monitor.PulseAll();

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

        private async Task<bool> IsLastLogUpToDate(RequestVoteArgument candidate)
        {
            // NodeReady local candidate
            //           false false       IsLastLogUpToDate
            //           false true        false
            //           true  false       false
            //           true  true        IsLastLogUpToDate
            var last = await LogSequence.LastRaftLogTermIndex();
            if (false == LogSequence.NodeReady)
            {
                if (false == candidate.NodeReady)
                {
                    // 整个Raft集群第一次启动时，允许给初始节点投票。此时所有的初始节点形成多数派。任何一个当选都是可以的。
                    // 以后由于机器更换再次启动而处于初始状态的节点肯定是少数派，即使它们之间互相投票，也不能成功。
                    // 如果违背了这点，意味着违背了Raft的可用原则，已经不在Raft的处理范围内了。
                    return IsLastLogUpToDate(last, candidate);
                }

                // 拒绝投票直到发现达成多数派。
                return false;
            }
            if (false == candidate.NodeReady)
                return false;
            return IsLastLogUpToDate(last, candidate);
        }

        private static bool IsLastLogUpToDate(RaftLog last, RequestVoteArgument candidate)
        {
            if (candidate.LastLogTerm > last.Term)
                return true;
            if (candidate.LastLogTerm < last.Term)
                return false;
            return candidate.LastLogIndex >= last.Index;
        }

        private async Task<long> ProcessRequestVote(Protocol p)
        {
            using (await Monitor.EnterAsync())
            {
                var r = p as RequestVote;

                // 不管任何状态重置下一次时间，使得每个node从大概一个时刻开始。
                NextVoteTime = Time.NowUnixMillis + RaftConfig.ElectionTimeout;

                if (await LogSequence.TrySetTerm(r.Argument.Term) == LogSequence.SetTermResult.Newer)
                {
                    // new term found.
                    await ConvertStateTo(RaftState.Follower);
                }
                // else continue process

                // RequestVote RPC
                // Receiver implementation:
                // 1.Reply false if term < currentTerm(§5.1)
                // 2.If votedFor is null or candidateId, and candidate’s log is at
                // least as up - to - date as receiver’s log, grant vote(§5.2, §5.4)

                r.Result.Term = LogSequence.Term;
                r.Result.VoteGranted = r.Argument.Term == LogSequence.Term
                    && LogSequence.CanVoteFor(r.Argument.CandidateId)
                    && await IsLastLogUpToDate(r.Argument);

                if (r.Result.VoteGranted)
                {
                    await LogSequence.SetVoteFor(r.Argument.CandidateId);
                }
                logger.Info("{0}: VoteFor={1} Rpc={2}", Name, LogSequence.VoteFor, r);
                r.SendResultCode(0);

                return Procedure.Success;
            }
        }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        private async Task<long> ProcessLeaderIs(Protocol p)
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            var r = p as LeaderIs;

            // 这个协议是发送给Agent(Client)的，
            // 为了简单，不做区分。
            // Raft也会收到，忽略。
            r.SendResultCode(0);

            return Procedure.Success;
        }

        private async Task<long> ProcessRequestVoteResult(RequestVote rpc)
        {
            if (rpc.IsTimeout || rpc.ResultCode != 0)
                return 0; // skip error. re-vote later. 

            using (await Monitor.EnterAsync())
            {
                if (LogSequence.Term != rpc.Argument.Term || State != RaftState.Candidate)
                {
                    // 结果回来时，上下文已经发生变化，忽略这个结果。
                    logger.Info($"{Name} NotOwner={LogSequence.Term != rpc.Argument.Term} NotCandidate={State != RaftState.Candidate}");
                    return 0;
                }

                if (await LogSequence.TrySetTerm(rpc.Result.Term) == LogSequence.SetTermResult.Newer)
                {
                    // new term found
                    await ConvertStateTo(RaftState.Follower);
                    return Procedure.Success;
                }

                if (RequestVotes.Contains(rpc) && rpc.Result.VoteGranted)
                {
                    int granteds = 0;
                    foreach (var vote in RequestVotes)
                    {
                        if (vote.Result.VoteGranted)
                            ++granteds;
                    }
                    if (
                        // 确保当前状态是选举中。没有判断这个，
                        // 后面 ConvertStateTo 也会忽略不正确的状态转换。
                        State == RaftState.Candidate
                        // 加上自己就是多数派了。
                        && granteds >= RaftConfig.HalfCount
                        && LogSequence.CanVoteFor(Name))
                    {
                        await LogSequence.SetVoteFor(Name);
                        await ConvertStateTo(RaftState.Leader);
                    }
                }

                return Procedure.Success;
            }
        }

        private async Task SendRequestVote()
        {
            RequestVotes.Clear(); // 每次选举开始清除。
            //LogSequence.SetVoteFor(Name); // 先收集结果，达到 RaftConfig.HalfCount 才判断是否给自己投票。
            await LogSequence.TrySetTerm(LogSequence.Term + 1);

            var arg = new RequestVoteArgument
            {
                Term = LogSequence.Term,
                CandidateId = Name
            };
            var log = await LogSequence.LastRaftLogTermIndex();
            arg.LastLogIndex = log.Index;
            arg.LastLogTerm = log.Term;
            arg.NodeReady = LogSequence.NodeReady;

            NextVoteTime = Time.NowUnixMillis + RaftConfig.ElectionTimeout;
            Server.Config.ForEachConnector((c) =>
            {
                var rpc = new RequestVote() { Argument = arg };
                RequestVotes.Add(rpc);
                var sendresult = rpc.Send(c.TryGetReadySocket(),
                    async (p) => await ProcessRequestVoteResult(rpc),
                    RaftConfig.AppendEntriesTimeout);
                logger.Info("{0}:{1}: SendRequestVote {2}", Name, sendresult, rpc);
            });
        }

        private async Task ConvertStateFromFollowerTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info($"RaftState {Name}: Follower->Follower");
                    LeaderLostTimeout = RaftConfig.ElectionTimeout;
                    return;

                case RaftState.Candidate:
                    logger.Info($"RaftState {Name}: Follower->Candidate");
                    State = RaftState.Candidate;
                    await SendRequestVote();
                    return;

                case RaftState.Leader:
                    // 并发的RequestVote的结果如果没有判断当前状态，可能会到达这里。
                    // 不是什么大问题。see ProcessRequestVoteResult
                    logger.Info($"RaftState {Name} Impossible! Follower->Leader");
                    return;
            }
        }

        private async Task ConvertStateFromCandidateTo(RaftState newState)
        {
            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info($"RaftState {Name}: Candidate->Follower");
                    LeaderLostTimeout = RaftConfig.ElectionTimeout;
                    State = RaftState.Follower;
                    RequestVotes.Clear();
                    return;

                case RaftState.Candidate:
                    logger.Info($"RaftState {Name}: Candidate->Candidate");
                    await SendRequestVote();
                    return;

                case RaftState.Leader:
                    RequestVotes.Clear();
                    CancelAllReceiveSnapshotting();

                    logger.Info($"RaftState {Name}: Candidate->Leader");
                    State = RaftState.Leader;
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
                    (LeaderWaitReadyTerm, LeaderWaitReadyIndex, _) = await LogSequence.AppendLog(
                        new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent, Name), false);
                    return;
            }
        }

        private async Task ConvertStateFromLeaderTo(RaftState newState)
        {
            // 本来 Leader -> Follower 需要，为了健壮性，全部改变都重置。
            ResetLeaderReadyAfterChangeState();
            await LogSequence.CancelAllInstallSnapshot();

            switch (newState)
            {
                case RaftState.Follower:
                    logger.Info($"RaftState {Name}: Leader->Follower");
                    State = RaftState.Follower;
                    LeaderLostTimeout = RaftConfig.ElectionTimeout;
                    return;

                case RaftState.Candidate:
                    logger.Error($"RaftState {Name} Impossible! Leader->Candidate");
                    return;

                case RaftState.Leader:
                    logger.Error($"RaftState {Name} Impossible! Leader->Leader");
                    return;
            }
        }

        internal async Task ConvertStateTo(RaftState newState)
        {
            ResetTimerTime();
            // 按真值表处理所有情况。
            switch (State)
            {
                case RaftState.Follower:
                    await ConvertStateFromFollowerTo(newState);
                    return;

                case RaftState.Candidate:
                    await ConvertStateFromCandidateTo(newState);
                    return;

                case RaftState.Leader:
                    await ConvertStateFromLeaderTo(newState);
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
