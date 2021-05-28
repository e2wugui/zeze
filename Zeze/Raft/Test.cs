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
        public string RaftConfigFileName { get; set; } = "raft.xml";

        public Test()
        { 
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
            RunTrace();
            Agent.Client.Stop();
            foreach (var raft in Rafts.Values)
            {
                raft.Raft.Server.Stop();
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

        private int CurrentCount;
        private int AddErrorCount;
        private int AddTimeoutCount;

        private void CheckCurrentCount(string stepName, int expect)
        {
            var before = CurrentCount;
            CurrentCount = GetCurrentCount();
            var diff = CurrentCount - before;
            if (diff != expect)
            {
                logger.Fatal($"#### {stepName} Expect={expect} Now={diff},Error={AddErrorCount},Timeout={AddTimeoutCount}");
            }
        }

        private void ConcurrentAddCount(string stepName, int concurrent)
        {
            AddTimeoutCount = 0;
            AddErrorCount = 0;
            var requests = new List<AddCount>();
            for (int i = 0; i < concurrent; ++i)
                requests.Add(new AddCount());

            Task[] tasks = new Task[concurrent];
            for (int i = 0; i < requests.Count; ++i)
            {
                tasks[i] = Agent.SendForWait(requests[i]).Task;
            }
            Task.WaitAll(tasks);
            foreach (var request in requests)
            {
                if (request.IsTimeout)
                    AddTimeoutCount++;
                else if (request.ResultCode != 0)
                    AddErrorCount++;
            }
            CheckCurrentCount(stepName, concurrent);
        }

        public void RunTrace()
        {
            Agent.SendForWait(new AddCount()).Task.Wait();
            CheckCurrentCount("FirstAddCount", 1);

            ConcurrentAddCount("SimpleConcurrent", 200);
            // 改成真正并发：测试rpc可靠性（重发，重定向）。
            for (int i = 0; i < 20; ++i)
            {
                RandomRaft().RestartNet();
                ConcurrentAddCount($"RestartNetConcurrent_{i}", 1);
            }
            // RandomRaft().RestarRaft();
            // InstallSnapshot;
        }

        private TestRaft RandomRaft()
        {
            var rands = Rafts.Values.ToArray();
            var rand = Util.Random.Instance.Next(rands.Length);
            return rands[rand];
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
            public Raft Raft { get; private set; }
            public TestStateMachine StateMachine { get; private set; }
            public string RaftConfigFileName { get; }
            public string RaftName { get; }

            public void RestartNet()
            {
                Raft.Server.Stop();
                Raft.Server.Start();
            }

            public void RestartRaft()
            {
                Raft?.Server.Stop();

                StateMachine = new TestStateMachine();

                var raftConfig = RaftConfig.Load(RaftConfigFileName);
                raftConfig.AppendEntriesTimeout = 1000;
                raftConfig.LeaderHeartbeatTimer = 2000;
                raftConfig.LeaderLostTimeout = 4000;
                raftConfig.DbHome = Path.Combine(".", RaftName.Replace(':', '_'));
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

            public TestRaft(string raftName, string raftConfigFileName)
            {
                RaftName = raftName;
                RaftConfigFileName = raftConfigFileName;
                RestartRaft();
            }

            private int ProcessAddCount(Zeze.Net.Protocol p)
            {
                var r = p as AddCount;
                lock (StateMachine)
                {
                    StateMachine.AddCountAndWait();
                    r.SendResultCode(0);
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
