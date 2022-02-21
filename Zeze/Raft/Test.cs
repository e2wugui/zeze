using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;
using RocksDbSharp;
using Zeze.Net;

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

        private void LogDump(string db)
        {
            var options = new DbOptions().SetCreateIfMissing(true);
            using var r1 = RocksDb.Open(options, Path.Combine(db, "logs"));
            using var it1 = r1.NewIterator();
            it1.SeekToFirst();
            var StateMachine = new TestStateMachine();
            using var dumpFile = new FileStream(db + ".txt", FileMode.Create);
            while (it1.Valid())
            {
                var l1 = RaftLog.Decode(new Binary(it1.Value()), StateMachine.LogFactory);
                dumpFile.Write(Encoding.UTF8.GetBytes(l1.ToString()));
                dumpFile.Write(Encoding.UTF8.GetBytes("\n"));
                it1.Next();
            }
        }

        public void Run(string command, string[] args)
        {
            Console.WriteLine(command); // 初始化Console，ReadKey死锁问题？
            try
            {
                _Run(command, args);
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("Press [x] Enter To Exit.");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            while (true)
            {
                System.Threading.Thread.Sleep(1000);
            }
        }

        private void _Run(string command, string[] args)
        {
            for (int i = 0; i < args.Length; ++i)
            {
                if (args[i].Equals("-Config"))
                    RaftConfigFileName = args[++i];
            }
            var logTarget = new NLog.Targets.FileTarget(RaftConfigFileName);
            logTarget.Layout = "${longdate} ${ threadid} ${callsite} ${level} ${message} ${exception: format=Message,StackTrace}";
            logTarget.FileName = RaftConfigFileName + ".log";

            var loggingRule = new NLog.Config.LoggingRule("*", NLog.LogLevel.Trace, logTarget);
            NLog.LogManager.Configuration.AddTarget("LogFile", logTarget);
            NLog.LogManager.Configuration.LoggingRules.Add(loggingRule);
            NLog.LogManager.ReconfigExistingLoggers();

            logger.Debug("Start.");
            var raftConfigStart = RaftConfig.Load(RaftConfigFileName);

            if (command.Equals("RaftDump"))
            {
                foreach (var node in raftConfigStart.Nodes.Values)
                {
                    LogDump($"{node.Host}_{node.Port}");
                }
                return;
            }

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

            Util.Task.LogIgnoreExceptionNames.TryAdd(typeof(RaftRetryException).FullName, typeof(RaftRetryException).FullName);
            Util.Task.Run(() =>
            {
                while (true)
                {
                    try
                    {
                        Console.ReadKey();
                        var sb = new StringBuilder();
                        sb.Append($"----------------------------------{raftConfigStart.XmlFileName}-------------------------------------\n");
                        foreach (var r in Rafts.Values)
                        {
                            var l = r.Raft?.LogSequence;
                            sb.Append($"{r.RaftName} CommitIndex={l?.CommitIndex} LastApplied={l?.LastApplied} LastIndex={l?.LastIndex} Count={r.StateMachine?.Count}");
                            sb.Append("\n");
                        }
                        foreach (var f in FailActions)
                        {
                            sb.Append($"{f.Name} TestCount={f.Count}");
                            sb.Append("\n");
                        }
                        sb.Append($"-----------------------------------{raftConfigStart.XmlFileName}------------------------------------\n");
                        Console.WriteLine(sb.ToString());
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex);
                    }
                }
            }, "DumpWorker");
            try
            {
                RunTrace();
            }
            finally
            {
                Agent.Client.Stop();
                foreach (var raft in Rafts.Values)
                {
                    raft.StopRaft();
                }
            }
        }

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private long GetCurrentCount()
        {
            while (true)
            {
                try
                {
                    var r = new GetCount();
                    Agent.SendForWait(r).Task.Wait();
                    if (false == r.IsTimeout && r.ResultCode == 0)
                        return r.Result.Count;
                }
                catch (Exception)
                {
                }
            }
        }

        private Util.AtomicLong ExpectCount = new Util.AtomicLong();
        private Dictionary<long, long> Errors { get; } = new Dictionary<long, long>();

        private void ErrorsAdd(long resultCode)
        {
            if (0 == resultCode)
                return;
            if (Errors.ContainsKey(resultCode))
                Errors[resultCode] = Errors[resultCode] + 1;
            else
                Errors[resultCode] = 1;
        }

        private long ErrorsSum()
        {
            long sum = 0;
            foreach (var e in Errors.Values)
            {
                sum += e;
            }
            return sum;
        }

        private string GetErrorsString()
        {
            var sb = new StringBuilder();
            ByteBuffer.BuildString(sb, Errors);
            return sb.ToString();
        }

        private bool CheckCurrentCount(string stepName, bool resetExpectCount = true)
        {
            var CurrentCount = GetCurrentCount();
            if (CurrentCount != ExpectCount.Get())
            {
                var report = new StringBuilder();
                var level = ExpectCount.Get() != CurrentCount + ErrorsSum()
                    ? NLog.LogLevel.Fatal : NLog.LogLevel.Info;
                report.Append($"{Environment.NewLine}-------------------------------------------");
                report.Append($"{Environment.NewLine}{stepName},");
                report.Append($"Expect={ExpectCount.Get()},");
                report.Append($"Now={CurrentCount},");
                report.Append($"Errors={GetErrorsString()}");
                report.Append($"{Environment.NewLine}-------------------------------------------");
                logger.Log(level, report.ToString());

                if (resetExpectCount)
                {
                    ExpectCount.GetAndSet(CurrentCount); // 下一个测试重新开始。
                    Errors.Clear();
                }
                return level == NLog.LogLevel.Info;
            }
            return true;
        }

        private int ConcurrentAddCount(string stepName, int concurrent)
        {
            var requests = new List<AddCount>();
            var tasks = new List<Task>();
            for (int i = 0; i < concurrent; ++i)
            {
                try
                {
                    var req = new AddCount();
                    tasks.Add(Agent.SendForWait(req).Task);
                    requests.Add(req);
                }
                catch (Exception)
                {
                    //发送错误不统计。ErrorsAdd(Procedure.ErrorSendFail);
                }
                //logger.Debug("+++++++++ REQUEST {0} {1}", stepName, requests[i]);
            }
            try
            {
                Task.WaitAll(tasks.ToArray());
            }
            catch (AggregateException ex)
            {
                // 这里只会发生超时错误RpcTimeoutException。
                // 后面会处理每个requests，这里不做处理了。
                logger.Warn(ex);
            }
            foreach (var request in requests)
            {
                //logger.Debug("--------- RESPONSE {0} {1}", stepName, request);
                if (request.IsTimeout)
                {
                    ErrorsAdd(Procedure.Timeout);
                }
                else
                {
                    ErrorsAdd(request.ResultCode);
                }
            }
            return tasks.Count;
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
            ExpectCount.AddAndGet(ConcurrentAddCount(testname, count));
            CheckCurrentCount(testname);
        }

        public void RunTrace()
        {
            // 基本测试
            logger.Debug("基本测试");
            Agent.SendForWait(new AddCount()).Task.Wait();
            ExpectCount.IncrementAndGet();
            CheckCurrentCount("TestAddCount");

            // 基本并发请求
            SetLogLevel(NLog.LogLevel.Info);
            TestConcurrent("TestConcurrent", 200);

            SetLogLevel(NLog.LogLevel.Trace);

            // 普通节点重启网络一。
            logger.Debug("普通节点重启网络一");
            var NotLeaders = GetNodeNotLeaders();
            if (NotLeaders.Count > 0)
            {
                NotLeaders[0].RestartNet();
            }
            TestConcurrent("TestNormalNodeRestartNet1", 1);

            // 普通节点重启网络二。
            logger.Debug("普通节点重启网络二");
            if (NotLeaders.Count > 1)
            {
                NotLeaders[0].RestartNet();
                NotLeaders[1].RestartNet();
            }
            TestConcurrent("TestNormalNodeRestartNet2", 1);

            // Leader节点重启网络。
            logger.Debug("Leader节点重启网络");
            GetLeader().RestartNet();
            TestConcurrent("TestLeaderNodeRestartNet", 1);

            // Leader节点重启网络，【选举】。
            logger.Debug("Leader节点重启网络，【选举】");
            var leader = GetLeader();
            leader.Raft.Server.Stop();
            Util.Scheduler.Instance.Schedule(
                (ThisTask) => leader.Raft.Server.Start(),
                leader.Raft.RaftConfig.ElectionTimeoutMax);
            TestConcurrent("TestLeaderNodeRestartNet_NewVote", 1);

            // 普通节点重启一。
            logger.Debug("普通节点重启一");
            NotLeaders = GetNodeNotLeaders();
            if (NotLeaders.Count > 0)
            {
                NotLeaders[0].StopRaft();
                NotLeaders[0].StartRaft();
            }
            TestConcurrent("TestNormalNodeRestartRaft1", 1);

            // 普通节点重启二。
            logger.Debug("普通节点重启二");
            if (NotLeaders.Count > 1)
            {
                NotLeaders[0].StopRaft();
                NotLeaders[1].StopRaft();

                NotLeaders[0].StartRaft();
                NotLeaders[1].StartRaft();
            }
            TestConcurrent("TestNormalNodeRestartRaft2", 1);

            // Leader节点重启。
            logger.Debug("Leader节点重启");
            leader = GetLeader();
            var StartDely = leader.Raft.RaftConfig.ElectionTimeoutMax;
            leader.StopRaft();
            Util.Scheduler.Instance.Schedule((ThisTask) => leader.StartRaft(), StartDely);
            TestConcurrent("TestLeaderNodeRestartRaft", 1);

            // Snapshot & Load
            leader = GetLeader();
            leader.Raft.LogSequence.Snapshot();
            leader.StopRaft();
            leader.StartRaft();

            // InstallSnapshot;

            logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

            SetLogLevel(NLog.LogLevel.Info);

            FailActions.Add(new FailAction()
            {
                Name = "RestartNet1",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].RestartNet();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartNet2",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].RestartNet();
                    rafts[1].RestartNet();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartNet3",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].RestartNet();
                    rafts[1].RestartNet();
                    rafts[2].RestartNet();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartLeaderNetForVote",
                Action = () =>
                {
                    while (true)
                    {
                        var leader = GetLeader();
                        if (leader == null)
                        {
                            System.Threading.Thread.Sleep(10); // wait a Leader
                            continue;
                        }
                        leader.Raft.Server.Stop();
                        // delay for vote
                        System.Threading.Thread.Sleep(leader.Raft.RaftConfig.ElectionTimeoutMax);
                        leader.Raft.Server.Start();
                        break;
                    }
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartRaft1",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].StopRaft();
                    rafts[0].StartRaft();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartRaft2",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].StopRaft();
                    rafts[1].StopRaft();

                    rafts[0].StartRaft();
                    rafts[1].StartRaft();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartRaft3",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].StopRaft();
                    rafts[1].StopRaft();
                    rafts[2].StopRaft();

                    rafts[0].StartRaft();
                    rafts[1].StartRaft();
                    rafts[2].StartRaft();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartLeaderRaftForVote",
                Action = () =>
                {
                    while (true)
                    {
                        var leader = GetLeader();
                        if (leader == null)
                        {
                            System.Threading.Thread.Sleep(10); // wait a Leader
                            continue;
                        }
                        var startVoteDelay = leader.Raft.RaftConfig.ElectionTimeoutMax;
                        leader.StopRaft();
                        // delay for vote
                        System.Threading.Thread.Sleep(startVoteDelay);
                        leader.StartRaft();
                        break;
                    }
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "InstallSnapshot",
                Action = () =>
                {
                    foreach (var test in Rafts.Values)
                    {
                        test.Raft?.LogSequence.Snapshot(true);
                        test.StopRaft();
                    }
                    for (int i = 0; i < Rafts.Count; ++i)
                    {
                        Rafts.Values.ElementAt(i).StartRaft(i == 0);
                    }
                }
            });

            // Start Background FailActions
            Util.Task.Run(RandomTriggerFailActions, "RandomTriggerFailActions");
            var testname = "RealConcurrentDoRequest";
            var lastExpectCount = ExpectCount.Get();
            while (true)
            {
                ExpectCount.AddAndGet(ConcurrentAddCount(testname, 20));
                if (ExpectCount.Get() - lastExpectCount > 20 * 5)
                {
                    lastExpectCount = ExpectCount.Get();
                    if (false == Check(testname))
                        break;
                }
            }
            Running = false;
            SetLogLevel(NLog.LogLevel.Debug);
            CheckCurrentCount("Final Check!!!");
        }

        private bool Check(string testname)
        {
            int trycount = 2;
            for (int i = 0; i < trycount; ++i)
            {
                var check = CheckCurrentCount(testname, false);
                logger.Info($"");
                logger.Info($"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                logger.Info($"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                logger.Info($"Check={check} Step={i} ExpectCount={ExpectCount.Get()} Errors={GetErrorsString()}");
                logger.Info($"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                logger.Info($"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                logger.Info($"");
                if (check)
                {
                    return true;
                }
                if (i < trycount - 1)
                    System.Threading.Thread.Sleep(10000);
            }
            return false;
        }

        class FailAction
        {
            public string Name { get; set; }
            public Action Action { get; set; }
            public long Count { get; set; }
            public override string ToString()
            {
                return Name + "=" + Count;
            }
        }
        private List<FailAction> FailActions { get; } = new List<FailAction>();

        private void WaitExpectCountGrow(long growing = 20)
        {
            long oldTaskCount = ExpectCount.Get();
            while (true)
            {
                System.Threading.Thread.Sleep(10);
                if (ExpectCount.Get() - oldTaskCount > growing)
                    break;
            }
        }


        private bool Running { get; set; } = true;

        private void RandomTriggerFailActions()
        {
            while (Running)
            {
                var fa = FailActions[Util.Random.Instance.Next(FailActions.Count)];
                //foreach (var fa in FailActions)
                {
                    logger.Fatal($"___________________________ {fa.Name} _____________________________");
                    try
                    {
                        fa.Action();
                        fa.Count++;
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex, "FailAction {0}", fa.Name);
                        Console.WriteLine("___________________________________________");
                        Console.WriteLine("___________________________________________");
                        Console.WriteLine("___________________________________________");
                        Console.WriteLine("Press [y] Enter To Exit.");
                        Console.WriteLine("___________________________________________");
                        Console.WriteLine("___________________________________________");
                        Console.WriteLine("___________________________________________");
                        foreach (var raft in Rafts.Values)
                        {
                            if (raft.Raft != null && raft.Raft.LogSequence != null)
                            {
                                raft.Raft.IsShutdown = true;
                                raft.Raft.LogSequence.Close();
                            }
                        }
                        NLog.LogManager.Shutdown();
                        System.Diagnostics.Process.GetCurrentProcess().Kill();
                        /*
                        foreach (var raft in Rafts.Values)
                        {
                            raft.StartRaft(); // error recover
                        }
                        // */
                    }
                    // 等待失败的节点恢复正常并且服务了一些请求。
                    // 由于一个follower失败时，请求处理是能持续进行的，这个等待可能不够。
                    WaitExpectCountGrow(110);
                }
            }
            var sb = new StringBuilder();
            ByteBuffer.BuildString(sb, FailActions);
            logger.Fatal(sb.ToString());
        }

        private TestRaft GetLeader()
        { 
            while (true)
            {
                foreach (var raft in Rafts.Values)
                {
                    if (null == raft.Raft)
                        continue;

                    if (raft.Raft.IsLeader)
                        return raft;
                }
                System.Threading.Thread.Sleep(1000);
            }
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

        
        private TestRaft[] ShuffleRafts()
        {
            return Util.Random.Shuffle(Rafts.Values.ToArray());
        }

        public sealed class AddCount : RaftRpc<EmptyBean, EmptyBean>
        {
            public readonly static int ProtocolId_ = Bean.Hash32(typeof(AddCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;

            public AddCount()
            { 
            }
        }

        public sealed class GetCountResult : Bean
        {
            public long Count { get; set; }

            public override void Decode(ByteBuffer bb)
            {
                Count = bb.ReadLong();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteLong(Count);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }
        }

        public sealed class GetCount : RaftRpc<EmptyBean, GetCountResult>
        {
            public readonly static int ProtocolId_ = Bean.Hash32(typeof(GetCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;

            public GetCount()
            { 
            }
        }

        public class TestStateMachine : StateMachine
        {
            public long Count { get; set; }

            public void AddCountAndWait(IRaftRpc req)
            {
                Raft.AppendLog(new AddCount(req));
            }

            public sealed class AddCount : Log
            {
                public AddCount(IRaftRpc req)
                    : base(req)
                {

                }

                public override void Apply(RaftLog holder, StateMachine stateMachine)
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

            public override void LoadSnapshot(string path)
            {
                lock (Raft)
                {
                    using var file = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
                    var bytes = new byte[1024];
                    int rc = file.Read(bytes);
                    var bb = ByteBuffer.Wrap(bytes, 0, rc);
                    Count = bb.ReadLong();
                }
            }

            // 这里没有处理重入，调用者需要保证。
            public override bool Snapshot(string path, out long LastIncludedIndex, out long LastIncludedTerm)
            {
                LastIncludedIndex = 0;
                LastIncludedTerm = 0;

                lock (Raft)
                {
                    if (null != Raft.LogSequence)
                    {
                        using var file = new FileStream(path, FileMode.Create);
                        var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
                        LastIncludedIndex = lastAppliedLog.Index;
                        LastIncludedTerm = lastAppliedLog.Term;
                        var bb = ByteBuffer.Allocate();
                        bb.WriteLong(Count);
                        file.Write(bb.Bytes, bb.ReadIndex, bb.Size);
                        file.Close();
                        Raft.LogSequence.CommitSnapshot(path, LastIncludedIndex);
                    }
                }
                return true;
            }

            public TestStateMachine()
            {
                AddFactory(new AddCount(null).TypeId, () => new AddCount(null));
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
                Raft?.Server.Stop();
                Raft?.Server.Start();
            }

            Util.SchedulerTask SnapshotTimer;

            public void StopRaft()
            {
                lock (this)
                {
                    logger.Debug("Raft {0} Stop ...", RaftName);
                    // 在同一个进程中，没法模拟进程退出，
                    // 此时RocksDb应该需要关闭，否则重启会失败吧。
                    Raft?.Shutdown();
                    Raft = null;
                }
            }

            public void StartRaft(bool resetLog = false)
            {
                lock (this)
                {
                    if (null != Raft)
                    {
                        Raft.Server.Start();
                        return;
                    }
                    logger.Debug("Raft {0} Start ...", RaftName);
                    StateMachine = new TestStateMachine();

                    var raftConfig = RaftConfig.Load(RaftConfigFileName);
                    raftConfig.SnapshotMinLogCount = 10;
                    raftConfig.DbHome = Path.Combine(".", RaftName.Replace(':', '_'));
                    if (resetLog)
                    {
                        logger.Warn("------------------------------------------------");
                        logger.Warn($"- Reset Log {raftConfig.DbHome} -");
                        logger.Warn("------------------------------------------------");
                        if (Directory.Exists(raftConfig.DbHome))
                            Directory.Delete(raftConfig.DbHome, true);
                    }
                    Directory.CreateDirectory(raftConfig.DbHome);

                    Raft = new Raft(StateMachine, RaftName, raftConfig);
                    Raft.LogSequence.WriteOptions.SetSync(false);
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
            }

            public TestRaft(string raftName, string raftConfigFileName)
            {
                RaftName = raftName;
                RaftConfigFileName = raftConfigFileName;
                StartRaft(true);
                SnapshotTimer = Util.Scheduler.Instance.Schedule(
                    (ThisTask) => Raft?.LogSequence?.Snapshot(false), 1 * 60 * 1000, 1 * 60 * 1000);
            }

            private long ProcessAddCount(Zeze.Net.Protocol p)
            {
                if (false == Raft.IsLeader)
                    return Procedure.RaftRetry; // fast fail

                var r = p as AddCount;
                lock (StateMachine)
                {
                    StateMachine.AddCountAndWait(r);
                    r.SendResultCode(0);
                }
                return Procedure.Success;
            }

            private long ProcessGetCount(Zeze.Net.Protocol p)
            {
                var r = p as GetCount;
                lock (StateMachine)
                {
                    r.Result.Count = StateMachine.Count;
                    r.SendResult();
                }
                return Procedure.Success;
            }
        }

    }
}
