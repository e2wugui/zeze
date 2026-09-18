package Zeze.Arch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND8-81回归：RedirectAllFutureImpl.onAllDone直跑路径（注册时已完成的future当场执行
 * 回调）不持ctx锁——isCompleted()为真不等于hashResults写入结束：processResult可在锁内
 * 循环put中途越过完成阈值（多hash批量报文），或isTimeout置位而迟到结果仍在写。回调按
 * 约定调getAllResults()无锁遍历IntHashMap，与put/resize并发即数据竞态（漏项/重复/
 * 撕裂key-value/AIOOBE）。对照onResult迟注册重放路径（184行c.lock()后遍历）有锁。
 * 修复：直跑路径（含newProcedure分支）整段在c.lock()内执行回调，与onRemoved→allDone
 * "ctx锁内跑回调"的先例对齐；ctx锁可重入，锁序保持ctx→future。
 * <p>
 * 断言核心：直跑回调内ctx.isLockHeldByCurrentThread()必须为真（修复前必为假，完全
 * 确定性）；并以"回调持锁期间并发processResult必须被互斥阻塞"作互斥证据（宽裕时间窗，
 * 仅作辅助）。框架路径（先注册后完成，onRemoved→allDone触发）作护栏。
 */
@Fast
public class TestFnd881RedirectAllOnAllDoneUnderLock {

	private Service service;

	@BeforeEach
	public void setUp() {
		// 裸Service（不启动、无Application）：getZeze()为null，onAllDone走else直跑分支；
		// 仅用于getService()/manualContexts（tryRemoveManualContext移除不存在的sessionId为no-op）。
		service = new Service("a7fnd881svc", (Zeze.Application)null, new Config());
	}

	/** 直跑路径：完成后再注册的回调必须在ctx锁内执行，且与并发processResult互斥。 */
	@Test
	public void testDirectRunHoldsCtxLockAndExcludesWriter() throws Exception {
		final int concurrentLevel = 4;
		var ctx = new RedirectAllContext<RedirectResult>(concurrentLevel, binary -> new RedirectResult());
		ctx.setService(service);
		ctx.setSessionId(1);

		// 单条多hash结果报文使完成阈值在processResult循环内被跨越（缺陷描述的常态形态）
		processHashes(ctx, 0, 1, 2, 3);
		Assertions.assertTrue(ctx.isCompleted(), "测试前提：结果已达完成阈值");

		var lockHeldInCallback = new AtomicBoolean(true);
		var lateFinishedDuringCallback = new AtomicBoolean(false);
		var resultsSeenInCallback = new AtomicInteger(-1);
		var lateThread = new AtomicReference<Thread>();
		var lateRan = new AtomicBoolean(false);

		ctx.getFuture().OnAllDone(c -> {
			// 核心断言采集：直跑回调是否持有ctx锁（修复前此处为false）
			lockHeldInCallback.set(c.isLockHeldByCurrentThread());
			// 并发迟到结果：回调持有ctx锁期间必须被挡在锁外
			var late = new Thread(() -> {
				processHashes(ctx, 99);
				lateRan.set(true);
			}, "a7fnd881-late-writer");
			lateThread.set(late);
			late.start();
			// 给迟到线程充足时间尝试进入processResult；互斥成立则它阻塞在ctx锁上不得完成
			var deadline = System.currentTimeMillis() + 300;
			while (!lateRan.get() && System.currentTimeMillis() < deadline)
				Thread.sleep(10);
			lateFinishedDuringCallback.set(lateRan.get());
			resultsSeenInCallback.set(c.getAllResults().size());
		});

		Assertions.assertTrue(lockHeldInCallback.get(),
				"直跑路径回调必须在ctx锁内执行（FND8-81：无锁遍历hashResults与processResult持锁写并发）");
		Assertions.assertFalse(lateFinishedDuringCallback.get(),
				"回调持锁期间并发的processResult必须被互斥阻塞（未加锁修复前它可立即完成写入）");
		var late = lateThread.get();
		Assertions.assertNotNull(late);
		late.join(10_000);
		Assertions.assertTrue(lateRan.get(), "回调返回释放锁后迟到结果必须能继续");
		// 已完成后迟到结果被isCompleted检查忽略，不新增条目
		Assertions.assertEquals(concurrentLevel, ctx.getAllResults().size(), "迟到的hash不得新增结果条目");
		Assertions.assertEquals(concurrentLevel, resultsSeenInCallback.get(), "回调内必须看到全部结果");
	}

	/** 护栏：先注册后完成的框架路径（onRemoved→allDone触发）不受修复影响，恰回调一次。 */
	@Test
	public void testFrameworkPathStillCallbackOnce() throws Exception {
		var ctx = new RedirectAllContext<RedirectResult>(2, binary -> new RedirectResult());
		ctx.setService(service);
		ctx.setSessionId(2);

		var called = new CountDownLatch(1);
		var callCount = new AtomicLong();
		var lockHeldInCallback = new AtomicBoolean(false);
		var resultsSeenInCallback = new AtomicInteger(-1);
		ctx.getFuture().OnAllDone(c -> {
			callCount.incrementAndGet();
			lockHeldInCallback.set(c.isLockHeldByCurrentThread());
			resultsSeenInCallback.set(c.getAllResults().size());
			called.countDown();
		});

		Assertions.assertEquals(0, callCount.get(), "未完成不得触发回调");
		processHashes(ctx, 0);
		Assertions.assertEquals(0, callCount.get(), "部分结果不得触发onAllDone");
		processHashes(ctx, 1);
		// ctx未真正注册进manualContexts（直接setService/setSessionId），模拟移除链路回调onRemoved
		ctx.onRemoved();
		Assertions.assertTrue(called.await(5, TimeUnit.SECONDS), "完成后（onRemoved→allDone）必须触发回调");
		Assertions.assertEquals(1, callCount.get(), "回调恰好一次");
		Assertions.assertTrue(lockHeldInCallback.get(), "框架路径回调在onRemoved的ctx锁内执行");
		Assertions.assertEquals(2, resultsSeenInCallback.get(), "回调内必须看到全部结果");
	}

	private static void processHashes(RedirectAllContext<RedirectResult> ctx, int... hashes) {
		var res = new ModuleRedirectAllResult();
		for (var hash : hashes)
			res.Argument.getHashes().put(hash, new BModuleRedirectAllHash.Data(Procedure.Success, Binary.Empty));
		ctx.processResult(res);
	}
}
