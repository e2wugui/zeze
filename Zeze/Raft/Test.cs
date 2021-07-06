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

            Agent = new Agent("Zeze.Raft.Agent.Test", raftConfigStart);
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
                raft.StopRaft();
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

        private int ExpectCount { get; set; }
        private Dictionary<int, int> Errors { get; } = new Dictionary<int, int>();

        private void ErrorsAdd(int resultCode)
        {
            if (0 == resultCode)
                return;
            if (Errors.ContainsKey(resultCode))
                Errors[resultCode] = Errors[resultCode] + 1;
            else
                Errors[resultCode] = 1;
        }

        private int ErrorsSum()
        {
            int sum = 0;
            foreach (var e in Errors.Values)
            {
                sum += e;
            }
            return sum;
        }

        private void CheckCurrentCount(string stepName)
        {
            var CurrentCount = GetCurrentCount();
            if (CurrentCount != ExpectCount)
            {
                var report = new StringBuilder();
                var level = ExpectCount != CurrentCount + ErrorsSum()
                    ? "FATAL" : "HAS ERROR";
                report.Append($"{Environment.NewLine}_______________{level}____________________________");
                report.Append($"{Environment.NewLine}{stepName},");
                report.Append($"Expect={ExpectCount},");
                report.Append($"Now={CurrentCount},");
                report.Append($"Errors=");
                ByteBuffer.BuildString(report, Errors);
                report.Append($"{Environment.NewLine}_______________{level}____________________________");
                logger.Fatal(report.ToString());

                ExpectCount = CurrentCount; // 下一个测试重新开始。
                Errors.Clear();
            }
        }

        private void ConcurrentAddCount(string stepName, int concurrent)
        {
            var requests = new List<AddCount>();
            for (int i = 0; i < concurrent; ++i)
                requests.Add(new AddCount());

            Task[] tasks = new Task[concurrent];
            for (int i = 0; i < requests.Count; ++i)
            {
                tasks[i] = Agent.SendForWait(requests[i]).Task;
                //logger.Debug("+++++++++ REQUEST {0} {1}", stepName, requests[i]);
            }
            Task.WaitAll(tasks);
            foreach (var request in requests)
            {
                //logger.Debug("--------- RESPONSE {0} {1}", stepName, request);
                if (request.IsTimeout)
                    ErrorsAdd(Procedure.Timeout);
                else
                    ErrorsAdd(request.ResultCode);
            }
        }

        private void SetLogLevel(NLog.LogLevel level)
        {
            NLog.LogManager.GlobalThreshold = level;
            /*
            foreach (var rule in NLog.LogManager.Configuration.LoggingRules)
            {
                //Console.WriteLine($"================ SetLoggingLevels {rule.RuleName}");
                rule.DisableLoggingForLevels(NLog.LogLevel.Trace, NLog.LogLevel.Fatal);
                rule.EnableLoggingForLevels(level, NLog.LogLevel.Fatal);
                //rule.SetLoggingLevels(level, NLog.LogLevel.Fatal);
            }
            */
        }

        private void TestConcurrent(string testname, int count)
        {
            ConcurrentAddCount(testname, count);
            ExpectCount += count;
            CheckCurrentCount(testname);
        }

        public void RunTrace()
        {
            // 基本测试
            Agent.SendForWait(new AddCount()).Task.Wait();
            ExpectCount += 1;
            CheckCurrentCount("TestAddCount");

            // 基本并发请求
            SetLogLevel(NLog.LogLevel.Info);
            TestConcurrent("TestConcurrent", 200);

            SetLogLevel(NLog.LogLevel.Trace);

            // 普通节点重启网络一。
            var NotLeaders = GetNodeNotLeaders();
            if (NotLeaders.Count > 0)
            {
                NotLeaders[0].RestartNet();
            }
            TestConcurrent("TestNormalNodeRestartNet1", 1);

            // 普通节点重启网络二。
            if (NotLeaders.Count > 1)
            {
                NotLeaders[0].RestartNet();
                NotLeaders[1].RestartNet();
            }
            TestConcurrent("TestNormalNodeRestartNet2", 1);

            // Leader节点重启网络。
            GetLeader().RestartNet();
            TestConcurrent("TestLeaderNodeRestartNet", 1);

            // Leader节点重启网络，【选举】。
            var leader = GetLeader();
            leader.Raft.Server.Stop();
            Util.Scheduler.Instance.Schedule((ThisTask) => leader.Raft.Server.Start(),
                leader.Raft.RaftConfig.LeaderLostTimeout + 2000);
            TestConcurrent("TestLeaderNodeRestartNet_NewVote", 1);

            // 普通节点重启一。
            NotLeaders = GetNodeNotLeaders();
            if (NotLeaders.Count > 0)
            {
                NotLeaders[0].StopRaft();
                NotLeaders[0].StartRaft();
            }
            TestConcurrent("TestNormalNodeRestartRaft1", 1);

            // 普通节点重启网络二。
            if (NotLeaders.Count > 1)
            {
                NotLeaders[0].StopRaft();
                NotLeaders[1].StopRaft();

                NotLeaders[0].StartRaft();
                NotLeaders[1].StartRaft();
            }
            TestConcurrent("TestNormalNodeRestartRaft2", 1);

            // Leader节点重启。
            leader = GetLeader();
            var StartDely = leader.Raft.RaftConfig.LeaderLostTimeout + 2000;
            leader.StopRaft();
            Util.Scheduler.Instance.Schedule((ThisTask) => leader.StartRaft(), StartDely);
            TestConcurrent("TestLeaderNodeRestartRaft", 1);

            // RandomRaft().RestarRaft();
            // InstallSnapshot;

            SetLogLevel(NLog.LogLevel.Info);
        }

        private TestRaft GetLeader()
        { 
            foreach (var raft in Rafts.Values)
            {
                if (raft.Raft.IsLeader)
                    return raft;
            }
            return null;
        }

        private List<TestRaft> GetNodeNotLeaders()
        {
            var NotLeader = new List<TestRaft>();
            foreach (var raft in Rafts.Values)
            {
                if (!raft.Raft.IsLeader)
                    NotLeader.Add(raft);
            }
            return NotLeader;
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

            public void AddCountAndWait(long requestId)
            {
                Raft.AppendLog(new AddCount(requestId));
            }

            public sealed class AddCount : Log
            {
                public AddCount(long requestId) : base(requestId)
                {

                }
                public override void Apply(StateMachine stateMachine)
                {
                    (stateMachine as TestStateMachine).Count += 1;
                }

                public override void Decode(ByteBuffer bb)
                {
                    base.Decode(bb);
                }

                public override void Encode(ByteBuffer bb)
                {
                    base.Encode(bb);
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
                AddFactory(new AddCount(0).TypeId, () => new AddCount(0));
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
                logger.Debug("Raft.Net {0} Restart ...", RaftName);
                Raft.Server.Stop();
                Raft.Server.Start();
            }

            public void StopRaft()
            {
                logger.Debug("Raft {0} Stop ...", RaftName);
                Raft?.Server.Stop();

                // 在同一个进程中，没法模拟进程退出，
                // 此时RocksDb应该需要关闭，否则重启回失败吧。
                Raft?.Close();
                Raft = null;
            }

            public void StartRaft(bool resetLog = false)
            {
                if (null != Raft)
                    throw new Exception($"Raft {RaftName} Has Started.");
                logger.Debug("Raft {0} Start ...", RaftName);
                StateMachine = new TestStateMachine();

                var raftConfig = RaftConfig.Load(RaftConfigFileName);
                raftConfig.AppendEntriesTimeout = 1000;
                raftConfig.LeaderHeartbeatTimer = 1500;
                raftConfig.LeaderLostTimeout = 2000;
                raftConfig.DbHome = Path.Combine(".", RaftName.Replace(':', '_'));
                if (resetLog)
                {
                    if (Directory.Exists(raftConfig.DbHome))
                        Directory.Delete(raftConfig.DbHome, true);
                }
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
                Raft.Server.Start();
            }

            public TestRaft(string raftName, string raftConfigFileName)
            {
                RaftName = raftName;
                RaftConfigFileName = raftConfigFileName;
                StartRaft(true);
            }

            private int ProcessAddCount(Zeze.Net.Protocol p)
            {
                var r = p as AddCount;
                lock (StateMachine)
                {
                    StateMachine.AddCountAndWait(r.UniqueRequestId);
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
