package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.BindException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Netty.Netty;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 管理口 HTTP bind 结果确认直测：HttpServer.start 对 b.bind 的 ChannelFuture 不同步
 * 不记日志（跨域），bind 失败异步发生在 event loop 上——调用方丢弃返回值则进程
 * "正常"运行（startServer 日志已打出）但 9980 无人监听、零错误日志，且 new Netty()
 * 的非守护 event loop 线程使失败进程存活为"健康"僵尸——与 MainZokerManager 对
 * checkDeployPolicy 的显式 fail-fast 契约不一致。修复后 startAdminHttpServer 同步
 * 等待 bind：失败回收 event loop 线程组后抛含 cause 的 IllegalStateException。
 * 坏地址形态（TEST-NET-1 非本机地址，BindException 确定性失败）驱动；同机端口冲突
 * 形态走同一 await/isSuccess 代码路径（Windows SO_REUSEADDR 语义使占口复测不稳定，
 * 不做直测面）。
 *
 * <p>@Isolated：写 LogAgentManager.httpServer 静态引用，独占运行。</p>
 */
@Fast
@Isolated
public class TestAdminPortBindFailureFailsFast {

	/** bind 失败必须显式抛出（含 cause 指向 BindException），不残留半启动 server 引用。 */
	@Test
	public void testBindFailureThrowsWithCause() throws Exception {
		Task.tryInitThreadPool();
		var conf = new ZokerManagerConf();
		conf.bind = "192.0.2.1"; // TEST-NET-1：本机必不存在的地址，bind 必失败
		conf.token = "secret";   // 非回环必须配 Token——过部署契约校验以触达 bind 本身

		var ex = assertThrows(IllegalStateException.class,
				() -> LogAgentManager.startAdminHttpServer(conf, 0),
				"bind 失败必须显式失败而非静默吞掉");
		assertTrue(ex.getMessage().contains("bind"), "报错指向 bind 失败: " + ex.getMessage());
		assertEquals(BindException.class, ex.getCause().getClass(),
				"cause 保留底层 BindException 便于诊断: " + ex.getCause());
		assertNull(LogAgentManager.httpServer, "失败后不得残留半启动 server 引用");
	}

	/** 契约校验失败（非回环无 Token）仍先于一切启动 fail-fast——既有行为不回归。 */
	@Test
	public void testDeployPolicyStillFailsFirst() throws Exception {
		Task.tryInitThreadPool();
		var conf = new ZokerManagerConf();
		conf.bind = "192.0.2.1";
		conf.token = "";
		assertThrows(IllegalStateException.class,
				() -> LogAgentManager.startAdminHttpServer(conf, 0),
				"非回环无 Token 必须先于 bind 拒绝（FND29 契约不回归）");
		assertNull(LogAgentManager.httpServer);
	}

	/** bind 成功（默认回环+无 Token，port 0）：正常返回，channel future 就绪。 */
	@Test
	public void testBindSuccessReturns() throws Exception {
		Task.tryInitThreadPool();
		var conf = new ZokerManagerConf();
		var netty = assertDoesNotThrow(() -> LogAgentManager.startAdminHttpServer(conf, 0),
				"默认回环形态必须正常启动");
		try {
			assertNotNull(LogAgentManager.httpServer);
			assertTrue(LogAgentManager.httpServer.getChannelFuture().isSuccess(),
					"返回时 bind 必须已确认成功");
		} finally {
			LogAgentManager.httpServer.close();
			netty.close();
			LogAgentManager.httpServer = null; // 复位静态引用，不污染同 JVM 其他用例
		}
	}
}
