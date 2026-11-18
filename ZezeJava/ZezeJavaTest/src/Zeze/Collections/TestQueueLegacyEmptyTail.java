package Zeze.Collections;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.Application;
import Zeze.BMyBean;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Config;
import org.junit.jupiter.api.Test;

public class TestQueueLegacyEmptyTail {

	@Test
	public void pushThenAddRebuildsBothEndsOfPreviouslyDrainedQueue() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setTakeoverMode("off");
		config.setServerId(7108);
		config.setDefaultTableConf(new Config.TableConf());
		var database = new Config.DatabaseConf();
		database.setDatabaseUrl("queue_legacy_empty_tail");
		config.getDatabaseConfMap().put("", database);
		var app = new Application("QueueLegacyEmptyTail", config);
		try {
			app.start();
			var name = "legacyEmptyTail";
			var queue = app.getQueueModule().open(name, BMyBean.class, 1);

			assertEquals(0, app.newProcedure(() -> {
				var root = queue.getOrAddRoot();
				root.setLastNodeId(1);
				root.setTailNodeKey(new BQueueNodeKey(name, 1));
				return 0;
			}, "queueLegacyEmptyTail.prepare").call());
			app.checkpointRun();

			assertEquals(0, app.newProcedure(() -> {
				assertTrue(queue.isEmpty());
				var first = new BMyBean();
				first.setI(1);
				queue.push(first);
				var root = queue.getOrAddRoot();
				assertEquals(root.getHeadNodeKey(), root.getTailNodeKey());
				var second = new BMyBean();
				second.setI(2);
				queue.add(second);
				assertEquals(1, requireNonNull(queue.poll()).getI());
				assertEquals(2, requireNonNull(queue.poll()).getI());
				assertTrue(queue.isEmpty());
				return 0;
			}, "queueLegacyEmptyTail.reuse").call());
		} finally {
			app.stop();
		}
	}
}
