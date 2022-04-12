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

		app1.Start(new String[]{ "-ServerId", "0" });
		app2.Start(new String[]{ "-ServerId", "1", "-ProviderDirectPort", "20002" });

		System.out.println("Begin Thread.sleep");
		Thread.sleep(2000); // wait connected
		System.out.println("End Thread.sleep app1 " + app1.Zeze.getServiceManagerAgent().getSubscribeStates().values());
		System.out.println("End Thread.sleep app2 " + app1.Zeze.getServiceManagerAgent().getSubscribeStates().values());

		try {
			var in = new Zeze.Util.OutInt();
			var serverId = new Zeze.Util.OutInt();

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

			// RedirectAll
			app1.Game_Rank.TestToAllConcLevel = 5;
			var future1 = new TaskCompletionSource<Boolean>();
			var hashes = new ConcurrentHashSet<Integer>();
			app1.Game_Rank.TestToAll(12345, (sid, h, out) -> {
				System.out.println("TestToAll onHashResult: " + sid + ", " + h + ", " + out);
				assertTrue(h >= 0 && h < 5);
				assertTrue(hashes.add(h));
				assertEquals(12345, out.intValue());
			}, ctx -> {
				try {
					System.out.println("TestToAll onHashEnd: HashResults=" + ctx.getHashResults());
					assertEquals(5, ctx.getHashResults().size());
					assertEquals(Procedure.Success, ctx.getHashResults().get(0).longValue());
					assertEquals(Procedure.Success, ctx.getHashResults().get(1).longValue());
					assertEquals(Procedure.Success, ctx.getHashResults().get(2).longValue());
					assertEquals(Procedure.Exception, ctx.getHashResults().get(3).longValue());
					assertEquals(Procedure.Success, ctx.getHashResults().get(4).longValue());
				} finally {
					future1.SetResult(true);
				}
			});
			assertTrue(future1.get());
			assertEquals(4, hashes.size()); // 还有1个因异常没有结果
			assertFalse(hashes.contains(3));

			var future2 = new TaskCompletionSource<Boolean>();
			app2.Game_Rank.TestToAllConcLevel = 0;
			app2.Game_Rank.TestToAll(12345, (sid, h, out) -> fail(), ctx -> {
				System.out.println("TestToAll onHashEnd: HashResults=" + ctx.getHashResults());
				assertEquals(0, ctx.getHashResults().size());
				future2.SetResult(true);
			});
			assertTrue(future2.get());
		} finally {
			app1.Stop();
			app2.Stop();
		}
	}
}
