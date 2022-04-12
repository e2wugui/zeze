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
		Thread.sleep(5000); // wait connected
		System.out.println("End Thread.sleep");

		try {
			var in = new Zeze.Util.OutInt();
			var serverId = new Zeze.Util.OutInt();
			app1.Game_Rank.TestToServer(0, 12345, (i, s) -> { in.Value = i; serverId.Value = s; }).Wait();
			assert in.Value == 12345;
			assert serverId.Value == 0;

			app1.Game_Rank.TestToServer(1, 12345, (i, s) -> { in.Value = i; serverId.Value = s; }).Wait();
			assert in.Value == 12345;
			assert serverId.Value == 1;
		} finally {
			app1.Stop();
			app2.Stop();
		}
	}
}
