package Zezex;

import Game.App;
import junit.framework.TestCase;

public class ModuleRedirectRank extends TestCase {
	public void testRedirect() throws Throwable {
		var app1 = App.Instance;
		var app2 = new App();

		app1.Start(new String[]{ "-ServerId", "0" });
		app2.Start(new String[]{ "-ServerId", "1", "-ProviderDirectPort", "20002" });

		Thread.sleep(5000); // wait connected

		try {
			app1.Game_Rank.TestToServer(0, 12345, result -> { assert result == 12345; });
			app1.Game_Rank.TestToServer(1, 12345, result -> { assert result == 12345; });
		} finally {
			app1.Stop();
			app2.Stop();
		}
	}
}
