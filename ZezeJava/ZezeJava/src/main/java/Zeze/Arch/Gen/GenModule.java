package Zeze.Arch.Gen;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import Zeze.AppBase;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectToServer;
import Zeze.Collections.BeanFactory;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.StringBuilderCs;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/**
 * 把模块的方法调用发送到其他服务器实例上执行。
 * 被重定向的方法用注解标明(RedirectToServer,RedirectHash,RedirectAll)。
 * 被重定向的方法需要是virtual的(非private非final非static的)。
 * <p>
 * 实现方案：
 * Game.App创建Module的时候调用回调。
 * 在回调中判断是否存在需要拦截的方法。
 * 如果需要就动态生成子类实现代码并编译并返回新的实例。
 * 可以提供和原来模块一致的接口。
 */
public final class GenModule extends ReentrantLock {
	public static final String REDIRECT_PREFIX = "Redirect_";
	public static final GenModule instance = new GenModule();
	private static final Pattern genericPat = Pattern.compile("<.+>");

	private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
	// 冷路径生成类缓存（热模块产物装载/定义进各HotModule，不进此表）。
	private final HashMap<String, Class<?>> genClassMap = new HashMap<>();

	public InMemoryJavaCompiler getCompiler() {
		return compiler;
	}

	private GenModule() {
		compiler.ignoreWarnings();
	}

