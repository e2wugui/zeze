using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Raft
{
    public class Test
    {
        public enum Mode
        {
            Trace,
            Batch,
        }

        public Mode TestMode { get; set; } = Mode.Trace;
        public int BatchCount { get; set; } = 1000000;
        public string RaftConfigFileName { get; set; }

        private void SetMode(string mode)
        {
            switch (mode.ToLower())
            {
                case "trace":
                    TestMode = Mode.Trace;
                    break;

                case "batch":
                    TestMode = Mode.Batch;
                    break;
            }
        }

        public Test(string[] args)
        {
            Console.WriteLine("-mode [trace|batch] -BatchCount [1000000]");
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-mode":
                        SetMode(args[++i]);
                        break;

                    case "-BatchCount":
                        BatchCount = int.Parse(args[++i]);
                        break;

                    case "-RaftConfig":
                        RaftConfigFileName = args[++i];
                        break;
                }
            }
        }

        public static void Run(string [] args)
        {
            new Test(args).Run();
        }

        private ConcurrentDictionary<string, TestRaft> Rafts { get; }
            = new ConcurrentDictionary<string, TestRaft>();

        public void Run()
        {
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

            switch (TestMode)
            {
                case Mode.Trace:
                    RunTrace();
                    break;

                case Mode.Batch:
                    RunBatch();
                    break;
            }
        }

        public void RunTrace()
        { 
        }

        public void RunBatch()
        { 
        }

        public sealed class SetCount : Zeze.Net.Rpc<EmptyBean, EmptyBean>
        {
            public readonly static int ProtocolId_ = Bean.Hash16(typeof(SetCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;
        }

        public class TestStateMachine : StateMachine
        {
            public int Count { get; set; }

            public void SetCountAndWait(int count)
            {
                Raft.AppendLog(new SetCount() { Count = count });
            }

            public sealed class SetCount : Log
            {
                public int Count { get; set; }

                public override void Apply(StateMachine stateMachine)
                {
                    (stateMachine as TestStateMachine).Count = Count;
                }

                public override void Decode(ByteBuffer bb)
                {
                    Count = bb.ReadInt();
                }

                public override void Encode(ByteBuffer bb)
                {
                    bb.WriteInt(Count);
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
                lock (Raft)
                {
                    var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
                    LastIncludedIndex = lastAppliedLog.Index;
                    LastIncludedTerm = lastAppliedLog.Term;
                    var bb = ByteBuffer.Allocate();
                    bb.WriteInt(Count);
                    file.Write(bb.Bytes, bb.ReadIndex, bb.Size);
                }
                Raft.LogSequence.RemoveLogBefore(LastIncludedIndex);
                return true;
            }

            public TestStateMachine()
            {
                AddFactory(new SetCount().TypeId, () => new SetCount());
            }
        }

        public class TestRaft
        {
            public Raft Raft { get; }
            public TestStateMachine StateMachine { get; }

            public TestRaft(string RaftName, string RaftConfigFileName)
            {
                StateMachine = new TestStateMachine();
                Raft = new Raft(StateMachine, RaftName, RaftConfig.Load(RaftConfigFileName));

                Raft.Server.AddFactoryHandle(
                    new SetCount().TypeId,
                    new Net.Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new SetCount(),
                        Handle = ProcessSetCount,
                    });
            }

            private int ProcessSetCount(Zeze.Net.Protocol p)
            {
                var r = p as SetCount;
                StateMachine.SetCountAndWait(r.ResultCode);
                r.SendResultCode(r.ResultCode);
                return Procedure.Success;
            }
        }

    }
}
