package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

	@Test
	public void openOutputStreamCannotPreventDiagnosticDeadline(@TempDir Path dir) throws Exception {
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
