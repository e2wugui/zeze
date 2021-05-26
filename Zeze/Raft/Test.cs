using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Raft
{
    public class Test
    {
        public enum Mode { Trace, Net, Restart, Full, }
        public Mode TestMode { get; set; } = Mode.Trace;
        public int Count { get; set; } = 2000;
        public string RaftConfigFileName { get; set; } = "raft.xml";
        public int RaftFailPercent { get; set; } = 10;

        public Test()
        { 
        }

        public Test(string[] args)
        {
            Console.WriteLine("-mode [Trace|Net|Restart|Full] -Count [2000]");
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-mode":
                        TestMode = (Mode)Enum.Parse(typeof(Mode), args[++i]);
                        break;

                    case "-Count":
                        Count = int.Parse(args[++i]);
                        break;

                    case "-RaftConfig":
                        RaftConfigFileName = args[++i];
                        break;
                }
            }
        }

        private ConcurrentDictionary<string, TestRaft> Rafts { get; }
            = new ConcurrentDictionary<string, TestRaft>();

        private Agent Agent { get; set; }

        public void Run()
        {
            logger.Debug("Start.");
            var raftConfigStart = RaftConfig.Load(RaftConfigFileName);
            foreach (var node in raftConfigStart.Nodes)
            {
                // every node need a private config-file.
                var confName = System.IO.Path.GetTempFileName() + ".xml";
                System.IO.File.Copy(raftConfigStart.XmlFileName, confName);
                Rafts.GetOrAdd(node.Value.Name, (_) => new TestRaft(node.Value.Name, confName));
            }

            foreach (var raft in Rafts.Values)
            {
                raft.Raft.Server.Start();
            }

            Agent = new Agent(raftConfigStart);
            Agent.Client.AddFactoryHandle(
                new AddCount().TypeId,
                new Net.Service.ProtocolFactoryHandle()
                {
                    Factory = () => new AddCount(),
                });
            Agent.Client.AddFactoryHandle(
                new GetCount().TypeId,
                new Net.Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GetCount(),
                });
            Agent.Client.Start();

            switch (TestMode)
            {
                case Mode.Trace:
                    RunTrace();
                    break;

                default:
                    RunBatch();
                    break;
            }

            Agent.Client.Close();
            foreach (var raft in Rafts.Values)
            {
                raft.Raft.Server.Close();
            }
            logger.Debug("End.");
        }

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private int GetCurrentCount()
        {
            var r = new GetCount();
            Agent.SendForWait(r).Task.Wait();
            return r.ResultCode;
        }

        public void RunTrace()
        {
            Agent.SendForWait(new AddCount()).Task.Wait();
            logger.Debug($"#### Count={GetCurrentCount()} ####");
            Task[] tasks = new Task[1000];
            for (int i = 0; i < tasks.Length; ++i)
            {
                tasks[i] = Agent.SendForWait(new AddCount()).Task;
            }
            Task.WaitAll(tasks);
            logger.Debug($"#### Count={GetCurrentCount()} ####");
        }

        public void RunBatch()
        { 
        }

        public sealed class AddCount : Zeze.Net.Rpc<EmptyBean, EmptyBean>
        {
            public readonly static int ProtocolId_ = Bean.Hash16(typeof(AddCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;
        }

        public sealed class GetCount : Zeze.Net.Rpc<EmptyBean, EmptyBean>
        {
            public readonly static int ProtocolId_ = Bean.Hash16(typeof(GetCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;
        }

        public class TestStateMachine : StateMachine
        {
            public int Count { get; set; }

            public void AddCountAndWait()
            {
                Raft.AppendLog(new AddCount());
            }

            public sealed class AddCount : Log
            {
                public override void Apply(StateMachine stateMachine)
                {
                    (stateMachine as TestStateMachine).Count += 1;
                }

                public override void Decode(ByteBuffer bb)
                {
                }

                public override void Encode(ByteBuffer bb)
                {
                }
            }

            public override void LoadFromSnapshot(string path)
            {
                lock (Raft)
                {
                    using var file = new System.IO.FileStream(path, System.IO.FileMode.Open);
                    var bytes = new byte[1024];
                    int rc = file.Read(bytes);
                    var bb = ByteBuffer.Wrap(bytes, 0, rc);
                    Count = bb.ReadInt();
                }
            }

            public override bool Snapshot(string path, out long LastIncludedIndex, out long LastIncludedTerm)
            {
                using var file = new System.IO.FileStream(path, System.IO.FileMode.Create);
                long oldFirstIndex = 0;
                lock (Raft)
                {
                    var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
                    LastIncludedIndex = lastAppliedLog.Index;
                    LastIncludedTerm = lastAppliedLog.Term;
                    var bb = ByteBuffer.Allocate();
                    bb.WriteInt(Count);
                    file.Write(bb.Bytes, bb.ReadIndex, bb.Size);
                    file.Close();
                    oldFirstIndex = Raft.LogSequence.GetAndSetFirstIndex(LastIncludedIndex);
                }
                Raft.LogSequence.RemoveLogBeforeLastApplied(oldFirstIndex);
                return true;
            }

            public TestStateMachine()
            {
                AddFactory(new AddCount().TypeId, () => new AddCount());
            }
        }

        public class TestRaft
        {
            public Raft Raft { get; }
            public TestStateMachine StateMachine { get; }

            public TestRaft(string RaftName, string RaftConfigFileName)
            {
                StateMachine = new TestStateMachine();

                var raftConfig = RaftConfig.Load(RaftConfigFileName);
                raftConfig.AppendEntriesTimeout = 1000;
                raftConfig.LeaderHeartbeatTimer = 2000;
                raftConfig.LeaderLostTimeout = 4000;
                raftConfig.DbHome = Path.Combine(".", ((uint)RaftName.GetHashCode()).ToString());
                if (Directory.Exists(raftConfig.DbHome))
                    Directory.Delete(raftConfig.DbHome, true);
                Directory.CreateDirectory(raftConfig.DbHome);

                Raft = new Raft(StateMachine, RaftName, raftConfig);
                Raft.Server.AddFactoryHandle(
                    new AddCount().TypeId,
                    new Net.Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new AddCount(),
                        Handle = ProcessAddCount,
                    });

                Raft.Server.AddFactoryHandle(
                    new GetCount().TypeId,
                    new Net.Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GetCount(),
                        Handle = ProcessGetCount,
                    });
            }

            private int ProcessAddCount(Zeze.Net.Protocol p)
            {
                var r = p as AddCount;
                lock (StateMachine)
                {
                    StateMachine.AddCountAndWait();
                    r.SendResultCode(StateMachine.Count);
                }
                return Procedure.Success;
            }

            private int ProcessGetCount(Zeze.Net.Protocol p)
            {
                var r = p as GetCount;
                lock (StateMachine)
                {
                    r.SendResultCode(StateMachine.Count);
                }
                return Procedure.Success;
            }
        }

    }
}
