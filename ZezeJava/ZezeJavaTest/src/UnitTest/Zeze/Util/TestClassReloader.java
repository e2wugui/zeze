package UnitTest.Zeze.Util;

import Zeze.Util.ClassReloader;
import Zeze.Util.InMemoryJavaCompiler;
import org.junit.Assert;
import org.junit.Test;

public class TestClassReloader {
	@Test
	public void test() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		var c = compiler.compile("TestCR", "public class TestCR { public static int f() { return 1; } }");
		var m = c.getMethod("f");
		var v = (int)m.invoke(null);
		Assert.assertEquals(1, v);

		var b = compiler.compileToByteCode("TestCR", "public class TestCR { public static int f() { return 2; } }");
		ClassReloader.reloadClass(b, compiler.getClassloader());
		v = (int)m.invoke(null);
		Assert.assertEquals(2, v);
	}
}
