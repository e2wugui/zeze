package UnitTest.Zeze.Services;

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
public class TestFnd13Svc01GcmStartFailureResetsOpen {
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
