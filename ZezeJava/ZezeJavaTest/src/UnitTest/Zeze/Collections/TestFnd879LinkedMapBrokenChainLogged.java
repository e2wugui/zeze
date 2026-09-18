package UnitTest.Zeze.Collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import UnitTest.Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;
import Zeze.Util.OutLong;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-79孪生回归：LinkedMap对"根声明链上有节点但节点行缺失"（数据损坏/外部篡改，
 * 正常事务不会产生）的三处静默——getFirstNode谎报空链、addTailUnsafe把尚存活链
 * 变成不可达孤岛、addHeadUnsafe静默绕过断头重建。修复：按Queue同款判据
 * （活链存在才告警）记error（带name/节点键/count），不抛ISE（乐观并发重试交错下
 * 瞬时行缺失是良性的），不自行修复损坏。
 * 伪造手法沿用TestLinkedMapBrokenData：绕过API直接删表行。
 */
@Fast
public class TestFnd879LinkedMapBrokenChainLogged {

	// 独占号段8791+（全景查号）：既与TestLinkedMapBrokenData的700+错开，也避开TestCheckpointRunThreadSentinel
	// 等既有的750族——FND8-26目录锁后同CWD撞号必炸（全量第一轮曾侥幸绿，第二轮并发交错即红）。
	private static final AtomicInteger NextServerId = new AtomicInteger(8790);

	private Application app;
	private LinkedMap.Module linkedMapModule;
	private Logger linkedMapLogger;
	private CapturingAppender appender;

	static final class CapturingAppender extends AbstractAppender {
		final List<LogEvent> events = new ArrayList<>();

		CapturingAppender() {
			super("TestFnd879Capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(@NotNull LogEvent event) {
			synchronized (events) {
				events.add(event.toImmutable());
			}
		}

		boolean hasErrorContaining(@NotNull String fragment) {
			synchronized (events) {
				return events.stream().anyMatch(e ->
						e.getLevel() == Level.ERROR && e.getMessage().getFormattedMessage().contains(fragment));
			}
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("a3_fnd879_linkedmap_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("a3TestFnd879_" + conf.getServerId(), conf);
		linkedMapModule = new LinkedMap.Module(app);
		app.start();

		linkedMapLogger = (Logger)LogManager.getLogger(LinkedMap.class);
		appender = new CapturingAppender();
		appender.start();
		linkedMapLogger.addAppender(appender);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (linkedMapLogger != null && appender != null)
			linkedMapLogger.removeAppender(appender);
		if (appender != null)
			appender.stop();
		if (app != null) {
			app.stop();
			app = null;
		}
	}

	@SuppressWarnings("unchecked")
	private static TableX<BLinkedMapNodeKey, BLinkedMapNode> tNodes(Application app) {
		return (TableX<BLinkedMapNodeKey, BLinkedMapNode>)app.getTable("Zeze_Builtin_Collections_LinkedMap_tLinkedMapNodes");
	}

	private void run(String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		assertEquals(0L, rc);
	}

	private long nodeIdOf(String mapName, String id) {
		final long[] out = {0};
		run("nodeIdOf", () -> {
			@SuppressWarnings("unchecked")
			var tIndex = (TableX<BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId>)app
					.getTable("Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId");
			var nodeId = tIndex.get(new BLinkedMapKey(mapName, id));
			assertTrue(nodeId != null, "前置：映射行必须存在");
			out[0] = nodeId.getNodeId();
			return 0L;
		});
		return out[0];
	}

	private void put(String mapName, String id, int v, boolean ahead) {
		run("put", () -> {
			var map = linkedMapModule.open(mapName, BMyBean.class, 1);
			var bean = new BMyBean();
			bean.setI(v);
			map.put(id, bean, ahead);
			return 0L;
		});
	}

	// 正控：链完好时getFirstNode/put不产生任何error。
	@Test
	public void testIntactChainNoError() throws Exception {
		put("ok1", "a", 1, true);
		var out = new OutLong();
		var nodeRef = new BLinkedMapNode[1];
		run("getFirstNode", () -> {
			out.value = 0;
			nodeRef[0] = (BLinkedMapNode)linkedMapModule.<BMyBean>open("ok1", BMyBean.class, 1).getFirstNode(out);
			return 0L;
		});
		assertTrue(nodeRef[0] != null && out.value != 0, "完好链getFirstNode返回头节点");
		put("ok1", "b", 2, false); // addTailUnsafe正常路径
		assertFalse(appender.hasErrorContaining("broken data"), "完好链不得告警");
	}

	// getFirstNode：头行缺失静默返null谎报空链——必须记error（Queue.peekNode孪生）。
	@Test
	public void testGetFirstNodeOnMissingHeadRowLogged() {
		put("g1", "a", 1, true);
		put("g1", "b", 2, true); // 头插：b是头
		var headNodeId = nodeIdOf("g1", "b");
		run("forgeDeleteHeadRow", () -> {
			tNodes(app).remove(new BLinkedMapNodeKey("g1", headNodeId));
			return 0L;
		});
		var out = new OutLong();
		var nodeRef = new BLinkedMapNode[1];
		run("getFirstNode", () -> {
			nodeRef[0] = (BLinkedMapNode)linkedMapModule.<BMyBean>open("g1", BMyBean.class, 1).getFirstNode(out);
			return 0L;
		});
		assertNull(nodeRef[0], "行为不变：仍返回null（不自行修复）");
		assertTrue(appender.hasErrorContaining("LinkedMap.getFirstNode: head node row missing"),
				"头行缺失必须记error留诊断线索");
	}

	// addTailUnsafe：活链存在（头键非0）而尾行缺失=真断链，另立新尾孤立活链——必须记error。
	@Test
	public void testAddTailOnMissingTailRowLogged() {
		put("t1", "a", 1, false); // 尾插：a在头
		put("t1", "b", 2, false); // b是尾
		var tailNodeId = nodeIdOf("t1", "b");
		run("forgeDeleteTailRow", () -> {
			tNodes(app).remove(new BLinkedMapNodeKey("t1", tailNodeId));
			return 0L;
		});
		put("t1", "c", 3, false); // addTailUnsafe走"另立新尾"分支
		assertTrue(appender.hasErrorContaining("LinkedMap.addTailUnsafe: tail node row missing"),
				"活链存在时尾行缺失必须记error");
	}

	// addHeadUnsafe：活链存在（尾键非0）而头行缺失=真断链，绕过断头重建孤立活链——必须记error。
	@Test
	public void testAddHeadOnMissingHeadRowLogged() {
		put("h1", "a", 1, true); // a在尾
		put("h1", "b", 2, true); // b是头
		var headNodeId = nodeIdOf("h1", "b");
		run("forgeDeleteHeadRow", () -> {
			tNodes(app).remove(new BLinkedMapNodeKey("h1", headNodeId));
			return 0L;
		});
		put("h1", "c", 3, true); // addHeadUnsafe走"绕过断头重建"分支
		assertTrue(appender.hasErrorContaining("LinkedMap.addHeadUnsafe: head node row missing"),
				"活链存在时头行缺失必须记error");
	}
}