	public static <T extends IModule> Constructor<T> getCtor(@NotNull Class<?> cls, @NotNull AppBase app) throws ReflectiveOperationException {
		var appClass = app.getClass();
		@SuppressWarnings("unchecked")
		var ctors = (Constructor<T>[])cls.getDeclaredConstructors();
		for (var ctor : ctors) {
			if ((ctor.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0 &&
					ctor.getParameterCount() == 1 && ctor.getParameters()[0].getType().isAssignableFrom(appClass))
				return ctor;
		}
		for (var ctor : ctors) {
			if ((ctor.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0 && ctor.getParameterCount() == 0)
				return ctor;
		}
		throw new NoSuchMethodException("No suitable constructor for redirect module: " + cls.getName());
	}

	public static <T extends IModule> T newModule(@NotNull Class<?> cls, @NotNull AppBase app) throws ReflectiveOperationException {
		@SuppressWarnings("unchecked")
		var ctor = (Constructor<T>)getCtor(cls, app);
		if (ctor.getParameterCount() != 1)
			throw new NoSuchMethodException("No suitable constructor for redirect module: " + cls.getName());
		return ctor.newInstance(app);
	}

	private static String getRedirectClassName(@NotNull Class<?> moduleClass) {
		String className = moduleClass.getName();
		return className.startsWith(REDIRECT_PREFIX) ? className : REDIRECT_PREFIX + className.replace('.', '_');
	}

	/**
	 * 热模块装载器契约：由HotModule实现（定义装载器即模块装载器，无需额外传参）。
	 * 故意不依赖Zeze.Hot：依赖方向保持GenModule被Hot侧引用的单向。
	 */
	public interface RedirectClassSink {
		/**
		 * 只从本模块jar装载打包好的Redirect_子类（Distribute.pack产出），绕过双亲委派。
		 *
		 * @throws ClassNotFoundException 本jar不存在该子类
		 */
		Class<?> findRedirectClass(String className) throws ClassNotFoundException;

		/**
		 * 把运行时兜底编译的产物定义进本装载器：每模块版本一份，换代即隔离；
		 * 同名define每装载器至多一次（findLoadedClass守卫）。
		 */
		Class<?> defineRedirectClass(String className, byte[] byteCode);
	}

	public static <T extends IModule> @NotNull T createRedirectModule(@NotNull Class<T> moduleClass, @NotNull AppBase app) {
		try {
			Class<?> genClass;
			// 热模块：只从模块jar装载打包子类（与批量路径同契约）；冷模块走classpath。
			if (moduleClass.getClassLoader() instanceof RedirectClassSink sink) {
				try {
					genClass = sink.findRedirectClass(getRedirectClassName(moduleClass));
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException("hot module redirect class not packaged in module jar: "
							+ moduleClass.getName(), e);
				}
			} else
				genClass = Class.forName(getRedirectClassName(moduleClass));
			return newModule(genClass, app);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	// FND7-67：沿类层级向上收集带注解方法（到IModule为止）。原先仅扫
	// getDeclaredMethods：基类声明的redirect方法既不生成拦截子类方法、
	// 也不注册redirect.handles，调用静默本地执行且远程不可达、无任何告警，
	// 与方法签名非法时的fail-fast形成反差。同签名（名字+参数类型）去重，
	// 派生类声明优先（覆盖者的注解生效）。空=没有需要重定向的方法。
	private static ArrayList<MethodOverride> collectOverrides(@NotNull Class<?> moduleClass) {
		var overridesBySignature = new LinkedHashMap<String, MethodOverride>();
		for (var cls = moduleClass; cls != null && cls != IModule.class; cls = cls.getSuperclass()) {
			for (var method : cls.getDeclaredMethods()) {
				String methodKey = methodKey(method);
				if (overridesBySignature.containsKey(methodKey))
					continue; // 派生类同签名覆盖已收集，基类声明忽略
				for (var anno : method.getAnnotations()) {
					var type = anno.annotationType();
					if (type == RedirectToServer.class || type == RedirectHash.class || type == RedirectAll.class) {
						overridesBySignature.put(methodKey, new MethodOverride(method, anno));
						break;
					}
				}
			}
		}
		var overrides = new ArrayList<>(overridesBySignature.values());
		overrides.sort(Comparator.comparing(o -> o.method.getName())); // 按方法名排序，避免每次生成结果发生变化。
		return overrides;
	}

	private static String methodKey(@NotNull Method method) {
		var sb = new StringBuilder(method.getName());
		for (var paramType : method.getParameterTypes())
			sb.append(':').append(paramType.getName());
		return sb.toString();
	}

	/**
	 * 生成模式（构建期）：把带redirect注解模块的Redirect_子类源码写到srcRoot（总是覆盖）。
	 * 只产出源码，不装载不实例化；退出与否由调用方（工具main/应用Start）决定。
	 *
	 * @param tryCompile 写盘前逐模块内存javac试编译，不可编译产物不落盘（FND8-83，默认false）
	 */
	public void generateRedirectSources(@NotNull String srcRoot, @NotNull AppBase userApp,
										@NotNull Class<?> @NotNull [] moduleClasses, boolean tryCompile) {
		lock();
		try {
			for (Class<?> moduleClass : moduleClasses) {
				try {
					if (moduleClass.getName().startsWith(REDIRECT_PREFIX)) // 预防二次replace
						continue;
					var overrides = collectOverrides(moduleClass);
					if (overrides.isEmpty())
						continue; // 没有需要重定向的方法。

					var genClassName = getRedirectClassName(moduleClass);
					var code = genModuleCode(genClassName, moduleClass, overrides, userApp);
					// FND8-83：写盘前试编译，失败即中止不落盘；只取字节码，不在装载器define。
					if (tryCompile)
						compiler.compileAllToByteCode(Map.of(genClassName, code));
					byte[] oldBytes = null;
					byte[] newBytes = code.getBytes(StandardCharsets.UTF_8);
					var file = new File(srcRoot, genClassName + ".java");
					if (file.exists()) {
						oldBytes = Files.readAllBytes(file.toPath());
						if (Arrays.equals(oldBytes, newBytes))
							System.out.println("  Existed File: " + file.getAbsolutePath());
						else {
							System.out.println("Overwrite File: " + file.getAbsolutePath());
							oldBytes = null;
						}
					} else
						System.out.println("      New File: " + file.getAbsolutePath());
					if (oldBytes == null)
						AtomicFileWriter.replace(file.toPath(), newBytes);

				} catch (Exception e) {
					throw new IllegalStateException("module class: " + moduleClass.getName(), e);
				}
			}
		} finally {
			unlock();
		}

		System.out.println("---------------");
		System.out.println("New Source File Has Generate. Re-Compile Need.");
	}

	// ③装载不命中路径的待编译条目：源码与编译产物的归属装载器绑定（热模块为其
	// HotModule；冷模块null=编译器装载器），同批收集后单次javac。
	private record PendingCompile(String genClassName, String code, RedirectClassSink sink) {
	}

	/**
	 * 运行时装载Redirect_子类并实例化模块（构建期源码生成走{@link #generateRedirectSources}）。
	 * 每个模块按序尝试：①热模块从模块jar装载打包好的子类（免编译）；②冷模块从进程缓存/
	 * classpath装载已存在的子类；③装载不命中的生成源码，批量编译后按归属define（热模块
	 * 进其HotModule，冷模块进编译器装载器）。无redirect方法的模块直接用原始模块类实例化。
	 */
	public @NotNull IModule @NotNull [] createRedirectModules(@NotNull AppBase userApp,
															  @NotNull Class<?> @NotNull [] moduleClasses) {
		lock();
		try {
			int n = moduleClasses.length;
			// 与moduleClasses索引对齐，同一下标互斥占用：就绪的子类（装载命中或编译后
			// 回填）/待编译条目；两者皆null=该模块没有redirect方法。
			var genClasses = new Class<?>[n];
			var pending = new PendingCompile[n];
			for (var i = 0; i < n; i++) {
				var moduleClass = moduleClasses[i];
				try {
					if (moduleClass.getName().startsWith(REDIRECT_PREFIX)) // 预防二次replace
						continue;
					var overrides = collectOverrides(moduleClass);
					if (overrides.isEmpty())
						continue; // 没有需要重定向的方法。

					var genClassName = getRedirectClassName(moduleClass);
					// ①热模块装载优先：装载模块jar里打包好的子类（Distribute.pack产出，
					// 与模块类同批构建），免运行时编译。findRedirectClass绕过双亲委派
					// 只查本jar，冷classpath残留无法抢先。
					var sink = moduleClass.getClassLoader() instanceof RedirectClassSink s ? s : null;
					if (sink != null) {
						try {
							var genClass = sink.findRedirectClass(genClassName);
							checkRedirectSuperclass(genClass, moduleClass, "module jar");
							genClasses[i] = genClass;
							continue;
						} catch (ClassNotFoundException ignored) {
						}
					}
					// ②冷装载：进程缓存或classpath上已存在的子类（热模块的子类在模块jar里，冷链不可见）。
					Class<?> genClass = genClassMap.get(genClassName);
					if (genClass == null) {
						try {
							genClass = Class.forName(genClassName);
							genClassMap.put(genClassName, genClass);
						} catch (ClassNotFoundException ignored) {
						}
					}
					if (genClass != null) {
						// 冷类身份进程内不变，命中陈旧父类即装载器混用。
						checkRedirectSuperclass(genClass, moduleClass, "cold cache");
						genClasses[i] = genClass;
						continue;
					}
					// ③装载不命中：生成源码，收集待批量编译。
					String code = genModuleCode(genClassName, moduleClass, overrides, userApp);
					pending[i] = new PendingCompile(genClassName, code, sink);
				} catch (Exception e) {
					throw new IllegalStateException("module class: " + moduleClass.getName(), e);
				}
			}

			// 批量编译（单次javac）：产物按归属define回填genClasses。
			compilePending(pending, genClasses);

			var modules = new IModule[n];
			for (var i = 0; i < n; i++)
				modules[i] = newModule(genClasses[i] != null ? genClasses[i] : moduleClasses[i], userApp);
			return modules;
		} catch (Exception e) {
			throw Task.forceThrow(e);
		} finally {
			unlock();
		}
	}

	// 批量编译（单次javac）③的待编译条目，产物按归属define后回填genClasses：
	// 热模块产物进其HotModule（每代隔离、失败安装零残留）；冷模块产物进编译器
	// 装载器并写入genClassMap进程级缓存（冷类身份不变，下次直接装载）。
	private void compilePending(PendingCompile[] pending, Class<?>[] genClasses) throws ClassNotFoundException {
		var codes = new HashMap<String, String>();
		for (var p : pending)
			if (p != null)
				codes.put(p.genClassName, p.code);
		if (codes.isEmpty())
			return;
		var byteCodes = compiler.compileAllToByteCode(codes);
		for (var i = 0; i < pending.length; i++) {
			var p = pending[i];
			if (p == null)
				continue;
			if (p.sink != null)
				genClasses[i] = p.sink.defineRedirectClass(p.genClassName, byteCodes.get(p.genClassName));
			else {
				var cls = compiler.defineCompiled(p.genClassName);
				genClassMap.put(p.genClassName, cls);
				genClasses[i] = cls;
			}
		}
	}

	// 装载命中的生成类必须extends传入的moduleClass（身份自洽）：jar产物与模块类同批
	// 构建、冷缓存类身份进程内不变——错配即手搓jar或装载器混用，fail-fast。
	private static void checkRedirectSuperclass(@NotNull Class<?> genClass, @NotNull Class<?> moduleClass,
												@NotNull String source) {
		if (genClass.getSuperclass() != moduleClass)
			throw new IllegalStateException("redirect class mismatch (" + source + "): " + genClass.getName()
					+ ", superclass=" + genClass.getSuperclass().getName()
					+ ", expect=" + moduleClass.getName());
	}

	// FND7-67：方法去重键——同签名（名字+参数类型）视为同一个覆盖点，类层级收集中
	// 用于"派生类声明优先、基类声明忽略"。


	private static String genModuleCode(@NotNull String genClassName, @NotNull Class<?> moduleClass,
										@NotNull List<MethodOverride> overrides, @NotNull AppBase userApp) throws Exception {
		checkBeanFactorySymbol(moduleClass, overrides);
		var sb = new StringBuilderCs();
		sb.appendLine("// auto-generated @" + "formatter:off");
		sb.appendLine();
		sb.appendLine("public class {} extends {} {", genClassName, moduleClass.getName());
		sb.appendLine("    private final Zeze.Arch.RedirectBase _redirect_;");
		sb.appendLine();

		int moduleId = moduleClass.getField("ModuleId").getInt(null);
		var moduleFullName = (String)moduleClass.getField("ModuleFullName").get(null);
		var redirectFullNames = new HashSet<String>();

		var sbHandles = new StringBuilderCs();
		for (var m : overrides) {
			try {
				var parametersDefine = m.getDefineString();
				var methodNameHash = m.method.getName();
				String returnName;
				var type = m.method.getReturnType();
				if (type == void.class)
					returnName = "void";
				else if (type == RedirectFuture.class) {
					// 兜底：MethodOverride已对配对/raw泛型/非法实参fail-fast，这里到达时resultTypeName
					// 理应非null；仍判空防止未来改动漏网时拼出非法源码RedirectFuture<null>（晦涩编译失败）。
					if (m.resultTypeName == null)
						throw new IllegalStateException("RedirectFuture<> missing result type: "
								+ moduleClass.getName() + '.' + m.method.getName());
					returnName = "Zeze.Arch.RedirectFuture<" + m.resultTypeName + '>';
				} else if (type == RedirectAllFuture.class) {
					if (m.resultTypeName == null)
						throw new IllegalStateException("RedirectAllFuture<> missing result type: "
								+ moduleClass.getName() + '.' + m.method.getName());
					returnName = "Zeze.Arch.RedirectAllFuture<" + m.resultTypeName + '>';
				} else {
					throw new UnsupportedOperationException("Redirect return type Must Be void or RedirectFuture or RedirectAllFuture: "
							+ moduleClass.getName() + '.' + m.method.getName());
				}
				String modifier;
				int flags = m.method.getModifiers();
				if ((flags & Modifier.PUBLIC) != 0)
					modifier = "public ";
				else if ((flags & Modifier.PROTECTED) != 0)
					modifier = "protected ";
				else {
					throw new UnsupportedOperationException("Redirect method Must Be public or protected: "
							+ moduleClass.getName() + '.' + m.method.getName());
				}

				var redirectFullName = moduleFullName + ':' + m.method.getName();
				if (!redirectFullNames.add(redirectFullName))
					throw new UnsupportedOperationException("Duplicate redirect method name: " + redirectFullName);

				sb.appendLine("    @Override");
				sb.appendLine("    {}{} {}({}) {", modifier, returnName, m.method.getName(), parametersDefine); // m.getThrows() // 继承方法允许不标throws
				var prefix = "        ";
				if (!(m.annotation instanceof RedirectAll) && !returnName.equals("void")) {
					sb.appendLine("{}var _f_ = new Zeze.Arch.RedirectFuture<{}>();", prefix, m.resultTypeName);
					sb.appendLine("{}try {", prefix);
					prefix = "            ";
				}

				choiceTargetRunLoopback(sb, m, returnName, prefix);

				if (m.annotation instanceof RedirectAll) {
					genRedirectAll(sb, sbHandles, moduleId, moduleFullName, m);
					continue;
				}

				sb.appendLine("{}var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();", prefix);
				sb.appendLine("{}var _a_ = _p_.Argument;", prefix);
				sb.appendLine("{}_a_.setModuleId({});", prefix, moduleId);
				sb.appendLine("{}_a_.setRedirectType({});", prefix, m.getRedirectType());
				sb.appendLine("{}_a_.setHashCode({});", prefix, m.hashOrServerIdParameter.getName());
				if (m.oneByOne)
					sb.appendLine("{}_a_.setKey({});", prefix, m.keyHashCode);
				else
					sb.appendLine("{}_a_.setNoOneByOne(true);", prefix);
				sb.appendLine("{}_a_.setMethodFullName(\"{}:{}\");", prefix, moduleFullName, m.method.getName());
				sb.appendLine("{}_a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);", prefix);
				int version = m.annotation instanceof RedirectHash
						? ((RedirectHash)m.annotation).version()
						: ((RedirectToServer)m.annotation).version();
				if (version != 0)
					sb.appendLine("{}_a_.setVersion({});", prefix, version);
				if (!m.inputParameters.isEmpty()) {
					sb.appendLine("{}var _b_ = Zeze.Serialize.ByteBuffer.Allocate();", prefix);
					Gen.instance.genEncode(sb, prefix, "_b_", "_m_", "", m.inputParameters, m.redirectKeyParameter);
					sb.appendLine("{}_a_.setParams(new Zeze.Net.Binary(_b_));", prefix);
				}
				sb.appendLine();
				if (returnName.equals("void"))
					// FND8-86：Send失败（socket失效/背压）只返回false不抛异常，原先丢弃布尔值
					// 无redirect归因——经sendVoid封装，失败记带方法名的error日志（at-most-once不变）。
					sb.appendLine("{}_redirect_.sendVoid(_t_, _p_, \"{}:{}\");", prefix, moduleFullName, m.method.getName());
				else {
					sb.appendLine("{}if (!_p_.Send(_t_, _rpc_ -> {", prefix);
					sb.appendLine("{}    if (_rpc_.isTimeout()) {", prefix);
					sb.appendLine("{}        _f_.setException(Zeze.Arch.RedirectException.timeoutInstance);", prefix);
					sb.appendLine("{}        return Zeze.Transaction.Procedure.Success;", prefix);
					sb.appendLine("{}    }", prefix);
					sb.appendLine("{}    var _c_ = _rpc_.getResultCode();", prefix);
					sb.appendLine("{}    if (_c_ != Zeze.Transaction.Procedure.Success) {", prefix);
					sb.appendLine("{}        _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.REMOTE_EXECUTION, \"resultCode=\" + _c_));", prefix);
					sb.appendLine("{}        return Zeze.Transaction.Procedure.Success;", prefix);
					sb.appendLine("{}    }", prefix);
					if (m.resultType == Long.class)
						sb.appendLine("{}    _f_.setResult(_rpc_.Result.isNullParam() ? null : Zeze.Serialize.ByteBuffer.Wrap(_rpc_.Result.getParams()).ReadLong());", prefix);
					else if (m.resultType == String.class)
						sb.appendLine("{}    _f_.setResult(_rpc_.Result.isNullParam() ? null : Zeze.Util.Str.fromBinary(_rpc_.Result.getParams()));", prefix);
					else if (m.resultType == Binary.class)
						sb.appendLine("{}    _f_.setResult(_rpc_.Result.isNullParam() ? null : _rpc_.Result.getParams());", prefix);
					else {
						sb.appendLine("{}    {} _r_;", prefix, m.resultTypeName);
						sb.appendLine("{}    if (_rpc_.Result.isNullParam())", prefix);
						sb.appendLine("{}        _r_ = null;", prefix);
						sb.appendLine("{}    else {", prefix);
						if (Serializable.class.isAssignableFrom(m.resultClass)) {
							sb.appendLine("{}        _r_ = new {}();", prefix, genericPat.matcher(m.resultTypeName).replaceAll("<>"));
							sb.appendLine("{}        _r_.decode(_rpc_.Result.getParams().Wrap());", prefix);
						} else {
							sb.appendLine("{}        var _bb_ = _rpc_.Result.getParams().Wrap();", prefix);
							sb.appendLine("{}        _r_ = new {}();", prefix, genericPat.matcher(m.resultTypeName).replaceAll("<>"));
							Gen.instance.genDecode(sb, prefix + "        ", "_bb_", "_mm_", "_r_.", m.resultFields);
						}
						sb.appendLine("{}    }", prefix);
						sb.appendLine("{}    _f_.setResult(_r_);", prefix);
					}
					sb.appendLine("{}    return Zeze.Transaction.Procedure.Success;", prefix);
					if (m.annotation instanceof RedirectHash)
						sb.appendLine("{}}, {})) {", prefix, ((RedirectHash)m.annotation).timeout());
					else
						sb.appendLine("{}}, {})) {", prefix, ((RedirectToServer)m.annotation).timeout());
					if (m.annotation instanceof RedirectHash)
						sb.appendLine("{}    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, \"not found hash=\" + {}));", prefix, m.hashOrServerIdParameter.getName());
					else
						sb.appendLine("{}    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, \"not found serverId=\" + {}));", prefix, m.hashOrServerIdParameter.getName());
					sb.appendLine("{}}", prefix);
					prefix = "        ";
					sb.appendLine("{}} catch (Exception e) {", prefix);
					sb.appendLine("{}    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.LOCAL_EXECUTION, e.getMessage(), e));", prefix);
					sb.appendLine("{}}", prefix);
					sb.appendLine("{}return _f_;", prefix);
				}
				sb.appendLine("    }");
				sb.appendLine();

				// Handles
				sbHandles.appendLine("        _app_.getZeze().redirect.handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", moduleFullName, m.method.getName());
				sbHandles.appendLine("            Zeze.Transaction.TransactionLevel.{}, (_hash_, _params_) -> {", m.transactionLevel);
				boolean genLocal = false;
				for (int i = 0; i < m.inputParameters.size(); ++i) {
					var p = m.inputParameters.get(i);
					Gen.instance.genLocalVariable(sbHandles, "                ", p);
					genLocal = true;
				}
				if (genLocal)
					sbHandles.appendLine("                var _b_ = _params_.Wrap();");
				Gen.instance.genDecode(sbHandles, "                ", "_b_", "_m_", "", m.inputParameters);
				var normalCall = m.getNormalCallString();
				var sep = normalCall.isEmpty() ? "" : ", ";
				if (returnName.equals("void")) {
					sbHandles.appendLine("                super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
					sbHandles.appendLine("                return null;");
				} else {
					if (normalCall.isEmpty())
						sbHandles.appendLine("                //noinspection CodeBlock2Expr");
					sbHandles.appendLine("                return super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
				}
				if (m.resultType != null && Serializable.class.isAssignableFrom(m.resultClass)) {
					sbHandles.appendLine("            }, _result_ -> {");
					sbHandles.appendLine("                if (_result_ == null)");
					sbHandles.appendLine("                    return Zeze.Net.Binary.Empty;");
					sbHandles.appendLine("                var _r_ = ({})_result_;", m.resultTypeName);
					sbHandles.appendLine("                int _s_ = _r_.preAllocSize();");
					sbHandles.appendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate(Math.min(_s_, 65536));");
					sbHandles.appendLine("                _r_.encode(_b_);");
					sbHandles.appendLine("                int _t_ = _b_.WriteIndex;");
					sbHandles.appendLine("                if (_t_ > _s_)");
					sbHandles.appendLine("                    _r_.preAllocSize(_t_);");
					sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
					sbHandles.appendLine("            }, {}));", version);
				} else if (!m.resultFields.isEmpty()) {
					sbHandles.appendLine("            }, _result_ -> {");
					sbHandles.appendLine("                if (_result_ == null)");
					sbHandles.appendLine("                    return Zeze.Net.Binary.Empty;");
					sbHandles.appendLine("                var _r_ = ({})_result_;", m.resultTypeName);
					sbHandles.appendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
					Gen.instance.genEncode(sbHandles, "                ", "_b_", "_m_", "_r_.", m.resultFields, null);
					sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
					sbHandles.appendLine("            }, {}));", version);
				} else
					sbHandles.appendLine("            }, null, {}));", version);
			} catch (Exception e) {
				throw new IllegalStateException("generate redirect method failed: " + m.method.getName() + " in " + moduleClass.getName(), e);
			}
		}

		sb.appendLine("    @SuppressWarnings({\"unchecked\", \"RedundantSuppression\"})");
		var ctor = getCtor(moduleClass, userApp);
		if (ctor.getParameterCount() == 1) {
			sb.appendLine("    public {}({} _app_) {", genClassName, ctor.getParameters()[0].getType().getName().replace('$', '.'));
			sb.appendLine("        super(_app_);");
		} else
			sb.appendLine("    public {}(Zeze.AppBase _app_) {", genClassName);
		sb.appendLine("        _redirect_ = _app_.getZeze().redirect;");
		sb.appendLine();
		sb.append(sbHandles.toString());
		sb.appendLine("    }");
		sb.appendLine("}");
		return sb.toString();
	}

	// FND8-85：decode生成引用未限定的beanFactory，按"模块类父类链自带可访问静态
	// beanFactory"惯例解析（IModule无此契约）——生成期校验并给出修复提示。
	private static void checkBeanFactorySymbol
	(@NotNull Class<?> moduleClass, @NotNull List<MethodOverride> overrides) {
		var methodsNeedingFactory = new ArrayList<String>();
		for (var m : overrides) {
			var need = false;
			for (var p : m.allParameters) {
				if (p.getType() == Bean.class || p.getType() == Data.class) {
					need = true;
					break;
				}
			}
			if (!need) {
				for (var f : m.resultFields) {
					if (f.getType() == Bean.class || f.getType() == Data.class) {
						need = true;
						break;
					}
				}
			}
			if (need)
				methodsNeedingFactory.add(m.method.getName());
		}
		if (methodsNeedingFactory.isEmpty())
			return;
		// getSuperclass()只走类不走接口，链条止于Object（IModule/AbstractModule无beanFactory）
		for (var cls = moduleClass; cls != null; cls = cls.getSuperclass()) {
			for (var field : cls.getDeclaredFields()) {
				if (!field.getName().equals("beanFactory") || !Modifier.isStatic(field.getModifiers()))
					continue;
				if (!BeanFactory.class.isAssignableFrom(field.getType()))
					throw new UnsupportedOperationException("redirect Bean/Data param unsupported: beanFactory "
							+ "field type must be Zeze.Collections.BeanFactory, but is " + field.getType().getName()
							+ " (module " + moduleClass.getName() + ", methods " + methodsNeedingFactory + ")");
				if ((field.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0)
					throw new UnsupportedOperationException("redirect Bean/Data param unsupported: beanFactory field "
							+ "must be public or protected (the generated subclass lives in the default package and "
							+ "inherits across packages), module " + cls.getName() + ", methods " + methodsNeedingFactory);
				return; // 父类链上找到可访问的静态beanFactory
			}
		}
		throw new UnsupportedOperationException("redirect Bean/Data param unsupported: module "
				+ moduleClass.getName() + " (methods " + methodsNeedingFactory + ") has no accessible static "
				+ "beanFactory in its superclass chain; declare: protected static final "
				+ "Zeze.Collections.BeanFactory beanFactory = new Zeze.Collections.BeanFactory();");
	}

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	private static void choiceTargetRunLoopback(StringBuilderCs sb, MethodOverride m, String returnName, String
			prefix) {
		if (m.annotation instanceof RedirectHash) {
			sb.appendLine("{}var _t_ = _redirect_.choiceHash(this, {}, {});",
					prefix, m.hashOrServerIdParameter.getName(), m.getConcurrentLevelSource());
		} else if (m.annotation instanceof RedirectToServer) {
			sb.appendLine("{}var _t_ = _redirect_.choiceServer(this, {}, {});",
					prefix, m.hashOrServerIdParameter.getName(), ((RedirectToServer)m.annotation).orOtherServer());
		} else if (m.annotation instanceof RedirectAll)
			return; // RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。

		sb.appendLine("{}if (_t_ == null) { // local: loop-back", prefix);
		if (returnName.equals("void")) {
			sb.appendLine("{}    _redirect_.runVoid(Zeze.Transaction.TransactionLevel.{},", prefix, m.transactionLevel);
			sb.appendLine("{}        () -> super.{}({}), \"RedirectLoopBack_{}\");", prefix, m.method.getName(), m.getBaseCallString(), m.method.getName());
			sb.appendLine("{}    return;", prefix);
		} else {
			sb.appendLine("{}    return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.{},", prefix, m.transactionLevel);
			sb.appendLine("{}        () -> super.{}({}), \"RedirectLoopBack_{}\");", prefix, m.method.getName(), m.getBaseCallString(), m.method.getName());
		}
		sb.appendLine("{}}", prefix);
		sb.appendLine();
	}

	private static void genRedirectAll(StringBuilderCs sb, StringBuilderCs sbHandles,
									   int moduleId, String moduleFullName, MethodOverride m) throws Exception {
		sb.append("        var _c_ = new Zeze.Arch.RedirectAllContext<>({}, ", m.hashOrServerIdParameter.getName());
		if (m.resultTypeName != null) {
			if (m.resultFields.isEmpty())
				sb.appendLine("_params_ -> new {}());", m.resultTypeName);
			else {
				sb.appendLine("_params_ -> {");
				sb.appendLine("            var _r_ = new {}();", m.resultTypeName);
				// encoder把null结果映射为Binary.Empty，守卫必须同时判空缓冲（size()==0时decode
				// 读空缓冲抛异常会冲出processResult的per-hash循环）；但_r_必须始终返回非null
				// 空对象——processResult对返回值直接setHash，返回null会NPE。
				sb.appendLine("            if (_params_ != null && _params_.size() > 0) {");
				sb.appendLine("                var _b_ = _params_.Wrap();");
				Gen.instance.genDecode(sb, "                ", "_b_", "_m_", "_r_.", m.resultFields);
				sb.appendLine("            }");
				sb.appendLine("            return _r_;");
				sb.appendLine("        });");
			}
		} else
			sb.appendLine("null);");
		sb.appendLine("        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();");
		sb.appendLine("        var _a_ = _p_.Argument;");
		sb.appendLine("        _a_.setModuleId({});", moduleId);
		sb.appendLine("        _a_.setHashCodeConcurrentLevel({});", m.hashOrServerIdParameter.getName());
		sb.appendLine("        _a_.setMethodFullName(\"{}:{}\");", moduleFullName, m.method.getName());
		sb.appendLine("        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);");
		sb.appendLine("        _a_.setSessionId(_redirect_.providerApp.providerDirectService.addManualContextWithTimeout(_c_, {}));", ((RedirectAll)m.annotation).timeout());
		int version = ((RedirectAll)m.annotation).version();
		if (version != 0)
			sb.appendLine("        _a_.setVersion({});", version);
		if (!m.inputParameters.isEmpty()) {
			sb.appendLine("        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
			Gen.instance.genEncode(sb, "        ", "_b_", "_m_", "", m.inputParameters, m.redirectKeyParameter);
			sb.appendLine("        _a_.setParams(new Zeze.Net.Binary(_b_));");
		}
		if (m.resultType != null)
			sb.appendLine("        return _redirect_.redirectAll(this, _p_, _c_);");
		else
			sb.appendLine("        _redirect_.redirectAll(this, _p_, _c_);");
		sb.appendLine("    }");
		sb.appendLine();

		// handles
		sbHandles.appendLine("        _app_.getZeze().redirect.handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", moduleFullName, m.method.getName());
		sbHandles.appendLine("            Zeze.Transaction.TransactionLevel.{}, (_hash_, _params_) -> {", m.transactionLevel);
		if (!m.inputParameters.isEmpty()) {
			sbHandles.appendLine("                var _b_ = _params_.Wrap();");
			for (int i = 0; i < m.inputParameters.size(); ++i) {
				var p = m.inputParameters.get(i);
				Gen.instance.genLocalVariable(sbHandles, "                ", p);
			}
			Gen.instance.genDecode(sbHandles, "                ", "_b_", "_m_", "", m.inputParameters);
		}

		var normalCall = m.getNormalCallString();
		if (m.resultType != null) {
			if (normalCall.isEmpty())
				sbHandles.appendLine("                //noinspection CodeBlock2Expr");
			sbHandles.appendLine("                return super.{}(_hash_{}{});", m.method.getName(), normalCall.isEmpty() ? "" : ", ", normalCall);
		} else {
			sbHandles.appendLine("                super.{}(_hash_{}{});", m.method.getName(), normalCall.isEmpty() ? "" : ", ", normalCall);
			sbHandles.appendLine("                return null;");
		}
		if (m.resultTypeName != null && !m.resultFields.isEmpty()) {
			sbHandles.appendLine("            }, _result_ -> {");
			sbHandles.appendLine("                if (_result_ == null)");
			sbHandles.appendLine("                    return Zeze.Net.Binary.Empty;");
			sbHandles.appendLine("                var _r_ = ({})_result_;", m.resultTypeName);
			sbHandles.appendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
			Gen.instance.genEncode(sbHandles, "                ", "_b_", "_m_", "_r_.", m.resultFields, null);
			sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
			sbHandles.appendLine("            }, {}));", version);
		} else
			sbHandles.appendLine("            }, null, {}));", version);
	}
}
