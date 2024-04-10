package Zeze.Arch.Gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.AppBase;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectToServer;
import Zeze.IModule;
import Zeze.Serialize.Serializable;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.StringBuilderCs;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	/**
	 * 源代码跟目录。
	 * 指定的时候，生成到文件，总是覆盖。
	 * 没有指定的时候，先查看目标类是否存在，存在则直接class.forName装载，否则生成到内存并动态编译。
	 */
	public @Nullable String genFileSrcRoot = System.getProperty("GenFileSrcRoot"); // 支持通过给JVM传递-DGenFileSrcRoot=xxx参数指定
	private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
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

	public static <T extends IModule> @NotNull T createRedirectModule(@NotNull Class<T> moduleClass, @NotNull AppBase app) {
		try {
			return newModule(Class.forName(GenModule.getRedirectClassName(moduleClass)), app);
		} catch (ReflectiveOperationException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public IModule[] createRedirectModules(@NotNull AppBase userApp, Class<?> @NotNull [] moduleClasses) {
		lock();
		try {
			int i = 0, n = moduleClasses.length;
			try {
				var classNames = new String[n];
				var classNameAndCodes = new HashMap<String, String>(); // <className, code>
				for (; i < n; i++) {
					var moduleClass = moduleClasses[i];
					if (moduleClass.getName().startsWith(REDIRECT_PREFIX)) // 预防二次replace
						continue;

					var overrides = new ArrayList<MethodOverride>();
					for (var method : moduleClass.getDeclaredMethods()) {
						for (var anno : method.getAnnotations()) {
							var type = anno.annotationType();
							if (type == RedirectToServer.class || type == RedirectHash.class || type == RedirectAll.class) {
								overrides.add(new MethodOverride(method, anno));
								break;
							}
						}
					}
					if (overrides.isEmpty())
						continue; // 没有需要重定向的方法。
					overrides.sort(Comparator.comparing(o -> o.method.getName())); // 按方法名排序，避免每次生成结果发生变化。

					String genClassName = getRedirectClassName(moduleClass);
					if (genFileSrcRoot == null) { // 不需要生成到文件的时候，尝试装载已经存在的生成模块子类。
						var genClass = genClassMap.get(genClassName);
						if (genClass == null) {
							try {
								genClass = Class.forName(genClassName);
								genClassMap.put(genClassName, genClass);
							} catch (ClassNotFoundException ignored) {
							}
						}
						if (genClass != null) {
							classNames[i] = genClassName;
							continue;
						}
					}

					var code = genModuleCode(genClassName, moduleClass, overrides, userApp);

					if (genFileSrcRoot != null) {
						byte[] oldBytes = null;
						byte[] newBytes = code.getBytes(StandardCharsets.UTF_8);
						var file = new File(genFileSrcRoot, genClassName + ".java");
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
						if (oldBytes == null) {
							try (var fos = new FileOutputStream(file)) {
								fos.write(newBytes);
							} catch (IOException e) {
								//noinspection CallToPrintStackTrace
								e.printStackTrace();
							}
						}
					}
					classNames[i] = genClassName;
					classNameAndCodes.put(genClassName, code);
				}
				if (genFileSrcRoot != null) // 仅生成代码时无需编译和创建模块实例
					return null;

				var modules = new IModule[n];
				if (!classNameAndCodes.isEmpty())
					compiler.compileAll(classNameAndCodes, genClassMap);
				for (i = 0; i < n; i++) {
					var className = classNames[i];
					modules[i] = newModule(className != null ? genClassMap.get(className) : moduleClasses[i], userApp);
				}
				return modules;
			} catch (Exception e) {
				if (i < n)
					throw new IllegalStateException("module class: " + moduleClasses[i].getName(), e);
				Task.forceThrow(e);
				return null; // never run here
			}
		} finally {
			unlock();
		}
	}

	private static String genModuleCode(@NotNull String genClassName, @NotNull Class<?> moduleClass,
										@NotNull List<MethodOverride> overrides, @NotNull AppBase userApp) throws Exception {
		var sb = new StringBuilderCs();
		sb.appendLine("// auto-generated @" + "formatter:off");
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
				else if (type == RedirectFuture.class)
					returnName = "Zeze.Arch.RedirectFuture<" + m.resultTypeName + '>';
				else if (type == RedirectAllFuture.class)
					returnName = "Zeze.Arch.RedirectAllFuture<" + m.resultTypeName + '>';
				else {
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
				if (m.oneByone)
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
					Gen.instance.genEncode(sb, prefix, "_b_", m.inputParameters);
					sb.appendLine("{}_a_.setParams(new Zeze.Net.Binary(_b_));", prefix);
				}
				sb.appendLine();
				if (returnName.equals("void"))
					sb.appendLine("{}_p_.Send(_t_, null);", prefix);
				else {
					sb.appendLine("{}if (!_p_.Send(_t_, _rpc_ -> {", prefix);
					sb.appendLine("{}    if (_rpc_.isTimeout()) {", prefix);
					sb.appendLine("{}        _f_.setException(Zeze.Arch.RedirectException.timeoutInstance);", prefix);
					sb.appendLine("{}        return Zeze.Transaction.Procedure.Success;", prefix);
					sb.appendLine("{}    }", prefix);
					if (m.resultType == Long.class)
						sb.appendLine("{}    _f_.setResult(_rpc_.getResultCode());", prefix);
					else {
						if ("String".equals(m.resultTypeName))
							sb.appendLine("{}    var _r_ = Zeze.Util.Str.fromBinary(_rpc_.Result.getParams());", prefix);
						else if ("Zeze.Net.Binary".equals(m.resultTypeName))
							sb.appendLine("{}    var _r_ = _rpc_.Result.getParams();", prefix);
						else
							sb.appendLine("{}    var _r_ = new {}();", prefix, m.resultTypeName);
						if (Serializable.class.isAssignableFrom(m.resultClass)) {
							sb.appendLine("{}    var _param_ = _rpc_.Result.getParams();", prefix);
							sb.appendLine("{}    if (_param_.size() > 0)", prefix);
							sb.appendLine("{}        _r_.decode(_param_.Wrap());", prefix);
							if (m.returnTypeHasResultCode)
								sb.appendLine("{}    _r_.setResultCode(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());", prefix);
						} else {
							if (!m.resultFields.isEmpty()) {
								sb.appendLine("{}    var _param_ = _rpc_.Result.getParams();", prefix);
								sb.appendLine("{}    if (_param_.size() > 0) {", prefix);
								sb.appendLine("{}        var _bb_ = _param_.Wrap();", prefix);
								for (var field : m.resultFields)
									Gen.instance.genDecode(sb, prefix + "        ", "_bb_", field.getType(), field.getGenericType(), "_r_." + field.getName());
								sb.appendLine("{}    }", prefix);
							}
							if (m.returnTypeHasResultCode)
								sb.appendLine("{}    _r_.resultCode = _rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode();", prefix);
						}
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
					sb.appendLine("{}    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, e.getMessage(), e));", prefix);
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
				Gen.instance.genDecode(sbHandles, "                ", "_b_", m.inputParameters);
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
					for (var field : m.resultFields)
						Gen.instance.genEncode(sbHandles, "                ", "_b_", field.getType(), field.getGenericType(), "_r_." + field.getName());
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

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	private static void choiceTargetRunLoopback(StringBuilderCs sb, MethodOverride m, String returnName, String prefix) {
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
			sb.appendLine("{}        () -> super.{}({}));", prefix, m.method.getName(), m.getBaseCallString());
			sb.appendLine("{}    return;", prefix);
		} else {
			sb.appendLine("{}    return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.{},", prefix, m.transactionLevel);
			sb.appendLine("{}        () -> super.{}({}));", prefix, m.method.getName(), m.getBaseCallString());
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
				sb.appendLine("            if (_params_ != null) {");
				sb.appendLine("                var _b_ = _params_.Wrap();");
				for (var field : m.resultFields)
					Gen.instance.genDecode(sb, "                ", "_b_", field.getType(), field.getGenericType(), "_r_." + field.getName());
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
			Gen.instance.genEncode(sb, "        ", "_b_", m.inputParameters);
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
			Gen.instance.genDecode(sbHandles, "                ", "_b_", m.inputParameters);
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
			for (var field : m.resultFields)
				Gen.instance.genEncode(sbHandles, "                ", "_b_", field.getType(), field.getGenericType(), "_r_." + field.getName());
			sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
			sbHandles.appendLine("            }, {}));", version);
		} else
			sbHandles.appendLine("            }, null, {}));", version);
	}
}
