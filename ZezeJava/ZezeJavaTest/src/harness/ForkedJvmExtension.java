package harness;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 仅测试工程识别子进程身份；生产代码始终运行原有fatal/close/halt路径。 */
public final class ForkedJvmExtension implements InvocationInterceptor {
	private static boolean childProcess;
	private static String childTest = "";

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> method,
			ExtensionContext context) throws Throwable {
		lifecycle(invocation);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> method,
			ExtensionContext context) throws Throwable {
		lifecycle(invocation);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> method,
			ExtensionContext context) throws Throwable {
		lifecycle(invocation);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> method,
			ExtensionContext context) throws Throwable {
		lifecycle(invocation);
	}

	private static void lifecycle(Invocation<Void> invocation) throws Throwable {
		if (childProcess)
			invocation.proceed();
		else
			invocation.skip();
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> method,
			ExtensionContext context) throws Throwable {
		if (childProcess) {
			invocation.proceed();
			return;
		}
		invocation.skip();
		assertEquals(21, Runtime.version().feature(), "子JVM测试需要JDK 21");
		var testClass = context.getRequiredTestClass();
		var testMethod = context.getRequiredTestMethod();
		var root = Path.of("build", "forked-tests").toAbsolutePath().normalize();
		Files.createDirectories(root);
		var dir = Files.createTempDirectory(root, testClass.getSimpleName() + '-');
		boolean success = false;
		try {
			// 原有SM测试通过Config.load()读取测试XML；每个子进程使用自己的副本和数据目录。
			var config = Path.of("zeze.xml");
			if (Files.exists(config))
				Files.copy(config, dir.resolve("zeze.xml"));
			Files.createDirectory(dir.resolve("tmp"));
			var halt = testMethod.getAnnotation(ForkedJvm.ExpectHalt.class);
			int timeout = testClass.getAnnotation(ForkedJvm.class).timeoutSeconds();
			run(dir, testClass, testMethod.getName(), "", timeout, halt);
			if (halt != null && !halt.verifyAfterHalt().isEmpty())
				run(dir, testClass, testMethod.getName(), halt.verifyAfterHalt(), timeout, null);
			success = true;
		} finally {
			if (success) {
				assertTrue(dir.startsWith(root), "只清理本测试创建的目录");
				try (var paths = Files.walk(dir)) {
					for (var path : paths.sorted(Comparator.reverseOrder()).toList())
						Files.delete(path);
				}
			} else
				System.err.println("子JVM测试现场保留在 " + dir);
		}
	}

	private static String classpath(Class<?> testClass) throws Exception {
		var entries = new LinkedHashSet<Path>();
		// Gradle 9的java.class.path可能只有worker引导jar，依赖还在URLClassLoader中。
		for (var loader = testClass.getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs()) {
					if (url.getProtocol().equals("file"))
						entries.add(Path.of(url.toURI()).toAbsolutePath().normalize());
				}
			}
		}
		for (var entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
			entries.add(Path.of(entry).toAbsolutePath().normalize());
		for (var type : new Class<?>[]{testClass, ForkedJvmExtension.class, Application.class})
			entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
					.toAbsolutePath().normalize());
		return String.join(File.pathSeparator, entries.stream().map(Path::toString).toList());
	}

	private static String quote(String argument) {
		return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}

	private static void run(Path dir, Class<?> testClass, String method, String verifier, int timeout,
			ForkedJvm.@Nullable ExpectHalt halt) throws Exception {
		var id = testClass.getName() + '#' + method;
		boolean windows = System.getProperty("os.name").startsWith("Windows");
		var arguments = new ArrayList<>(List.of("-ea", "-DZezeCounter=null", "-Dzeze.test.env=off",
				"-Djava.io.tmpdir=" + dir.resolve("tmp"), "-cp", classpath(testClass),
				ForkedJvmExtension.class.getName(), testClass.getName(), method));
		if (!verifier.isEmpty())
			arguments.add(verifier);
		var argumentFile = dir.resolve("java.args");
		Files.write(argumentFile, arguments.stream().map(ForkedJvmExtension::quote).toList(),
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		var log = dir.resolve(verifier.isEmpty() ? "test.log" : "verify.log");
		var java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
		var child = new ProcessBuilder(java.toString(), "@" + argumentFile)
				.directory(dir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			boolean completed = child.waitFor(timeout, TimeUnit.SECONDS);
			if (!completed)
				stopOwnChild(child);
			var transcript = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
			var tail = transcript.length() <= 12_000 ? transcript
					: "[前文省略，完整日志见文件]\n" + transcript.substring(transcript.length() - 12_000);
			var details = id + "，日志：" + log + "\n" + tail;
			assertTrue(completed, "子JVM执行超时：" + details);
			assertTrue(transcript.contains("READY " + id), "未进入目标测试：" + details);
			assertFalse(transcript.contains("UNEXPECTED_RETURN"), "致命调用返回后仍在执行：" + details);
			assertEquals(halt == null ? 0 : (windows ? -1 : 255), child.exitValue(), "退出码不符：" + details);
			if (halt == null)
				assertTrue(transcript.contains("PASS " + id), "测试和生命周期未完整通过：" + details);
			else {
				assertTrue(transcript.contains(halt.value()), "未命中目标致命分支：" + details);
				assertTrue(transcript.contains("FATAL_CALLBACK " + id), "fatal回调断言未通过：" + details);
				assertFalse(transcript.contains("JVM_SHUTDOWN " + id), "必须halt，不能执行JVM停机钩子：" + details);
			}
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

	/** 仅在真实addAtFatalKill回调内的断言全部通过后调用，不改变致命退出。 */
	public static void fatalCallbackPassed() {
		assertTrue(childProcess, "fatal断言只能在子JVM中运行");
		marker("FATAL_CALLBACK");
	}

	public static void unexpectedReturn() {
		marker("UNEXPECTED_RETURN");
		throw new AssertionError("致命调用必须halt，不能继续返回");
	}

	private static void marker(String kind) {
		System.out.println(kind + ' ' + childTest);
		System.out.flush();
	}

	public static void main(String[] args) {
		childProcess = true;
		childTest = args[0] + '#' + args[1];
		Runtime.getRuntime().addShutdownHook(new Thread(() -> marker("JVM_SHUTDOWN")));
		try {
			if (args.length == 3) {
				marker("READY");
				Class.forName(args[0]).getMethod(args[2], Path.class).invoke(null, Path.of("").toAbsolutePath());
			} else {
				var testClass = Class.forName(args[0]);
				var target = Arrays.stream(testClass.getDeclaredMethods())
						.filter(method -> method.getName().equals(args[1])).findFirst().orElseThrow();
				var summary = new SummaryGeneratingListener();
				try (var session = LauncherFactory.openSession()) {
					var launcher = session.getLauncher();
					launcher.registerTestExecutionListeners(summary, new TestExecutionListener() {
						@Override
						public void executionStarted(TestIdentifier test) {
							if (test.isTest())
								marker("READY");
						}
					});
					launcher.execute(LauncherDiscoveryRequestBuilder.request()
							.selectors(DiscoverySelectors.selectMethod(testClass, target))
							.configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build());
				}
				var result = summary.getSummary();
				result.printTo(new PrintWriter(System.out, true));
				result.printFailuresTo(new PrintWriter(System.out, true));
				assertEquals(0, result.getTotalFailureCount(), "子JVM中测试或生命周期失败");
				assertEquals(1, result.getTestsSucceededCount(), "必须完整通过一个目标测试，不能跳过或中止");
			}
			marker("PASS");
			System.exit(0);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(2); // 启动/断言失败与生产halt(-1)区分。
		}
	}
}
