package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManager.AllocateId;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * ServiceManagerServer曾在端口监听上线之后才发布回调依赖字段
 * （server/autoKeyTable等）——newServerSocket内部bind+OP_ACCEPT即上线（selector线程
 * 全局已运行），早到的连接在未认证TCP端口上Direct派发processAllocateId即读到
 * 未发布字段（数据竞争/NPE窗口）。修复后字段赋值先于newServerSocket。
 * <p>
 * 用例分两层：确定性部分——构造完成即握手+AllocateId必须成功；竞争部分——构造期内
 * 并发连接+分配（bind上线瞬间即有请求在飞，命中窗口与否取决于调度，修复后无论如何
 * 不得失败）。客户端形态对齐TestAllocateIdValidation（HandshakeClient裸协议）。
 */
@Fast
public class TestServiceManagerEarlyCallback {
	// 固定端口契约见TestTakeoverIdentifySuspect；26115避开26110-26114（SM同端口绑TCP+UDP）。
	private static final int PORT = 26115;

	private static ServiceManagerServer sm;
	private static Client client;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys")); // autokeys/已被gitignore；RocksDB需要父目录存在
		client = new Client("sm-early.client");
		client.start();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (client != null)
			client.stop();
		if (sm != null)
			sm.stop();
	}

	private static final class Client extends HandshakeClient {
		Client(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
					AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
		}
	}

	/** 已连接socket上分配一次；失败（含异常/超时/错误码）返回false。 */
	private static boolean allocOnce(AsyncSocket sock, String name) {
		try {
			var rpc = new AllocateId();
			rpc.Argument.setName(name);
			rpc.Argument.setCount(1);
			if (!rpc.SendForWait(sock, 5_000).await(5_000))
				return false;
			return !rpc.isTimeout() && rpc.getResultCode() == 0 && rpc.Result.getStartId() >= 1;
		} catch (Throwable e) {
			return false;
		}
	}

	@Test
	@Timeout(60)
	public void testFieldsPublishedBeforeListener() throws Exception {
		// 竞争线程用独立Client实例（独立Connector表）：与主线程的连接互不干扰，
		// 避免同Client下addConnector重复冲突。
		var racerClient = new Client("sm-early.racer");
		racerClient.start();
		var stop = new AtomicBoolean();
		var allocated = new AtomicInteger();
		var rpcFailures = new AtomicInteger(); // bind前连接失败不算（构造未完成）；已连接上的分配失败才算

		// 竞争线程：反复连接（bind前不断重试），连上后紧贴监听上线时刻连发AllocateId。
		// 修复前：请求若落在"已监听、字段未发布"窗口内，服务端NPE→断连→SendForWait失败。
		var racer = new Thread(() -> {
			while (!stop.get()) {
				var connector = new Connector("127.0.0.1", PORT, false);
				racerClient.getConfig().addConnector(connector);
				try {
					racerClient.start(); // addConnector不自动start：须start驱动建连（对齐族内connect形态）
					var sock = connector.WaitReady();
					while (!stop.get()) {
						if (allocOnce(sock, "smEarlyRace"))
							allocated.incrementAndGet();
						else
							rpcFailures.incrementAndGet();
					}
				} catch (Throwable e) {
					// bind前/构造中连接失败：重试（构造尚未完成属预期）
				} finally {
					racerClient.getConfig().removeConnector(connector);
					connector.stop();
				}
			}
		}, "sm-early-racer");
		racer.setDaemon(true);
		racer.start();
		try {
			// 构造期间racer在飞：bind瞬间即可有连接+握手+AllocateId
			sm = new ServiceManagerServer(null, PORT, new Zeze.Config(), "autokeys/smearly");

			// 确定性部分：构造完成即分配必须成功（字段已发布、监听已上线）
			var sock = connectOnce();
			Assertions.assertTrue(allocOnce(sock, "smEarlyFinal"),
					"构造完成后立即AllocateId必须成功");
			// racer的WaitReady节奏不确定：带deadline等待它完成至少一次分配（防抢跑假红）
			var deadline = System.currentTimeMillis() + 10_000;
			while (allocated.get() < 1 && System.currentTimeMillis() < deadline)
				//noinspection BusyWait
				Thread.sleep(20);
			Assertions.assertTrue(allocated.get() >= 1, "竞争线程至少成功分配一次");
			// 竞争窗口判定：已连接上的任何一次分配都不得失败（失败=NPE断连/处理异常）
			Assertions.assertEquals(0, rpcFailures.get(),
					"监听上线后的AllocateId不得失败：早到回调读到未发布字段（修复前NPE窗口）");
		} finally {
			stop.set(true);
			racer.join(5_000);
			racerClient.stop();
		}
	}

	/** 对齐TestAllocateIdValidation的有界重试连接（addConnector后须start驱动建连）。 */
	private AsyncSocket connectOnce() throws Exception {
		for (int attempt = 1; ; ++attempt) {
			var connector = new Connector("127.0.0.1", PORT, false);
			client.getConfig().addConnector(connector);
			client.start();
			try {
				return connector.WaitReady();
			} catch (Exception e) {
				if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
					throw e;
				client.getConfig().removeConnector(connector);
				connector.stop();
				//noinspection BusyWait
				Thread.sleep(200);
			}
		}
	}
}
