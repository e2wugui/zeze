package Zeze.Collections;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import Zeze.Application;
import Zeze.BMyBean;
import Zeze.Config;
import org.junit.jupiter.api.Test;

public class TestQueueNames {

	@Test
	public void ordinaryAndServerQueuesCanShareBaseName() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setTakeoverMode("off");
		config.setServerId(7107);
		config.setDefaultTableConf(new Config.TableConf());
		var database = new Config.DatabaseConf();
		database.setDatabaseUrl("queue_names");
		config.getDatabaseConfMap().put("", database);
		var app = new Application("QueueNames", config);
		try {
			app.start();
			var module = app.getQueueModule();
			var ordinaryFirst = "ordinaryFirst";
			var serverFirst = "serverFirst";

			var ordinary = module.open(ordinaryFirst, BMyBean.class);
			var server = module.openCsQueue(ordinaryFirst, BMyBean.class);
			var reverseServer = module.openCsQueue(serverFirst, BMyBean.class);
			var reverseOrdinary = module.open(serverFirst, BMyBean.class);

			assertSame(ordinary, module.open(ordinaryFirst, BMyBean.class));
			assertSame(server, module.openCsQueue(ordinaryFirst, BMyBean.class));
			assertSame(reverseOrdinary, module.open(serverFirst, BMyBean.class));
			assertSame(reverseServer, module.openCsQueue(serverFirst, BMyBean.class));

			assertEquals(0, app.newProcedure(() -> {
				var first = new BMyBean();
				first.setI(1);
				ordinary.add(first);
				var second = new BMyBean();
				second.setI(2);
				server.add(second);
				assertEquals(1, requireNonNull(ordinary.poll()).getI());
				assertEquals(2, requireNonNull(server.poll()).getI());
				return 0;
			}, "queueNames").call());
		} finally {
			app.stop();
		}
	}
}
