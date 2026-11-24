package Zeze.Component;

import Zeze.Application;
import Zeze.Arch.ProviderApp;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestApplicationRepeatedInitialization {
	@Test
	public void preservesTimerAndSafeBatchOnSecondInitialization() throws Exception {
		var conf = TakeoverTestEnv.newConf("off", 60_000, 30_000);
		var app = new Application("ApplicationRepeatedInitialization", conf);
		try {
			new ProviderApp(app);
			var appBase = new TakeoverTestEnv.TestAppBase(app);
			app.initialize(appBase);
			var timer = app.getTimer();
			var safeBatch = app.getSafeBatch();
			app.start();

			app.initialize(appBase);

			assertSame(timer, app.getTimer());
			assertSame(safeBatch, app.getSafeBatch());
		} finally {
			app.stop();
		}
	}
}
