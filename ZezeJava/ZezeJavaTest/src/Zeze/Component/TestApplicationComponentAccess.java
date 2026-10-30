package Zeze.Component;

import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.SafeBatch.BBatch;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestApplicationComponentAccess {

	private static Config newConf(String name) {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_APPLICATION_COMPONENT_ACCESS);
		conf.setTakeoverMode("off");
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("application_component_access_" + name + '_' + conf.getServerId());
		conf.getDatabaseConfMap().put("", dbConf);
		return conf;
	}

	private static void assertAllUnavailable(Application app) {
		assertTimerComponentsUnavailable(app);
		Assertions.assertThrows(NullPointerException.class, app::getQueueModule);
		Assertions.assertThrows(NullPointerException.class, app::getDelayRemove);
		Assertions.assertThrows(NullPointerException.class, app::getTakeover);
		Assertions.assertNull(app.getTakeoverOrNull());
		Assertions.assertThrows(NullPointerException.class, () -> app.getAutoKey("ids"));
	}

	private static void assertTimerComponentsUnavailable(Application app) {
		Assertions.assertThrows(NullPointerException.class, app::getTimer);
		Assertions.assertThrows(NullPointerException.class, app::getSafeBatch);
		Assertions.assertNull(app.getTimerOrNull());
		Assertions.assertNull(app.getSafeBatchOrNull());
	}

	@Test
	public void testDatabaseComponentsAvailableBeforeStartAndRemovedAfterStop() throws Exception {
		var app = new Application("ComponentAccessLifecycle", newConf("lifecycle"));
		try {
			Assertions.assertEquals(Application.StartState.eUninitialized, app.getStartState());
			Assertions.assertNotNull(app.getQueueModule());
			Assertions.assertNotNull(app.getDelayRemove());
			Assertions.assertNotNull(app.getTakeover());
			Assertions.assertSame(app.getTakeover(), app.getTakeoverOrNull());
			Assertions.assertEquals("off", app.getTakeover().getMode());
			Assertions.assertSame(app.getAutoKey("ids"), app.getAutoKey("ids"));
			app.start();
		} finally {
			app.stop();
		}
		Assertions.assertEquals(Application.StartState.eStopped, app.getStartState());
		assertAllUnavailable(app);
	}

	@Test
	public void testTimerAndSafeBatchRequireInitializationWithRedirect() throws Exception {
		var app = new Application("ComponentAccessTimer", newConf("timer"));
		try {
			assertTimerComponentsUnavailable(app);

			var appBase = new TakeoverTestEnv.TestAppBase(app);
			app.initialize(appBase);
			assertTimerComponentsUnavailable(app);

			new ProviderApp(app);
			app.initialize(appBase);
			var timer = app.getTimer();
			var safeBatch = app.getSafeBatch();
			Assertions.assertSame(timer, app.getTimerOrNull());
			Assertions.assertSame(safeBatch, app.getSafeBatchOrNull());
			app.start();
			Assertions.assertSame(timer, app.getTimer());
			Assertions.assertSame(safeBatch, app.getSafeBatch());
		} finally {
			app.stop();
		}
		assertAllUnavailable(app);
	}

	@Test
	public void testBatchTimerSkipsApplicationWithoutSafeBatch() throws Exception {
		var app = new Application("ComponentAccessBatchTimer", newConf("batchTimer"));
		var timer = new Timer(new TakeoverTestEnv.TestAppBase(app));
		try {
			app.start();
			Assertions.assertNull(app.getSafeBatchOrNull());
			var batch = new BBatch();
			batch.setAppInstanceId(app.getProjectName());
			var context = new TimerContext(timer, "missingSafeBatch", "", batch, 1, 0, 0);
			new SafeBatch.BatchTimerHandle().onTimer(context);
		} finally {
			app.stop();
		}
	}

	@Test
	public void testNoDatabaseHasNoComponentsEvenWithRedirect() throws Exception {
		var conf = newConf("noDatabase");
		conf.setNoDatabase(true);
		var app = new Application("ComponentAccessNoDatabase", conf);
		try {
			new ProviderApp(app);
			app.initialize(new TakeoverTestEnv.TestAppBase(app));
			assertAllUnavailable(app);
			app.start();
			assertAllUnavailable(app);
		} finally {
			app.stop();
		}
		assertAllUnavailable(app);
	}

	@Test
	public void testNegativeServerIdDisablesDatabaseComponents() throws Exception {
		var conf = newConf("negativeServerId");
		conf.setServerId(-1);
		var app = new Application("ComponentAccessNegativeServerId", conf);
		try {
			Assertions.assertTrue(app.isNoDatabase());
			assertAllUnavailable(app);
			app.start();
			assertAllUnavailable(app);
		} finally {
			app.stop();
		}
	}
}
