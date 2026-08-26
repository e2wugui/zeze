package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.ClassReloader;
import Zeze.Util.InMemoryJavaCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestClassReloader {
	@Test
	public void test() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		var c = compiler.compile("TestCR", "public class TestCR { public static int f() { return 1; } }");
		var m = c.getMethod("f");
		var v = (int)m.invoke(null);
		Assertions.assertEquals(1, v);

		var b = compiler.compileToByteCode("TestCR", "public class TestCR { public static int f() { return 2; } }");
		ClassReloader.reloadClass(b, compiler.getClassloader());
		v = (int)m.invoke(null);
		Assertions.assertEquals(2, v);
	}
}
