package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Util.Task;
import harness.Fast;

/**
 * log4j-02回归：（logDir, logActive）进程级独占登记与indexLinks分活性子目录。
 * 首版按整目录独占误禁同目录多活性（TestLogService的zeze.log+zeze_error.log同目录形态），
 * 精化为按（目录,活性折叠）登记——本测试直驱锁定精化后的四条语义：
 * 同键存活期重复构造拒绝、stop释放后重建合法、同目录不同活性共存（子目录命名空间）、
 * 活性大小写折叠同键（Windows同物理文件必须拒；Linux不同文件同键误拒=安全向）。
 */
@Fast
@Extra
public class TestLogDirExclusiveRegistration {
	@TempDir
	Path logDir;

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	private static LogServiceConf.LogConf conf(Path dir, String active) {
		var c = new LogServiceConf.LogConf();
		c.logActive = active;
		c.logDir = dir.toString();
		return c;
	}

	@Test
	public void testDuplicateRejectedWhileAliveAndReleasedAfterStop() throws Exception {
		var m1 = new Log4jFileManager(conf(logDir, "zeze.log"));
		try {
			assertThrows(IllegalArgumentException.class,
					() -> new Log4jFileManager(conf(logDir, "zeze.log")),
					"同一（目录,活性）双manager管理同一日志文件必须在构造期拒绝（indexLinks子目录撞号交错写/链接互删）");
		} finally {
			m1.stop();
		}
		// stop释放登记后同键重建合法（登记生命周期与实例生命周期配对）。
		var m2 = assertDoesNotThrow(() -> new Log4jFileManager(conf(logDir, "zeze.log")));
		m2.stop();
	}

	@Test
	public void testDifferentActiveSameDirCoexist() throws Exception {
		// 同目录两个活性（合法部署形态，server.xml的LogServiceConf同款）：共存且各自子目录独立。
		Files.write(logDir.resolve("zeze.log"), new byte[0]);
		Files.write(logDir.resolve("zeze_error.log"), new byte[0]);
		var m1 = new Log4jFileManager(conf(logDir, "zeze.log"));
		try {
			var m2 = new Log4jFileManager(conf(logDir, "zeze_error.log"));
			try {
				assertTrue(Files.isDirectory(logDir.resolve("indexLinks").resolve("zeze.log")),
						"indexLinks按logActive分子目录：编号分配/清理互不共享（首版整目录独占会误拒本形态）");
				assertTrue(Files.isDirectory(logDir.resolve("indexLinks").resolve("zeze_error.log")),
						"第二个活性的indexLinks子目录独立建立");
			} finally {
				m2.stop();
			}
		} finally {
			m1.stop();
		}
		// 两者都stop后，同键重建合法（无残留登记）。
		var m3 = new Log4jFileManager(conf(logDir, "zeze.log"));
		m3.stop();
	}

	@Test
	public void testActiveCaseFoldSharesKey() throws Exception {
		var m1 = new Log4jFileManager(conf(logDir, "zeze.log"));
		try {
			// 活性折叠小写：Windows上ZEZE.log与zeze.log是同一物理文件，同键必须拒；
			// Linux上是不同物理文件但同键误拒=既定安全向（跨平台断言一致按key拒绝）。
			assertThrows(IllegalArgumentException.class,
					() -> new Log4jFileManager(conf(logDir, "ZEZE.log")),
					"活性大小写折叠后同键：Windows同物理文件必须拒绝（Linux安全向误拒为既定裁量）");
		} finally {
			m1.stop();
		}
	}
}
