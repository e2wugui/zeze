package UnitTest.Zeze.Collections;

import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND2-C0-2/C0-5回归：伪造"索引有效但节点行缺失/邻接行缺失/空Values"的损坏数据
 * （参照TestQueueCompatible.forgeLegacyQueue的伪造手法：绕过API直接操作表行），
 * get/remove/removeNodeUnsafe/move必须抛带语义ISE而非裸NPE/NoSuchElementException；
 * walk对持久断链必须抛ISE而非静默返回部分计数。
 * 799e38ed1修了put/move入口但同族防护不完整，且无新增测试锁定行为。
 */
@Fast
public class TestLinkedMapBrokenData {

	// 与TestQueueCompatible的500+、TestDelayRemoveOnTimer的600+错开：
	// @Fast类并行时Application的本地缓存按serverId一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(700);

	private LinkedMap.Module linkedMapModule;

	private Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("linkedmap_broken_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestLinkedMapBrokenData" + conf.getServerId(), conf);
		// LinkedMap.Module必须在start之前注册（demo.App同序）
		linkedMapModule = new LinkedMap.Module(app);
		return app;
	}

	@SuppressWarnings("unchecked")
	private static TableX<BLinkedMapNodeKey, BLinkedMapNode> tNodes(Application app) {
		return (TableX<BLinkedMapNodeKey, BLinkedMapNode>)app.getTable("Zeze_Builtin_Collections_LinkedMap_tLinkedMapNodes");
	}

	@SuppressWarnings("unchecked")
	private static TableX<BLinkedMapKey, BLinkedMapNodeId> tIndex(Application app) {
		return (TableX<BLinkedMapKey, BLinkedMapNodeId>)app.getTable("Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId");
	}

	private static void run(Application app, String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		Assertions.assertEquals(0L, rc);
	}

	private static long nodeIdOf(Application app, String mapName, String id) {
		final long[] out = {0};
		run(app, "nodeIdOf", () -> {
			var nodeId = tIndex(app).get(new BLinkedMapKey(mapName, id));
			Assertions.assertNotNull(nodeId, "前置：映射行必须存在");
			out[0] = nodeId.getNodeId();
			return 0L;
		});
		return out[0];
	}

	// 事务内执行op（get/remove/move不声明checked异常，Runnable即可），断言抛带语义ISE。
	// 捕获后返回非0让事务回滚：伪造损坏路径上的半途写（removeNodeUnsafe分支）不落库。
	private static void assertBrokenIse(Application app, String what, Runnable op) {
		var caught = new Throwable[1];
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			try {
				op.run();
			} catch (Throwable e) {
				caught[0] = e;
				return 1L;
			}
			return 0L;
		}, what)).call();
		Assertions.assertEquals(1L, rc, what + " 应在损坏检测处中断并回滚");
		Assertions.assertTrue(caught[0] instanceof IllegalStateException,
				what + " 必须抛带语义ISE而非裸NPE，实际: " + caught[0]);
		Assertions.assertTrue(caught[0].getMessage() != null && caught[0].getMessage().contains("broken data"),
				what + " 异常消息必须可诊断（含broken data），实际: " + caught[0].getMessage());
	}

	// 索引有效（SerialNo匹配）但节点行缺失：get/remove必须抛ISE（原实现裸NPE）。
	@Test
	public void test1_GetRemoveOnMissingNodeRow() throws Exception {
		var app = newApp();
		try {
			app.start();
			var map = linkedMapModule.open("broken1", BMyBean.class, 1);
			run(app, "put", () -> {
				var bean = new BMyBean();
				bean.setI(11);
				return map.put("a", bean) == null ? 0L : 1L;
			});
			var nodeId = nodeIdOf(app, "broken1", "a");
			run(app, "forgeDeleteNodeRow", () -> {
				tNodes(app).remove(new BLinkedMapNodeKey("broken1", nodeId));
				return 0L;
			});
			assertBrokenIse(app, "get", () -> map.get("a"));
			assertBrokenIse(app, "remove", () -> map.remove("a"));
		} finally {
			app.stop();
		}
	}

	// 摘链时邻接节点行缺失：remove走到removeNodeUnsafe必须抛ISE（原实现裸NPE）。
	// nodeSize=1：put两次产生两个节点，删头节点行后remove尾节点值触发prev行缺失分支。
	@Test
	public void test2_RemoveNodeOnMissingNeighborRow() throws Exception {
		var app = newApp();
		try {
			app.start();
			var map = linkedMapModule.open("broken2", BMyBean.class, 1);
			run(app, "putA", () -> {
				var bean = new BMyBean();
				bean.setI(1);
				return map.put("a", bean) == null ? 0L : 1L;
			});
			run(app, "putB", () -> {
				var bean = new BMyBean();
				bean.setI(2);
				return map.put("b", bean) == null ? 0L : 1L;
			});
			var headNodeId = nodeIdOf(app, "broken2", "b"); // 头插：后put的是头
			run(app, "forgeDeleteHeadRow", () -> {
				tNodes(app).remove(new BLinkedMapNodeKey("broken2", headNodeId));
				return 0L;
			});
			// remove("a")清空尾节点values→removeNodeUnsafe：prev=head行已缺失→ISE
			assertBrokenIse(app, "removeNeighborMissing", () -> map.remove("a"));
		} finally {
			app.stop();
		}
	}

	// 节点行在但Values为空（另一形态损坏）：move必须抛ISE（原实现getFirst抛NoSuchElementException）。
	@Test
	public void test3_MoveOnEmptyValuesNode() throws Exception {
		var app = newApp();
		try {
			app.start();
			var map = linkedMapModule.open("broken3", BMyBean.class, 30);
			run(app, "put", () -> {
				var bean = new BMyBean();
				bean.setI(3);
				return map.put("c", bean) == null ? 0L : 1L;
			});
			var nodeId = nodeIdOf(app, "broken3", "c");
			run(app, "forgeEmptyValues", () -> {
				var node = tNodes(app).get(new BLinkedMapNodeKey("broken3", nodeId));
				Assertions.assertNotNull(node);
				node.getValues().clear();
				return 0L;
			});
			assertBrokenIse(app, "moveEmptyValues", () -> map.moveAhead("c"));
			assertBrokenIse(app, "moveTailEmptyValues", () -> map.moveTail("c"));
		} finally {
			app.stop();
		}
	}

	// FND2-C0-5：walk持久断链（root仍指向已删除的节点行）必须抛ISE，
	// 而不是静默endWalk(部分计数)——对账/导出拿偏低计数无任何错误信号。
	@Test
	public void test4_WalkPersistentBrokenChainThrows() throws Exception {
		var app = newApp();
		try {
			app.start();
			var map = linkedMapModule.open("broken4", BMyBean.class, 1);
			run(app, "put", () -> {
				var bean = new BMyBean();
				bean.setI(4);
				map.put("a", bean);
				var bean2 = new BMyBean();
				bean2.setI(5);
				map.put("b", bean2);
				return 0L;
			});
			// 摘链前walk正常走完
			Assertions.assertEquals(2L, map.walk((k, v) -> true));
			var headNodeId = nodeIdOf(app, "broken4", "b"); // 头插：b在头节点
			run(app, "forgeDeleteHeadRow", () -> {
				tNodes(app).remove(new BLinkedMapNodeKey("broken4", headNodeId));
				return 0L;
			});
			// root.head仍指向已删行：重启遍历后断在同一节点=持久断链→ISE（原实现静默返回0）
			Assertions.assertThrows(IllegalStateException.class, () -> map.walk((k, v) -> true));
		} finally {
			app.stop();
		}
	}
}
