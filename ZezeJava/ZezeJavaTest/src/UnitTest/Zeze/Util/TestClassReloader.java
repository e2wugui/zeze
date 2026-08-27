package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import Zeze.Util.ClassReloader;
import Zeze.Util.InMemoryJavaCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestClassReloader {
	@Test
	public void test() throws Exception {
		// InMemoryJavaCompiler 单实例非线程安全（共享 sourceCodes），两个编译各用独立实例并发进行；
		// 热替换语义保持串行：v1 加载 → 旧 Method 断言 → 用 v2 字节码重定义 → 同一 Method 再断言
		var compiler2 = new InMemoryJavaCompiler();
		var v2Future = ForkJoinPool.commonPool().submit(() ->
				compiler2.compileToByteCode("TestCR", "public class TestCR { public static int f() { return 2; } }"));

		var compiler = new InMemoryJavaCompiler();
		var c = compiler.compile("TestCR", "public class TestCR { public static int f() { return 1; } }");
		var m = c.getMethod("f");
		var v = (int)m.invoke(null);
		Assertions.assertEquals(1, v);

		ClassReloader.reloadClass(v2Future.get(60, TimeUnit.SECONDS), compiler.getClassloader());
		v = (int)m.invoke(null);
		Assertions.assertEquals(2, v);
	}
}
