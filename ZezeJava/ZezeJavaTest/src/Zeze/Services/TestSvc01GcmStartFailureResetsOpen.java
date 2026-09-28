package Zeze.Services;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Util.Task;

/**
 * FND13 svc-01：GCM 同步/异步版 start() 在 newServerSocket 失败后 open 残留 true，
 * 调用方按"启动失败→重试"模式再调 start() 被幂等早退静默吞掉（假成功），服务永不监听。
 * 修复后失败路径复位 open，重试语义正确。
 * <p>
 * 构造性场景：非法端口（70000，InetSocketAddress 构造即抛 IllegalArgumentException，跨平台确定性，
 * 规避 Windows 双方 setReuseAddress 下端口占用可能不失败的歧义）触发启动段失败 →
 * 断言 open 复位 false（修复前残留 true，本用例红）；再以合法端口重试 start() →
 * 必须真正建立监听（修复前静默空转，serverSocket 恒 null）。
 */
@Fast
public class TestSvc01GcmStartFailureResetsOpen {
	private static final int PORT_SYNC = 19731; // @Fast固定端口独占
	private static final int PORT_ASYNC = 19732;

	@Test
	public final void testSyncStartFailureThenRetryListens() throws Exception {
		Task.tryInitThreadPool();
		var ctor = GlobalCacheManagerServer.class.getDeclaredConstructor(); // 单例类，私有构造
		ctor.setAccessible(true);
		var gcm = ctor.newInstance();
		Assertions.assertThrows(RuntimeException.class, () -> gcm.start(null, 70000, null));
		Assertions.assertFalse(openOf(gcm), "启动失败后open必须复位false（残留true使重试start幂等早退假成功）");
		try {
			gcm.start(null, PORT_SYNC, null);
			Assertions.assertNotNull(serverSocketOf(gcm), "重试start必须真正建立监听（修复前静默空转）");
		} finally {
			gcm.stop();
		}
	}

	@Test
	public final void testAsyncStartFailureThenRetryListens() throws Exception {
		Task.tryInitThreadPool();
		var gcm = new GlobalCacheManagerAsyncServer();
		Assertions.assertThrows(RuntimeException.class, () -> gcm.start(null, 70000, null));
		Assertions.assertFalse(openOf(gcm), "启动失败后open必须复位false（残留true使重试start幂等早退假成功）");
		try {
			gcm.start(null, PORT_ASYNC, null);
			Assertions.assertNotNull(serverSocketOf(gcm), "重试start必须真正建立监听（修复前静默空转）");
		} finally {
			gcm.stop();
		}
	}

	/**
	 * R1-I01（svc-01的fix-the-fix）：失败路径必须拆除部分态。TcpSocket构造在bind前已对server
	 * 懒启动keepCheckTimer（KeepCheckPeriod>0时），只复位open会使后续stop()在!open早退失去
	 * server.stop()拆除路径，重试start替换server字段后旧Service的周期任务永久泄漏。
	 * 断言失败后server的keepCheckTimer已被取消置null（修复前非null，本用例红）。
	 */
	@Test
	public final void testStartFailureStopsKeepCheckTimer() throws Exception {
		Task.tryInitThreadPool();
		var config = new Zeze.Config();
		// ServerService未名单时回退复制defaultServiceConf的HandshakeOptions（Service.java:179）：
		// period=1启用TcpSocket构造内的懒启动。
		config.getDefaultServiceConf().getHandshakeOptions().setKeepCheckPeriod(1);
		var gcm = new GlobalCacheManagerAsyncServer();
		// 失败点必须在TcpSocket构造的bind段（懒启动之后）：非法端口在InetSocketAddress构造即抛、
		// 到不了TcpSocket；绑定TEST-NET保留地址192.0.2.1必抛"Cannot assign requested address"。
		var unassignable = java.net.InetAddress.getByName("192.0.2.1");
		Assertions.assertThrows(RuntimeException.class, () -> gcm.start(unassignable, 19733, config));
		Assertions.assertNull(keepCheckTimerOf(serverOf(gcm)),
				"失败server的keepCheckTimer必须被拆除取消（否则重试替换server后周期任务永久泄漏）");
		gcm.stop(); // 幂等收尾（open已false为no-op，仅保险）
	}

	private static Object serverOf(GlobalCacheManagerAsyncServer gcm) throws Exception {
		var field = GlobalCacheManagerAsyncServer.class.getDeclaredField("server");
		field.setAccessible(true);
		return field.get(gcm);
	}

	private static Object keepCheckTimerOf(Object service) throws Exception {
		var field = Zeze.Net.Service.class.getDeclaredField("keepCheckTimer");
		field.setAccessible(true);
		return field.get(service);
	}

	private static boolean openOf(Object gcm) throws Exception {
		var field = gcm.getClass().getDeclaredField("open");
		field.setAccessible(true);
		return field.getBoolean(gcm);
	}

	private static Object serverSocketOf(Object gcm) throws Exception {
		var field = gcm.getClass().getDeclaredField("serverSocket");
		field.setAccessible(true);
		return field.get(gcm);
	}
}
