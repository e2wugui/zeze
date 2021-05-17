using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Raft
{
    /// <summary>
    /// Raft Core
    /// </summary>
    public sealed class Raft
    {
        public string Name => RaftConfig.Name;
        public RaftConfig RaftConfig { get; }
        public LogSequence LogSequence { get; }

        public Server Server { get; }

        public StateMachine StateMachine { get; }

        private int ProcessRequestVote(Protocol p)
        {
            var r = p as RequestVote;
            RaftConfig.TrySetTerm(r.Argument.Term);
            r.SendResultCode(0);
            return Procedure.Success;
        }

        private int ProcessAppendEntries(Protocol p)
        {
            var r = p as AppendEntries;
            RaftConfig.TrySetTerm(r.Argument.Term);
            return LogSequence.FollowerOnAppendEntries(r);
        }

        private int ProcessInstallSnapshot(Protocol p)
        {
            var r = p as InstallSnapshot;
            RaftConfig.TrySetTerm(r.Argument.Term);
            r.SendResultCode(0);
            return Procedure.Success;
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
            Server.CreateAcceptor(Server, raftconf);
            Server.CreateConnector(Server, raftconf);

            sm.Raft = this;
            StateMachine = sm;
            RegisterInternalRpc();

            // TODO vote now
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
        }

    }
}
