package Zeze.Arch.Gen;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
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
 * FND8-83回归：Gen对抽象Serializable形参/元素的编解码资格判定不对称——encode侧有
 * !isAbstract守卫，decode侧对抽象Zeze Serializable形参无条件生成new 抽象类()（生成
 * 源码不可编译：内存路径启动即炸；文件模式把必然编译不过的.java写进源码树后脚本照样
 * 成功退出）；集合/映射元素不满足Zeze编码谓词时容器退化为WriteJavaObject/ReadJavaObject，
 * 抽象Zeze Bean元素不是java.io.Serializable，运行时才抛NotSerializableException。
 * 修复：encode/decode共用checkGenElement分层谓词（Bean/Data多态通道除外，抽象Zeze
 * Serializable形参生成期拒绝；容器兜底要求元素java.io.Serializable，报错指明元素）；
 * decode集合/映射分支谓词与encode对齐；MethodOverride结果类型检查补!isAbstract
 * （RedirectFuture与RedirectAll两分支，原先反射getConstructor不查可实例化性）；
 * 文件模式增加写盘前试编译防线（-DGenFileTryCompile开启）。
 */
@Fast
@Isolated // genFileSrcRoot/tryCompileGeneratedFile是GenModule.instance上的JVM级全局开关
public class TestFnd883GenAbstractSerializablePredicate {

	/** 抽象Zeze Serializable形参：decode侧无法反建实例（缺陷主体）。 */
	public abstract static class AbstractBeanParam extends Bean {
	}

