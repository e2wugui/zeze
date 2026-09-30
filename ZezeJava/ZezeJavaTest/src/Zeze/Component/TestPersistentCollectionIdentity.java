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




}
