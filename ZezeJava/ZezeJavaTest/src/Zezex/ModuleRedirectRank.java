package Zezex;

import Game.App;
import junit.framework.TestCase;

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

		} finally {
			app1.Stop();
			app2.Stop();
		}
	}
}
