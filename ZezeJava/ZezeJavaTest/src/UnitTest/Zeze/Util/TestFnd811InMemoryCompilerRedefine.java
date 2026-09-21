package UnitTest.Zeze.Util;

import java.util.Map;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-11回归：同一InMemoryJavaCompiler实例二次编译同名类——defineCompiled直调
 * classLoader.findClass（绕过loadClass的findLoadedClass缓存）重复defineClass
 * 必LinkageError；二次编译的字节码与旧类错配。修复：DynamicClassLoader暴露
 * isDefined，编译入口对已define的名字fail-fast抛IllegalStateException，
 * 指引轮换实例或useParentClassLoader。
 */
@Fast
public class TestFnd811InMemoryCompilerRedefine {

	private static final String CLASS_A = "a1_fnd811.A811";
	private static final String SRC_A = """
			package a1_fnd811;
			public class A811 {
			    public int f() { return 42; }
			}
			""";

	private static int invokeF(Class<?> cls) throws Exception {
		return (int)cls.getDeclaredMethod("f").invoke(cls.getDeclaredConstructor().newInstance());
	}

	@Test
	public void testSameInstanceRecompileRejected() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.compileAllToByteCode(Map.of(CLASS_A, SRC_A));
		var cls = compiler.defineCompiled(CLASS_A);
		Assertions.assertEquals(42, invokeF(cls));

		// 修复前红：重复defineClass必LinkageError(attempted duplicate class definition)
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> compiler.compileAllToByteCode(Map.of(CLASS_A, SRC_A)));
		Assertions.assertTrue(ex.getMessage().contains(CLASS_A), ex.getMessage());

		// 指引可行：轮换实例重编译成功
		var compiler2 = new InMemoryJavaCompiler();
		compiler2.compileAllToByteCode(Map.of(CLASS_A, SRC_A));
		Assertions.assertEquals(42, invokeF(compiler2.defineCompiled(CLASS_A)));
	}

	@Test
	public void testUseParentClassLoaderRotateThenRecompile() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.compileAllToByteCode(Map.of(CLASS_A, SRC_A));
		var cls1 = compiler.defineCompiled(CLASS_A);

		// 轮换加载器：新加载器可重编译同名类，得到新的Class身份
		compiler.useParentClassLoader(compiler.getClassloader().getParent());
		compiler.compileAllToByteCode(Map.of(CLASS_A, SRC_A));
		var cls2 = compiler.defineCompiled(CLASS_A);
		Assertions.assertNotSame(cls1, cls2);
		Assertions.assertEquals(42, invokeF(cls2));
	}

	@Test
	public void testBatchRejectedIfAnyDefined() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		var srcB = "package a1_fnd811;\npublic class B811 { public int g() { return 7; } }\n";
		compiler.compileAllToByteCode(Map.of("a1_fnd811.B811", srcB));
		compiler.defineCompiled("a1_fnd811.B811");

		// 批次含已define名字：入口fail-fast（修复前：LinkageError或静默错配）
		Assertions.assertThrows(IllegalStateException.class,
				() -> compiler.compileAllToByteCode(Map.of("a1_fnd811.C811", SRC_A.replace("A811", "C811"),
						"a1_fnd811.B811", srcB)));

		// 纯新名字批次照常成功
		var result = compiler.compileAllToByteCode(Map.of("a1_fnd811.D811", SRC_A.replace("A811", "D811")));
		Assertions.assertNotNull(result.get("a1_fnd811.D811"));
	}
}
