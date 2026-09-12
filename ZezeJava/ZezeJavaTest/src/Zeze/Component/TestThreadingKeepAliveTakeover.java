package Zeze.Component;

import java.util.concurrent.TimeUnit;

import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.DispatchMode;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND4-40：lastAppSerial 的"同连接KeepAlive有序处理"前提依赖派发模式——KeepAlive 以
 * TransactionLevel.None+Normal 经共享线程池派发，既不保证同连接顺序也不保证跨连接顺序：
 * 迟到的旧实例 KeepAlive 可在新实例接管并持锁后再次触发 release，强制释放新实例持有的
 * 全部分布式锁（互斥性破坏）。修复：KeepAlive 改 Direct 派发（IO线程按TCP接收序串行），
 * 前提真实成立；lastAppSerial 声明 volatile。
 * <p>
 * 跨连接迟到时序的确定红不可构造（需控制池调度/网络延迟，天然竞态档），本测试锁定：
 * ①派发契约——KeepAlive 注册必须为 Direct（修复前为 Normal，红）；②接管语义——
 * 新 serial 触发释放、同 serial 不释放（修复前后行为不变，防回归）。
 * 注：包内直调 protected rpc 处理器（与 TestThreadingRWLockDowngrade 同款测试缝）。
 */
@Fast
public class TestThreadingKeepAliveTakeover {

	private static final int SERVER_ID = 941;
	private static final BGlobalThreadId T1 = new BGlobalThreadId(SERVER_ID, 1);
	private static final BGlobalThreadId T2 = new BGlobalThreadId(SERVER_ID, 2);

	private ThreadingServer server;

	@BeforeEach
	public void setup() {
		Zeze.Util.Task.tryInitThreadPool();
		server = new ThreadingServer(new Service("TestThreadingKeepAliveTakeover"), new ServiceManagerServer.Conf());
	}

	@AfterEach
	public void cleanup() {
		server.close();
	}

	/** KeepAlive 必须以 Direct 派发注册：字段前提"同连接有序"的机制载体（FND4-40 修复本体）。 */
	@Test
	public void testKeepAliveDispatchModeIsDirect() {
		var service = new Service("TestThreadingKeepAliveTakeover.Mode");
		server.RegisterProtocols(service);
		var handle = service.getFactorys().get(KeepAlive.TypeId_);
		Assertions.assertNotNull(handle);
		Assertions.assertEquals(DispatchMode.Direct, handle.Mode,
				"KeepAlive 的 lastAppSerial 有序前提要求 IO 线程按接收序串行处理（Direct）");
	}

	private static void keepAlive(ThreadingServer server, long appSerialId) throws Exception {
		var ka = new KeepAlive();
		ka.Argument.setServerId(SERVER_ID);
		ka.Argument.setAppSerialId(appSerialId);
		Assertions.assertEquals(0L, server.ProcessKeepAlive(ka));
	}

	/** 构造并入队SimulateThread，等动作执行完应答（resultCode在sendResultDone=true之前赋值）。 */
	private long tryLock(BGlobalThreadId id, String name, int timeoutMs) throws InterruptedException {
		var r = new MutexTryLock();
		r.Argument.setLockName(new BLockName(id, name));
		r.Argument.setTimeoutMs(timeoutMs);
		Assertions.assertEquals(0L, server.ProcessMutexTryLockRequest(r));
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!r.isSendResultDone()) {
			if (System.nanoTime() > deadline)
				throw new AssertionError("timeout waiting mutex tryLock result: " + name);
			Thread.sleep(10);
		}
		return r.getResultCode();
	}

	/** 接管语义锁：新 appSerialId 触发全量释放（预期接管清理）；同 serial 不得释放。 */
	@Test
	public void testTakeoverSemantics() throws Exception {
		var name = "UnitTest.Threading.KeepAliveTakeover";
		keepAlive(server, 1);
		Assertions.assertEquals(0L, tryLock(T1, name, 1000)); // 实例1持锁
		Assertions.assertEquals(1L, tryLock(T2, name, 200)); // 互斥基线：T2拿不到

		keepAlive(server, 2); // 新实例接管：serial变化 → 释放实例1全部锁
		Assertions.assertEquals(0L, tryLock(T2, name, 500), "接管后旧实例锁必须被释放");

		keepAlive(server, 2); // 同 serial：不得再次释放
		Assertions.assertEquals(1L, tryLock(T1, name, 200),
				"同 appSerialId 的 KeepAlive 不得释放新实例已持有的锁");
	}
}
