package Zeze.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.BMyBean;
import Zeze.Collections.CHashMap;
import Zeze.Collections.CsQueue;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutLong;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestPersistentCollectionIdentity {
	private static BMyBean value(int i) {
		var bean = new BMyBean();
		bean.setI(i);
		return bean;
	}

	@Test
	public void splicedNodesWithEqualIdsKeepTheActualTailForPollAndPollNode() throws Exception {
		var app = new Application("QueueFullNodeIdentity", TakeoverTestEnv.newConf("off", 600_000, 600_000));
		try {
			app.start();
			for (var wholeNode : List.of(false, true)) {
				var name = "full-key-" + wholeNode;
				var live = new CsQueue<>(app.getQueueModule(), name, app.getConfig().getServerId(), BMyBean.class, 1);
				var deadId = app.getConfig().getServerId() + 20_000;
				var dead = new CsQueue<>(app.getQueueModule(), name, deadId, BMyBean.class, 1);
				assertEquals(0, app.newProcedure(() -> {
					live.add(value(2));
					dead.add(value(1));
					return 0;
				}, "seed").call());
				live.splice(deadId, 0);
				assertEquals(0, app.newProcedure(() -> {
					if (wholeNode)
						assertNotNull(live.pollNode());
					else
						assertEquals(1, live.poll().getI());
					live.push(value(3));
					live.add(value(4));
					assertEquals(3, live.size());
					assertEquals(3, live.poll().getI());
					assertEquals(2, live.poll().getI());
					assertEquals(4, live.poll().getI());
					assertNull(live.poll());
					return 0;
				}, "consume-spliced").call());
			}
		} finally {
			app.stop();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void bothConcurrentOverloadsHonorThePersistedLegacyLayout() throws Exception {
		var app = new Application("ConcurrentLayout", TakeoverTestEnv.newConf("off", 600_000, 600_000));
		var module = new LinkedMap.Module(app);
		try {
			app.start();
			var key = "key";
			for (var i = 0; Integer.remainderUnsigned(ByteBuffer.calc_hashnr(key), 256) < 128; i++)
				key = "key" + i;
			final var highKey = key;
			var fresh = module.openConcurrent("fresh", BMyBean.class, 20);
			assertEquals(0, app.newProcedure(() -> { fresh.put(highKey, value(7)); return 0; }, "put-fresh").call());
			var cacheField = LinkedMap.Module.class.getDeclaredField("linkedMaps");
			cacheField.setAccessible(true);
			var cache = (ConcurrentHashMap<String, Object>)cacheField.get(module);
			cache.remove("fresh");
			var reopenedFresh = module.openConcurrent("fresh", BMyBean.class);
			assertEquals(0, app.newProcedure(() -> { assertEquals(7, reopenedFresh.get(highKey).getI()); return 0; }, "read-fresh").call());

			// 只伪造升级前的公开布局：真实LinkedMap.put产生旧256桶业务记录，没有元数据。
			var openBucket = LinkedMap.Module.class.getDeclaredMethod("_open", String.class, Class.class, int.class);
			openBucket.setAccessible(true);
			var bucketId = Integer.remainderUnsigned(ByteBuffer.calc_hashnr(highKey), 256);
			var bucket = (LinkedMap<BMyBean>)openBucket.invoke(module, "legacy@" + bucketId, BMyBean.class, 30);
			assertEquals(0, app.newProcedure(() -> { bucket.put(highKey, value(9)); return 0; }, "legacy-put").call());
			assertThrows(IllegalStateException.class, () -> module.openConcurrent("legacy", BMyBean.class));
			assertThrows(IllegalStateException.class, () -> module.adoptLegacyConcurrentLayout("legacy", 128));
			module.adoptLegacyConcurrentLayout("legacy", 256);
			CHashMap<BMyBean> first = module.openConcurrent("legacy", BMyBean.class);
			assertEquals(0, app.newProcedure(() -> { assertEquals(9, first.get(highKey).getI()); return 0; }, "legacy-read").call());
			cache.remove("legacy");
			var second = module.openConcurrent("legacy", BMyBean.class, 20);
			assertEquals(0, app.newProcedure(() -> { assertEquals(9, second.get(highKey).getI()); return 0; }, "legacy-read-other-overload").call());
		} finally {
			app.stop();
		}
	}


}
