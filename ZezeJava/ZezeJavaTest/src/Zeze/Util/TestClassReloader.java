package Zeze.Util;

import harness.Fast;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import Zeze.Util.ClassReloader;
import Zeze.Util.InMemoryJavaCompiler;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Fast
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestClassReloader {
	@Test
	@Order(1)
	public void concurrentGetInstReturnsSameInstrumentation() throws Exception {
		int threads = 8;
		var ready = new CountDownLatch(threads);
		var start = new CountDownLatch(1);
		var results = new AtomicReferenceArray<@Nullable Instrumentation>(threads);
		var failure = new AtomicReference<@Nullable Throwable>();
		var workers = new Thread[threads];
		for (int i = 0; i < threads; i++) {
			final int idx = i;
			workers[i] = new Thread(() -> {
				ready.countDown();
				try {
					if (!start.await(10, TimeUnit.SECONDS))
						throw new AssertionError("start latch timed out");
					results.set(idx, ClassReloader.getInst());
				} catch (Throwable t) {
					failure.compareAndSet(null, t);
				}
			}, "class-reloader-concurrent-" + i);
			workers[i].setDaemon(true);
			workers[i].start();
		}
		Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));
		start.countDown();
		for (var worker : workers) {
			worker.join(30_000);
			Assertions.assertFalse(worker.isAlive(), "worker thread timed out");
		}
		Assertions.assertNull(failure.get(), () -> "unexpected failure: " + failure.get());
		Instrumentation first = results.get(0);
		Assertions.assertNotNull(first);
		for (int i = 1; i < threads; i++)
			Assertions.assertSame(first, results.get(i));
		Assertions.assertTrue(ClassReloader.getLoadAgentCount() <= 1,
				"concurrent getInst must attach at most once, actual=" + ClassReloader.getLoadAgentCount());
	}

	@Test
	@Order(2)
	public void test() throws Exception {
		// InMemoryJavaCompiler 单实例非线程安全（共享 sourceCodes），两个编译各用独立实例并发进行；
		// 热替换语义保持串行：v1 加载 → 旧 Method 断言 → 用 v2 字节码重定义 → 同一 Method 再断言
		var compiler2 = new InMemoryJavaCompiler();
		var v2Future = ForkJoinPool.commonPool().submit(() -> compiler2.compileAllToByteCode(
				Map.of("TestCR", "public class TestCR { public static int f() { return 2; } }")).get("TestCR"));

		var compiler = new InMemoryJavaCompiler();
		compiler.compileAllToByteCode(Map.of("TestCR", "public class TestCR { public static int f() { return 1; } }"));
		var c = compiler.defineCompiled("TestCR");
		var m = c.getMethod("f");
		var v = (int)m.invoke(null);
		Assertions.assertEquals(1, v);

		ClassReloader.reloadClass(v2Future.get(60, TimeUnit.SECONDS), compiler.getClassloader());
		v = (int)m.invoke(null);
		Assertions.assertEquals(2, v);
	}
}
