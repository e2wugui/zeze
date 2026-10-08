package Zeze.Dbh2.Master;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
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
 * FND22 GA-C01回归（agent侧重试契约钉住）：宽方向拒绝码eSplittingStaleMain(8)必须落在
 * MasterAgent重试端的"非终局码保留重试"分支——重试链存活、不触发onSettled（不清pending-settle
 * 标志），成功（rc=0）后终局且onSettled恰执行一次。与master侧主用例
 * （TestWideRefusalRetryable：宽拒绝返回码4→8的行为差异）互补：本用例在新旧代码
 * 上均绿（非终局码重试是既有契约，FND20 GA-C05钉过eTableNotFound形态），钉住的是新码所依赖
 * 的承载面——任何码值≠eSplittingBucketNotFound都不得停链。
 * 形态对齐TestFnd20GAC05：本机回环桩master（真实rpc派发路径）+反射收缩endRetryDelayMs。
 */
@Fast
public class TestAgentRetriesOnStaleMain {

	// 修复码eSplittingStaleMain=8（不静态引用：未修基线无此常量，桩按值回码即可——agent侧
	// 只区分终局4/非终局其余，值本身由master侧用例钉死）。
	private static final int eStaleMainForScript = 8;

	private static final class StubMasterService extends Zeze.Net.Service {
		final AtomicInteger requests = new AtomicInteger();
		private final ArrayDeque<Integer> script;

		StubMasterService(int port, List<Integer> replyCodes) {
			super("stubMasterFnd22C01", stubServerConfig(port));
			setNoProcedure(true);
			script = new ArrayDeque<>(replyCodes);

			var fh = new Zeze.Net.Service.ProtocolFactoryHandle<>(EndMove.class, EndMove.TypeId_);
			fh.Factory = EndMove::new;
			fh.Handle = this::onEndMove;
			fh.Level = TransactionLevel.None;
			fh.Mode = DispatchMode.Normal;
			AddFactoryHandle(EndMove.TypeId_, fh);
		}

		private long onEndMove(EndMove r) {
			requests.incrementAndGet();
			var rc = script.poll();
			r.SendResultCode(rc != null ? IModule.errorCode(AbstractMasterAgent.ModuleId, rc) : 0);
			return 0;
		}

		private static Config stubServerConfig(int port) {
			var conf = new ServiceConf();
			conf.addAcceptor(new Acceptor(port, "127.0.0.1"));
			var config = new Config();
			config.getServiceConfMap().put("stubMasterFnd22C01", conf);
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

	private static Field endRetryDelayField() throws NoSuchFieldException {
		var f = MasterAgent.class.getDeclaredField("endRetryDelayMs");
		f.setAccessible(true);
		return f;
	}

	private static boolean trySetEndRetryDelayMs(long ms) {
		try {
			endRetryDelayField().setLong(null, ms);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 桩master先回两次eSplittingStaleMain（宽拒绝），第三次回0：必须形成3条请求（重试链存活
	 * 跨越宽拒绝），且onSettled只在rc=0终局执行一次（宽拒绝期间不得执行——那是终局回调，
	 * bug语义下它会清pending-settle标志拆掉补发源）。
	 */
	@Test
	public void testStaleMainRetriesUntilSuccessAndSettlesOnce() throws Exception {
		Task.tryInitThreadPool();
		Assumptions.assumeTrue(trySetEndRetryDelayMs(250),
				"旧基线无endRetryDelayMs接缝（FND20 GA-C05引入）：30s重试节奏不可在@Fast窗口内观测");
		var savedDelay = endRetryDelayField().getLong(null);
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var server = new StubMasterService(port, List.of(eStaleMainForScript, eStaleMainForScript));
		server.start();
		var agent = new MasterAgent(stubClientConfig(port));
		agent.startAndWaitConnectionReady();
		var settled = new AtomicInteger();
		try {
			agent.endMoveWithRetryAsync(meta(new Binary(new byte[]{2}), new Binary(new byte[]{5})),
					settled::incrementAndGet);
			var deadline = System.currentTimeMillis() + 5_000;
			while (server.requests.get() < 3 && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertEquals(3, server.requests.get(),
					"eSplittingStaleMain（宽拒绝、主表陈旧仍活）必须保留重试：2次拒绝+1次成功共3条请求");
			// 终局回调异步到达（rpc响应派发线程），等待后再断言恰一次。
			var settledDeadline = System.currentTimeMillis() + 5_000;
			while (settled.get() < 1 && System.currentTimeMillis() < settledDeadline)
				Thread.sleep(50);
			Assertions.assertEquals(1, settled.get(),
					"onSettled只在rc=0终局执行恰一次（宽拒绝期间执行=终局语义外溢，会清掉仍活迁移的补发源）");
			// 成功后停链：窗口内不再有新请求。
			Thread.sleep(750);
			Assertions.assertEquals(3, server.requests.get(), "成功（rc=0）后必须停止重试");
		} finally {
			agent.stop();
			server.stop();
			trySetEndRetryDelayMs(savedDelay);
		}
	}
}
