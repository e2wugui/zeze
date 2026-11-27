package Zeze.Arch.Gen;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Util.AtomicOutputFile;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.StringBuilderCs;
import Zeze.Util.Task;

/**
 * Redirect 子类生成器：扫描模块中标注 {@link RedirectToServer}/{@link RedirectHash}/{@link RedirectAll}
 * 的非 private/final/static 方法，生成拦截子类源码并在构建期写盘或运行时编译装载。
 */
public final class GenModule extends ReentrantLock {
	public static final String REDIRECT_PREFIX = "Redirect_";
	public static final GenModule instance = new GenModule();
	private static final Pattern genericPat = Pattern.compile("<.+>");

	private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
	private final HashMap<String, Class<?>> genClassMap = new HashMap<>();

	public InMemoryJavaCompiler getCompiler() {
		return compiler;
	}

	private GenModule() {
		compiler.ignoreWarnings();
	}

	public static <T extends IModule> Constructor<T> getCtor(Class<?> cls, AppBase app) throws ReflectiveOperationException {
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

	public static <T extends IModule> T newModule(Class<?> cls, AppBase app) throws ReflectiveOperationException {
		@SuppressWarnings("unchecked")
		var ctor = (Constructor<T>)getCtor(cls, app);
		return ctor.getParameterCount() == 1 ? ctor.newInstance(app) : ctor.newInstance();
	}

	private static String getRedirectClassName(Class<?> moduleClass) {
		String className = moduleClass.getName();
		return className.startsWith(REDIRECT_PREFIX) ? className : REDIRECT_PREFIX + className.replace('.', '_');
	}

	public static <T extends IModule> T createRedirectModule(Class<T> moduleClass, AppBase app) {
		try {
			Class<?> genClass = Class.forName(getRedirectClassName(moduleClass));
			return newModule(genClass, app);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	// 沿父类链向上收集带 redirect 注解的方法（至 IModule 为止），同签名以派生类优先。
	private static ArrayList<MethodOverride> collectOverrides(Class<?> moduleClass) {
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

	private static String methodKey(Method method) {
		var sb = new StringBuilder(method.getName());
		for (var paramType : method.getParameterTypes())
			sb.append(':').append(paramType.getName());
		return sb.toString();
	}

	/**
	 * 生成模式（构建期）：把带redirect注解模块的Redirect_子类源码写到srcRoot（总是覆盖）。
	 * 只产出源码，不装载不实例化；退出与否由调用方（工具main/应用Start）决定。
	 *
	 * @param tryCompile 写盘前逐模块内存javac试编译，不可编译产物不落盘（默认false）
	 */
	public void generateRedirectSources(String srcRoot, AppBase userApp,
										Class<?>[] moduleClasses, boolean tryCompile) {
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
					// 写盘前试编译，失败即中止不落盘；只取字节码，不在装载器define。
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
						AtomicOutputFile.writeAtomically(file.toPath(), newBytes);

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

	private record PendingCompile(String genClassName, String code) {
	}

	/**
	 * 运行时装载 Redirect_ 子类并实例化模块（构建期源码生成走 {@link #generateRedirectSources}）。
	 * 每个模块按序尝试：从进程缓存/classpath 装载已存在的子类，未命中的生成源码并批量编译后 define。
	 * 无 redirect 方法的模块直接用原始模块类实例化。
	 */
	public IModule[] createRedirectModules(AppBase userApp,
										   Class<?>[] moduleClasses) {
		lock();
		try {
			int n = moduleClasses.length;
			var genClasses = new Class<?>[n];
			var pending = new @Nullable PendingCompile[n];
			for (var i = 0; i < n; i++) {
				var moduleClass = moduleClasses[i];
				try {
					if (moduleClass.getName().startsWith(REDIRECT_PREFIX)) // 预防二次replace
						continue;
					var overrides = collectOverrides(moduleClass);
					if (overrides.isEmpty())
						continue; // 没有需要重定向的方法。

					var genClassName = getRedirectClassName(moduleClass);
					Class<?> genClass = genClassMap.get(genClassName);
					if (genClass == null) {
						try {
							genClass = Class.forName(genClassName);
							genClassMap.put(genClassName, genClass);
						} catch (ClassNotFoundException ignored) {
						}
					}
					if (genClass != null) {
						checkRedirectSuperclass(genClass, moduleClass, "cache");
						genClasses[i] = genClass;
						continue;
					}
					String code = genModuleCode(genClassName, moduleClass, overrides, userApp);
					pending[i] = new PendingCompile(genClassName, code);
				} catch (Exception e) {
					throw new IllegalStateException("module class: " + moduleClass.getName(), e);
				}
			}

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

	private void compilePending(@Nullable PendingCompile[] pending, Class<?>[] genClasses) throws ClassNotFoundException {
		var codes = new HashMap<String, String>();
		for (var p : pending)
			if (p != null)
				codes.put(p.genClassName, p.code);
		if (codes.isEmpty())
			return;
		var compiled = compiler.compileAll(codes);
		for (var i = 0; i < pending.length; i++) {
			var p = pending[i];
			if (p == null)
				continue;
			var cls = Objects.requireNonNull(compiled.get(p.genClassName));
			genClassMap.put(p.genClassName, cls);
			genClasses[i] = cls;
		}
	}

	private static void checkRedirectSuperclass(Class<?> genClass, Class<?> moduleClass,
												String source) {
		if (genClass.getSuperclass() != moduleClass)
			throw new IllegalStateException("redirect class mismatch (" + source + "): " + genClass.getName()
				+ ", superclass=" + genClass.getSuperclass().getName()
				+ ", expect=" + moduleClass.getName());
	}

	private static String genModuleCode(String genClassName, Class<?> moduleClass,
										List<MethodOverride> overrides, AppBase userApp) throws Exception {
		checkBeanFactorySymbol(moduleClass, overrides);
		var sb = new StringBuilderCs();
		sb.appendLine("// auto-generated @" + "formatter:off");
		sb.appendLine();
		sb.appendLine("import org.jspecify.annotations.NullMarked;");
		if (overrides.stream().anyMatch(m -> m.getDefineString().contains("@Nullable")
			|| Gen.instance.getTypeName(m.method.getAnnotatedReturnType()).contains("@Nullable")))
			sb.appendLine("import org.jspecify.annotations.Nullable;");
		sb.appendLine();
		sb.appendLine("@NullMarked");
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
					if (m.resultTypeName == null)
						throw new IllegalStateException("RedirectFuture<> missing result type: "
							+ moduleClass.getName() + '.' + m.method.getName());
					returnName = Gen.instance.getTypeName(m.method.getAnnotatedReturnType());
				} else if (type == RedirectAllFuture.class) {
					if (m.resultTypeName == null)
						throw new IllegalStateException("RedirectAllFuture<> missing result type: "
							+ moduleClass.getName() + '.' + m.method.getName());
					returnName = Gen.instance.getTypeName(m.method.getAnnotatedReturnType());
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
				sb.appendLine("    {}{} {}({}) {", modifier, returnName, m.method.getName(), parametersDefine); // 继承方法允许不标throws
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
				sb.appendLine("{}_a_.setServiceNamePrefix(java.util.Objects.requireNonNull(_redirect_.providerApp.serverServiceNamePrefix));", prefix);
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
					sb.appendLine("{}_redirect_.sendVoid(_t_, _p_, \"{}:{}\");", prefix, moduleFullName, m.method.getName());
				else {
					sb.appendLine("{}if (!_p_.Send(_t_, _rpc_ -> {", prefix);
					sb.appendLine("{}    try {", prefix);
					prefix += "    ";
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
						if (BeanKey.class.isAssignableFrom(Objects.requireNonNull(m.resultClass))) {
							sb.appendLine("{}        _r_ = {}.readFrom(_rpc_.Result.getParams().Wrap());",
									prefix, Gen.instance.getTypeName(m.resultClass));
						} else if (Serializable.class.isAssignableFrom(m.resultClass)) {
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
					prefix = prefix.substring(0, prefix.length() - 4);
					sb.appendLine("{}    } catch (Throwable _decodeError_) {", prefix);
					sb.appendLine("{}        if (_f_.isDone())", prefix);
					sb.appendLine("{}            throw Zeze.Util.Task.forceThrow(_decodeError_);", prefix);
					sb.appendLine("{}        _f_.setException(_decodeError_);", prefix);
					sb.appendLine("{}    }", prefix);
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
					sb.appendLine("{}} catch (Zeze.Arch.RedirectException e) {", prefix);
					sb.appendLine("{}    _f_.setException(e);", prefix);
					sb.appendLine("{}} catch (Exception e) {", prefix);
					sb.appendLine("{}    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.LOCAL_EXECUTION, e.getMessage(), e));", prefix);
					sb.appendLine("{}}", prefix);
					sb.appendLine("{}return _f_;", prefix);
				}
				sb.appendLine("    }");
				sb.appendLine();

				// Handles
				sbHandles.appendLine("        java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(_app_.getZeze()).redirect).handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", moduleFullName, m.method.getName());
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
				if (m.resultType != null && Gen.isZezeValue(Objects.requireNonNull(m.resultClass))) {
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
		sb.appendLine("        _redirect_ = java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(_app_.getZeze()).redirect);");
		sb.appendLine();
		sb.append(sbHandles.toString());
		sb.appendLine("    }");
		sb.appendLine("}");
		return sb.toString();
	}

	// 校验模块父类链是否存在可访问静态 beanFactory，供 Bean/Data 多态 decode 使用。
	private static void checkBeanFactorySymbol
	(Class<?> moduleClass, List<MethodOverride> overrides) {
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
				return;
			}
		}
		throw new UnsupportedOperationException("redirect Bean/Data param unsupported: module "
			+ moduleClass.getName() + " (methods " + methodsNeedingFactory + ") has no accessible static "
			+ "beanFactory in its superclass chain; declare: protected static final "
			+ "Zeze.Collections.BeanFactory beanFactory = new Zeze.Collections.BeanFactory();");
	}

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	private static void choiceTargetRunLoopback(StringBuilderCs sb, MethodOverride m, String returnName, String prefix) {
		switch (m.annotation) {
		case RedirectHash redirectHash -> sb.appendLine("{}var _t_ = _redirect_.choiceHash(this, {}, {});",
			prefix, m.hashOrServerIdParameter.getName(), m.getConcurrentLevelSource());
		case RedirectToServer redirectToServer -> sb.appendLine("{}var _t_ = _redirect_.choiceServer(this, {}, {});",
			prefix, m.hashOrServerIdParameter.getName(), redirectToServer.orOtherServer());
		case RedirectAll redirectAll -> {
			return; // RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。
		}
		default -> {
		}
		}

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
				// encoder 将 null 结果编码为 Binary.Empty，需判非空再 decode；同时返回非 null 实例供 processResult 设置 hash。
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
		sb.appendLine("        _a_.setServiceNamePrefix(java.util.Objects.requireNonNull(_redirect_.providerApp.serverServiceNamePrefix));");
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
		sbHandles.appendLine("        java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(_app_.getZeze()).redirect).handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", moduleFullName, m.method.getName());
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