	/** 具体Zeze Serializable形参：合法形态护栏。 */
	public static class ConcreteBeanParam extends Bean {
		@Override
		public void encode(Zeze.Serialize.ByteBuffer bb) {
		}

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
		}
	}

	/** 既非Zeze Serializable也非java.io.Serializable的元素。 */
	public static class PlainPojo {
	}

	/** 抽象但实现java.io.Serializable的元素：容器可走Java序列化兜底（运行时按实际子类）。 */
	public abstract static class AbstractJavaSer implements java.io.Serializable {
	}

	@SuppressWarnings("unused")
	public static class ModuleSignatures {
		public void mAbstractBean(int hash, AbstractBeanParam p) {
		}

		public void mBeanPoly(int hash, Bean p) {
		}

		public void mData(int hash, Data p) {
		}

		public void mConcreteBean(int hash, ConcreteBeanParam p) {
		}

		public void mString(int hash, String s) {
		}

		public void mListAbstract(int hash, ArrayList<AbstractBeanParam> ps) {
		}

		public void mListPlain(int hash, ArrayList<PlainPojo> ps) {
		}

		public void mListJavaSer(int hash, ArrayList<AbstractJavaSer> ps) {
		}

		public void mListKnownElem(int hash, List<Long> ps) {
		}

		public void mMapAbstractValue(int hash, java.util.HashMap<Long, AbstractBeanParam> ps) {
		}
	}

	@TempDir
	Path tempDir;

	@BeforeEach
	public void setUp() {
		Assertions.assertNull(GenModule.instance.genFileSrcRoot, "测试前提：全局genFileSrcRoot默认关闭");
		Assertions.assertFalse(GenModule.instance.tryCompileGeneratedFile, "测试前提：试编译默认关闭");
	}

	@AfterEach
	public void tearDown() {
		GenModule.instance.genFileSrcRoot = null; // 全局开关必须恢复
		GenModule.instance.tryCompileGeneratedFile = false;
	}

	private static Parameter inputParam(String methodName) {
		for (Method m : ModuleSignatures.class.getDeclaredMethods()) {
			if (m.getName().equals(methodName) && m.getParameterCount() == 2)
				return m.getParameters()[1];
		}
		throw new AssertionError("method not found: " + methodName);
	}

	private static List<Parameter> paramsOf(String methodName) {
		return List.of(inputParam(methodName));
	}

	/** 抽象Zeze Serializable形参：encode与decode两侧都必须生成期拒绝，报错含方法/形参上下文。 */
	@Test
	public void testAbstractSerializableParamRejected() {
		var paramName = inputParam("mAbstractBean").getName();
		for (var useEncode : new boolean[]{true, false}) {
			var ex = Assertions.assertThrows(UnsupportedOperationException.class,
					() -> runGen(useEncode, paramsOf("mAbstractBean")),
					(useEncode ? "encode" : "decode") + "侧必须拒绝抽象Serializable形参");
			Assertions.assertTrue(ex.getMessage().contains(ModuleSignatures.class.getName()), "报错须含模块类名");
			Assertions.assertTrue(ex.getMessage().contains("mAbstractBean"), "报错须含方法名");
			Assertions.assertTrue(ex.getMessage().contains(paramName), "报错须含形参名: " + ex.getMessage());
			Assertions.assertTrue(ex.getMessage().contains("#1"), "报错须含形参序号");
			Assertions.assertTrue(ex.getMessage().contains(AbstractBeanParam.class.getName()), "报错须含类型名");
		}
	}

	/** 合法形态护栏：多态Bean/Data通道、具体Bean、String、已知元素容器全部照常生成。 */
	@Test
	public void testLegalFormsStillGenerate() throws Exception {
		for (var methodName : new String[]{"mBeanPoly", "mData", "mConcreteBean", "mString", "mListKnownElem"}) {
			var enc = new StringBuilderCs();
			Gen.instance.genEncode(enc, "", "_b_", "_m_", "", paramsOf(methodName), null);
			Assertions.assertTrue(enc.toString().contains("_b_"), methodName + " encode必须产出代码");
			var dec = new StringBuilderCs();
			Gen.instance.genDecode(dec, "", "_b_", "_m_", "", paramsOf(methodName));
			Assertions.assertTrue(dec.toString().contains("_b_"), methodName + " decode必须产出代码");
		}
	}

	/** 集合/映射的抽象Zeze Serializable或不可序列化元素：生成期拒绝且报错指明元素类型。 */
	@Test
	public void testContainerElementRejected() throws Exception {
		for (var useEncode : new boolean[]{true, false}) {
			for (var methodName : new String[]{"mListAbstract", "mListPlain", "mMapAbstractValue"}) {
				var ex = Assertions.assertThrows(UnsupportedOperationException.class,
						() -> runGen(useEncode, paramsOf(methodName)),
						methodName + (useEncode ? " encode" : " decode") + "侧必须拒绝不可编码元素");
				var elemName = methodName.equals("mMapAbstractValue")
						? AbstractBeanParam.class.getName() : methodName.equals("mListAbstract")
						? AbstractBeanParam.class.getName() : PlainPojo.class.getName();
				Assertions.assertTrue(ex.getMessage().contains(elemName),
						"报错须指明元素类型（原先报容器误导排障）: " + ex.getMessage());
			}
		}
	}

	/** java.io.Serializable元素走Java序列化兜底：允许，且decode不再对抽象元素生成new（不可编译源）。 */
	@Test
	public void testJavaSerializableElementFallback() throws Exception {
		var paramName = inputParam("mListJavaSer").getName();
		var enc = new StringBuilderCs();
		Gen.instance.genEncode(enc, "", "_b_", "_m_", "", paramsOf("mListJavaSer"), null);
		Assertions.assertTrue(enc.toString().contains("_b_.WriteJavaObject(" + paramName + ");"),
				"javaSer元素容器走WriteJavaObject兜底");

		var dec = new StringBuilderCs();
		Gen.instance.genDecode(dec, "", "_b_", "_m_", "", paramsOf("mListJavaSer"));
		var decText = dec.toString();
		Assertions.assertTrue(decText.contains(paramName + " = _b_.ReadJavaObject();"), "decode对称走ReadJavaObject兜底");
		Assertions.assertFalse(decText.contains("new " + AbstractJavaSer.class.getName().replace('$', '.')),
				"decode不得对抽象元素生成new（FND8-83：生成不可编译源）");
	}

	/** 孪生：MethodOverride结果类型抽象必须fail-fast（RedirectFuture与RedirectAll两分支）。 */
	public abstract static class AbstractResult extends RedirectResult {
		public AbstractResult() {
		}
	}

	public static class ConcreteResult extends RedirectResult {
		public long value;
	}

	@SuppressWarnings("unused")
	public static class ResultSignatures {
		@RedirectToServer
		public RedirectFuture<AbstractResult> futureAbstract(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectToServer
		public RedirectFuture<ConcreteResult> futureConcrete(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectAll
		public RedirectAllFuture<AbstractResult> allAbstract(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectAll
		public RedirectAllFuture<ConcreteResult> allConcrete(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testAbstractResultTypeRejected() throws Exception {
		var m1 = ResultSignatures.class.getMethod("futureAbstract", int.class);
		var ex1 = Assertions.assertThrows(IllegalStateException.class,
				() -> new MethodOverride(m1, m1.getAnnotation(RedirectToServer.class)));
		Assertions.assertTrue(ex1.getMessage().contains("Abstract"), ex1.getMessage());

		var m2 = ResultSignatures.class.getMethod("allAbstract", int.class);
		var ex2 = Assertions.assertThrows(IllegalStateException.class,
				() -> new MethodOverride(m2, m2.getAnnotation(RedirectAll.class)));
		Assertions.assertTrue(ex2.getMessage().contains("Abstract"), ex2.getMessage());
	}

	/** 护栏：具体结果类型两分支照常通过。 */
	@Test
	public void testConcreteResultTypeAccepted() throws Exception {
		var m1 = ResultSignatures.class.getMethod("futureConcrete", int.class);
		Assertions.assertTrue(new MethodOverride(m1, m1.getAnnotation(RedirectToServer.class)).resultFields.size() >= 0);
		var m2 = ResultSignatures.class.getMethod("allConcrete", int.class);
		Assertions.assertNotNull(new MethodOverride(m2, m2.getAnnotation(RedirectAll.class)));
	}

	/** 文件模式试编译（第二道防线）：可编译产物正常写盘；不可编译产物写盘前拦下。 */
	@Test
	public void testFileModeTryCompile() throws Exception {
		var dummyApp = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return null; // 文件模式只读app.getClass()匹配构造器，不触碰zeze
			}
		};
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		GenModule.instance.tryCompileGeneratedFile = true;

		// 正路径：顶层模块类可编译，照常写盘
		Assertions.assertNull(GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{A7Fnd883TopModule.class}));
		var goodFile = tempDir.resolve(GenModule.REDIRECT_PREFIX
				+ A7Fnd883TopModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(goodFile), "可编译产物必须写盘");

		// 负路径：嵌套fixture类的生成源码（extends含$）不可编译——写盘前必须拦下
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{NestModule.class}));
		Assertions.assertTrue(String.valueOf(ex.getCause()).contains("Unable to compile")
						|| ex.getMessage().contains(NestModule.class.getName()),
				"试编译失败须带编译诊断与模块上下文: " + ex);
		var badFile = tempDir.resolve(GenModule.REDIRECT_PREFIX
				+ NestModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertFalse(Files.exists(badFile), "不可编译产物不得写盘（原先写盘后脚本照样成功退出）");
	}

	/** 嵌套模块fixture：生成源码extends二进制名含$，javac必然解析失败（试编译负路径形态）。 */
	public static class NestModule {
		public static final int ModuleId = 8832;
		public static final String ModuleFullName = "TestFnd883.NestModule";

		@RedirectToServer
		public void ping(int serverId) {
		}
	}

	private static void runGen(boolean useEncode, List<Parameter> params) throws Exception {
		if (useEncode)
			Gen.instance.genEncode(new StringBuilderCs(), "", "_b_", "_m_", "", params, null);
		else
			Gen.instance.genDecode(new StringBuilderCs(), "", "_b_", "_m_", "", params);
	}
}
