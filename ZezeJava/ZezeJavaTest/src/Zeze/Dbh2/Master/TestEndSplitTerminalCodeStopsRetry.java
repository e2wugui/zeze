package Zeze.Dbh2.Master;

import harness.Extra;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Acceptor;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * FND20 GA-C05回归：endSplitWithRetryAsync对终局码eSplittingBucketNotFound必须停止重试。
 * 首次settle已成功、仅响应丢失（连接闪断→rpc超时）时30s后重发，master回eSplittingBucketNotFound
 * （splitting表中已无该桶），bug时对任何非零码一律再调度30s重试且零日志——每条丢失响应的
 * EndSplit/EndMove留下一条永不停止的30s自续链。修复=该码视为幂等完成停止重试（info日志），
 * 其余非零码保留重试但每次记warn。
 * 形态：本机回环桩master（真实rpc派发路径）+测试收缩重试间隔。
 * endRetryDelayMs为修复引入的接缝字段，反射访问（对齐TestFnd20GBC01 trySetStopped优雅反射
 * 先例）：旧基线无此字段时测试仍可编译运行，由断言消息显式判红——基线红因=真实行为差异
 * （终局码仍在无限30s重试），不是字段缺失的链接红（R1增量审F-2修正）。
 */
@Fast
@Extra
public class TestEndSplitTerminalCodeStopsRetry {

	// 桩master服务：注册EndSplit协议，按脚本回码（脚本耗尽回0=成功），统计请求数。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger requests = new AtomicInteger();
		private final ArrayDeque<Integer> script;

		StubMasterService(int port, List<Integer> replyCodes) {
			super("stubMasterTerminalCode", stubServerConfig(port));
			setNoProcedure(true);
			script = new ArrayDeque<>(replyCodes);

			var fh = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndSplit.class, EndSplit.TypeId_);
			fh.Factory = EndSplit::new;
			fh.Handle = this::onEndSplit;
			fh.Level = TransactionLevel.None;
			fh.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndSplit.TypeId_, fh);
		}

		private long onEndSplit(EndSplit r) {
			requests.incrementAndGet();
			var rc = script.poll();
			r.SendResultCode(rc != null ? IModule.errorCode(AbstractMasterAgent.ModuleId, rc) : 0);
			return 0;
		}

		private static Config stubServerConfig(int port) {
			var conf = new ServiceConf();
			conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
			var config = new Config();
			config.getServiceConfMap().put("stubMasterTerminalCode", conf);
			return config;
		}
	}

	private static Config stubClientConfig(int port) {
		var conf = new ServiceConf();
		conf.addConnector(new Connector("127.0.0.1", port, true));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, conf);
		return config;
	}

	private static BBucketMeta.Data meta(Binary keyFirst, Binary keyLast) {
		var m = new BBucketMeta.Data();
		m.setDatabaseName("db1");
		m.setTableName("t1");
		m.setRaftConfig("");
		m.setKeyFirst(keyFirst);
		m.setKeyLast(keyLast);
		return m;
	}

	/** 反射取endRetryDelayMs接缝字段；旧基线（FND20 GA-C05修复不存在）无此字段。 */
	private static Field endRetryDelayField() throws NoSuchFieldException {
		var f = MasterAgent.class.getDeclaredField("endRetryDelayMs");
		f.setAccessible(true);
		return f;
	}

	/** 反射收缩重试间隔（bug时窗口内可见多条请求）；旧基线无此字段返回false（由断言消息判红）。 */
	private static boolean trySetEndRetryDelayMs(long ms) {
		try {
			endRetryDelayField().setLong(null, ms);
			return true;
		} catch (NoSuchFieldException e) {
			return false; // 旧代码（终局码不停重试）：反射形态保持可编译可运行，红因显式化
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAlreadySettledStopsRetry() throws Exception {
		Task.tryInitThreadPool();
		Assertions.assertTrue(trySetEndRetryDelayMs(250),
				"MasterAgent.endRetryDelayMs接缝缺失（FND20 GA-C05修复不存在）："
						+ "eSplittingBucketNotFound（幂等完成证据）仍被当可重试码无限30s重试，"
						+ "每条丢失响应的EndSplit/EndMove留下永不停止的自续重试链");
		var savedDelay = endRetryDelayField().getLong(null);
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port, List.of(
				AbstractMasterAgent.eSplittingBucketNotFound, AbstractMasterAgent.eSplittingBucketNotFound,
				AbstractMasterAgent.eSplittingBucketNotFound, AbstractMasterAgent.eSplittingBucketNotFound,
				AbstractMasterAgent.eSplittingBucketNotFound, AbstractMasterAgent.eSplittingBucketNotFound));
		server.start();
		var agent = new MasterAgent(stubClientConfig(port));
		agent.startAndWaitConnectionReady();
		try {
			agent.endSplitWithRetryAsync(meta(new Binary(new byte[]{2}), new Binary(new byte[]{5})),
					meta(new Binary(new byte[]{5}), Binary.Empty));
			// 窗口=4×重试间隔：bug时eSplittingBucketNotFound也无限续约（≥2条请求，红）。
			Thread.sleep(1000);
			Assertions.assertEquals(1, server.requests.get(),
					"eSplittingBucketNotFound（幂等完成证据）必须停止重试，不得形成30s自续链");
		} finally {
			agent.stop();
			server.stop();
			trySetEndRetryDelayMs(savedDelay);
		}
	}

	@Test
	public void testOtherErrorRetriesUntilSuccess() throws Exception {
		Task.tryInitThreadPool();
		// 本用例是"其余码保留重试"的回归守卫（新旧代码行为一致，bug时同样绿）：旧基线无接缝
		// 且30s重试节奏不可在@Fast窗口内观测，跳过而非判红——基线红因只来自testAlreadySettled
		// StopsRetry的终局码行为差异，红因保持单一。
		Assumptions.assumeTrue(trySetEndRetryDelayMs(250),
				"旧基线无endRetryDelayMs接缝（终局码停重试差异由testAlreadySettledStopsRetry判红）");
		var savedDelay = endRetryDelayField().getLong(null);
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		// eTableNotFound两次后成功：其余非零码保留重试契约（回归保护，bug时同样绿）。
		var server = new StubMasterService(port, List.of(
				AbstractMasterAgent.eTableNotFound, AbstractMasterAgent.eTableNotFound));
		server.start();
		var agent = new MasterAgent(stubClientConfig(port));
		agent.startAndWaitConnectionReady();
		try {
			agent.endSplitWithRetryAsync(meta(new Binary(new byte[]{2}), new Binary(new byte[]{5})),
					meta(new Binary(new byte[]{5}), Binary.Empty));
			var deadline = System.currentTimeMillis() + 5_000;
			while (server.requests.get() < 3 && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertEquals(3, server.requests.get(),
					"非终局码（eTableNotFound）必须保留重试，成功（rc=0）后停止");
		} finally {
			agent.stop();
			server.stop();
			trySetEndRetryDelayMs(savedDelay);
		}
	}
}
