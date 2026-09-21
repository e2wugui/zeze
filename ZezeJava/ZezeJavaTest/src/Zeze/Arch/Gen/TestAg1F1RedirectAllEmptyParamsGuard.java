package Zeze.Arch.Gen;

import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectResult;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * AG1-F1回归：RedirectAll空结果编解码不对称——encoder把null映射为Binary.Empty，
 * 发起端工厂守卫只判!=null，空缓冲decode读掩码抛异常冲出processResult的per-hash
 * 循环，同消息后续分组全部丢失。修复：工厂守卫改为
 * `if (_params_ != null && _params_.size() > 0)`（跳过decode但必须继续返回非null
 * 空对象——processResult对返回值直接setHash）。
 * 断言（生成源码级，同FND8-84判例）：RedirectAll生成工厂必须带size守卫，
 * 且return _r_在守卫之外（非null返回）。
 */
@Fast
public class TestAg1F1RedirectAllEmptyParamsGuard {

	public static class EmptyAbleResult extends RedirectResult {
		public long value;
	}

	public static class AllModule {
		public static final int ModuleId = 8711;
		public static final String ModuleFullName = "TestAg1F1.AllModule";

		@RedirectAll
		public RedirectAllFuture<EmptyAbleResult> collect(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@TempDir
	Path tempDir;

	@Test
	public void testGeneratedFactoryGuardsEmptyParams() throws Exception {
		var dummyApp = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return null; // 离线生成只读app.getClass()匹配构造器，不触碰zeze
			}
		};
		GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp, new Class<?>[]{AllModule.class}, false);
		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX + AllModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file), "生成文件必须存在");
		var content = Files.readString(file);

		// 守卫必须同时判空与空缓冲（encoder把null映射为Binary.Empty，仅判null挡不住）。
		Assertions.assertTrue(content.contains("if (_params_ != null && _params_.size() > 0)"),
				"发起端工厂守卫必须是 !=null && size()>0（AG1-F1：空缓冲decode读掩码抛异常丢整批结果）");
		// 返回必须是非null空对象：return _r_ 必须存在于工厂内（processResult对返回值直接setHash）。
		var guardIndex = content.indexOf("if (_params_ != null && _params_.size() > 0)");
		Assertions.assertTrue(guardIndex >= 0);
		var returnIndex = content.indexOf("return _r_;", guardIndex);
		Assertions.assertTrue(returnIndex > guardIndex,
				"工厂必须在守卫之后返回_r_（非null空对象），不得改成返回null");
		Assertions.assertTrue(content.contains("var _r_ = new Zeze.Arch.Gen.TestAg1F1RedirectAllEmptyParamsGuard.EmptyAbleResult();"),
				"工厂必须先构造空结果对象");
	}
}
