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
        public Net Net { get; }
        public StateMachine StateMachine { get; }

        public void AppendLog(Log log)
        {
            // 构建RaftLog；加到队列中并广播；等待多数确认；提交
        }

        private int ProcessRequestVote(Zeze.Net.Protocol p)
        {
            var r = p as RequestVote;
            r.SendResultCode(0);
            return Procedure.Success;
        }

        private int ProcessAppendEntries(Zeze.Net.Protocol p)
        {
            var r = p as AppendEntries;
            r.SendResultCode(0);
            return Procedure.Success;
        }

        private int ProcessInstallSnapshot(Zeze.Net.Protocol p)
        {
            var r = p as InstallSnapshot;
            r.SendResultCode(0);
            return Procedure.Success;
        }

        public Raft(StateMachine sm, Config raftconf, Zeze.Config config = null)
        {
            if (null == config)
                config = Zeze.Config.Load();

            Net = new Net(this, config);

            if (Net.Config.AcceptorCount() != 0)
                throw new Exception("Acceptor Found!");

            if (Net.Config.ConnectorCount() != 0)
                throw new Exception("Connector Found!");

            // TODO 从 raftconf 中构建 Acceptor Connector 到 Net.Config 中

            sm.Raft = this;
            StateMachine = sm;
            RegisterInternalRpc();
        }

        private void RegisterInternalRpc()
        {
            Net.AddFactoryHandle(
                new RequestVote().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new RequestVote(),
                    Handle = ProcessRequestVote,
                });

            Net.AddFactoryHandle(
                new AppendEntries().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new AppendEntries(),
                    Handle = ProcessAppendEntries,
                });

            Net.AddFactoryHandle(
                new InstallSnapshot().TypeId,
                new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new InstallSnapshot(),
                    Handle = ProcessInstallSnapshot,
                });
        }

    }
}
