package Zeze.Dbh2.Master;

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
import org.junit.jupiter.api.Test;

/**
 * FND20 GA-C05回归：endSplitWithRetryAsync对终局码eSplittingBucketNotFound必须停止重试。
 * 首次settle已成功、仅响应丢失（连接闪断→rpc超时）时30s后重发，master回eSplittingBucketNotFound
 * （splitting表中已无该桶），bug时对任何非零码一律再调度30s重试且零日志——每条丢失响应的
 * EndSplit/EndMove留下一条永不停止的30s自续链。修复=该码视为幂等完成停止重试（info日志），
 * 其余非零码保留重试但每次记warn。
 * 形态：本机回环桩master（真实rpc派发路径）+测试收缩重试间隔
 * （endRetryDelayMs为包内可见，本测试同包访问，对齐GA-D04 createTableRetryBudgetMs先例）。
 */
@Fast
public class TestFnd20GAC05EndSplitTerminalCodeStopsRetry {

	// 桩master服务：注册EndSplit协议，按脚本回码（脚本耗尽回0=成功），统计请求数。
	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger requests = new AtomicInteger();
		private final ArrayDeque<Integer> script;

		StubMasterService(int port, List<Integer> replyCodes) {
			super("stubMasterFnd20C05", stubServerConfig(port));
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
			config.getServiceConfMap().put("stubMasterFnd20C05", conf);
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

	@Test
	public void testAlreadySettledStopsRetry() throws Exception {
		Task.tryInitThreadPool();
		var savedDelay = MasterAgent.endRetryDelayMs;
		MasterAgent.endRetryDelayMs = 250; // 收缩重试间隔：bug时窗口内可见多条请求
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
			MasterAgent.endRetryDelayMs = savedDelay;
		}
	}

	@Test
	public void testOtherErrorRetriesUntilSuccess() throws Exception {
		Task.tryInitThreadPool();
		var savedDelay = MasterAgent.endRetryDelayMs;
		MasterAgent.endRetryDelayMs = 250;
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
			MasterAgent.endRetryDelayMs = savedDelay;
		}
	}
}
