package Zeze.History;

import harness.Extra;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import Zeze.Application;
import Zeze.Config;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HistoryModule.startHttpServer 必须同步确认 bind 结果且失败不闩死幂等分支：
 * HttpServer.start 对 b.bind 的 ChannelFuture 不同步不记日志，bind 失败异步发生在
 * event loop 上——原实现丢弃返回值且 httpServer 字段先于 start 成功赋值，端口冲突
 * 等失败静默（startServer 的 info 日志已无条件打出、端点缺失零错误日志），且失败后
 * 字段闩死，重试命中"skip duplicate start"静默空转，仅重启进程可恢复。修复后对齐
 * Netty.java 的 .sync() 规范与 zokermanager startAdminHttpServer 先例：await bind
 * future、失败回收半启动 server 后抛含 cause 的 IllegalStateException、字段成功后
 * 发布——修复端口后重调即进入。host 参数供直测注入坏地址（TEST-NET-1 确定性
 * BindException；同机端口冲突走同一 await/isSuccess 路径，但 Windows SO_REUSEADDR
 * 语义使占口复测不稳定，不做直测面）。
 */
@Fast
@Extra
public class TestHistoryHttpServerBindFailureFailsFast {
	// 独立serverId+派生url：@Fast类并行时避免zeze_cache目录与DatabaseMemory同名url互撞。
	private static final int SERVER_ID = FastServerIds.TEST_HISTORY_HTTP_SERVER_BIND_FAILURE;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("history_http_bind_" + SERVER_ID);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestHistoryHttpServerBindFailureFailsFast", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.getHistoryModule().stop();
		app.stop();
	}

	/**
	 * bind 失败（坏地址）必须显式抛出而非静默返回；失败不闩死幂等分支——换可绑地址
	 * 重试必须真正起服务（WalkPage 端点可访问），再调一次幂等跳过（不抛不换端口）。
	 */
	@Test
	public void bindFailureThrowsAndRetryAfterFixIsNotLatched() throws Exception {
		Task.tryInitThreadPool();
		var hm = app.getHistoryModule();
		try (var netty = new Netty()) {
			// 红①：坏地址（TEST-NET-1，本机必不存在）bind 必失败——原实现静默返回。
			var ex = assertThrows(IllegalStateException.class,
					() -> hm.startHttpServer(netty, "192.0.2.1", 0),
					"bind 失败必须显式抛出（bug：静默返回、零错误日志、端点缺失无感）");
			assertEquals(BindException.class, ex.getCause().getClass(),
					"cause 保留底层 BindException 便于诊断: " + ex.getCause());

			// 红②：失败不得闩死幂等分支——原实现字段已赋值，重试命中 skip 分支静默空转。
			int port;
			try (var ss = new ServerSocket(0)) {
				port = ss.getLocalPort();
			}
			hm.startHttpServer(netty, null, port); // 重试必须真正起服务（bug：静默跳过）
			assertEquals(200, httpGetStatus(port, "/Zeze/Builtin/HistoryModule/WalkPage"),
					"重试后 WalkPage 端点必须可访问（bug：闩死使端点永久缺失）");

			// 成功后的幂等语义保持：重复调用静默跳过，不抛不重启服务。
			hm.startHttpServer(netty, null, port);
			assertEquals(200, httpGetStatus(port, "/Zeze/Builtin/HistoryModule/WalkPage"),
					"重复启动幂等跳过后端点必须仍在服务");
		}
	}

	private static int httpGetStatus(int port, String path) throws Exception {
		var conn = (HttpURLConnection)new URL("http://127.0.0.1:" + port + path).openConnection();
		conn.setConnectTimeout(3_000);
		conn.setReadTimeout(10_000);
		try {
			conn.getInputStream().close();
			return conn.getResponseCode();
		} finally {
			conn.disconnect();
		}
	}
}
