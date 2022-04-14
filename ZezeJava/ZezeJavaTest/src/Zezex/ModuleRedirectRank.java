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
			var in = new Zeze.Util.OutInt();
			var serverId = new Zeze.Util.OutInt();

			// @formatter:off
			// RedirectToServer
			app1.Game_Rank.TestToServer(0, 12345, (i, s) -> { in.Value = i; serverId.Value = s; }).Wait();
			assert in.Value == 12345;
			assert serverId.Value == 0;

			app1.Game_Rank.TestToServer(1, 12345, (i, s) -> { in.Value = i; serverId.Value = s; }).Wait();
			assert in.Value == 12345;
			assert serverId.Value == 1;

			app2.Game_Rank.TestToServer(0, 12345, (i, s) -> { in.Value = i; serverId.Value = s; }).Wait();
			assert in.Value == 12345;
			assert serverId.Value == 0;

			app2.Game_Rank.TestToServer(1, 12345, (i, s) -> { in.Value = i; serverId.Value = s; }).Wait();
			assert in.Value == 12345;
			assert serverId.Value == 1;

			// RedirectHash
			var hash = new Zeze.Util.OutInt();
			app1.Game_Rank.TestHash(0, 12345, (h, i, s) -> { hash.Value = h; in.Value = i; serverId.Value = s;}).Wait();
			assert hash.Value == 0;
			assert in.Value == 12345;
			assert serverId.Value == 0;

			app1.Game_Rank.TestHash(1, 12345, (h, i, s) -> { hash.Value = h; in.Value = i; serverId.Value = s;}).Wait();
			assert hash.Value == 1;
			assert in.Value == 12345;
			assert serverId.Value == 1;

			app2.Game_Rank.TestHash(0, 12345, (h, i, s) -> { hash.Value = h; in.Value = i; serverId.Value = s;}).Wait();
			assert hash.Value == 0;
			assert in.Value == 12345;
			assert serverId.Value == 0;

			app2.Game_Rank.TestHash(1, 12345, (h, i, s) -> { hash.Value = h; in.Value = i; serverId.Value = s;}).Wait();
			assert hash.Value == 1;
			assert in.Value == 12345;
			assert serverId.Value == 1;
			// @formatter:on

			// RedirectAll
			app1.Game_Rank.TestToAllConcLevel = 5;
			var future1 = new TaskCompletionSource<Boolean>();
			var hashes = new ConcurrentHashSet<Integer>();
			app1.Game_Rank.TestToAll(12345, ctx -> {
				var lastResult = ctx.getLastResult();
				var h = lastResult.getHash();
				var out = lastResult.out;
				System.out.println("TestToAll onResult: " + lastResult.getSessionId() + ", " + h + ", " + out);
				assertTrue(h >= 0 && h < 5);
				assertTrue(hashes.add(h));
				if (lastResult.getResultCode() == Procedure.Success)
					assertEquals(12345, out);
				if (ctx.isCompleted()) {
					try {
						System.out.println("TestToAll onHashEnd: HashResults=" + ctx.getAllResults());
						assertEquals(5, ctx.getAllResults().size());
						assertEquals(Procedure.Success, ctx.getAllResults().get(0).getResultCode());
						assertEquals(Procedure.Success, ctx.getAllResults().get(1).getResultCode());
						assertEquals(Procedure.Success, ctx.getAllResults().get(2).getResultCode());
						assertEquals(Procedure.Exception, ctx.getAllResults().get(3).getResultCode());
						assertEquals(Procedure.Success, ctx.getAllResults().get(4).getResultCode());
					} finally {
						future1.SetResult(true);
					}
				}
			});
			assertTrue(future1.get());
			assertEquals(5, hashes.size());

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
