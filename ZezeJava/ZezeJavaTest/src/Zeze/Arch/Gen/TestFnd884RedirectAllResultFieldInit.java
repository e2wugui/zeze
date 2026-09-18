package Zeze.Arch.Gen;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.StringBuilderCs;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND8-84回归：结果类抽象集合/映射字段未初始化时decode NPE——genDecode对isField的
 * 抽象容器跳过new分配、直接add/put，用户结果类"赋值式填充"（执行端new结果对象赋集合，
 * 发起端decode的是本地new的空结果对象）即触发：发起端RedirectAllContext.processResult
 * 在ctx锁内NPE冲出for循环，同报文后续hash全部丢失，该次RedirectAll只能等超时兜底完成，
 * await正常返回拿到"看似成功"的残缺结果。RedirectHash/ToServer应答回调走同一genDecode，
 * 后果更重（await永久挂死，无超时兜底）。
 * 修复：decode对抽象容器字段生成"判空后new"兜底（已初始化不覆盖，保留用户实现选择）；
 * 孪生T1（抽象Serializable字段无法new兜底）改为实例化探测初始化器、未初始化生成期拒绝。
 */
@Fast
@Isolated // genFileSrcRoot是GenModule.instance上的JVM级全局开关，独占运行
public class TestFnd884RedirectAllResultFieldInit {

	public static class ResultWithFields extends RedirectResult {
		public java.util.List<Long> xs; // 主缺陷：未初始化抽象集合字段
		public java.util.List<Long> ys = new java.util.LinkedList<>(); // 已初始化：保留用户实现选择
		public java.util.ArrayList<Long> zs; // 具体类型：原有无条件new路径
		public java.util.Map<Long, String> m0; // 未初始化抽象映射字段
	}

	public abstract static class AbstractResultBean extends Bean {
	}

	public static class ConcreteResultBean extends AbstractResultBean {
		@Override
		public void encode(ByteBuffer bb) {
		}

		@Override
		public void decode(IByteBuffer bb) {
		}
	}

	/** T1：抽象Serializable结果字段未初始化——生成期拒绝。 */
	@SuppressWarnings("unused")
	public static class ResultAbstractFieldNotInit extends RedirectResult {
		public AbstractResultBean bad;
	}

	/** T1护栏：声明处初始化的抽象Serializable字段——允许，decode原位decode。 */
	@SuppressWarnings("unused")
	public static class ResultAbstractFieldInit extends RedirectResult {
		public AbstractResultBean good = new ConcreteResultBean();
	}

	public static class AllModule {
		public static final int ModuleId = 8841;
		public static final String ModuleFullName = "TestFnd884.AllModule";

		@RedirectAll
		public RedirectAllFuture<ResultWithFields> collect(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectToServer
		public RedirectFuture<ResultWithFields> fetch(int hash, long arg) {
			throw new UnsupportedOperationException();
		}
	}

	@TempDir
	Path tempDir;

	@BeforeEach
	public void setUp() {
		Assertions.assertNull(GenModule.instance.genFileSrcRoot, "测试前提：全局genFileSrcRoot默认关闭");
	}

	@AfterEach
	public void tearDown() {
		GenModule.instance.genFileSrcRoot = null; // 全局开关必须恢复
	}

	/** 抽象容器字段decode必须判空后new兜底；具体类型保持无条件new；已初始化不覆盖。 */
	@Test
	public void testAbstractContainerFieldNullGuard() throws Exception {
		var sb = new StringBuilderCs();
		Gen.instance.genDecode(sb, "", "_b_", "_m_", "_r_.", fieldsOf(ResultWithFields.class));
		var dec = sb.toString();
		Assertions.assertTrue(dec.contains("if (_r_.xs == null)") && dec.contains("_r_.xs = new java.util.ArrayList<>();"),
				"未初始化抽象集合字段必须判空后new（FND8-84：原先跳过分配直接add，null即NPE）");
		Assertions.assertTrue(dec.contains("if (_r_.m0 == null)") && dec.contains("_r_.m0 = new java.util.HashMap<>();"),
				"未初始化抽象映射字段必须判空后new");
		Assertions.assertTrue(dec.contains("if (_r_.ys == null)"),
				"已初始化字段同样带判空守卫（运行时非空不覆盖，保留LinkedList等用户实现选择）");
		Assertions.assertTrue(dec.contains("_r_.zs = new java.util.ArrayList<>();"),
				"具体容器类型保持无条件new（既有路径不回归）");
	}

	/** T1：抽象Serializable结果字段未初始化生成期拒绝；声明处初始化的允许。 */
	@Test
	public void testAbstractSerializableFieldFailFast() throws Exception {
		var ex = Assertions.assertThrows(UnsupportedOperationException.class,
				() -> Gen.instance.genDecode(new StringBuilderCs(), "", "_b_", "_m_", "_r_.",
						fieldsOf(ResultAbstractFieldNotInit.class)));
		Assertions.assertTrue(ex.getMessage().contains("not initialized"), ex.getMessage());
		Assertions.assertTrue(ex.getMessage().contains("bad"), "报错须指明字段: " + ex.getMessage());

		var sb = new StringBuilderCs();
		Gen.instance.genDecode(sb, "", "_b_", "_m_", "_r_.", fieldsOf(ResultAbstractFieldInit.class));
		var dec = sb.toString();
		Assertions.assertTrue(dec.contains("_r_.good.decode(_b_);"), "已初始化抽象字段原位decode");
		Assertions.assertFalse(dec.contains("new Zeze.Arch.Gen.TestFnd884RedirectAllResultFieldInit.AbstractResultBean"),
				"不得对抽象字段生成new");
	}

	/** RedirectAll结果解码与RedirectHash/ToServer应答回调共用同一genDecode：生成产物内均须带守卫。 */
	@Test
	public void testGeneratedModuleDecoderHasGuard() throws Exception {
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		var dummyApp = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return null; // 文件模式只读app.getClass()匹配构造器，不触碰zeze
			}
		};
		Assertions.assertNull(GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{AllModule.class}));
		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX + AllModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file), "生成文件必须存在");
		var content = Files.readString(file);
		Assertions.assertTrue(countOccurrences(content, "if (_r_.xs == null)") >= 2,
				"RedirectAll结果解码与RedirectFuture应答回调两处解码都必须带判空守卫（T2同修）");
	}

	private static List<Field> fieldsOf(Class<?> cls) {
		return List.of(cls.getFields());
	}

	private static int countOccurrences(String content, String token) {
		int count = 0;
		for (int index = content.indexOf(token); index >= 0; index = content.indexOf(token, index + token.length()))
			++count;
		return count;
	}
}
