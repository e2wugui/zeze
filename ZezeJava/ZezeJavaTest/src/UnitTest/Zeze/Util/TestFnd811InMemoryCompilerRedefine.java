package UnitTest.Zeze.Util;

import java.util.Map;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-11回归：同一InMemoryJavaCompiler实例二次编译同名类——compile直调
 * classLoader.findClass（绕过loadClass的findLoadedClass缓存）重复defineClass
 * 必LinkageError；compileToByteCode返回新字节码而旧类仍在加载器中，新旧错配。
 * 修复：DynamicClassLoader暴露isDefined，compile/compileToByteCode/compileAll/
 * compileAllToByteCode入口对已define的名字fail-fast抛IllegalStateException，
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

	@Test
	public void testSameInstanceRecompileRejected() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		var cls = compiler.compile(CLASS_A, SRC_A);
		Assertions.assertEquals(42, cls.getDeclaredMethod("f").invoke(cls.getDeclaredConstructor().newInstance()));

		// 修复前红：LinkageError(attempted duplicate class definition)
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> compiler.compile(CLASS_A, SRC_A));
		Assertions.assertTrue(ex.getMessage().contains(CLASS_A), ex.getMessage());

		// 字节码入口同样拒绝（修复前静默返回新字节码，与旧类错配）
		Assertions.assertThrows(IllegalStateException.class,
				() -> compiler.compileToByteCode(CLASS_A, SRC_A));

		// 指引可行：轮换实例重编译成功
		var cls2 = new InMemoryJavaCompiler().compile(CLASS_A, SRC_A);
		Assertions.assertEquals(42, cls2.getDeclaredMethod("f").invoke(cls2.getDeclaredConstructor().newInstance()));
	}

	@Test
	public void testUseParentClassLoaderRotateThenRecompile() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		var cls1 = compiler.compile(CLASS_A, SRC_A);

		// 轮换加载器：新加载器可重编译同名类，得到新的Class身份
		compiler.useParentClassLoader(compiler.getClassloader().getParent());
		var cls2 = compiler.compile(CLASS_A, SRC_A);
		Assertions.assertNotSame(cls1, cls2);
		Assertions.assertEquals(42, cls2.getDeclaredMethod("f").invoke(cls2.getDeclaredConstructor().newInstance()));
	}

	@Test
	public void testCompileAllBatchRejectedIfAnyDefined() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		var srcB = "package a1_fnd811;\npublic class B811 { public int g() { return 7; } }\n";
		compiler.compile("a1_fnd811.B811", srcB);

		// 批次含已define名字：入口fail-fast（修复前：LinkageError或静默错配）
		Assertions.assertThrows(IllegalStateException.class,
				() -> compiler.compileAll(Map.of("a1_fnd811.C811", SRC_A.replace("A811", "C811"),
						"a1_fnd811.B811", srcB)));

		// 纯新名字批次照常成功
		var result = compiler.compileAll(Map.of("a1_fnd811.D811", SRC_A.replace("A811", "D811")));
		Assertions.assertTrue(result.containsKey("a1_fnd811.D811"));
	}
}
