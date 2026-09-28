package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManager.AllocateId;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManager.Tid128Cache;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND15 svc-01 红绿钉板（非raft版）：SM TCP AllocateId 端点对客户端可控 name 的
 * 无界驻留防护（判例移植自同端口UDP面 Id128UdpServer/FND6-28）。
 * <p>
 * 用例1（红绿双向）：入口校验——count<=0/超上限/超长name 返回 ErrorRequestId（修复前
 * count<0被修正为1照常成功、无任何校验）；合法请求不受影响。
 * 用例2（绿侧不变式守护）：唯一名满员后逐出闲置自愈（新name仍成功），且全量重分配
 * 不重号——逐出仅回收CHM内存，重建时current向前重置到持久max（烧号洞不重发）。
 * 该不变式守护的是"防护自身不引入重号"（持锁+在册复核协议），修复前无逐出无可守护面。
 * <p>
 * 走真实TCP派发路径（IO线程Direct模式），客户端对齐族内HandshakeClient裸协议形态。
 */
@Fast
public class TestSvc01AllocateIdValidation {
	// 固定端口契约与选段说明见TestTakeoverIdentifySuspect；26113避开26110/26111/26112
	//（SM在同端口号绑TCP+UDP，本测试只触碰TCP面；UDP面的同构防护由TestId128UniqueNamesLimit钉板）。
	private static final int PORT = 26113;
	private static ServiceManagerServer sm;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys")); // autokeys/已被gitignore；RocksDB需要父目录存在
		sm = new ServiceManagerServer(null, PORT, new Zeze.Config(), "autokeys/fnd15-svc01");
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (sm != null)
			sm.stop();
	}

	private static final class Client extends HandshakeClient {
		Client(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
					AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect() throws Exception {
			// WaitReady固定5s超时：loopback connect偶发SYN无应答（族内Peer.connect的负载加固），
			// 有界重试：失败连接remove+stop后重建。
			for (int attempt = 1; ; ++attempt) {
				var connector = new Connector("127.0.0.1", PORT, false);
				getConfig().addConnector(connector);
				start();
				try {
					return connector.WaitReady();
				} catch (Exception e) { // 超时经Task.forceThrow sneaky-throw受检TimeoutException，编译期不可见
					if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
						throw e;
					getConfig().removeConnector(connector);
					connector.stop();
					//noinspection BusyWait
					Thread.sleep(200);
				}
			}
		}
	}

	/** 发送并等待应答到达（不断言resultCode，由调用方按预期断言）。 */
	private static AllocateId alloc(AsyncSocket sock, String name, int count) throws Exception {
		var rpc = new AllocateId();
		rpc.Argument.setName(name);
		rpc.Argument.setCount(count);
		Assertions.assertTrue(rpc.SendForWait(sock, 30_000).await(30_000), "alloc await: " + name);
		Assertions.assertFalse(rpc.isTimeout(), "alloc timeout: " + name);
		return rpc;
	}

	@Test
	@Timeout(120)
	public void testValidationReject() throws Exception {
		var client = new Client("UnitTest.Fnd15Svc01.Client");
		try {
			var sock = client.connect();
			Assertions.assertEquals(Procedure.ErrorRequestId, alloc(sock, "fnd15svc01-count0", 0).getResultCode(),
					"count=0必须拒绝");
			Assertions.assertEquals(Procedure.ErrorRequestId,
					alloc(sock, "fnd15svc01-countmax", Tid128Cache.ALLOCATE_COUNT_MAX + 1).getResultCode(),
					"count超上限必须拒绝");
			Assertions.assertEquals(Procedure.ErrorRequestId,
					alloc(sock, "x".repeat(129), 1).getResultCode(), "name超128字节必须拒绝");

			var ok = alloc(sock, "fnd15svc01-ok", 100);
			Assertions.assertEquals(0, ok.getResultCode(), "合法请求不受校验影响");
			Assertions.assertEquals(100, ok.Result.getCount());
			Assertions.assertTrue(ok.Result.getStartId() >= 1, "合法startId必须有效");
		} finally {
			client.stop();
		}
	}

	@Test
	@Timeout(180)
	public void testUniqueNamesBoundedEvictNoDup() throws Exception {
		var client = new Client("UnitTest.Fnd15Svc01.Client2");
		try {
			var sock = client.connect();
			var max = Id128UdpServer.MAX_UNIQUE_NAMES;

			// 填满上限：MAX个不同name全部成功，记录各自首次区间起点。
			var firstStart = new HashMap<String, Long>();
			for (int i = 0; i < max; i++) {
				var name = "fnd15svc01-name-" + i;
				var rpc = alloc(sock, name, 1);
				Assertions.assertEquals(0, rpc.getResultCode(), "填满阶段name必须全部成功: " + name);
				firstStart.put(name, rpc.Result.getStartId());
			}

			// 满员自愈：新name触发逐出闲置（全部条目无并发持锁，逐出必然成功）腾位成功。
			var overflow = alloc(sock, "fnd15svc01-overflow", 1);
			Assertions.assertEquals(0, overflow.getResultCode(), "满员后新name必须经逐出自愈成功");
			Assertions.assertTrue(overflow.Result.getStartId() >= 1);

			// 不重号不变式：全部name（含被逐出后重建的）再分配，新区间起点必须越过旧区间终点
			//（未逐出的自然接续+1；被逐出的从持久max向前重置，跳号洞）。
			for (int i = 0; i < max; i++) {
				var name = "fnd15svc01-name-" + i;
				var rpc = alloc(sock, name, 1);
				Assertions.assertEquals(0, rpc.getResultCode(), "既有name必须始终可用: " + name);
				Assertions.assertTrue(rpc.Result.getStartId() >= firstStart.get(name) + 1,
						"重分配不得重号: " + name + " first=" + firstStart.get(name)
								+ " second=" + rpc.Result.getStartId());
			}
			var ov2 = alloc(sock, "fnd15svc01-overflow", 1);
			Assertions.assertEquals(0, ov2.getResultCode());
			Assertions.assertTrue(ov2.Result.getStartId() >= overflow.Result.getStartId() + 1,
					"overflow重分配不得重号");
		} finally {
			client.stop();
		}
	}
}
