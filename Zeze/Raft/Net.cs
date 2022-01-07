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
                    if (Raft.LogSequence.LastAppliedAppRpcUniqueRequestId.TryGetValue(
                        p.Sender.RemoteAddress, out var max))
                    {
                        if (p.UniqueRequestId <= max)
                        {
                            p.SendResultCode(Procedure.DuplicateRequest);
                            return Procedure.DuplicateRequest;
                        }
                    }
                    return factoryHandle.Handle(p);
                }
                TrySendLeaderIs(p.Sender);
                return Procedure.LogicError;
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
                if (p.UniqueRequestId <= 0)
                {
                    p.SendResultCode(Procedure.ErrorRequestId);
                    return;
                }

                // 默认0，每个远程ip地址允许并发。
                // 不直接包含port信息，client.port容易改变。

                //【防止重复的请求】
                // see Log.cs::LogSequence.TryApply
                TaskOneByOne.Execute(p.Sender.RemoteAddress,
                    () => ProcessRequest(p, factoryHandle),
                    p.GetType().FullName,
                    () => p.SendResultCode(Procedure.CancelExcption)
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
            if (Raft.IsLeader && Raft.LeaderReadyEvent.WaitOne(0))
            {
                var r = new LeaderIs();
                r.Argument.Term = Raft.LogSequence.Term;
                r.Argument.LeaderId = Raft.LeaderId;
                r.Send(so); // skip result
            }
        }
    }

    public sealed class Agent
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public RaftConfig RaftConfig { get; private set; }
        public NetClient Client { get; private set; }
        public string Name => Client.Name;

        public ConnectorEx Leader => _Leader;

        private volatile ConnectorEx _Leader;

        private Util.IdentityHashMap<Protocol, Protocol> NotAutoResend { get; }
            = new Util.IdentityHashMap<Protocol, Protocol>();

        private List<Protocol> Pending = new List<Protocol>();

        public Action<Agent, Action> OnLeaderChanged { get; private set; }

        /// <summary>
        /// 发送Rpc请求。
        /// 如果 autoResend == true，那么总是返回成功。内部会在需要的时候重发请求。
        /// 如果 autoResend == false，那么返回结果表示是否成功。
        /// </summary>
        /// <typeparam name="TArgument"></typeparam>
        /// <typeparam name="TResult"></typeparam>
        /// <param name="rpc"></param>
        /// <param name="handle"></param>
        /// <param name="autoResend"></param>
        /// <param name="timeout"></param>
        /// <returns></returns>
        public bool Send<TArgument, TResult>(
            Rpc<TArgument, TResult> rpc,
            Func<Protocol, long> handle,
            bool autoResend = true,
            int timeout = -1)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (timeout < 0)
                timeout = RaftConfig.AppendEntriesTimeout + 1000;

            if (autoResend)
            {
                var tmp = _Leader;
                if (null != tmp
                    && tmp.IsHandshakeDone
                    && rpc.Send(tmp.Socket, handle, timeout))
                    return true;

                rpc.ResponseHandle = handle;
                rpc.Timeout = timeout;
                lock (this)
                {
                    Pending.Add(rpc);
                }
                return true;
            }

            // 记录不要自动发送的请求。
            NotAutoResend[rpc] = rpc;
            return rpc.Send(_Leader?.Socket, (p) =>
            {
                NotAutoResend.TryRemove(p, out var _);
                return handle(p);
            },
            timeout);
        }

        private long SendForWaitHandle<TArgument, TResult>(
            TaskCompletionSource<Rpc<TArgument, TResult>> future,
            Rpc<TArgument, TResult> rpc)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (rpc.IsTimeout)
            {
                future.TrySetException(new RpcTimeoutException("RaftRpcTimeout"));
            }
            else
            {
                future.SetResult(rpc);
            }
            return Procedure.Success;
        }

        public TaskCompletionSource<Rpc<TArgument, TResult>>
            SendForWait<TArgument, TResult>(
            Rpc<TArgument, TResult> rpc,
            bool autoResend = true,
            int timeout = -1)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (timeout < 0)
                timeout = RaftConfig.AppendEntriesTimeout + 1000;

            var future = new TaskCompletionSource<Rpc<TArgument, TResult>>();
            if (autoResend)
            {
                var tmp = _Leader;
                if (null != tmp
                    && tmp.IsHandshakeDone
                    && rpc.Send(tmp.Socket, (p) => SendForWaitHandle(future, rpc), timeout))
                    return future;

                rpc.ResponseHandle = (p) => SendForWaitHandle(future, rpc);
                rpc.Timeout = timeout;
                lock (this)
                {
                    Pending.Add(rpc);
                }
                return future;
            }

            // 记录不要自动发送的请求。
            NotAutoResend[rpc] = rpc;
            if (false == rpc.Send(_Leader?.Socket,
                (p) =>
                {
                    NotAutoResend.TryRemove(p, out var _);
                    return SendForWaitHandle(future, rpc);
                },
                timeout))
            {
                future.TrySetException(new Exception("Send Failed."));
            };
            return future;
        }

        public class ConnectorEx : Connector
        {
            public long Term { get; internal set; }

            public ConnectorEx(string host, int port = 0)
                : base(host, port)
            {
            }

            public override void OnSocketClose(AsyncSocket closed, Exception e)
            {
                // 先关闭重连，防止后面重发收集前又连上。
                // see Agent.NetClient
                base.IsAutoReconnect = false;
                base.OnSocketClose(closed, e);
            }
        }

        public Agent(
            string name,
            Application zeze,
            RaftConfig raftconf = null,
            Action<Agent, Action> onLeaderChanged = null
            )
        {
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
            Action<Agent, Action> onLeaderChanged = null
            )
        {
            if (null == config)
                config = Config.Load();

            Init(new NetClient(this, name, config), raftconf, onLeaderChanged);
        }

        private void Init(NetClient client, RaftConfig raftconf, Action<Agent, Action> onLeaderChanged)
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
        }

        private long ProcessLeaderIs(Protocol p)
        {
            var r = p as LeaderIs;
            logger.Debug("{0}: {1}", Name, r);

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

            if (TrySetLeader(r, node as ConnectorEx))
            {
                if (r.Sender.Connector.Name.Equals(r.Argument.LeaderId))
                {
                    // 来自 Leader 的公告。
                    if (null != OnLeaderChanged)
                    {
                        OnLeaderChanged(this, SetReady);
                    }
                    else
                    {
                        SetReady();
                    }
                }
                else
                {
                    // 从 Follower 得到的重定向，原则上不需要处理。
                    // 等待 LeaderIs 的通告即可。但是为了防止LeaderIs丢失，就处理一下吧。
                    // 【实际上和上面的处理逻辑一样】。
                    // 此时Leader可能没有准备好，但是提前给Leader发送请求是可以的。
                    if (null != OnLeaderChanged)
                    {
                        OnLeaderChanged(this, SetReady);
                    }
                    else
                    {
                        SetReady();
                    }
                }

            }
            r.SendResultCode(0);
            return Procedure.Success;
        }

        private void SetReady()
        {
            // ReSendPendingRpc
            var fails = new List<Protocol>();
            List<Protocol> pending = null;
            lock (this)
            {
                pending = Pending;
                Pending = new List<Protocol>();
            }
            try
            {
                foreach (var rpc in pending)
                {
                    if (NotAutoResend.ContainsKey(rpc))
                        continue;

                    if (false == rpc.Send(_Leader?.Socket))
                    {
                        // 这里发送失败，等待新的 LeaderIs 通告再继续。
                        fails.Add(rpc);
                    }
                }
            }
            finally
            {
                lock (this)
                {
                    foreach (var fail in fails)
                    {
                        Pending.Add(fail);
                    }
                }
            }
        }

        private void CollectPendingRpc(ConnectorEx oldLeader, AsyncSocket oldSocket)
        {
            if (null != oldLeader)
            {
                // 再 Rpc.UserState 里面记录发送目的的ConnectorEx，然后这里严格判断？
                // 由于一个时候只有Leader，所以直接使用Sender也足够了吧。
                var ctxSends = Client.GetRpcContextsToSender(oldSocket);
                var ctxPending = Client.RemoveRpcContets(ctxSends.Keys);
                lock (this)
                {
                    foreach (var rpc in ctxPending)
                    {
                        Pending.Add(rpc);
                    }
                }
            }
        }

        internal bool TrySetLeader(LeaderIs r, ConnectorEx newLeader)
        {
            lock (this)
            {
                if (r.Argument.Term < newLeader.Term)
                {
                    logger.Warn("{0} Skip LeaderIs {1}", Name, r);
                    return false;
                }
                if (_Leader != newLeader)
                {
                    // 把旧的_Leader的没有返回结果的请求收集起来，准备重新发送。
                    CollectPendingRpc(_Leader, _Leader?.Socket);
                }
                newLeader.Term = r.Argument.Term;
                _Leader = newLeader;
                return true;
            }
        }

        internal bool TryClearLeader(ConnectorEx oldLeader, AsyncSocket oldSocket)
        {
            lock (this)
            {
                if (_Leader == oldLeader)
                {
                    CollectPendingRpc(_Leader, oldSocket);
                    _Leader = null;
                    return true;
                }
                return false;
            }
        }

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

            public override void OnSocketDisposed(AsyncSocket so)
            {
                var connector = so.Connector as ConnectorEx;
                Agent.TryClearLeader(connector, so);
                base.OnSocketDisposed(so);
                connector.IsAutoReconnect = true;
                connector.TryReconnect();
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
