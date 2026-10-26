package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * RFD1-03：真实进程死亡测试——子进程halt(137)（不走shutdown hook、不flush），
 * 父进程断言old-or-new与清扫后结论不变。测试不进生产代码。
 */
@Fast
public class TestAtomicOutputFileCrash {

	@Test
	public void testRealProcessDeathLeavesOldOrNew() throws Exception {
		for (var scenario : new String[]{"halt-before-commit", "halt-after-commit"}) {
			var dir = Files.createTempDirectory("atomic-kill-");
			var target = dir.resolve("state.bin");
			var oldBytes = "old-complete-version".getBytes();
			var newBytes = "new-complete-version".getBytes();
			AtomicOutputFile.writeAtomically(target, oldBytes);

			var java = ProcessHandle.current().info().command().orElse("java");
			var child = new ProcessBuilder(java, "-cp", childClasspath(),
					AtomicCrashLauncher.class.getName(),
					target.toString(), scenario)
					.redirectErrorStream(true).start();
			var childOutput = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			assertEquals(137, child.waitFor(),
					"子进程必须按剧情halt: " + scenario + "\n子进程输出:\n" + childOutput);

			// 提交前死亡保留旧版，提交完成后死亡保留新版。
			var expected = scenario.equals("halt-before-commit") ? oldBytes : newBytes;
			var got = Files.readAllBytes(target);
			assertArrayEquals(expected, got, scenario);

			// 句柄已被OS释放，清扫残留tmp（生产中由调用方启动清扫）后结论不变
			try (var list = Files.list(dir)) {
				for (var p : list.filter(f -> f.getFileName().toString().endsWith(".tmp")).toList())
					Files.deleteIfExists(p);
			}
			got = Files.readAllBytes(target);
			assertArrayEquals(expected, got, scenario + ": 清扫后结论不变");
		}
	}

	// 子进程classpath取三处code source：不受worker长classpath与gradle argfile处理影响；
	// log4j-api自带SimpleLogger回退。
	private static String childClasspath() throws Exception {
		var parts = new LinkedHashSet<String>();
		for (var cls : List.of(TestAtomicOutputFileCrash.class, AtomicOutputFile.class,
				org.apache.logging.log4j.LogManager.class)) {
			var location = cls.getProtectionDomain().getCodeSource().getLocation();
			parts.add(Path.of(location.toURI()).toAbsolutePath().toString());
		}
		return String.join(File.pathSeparator, parts);
	}
}
