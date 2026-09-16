package Zeze.Arch.Gen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectToServer;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND7-67回归：GenModule生成redirect拦截子类时仅扫moduleClass.getDeclaredMethods()，
 * 基类（非叶子模块类）声明的@RedirectToServer/Hash/All方法既不生成拦截子类方法、
 * 也不注册redirect.handles——调用静默本地执行、远程不可达，无任何告警，与方法签名
 * 非法时的IllegalStateException fail-fast形成反差。
 * 修复：沿类层级向上收集带注解方法（到IModule为止），同签名（名字+参数类型）去重、
 * 派生类声明优先。
 * <p>
 * 复现：抽象基类模块声明@RedirectToServer方法+叶子模块继承，离线生成模式
 * （-DGenFileSrcRoot同路径）调createRedirectModules。修复前叶子类收集到空overrides
 * 直接continue，不生成任何文件；断言"生成拦截方法+handles注册"失败。
 * 注：内存编译模式对嵌套fixture类不可用（生成源码extends pkg.Outer$Inner中的$
 * 是源码标识符，javac无法解析嵌套二进制名），与生产顶层模块类形态不符，故用
 * 离线模式断言生成源码内容（拦截方法+handles注册键）。
 */
@Fast
@Isolated // genFileSrcRoot是GenModule.instance上的JVM级全局开关，独占运行
public class TestFnd767GenModuleInheritedRedirect {
	private static final String LEAF_FULL_NAME = "TestFnd767GenModuleInheritedRedirect.LeafModule";

	// 基类模块声明redirect方法（缺陷场景：注解不在叶子类上）。
	public static abstract class BaseModule {
		public static final int ModuleId = 7670;
		public static final String ModuleFullName = "TestFnd767GenModuleInheritedRedirect.BaseModule";

		@RedirectToServer
		public void basePing(int serverId, long arg) {
		}
	}

	public static class LeafModule extends BaseModule {
		public static final int ModuleId = 7671;
		public static final String ModuleFullName = LEAF_FULL_NAME;
	}

	// 叶子重声明同签名方法（派生类声明优先，同签名不得重复生成）。
	public static class OverrideLeafModule extends BaseModule {
		public static final int ModuleId = 7672;
		public static final String ModuleFullName = "TestFnd767GenModuleInheritedRedirect.OverrideLeafModule";

		@Override
		@RedirectToServer
		public void basePing(int serverId, long arg) {
		}
	}

	@TempDir
	Path tempDir;

	private final AppBase dummyApp = new AppBase() {
		@Override
		public @Nullable Application getZeze() {
			return null; // 离线生成模式只读app.getClass()匹配构造器，不触碰zeze
		}
	};

	@BeforeEach
	public void setUp() {
		Assertions.assertNull(GenModule.instance.genFileSrcRoot, "测试前提：全局genFileSrcRoot默认关闭");
	}

	@AfterEach
	public void tearDown() {
		GenModule.instance.genFileSrcRoot = null; // 全局开关必须恢复
	}

	/** 继承的redirect方法必须触发生成：拦截方法覆盖+handles注册。 */
	@Test
	public void testInheritedRedirectGenerated() throws Exception {
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		var modules = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{LeafModule.class});
		Assertions.assertNull(modules, "离线生成模式返回null（不编译不实例化）");

		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX
				+ LeafModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file),
				"继承的redirect方法必须触发生成（FND7-67：原先overrides为空直接跳过，静默本地化）");
		var content = Files.readString(file, StandardCharsets.UTF_8);
		Assertions.assertTrue(content.contains("void basePing("),
				"生成内容必须包含继承方法的拦截覆盖");
		Assertions.assertTrue(content.contains("super.basePing("),
				"拦截覆盖必须回环调用super（本地执行路径）");
		Assertions.assertTrue(content.contains("\"" + LEAF_FULL_NAME + ":basePing\""),
				"生成内容必须包含handles注册（module全名:方法名）——原先既不生成拦截也不注册");
	}

	/** 派生类重声明同签名：去重后只生成一次（重复生成会产生重复方法无法编译）。 */
	@Test
	public void testOverriddenSignatureNotDuplicated() throws Exception {
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{OverrideLeafModule.class});

		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX
				+ OverrideLeafModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file), "重声明注解的叶子必须照常生成");
		var content = Files.readString(file, StandardCharsets.UTF_8);
		Assertions.assertEquals(1, countOccurrences(content, "void basePing("),
				"同签名方法去重后必须只生成一次拦截覆盖");
	}

	private static int countOccurrences(@NotNull String content, @NotNull String token) {
		int count = 0;
		for (int index = content.indexOf(token); index >= 0; index = content.indexOf(token, index + token.length()))
			++count;
		return count;
	}
}
