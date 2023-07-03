using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Raft
{
    /// <summary>
    /// 同时配置 Acceptor 和 Connector。
    /// 逻辑上主要使用 Connector。
    /// 两个Raft之间会有两个连接。
    /// 【注意】
    /// 为了简化配置，应用可以注册协议到Server，使用同一个Acceptor进行连接。
    /// </summary>
    public class Server : Services.HandshakeBoth
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
            /// 每个连接只允许存在一个AppendEntries。
            /// </summary>
            internal AppendEntries Pending { get; set; }

            internal InstallSnapshotState InstallSnapshotState { get; set; }

            internal long AppendLogActiveTime { get; set; } = Util.Time.NowUnixMillis;

            public override void OnSocketClose(AsyncSocket closed, Exception e)
            {
                var server = closed.Service as Server;
                _ = Util.Mission.CallAsync(async () =>
                {
                    // avoid deadlock: lock(socket), lock (Raft).
                    using (await server.Raft.Monitor.EnterAsync())
                    {
                        if (Socket == closed) // check is owner
                            await server.Raft.LogSequence.EndInstallSnapshot(this);
                        return 0;
                    }
                }, "OnSocketClose.EndInstallSnapshot");
                base.OnSocketClose(closed, e);
            }

            public override void OnSocketHandshakeDone(AsyncSocket so)
            {
                base.OnSocketHandshakeDone(so);
                var raft = (Service as Server).Raft;
                _ = Util.Mission.CallAsync(async () =>
                {
                    using (await raft.Monitor.EnterAsync())
                    {
                        await raft.LogSequence.TrySendAppendEntries(this, null);
                        return 0;
                    }
                }, "OnSocketHandshakeDone.TrySendAppendEntries");
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
                throw new Exception($"Raft.Name={raftconf.Name} Not In Node");
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
            Func<Protocol, Task<long>> responseHandle,
            ProtocolFactoryHandle factoryHandle)
        {
            DispatchRaftRpcResponse(p, responseHandle, factoryHandle);
        }

        public virtual void DispatchRaftRpcResponse(Protocol p, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
        {
            // 覆盖基类方法，不支持存储过程。
            // 按收到顺序处理，不并发。这样也避免线程切换。
            _ = Util.Mission.CallAsync(responseHandle, p, null);
        }

        public Util.TaskOneByOneByKey TaskOneByOne { get; } = new Util.TaskOneByOneByKey();

        public async Task<long> ProcessReqeust(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (await Raft.WaitLeaderReady())
            {
                var (expired, state) = await Raft.LogSequence.TryGetRequestState(p);
                if (expired)
                {
                    p.SendResultCode(ResultCode.RaftExpired);
                    return 0;
                }

                if (null != state)
                {
                    if (state.IsApplied)
                    {
                        p.SendResultCode(ResultCode.RaftApplied, state.RpcResult.Count > 0 ? state.RpcResult : null);
                        return 0;
                    }
                    p.SendResultCode(ResultCode.DuplicateRequest);
                    return 0;
                }
                return await factoryHandle.Handle(p);
            }
            TrySendLeaderIs(p.Sender);
            return 0;
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (IsImportantProtocol(p.TypeId))
            {
                _ = Util.Mission.CallAsync(factoryHandle.Handle, p, null);
                return;
            }

            // User Request
            if (Raft.IsWorkingLeader)
            {
                var iraftrpc = p as IRaftRpc;
                if (iraftrpc.Unique.RequestId <= 0)
                {
                    p.SendResultCode(ResultCode.ErrorRequestId);
                    return;
                }

                DispatchRaftRequest(p, async (p) => await ProcessReqeust(p, factoryHandle),
                    (p, code) => p.TrySendResultCode(code), () => p.TrySendResultCode(ResultCode.RaftRetry));
                //TaskOneByOne.Execute(iraftrpc.Unique, async (p) => await ProcessReqeust(p, factoryHandle),
                //    p, (p, code) => p.TrySendResultCode(code), () => p.TrySendResultCode(ResultCode.RaftRetry));
                return;
            }

            TrySendLeaderIs(p.Sender);

            // 选举中
            // DONOT process application request.
        }

        public virtual void DispatchRaftRequest(Protocol p, Func<Protocol, Task<long>> func,
            Action<Net.Protocol, long> actionWhenError, Action cancel)
        {
            //【防止重复的请求】
            // see Log.cs::LogSequence.TryApply
            TaskOneByOne.Execute(((IRaftRpc) p).Unique, func, p, actionWhenError, cancel);
	    }

        private void TrySendLeaderIs(AsyncSocket sender)
        {
            if (string.IsNullOrEmpty(Raft.LeaderId))
                return;

            if (Raft.Name.Equals(Raft.LeaderId) && false == Raft.IsLeader)
                return;

            // redirect
            var redirect = new LeaderIs();
            redirect.Argument.Term = Raft.LogSequence.Term;
            redirect.Argument.LeaderId = Raft.LeaderId; // maybe empty
            redirect.Argument.IsLeader = Raft.IsLeader;
            redirect.Send(sender); // ignore response
        }

        public override void OnHandshakeDone(AsyncSocket so)
        {
            base.OnHandshakeDone(so);

            // 没有判断是否和其他Raft-Node的连接。
            _ = Util.Mission.CallAsync(async () =>
            {
                using (await Raft.Monitor.EnterAsync())
                {
                    if (Raft.IsReadyLeader)
                    {
                        var r = new LeaderIs();
                        r.Argument.Term = Raft.LogSequence.Term;
                        r.Argument.LeaderId = Raft.LeaderId;
                        r.Argument.IsLeader = Raft.IsLeader;
                        r.Send(so); // skip result
                    }
                    return 0;
                }
            }, "OnHandshakeDone.Send.LeaderIs");
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

    public class UniqueRequestState : Serializable
    {
        public long LogIndex { get; set; }
        public bool IsApplied { get; set; }
        public Binary RpcResult { get; set; }

        public UniqueRequestState()
        {
        }

        public UniqueRequestState(RaftLog raftLog, bool isApplied)
        {
            LogIndex = raftLog.Index;
            IsApplied = isApplied;
            RpcResult = raftLog.Log.RpcResult;
        }

        public void Decode(ByteBuffer bb)
        {
            LogIndex = bb.ReadLong();
            IsApplied = bb.ReadBool();
            RpcResult = bb.ReadBinary();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteLong(LogIndex);
            bb.WriteBool(IsApplied);
            bb.WriteBinary(RpcResult);
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
        public abstract int RaftTimeout { get; }
        public abstract bool TrySetFutureException(Exception e);
        public abstract Func<Protocol, Task<long>> RaftHandle { get; set; }
        public abstract void SetIsTimeout(bool value);
    }

    public abstract class RaftRpc<TArgument, TResult> : Rpc<TArgument, TResult>, IRaftRpc
        where TArgument : Bean, new()
        where TResult : Bean, new()
    {
        public long CreateTime { get; set; }
        public UniqueRequestId Unique { get; set; } = new UniqueRequestId();
        public long SendTime { get; set; }
        public bool Urgent { get; set; } = false;

        // internal TaskCompletionSource<TResult>> RaftFuture; // 直接使用 Zeze.Net.Rpc.Future
        public bool TrySetFutureException(Exception e)
        {
            if (base.Future != null)
            {
                base.Future.TrySetException(e);
                return true; // 忽略结果，总是true。这里需要的语义是Future是否存在。
            }
            return false;
        }

        public int RaftTimeout => base.Timeout;
        public Func<Protocol, Task<long>> RaftHandle { get; set; }
        public void SetIsTimeout(bool value)
        {
            base.IsTimeout = value;
        }

        public override bool Send(AsyncSocket socket)
        {
            var bridge = new RaftRpcBridge<TArgument, TResult>(this)
            {
                ResponseHandle = this.ResponseHandle,
                Argument = this.Argument,
                CreateTime = this.CreateTime,
                Unique = this.Unique,
                ResultCode = this.ResultCode
            };

            return bridge.Send(socket, bridge.ResponseHandle, this.Timeout);
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

    internal sealed class RaftRpcBridge<TArgument, TResult> : RaftRpc<TArgument, TResult>
        where TArgument : Bean, new()
        where TResult : Bean, new()
    {
        private RaftRpc<TArgument, TResult> Real { get; }

        internal RaftRpcBridge(RaftRpc<TArgument, TResult> real)
        {
            Real = real;
        }

        public override int ModuleId => Real.ModuleId;
        public override int ProtocolId => Real.ProtocolId;
    }

    public sealed class Agent
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(Agent));

        // 保证在Raft-Server检查UniqueRequestId唯一性过期前唯一即可。
        // 使用持久化是为了避免短时间重启，Id重复。
        public Zeze.Util.PersistentAtomicLong UniqueRequestIdGenerator { get; }

        public RaftConfig RaftConfig { get; private set; }
        public NetClient Client { get; private set; }
        public string Name => Client.Name;

        public ConnectorEx Leader => _Leader;
        private volatile ConnectorEx _Leader;

        private readonly ConcurrentDictionary<long, Protocol> Pending = new();
        // 加急请求ReSend时优先发送，多个请求不保证顺序。这个应该仅用于Login之类的特殊协议，一般来说只有一个。
        private readonly ConcurrentDictionary<long, Protocol> UrgentPending = new();

        public Action<Agent> OnSetLeader { get; set; }
        public int PendingLimit { get; set; } = 5000; // -1 no limit

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
            Func<Protocol, Task<long>> handle, bool urgent = false)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            if (null == handle)
                throw new ArgumentException("null == handle");
            if (PendingLimit > 0 && Pending.Count > PendingLimit) // UrgentPending不限制。
                throw new Exception("too many pending");

            // 由于interface不能把setter弄成保护的，实际上外面可以修改。
            // 简单检查一下吧。
            if (rpc.Unique.RequestId != 0)
                throw new Exception("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

            rpc.Unique.RequestId = UniqueRequestIdGenerator.Next();
            // 外面在发送前可以设置clientId
            if (rpc.Unique.ClientId.Length == 0)
                rpc.Unique.ClientId = UniqueRequestIdGenerator.Name;
            rpc.CreateTime = Util.Time.NowUnixMillis;
            rpc.SendTime = rpc.CreateTime;
            if (rpc.Timeout == 0)
                rpc.Timeout = RaftConfig.AppendEntriesTimeout;
            rpc.RaftHandle = handle;
            rpc.Urgent = urgent;
            if (urgent)
            {
                if (!UrgentPending.TryAdd(rpc.Unique.RequestId, rpc))
                    throw new Exception("duplicate requestid rpc=" + rpc);
            }
            else
            {
                if (!Pending.TryAdd(rpc.Unique.RequestId, rpc))
                    throw new Exception("duplicate requestid rpc=" + rpc);
            }

            rpc.ResponseHandle = async (p) => await SendHandle(p, handle, rpc);
            rpc.Send(_Leader?.TryGetReadySocket());
        }

        private async Task<long> SendHandle<TArgument, TResult>(Protocol p, Func<Protocol, Task<long>> userHandle, RaftRpc<TArgument, TResult> rpc)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            var net = p as RaftRpc<TArgument, TResult>;
            if (net.IsTimeout || IsRetryError(net.ResultCode))
            {
                // Pending Will Resend.
                return 0;
            }

            if (Pending.TryRemove(rpc.Unique.RequestId, out _) || UrgentPending.TryRemove(rpc.Unique.RequestId, out _))
            {
                rpc.IsRequest = net.IsRequest;
                rpc.Result = net.Result;
                rpc.Sender = net.Sender;
                rpc.ResultCode = net.ResultCode;

                if (rpc.ResultCode == ResultCode.RaftApplied)
                {
                    rpc.IsTimeout = false;
                }
                logger.Debug($"Agent Rpc={rpc.GetType().Name} RequestId={rpc.Unique.RequestId} ResultCode={rpc.ResultCode} Sender={rpc.Sender}");
                return await userHandle(rpc);
            }
            return 0;
        }

        private static bool IsRetryError(long error)
        {
            return error switch
            {
                ResultCode.CancelException or ResultCode.RaftRetry or ResultCode.DuplicateRequest => true,
                _ => false,
            };
        }

        private long SendForWaitHandle<TArgument, TResult>(
            Protocol p,
            TaskCompletionSource<TResult> future,
            RaftRpc<TArgument, TResult> rpc)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            var net = p as RaftRpc<TArgument, TResult>;
            if (net.IsTimeout || IsRetryError(net.ResultCode))
            {
                // Pending Will Resend.
                return ResultCode.Success;
            }

            if (Pending.TryRemove(rpc.Unique.RequestId, out _) || UrgentPending.TryRemove(rpc.Unique.RequestId, out _))
            {
                rpc.IsRequest = net.IsRequest;
                rpc.Result = net.Result;
                rpc.Sender = net.Sender;
                rpc.ResultCode = net.ResultCode;

                if (rpc.ResultCode == ResultCode.RaftApplied)
                {
                    rpc.IsTimeout = false;
                }
                logger.Debug($"Agent Rpc={rpc.GetType().Name} RequestId={rpc.Unique.RequestId} ResultCode={rpc.ResultCode} Sender={rpc.Sender}");
                future.SetResult(rpc.Result);
            }
            return ResultCode.Success;
        }

        public async Task
            SendAsync<TArgument, TResult>(
            RaftRpc<TArgument, TResult> rpc, bool urgent = false)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            // 由于interface不能把setter弄成保护的，实际上外面可以修改。
            // 简单检查一下吧。
            if (rpc.Unique.RequestId != 0)
                throw new Exception("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

            if (PendingLimit > 0 && Pending.Count > PendingLimit) // UrgentPending不限制。
                throw new Exception("too many pending");

            rpc.Unique.RequestId = UniqueRequestIdGenerator.Next();
            // 外面在发送前可以设置clientId
            if (rpc.Unique.ClientId.Length == 0)
                rpc.Unique.ClientId = UniqueRequestIdGenerator.Name;
            rpc.CreateTime = Util.Time.NowUnixMillis;
            rpc.SendTime = rpc.CreateTime;
            if (rpc.Timeout == 0)
                rpc.Timeout = RaftConfig.AppendEntriesTimeout;

            var future = new TaskCompletionSource<TResult>(TaskCreationOptions.RunContinuationsAsynchronously);
            rpc.Future = future;
            rpc.Urgent = urgent;
            if (urgent)
            {
                if (!UrgentPending.TryAdd(rpc.Unique.RequestId, rpc))
                    throw new Exception("duplicate requestid rpc=" + rpc);
            }
            else
            {
                if (!Pending.TryAdd(rpc.Unique.RequestId, rpc))
                    throw new Exception("duplicate requestid rpc=" + rpc);
            }

            rpc.ResponseHandle = (p) => Task.FromResult(SendForWaitHandle(p, future, rpc));
            rpc.Send(_Leader?.TryGetReadySocket());
            await future.Task;
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

                var zeze = Client.Zeze;
                Client.Stop();
                Client = null;

                _Leader = null;
                Pending.Clear();
                UrgentPending.Clear();
            }
        }

        public Agent(
            string name,
            Application zeze,
            RaftConfig raftconf = null
            )
        {
            UniqueRequestIdGenerator = Zeze.Util.PersistentAtomicLong.GetOrAdd($"{name}.{zeze.Config.ServerId}");
            Init(new NetClient(this, name, zeze), raftconf);
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="raftconf"></param>
        /// <param name="config"></param>
        /// <param name="name"></param>
        public Agent(
            string name,
            RaftConfig raftconf = null,
            Zeze.Config config = null
            )
        {
            if (null == config)
                config = Config.Load();

            UniqueRequestIdGenerator = Zeze.Util.PersistentAtomicLong.GetOrAdd($"{name}.{config.ServerId}");
            Init(new NetClient(this, name, config), raftconf);
        }

        private void Init(NetClient client, RaftConfig raftconf)
        {
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
            Util.Scheduler.Schedule((thisTask) => ReSend(), 1000, 1000);
        }

        private Connector GetRandomConnector(Connector except)
        {
            var notme = new List<Connector>(Client.Config.ConnectorCount());
            Client.Config.ForEachConnector((c) => { if (c != except) notme.Add(c); });
            if (notme.Count > 0)
            {
                return notme[Util.Random.Instance.Next(notme.Count)];
            }
            return null;
        }

        private Task<long> ProcessLeaderIs(Protocol p)
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
                if (address.Length != 2)
                    return Task.FromResult(0L);

                if (Client.Config.TryGetOrAddConnector(
                    address[0], int.Parse(address[1]), true, out node))
                {
                    node.Start();
                }
            }
            else if (false == r.Argument.IsLeader && r.Argument.LeaderId.Equals(p.Sender.Connector.Name))
            {
                // 【错误处理】用来观察。
                logger.Warn("New Leader Is Not A Leader.");
                // 发送者不是Leader，但它的发送的LeaderId又是自己，【尝试选择另外一个Node】。
                node = GetRandomConnector(node);
            }

            if (SetLeader(r, node as ConnectorEx))
            {
                ReSend(true);
            }
            //OnLeaderChanged?.Invoke(this);
            r.SendResultCode(0);
            return Task.FromResult(ResultCode.Success);
        }

        private void ReSend(bool immediately = false)
        {
            _Leader?.Start();
            // ReSendPendingRpc
            var now = Util.Time.NowUnixMillis;
            var leaderSocket = _Leader?.TryGetReadySocket();
            var removed = new List<Protocol>(UrgentPending.Count + Pending.Count);
            foreach (var e in UrgentPending)
            {
                var rpc = e.Value;
                var iraft = rpc as IRaftRpc;
                if (iraft.RaftTimeout > 0 && now - iraft.CreateTime > iraft.RaftTimeout)
                {
                    if (UrgentPending.Remove(e.Key, out var r))
                        removed.Add(r);
                    continue;
                }
                if ((immediately && now - iraft.CreateTime > RaftConfig.AppendEntriesTimeout + 1000)
                    || now - iraft.SendTime > RaftConfig.AppendEntriesTimeout + 1000)
                {
                    logger.Debug($"{leaderSocket} {rpc}");

                    iraft.SendTime = now;
                    if (false == rpc.Send(leaderSocket))
                    {
                        logger.Info("SendRequest failed {0}", rpc);
                        break;
                    }
                }
            }

            foreach (var e in Pending)
            {
                var rpc = e.Value;
                var iraft = rpc as IRaftRpc;
                if (iraft.RaftTimeout > 0 && now - iraft.CreateTime > iraft.RaftTimeout)
                {
                    if (Pending.Remove(e.Key, out var r))
                        removed.Add(r);
                    continue;
                }
                if ((immediately && now - iraft.CreateTime > RaftConfig.AppendEntriesTimeout + 1000)
                    || now - iraft.SendTime > RaftConfig.AppendEntriesTimeout + 1000)
                {
                    logger.Debug($"{leaderSocket} {rpc}");

                    iraft.SendTime = now;
                    if (false == rpc.Send(leaderSocket))
                    {
                        logger.Info("SendRequest failed {0}", rpc);
                        break;
                    }
                }
            }
            Trigger(removed, "Timeout");
        }


        private void Trigger(List<Protocol> removed, string reason)
        {
            if (removed.Count == 0)
                return;

            Task.Run(() =>
            {
                foreach (var p in removed)
                {
                    var r = p as IRaftRpc;
                    r.SetIsTimeout(true);
                    if (r.TrySetFutureException(new Exception(reason)))
                        continue;
                    try
                    {
                        r.RaftHandle?.Invoke(p);
                    }
                    catch (Exception e)
                    {
                        logger.Error(e);
                    }
                }
            });
        }

        public void CancelPending()
        {
            // 不包括UrgentPending
            if (Pending.Count == 0)
                return;

            var removed = new List<Protocol>();
            // Pending存在并发访问，这样写更可靠。
            foreach (var e in Pending)
            {
                if (Pending.Remove(e.Key, out var r))
                    removed.Add(r);
            }
            Trigger(removed, "Cancel");
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

                _Leader = newLeader; // change current Leader

                Term = r.Argument.Term;
                _Leader?.Start(); // try connect immediately
                OnSetLeader?.Invoke(this);
                return true;
            }
        }

        public sealed class NetClient : Services.HandshakeClient
        {
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

            public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
            {
                _ = Util.Mission.CallAsync(responseHandle, rpc, null);
            }

            public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle pfh)
            {
                // await 按收到顺序处理，不并发。这样也避免线程切换。
                _ = Util.Mission.CallAsync(pfh.Handle, p, null);
            }
        }
    }

    public sealed class RequestVoteArgument : Bean
    {
        public long Term { get; set; }
        public string CandidateId { get; set; }
        public long LastLogIndex { get; set; }
        public long LastLogTerm { get; set; }
        public bool NodeReady { get; set; }


        public override void ClearParameters()
        {
            Term = 0;
            CandidateId = null;
            LastLogIndex = 0;
            LastLogTerm = 0;
            NodeReady = false;
        }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            CandidateId = bb.ReadString();
            LastLogIndex = bb.ReadLong();
            LastLogTerm = bb.ReadLong();
            NodeReady = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(CandidateId);
            bb.WriteLong(LastLogIndex);
            bb.WriteLong(LastLogTerm);
            bb.WriteBool(NodeReady);
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

        public override void ClearParameters()
        {
            Term = 0;
            VoteGranted = false;
        }

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

        public override string ToString()
        {
            return $"(Term={Term} VoteGranted={VoteGranted})";
        }
    }

    public sealed class RequestVote : Rpc<RequestVoteArgument, RequestVoteResult>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(RequestVote).FullName);
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
            for (int c = bb.ReadUInt(); c > 0; --c)
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

            bb.WriteUInt(Entries.Count);
            foreach (var e in Entries)
            {
                bb.WriteBinary(e);
            }

            bb.WriteLong(LeaderCommit);
        }

        public override string ToString()
        {
            return $"(Term={Term} LeaderId={LeaderId} PrevLogIndex={PrevLogIndex} PrevLogTerm={PrevLogTerm} LeaderCommit={LeaderCommit})";
        }

        public override void ClearParameters()
        {
            Term = 0;
            LeaderId = null;
            PrevLogIndex = 0;
            PrevLogTerm = 0;
            Entries.Clear();
            LeaderCommit = 0;
        }
    }

    public sealed class AppendEntriesResult : Bean
    {
        public long Term { get; set; }
        public bool Success { get; set; }
        public long NextIndex { get; set; } // for fast locate when mismatch

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            Success = bb.ReadBool();
            NextIndex = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteBool(Success);
            bb.WriteLong(NextIndex);
        }

        public override string ToString()
        {
            return $"(Term={Term} Success={Success})";
        }

        public override void ClearParameters()
        {
            Term = 0;
            Success = false;
            NextIndex = 0;
        }
    }

    public sealed class AppendEntries : Rpc<AppendEntriesArgument, AppendEntriesResult>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(AppendEntries).FullName);
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

        public override void ClearParameters()
        {
            Term = 0;
            LeaderId = null;
            LastIncludedIndex = 0;
            LastIncludedTerm = 0;
            Offset = 0;
            Data = null;
            Done = false;
            LastIncludedLog = Binary.Empty;
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

        public override string ToString()
        {
            return $"(Term={Term} Offset={Offset})";
        }

        public override void ClearParameters()
        {
            Term = 0;
            Offset = -1;
        }
    }

    public sealed class InstallSnapshot : Rpc<InstallSnapshotArgument, InstallSnapshotResult>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(InstallSnapshot).FullName);
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
        public bool IsLeader { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            LeaderId = bb.ReadString();
            IsLeader = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(LeaderId);
            bb.WriteBool(IsLeader);
        }

        public override string ToString()
        {
            return $"(Term={Term} LeaderId={LeaderId} IsLeader={IsLeader})";
        }

        public override void ClearParameters()
        {
            Term = 0;
            LeaderId = null;
            IsLeader = false;
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
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(LeaderIs).FullName);
        public readonly static long TypeId_ = (uint)ProtocolId_;

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

}
