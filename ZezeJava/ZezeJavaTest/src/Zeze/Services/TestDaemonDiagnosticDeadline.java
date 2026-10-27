package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import harness.DirCleanup;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDaemonDiagnosticDeadline {
	public static class HangingDiagnostic {
		public static void main(String[] args) throws Exception {
			Files.writeString(Path.of(args[0]), Long.toString(ProcessHandle.current().pid()));
			System.out.println("diagnostic ready");
			System.out.flush();
			new java.util.concurrent.CountDownLatch(1).await();
		}
	}

	// 不用@TempDir：redirectOutput的目标文件句柄由本进程ProcessImpl持有、GC/finalize才释放
	// （d4f20df5e改重定向后，断言全过仍会"Failed to close extension context"，soak批5/20红）。
	// 自管目录+有界重试自删，残余留给系统临时目录清理（harness.DirCleanup哲学）。
	private Path dir;

	@AfterEach
	void deleteTempDirBestEffort() throws InterruptedException {
		if (dir == null)
			return;
		for (int i = 0; i < 5 && Files.exists(dir); i++) {
			System.gc(); // 促使ProcessImpl finalize释放重定向句柄
			DirCleanup.deleteBestEffort(dir);
			if (!Files.exists(dir))
				return;
			Thread.sleep(200);
		}
		// 耗尽仍有残余则不红，留给系统临时目录清理。
	}

	@Test
	public void openOutputStreamCannotPreventDiagnosticDeadline() throws Exception {
		dir = Files.createTempDirectory("daemon-diagnostic");
		var pidFile = dir.resolve("pid");
		var output = dir.resolve("output");
		Files.writeString(output, "old diagnostic");
		var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		var classPath = Path.of(HangingDiagnostic.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		var command = new String[]{java, "-cp", classPath.toString(), HangingDiagnostic.class.getName(), pidFile.toString()};
		var capture = new FutureTask<Void>(() -> {
			Daemon.captureDiagnostic(command, output, 2_000);
			return null;
		});
		var worker = new Thread(capture, "test-diagnostic-capture");
		worker.setDaemon(true);
		worker.start();
		try {
			capture.get(6, TimeUnit.SECONDS);
			assertTrue(Files.exists(pidFile), "diagnostic must have started");
			assertEquals("diagnostic ready", Files.readString(output).trim());
			var child = ProcessHandle.of(Long.parseLong(Files.readString(pidFile)));
			if (child.isPresent())
				child.get().onExit().get(3, TimeUnit.SECONDS);
		} finally {
			// 旧实现的流复制会阻塞；先收掉本测试子进程，让失败回归自身也能安全退出。
			if (Files.exists(pidFile)) {
				var child = ProcessHandle.of(Long.parseLong(Files.readString(pidFile)));
				if (child.isPresent()) {
					child.get().destroyForcibly();
					child.get().onExit().get(5, TimeUnit.SECONDS);
				}
			}
			worker.join(5_000);
			assertFalse(worker.isAlive());
		}
	}
}
