using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Raft
{
    /// <summary>
    /// 同时配置 Acceptor 和 Connector。
    /// 逻辑上主要使用 Connector。
    /// 两个Raft之间会有两个连接。
    /// 【注意】
    /// 为了简化配置，应用可以注册协议到Server，使用同一个Acceptor进行连接。
    /// </summary>
    public sealed class Server : Services.HandshakeBoth
    {
        public Raft Raft { get; }

        // 多个Raft实例才需要自定义配置名字，否则使用默认名字就可以了。
        public Server(Raft raft, string name, Zeze.Config config) : base(name, config)
        {
            Raft = raft;
        }

        public class ConnectorEx : Connector
        {
            public ConnectorEx(string host, int port)
                : base(host, port)
            {
                MaxReconnectDelay = 1000;
            }

            ////////////////////////////////////////////////
            // Volatile state on leaders:
            // (Reinitialized after election)
            /// <summary>
            /// for each server, index of the next log entry
            /// to send to that server(initialized to leader
            /// last log index + 1)
            /// </summary>
            internal long NextIndex { get; set; }

            /// <summary>
            /// for each server, index of highest log entry
            /// known to be replicated on server
            /// (initialized to 0, increases monotonically)
            /// </summary>
            internal long MatchIndex { get; set; }

            /// <summary>
            /// 正在安装Snapshot，用来阻止新的安装。
            /// </summary>
            internal bool InstallSnapshotting { get; set; }

            /// <summary>
            /// 每个连接只允许存在一个AppendEntries。
            /// </summary>
            internal AppendEntries Pending { get; set; }
            internal long AppendLogActiveTime { get; set; } = Util.Time.NowUnixMillis;

            public override void OnSocketClose(AsyncSocket closed, Exception e)
            {
                var server = closed.Service as Server;
                // 不能在网络回调中锁Raft，会死锁。因为在锁内发送数据是常用操作。
                Util.Task.Run(() =>
                {
                    lock (server.Raft)
                    {
                        // 安装快照服务器端不考虑续传，网络断开以后，重置状态。
                        // 以后需要的时候，再次启动新的安装流程。
                        InstallSnapshotting = false;
                        server.Raft.LogSequence.InstallSnapshotting.Remove(Name);
                    }
                }, "InstallSnapshotting.Remove");
                base.OnSocketClose(closed, e);
            }

            public override void OnSocketHandshakeDone(AsyncSocket so)
            {
                base.OnSocketHandshakeDone(so);
                var raft = (Service as Server).Raft;
                raft.ImportantThreadPool.QueueUserWorkItem(
                    () => Util.Task.Call(() => raft.LogSequence.TrySendAppendEntries(this, null), "TryStartLogCopy"));
            }
        }

        public static void CreateConnector(Service service, RaftConfig raftconf)
        {
            foreach (var node in raftconf.Nodes.Values)
            {
                if (raftconf.Name.Equals(node.Name))
                    continue; // skip self.
                service.Config.AddConnector(new ConnectorEx(node.Host, node.Port));
            }
        }

        public static void CreateAcceptor(Service service, RaftConfig raftconf)
        {
            if (false == raftconf.Nodes.TryGetValue(raftconf.Name, out var node))
                throw new Exception("raft Name Not In Node");
            service.Config.AddAcceptor(new Acceptor(node.Port, node.Host));
        }

        private bool IsImportantProtocol(long typeId)
        {
            return IsHandshakeProtocol(typeId)
                // 【注意】下面这些模块的Id总是为0。
                || typeId == RequestVote.TypeId_
                || typeId == AppendEntries.TypeId_
                || typeId == InstallSnapshot.TypeId_
                || typeId == LeaderIs.TypeId_;
        }

        public override void DispatchRpcResponse(Protocol p,
            Func<Protocol, long> responseHandle,
            ProtocolFactoryHandle factoryHandle)
        {
            if (IsImportantProtocol(p.TypeId))
            {
                // 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
                Raft.ImportantThreadPool.QueueUserWorkItem(
                    () => Util.Task.Call(() => responseHandle(p), p));
                return;
            }

            base.DispatchRpcResponse(p, responseHandle, factoryHandle);
        }

        public Util.TaskOneByOneByKey TaskOneByOne { get; } = new Util.TaskOneByOneByKey();

        private long ProcessRequest(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            return Util.Task.Call(() =>
            {
                if (Raft.WaitLeaderReady())
                {
                    var state = Raft.LogSequence.GetRequestState(p);
                    if (state > 0)
                    {
                        p.SendResultCode(Procedure.DuplicateRequest);
                        return Procedure.DuplicateRequest;
                    }

                    if (state < 0)
                    {
                        p.SendResultCode(Procedure.RaftApplied);
                        return Procedure.RaftApplied;
                    }
                    
                    return factoryHandle.Handle(p);
                }
                TrySendLeaderIs(p.Sender);
                return 0;
            },
            p,
            (p, code) => p.SendResultCode(code)
            );
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            // 防止Client不进入加密，直接发送用户协议。
            if (false == IsHandshakeProtocol(p.TypeId))
                p.Sender.VerifySecurity();

            if (IsImportantProtocol(p.TypeId))
            {
                // 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
                // 内部协议总是使用明确返回值或者超时，不使用框架的错误时自动发送结果。
                Raft.ImportantThreadPool.QueueUserWorkItem(
                    () => Util.Task.Call(() => factoryHandle.Handle(p), p, null));
                return;
            }
            // User Request
            if (Raft.IsLeader)
            {
                var iraftrpc = p as IRaftRpc;
                if (iraftrpc.Unique.RequestId <= 0)
                {
                    p.SendResultCode(Procedure.ErrorRequestId);
                    return;
                }

                //【防止重复的请求】
                // see Log.cs::LogSequence.TryApply
                TaskOneByOne.Execute(iraftrpc.Unique,
                    () => ProcessRequest(p, factoryHandle),
                    p.GetType().FullName,
                    () => p.SendResultCode(Procedure.RaftRetry)
                    );
                return;
            }

            TrySendLeaderIs(p.Sender);

            // 选举中
            // DONOT process application request.
        }

        private void TrySendLeaderIs(AsyncSocket sender)
        {
            if (Raft.HasLeader)
            {
                // redirect
                var redirect = new LeaderIs();
                redirect.Argument.Term = Raft.LogSequence.Term;
                redirect.Argument.LeaderId = Raft.LeaderId;
                redirect.Send(sender); // ignore response
                // DONOT process application request.
                return;
            }
        }

        public override void OnHandshakeDone(AsyncSocket so)
        {
            base.OnHandshakeDone(so);

            // 没有判断是否和其他Raft-Node的连接。
            Util.Task.Run(() =>
            {
                lock (Raft)
                {
                    if (Raft.IsLeader && Raft.LeaderReadyEvent.WaitOne(0))
                    {
                        var r = new LeaderIs();
                        r.Argument.Term = Raft.LogSequence.Term;
                        r.Argument.LeaderId = Raft.LeaderId;
                        r.Send(so); // skip result
                    }
                }
            }, "Raft.LeaderIs.Me");
        }
    }

    public class UniqueRequestId : Serializable
    {
        public string ClientId { get; set; } = string.Empty;
        public long RequestId { get; set; }

        public override int GetHashCode()
        {
            const int _prime_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _prime_ + ClientId.GetHashCode();
            _h_ = _h_ * _prime_ + RequestId.GetHashCode();
            return _h_;
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is UniqueRequestId other)
                return ClientId.Equals(other.ClientId) && RequestId == other.RequestId;
            return false;
        }

        public void Decode(ByteBuffer bb)
        {
            ClientId = bb.ReadString();
            RequestId = bb.ReadLong();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteString(ClientId);
            bb.WriteLong(RequestId);
        }

        public override string ToString()
        {
            return $"ClientId={ClientId} RequestId={RequestId}";
        }
    }

    public interface IRaftRpc
    {
        public abstract long CreateTime { get; set; }
        /// <summary>
        /// 唯一的请求编号，重发时保持不变。在一个ClientId内唯一即可。
        /// </summary>
        public abstract UniqueRequestId Unique { get; set; }
        public abstract long SendTime { get; set; } // 不系列化，Agent本地只用。
    }

    public abstract class RaftRpc<TArgument, TResult> : Rpc<TArgument, TResult>, IRaftRpc
        where TArgument : Bean, new()
        where TResult : Bean, new()
    {
        public long CreateTime { get; set; }
        public UniqueRequestId Unique { get; set; } = new UniqueRequestId();
        public long SendTime { get; set; }

        public override bool Send(AsyncSocket so)
        {
            // reset before re-send
            ResultCode = 0;
            IsTimeout = false;
            return base.Send(so);
        }

        public override string ToString()
        {
            return $"Client={Sender?.RemoteAddress} Unique={Unique} {base.ToString()}";
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteBool(IsRequest);
            bb.WriteLong(SessionId);
            bb.WriteLong(ResultCode);
            Unique.Encode(bb);
            bb.WriteLong(CreateTime);

            if (IsRequest)
            {
                Argument.Encode(bb);
            }
            else
            {
                Result.Encode(bb);
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            IsRequest = bb.ReadBool();
            SessionId = bb.ReadLong();
            ResultCode = bb.ReadLong();
            Unique.Decode(bb);
            CreateTime = bb.ReadLong();

            if (IsRequest)
            {
                Argument.Decode(bb);
            }
            else
            {
                Result.Decode(bb);
            }
        }
    }

    public sealed class Agent
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        // 保证在Raft-Server检查UniqueRequestId唯一性过期前唯一即可。
        // 使用持久化是为了避免短时间重启，Id重复。
        public Zeze.Util.PersistentAtomicLong UniqueRequestIdGenerator { get; }

        public RaftConfig RaftConfig { get; private set; }
        public NetClient Client { get; private set; }
        public string Name => Client.Name;

        public ConnectorEx Leader => _Leader;
        private volatile ConnectorEx _Leader;
        private Util.IdentityHashSet<Protocol> Pending = new Util.IdentityHashSet<Protocol>();

        public Util.SimpleThreadPool InternalThreadPool { get; }

        public Action<Agent> OnLeaderChanged { get; private set; }

        /// <summary>
        /// 发送Rpc请求。
        /// </summary>
        /// <typeparam name="TArgument"></typeparam>
        /// <typeparam name="TResult"></typeparam>
        /// <param name="rpc"></param>
        /// <param name="handle"></param>
        /// <returns></returns>
        public void Send<TArgument, TResult>(
            RaftRpc<TArgument, TResult> rpc,
            Func<Protocol, long> handle)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (null == handle)
                throw new ArgumentException();

            // 由于interface不能把setter弄成保护的，实际上外面可以修改。
            // 简单检查一下吧。
            if (rpc.Unique.RequestId != 0)
                throw new Exception("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

            rpc.Unique.RequestId = UniqueRequestIdGenerator.Next();
            rpc.Unique.ClientId = UniqueRequestIdGenerator.Name;
            rpc.CreateTime = Util.Time.NowUnixMillis;
            rpc.SendTime = rpc.CreateTime;

            var resendTimeout = RaftConfig.AppendEntriesTimeout + 2000;
            rpc.Timeout = resendTimeout;
            rpc.ResponseHandle = (p) => SendHandle(handle, rpc);
            Pending.Add(rpc);

            rpc.Send(_Leader?.TryGetReadySocket(), (p) => SendHandle(handle, rpc), resendTimeout);
        }

        private long SendHandle<TArgument, TResult>(Func<Protocol, long> userHandle, RaftRpc<TArgument, TResult> rpc)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (rpc.IsTimeout || IsRetryError(rpc.ResultCode))
            {
                rpc.IsTimeout = false;
                rpc.ResultCode = 0;
                return 0;
            }

            if (rpc.ResultCode == Procedure.RaftApplied)
                rpc.ResultCode = Procedure.Success;

            if (Pending.Remove(rpc))
                return userHandle(rpc);
            return 0;
        }

        private bool IsRetryError(long error)
        {
            switch (error)
            {
                case Procedure.CancelException:
                case Procedure.RaftRetry:
                case Procedure.DuplicateRequest:
                    return true;
            }
            return false;
        }

        private long SendForWaitHandle<TArgument, TResult>(
            TaskCompletionSource<RaftRpc<TArgument, TResult>> future,
            RaftRpc<TArgument, TResult> rpc)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (rpc.IsTimeout || IsRetryError(rpc.ResultCode))
            {
                rpc.IsTimeout = false;
                rpc.ResultCode = 0;
                return Procedure.Success;
            }

            if (rpc.ResultCode == Procedure.RaftApplied)
                rpc.ResultCode = Procedure.Success;

            if (Pending.Remove(rpc))
                future.SetResult(rpc);
            return Procedure.Success;
        }

        public TaskCompletionSource<RaftRpc<TArgument, TResult>>
            SendForWait<TArgument, TResult>(
            RaftRpc<TArgument, TResult> rpc)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            // 由于interface不能把setter弄成保护的，实际上外面可以修改。
            // 简单检查一下吧。
            if (rpc.Unique.RequestId != 0)
                throw new Exception("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

            rpc.Unique.RequestId = UniqueRequestIdGenerator.Next();
            rpc.Unique.ClientId = UniqueRequestIdGenerator.Name;
            rpc.CreateTime = Util.Time.NowUnixMillis;
            rpc.SendTime = rpc.CreateTime;

            var resendTimeout = RaftConfig.AppendEntriesTimeout + 2000;
            var future = new TaskCompletionSource<RaftRpc<TArgument, TResult>>();
            rpc.ResponseHandle = (p) => SendForWaitHandle(future, rpc);
            rpc.Timeout = resendTimeout;
            Pending.Add(rpc);

            rpc.Send(_Leader?.TryGetReadySocket(), (p) => SendForWaitHandle(future, rpc), resendTimeout);
            return future;
        }

        public long Term { get; internal set; }

        public class ConnectorEx : Connector
        {
            public ConnectorEx(string host, int port = 0)
                : base(host, port)
            {
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Client)
                    return;

                Client.Stop();
                Client = null;

                if (null == Client.Zeze)
                    InternalThreadPool.Shutdown();

                _Leader = null;
                Pending.Clear();
            }
        }

        public Agent(
            string name,
            Application zeze,
            RaftConfig raftconf = null,
            Action<Agent> onLeaderChanged = null
            )
        {
            InternalThreadPool = zeze.InternalThreadPool;
            UniqueRequestIdGenerator = Zeze.Util.PersistentAtomicLong.GetOrAdd($"{name}.{zeze.Config.ServerId}");
            Init(new NetClient(this, name, zeze), raftconf, onLeaderChanged);
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="raftconf"></param>
        /// <param name="config"></param>
        /// <param name="onLeaderChanged"></param>
        /// <param name="name"></param>
        public Agent(
            string name,
            RaftConfig raftconf = null,
            Zeze.Config config = null,
            Action<Agent> onLeaderChanged = null
            )
        {
            InternalThreadPool = new Util.SimpleThreadPool(5, "RaftAgentThreadPool");
            if (null == config)
                config = Config.Load();

            UniqueRequestIdGenerator = Zeze.Util.PersistentAtomicLong.GetOrAdd($"{name}.{config.ServerId}");
            Init(new NetClient(this, name, config), raftconf, onLeaderChanged);
        }

        private void Init(NetClient client, RaftConfig raftconf, Action<Agent> onLeaderChanged)
        {
            OnLeaderChanged = onLeaderChanged;

            if (null == raftconf)
                raftconf = RaftConfig.Load();

            RaftConfig = raftconf;
            Client = client;

            if (Client.Config.AcceptorCount() != 0)
                throw new Exception("Acceptor Found!");

            if (Client.Config.ConnectorCount() != 0)
                throw new Exception("Connector Found!");

            foreach (var node in RaftConfig.Nodes.Values)
            {
                Client.Config.AddConnector(new ConnectorEx(node.Host, node.Port));
            }

            Client.AddFactoryHandle(new LeaderIs().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new LeaderIs(),
                Handle = ProcessLeaderIs,
            });

            // ugly
            Util.Scheduler.Instance.Schedule((thisTask) => ReSend(), 1000, 1000);
        }

        private long ProcessLeaderIs(Protocol p)
        {
            var r = p as LeaderIs;
            logger.Info("=============== LEADERIS Old={0} New={1} From={2}", _Leader?.Name, r.Argument.LeaderId, p.Sender);

            var node = Client.Config.FindConnector(r.Argument.LeaderId);
            if (null == node)
            {
                // 当前 Agent 没有 Leader 的配置，创建一个。
                // 由于 Agent 在新增 node 时也会得到新配置广播，
                // 一般不会发生这种情况。
                var address = r.Argument.LeaderId.Split(':');
                if (Client.Config.TryGetOrAddConnector(
                    address[0], int.Parse(address[1]), true, out node))
                {
                    node.Start();
                }
            }

            if (SetLeader(r, node as ConnectorEx))
            {
                ReSend(true);
            }
            OnLeaderChanged?.Invoke(this);
            r.SendResultCode(0);
            return Procedure.Success;
        }

        private void ReSend(bool immediately = false)
        {
            // ReSendPendingRpc
            var leaderSocket = _Leader?.TryGetReadySocket();
            if (null != leaderSocket)
            {
                var now = Util.Time.NowUnixMillis;
                foreach (var rpc in Pending)
                {
                    var iraft = rpc as IRaftRpc;
                    if (immediately || now - iraft.SendTime > RaftConfig.AppendEntriesTimeout + 3000)
                    {
                        iraft.SendTime = now;
                        if (false == rpc.Send(leaderSocket))
                            logger.Warn("SendRequest faild {0}", rpc);
                    }
                }
            }
        }

        internal bool SetLeader(LeaderIs r, ConnectorEx newLeader)
        {
            lock (this)
            {
                if (r.Argument.Term < Term)
                {
                    logger.Warn("Skip LeaderIs {0} {1}", newLeader.Name, r);
                    return false;
                }

                //CollectPendingRpc(_Leader);
                var newone = _Leader != newLeader || r.Argument.Term > Term;
                _Leader = newLeader; // change current Leader
                Term = r.Argument.Term;
                return newone;
            }
        }

        /*
        private void CollectPendingRpc(Connector oldLeader)
        {
            var ctxSends = Client.GetRpcContexts((p) => p.Sender.Connector == oldLeader);
            var ctxPending = Client.RemoveRpcContexts(ctxSends.Keys);

            foreach (var rpc in ctxPending)
            {
                Pending.Add(rpc);
            }
        }
        */

        public sealed class NetClient : Services.HandshakeClient
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public Agent Agent { get; }

            public NetClient(Agent agent, string name, Application zeze)
                : base(name, zeze)
            {
                Agent = agent;
            }

            public NetClient(Agent agent, string name, Zeze.Config config)
                : base(name, config)
            {
                Agent = agent;
            }

            public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, long> responseHandle, ProtocolFactoryHandle factoryHandle)
            {
                Agent.InternalThreadPool.QueueUserWorkItem(() => responseHandle(rpc));
            }
        }
    }

    public sealed class RequestVoteArgument : Bean
    {
        public long Term { get; set; }
        public string CandidateId { get; set; }
        public long LastLogIndex { get; set; }
        public long LastLogTerm { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            CandidateId = bb.ReadString();
            LastLogIndex = bb.ReadLong();
            LastLogTerm = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(CandidateId);
            bb.WriteLong(LastLogIndex);
            bb.WriteLong(LastLogTerm);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} CandidateId={CandidateId} LastLogIndex={LastLogIndex} LastLogTerm={LastLogTerm})";
        }
    }

    public sealed class RequestVoteResult : Bean
    { 
        public long Term { get; set; }
        public bool VoteGranted { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            VoteGranted = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteBool(VoteGranted);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} VoteGranted={VoteGranted})";
        }
    }

    public sealed class RequestVote : Rpc<RequestVoteArgument, RequestVoteResult>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(RequestVote).FullName);
        public readonly static long TypeId_ = (uint)ProtocolId_;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class AppendEntriesArgument : Bean
    {
        public long Term { get; set; }
        public string LeaderId { get; set; } // Ip:Port
        public long PrevLogIndex { get; set; }
        public long PrevLogTerm { get; set; }
        public List<Binary> Entries { get; } = new List<Binary>();
        public long LeaderCommit { get; set; }

        // Leader发送AppendEntries时，从这里快速得到Entries的最后一个日志的Index
        // 不会系列化。
        public long LastEntryIndex { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            LeaderId = bb.ReadString();
            PrevLogIndex = bb.ReadLong();
            PrevLogTerm = bb.ReadLong();

            Entries.Clear();
            for (int c = bb.ReadInt(); c > 0; --c)
            {
                Entries.Add(bb.ReadBinary());
            }

            LeaderCommit = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(LeaderId);
            bb.WriteLong(PrevLogIndex);
            bb.WriteLong(PrevLogTerm);

            bb.WriteInt(Entries.Count);
            foreach (var e in Entries)
            {
                bb.WriteBinary(e);
            }

            bb.WriteLong(LeaderCommit);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} LeaderId={LeaderId} PrevLogIndex={PrevLogIndex} PrevLogTerm={PrevLogTerm} LeaderCommit={LeaderCommit})";
        }
    }

    public sealed class AppendEntriesResult : Bean
    {
        public long Term { get; set; }
        public bool Success { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            Success = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteBool(Success);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} Success={Success})";
        }
    }

    public sealed class AppendEntries : Rpc<AppendEntriesArgument, AppendEntriesResult>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(AppendEntries).FullName);
        public readonly static long TypeId_ = (uint)ProtocolId_;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class InstallSnapshotArgument : Bean
    {
        public long Term { get; set; }
        public string LeaderId { get; set; } // Ip:Port
        public long LastIncludedIndex { get; set; }
        public long LastIncludedTerm { get; set; }
        public long Offset { get; set; }
        public Binary Data { get; set; }
        public bool Done { get; set; }

        // 当Done为true时，把LastIncludedLog放到这里，Follower需要至少一个日志。
        public Binary LastIncludedLog { get; set; } = Binary.Empty;

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            LeaderId = bb.ReadString();
            LastIncludedIndex = bb.ReadLong();
            LastIncludedTerm = bb.ReadLong();

            Offset = bb.ReadLong();
            Data = bb.ReadBinary();
            Done = bb.ReadBool();

            LastIncludedLog = bb.ReadBinary();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(LeaderId);
            bb.WriteLong(LastIncludedIndex);
            bb.WriteLong(LastIncludedTerm);

            bb.WriteLong(Offset);
            bb.WriteBinary(Data);
            bb.WriteBool(Done);

            bb.WriteBinary(LastIncludedLog);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} LeaderId={LeaderId} LastIncludedIndex={LastIncludedIndex} LastIncludedTerm={LastIncludedTerm} Offset={Offset} Done={Done})";
        }
    }

    public sealed class InstallSnapshotResult : Bean
    {
        public long Term { get; set; }
        // 非标准Raft协议参数：用来支持续传。
        // >=0 : 让Leader从该位置继续传输数据。
        // -1  : 让Leader按自己顺序传输数据。
        public long Offset { get; set; } = -1;

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            Offset = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteLong(Offset);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} Offset={Offset})";
        }
    }

    public sealed class InstallSnapshot : Rpc<InstallSnapshotArgument, InstallSnapshotResult>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(InstallSnapshot).FullName);
        public readonly static long TypeId_ = (uint)ProtocolId_;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public const int ResultCodeTermError = 1;
        public const int ResultCodeOldInstall = 2;
        public const int ResultCodeNewOffset = 3;
    }

    /// <summary>
    /// 下面是非标准的Raft-Rpc，辅助Agent用的。
    /// </summary>
    public sealed class LeaderIsArgument : Bean
    {
        public long Term { get; set; }
        public string LeaderId { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            LeaderId = bb.ReadString();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(LeaderId);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return $"(Term={Term} LeaderId={LeaderId})";
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is LeaderIsArgument other)
            {
                return Term == other.Term && LeaderId.Equals(other.LeaderId);
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _prime_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _prime_ + Term.GetHashCode();
            _h_ = _h_ * _prime_ + LeaderId.GetHashCode();
            return _h_;
        }
    }

    /// <summary>
    /// LeaderIs 的发送时机
    /// 0. Agent 刚连上来时，如果Node是当前Leader，它马上这个Rpc给Agent。
    /// 1. Node 收到应用请求时，发现自己不是Leader，发送重定向。此时Node不处理请求（也不返回结果）。
    /// 2. 选举结束时也给Agent广播选举结果。
    /// </summary>
    public sealed class LeaderIs : Rpc<LeaderIsArgument, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash32(typeof(LeaderIs).FullName);
        public readonly static long TypeId_ = (uint)ProtocolId_;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

}
