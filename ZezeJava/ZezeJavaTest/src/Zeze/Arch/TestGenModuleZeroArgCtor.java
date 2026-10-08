package Zeze.Arch;

import java.util.Map;

import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.IModule;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND16 arch-01 红绿钉板：仅 0 参构造器的合法 IModule 模块经 createRedirectModules
 * （无 redirect 方法 → 直接原始类实例化路径）必须能启动——修复前 newModule 对 0 参
 * 构造器硬抛 NoSuchMethodException（"No suitable constructor for redirect module"，
 * 文案误导指向 redirect 配置）；A1 三案例实证双标准：同形态加 redirect 方法能启动
 * （genModuleCode 为 0 参基类生成桥接构造器）、删光反而崩溃。
 * <p>
 * GenModule.instance 是 JVM 级静态（编译器/装载器），@Isolated 独占运行。
 */
@Fast
@Isolated
public class TestGenModuleZeroArgCtor {

	private static final String MODULE_CLASS = "archzerocrg.ModuleZero";

	private static final String MODULE_SRC = """
			package archzerocrg;
			public class ModuleZero implements Zeze.IModule {
			    public static final int ModuleId = 18792;
			    // 仅0参构造器（含隐式默认）：修复前的拒绝形态。
			    public ModuleZero() {
			    }
			    @Override
			    public String getFullName() {
			        return "archzerocrg";
			    }
			    @Override
			    public String getName() {
			        return "ModuleZero";
			    }
			    @Override
			    public int getId() {
			        return ModuleId;
			    }
			}
			""";

	@Test
	public void testZeroArgCtorModuleInstantiable() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.useOptions("-cp", System.getProperty("java.class.path"));
		var bytes = compiler.compileAllToByteCode(Map.of(MODULE_CLASS, MODULE_SRC)).get(MODULE_CLASS);
		Assertions.assertNotNull(bytes);

		// 独立装载器define（不落冷classpath，避免HotModule冷抢载语义干扰）。
		var loader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
			@Override
			protected Class<?> findClass(String name) {
				if (name.equals(MODULE_CLASS)) {
					return defineClass(name, bytes, 0, bytes.length);
				}
				throw new IllegalArgumentException(name);
			}
		};
		@SuppressWarnings("unchecked")
		var moduleClass = (Class<? extends IModule>)Class.forName(MODULE_CLASS, true, loader);

		var appBase = new AppBase() {
			@Override
			public Zeze.Application getZeze() {
				return null; // 0参路径不触达zeze
			}
		};

		// 无redirect方法的模块：createRedirectModules承诺"直接用原始模块类实例化"。
		var modules = GenModule.instance.createRedirectModules(appBase, new Class<?>[]{moduleClass});
		Assertions.assertEquals(1, modules.length);
		Assertions.assertNotNull(modules[0], "仅0参构造器的模块必须可实例化（arch-01）");
		Assertions.assertEquals(MODULE_CLASS, modules[0].getClass().getName());
	}
}
