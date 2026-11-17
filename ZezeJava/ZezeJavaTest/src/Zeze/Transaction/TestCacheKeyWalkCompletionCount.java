package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestCacheKeyWalkCompletionCount {
	@Test
	public void interruptedWalkCountsDeliveredKeyAndForwardsCompletionResult() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("CacheKeyWalkCompletionCount", config);
		var table = new Table3();
		((Table)table).setTableConf(new Config.TableConf());
		var cache = new TableCache<>(app, table);
		try {
			cache.getOrAdd(1L, () -> new Record1<>(table, 1L, null));
			cache.getOrAdd(2L, () -> new Record1<>(table, 2L, null));
			assertEquals(42, cache.walkKey(new TableWalkKey<>() {
				@Override
				public boolean handle(Long key) {
					return false;
				}

				@Override
				public long endWalk(long count) {
					assertEquals(1, count);
					return 42;
				}
			}));
			assertEquals(2, cache.walkKey(key -> true));
		} finally {
			cache.close();
		}
	}
}
