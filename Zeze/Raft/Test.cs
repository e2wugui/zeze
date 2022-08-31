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
using Zeze.Util;

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

        public static void LogDump(string db)
        {            
            var options = new DbOptions().SetCreateIfMissing(true);
            using var r1 = RocksDb.Open(options, Path.Combine(db, "logs"));
            using var it1 = r1.NewIterator();
            it1.SeekToFirst();

            var StateMachine = new TestStateMachine();
            var snapshot = Path.Combine(db, LogSequence.SnapshotFileName);
            if (File.Exists(snapshot))
                StateMachine.LoadSnapshotInternal(snapshot);
            using var dumpFile = new FileStream(db + ".txt", FileMode.Create);
            dumpFile.Write(Encoding.UTF8.GetBytes($"SnapshotCount = {StateMachine.Count}\n"));
            while (it1.Valid())
            {
                var l1 = RaftLog.Decode(new Binary(it1.Value()), StateMachine.LogFactory);
                dumpFile.Write(Encoding.UTF8.GetBytes(l1.ToString()));
                dumpFile.Write(Encoding.UTF8.GetBytes("\n"));
                it1.Next();
            }
        }

        public async Task Run(string command, string[] args)
        {
            Console.WriteLine(command); // 初始化Console，ReadKey死锁问题？
            try
            {
                await RunPrivate(command, args);
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("Press [Ctrl+c] Enter To Exit.");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            Console.WriteLine("___________________________________________");
            while (true)
            {
                System.Threading.Thread.Sleep(1000);
            }
        }

        private async Task RunPrivate(string command, string[] args)
        {
            for (int i = 0; i < args.Length; ++i)
            {
                if (args[i].Equals("-Config"))
                    RaftConfigFileName = args[++i];
            }
            var logTarget = new NLog.Targets.FileTarget(RaftConfigFileName)
            {
                Layout = "${longdate} ${ threadid} ${callsite} ${level} ${message} ${exception: format=Message,StackTrace}",
                FileName = RaftConfigFileName + ".log"
            };

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
                File.Copy(raftConfigStart.XmlFileName, confName);
                Rafts.GetOrAdd(node.Value.Name, (_) => new TestRaft(node.Value.Name, confName));
            }

            foreach (var raft in Rafts.Values)
            {
                await raft.StartRaft(true);
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

            _ = Task.Run(() =>
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
                            sb.Append('\n');
                        }
                        foreach (var f in FailActions)
                        {
                            sb.Append($"{f.Name} TestCount={f.Count}");
                            sb.Append('\n');
                        }
                        sb.Append($"-----------------------------------{raftConfigStart.XmlFileName}------------------------------------\n");
                        Console.WriteLine(sb.ToString());
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex);
                    }
                }
            });
            try
            {
                await RunTrace();
            }
            finally
            {
                Agent.Client.Stop();
                SnapshotTimer?.Cancel();
                foreach (var raft in Rafts.Values)
                {
                    raft.StopRaft().Wait();
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
                    Agent.SendAsync(r).Wait();
                    if (false == r.IsTimeout && r.ResultCode == 0)
                        return r.Result.Count;
                }
                catch (Exception)
                {
                }
            }
        }

        private readonly Util.AtomicLong ExpectCount = new();
        private Dictionary<long, long> Errors { get; } = new();

        private void ErrorsAdd(long resultCode)
        {
            if (0 == resultCode)
                return;
            if (resultCode == ResultCode.RaftApplied)
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
                    tasks.Add(Agent.SendAsync(req));
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
                logger.Debug("--------- RESPONSE {0} {1}", stepName, request);
                if (request.IsTimeout)
                {
                    ErrorsAdd(ResultCode.Timeout);
                }
                else
                {
                    ErrorsAdd(request.ResultCode);
                }
            }
            return tasks.Count;
        }

        private static void SetLogLevel(NLog.LogLevel level)
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

        Util.SchedulerTask SnapshotTimer;

        private void RandomSnapshotTimer(Util.SchedulerTask ThisTask)
        {
            var randindex = Util.Random.Instance.Next(Rafts.Count);
            var index = 0;

            foreach (var test in Rafts.Values)
            {
                if (index++ == randindex)
                {
                    test.Raft?.LogSequence.Snapshot();
                    return;
                }
            }
        }

        public async Task RunTrace()
        {
            // 基本测试
            logger.Debug("基本测试");
            Agent.SendAsync(new AddCount()).Wait();
            ExpectCount.IncrementAndGet();
            CheckCurrentCount("TestAddCount");

            // 基本并发请求
            logger.Debug("基本并发请求");
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
            await Task.Delay(leader.Raft.RaftConfig.ElectionTimeoutMax);
            leader.Raft.Server.Start();
            TestConcurrent("TestLeaderNodeRestartNet_NewVote", 1);

            // 普通节点重启一。
            logger.Debug("普通节点重启一");
            NotLeaders = GetNodeNotLeaders();
            if (NotLeaders.Count > 0)
            {
                NotLeaders[0].StopRaft().Wait();
                NotLeaders[0].StartRaft().Wait();
            }
            TestConcurrent("TestNormalNodeRestartRaft1", 1);

            // 普通节点重启二。
            logger.Debug("普通节点重启二");
            if (NotLeaders.Count > 1)
            {
                NotLeaders[0].StopRaft().Wait();
                NotLeaders[1].StopRaft().Wait();

                NotLeaders[0].StartRaft().Wait();
                NotLeaders[1].StartRaft().Wait();
            }
            TestConcurrent("TestNormalNodeRestartRaft2", 1);

            // Leader节点重启。
            logger.Debug("Leader节点重启");
            leader = GetLeader();
            var StartDely = leader.Raft.RaftConfig.ElectionTimeoutMax;
            leader.StopRaft().Wait();
            Util.Scheduler.Schedule((ThisTask) => leader.StartRaft(), StartDely);
            TestConcurrent("TestLeaderNodeRestartRaft", 1);

            // Snapshot & Load
            leader = GetLeader();
            await leader.Raft.LogSequence.Snapshot();
            leader.StopRaft().Wait();
            leader.StartRaft().Wait();

            // InstallSnapshot;

            logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

            SnapshotTimer = Util.Scheduler.Schedule(RandomSnapshotTimer, 1 * 60 * 1000, 1 * 60 * 1000);

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
                    rafts[0].StopRaft().Wait();
                    rafts[0].StartRaft().Wait();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartRaft2",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].StopRaft().Wait();
                    rafts[1].StopRaft().Wait();

                    rafts[0].StartRaft().Wait();
                    rafts[1].StartRaft().Wait();
                }
            });
            FailActions.Add(new FailAction()
            {
                Name = "RestartRaft3",
                Action = () =>
                {
                    var rafts = ShuffleRafts();
                    rafts[0].StopRaft().Wait();
                    rafts[1].StopRaft().Wait();
                    rafts[2].StopRaft().Wait();

                    rafts[0].StartRaft().Wait();
                    rafts[1].StartRaft().Wait();
                    rafts[2].StartRaft().Wait();
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
                        leader.StopRaft().Wait();
                        // delay for vote
                        System.Threading.Thread.Sleep(startVoteDelay);
                        leader.StartRaft().Wait();
                        break;
                    }
                }
            });
            var InstallSnapshotCleanNode = Rafts.Values.ElementAt(0); // 记住，否则Release版本，每次返回值可能会变。
            FailActions.Add(new FailAction()
            {
                Name = "InstallSnapshot Clean One Node Data",
                Action = () =>
                {
                    foreach (var test in Rafts.Values)
                    {
                        test.StopRaft().Wait(); // 先停止，这样才能强制启动安装。
                        test.StartRaft(test == InstallSnapshotCleanNode).Wait();
                    }
                }
            });

            // Start Background FailActions
            _ = Task.Run(RandomTriggerFailActions);
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

        public sealed class AddCount : RaftRpc<EmptyBean, CountResult>
        {
            public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(AddCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;

            public AddCount()
            { 
            }
        }

        public sealed class CountResult : Bean
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

            protected override void ResetChildrenRootInfo()
            {
                throw new NotImplementedException();
            }

            public override string ToString()
            {
                return $"Count={Count}";
            }
        }

        public sealed class GetCount : RaftRpc<EmptyBean, CountResult>
        {
            public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(GetCount).FullName);

            public override int ModuleId => 0;
            public override int ProtocolId => ProtocolId_;

            public GetCount()
            { 
            }
        }

        public class TestStateMachine : StateMachine
        {
            public long Count { get; set; }

            public async Task AddCountAndWait(Test.AddCount req)
            {
                req.Result.Count = Count;
                await Raft.AppendLog(new AddCount(req), req.Result);
            }

            public sealed class AddCount : Log
            {
                public AddCount(IRaftRpc req)
                    : base(req)
                {

                }

                public override Task Apply(RaftLog holder, StateMachine stateMachine)
                {
                    (stateMachine as TestStateMachine).Count += 1;
                    return Task.CompletedTask;
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

            internal void LoadSnapshotInternal(string path)
            {
                using var file = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
                var bytes = new byte[1024];
                int rc = file.Read(bytes);
                var bb = ByteBuffer.Wrap(bytes, 0, rc);
                Count = bb.ReadLong();
            }

            public override Task LoadSnapshot(string path)
            {
                LoadSnapshotInternal(path);
                logger.Info($"{Raft.Name} LoadSnapshot Count={Count}");
                return Task.CompletedTask;
            }

            // 这里没有处理重入，调用者需要保证。
            public override async Task<(bool, long, long)> Snapshot(string path)
            {
                long LastIncludedTerm = 0;
                long LastIncludedIndex = 0;

                if (null != Raft.LogSequence)
                {
                    var lastAppliedLog = await Raft.LogSequence.LastAppliedLogTermIndex();
                    LastIncludedIndex = lastAppliedLog.Index;
                    LastIncludedTerm = lastAppliedLog.Term;
                    var bb = ByteBuffer.Allocate();
                    logger.Info($"{Raft.Name} Snapshot Count={Count}");
                    bb.WriteLong(Count);
                    using var file = new FileStream(path, FileMode.Create);
                    file.Write(bb.Bytes, bb.ReadIndex, bb.Size);
                    file.Close();
                    await Raft.LogSequence.CommitSnapshot(path, LastIncludedIndex);
                }

                return (true, LastIncludedTerm, LastIncludedIndex);
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

            public async Task StopRaft()
            {
                logger.Debug("Raft {0} Stop ...", RaftName);
                // 在同一个进程中，没法模拟进程退出，
                // 此时RocksDb应该需要关闭，否则重启会失败吧。
                await Util.Mission.AwaitNullableTask(Raft?.Shutdown());
                Raft = null;
            }

            public async Task StartRaft(bool resetLog = false)
            {
                if (null != Raft)
                {
                    Raft.Server.Start();
                    return;
                }
                logger.Debug("Raft {0} Start ...", RaftName);
                StateMachine = new TestStateMachine();

                var raftConfig = RaftConfig.Load(RaftConfigFileName);
                raftConfig.UniqueRequestExpiredDays = 1;
                raftConfig.DbHome = Path.Combine(".", RaftName.Replace(':', '_'));
                if (resetLog)
                {
                    logger.Warn("------------------------------------------------");
                    logger.Warn($"- Reset Log {raftConfig.DbHome} -");
                    logger.Warn("------------------------------------------------");
                    // 只删除日志相关数据库。保留重复请求数据库。
                    var logsdir = Path.Combine(raftConfig.DbHome, "logs");
                    if (Directory.Exists(logsdir))
                        Util.FileSystem.DeleteDirectory(logsdir);
                    var raftsdir = Path.Combine(raftConfig.DbHome, "rafts");
                    if (Directory.Exists(raftsdir))
                        Util.FileSystem.DeleteDirectory(raftsdir);
                    var snapshotFile = Path.Combine(raftConfig.DbHome, "snapshot.dat");
                    if (File.Exists(snapshotFile))
                        File.Delete(snapshotFile);
                }
                Util.FileSystem.CreateDirectory(raftConfig.DbHome);

                Raft = await new Raft(StateMachine).OpenAsync(RaftName, raftConfig);
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

            public TestRaft(string raftName, string raftConfigFileName)
            {
                RaftName = raftName;
                RaftConfigFileName = raftConfigFileName;
            }

            private async Task<long> ProcessAddCount(Zeze.Net.Protocol p)
            {
                if (false == Raft.IsLeader)
                    return ResultCode.RaftRetry; // fast fail

                var r = p as AddCount;
                await StateMachine.AddCountAndWait(r);
                r.SendResultCode(0);
                return ResultCode.Success;
            }

            private Task<long> ProcessGetCount(Zeze.Net.Protocol p)
            {
                var r = p as GetCount;

                r.Result.Count = StateMachine.Count;
                r.SendResult();

                return Task.FromResult(ResultCode.Success);
            }
        }

    }
}
