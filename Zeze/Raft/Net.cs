using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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
    public sealed class Server : Service
    {
        public Raft Raft { get; }

        // 多个Raft实例才需要自定义配置名字，否则使用默认名字就可以了。
        public Server(Raft raft, string name, Zeze.Config config) : base(name, config)
        {
            Raft = raft;
        }

        public static void CreateConnector(Service service, Config raftconf)
        {
            foreach (var node in raftconf.Nodes.Values)
            {
                if (raftconf.Name.Equals(node.Name))
                    continue; // skip self.
                service.Config.AddConnector(new Connector(node.HostNameOrAddress, node.Port));
            }
        }

        public static void CreateAcceptor(Service service, Config raftconf)
        {
            if (false == raftconf.Nodes.TryGetValue(raftconf.Name, out var node))
                throw new Exception("raft Name Not In Node");
            service.Config.AddAcceptor(new Acceptor(node.Port, node.HostNameOrAddress));
        }
    }

    public sealed class Agent
    {
        public Config RaftConfig { get; }
        public NetClient Net { get; }
        public string Name => Net.Name;

        public ConcurrentDictionary<string, Connector> RaftNodes { get; }
            = new ConcurrentDictionary<string, Connector>();

        private Connector LeaderMaybe;

        public void SendRpc<TArgument, TResult>(
            Rpc<TArgument, TResult> rpc,
            Func<Protocol, int> handle,
            int timeout = 30000)
            where TArgument : Bean, new()
            where TResult : Bean, new()
        {
            // TODO 检查并等待Node的连接，等待Leader，发送，
            rpc.Send(LeaderMaybe.Socket, handle, timeout);
        }

        public Agent(
            Config raftconf = null,
            Zeze.Config config = null,
            string name = "Zeze.Raft.Agent")
        {
            if (null == raftconf)
                raftconf = Config.Load();

            RaftConfig = raftconf;

            if (null == config)
                config = Zeze.Config.Load();

            Net = new NetClient(this, name, config);

            if (Net.Config.AcceptorCount() != 0)
                throw new Exception("Acceptor Found!");

            if (Net.Config.ConnectorCount() != 0)
                throw new Exception("Connector Found!");

            raftconf.Name = ""; // Agent 需要连所有的Node，自己不会是Server。
            Server.CreateConnector(Net, raftconf);
            Net.Config.ForEachConnector((c) => RaftNodes.TryAdd(c.Name, c));

            Net.AddFactoryHandle(new LeaderIs().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new LeaderIs(),
                Handle = ProcessLeaderIs,
            });
        }

        private int ProcessLeaderIs(Protocol p)
        {
            var r = p as LeaderIs;

            if (false == RaftNodes.TryGetValue(r.Argument.LeaderId, out var nodeConnector))
            {
                // 当前 Agent 没有 Leader 的配置，创建一个。
                // 由于 Agent 在新增 node 时也会得到新配置广播，一般不会发生这种情况。
                var address = r.Argument.LeaderId.Split(':');
                if (Net.Config.TryGetOrAddConnector(
                    address[0], int.Parse(address[1]), true, out nodeConnector))
                {
                    RaftNodes.TryAdd(nodeConnector.Name, nodeConnector);
                }
            }

            ReplaceLeaderMaybe(nodeConnector);

            r.SendResultCode(0);
            return Procedure.Success;
        }

        internal void ReplaceLeaderMaybe(Connector newr)
        {
            lock (this)
            {
                // ReSendPending TODO
                LeaderMaybe = newr;
            }
        }

        internal void SetLeaderMaybe(Connector equalThis, Connector newset)
        {
            lock (this)
            {
                if (LeaderMaybe == equalThis)
                {
                    LeaderMaybe = newset;
                }
            }
        }

        public sealed class NetClient : Service
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public Agent Agent { get; }

            public NetClient(Agent agent, string name, Zeze.Config config)
                : base(name, config)
            {
                Agent = agent;
            }

            public override void OnHandshakeDone(AsyncSocket sender)
            {
                base.OnHandshakeDone(sender);

                // 首先尝试使用第一个连上的Node。
                Agent.SetLeaderMaybe(null, sender.Connector);
            }

            public override void OnSocketClose(AsyncSocket so, Exception e)
            {
                base.OnSocketClose(so, e);
                Agent.SetLeaderMaybe(so.Connector, null);
            }

            public override void OnSocketDisposed(AsyncSocket so)
            {
                base.OnSocketDisposed(so);
                var ctxSends = GetRpcContextsToSender(so);
                var ctxPending = RemoveRpcContets(ctxSends.Keys);
                foreach (var rpc in ctxPending)
                {
                    rpc.Send(Agent.LeaderMaybe.Socket); // TODO
                }
            }
        }
    }

    public sealed class RequestVoteArgument : Zeze.Transaction.Bean
    {
        public long Term { get; set; }
        public long CandidateId { get; set; }
        public long LastLogIndex { get; set; }
        public long LastLogTerm { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            CandidateId = bb.ReadLong();
            LastLogIndex = bb.ReadLong();
            LastLogTerm = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteLong(CandidateId);
            bb.WriteLong(LastLogIndex);
            bb.WriteLong(LastLogTerm);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }
    }

    public sealed class RequestVoteResult : Zeze.Transaction.Bean
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
    }

    public sealed class RequestVote : Rpc<RequestVoteArgument, RequestVoteResult>
    {
        public readonly static int ProtocolId_ = Bean.Hash16(typeof(RequestVote).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class AppendEntriesArgument : Zeze.Transaction.Bean
    {
        public long Term { get; set; }
        public string LeaderId { get; set; } // Ip:Port
        public long PrevLogIndex { get; set; }
        public long PrevLogTerm { get; set; }
        public List<Binary> Entries { get; } = new List<Binary>();
        public long LeaderCommit { get; set; }

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
    }

    public sealed class AppendEntriesResult : Zeze.Transaction.Bean
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
    }

    public sealed class AppendEntries : Rpc<AppendEntriesArgument, AppendEntriesResult>
    {
        public readonly static int ProtocolId_ = Bean.Hash16(typeof(AppendEntries).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class InstallSnapshotArgument : Zeze.Transaction.Bean
    {
        public long Term { get; set; }
        public string LeaderId { get; set; } // Ip:Port
        public long LastIncludedIndex { get; set; }
        public long LastIncludedTerm { get; set; }

        /// <summary>
        /// Raft 文档的Snapshot描述是基于文件流传输的。
        /// 这里使用Binary，也是基于流传输，但内容为自定义格式。
        /// 需要自己解析和处理，不仅限于文件传输。
        /// 【注意】跟Raft文档相比，这里去掉Offset。
        /// 当Leader发送最后一个Trunk时设置Done为true。
        /// </summary>
        public Binary Trunk { get; set; }
        public bool Done { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
            LeaderId = bb.ReadString();
            LastIncludedIndex = bb.ReadLong();
            LastIncludedTerm = bb.ReadLong();

            Trunk = bb.ReadBinary();
            Done = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
            bb.WriteString(LeaderId);
            bb.WriteLong(LastIncludedIndex);
            bb.WriteLong(LastIncludedTerm);

            bb.WriteBinary(Trunk);
            bb.WriteBool(Done);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }
    }

    public sealed class InstallSnapshotResult : Zeze.Transaction.Bean
    {
        public long Term { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            Term = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Term);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }
    }

    public sealed class InstallSnapshot : Rpc<InstallSnapshotArgument, InstallSnapshotResult>
    {
        public readonly static int ProtocolId_ = Bean.Hash16(typeof(InstallSnapshot).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    /// <summary>
    /// 下面是非标准的Raft-Rpc，辅助Agent用的。
    /// </summary>
    public sealed class LeaderIsArgument : Bean
    {
        public string LeaderId { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            LeaderId = bb.ReadString();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteString(LeaderId);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            throw new NotImplementedException();
        }
    }

    /// <summary>
    /// LeaderIs 的发送时机
    /// 0. Agent 刚连上来时，如果Node是当前Leader，它马上这个Rpc给Agent。
    /// 1. Node 收到应用请求时，发现自己不是Leader，发送重定向。此时Node不处理请求（也不返回结果）。
    /// 2. 选举结束时也给Agent广播选举结果。TOTO 实现选举时再考虑细节。
    /// </summary>
    public sealed class LeaderIs : Rpc<LeaderIsArgument, EmptyBean>
    {
        public readonly static int ProtocolId_ = Bean.Hash16(typeof(LeaderIs).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

}
