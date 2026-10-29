package Zeze.Component;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 子进程独占临时工作目录、缓存和内存库；父JUnit进程不拦截或替换生产退出动作。 */
final class TakeoverTestProcess {
	private static String childClasspath(Class<?> main) throws Exception {
		// Gradle 9的java.class.path可能只有worker引导jar，依赖还在URLClassLoader中。
		var entries = new LinkedHashSet<Path>();
		for (var loader = main.getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs()) {
					if (url.getProtocol().equals("file"))
						entries.add(Path.of(url.toURI()).toAbsolutePath().normalize());
				}
			}
		}
		for (var entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
			entries.add(Path.of(entry).toAbsolutePath().normalize());
		for (var type : new Class<?>[]{main, Application.class})
			entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
					.toAbsolutePath().normalize());
		return String.join(File.pathSeparator, entries.stream().map(Path::toString).toList());
	}

	private static String quoteArgument(String argument) {
		return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}

	static void run(Path dir, Class<?> main, String scenario, @Nullable String fatalReason) throws Exception {
		assertEquals(21, Runtime.version().feature(), "Takeover进程测试需要JDK 21");
		var arguments = List.of("-ea", "-DZezeCounter=null", "-Dzeze.test.env=off",
				"-cp", childClasspath(main), main.getName(), scenario);
		var argumentFile = dir.resolve("java.args");
		// Windows启动器按native.encoding读取argfile；classpath长于命令行上限时也可运行。
		Files.write(argumentFile, arguments.stream().map(TakeoverTestProcess::quoteArgument).toList(),
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		boolean windows = System.getProperty("os.name").startsWith("Windows");
		var java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
		var log = dir.resolve(scenario + ".log");
		var child = new ProcessBuilder(java.toString(), "@" + argumentFile.toAbsolutePath())
				.directory(dir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			boolean completed = child.waitFor(30, TimeUnit.SECONDS);
			if (!completed)
				stopOwnChild(child);
			var transcript = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
			assertTrue(transcript.contains("READY " + scenario), "子JVM未完成场景准备：\n" + transcript);
			assertTrue(completed, "子JVM未按时退出：\n" + transcript);
			assertFalse(transcript.contains("UNEXPECTED_RETURN"), "致命分支返回后继续执行了代码：\n" + transcript);
			int expectedExit = fatalReason != null ? (windows ? -1 : 255) : 0;
			assertEquals(expectedExit, child.exitValue(), "退出码不符：\n" + transcript);
			if (fatalReason != null) {
				assertTrue(transcript.contains("Takeover: " + fatalReason), "未命中预期致命分支：\n" + transcript);
				assertTrue(transcript.contains("LEASE_PRESERVED " + scenario), "退出时租约保护未通过：\n" + transcript);
				assertTrue(transcript.contains("STOPPED " + scenario), "应用停机未完成：\n" + transcript);
			} else
				assertTrue(transcript.contains("PASS " + scenario), "正常场景未完成：\n" + transcript);
		} finally {
			stopOwnChild(child);
		}
	}

	private static void stopOwnChild(Process child) {
		if (!child.isAlive())
			return;
		child.destroyForcibly();
		boolean interrupted = false;
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		try {
			while (child.isAlive()) {
				long remaining = deadline - System.nanoTime();
				assertTrue(remaining > 0, "测试子JVM强制终止后未回收");
				try {
					child.waitFor(remaining, TimeUnit.NANOSECONDS);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	static void marker(String kind, String scenario) {
		System.out.println(kind + ' ' + scenario);
		System.out.flush();
	}

	private TakeoverTestProcess() {
	}
}
