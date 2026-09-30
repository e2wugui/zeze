package Zeze.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Services.ServiceManager.Id128UdpClient;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManager.Tid128Cache;
import Zeze.Component.Threading;
import Zeze.Util.FuncLong;
import Zeze.Util.Id128;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;

/**
 * FND33 history-03 回归：段耗尽的续段取号必须整体发生在数据应用之前。修复前
 * beforeApply 只 future.get() 预热共享 cache（不保证余号），真正的 cache.next()
 * 在 afterApply（数据应用后）：预置仅 1 个号的号段被首笔事务耗尽后，第二笔事务
 * 的续段分配失败（SM 不可达）时数据已应用——Immediately 模式补刷数据后吞掉异常
 * 报假成功（Success），tHistory 缺行且 gid 未消费（键空间无空洞）、PendingGidLedger
 * 的 register 位于 next() 之后必然未执行——账本对自身主防场景失明。
 * 修复：cache.next()（含续段分配）前移到 beforeApply，失败=数据未应用即干净失败
 * （RejectHistoryAllocFailed→Closed）；取号成功立即入账，此后任何失败都留下账本痕迹。
 * 注入：与 TestTid128CacheFutureSelfHeal 同款手工控制 future——loopback Id128UdpServer
 * 但不启动 client 工作线程（future 保持 pending 由测试手动完成）；预置 count=1 号段后
 * 摘除 client 使续段分配同步抛错（确定性，不等真实 Udp 超时）。
 */
@Fast
public class TestHistorySegmentExhaustCleanFail {
	private static final long KEY = 1L;
	private static final long KEY2 = 2L;

	private static final int SERVER_ID = FastServerIds.TEST_HISTORY_SEGMENT_EXHAUST_CLEAN_FAIL;
	private static final String HISTORY_NAME = "UnitTest.SegmentExhaust";

	private static Id128UdpServer server;
	private static InjectedAgent agent;
	private static Id128UdpClient client;

	/** 手工控制的 Agent：future 由测试完成；detachClient 后任何新分配同步失败。 */
	private static final class InjectedAgent extends AbstractAgent {
		InjectedAgent() {
			config = new Config(); // 无global：getHistoryAllocCount()=0，对齐档位路径
		}

		void setTid128UdpClientForTest(Id128UdpClient client) {
			this.tid128UdpClient = client; // protected字段，子类内可访问
		}

		void detachClient() {
			this.tid128UdpClient = null; // 续段分配抛 IllegalStateException（确定性失败）
		}

		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() {
			// Application.start 在无 ServiceConf 时不调用；空实现兜底。
		}

		@Override
		public void waitReady() {
		}

		@Override
		public void editService(@NotNull BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(@NotNull BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		server = new Id128UdpServer();
		server.start();
		agent = new InjectedAgent();
		// client 不 start()：allocateFuture 创建的 future 保持 pending，由测试手动完成。
		client = new Id128UdpClient(agent, "127.0.0.1", server.getLocalPort(),
				new AtomicLong()::incrementAndGet);
		agent.setTid128UdpClientForTest(client);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		client.stop();
		server.stop();
	}

	private static Application newApp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable"); // Application 构造不建 Agent，随后反射注入手工控制实例
		config.setCheckpointMode(CheckpointMode.Immediately);
		config.setTakeoverMode("off");
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("history_segment_exhaust_" + SERVER_ID);
		config.getDatabaseConfMap().put("", dbConf);
		config.setHistory(HISTORY_NAME); // 启动即开启 history
		return new Application("TestHistorySegmentExhaust", config);
	}

	@Test
	public void testSegmentExhaustFailsCleanBeforeApply() throws Exception {
		// 预置仅 1 个号的号段（start=(0,0)，count=1）：首笔写事务耗尽它，第二笔必走续段分配。
		var planted = agent.allocateTid128CacheFuture(HISTORY_NAME, 0);
		Assertions.assertTrue(planted.setResult(new Tid128Cache(HISTORY_NAME, agent, new Id128(0, 0), 1)),
				"预置号段 future 必须成功完成");
		agent.detachClient(); // 续段分配自此同步失败（SM 不可达的确定性等价）

		Application app = newApp();
		// 注入手工 Agent（serviceManager 为构造期 final 字段，反射替换——仅测试装配面）。
		var field = Application.class.getDeclaredField("serviceManager");
		field.setAccessible(true);
		field.set(app, agent);
		var table = new tflush();
		app.addTable("", table);
		app.start();
		try {
			// 首笔：预置段内取号成功，事务成功且 tHistory 落行（gid=(0,0)）。
			Assertions.assertEquals(Procedure.Success, insert(app, table, KEY, 100), "预置段内取号的事务必须成功");
			Assertions.assertEquals(100, readValue(app, table, KEY));
			var historyTable = (Table)app.getHistoryModule().getHistoryTable();
			//noinspection DataFlowIssue
			var row = historyTable.getStorage().getDatabaseTable()
					.find(app.getHistoryModule().getHistoryTable(), new Id128(0, 0));
			Assertions.assertNotNull(row, "首笔事务的 tHistory 行必须已落库（Immediately 同步 flush）");

			// 第二笔：段耗尽的续段分配失败——必须数据未应用即干净失败（Closed）。
			// 修复前：beforeApply 只拿 cache 不取号，commit.run 应用数据，afterApply 的
			// cache.next() 失败经 Immediately 补刷吞异常——对外报 Success 且数据已生效
			//（tHistory 缺行、账本未登记，回放/对账双双无感）。
			Assertions.assertEquals(Procedure.Closed, insert(app, table, KEY2, 200),
					"段耗尽续段失败必须干净失败（修复前：补刷数据后吞异常报假成功 Success）");
			Assertions.assertEquals(-1, readValue(app, table, KEY2), "续段失败的事务数据不得应用（未应用即回滚）");
			Assertions.assertEquals(100, readValue(app, table, KEY), "此前已提交的数据不受影响");
		} finally {
			app.stop();
		}
	}

	private static long insert(Application app, tflush table, long key, long value) {
		var b = new BValue();
		b.setLong2(value);
		return app.newProcedure((FuncLong)() -> {
			table.insert(key, b);
			return 0L;
		}, "TestHistorySegmentExhaust.insert").call();
	}

	private static long readValue(Application app, tflush table, long key) {
		final var out = new long[]{-1};
		var rc = app.newProcedure((FuncLong)() -> {
			var v = table.get(key);
			if (v != null)
				out[0] = v.getLong2();
			return 0L;
		}, "TestHistorySegmentExhaust.read").call();
		Assertions.assertEquals(Procedure.Success, rc, "读事务不取号，必须成功");
		return out[0];
	}
}
