package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Fast
public class TestMemoryWalkCompletion {
	@Test
	public void erasedWalkForwardsCompletionAndResult() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("MemoryWalkCompletion", config);
		var table = new Table3();
		((Table)table).setTableConf(new Config.TableConf());
		var cache = new TableCache<>(app, table);
		var field = TableX.class.getDeclaredField("cache");
		field.setAccessible(true);
		field.set(table, cache);
		try {
			assertEquals(42, table.walkMemoryAny(new TableWalkHandle<>() {
				@Override
				public boolean handle(Object key, Bean value) {
					fail("empty cache must not yield records");
					return false;
				}

				@Override
				public long endWalk(long count) {
					assertEquals(0, count);
					return 42;
				}
			}));
		} finally {
			cache.close();
		}
	}
}
