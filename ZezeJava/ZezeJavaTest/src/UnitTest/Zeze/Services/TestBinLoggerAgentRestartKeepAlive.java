package UnitTest.Zeze.Services;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Files;

import Zeze.Net.Service;
import Zeze.Services.BinLogger;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-62 回归：start(host,port) 手工 connector 路径不调 super.start()，stop()（经
 * super.stop() 置 keepAliveCheckStopped 熔断）后重启无任何复位点——keepalive 定时器
 * 永久熔断，构造器配置的 5s检查/60s收/30s发超时全部失效，静默死链不再被检测。
 * 修复后 start(host,port) 在 connector.start() 前调 super.start() 复位熔断。
 * 断言直接观察 Service 的 keepCheckTimer/keepAliveCheckStopped 内部态（TcpSocket
 * 构造内同步调用 tryStartKeepAliveCheckTimer，start 返回即可判定，无需等待时序）。
 * TokenClient 为同一行级改法的精确孪生（FND8-62修复方案明确纳入），同用例覆盖。
 */
@Fast
public class TestBinLoggerAgentRestartKeepAlive {
	private static final Field KEEP_CHECK_TIMER = serviceField("keepCheckTimer");
	private static final Field KEEP_ALIVE_CHECK_STOPPED = serviceField("keepAliveCheckStopped");

	private static Field serviceField(String name) {
		try {
			var f = Service.class.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static int probePort() throws IOException {
		for (int port = 28560; port < 28599; port++) {
			try (var ignore = new ServerSocket(port)) { // 绑定成功即空闲；失败抛BindException继续
				return port;
			} catch (IOException e) {
				// 端口被占，探测下一个
			}
		}
		throw new IOException("no free port in [28560,28599)");
	}

	@Test
	public void testTokenClientRestartResetsFuse() throws Exception {
		Task.tryInitThreadPool();
		var port = probePort(); // 无监听也不妨：TcpSocket构造先于连接尝试同步调用tryStartKeepAliveCheckTimer
		var client = new Zeze.Services.Token.TokenClient(new Zeze.Config());
		try {
			// 从未启动的实例误调stop()也置熔断（stop经super.stop()无条件置位）
			client.stop();
			Assertions.assertTrue((Boolean)KEEP_ALIVE_CHECK_STOPPED.get(client), "stop应置熔断");

			// 重启（孪生修复点）：熔断必须复位并重建定时器；连接失败由autoReconnect后台重试
			client.start("127.0.0.1", port);
			Assertions.assertFalse((Boolean)KEEP_ALIVE_CHECK_STOPPED.get(client), "TokenClient重启必须复位熔断");
			Assertions.assertNotNull(KEEP_CHECK_TIMER.get(client), "TokenClient重启后keepalive定时器必须重建");
		} finally {
			try {
				client.stop();
			} catch (Throwable ignored) {
			}
		}
	}

	@Test
	public void testRestartResetsKeepAliveFuse() throws Exception {
		Task.tryInitThreadPool();
		var port = probePort();
		var dir = Files.createTempDirectory("a5-binlogger-agent-keepalive");
		var service = new BinLogger.BinLoggerService(dir.toString());
		service.start("127.0.0.1", port);
		var agent = new BinLogger.BinLoggerAgent();
		try {
			// 首启：手工connector路径靠懒启动兜底创建keepalive定时器
			agent.start("127.0.0.1", port);
			agent.waitReady();
			Assertions.assertNotNull(KEEP_CHECK_TIMER.get(agent), "首启懒启动应创建keepalive定时器");
			Assertions.assertFalse((Boolean)KEEP_ALIVE_CHECK_STOPPED.get(agent));

			// 停机：定时器取消并熔断懒启动
			agent.stop();
			Assertions.assertNull(KEEP_CHECK_TIMER.get(agent));
			Assertions.assertTrue((Boolean)KEEP_ALIVE_CHECK_STOPPED.get(agent), "stop应置熔断");

			// 重启（修复点）：熔断必须复位，定时器随新连接重建；修复前此断言失败（定时器永不再建）
			agent.start("127.0.0.1", port);
			agent.waitReady();
			Assertions.assertFalse((Boolean)KEEP_ALIVE_CHECK_STOPPED.get(agent), "重启必须复位熔断");
			Assertions.assertNotNull(KEEP_CHECK_TIMER.get(agent), "重启后keepalive定时器必须重建");
		} finally {
			try {
				agent.stop();
			} catch (Throwable ignored) {
			}
			service.stop();
		}
	}
}
