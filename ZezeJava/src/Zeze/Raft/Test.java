package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Test {
	private String RaftConfigFileName = "raft.xml";
	public final String getRaftConfigFileName() {
		return RaftConfigFileName;
	}
	public final void setRaftConfigFileName(String value) {
		RaftConfigFileName = value;
	}

	public Test() {
	}

	private java.util.concurrent.ConcurrentHashMap<String, TestRaft> Rafts = new java.util.concurrent.ConcurrentHashMap<String, TestRaft> ();
	private java.util.concurrent.ConcurrentHashMap<String, TestRaft> getRafts() {
		return Rafts;
	}

	private Agent Agent;
	private Agent getAgent() {
		return Agent;
	}
	private void setAgent(Agent value) {
		Agent = value;
	}

	public final void Run() {
		logger.Debug("Start.");
		var raftConfigStart = RaftConfig.Load(getRaftConfigFileName());
		for (var node : raftConfigStart.getNodes()) {
			// every node need a private config-file.
			var confName = System.IO.Path.GetTempFileName() + ".xml";
			Files.copy(Paths.get(raftConfigStart.getXmlFileName()), Paths.get(confName), StandardCopyOption.COPY_ATTRIBUTES);
			getRafts().putIfAbsent(node.Value.Name, (_) -> new TestRaft(node.Value.Name, confName));
		}

		for (var raft : getRafts().values()) {
			raft.Raft.Server.Start();
		}

		setAgent(new Agent("Zeze.Raft.Agent.Test", raftConfigStart));
		getAgent().getClient().AddFactoryHandle((new AddCount()).getTypeId(), new Net.Service.ProtocolFactoryHandle() {Factory = () -> new AddCount()});
		getAgent().getClient().AddFactoryHandle((new GetCount()).getTypeId(), new Net.Service.ProtocolFactoryHandle() {Factory = () -> new GetCount()});
		getAgent().getClient().Start();
		RunTrace();
		getAgent().getClient().Stop();
		for (var raft : getRafts().values()) {
			raft.StopRaft();
		}
		logger.Debug("End.");
	}

	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private int GetCurrentCount() {
		var r = new GetCount();
		while (true) {
			try {
				getAgent().SendForWait(r).Task.Wait();
				return r.getResultCode();
			}
			catch (RuntimeException e) {
			}
		}
	}

	private int ExpectCount;
	private int getExpectCount() {
		return ExpectCount;
	}
	private void setExpectCount(int value) {
		ExpectCount = value;
	}
	private HashMap<Integer, Integer> Errors = new HashMap<Integer, Integer> ();
	private HashMap<Integer, Integer> getErrors() {
		return Errors;
	}

	private void ErrorsAdd(int resultCode) {
		if (0 == resultCode) {
			return;
		}
		if (getErrors().containsKey(resultCode)) {
			getErrors().put(resultCode, getErrors().get(resultCode) + 1);
		}
		else {
			getErrors().put(resultCode, 1);
		}
	}

	private int ErrorsSum() {
		int sum = 0;
		for (var e : getErrors().values()) {
			sum += e;
		}
		return sum;
	}

	private boolean CheckCurrentCount(String stepName) {
		var CurrentCount = GetCurrentCount();
		if (CurrentCount != getExpectCount()) {
			var report = new StringBuilder();
			var level = getExpectCount() != CurrentCount + ErrorsSum() ? NLog.LogLevel.Fatal : NLog.LogLevel.Info;
			report.append(String.format("%1$s-------------------------------------------", System.lineSeparator()));
			report.append(String.format("%1$s%2$s,", System.lineSeparator(), stepName));
			report.append(String.format("Expect=%1$s,", getExpectCount()));
			report.append(String.format("Now=%1$s,", CurrentCount));
			report.append(String.format("Errors="));
			ByteBuffer.BuildString(report, getErrors());
			report.append(String.format("%1$s-------------------------------------------", System.lineSeparator()));
			logger.Log(level, report.toString());

			setExpectCount(CurrentCount); // 下一个测试重新开始。
			getErrors().clear();
			return level == NLog.LogLevel.Info;
		}
		return true;
	}

	private void ConcurrentAddCount(String stepName, int concurrent) {
		var requests = new ArrayList<AddCount>();
		for (int i = 0; i < concurrent; ++i) {
			requests.add(new AddCount());
		}

		Task[] tasks = new Task[concurrent];
		for (int i = 0; i < requests.size(); ++i) {
			tasks[i] = getAgent().SendForWait(requests.get(i)).Task;
			//logger.Debug("+++++++++ REQUEST {0} {1}", stepName, requests[i]);
		}
		try {
			Task.WaitAll(tasks);
		}
		catch (AggregateException e) {
			// 这里只会发生超时错误RpcTimeoutException。
			// 后面会处理每个requests，这里不做处理了。
			// logger.Warn(ex);
		}
		for (var request : requests) {
			//logger.Debug("--------- RESPONSE {0} {1}", stepName, request);
			if (request.isTimeout()) {
				ErrorsAdd(Procedure.Timeout);
			}
			else {
				ErrorsAdd(request.getResultCode());
			}
		}
	}

	private void SetLogLevel(NLog.LogLevel level) {
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

	private void TestConcurrent(String testname, int count) {
		ConcurrentAddCount(testname, count);
		setExpectCount(getExpectCount() + count);
		CheckCurrentCount(testname);
	}

	public final void RunTrace() {
		// 基本测试
		getAgent().SendForWait(new AddCount()).Task.Wait();
		setExpectCount(getExpectCount() + 1);
		CheckCurrentCount("TestAddCount");

		// 基本并发请求
		SetLogLevel(NLog.LogLevel.Info);
		TestConcurrent("TestConcurrent", 200);

		SetLogLevel(NLog.LogLevel.Trace);

		// 普通节点重启网络一。
		var NotLeaders = GetNodeNotLeaders();
		if (NotLeaders.Count > 0) {
			NotLeaders[0].RestartNet();
		}
		TestConcurrent("TestNormalNodeRestartNet1", 1);

		// 普通节点重启网络二。
		if (NotLeaders.Count > 1) {
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
		Util.Scheduler.getInstance().Schedule((ThisTask) -> leader.Raft.Server.Start(), leader.Raft.RaftConfig.LeaderLostTimeout + 2000, -1);
		TestConcurrent("TestLeaderNodeRestartNet_NewVote", 1);

		// 普通节点重启一。
		NotLeaders = GetNodeNotLeaders();
		if (NotLeaders.Count > 0) {
			NotLeaders[0].StopRaft();
			NotLeaders[0].StartRaft();
		}
		TestConcurrent("TestNormalNodeRestartRaft1", 1);

		// 普通节点重启二。
		if (NotLeaders.Count > 1) {
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
		Util.Scheduler.getInstance().Schedule((ThisTask) -> leader.StartRaft(), StartDely, -1);
		TestConcurrent("TestLeaderNodeRestartRaft", 1);

		// InstallSnapshot;

		logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.Fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

		SetLogLevel(NLog.LogLevel.Info);

		FailActions.Add(new FailAction() { Name = "RestartNet1", tangible.Action0Param = () -> {
					var rafts = ShuffleRafts();
					rafts[0].RestartNet();
		}});
		FailActions.Add(new FailAction() { Name = "RestartNet2", tangible.Action0Param = () -> {
					var rafts = ShuffleRafts();
					rafts[0].RestartNet();
					rafts[1].RestartNet();
		}});
		FailActions.Add(new FailAction() { Name = "RestartNet3", tangible.Action0Param = () -> {
					var rafts = ShuffleRafts();
					rafts[0].RestartNet();
					rafts[1].RestartNet();
					rafts[2].RestartNet();
		}});
		FailActions.Add(new FailAction() { Name = "RestartLeaderNetForVote", tangible.Action0Param = () -> {
					while (true) {
						var leader = GetLeader();
						if (leader == null) {
							Thread.sleep(10); // wait a Leader
							continue;
						}
						leader.Raft.Server.Stop();
						// delay for vote
						Thread.sleep(leader.Raft.RaftConfig.LeaderLostTimeout + 2000);
						leader.Raft.Server.Start();
						break;
					}
		}});
		FailActions.Add(new FailAction() { Name = "RestartRaft1", tangible.Action0Param = () -> {
					var rafts = ShuffleRafts();
					rafts[0].StopRaft();
					rafts[0].StartRaft();
		}});
		FailActions.Add(new FailAction() { Name = "RestartRaft2", tangible.Action0Param = () -> {
					var rafts = ShuffleRafts();
					rafts[0].StopRaft();
					rafts[1].StopRaft();

					rafts[0].StartRaft();
					rafts[1].StartRaft();
		}});
		FailActions.Add(new FailAction() { Name = "RestartRaft3", tangible.Action0Param = () -> {
					var rafts = ShuffleRafts();
					rafts[0].StopRaft();
					rafts[1].StopRaft();
					rafts[2].StopRaft();

					rafts[0].StartRaft();
					rafts[1].StartRaft();
					rafts[2].StartRaft();
		}});
		FailActions.Add(new FailAction() { Name = "RestartLeaderRaftForVote", tangible.Action0Param = () -> {
					while (true) {
						var leader = GetLeader();
						if (leader == null) {
							Thread.sleep(10); // wait a Leader
							continue;
						}
						leader.StopRaft();
						// delay for vote
						Thread.sleep(leader.Raft.RaftConfig.LeaderLostTimeout + 2000);
						leader.StartRaft();
						break;
					}
		}});

		// Start Background FailActions
		Util.Task.Run(RandomTriggerFailActions, "RandomTriggerFailActions");
		var testname = "RealConcurrentDoRequest";
		int lastExpectCount = getExpectCount();
		while (false == Console.KeyAvailable) {
			int count = 5;
			ConcurrentAddCount(testname, count);
			setExpectCount(getExpectCount() + count);
			if (getExpectCount() - lastExpectCount > count * 10) {
				lastExpectCount = getExpectCount();
				var check = CheckCurrentCount(testname);
				logger.Info(String.format("Check=%1$s ExpectCount=%2$s", check, getExpectCount()));
				if (false == check) {
					break;
				}
			}
		}
		Running = false;
		SetLogLevel(NLog.LogLevel.Debug);
		CheckCurrentCount("Final Check!!!");
	}

	private static class FailAction {
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}
		private tangible.Action0Param Action;
		public final tangible.Action0Param getAction() {
			return Action;
		}
		public final void setAction(tangible.Action0Param value) {
			Action = value;
		}
		private long Count;
		public final long getCount() {
			return Count;
		}
		public final void setCount(long value) {
			Count = value;
		}
		@Override
		public String toString() {
			return Name + "=" + Count;
		}
	}
	private ArrayList<FailAction> FailActions = new ArrayList<FailAction> ();
	private ArrayList<FailAction> getFailActions() {
		return FailActions;
	}


	private void WaitExpectCountGrow() {
		WaitExpectCountGrow(20);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private void WaitExpectCountGrow(long growing = 20)
	private void WaitExpectCountGrow(long growing) {
		long oldTaskCount = getExpectCount();
		while (true) {
			Thread.sleep(10);
			if (getExpectCount() - oldTaskCount > growing) {
				break;
			}
		}
	}


	private boolean Running = true;
	private boolean getRunning() {
		return Running;
	}
	private void setRunning(boolean value) {
		Running = value;
	}

	private void RandomTriggerFailActions() {
		while (Running) {
			var fa = FailActions[Util.Random.getInstance().nextInt(FailActions.Count)];
			try {
				fa.Action();
				fa.Count++;
			}
			catch (RuntimeException ex) {
				logger.Error(ex, "FailAction {0}", fa.Name);
				/*
				foreach (var raft in Rafts.Values)
				{
				    raft.StartRaft(); // error recover
				}
				// */
			}
			// 等待失败的节点恢复正常并且服务了一些请求。
			// 由于一个follower失败时，请求处理是能持续进行的，这个等待可能不够。
			WaitExpectCountGrow(100);
		}
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, FailActions);
		logger.Fatal(sb.toString());
	}

	private TestRaft GetLeader() {
		for (var raft : getRafts().values()) {
			if (raft.Raft.IsLeader) {
				return raft;
			}
		}
		return null;
	}

	private ArrayList<TestRaft> GetNodeNotLeaders() {
		var NotLeader = new ArrayList<TestRaft>();
		for (var raft : getRafts().values()) {
			if (!raft.Raft.IsLeader) {
				NotLeader.add(raft);
			}
		}
		return NotLeader;
	}


	private TestRaft[] ShuffleRafts() {
		return Util.Random.Shuffle(getRafts().values().ToArray());
	}

	public final static class AddCount extends Zeze.Net.Rpc<EmptyBean, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(AddCount.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class GetCount extends Zeze.Net.Rpc<EmptyBean, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(GetCount.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public static class TestStateMachine extends StateMachine {
		private int Count;
		public final int getCount() {
			return Count;
		}
		public final void setCount(int value) {
			Count = value;
		}

		public final void AddCountAndWait(String appInstance, long requestId) {
			Raft.AppendLog(new AddCount(appInstance, requestId));
		}

		public final static class AddCount extends Log {
			public AddCount(String appInstance, long requestId) {
				super(appInstance, requestId);

			}

			@Override
			public void Apply(StateMachine stateMachine) {
				(stateMachine instanceof TestStateMachine ? (TestStateMachine)stateMachine : null).setCount((stateMachine instanceof TestStateMachine ? (TestStateMachine)stateMachine : null).getCount() + 1);
			}

			@Override
			public void Decode(ByteBuffer bb) {
				super.Decode(bb);
			}

			@Override
			public void Encode(ByteBuffer bb) {
				super.Encode(bb);
			}
		}

		@Override
		public void LoadFromSnapshot(String path) {
			synchronized (Raft) {
				try (var file = new System.IO.FileStream(path, System.IO.FileMode.Open)) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var bytes = new byte[1024];
					var bytes = new byte[1024];
					int rc = file.Read(bytes);
					var bb = ByteBuffer.Wrap(bytes, 0, rc);
					Count = bb.ReadInt();
				}
			}
		}

		@Override
		public boolean Snapshot(String path, tangible.OutObject<Long> LastIncludedIndex, tangible.OutObject<Long> LastIncludedTerm) {
			try (var file = new System.IO.FileStream(path, System.IO.FileMode.Create)) {
				long oldFirstIndex = 0;
				synchronized (Raft) {
					var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
					LastIncludedIndex.outArgValue = lastAppliedLog.Index;
					LastIncludedTerm.outArgValue = lastAppliedLog.Term;
					var bb = ByteBuffer.Allocate();
					bb.WriteInt(Count);
					file.Write(bb.getBytes(), bb.getReadIndex(), bb.getSize());
					file.Close();
					oldFirstIndex = Raft.LogSequence.GetAndSetFirstIndex(LastIncludedIndex.outArgValue);
				}
				Raft.LogSequence.RemoveLogBeforeLastApplied(oldFirstIndex);
				return true;
			}
		}

		public TestStateMachine() {
			AddFactory((new AddCount("", 0)).getTypeId(), () -> new AddCount("", 0));
		}
	}

	public static class TestRaft {
		private Raft Raft;
		public final Raft getRaft() {
			return Raft;
		}
		private void setRaft(Raft value) {
			Raft = value;
		}
		private TestStateMachine StateMachine;
		public final TestStateMachine getStateMachine() {
			return StateMachine;
		}
		private void setStateMachine(TestStateMachine value) {
			StateMachine = value;
		}
		private String RaftConfigFileName;
		public final String getRaftConfigFileName() {
			return RaftConfigFileName;
		}
		private String RaftName;
		public final String getRaftName() {
			return RaftName;
		}

		public final void RestartNet() {
			logger.Debug("Raft.Net {0} Restart ...", RaftName);
			if (Raft != null) {
				Raft.Server.Stop();
			}
			if (Raft != null) {
				Raft.Server.Start();
			}
		}

		public final void StopRaft() {
			synchronized (this) {
				logger.Debug("Raft {0} Stop ...", RaftName);
				if (Raft != null) {
					Raft.Server.Stop();
				}

				// 在同一个进程中，没法模拟进程退出，
				// 此时RocksDb应该需要关闭，否则重启回失败吧。
				if (Raft != null) {
					Raft.Shutdown();
				}
				Raft = null;
			}
		}


		public final void StartRaft() {
			StartRaft(false);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void StartRaft(bool resetLog = false)
		public final void StartRaft(boolean resetLog) {
			synchronized (this) {
				if (null != Raft) {
					Raft.Server.Start();
					return;
				}
				logger.Debug("Raft {0} Start ...", RaftName);
				StateMachine = new TestStateMachine();

				var raftConfig = RaftConfig.Load(RaftConfigFileName);
				raftConfig.setAppendEntriesTimeout(1000);
				raftConfig.setLeaderHeartbeatTimer(1500);
				raftConfig.setLeaderLostTimeout(2000);
				raftConfig.setDbHome(Paths.get(".").resolve(RaftName.Replace(':', '_')).toString());
				if (resetLog) {
					logger.Warn("------------------------------------------------");
					logger.Warn("--------------- Reset Log ----------------------");
					logger.Warn("------------------------------------------------");
					if ((new File(raftConfig.getDbHome())).isDirectory()) {
						Directory.Delete(raftConfig.getDbHome(), true);
					}
				}
				(new File(raftConfig.getDbHome())).mkdirs();

				Raft = new Raft(StateMachine, RaftName, raftConfig);
				Raft.Server.AddFactoryHandle((new AddCount()).getTypeId(), new Net.Service.ProtocolFactoryHandle() {Factory = () -> new AddCount(), Handle = ProcessAddCount});

				Raft.Server.AddFactoryHandle((new GetCount()).getTypeId(), new Net.Service.ProtocolFactoryHandle() {Factory = () -> new GetCount(), Handle = ProcessGetCount});
				Raft.Server.Start();
			}
		}

		public TestRaft(String raftName, String raftConfigFileName) {
			RaftName = raftName;
			RaftConfigFileName = raftConfigFileName;
			StartRaft(true);
		}

		private int ProcessAddCount(Zeze.Net.Protocol p) {
			if (false == Raft.IsLeader) {
				return Procedure.CancelExcption; // fast fail
			}

			var r = p instanceof AddCount ? (AddCount)p : null;
			synchronized (StateMachine) {
				StateMachine.AddCountAndWait(r.getSender().getRemoteAddress(), r.getUniqueRequestId());
				r.SendResultCode(0);
			}
			return Procedure.Success;
		}

		private int ProcessGetCount(Zeze.Net.Protocol p) {
			var r = p instanceof GetCount ? (GetCount)p : null;
			synchronized (StateMachine) {
				r.SendResultCode(StateMachine.Count);
			}
			return Procedure.Success;
		}
	}

}