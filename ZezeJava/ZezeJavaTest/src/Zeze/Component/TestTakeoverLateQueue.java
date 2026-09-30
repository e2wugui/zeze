package Zeze.Component;

import Zeze.Application;
import Zeze.BMyBean;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Builtin.Collections.Queue.BQueueNodeValue;
import Zeze.Transaction.TableX;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTakeoverLateQueue {
	@Test
	@SuppressWarnings("unchecked")
	public void aLateScopeRecoversAnExistingLeaseTombstoneAndRepeatedTransferIsIdempotent() throws Exception {
		var app = new Application("TakeoverLateQueue", TakeoverTestEnv.newConf("on", 600_000, 600_000));
		try {
			app.start();
			app.getQueueModule().open("register-value", BMyBean.class);
			var roots = (TableX<String, BQueue>)app.getTable("Zeze_Builtin_Collections_Queue_tQueues");
			var nodes = (TableX<BQueueNodeKey, BQueueNode>)app.getTable("Zeze_Builtin_Collections_Queue_tQueueNodes");
			var deadId = app.getConfig().getServerId() + 20_000;
			var deadName = "late@" + deadId;
			assertEquals(0, app.newProcedure(() -> {
				var root = roots.getOrAdd(deadName);
				var key = new BQueueNodeKey(deadName, 1);
				root.setHeadNodeKey(key);
				root.setTailNodeKey(key);
				root.setCount(1);
				root.setLoadSerialNo(3);
				var value = new BQueueNodeValue();
				var bean = new BMyBean();
				bean.setI(42);
				value.getValue().setBean(bean);
				nodes.getOrAdd(key).getValues().add(value);
				return 0;
			}, "forge-unregistered-scope").call());
			TakeoverTestEnv.forgeLease(app, deadId, 3, 0); // 旧进程完成其他scope后留下的持久墓碑。
			var live = app.getQueueModule().openCsQueue("late", BMyBean.class);
			TakeoverTestEnv.waitTryTransferQueue();
			app.getTakeover().tryTransfer(deadId);
			TakeoverTestEnv.waitTryTransferQueue();
			assertEquals(0, app.newProcedure(() -> {
				assertEquals(1, live.size());
				assertEquals(42, live.poll().getI());
				assertNull(live.poll());
				assertEquals(0, roots.get(deadName).getCount());
				return 0;
			}, "verify-once").call());
		} finally {
			app.stop();
		}
	}
}
