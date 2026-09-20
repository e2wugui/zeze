package Zeze.Arch;

import java.util.concurrent.atomic.AtomicBoolean;

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
 * A3-F2回归：onResult迟注册扫描路径原先先c.unlock()再跑回调，与生产者路径
 * （processResult持ctx锁调用result()在ctx锁内跑回调）并发执行同一回调，
 * 违反本文件声明的串行契约（"此方法不会跟其它的onResult并发"）。
 * 修复：扫描路径的回调执行移回ctx锁内（可重入，锁序保持ctx→future）。
 * 核心断言：迟注册回调内isLockHeldByCurrentThread()必须为真；回调持锁期间
 * 并发processResult被互斥阻塞作辅助证据；回调返回后释放。
 */
@Fast
public class TestA3F2RedirectAllOnResultScanUnderLock {

	private Service service;

	@BeforeEach
	public void setUp() {
		// 裸Service（不启动、无Application）：getZeze()为null，回调走锁外直跑分支；
		// 仅用于getService()（tryRemoveManualContext对不存在sessionId为no-op）。
		service = new Service("a3f2svc", (Zeze.Application)null, new Config());
	}

	/** 迟注册扫描路径的回调必须在ctx锁内执行，且回调期间并发的processResult被互斥。 */
	@Test
	public void testScanPathCallbackHoldsCtxLockAndExcludesWriter() throws Exception {
		final int concurrentLevel = 4;
		var ctx = new RedirectAllContext<RedirectResult>(concurrentLevel, binary -> new RedirectResult());
		ctx.setService(service);
		ctx.setSessionId(1);

		// 生产者路径先产出一个结果（此时onResult未注册，result()直接返回）。
		processHashes(ctx, 0);
		Assertions.assertEquals(1, ctx.getAllResults().size(), "测试前提：已有一个结果");

		var lockHeldInCallback = new AtomicBoolean(true);
		var writerFinishedDuringCallback = new AtomicBoolean(false);
		var writerRan = new AtomicBoolean(false);
		var lateWriter = new Thread(() -> {
			processHashes(ctx, 1);
			writerRan.set(true);
		}, "a3f2-late-writer");
		var callbackChecked = new AtomicBoolean(false);

		ctx.getFuture().OnResult(r -> {
			// 迟到结果会经生产者路径再次回调本方法（hash 1）；锁语义断言只执行一次。
			if (!callbackChecked.compareAndSet(false, true))
				return;
			// 核心断言采集：迟注册扫描路径的回调是否持有ctx锁（修复前此处为false）
			lockHeldInCallback.set(ctx.isLockHeldByCurrentThread());
			// 并发生产者：回调持ctx锁期间必须被挡在锁外
			lateWriter.start();
			var deadline = System.currentTimeMillis() + 300;
			while (!writerRan.get() && System.currentTimeMillis() < deadline)
				Thread.sleep(10);
			writerFinishedDuringCallback.set(writerRan.get());
		});

		Assertions.assertTrue(lockHeldInCallback.get(),
				"迟注册扫描路径的回调必须在ctx锁内执行（A3-F2：锁外执行与生产者路径并发同一回调）");
		Assertions.assertFalse(writerFinishedDuringCallback.get(),
				"回调持锁期间并发的processResult必须被互斥阻塞（未加锁修复前它可立即完成写入）");
		lateWriter.join(10_000);
		Assertions.assertTrue(writerRan.get(), "回调返回释放锁后生产者必须能继续");
		Assertions.assertEquals(2, ctx.getAllResults().size(), "迟到结果正常写入");
	}

	/** 护栏：先注册后生产的结果路径（processResult→result回调）不受修复影响，恰回调一次。 */
	@Test
	public void testProducerPathStillCallbackOnce() {
		final int concurrentLevel = 2;
		var ctx = new RedirectAllContext<RedirectResult>(concurrentLevel, binary -> new RedirectResult());
		ctx.setService(service);
		ctx.setSessionId(2);

		var count = new long[]{0};
		ctx.getFuture().OnResult(r -> count[0]++);
		Assertions.assertEquals(0, count[0], "注册时不产结果");

		processHashes(ctx, 0);
		Assertions.assertEquals(1, count[0], "生产者路径对每个结果恰回调一次");
		processHashes(ctx, 1);
		Assertions.assertEquals(2, count[0], "第二个结果同样回调");
	}

	private static void processHashes(RedirectAllContext<RedirectResult> ctx, int... hashes) {
		var res = new ModuleRedirectAllResult();
		for (var hash : hashes)
			res.Argument.getHashes().put(hash, new BModuleRedirectAllHash.Data(Procedure.Success, Binary.Empty));
		ctx.processResult(res);
	}
}
