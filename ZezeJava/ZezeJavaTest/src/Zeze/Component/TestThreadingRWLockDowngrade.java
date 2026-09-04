package Zeze.Component;

import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.ReadWriteLockOperate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND2-C1-1回归：rwLockRefs按锁名共享一个条目、读写计数分开持有（JDK支持写→读降级）。
 * 原实现在eExitRead/eExitWrite里只看本模式计数清零即删refs条目——混合持有（写→读降级）
 * 场景另一模式的持有脱离跟踪：后续exit应答0却不再unlock、模拟线程持锁被判空闲退出、
 * timeoutRelease也遍历不到，锁永久悬挂直到进程重启。
 * 现有TestThreading.testRWLock只覆盖read-read-exit-exit，无混合持有用例。
 * 另锁FND2-C1-3：ProcessSemaphoreReleaseRequest的permits<=0前置校验应答-1
 * （65c291f2e修了TryAcquire家族，独漏Release）。
 * 注：文件放在 src/Zeze/Component/ 但声明 package Zeze.Component——需要直接调用
 * protected的rpc处理器的包内测试缝（与TestDelayRemoveOnTimer同款先例）。
 * rpc不绑定连接，SendResultCode对null sender只记warn日志；结果码经resultCode字段观察，
 * sendResultDone是volatile，其写发生在resultCode赋值之后，轮询可见性有保证。
 */
@Fast
public class TestThreadingRWLockDowngrade {

	private static final BGlobalThreadId T1 = new BGlobalThreadId(901, 1);
	private static final BGlobalThreadId T2 = new BGlobalThreadId(901, 2);

	private ThreadingServer server;

	@BeforeEach
	public void setup() {
		server = new ThreadingServer(new Service("TestThreadingRWLockDowngrade"), new ServiceManagerServer.Conf());
	}

	@AfterEach
	public void cleanup() {
		server.close();
	}

	private static ReadWriteLockOperate rwOp(BGlobalThreadId id, String name, int opType, int timeoutMs) {
		var r = new ReadWriteLockOperate();
		r.Argument.setLockName(new BLockName(id, name));
		r.Argument.setOperateType(opType);
		r.Argument.setTimeoutMs(timeoutMs);
		return r;
	}

	/** 等SimulateThread异步执行完动作并应答（resultCode在sendResultDone=true之前赋值）。 */
	private static long awaitResult(ReadWriteLockOperate r) throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!r.isSendResultDone()) {
			if (System.nanoTime() > deadline)
				throw new AssertionError("timeout waiting rwlock operate result: " + r.Argument.getOperateType());
			Thread.sleep(10);
		}
		return r.getResultCode();
	}

	// 写→读降级后先exitWrite：writeHold清零时readHold仍=1，refs条目必须保留，
	// 否则exitRead找不到条目应答0但读锁永不释放——之后任何人tryEnterWrite必须永久失败。
	@Test
	public void test1_DowngradeExitWriteFirst() throws Exception {
		var name = "UnitTest.Threading.RWLockDowngrade1";
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eEnterWrite, 1000)));
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eEnterRead, 1000))); // 降级：写持有时再读
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eExitWrite, 0)));

		// 降级窗口内读锁仍被持有：他人写必须拿不到（读锁语义正确性的旁证）。
		Assertions.assertEquals(1L, awaitSend(rwOp(T2, name, Threading.eEnterWrite, 200)));

		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eExitRead, 0)));

		// 核心断言：全部退出后锁必须真正释放，新写者立即拿到。
		var enter = rwOp(T2, name, Threading.eEnterWrite, 1000);
		Assertions.assertEquals(0L, awaitSend(enter), "exitWrite先于exitRead时读锁悬挂，新写者拿不到锁");
		Assertions.assertEquals(0L, awaitSend(rwOp(T2, name, Threading.eExitWrite, 0)));
	}

	// 对称次序：写→读降级后先exitRead（writeHold仍=1），refs条目同样必须保留。
	@Test
	public void test2_DowngradeExitReadFirst() throws Exception {
		var name = "UnitTest.Threading.RWLockDowngrade2";
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eEnterWrite, 1000)));
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eEnterRead, 1000)));
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eExitRead, 0)));
		Assertions.assertEquals(0L, awaitSend(rwOp(T1, name, Threading.eExitWrite, 0)));

		// 核心断言：若exitRead提前删了条目，exitWrite应答0却不再unlock——写锁悬挂，新写者拿不到。
		var enter = rwOp(T2, name, Threading.eEnterWrite, 1000);
		Assertions.assertEquals(0L, awaitSend(enter), "exitRead先于exitWrite时写锁悬挂，新写者拿不到锁");
		Assertions.assertEquals(0L, awaitSend(rwOp(T2, name, Threading.eExitWrite, 0)));
	}

	private long awaitSend(ReadWriteLockOperate r) throws Exception {
		server.ProcessReadWriteLockOperateRequest(r);
		return awaitResult(r);
	}

	// FND2-C1-3：permits<=0必须入队前校验直接应答-1（动作内release(负数)抛IllegalArgumentException
	// 会被SimulateThread.run()吞掉且不补发结果码）。
	@Test
	public void test3_SemaphoreReleaseInvalidPermits() throws Exception {
		var r = new SemaphoreRelease();
		r.Argument.setLockName(new BLockName(T1, "UnitTest.Threading.SemRelease"));
		r.Argument.setPermits(0);
		server.ProcessSemaphoreReleaseRequest(r);
		Assertions.assertTrue(r.isSendResultDone(), "permits<=0必须同步应答，不入队");
		Assertions.assertEquals(ThreadingServer.ResultCodeInvalidArgument, r.getResultCode());
	}
}
