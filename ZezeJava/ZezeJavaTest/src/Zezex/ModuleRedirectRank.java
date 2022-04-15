package Zezex;

import Game.App;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.TaskCompletionSource;
import junit.framework.TestCase;

@SuppressWarnings("NewClassNamingConvention")
public class ModuleRedirectRank extends TestCase {
	public void testRedirect() throws Throwable {
		var app1 = App.Instance;
		var app2 = new App();

		app1.Start(new String[]{"-ServerId", "0"});
		app2.Start(new String[]{"-ServerId", "1", "-ProviderDirectPort", "20002"});

		System.out.println("Begin Thread.sleep");
		Thread.sleep(2000); // wait connected
		System.out.println("End Thread.sleep app1 " + app1.Zeze.getServiceManagerAgent().getSubscribeStates().values());
		System.out.println("End Thread.sleep app2 " + app1.Zeze.getServiceManagerAgent().getSubscribeStates().values());

		try {
			// RedirectToServer
			app1.Game_Rank.TestToServer(0, 111).then(result -> {
				assertEquals(111, result.out);
				assertEquals(0, result.serverId);
			}).Wait();

			app1.Game_Rank.TestToServer(1, 222).then(result -> {
				assertEquals(222, result.out);
				assertEquals(1, result.serverId);
			}).Wait();

			app2.Game_Rank.TestToServer(0, 333).then(result -> {
				assertEquals(333, result.out);
				assertEquals(0, result.serverId);
			}).Wait();

			app2.Game_Rank.TestToServer(1, 444).then(result -> {
				assertEquals(444, result.out);
				assertEquals(1, result.serverId);
			}).Wait();

			// RedirectHash
			app1.Game_Rank.TestHash(0, 555).then(result -> {
				assertEquals(0, result.hash);
				assertEquals(555, result.out);
				assertEquals(0, result.serverId);
			}).Wait();

			app1.Game_Rank.TestHash(1, 666).then(result -> {
				assertEquals(1, result.hash);
				assertEquals(666, result.out);
				assertEquals(1, result.serverId);
			}).Wait();

			app2.Game_Rank.TestHash(0, 777).then(result -> {
				assertEquals(0, result.hash);
				assertEquals(777, result.out);
				assertEquals(0, result.serverId);
			}).Wait();

			app2.Game_Rank.TestHash(1, 888).then(result -> {
				assertEquals(1, result.hash);
				assertEquals(888, result.out);
				assertEquals(1, result.serverId);
			}).Wait();

			// RedirectAll
			app1.Game_Rank.TestToAllConcLevel = 6;
			var future1 = new TaskCompletionSource<Boolean>();
			var hashes = new ConcurrentHashSet<Integer>();
			app1.Game_Rank.TestToAll(12345, ctx -> {
				assertFalse(ctx.isTimeout());
				var lastResult = ctx.getLastResult();
				var h = lastResult.getHash();
				var out = lastResult.out;
				System.out.println("TestToAll onResult: " + lastResult.getSessionId() + ", " + h + ", " + out);
				assertTrue(h >= 0 && h < app1.Game_Rank.TestToAllConcLevel);
				assertTrue(hashes.add(h));
				if (lastResult.getResultCode() == Procedure.Success)
					assertEquals(12345, out);
				else if (lastResult.getResultCode() == Procedure.Exception)
					assertEquals(0, out);
				if (ctx.isCompleted()) {
					try {
						var allResults = ctx.getAllResults();
						System.out.println("TestToAll onHashEnd: HashResults=" + allResults);
						assertEquals(app1.Game_Rank.TestToAllConcLevel, allResults.size());
						assertEquals(Procedure.Success, allResults.get(0).getResultCode()); // local
						assertEquals(Procedure.Success, allResults.get(1).getResultCode()); // remote
						assertEquals(Procedure.Exception, allResults.get(2).getResultCode()); // local exception
						assertEquals(Procedure.Exception, allResults.get(3).getResultCode()); // remote exception
						assertEquals(Procedure.Success, allResults.get(4).getResultCode()); // local async
						assertEquals(Procedure.Success, allResults.get(4).getResultCode()); // remote async
					} finally {
						future1.SetResult(true);
					}
				}
			});
			assertTrue(future1.get());
			assertEquals(app1.Game_Rank.TestToAllConcLevel, hashes.size());

			var future2 = new TaskCompletionSource<Boolean>();
			app2.Game_Rank.TestToAllConcLevel = 0;
			app2.Game_Rank.TestToAll(12345, ctx -> {
				if (ctx.isCompleted()) {
					System.out.println("TestToAll onHashEnd: HashResults=" + ctx.getAllResults());
					assertEquals(0, ctx.getAllResults().size());
					future2.SetResult(true);
				}
			});
			assertTrue(future2.get());
		} finally {
			System.out.println("Begin Stop");
			app1.Stop();
			app2.Stop();
			System.out.println("End Stop");
		}
	}
}
