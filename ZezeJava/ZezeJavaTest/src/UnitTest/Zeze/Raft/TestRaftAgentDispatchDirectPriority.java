package UnitTest.Zeze.Raft;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Net.Service;
import Zeze.Raft.Agent;
import Zeze.Raft.RaftConfig;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND3-40：SM-raft订阅的Edit推送乱序应用（订阅状态与注册表永久分叉）修复的机制级验证。
 * <p>
 * 根因：Agent.NetClient.dispatchProtocol 在 dispatchProtocolToInternalThreadPool=true 时
 * 无条件把入站协议投入critical池（cachedThreadPool，无顺序），无视factoryHandle.Mode；
 * 而应答路径dispatchRpcResponse始终内联IO线程——同socket上"请求(Edit)入池、应答
 * (Subscribe)内联"两条执行线并发，TCP接收序被反转：旧Edit在新快照(onFirstCommit整体
 * 替换)之后落地会复活已删实例；Edit之间倒序应用同样永久分叉。非raft版Edit/Subscribe
 * 均注册Direct在selector线程内联串行，无此问题。
 * <p>
 * 修复：①Direct优先于池化flag（显式注册Direct的协议在调用线程串行执行）；
 * ②ServiceManagerAgentWithRaft.ProcessEditRequest标注@DispatchModeAnnotation(Direct)，
 * 生成的RegisterProtocols经Reflect读取（无需改生成代码）。
 * <p>
 * 本测试headless构造Agent（不start、无网络活动），直接调用NetClient.dispatchProtocol
 * 验证派发语义；另验证注册侧注解被Reflect读到。
 */
@Fast
public class TestRaftAgentDispatchDirectPriority {
	private static final String RAFT_XML = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="testDispatchDirectPriority">
			<node Host="127.0.0.1" Port="19570"/>
			<node Host="127.0.0.1" Port="19571"/>
			<node Host="127.0.0.1" Port="19572"/>
			</raft>
			""";

	private Agent agent;
	private Agent.NetClient netClient;

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		agent = new Agent("testDispatchDirectPriority", RaftConfig.loadFromString(RAFT_XML), new Zeze.Config());
		netClient = agent.getClient();
		// 对齐ServiceManagerAgentWithRaft/GlobalCacheManagerWithRaftAgent的构造配置
		agent.dispatchProtocolToInternalThreadPool = true;
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (agent != null)
			agent.stop();
	}

	@Test
	public void testDirectPriorityOverPoolFlag() throws Exception {
		// 1. flag=true + Direct注册：必须在调用线程同步执行完（修前被flag无条件入池=乱序之源）
		var directThread = new AtomicReference<Thread>();
		var directHandle = new Service.ProtocolFactoryHandle<>(Edit::new, p -> {
			directThread.set(Thread.currentThread());
			return 0;
		}, TransactionLevel.None, DispatchMode.Direct);
		netClient.dispatchProtocol(new Edit(), directHandle);
		Assertions.assertSame(Thread.currentThread(), directThread.get(),
				"Direct注册的协议必须在调用线程内联执行（flag=true也不得入池）");

		// 2. flag=true + Normal注册：仍入池异步执行——flag语义不被Direct优先规则破坏
		var normalThread = new AtomicReference<Thread>();
		var normalDone = new CountDownLatch(1);
		var normalHandle = new Service.ProtocolFactoryHandle<>(Edit::new, p -> {
			normalThread.set(Thread.currentThread());
			normalDone.countDown();
			return 0;
		}, TransactionLevel.None, DispatchMode.Normal);
		netClient.dispatchProtocol(new Edit(), normalHandle);
		Assertions.assertTrue(normalDone.await(5, TimeUnit.SECONDS), "Normal注册的协议必须被执行");
		Assertions.assertNotSame(Thread.currentThread(), normalThread.get(),
				"flag=true + Normal注册仍应入池执行");

		// 3. flag=false + Direct注册：内联（修复前else分支已如此，锁定该半边不被回归）
		agent.dispatchProtocolToInternalThreadPool = false;
		var directNoFlagThread = new AtomicReference<Thread>();
		var directNoFlagHandle = new Service.ProtocolFactoryHandle<>(Edit::new, p -> {
			directNoFlagThread.set(Thread.currentThread());
			return 0;
		}, TransactionLevel.None, DispatchMode.Direct);
		netClient.dispatchProtocol(new Edit(), directNoFlagHandle);
		Assertions.assertSame(Thread.currentThread(), directNoFlagThread.get(),
				"flag=false + Direct注册必须在调用线程内联执行");

		// 4. 应答路径（dispatchRpcResponse）必须内联调用线程——Edit内联后与Subscribe应答
		//    同线程按TCP接收序串行，这是顺序契约的另一半。
		var responseThread = new AtomicReference<Thread>();
		netClient.dispatchRpcResponse(new Subscribe(), p -> {
			responseThread.set(Thread.currentThread());
			return 0;
		}, directHandle);
		Assertions.assertSame(Thread.currentThread(), responseThread.get(),
				"应答处理必须内联调用线程（Edit与应答同线程串行的前提）");
	}

	@Test
	public void testEditRequestRegisteredAsDirect() {
		// RegisterProtocols（生成代码）经 Reflect.getDispatchMode("ProcessEditRequest", Normal)
		// 读取方法注解——直接验证同一查找路径命中Direct（修前返回Normal）。
		var mode = new Reflect(ServiceManagerAgentWithRaft.class)
				.getDispatchMode("ProcessEditRequest", DispatchMode.Normal);
		Assertions.assertEquals(DispatchMode.Direct, mode,
				"ProcessEditRequest必须注册为Direct（Edit推送按接收序串行应用的前提）");
	}
}
