package Zezex;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Game.App;
import Zeze.Transaction.Procedure;
import Zeze.Util.IntHashSet;
import Zeze.Util.TaskCompletionSource;

@SuppressWarnings("NewClassNamingConvention")
public class ModuleRedirectRank {
	private App app1;
	private App app2;

	@BeforeEach
	protected void setUp() {
		app1 = App.Instance;
		app2 = new App();

		try {
			app1.Start(new String[]{"-ServerId", "30"});
		} catch (Throwable e) {
			// resource close（best-effort）：Stop 的异常只打印不抛，保证抛出的 RuntimeException 包的是原始启动异常 e，
			// 否则 Stop 失败（如半启动时的 NPE）会掩盖真正的启动失败原因。
			try {
				app1.Stop();
			} catch (Exception ex) {
				//noinspection CallToPrintStackTrace
				ex.printStackTrace();
			}
			throw new RuntimeException(e);
		}

		try {
			app2.Start(new String[]{"-ServerId", "31", "-ProviderDirectPort", "20002"});

			// 等两个app的provider服务在SM注册并且互相可见（identity即serverId），替代盲等2秒。
			// 双向都检查：RedirectToServer/TestHash/TestToAll 需要 app1 与 app2 都能路由到对方。
			harness.TestEnv.waitServerRegistered(app1.Zeze, 30, 31);
			harness.TestEnv.waitServerRegistered(app2.Zeze, 30, 31);
		} catch (Throwable e) {
			// resource close（best-effort）：Stop 的异常只打印不抛，保证抛出的 RuntimeException 包的是原始启动异常 e，
			// 否则 Stop 失败（如半启动时的 NPE）会掩盖真正的启动失败原因。
			try {
				app2.Stop();
			} catch (Exception ex) {
				//noinspection CallToPrintStackTrace
				ex.printStackTrace();
			}
			throw new RuntimeException(e);
		}
	}

	@AfterEach
	protected void tearDown() {
		System.out.println("Begin Stop");
		try {
			app2.stopBeforeModules();
			app1.stopBeforeModules();

			app2.Stop();
			app1.Stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.println("End Stop");
	}

	@Test

	public void testRedirect() throws Exception {
		// RedirectToServer
		/*
		var context = app1.Zeze.getHotManager().getModuleContext("Game.Rank", IModuleRank.class);
		var rank = context.getService();
		/*/
		var rank = app1.Game_Rank;
		// */
		rank.TestToServer(30, 111).then(result -> {
			assertEquals(111, result.getOut());
			assertEquals(30, result.getServerId());
		}).await();

		rank.TestToServer(31, 222).then(result -> {
			assertEquals(222, result.getOut());
			assertEquals(31, result.getServerId());
		}).await();

		rank.TestToServer(30, 333).then(result -> {
			assertEquals(333, result.getOut());
			assertEquals(30, result.getServerId());
		}).await();

		rank.TestToServer(31, 444).then(result -> {
			assertEquals(444, result.getOut());
			assertEquals(31, result.getServerId());
		}).await();

		// RedirectHash
		var hash11 = Zeze.Serialize.ByteBuffer.calc_hashnr(127366);
		System.out.println("11--->" + hash11);
		rank.TestHash(hash11, 555).then(result -> {
			assertEquals(hash11, result.getHash());
			assertEquals(555, result.getOut());
			System.out.println("11--->" + result.getServerId());
			assertEquals(30, result.getServerId());
		}).await();

		var hash12 = Zeze.Serialize.ByteBuffer.calc_hashnr(100);
		System.out.println("12--->" + hash12);
		rank.TestHash(hash12, 666).then(result -> {
			assertEquals(hash12, result.getHash());
			assertEquals(666, result.getOut());
			System.out.println("12--->" + result.getServerId());
			assertEquals(31, result.getServerId());
		}).await();

		var hash21 = Zeze.Serialize.ByteBuffer.calc_hashnr(127366);
		System.out.println("21--->" + hash21);
		rank.TestHash(hash21, 777).then(result -> {
			assertEquals(hash21, result.getHash());
			assertEquals(777, result.getOut());
			System.out.println("21--->" + result.getServerId());
			assertEquals(30, result.getServerId());
		}).await();

		var hash22 = Zeze.Serialize.ByteBuffer.calc_hashnr(100);
		System.out.println("22--->" + hash22);
		rank.TestHash(hash22, 888).then(result -> {
			assertEquals(hash22, result.getHash());
			assertEquals(888, result.getOut());
			System.out.println("22--->" + result.getServerId());
			assertEquals(31, result.getServerId());
		}).await();

		// RedirectAll
		final int CONCURRENT_LEVEL = 6;
		var future1 = new TaskCompletionSource<Boolean>();
		var hashes = new IntHashSet();
		rank.TestToAll(CONCURRENT_LEVEL, 12345).onResult(r -> {
			var h = r.getHash();
			var rc = r.getResultCode();
			System.out.println("TestToAll onResult: hash=" + h + ", resultCode=" + rc + ", out=" + r.out);
			assertTrue(h >= 0 && h < CONCURRENT_LEVEL);
			assertTrue(hashes.add(h));
			if (rc == Procedure.Success)
				assertEquals(12345, r.out);
			else if (rc == Procedure.Exception)
				assertEquals(0, r.out);
		}).onAllDone(ctx -> {
			assertFalse(ctx.isTimeout());
			try {
				var allResults = ctx.getAllResults();
				System.out.println("TestToAll onAllDone: allResults=" + allResults);
				assertEquals(CONCURRENT_LEVEL, allResults.size());
				assertEquals(Procedure.Success, allResults.get(0).getResultCode()); // local
				assertEquals(Procedure.Success, allResults.get(1).getResultCode()); // remote
				assertEquals(Procedure.Exception, allResults.get(2).getResultCode()); // local exception
				assertEquals(Procedure.Exception, allResults.get(3).getResultCode()); // remote exception
				assertEquals(Procedure.Success, allResults.get(4).getResultCode()); // local async
				assertEquals(Procedure.Success, allResults.get(5).getResultCode()); // remote async
			} finally {
				future1.setResult(true);
			}
		});
		assertTrue(future1.get());
		assertEquals(CONCURRENT_LEVEL, hashes.size());

		rank.TestToAll(0, 12345).await().onAllDone(ctx -> {
			if (ctx.isCompleted()) {
				System.out.println("TestToAll(0) onAllDone: allResults=" + ctx.getAllResults());
				assertEquals(0, ctx.getAllResults().size());
			}
		});

		rank.TestToServerBeanResult(30, true).await().onSuccess(Assertions::assertNull).onFail(r -> Assertions.fail(r.getMessage()));
		rank.TestToServerBeanResult(30, null).await().onSuccess(Assertions::assertNull).onFail(r -> Assertions.fail(r.getMessage()));
		rank.TestToServerBeanResult(30, false).await().onSuccess(Assertions::assertNotNull).onFail(r -> Assertions.fail(r.getMessage()));
	}
}
