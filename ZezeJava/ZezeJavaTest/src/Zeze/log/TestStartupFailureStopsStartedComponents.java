package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.MainZokerManager;
import Zeze.Services.Log4jQuery.FileCreateDetector;
import Zeze.Util.Task;

import harness.Fast;

/**
 * ZokerManager 启动序列失败的半启动回收直测：LogService 构造即启动文件监视
 * 线程（修复前为非守护 Thread，阻塞在 WatchService.take()），其后
 * LogAgentManager.init 失败（管理口 bind 冲突等）异常直接上抛、无人清理——
 * 进程被监视线程钉成"LogService 照常存活、9980 无人监听"的半启动僵尸，依赖
 * 进程退出的守护重启不动作，且重启撞 (logDir, logActive) 独占登记。修复后
 * MainZokerManager.start 全路径收尾：任一步失败逆序回收已启动组件
 * （LogAgentManager.stop → LogService.stop）后上抛，不残留存活线程与静态引用。
 *
 * <p>bind 失败注入形态对齐 TestAdminPortBindFailureFailsFast：TEST-NET-1
 * 非本机地址（BindException 确定性失败），Token 配置以通过部署契约校验触达
 * bind 本身；线程残留以"启动前后存活的非守护线程差集"断言（红=监视线程
 * 出现在差集，绿=差集为空——回收必须 join 掉监视线程，daemon 化只是兜底）。</p>
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
public class TestStartupFailureStopsStartedComponents {

	/** 启动失败注入：bind 失败后不得残留存活的非守护线程与半启动静态引用。 */
	@Test
	public void testBindFailureStopsLogServiceAndAgent() throws Exception {
		Task.tryInitThreadPool();
		var logDir = Files.createTempDirectory("zeze-zoker-startup-fail");
		int logServicePort;
		try (var ss = new ServerSocket(0)) {
			logServicePort = ss.getLocalPort();
		}
		var configXml = writeConfigXml(logDir, logServicePort);
		try {
			var before = nonDaemonThreadIds();
			var ex = assertThrows(IllegalStateException.class, () -> MainZokerManager.start(configXml),
					"bind 失败必须显式抛出（含 cause 指向 BindException）");
			assertTrue(ex.getMessage().contains("bind"), "报错指向 bind 失败: " + ex.getMessage());

			var leftover = new HashSet<>(nonDaemonThreadIds());
			leftover.removeAll(before);
			assertTrue(leftover.isEmpty(),
					"启动失败必须回收全部已启动组件，不残留钉住 JVM 的存活线程: " + describeThreads(leftover));
			// 监视线程必须被 LogService.stop 实际 join 掉（回收语义），daemon 化只是
			// 异常退出路径的兜底——活着的 watch 线程即半启动残留。
			assertTrue(noLiveWatchThread(), "LogService 的文件监视线程必须随失败清理退出（不得存活）");

			// 静态引用同步复位：半启动 manager/server 不得留给后续调用方。
			assertNull(LogAgentManager.httpServer, "失败后不得残留半启动 server 引用");
			assertNull(LogAgentManager.getInstance(), "失败后不得残留半启动 manager 引用");
		} finally {
			// init 经配置 ApiToken.configure("t") 配置了静态门禁，stop 不复位（生产形态
			// 停机即进程退出）——同 JVM 后续直挂处理器的用例（无 Authorization 头）会被
			// 401 误伤，此处复位静态。
			ApiToken.configure(null);
			// 同因：init 还以 Bind=192.0.2.1（非回环）装载 BrowserOriginGuard——Host 回环
			// 防线被解除且 stop 不复位。漏复位时同 JVM 后续依赖默认回环设防的防线用例
			// （TestAdminApiRejectsForeignOrigin 的 rebinding 403 断言）被静默放行，
			// 请求直落处理器（2026-09-30 IDEA 全量轮实证：403 变 500 假红）。
			BrowserOriginGuard.configure(null);
			deleteBestEffort(logDir);
		}
	}

	/**
	 * 监视线程 daemon 化（根因侧兜底）：生命周期虽由 stopAndJoin 显式管理，
	 * 宿主进程异常退出路径不得被 watch 线程钉住。
	 */
	@Test
	public void testFileCreateDetectorWatchThreadIsDaemon() throws Exception {
		Task.tryInitThreadPool();
		var dir = Files.createTempDirectory("zeze-zoker-watch-daemon");
		var detector = new FileCreateDetector(dir.toString(), p -> {
		});
		try {
			detector.start();
			var watcher = Thread.getAllStackTraces().keySet().stream()
					.filter(t -> t.isAlive() && t.getName().startsWith("log4j-watch-"))
					.findAny().orElseThrow(() -> new IllegalStateException("监视线程必须以可辨识名字启动"));
			assertTrue(watcher.isDaemon(), "watch 线程必须 daemon 化（stopAndJoin 显式管理生命周期）");
		} finally {
			detector.stopAndJoin();
			deleteBestEffort(dir);
		}
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * 临时 server.xml：ServiceManager disable（自包含），两份 LogConf（两个监视线程），
	 * ZokerManagerConf Bind=TEST-NET-1 + Token（过契约校验，bind 确定性失败）。
	 */
	private static String writeConfigXml(Path logDir, int logServicePort) throws Exception {
		var xml = "<zeze ServerId=\"0\" ServiceManager=\"\">\n"
				+ "<DatabaseConf Name=\"\" DatabaseType=\"Memory\" DatabaseUrl=\"\"/>\n"
				+ "<CustomizeConf Name=\"LogServiceConf\">\n"
				+ "<LogConf LogActive=\"zeze.log\" logDir=\"" + xmlEscape(logDir.toString()) + "\"/>\n"
				+ "<LogConf LogActive=\"zeze_error.log\" logDir=\"" + xmlEscape(logDir.toString()) + "\"/>\n"
				+ "</CustomizeConf>\n"
				+ "<CustomizeConf Name=\"ZokerManagerConf\" Bind=\"192.0.2.1\" Token=\"t\"/>\n"
				+ "<ServiceConf Name=\"Zeze.LogService.Server\">\n"
				+ "<Acceptor Ip=\"127.0.0.1\" Port=\"" + logServicePort + "\"/>\n"
				+ "</ServiceConf>\n"
				+ "</zeze>\n";
		var configXml = Files.createTempFile("zeze-zoker-server", ".xml");
		Files.writeString(configXml, xml, StandardCharsets.UTF_8);
		return configXml.toString();
	}

	private static String xmlEscape(String s) {
		return s.replace("\\", "/");
	}

	private static boolean noLiveWatchThread() {
		return Thread.getAllStackTraces().keySet().stream()
				.noneMatch(t -> t.isAlive() && t.getName().startsWith("log4j-watch-"));
	}

	private static Set<Long> nonDaemonThreadIds() {
		return Thread.getAllStackTraces().keySet().stream()
				// globalEventExecutor-* 是 Netty 的 JVM 级全局单例线程，不属于被测
				// 启动序列的任何组件——同 JVM 其他用例泄漏的重连循环（如 MQAgent
				// autoReconnect）持续喂任务即可在前后快照间把它拉活，落进差集即假红
				// （2026-09-30 IDEA 全量轮实证；对齐 Fnd770"JVM 全局资源没有只有我
				// 看得到"的教训：差集只断言本启动序列自己的组件线程）。
				.filter(t -> t.isAlive() && !t.isDaemon() && !t.getName().startsWith("globalEventExecutor-"))
				.map(Thread::threadId)
				.collect(Collectors.toSet());
	}

	private static String describeThreads(Set<Long> ids) {
		return Thread.getAllStackTraces().keySet().stream()
				.filter(t -> ids.contains(t.threadId()))
				.map(t -> t.getName() + "(stackTop=" + t.getStackTrace().length + ")")
				.collect(Collectors.joining(", "));
	}

	// 尽力删除：留给系统临时目录清理，失败不干扰测试结果。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					// ignore
				}
			});
		} catch (Exception e) {
			// ignore
		}
	}
}
