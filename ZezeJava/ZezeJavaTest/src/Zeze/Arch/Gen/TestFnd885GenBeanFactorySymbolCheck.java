package Zeze.Arch.Gen;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND8-85回归：Bean/Data形参的decode生成引用未限定的beanFactory，按"模块类父类链
 * 自带可访问的静态beanFactory"惯例解析——纯字符串拼接零校验，模块类没定义、或定义为
 * private/package-private（生成子类默认包跨包继承不可达）时生成文件编译失败。
 * 修复：生成期沿父类链校验静态beanFactory的类型与可见性，不满足抛带修复提示的
 * UnsupportedOperationException；孪生——RedirectAll结果类补public默认构造器校验。
 */
@Fast
public class TestFnd885GenBeanFactorySymbolCheck {

	/** 无beanFactory的模块：原先生成引用不存在符号的不可编译源码。 */
	public static class NoFactoryModule {
		public static final int ModuleId = 8851;
		public static final String ModuleFullName = "TestFnd885.NoFactoryModule";

		@RedirectToServer
		public void update(int hash, Bean value) {
		}
	}

	/** private beanFactory（Component.Timer形态）：生成子类跨包继承不可达。 */
	public static class PrivateFactoryModule {
		public static final int ModuleId = 8852;
		public static final String ModuleFullName = "TestFnd885.PrivateFactoryModule";

		@SuppressWarnings("unused")
		private static final BeanFactory beanFactory = new BeanFactory();

		@RedirectToServer
		public void update(int hash, Bean value) {
		}
	}

	/** protected beanFactory（Rank/Game.Online形态）：合法惯例，照常生成。 */
	public static class ProtectedFactoryModule {
		public static final int ModuleId = 8853;
		public static final String ModuleFullName = "TestFnd885.ProtectedFactoryModule";

		protected static final BeanFactory beanFactory = new BeanFactory();

		@RedirectToServer
		public void update(int hash, Bean value) {
		}
	}

	/** 无Bean/Data形参的模块：不触发校验（护栏，Timer.redirectCancel形态）。 */
	public static class PlainModule {
		public static final int ModuleId = 8854;
		public static final String ModuleFullName = "TestFnd885.PlainModule";

		@RedirectToServer
		public void cancel(int hash, String id) {
		}
	}

	/** 孪生：RedirectAll结果类仅有私有构造器——原先该分支不做构造器校验。 */
	public static class NoCtorResult extends RedirectResult {
		private NoCtorResult() {
		}
	}

	@SuppressWarnings("unused")
	public static class AllNoCtorModule {
		@RedirectAll
		public RedirectAllFuture<NoCtorResult> collect(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@TempDir
	Path tempDir;

	private final AppBase dummyApp = new AppBase() {
		@Override
		public @Nullable Application getZeze() {
			return null; // 文件模式只读app.getClass()匹配构造器，不触碰zeze
		}
	};

	/** 无beanFactory：生成期拒绝，带模块名/方法名/修复提示，且不落盘。 */
	@Test
	public void testMissingBeanFactoryRejected() {
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp, new Class<?>[]{NoFactoryModule.class}, false));
		var cause = ex.getCause();
		Assertions.assertTrue(cause instanceof UnsupportedOperationException, "根因必须是生成期校验异常");
		Assertions.assertTrue(cause.getMessage().contains(NoFactoryModule.class.getName()), "须含模块类名");
		Assertions.assertTrue(cause.getMessage().contains("update"), "须含方法名");
		Assertions.assertTrue(cause.getMessage().contains("protected static final"),
				"须含修复提示（声明beanFactory的模板）: " + cause.getMessage());
		Assertions.assertFalse(genFileExists(NoFactoryModule.class), "不可编译产物不得写盘");
	}

	/** private beanFactory：生成子类默认包跨包继承不可达，生成期拒绝。 */
	@Test
	public void testPrivateBeanFactoryRejected() {
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp, new Class<?>[]{PrivateFactoryModule.class}, false));
		var cause = ex.getCause();
		Assertions.assertTrue(cause instanceof UnsupportedOperationException, cause.toString());
		Assertions.assertTrue(cause.getMessage().contains("public or protected"),
				"须指出可见性要求: " + cause.getMessage());
		Assertions.assertFalse(genFileExists(PrivateFactoryModule.class));
	}

	/** protected beanFactory与无Bean形参的模块：照常生成不误伤。 */
	@Test
	public void testValidModulesStillGenerate() {
		GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp,
				new Class<?>[]{ProtectedFactoryModule.class, PlainModule.class}, false);
		Assertions.assertTrue(genFileExists(ProtectedFactoryModule.class), "合法模块必须照常生成");
		Assertions.assertTrue(genFileExists(PlainModule.class), "无Bean形参模块不触发校验");
	}

	/** 孪生：RedirectAll结果类缺public默认构造器，生成期拒绝（对齐RedirectFuture分支）。 */
	@Test
	public void testRedirectAllResultCtorChecked() throws Exception {
		Method m = AllNoCtorModule.class.getMethod("collect", int.class);
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> new MethodOverride(m, m.getAnnotation(RedirectAll.class)));
		Assertions.assertTrue(ex.getMessage().contains("default constructor"), ex.getMessage());
	}

	private boolean genFileExists(Class<?> moduleClass) {
		return Files.exists(tempDir.resolve(GenModule.REDIRECT_PREFIX
				+ moduleClass.getName().replace('.', '_') + ".java"));
	}
}
