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
import java.util.List;
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
public final class GenModule {
	private static final String REDIRECT_PREFIX = "Redirect_";
	public static final GenModule Instance = new GenModule();

	/**
	 * 源代码跟目录。
	 * 指定的时候，生成到文件，总是覆盖。
	 * 没有指定的时候，先查看目标类是否存在，存在则直接class.forName装载，否则生成到内存并动态编译。
	 */
	public String GenFileSrcRoot = System.getProperty("GenFileSrcRoot"); // 支持通过给JVM传递-DGenFileSrcRoot=xxx参数指定
	private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
	private final HashMap<String, Class<?>> GenClassMap = new HashMap<>();

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
		if (ctor.getParameterCount() != 1)
			throw new NoSuchMethodException("No suitable constructor for redirect module: " + cls.getName());
		return ctor.newInstance(app);
	}

	private static String getRedirectClassName(Class<? extends IModule> moduleClass) {
		String className = moduleClass.getName();
		return className.startsWith(REDIRECT_PREFIX) ? className : REDIRECT_PREFIX + className.replace('.', '_');
	}

	public static <T extends IModule> T createRedirectModule(Class<T> moduleClass, AppBase app) {
		try {
			return newModule(Class.forName(GenModule.getRedirectClassName(moduleClass)), app);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized <T extends IModule> T ReplaceModuleInstance(AppBase userApp, T module) {
		if (module.getClass().getName().startsWith(REDIRECT_PREFIX)) // 预防二次replace
			return module;

		var overrides = new ArrayList<MethodOverride>();
		for (var method : module.getClass().getDeclaredMethods()) {
			for (var anno : method.getAnnotations()) {
				var type = anno.annotationType();
				if (type == RedirectToServer.class || type == RedirectHash.class || type == RedirectAll.class) {
					overrides.add(new MethodOverride(method, anno));
					break;
				}
			}
		}
		if (overrides.isEmpty())
			return module; // 没有需要重定向的方法。
		overrides.sort(Comparator.comparing(o -> o.method.getName())); // 按方法名排序，避免每次生成结果发生变化。

		String genClassName = getRedirectClassName(module.getClass());
		try {
			if (GenFileSrcRoot == null) { // 不需要生成到文件的时候，尝试装载已经存在的生成模块子类。
				var genClass = GenClassMap.get(genClassName);
				if (genClass == null) {
					try {
						genClass = Class.forName(genClassName);
						GenClassMap.put(genClassName, genClass);
					} catch (ClassNotFoundException ignored) {
					}
				}
				if (genClass != null) {
					module.UnRegister();
					return newModule(genClass, userApp);
				}
			}

			var code = GenModuleCode(genClassName, module, overrides, userApp);

			if (GenFileSrcRoot != null) {
				byte[] oldBytes = null;
				byte[] newBytes = code.getBytes(StandardCharsets.UTF_8);
				var file = new File(GenFileSrcRoot, genClassName + ".java");
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
						e.printStackTrace();
					}
				}
				return module; // 生成带File需要再次编译，所以这里返回原来的module。
			}
			module.UnRegister();
			var genClass = compiler.compile(genClassName, code);
			GenClassMap.put(genClassName, genClass);
			return newModule(genClass, userApp);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static String GenModuleCode(String genClassName, IModule module, List<MethodOverride> overrides, AppBase userApp) throws Throwable {
		var sb = new StringBuilderCs();
		sb.AppendLine("// auto-generated @" + "formatter:off");
		sb.AppendLine("public final class {} extends {} {", genClassName, module.getClass().getName());
		sb.AppendLine("    private final Zeze.Arch.RedirectBase _redirect_;");
		sb.AppendLine();

		var sbHandles = new StringBuilderCs();
		for (var m : overrides) {
			var parametersDefine = m.GetDefineString();
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
				throw new RuntimeException("Redirect return type Must Be void or RedirectFuture or RedirectAllFuture: "
						+ module.getClass().getName() + '.' + m.method.getName());
			}
			String modifier;
			int flags = m.method.getModifiers();
			if ((flags & Modifier.PUBLIC) != 0)
				modifier = "public ";
			else if ((flags & Modifier.PROTECTED) != 0)
				modifier = "protected ";
			else {
				throw new RuntimeException("Redirect method Must Be public or protected: "
						+ module.getClass().getName() + '.' + m.method.getName());
			}

			sb.AppendLine("    @Override");
			sb.AppendLine("    {}{} {}({}) {", modifier, returnName, m.method.getName(), parametersDefine); // m.getThrows() // 继承方法允许不标throws

			ChoiceTargetRunLoopback(sb, m, returnName);

			if (m.annotation instanceof RedirectAll) {
				GenRedirectAll(sb, sbHandles, module, m);
				continue;
			}

			sb.AppendLine("        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();");
			sb.AppendLine("        var _a_ = _p_.Argument;");
			sb.AppendLine("        _a_.setModuleId({});", module.getId());
			sb.AppendLine("        _a_.setRedirectType({});", m.getRedirectType());
			sb.AppendLine("        _a_.setHashCode({});", m.hashOrServerIdParameter.getName());
			sb.AppendLine("        _a_.setMethodFullName(\"{}:{}\");", module.getFullName(), m.method.getName());
			sb.AppendLine("        _a_.setServiceNamePrefix(_redirect_.ProviderApp.ServerServiceNamePrefix);");
			if (m.inputParameters.size() > 0) {
				sb.AppendLine("        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
				Gen.Instance.GenEncode(sb, "        ", "_b_", m.inputParameters);
				sb.AppendLine("        _a_.setParams(new Zeze.Net.Binary(_b_));");
			}
			sb.AppendLine();
			if (returnName.equals("void"))
				sb.AppendLine("        _p_.Send(_t_, null);");
			else {
				sb.AppendLine("        var _f_ = new Zeze.Arch.RedirectFuture<{}>();", m.resultTypeName);
				sb.AppendLine("        _p_.Send(_t_, _rpc_ -> {");
				if (m.resultType == Long.class)
					sb.AppendLine("            _f_.SetResult(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());");
				else {
					if (!m.returnTypeHasResultCode) {
						sb.AppendLine("            if (_rpc_.isTimeout()) {");
						sb.AppendLine("                _f_.SetResult(null);");
						sb.AppendLine("                return Zeze.Transaction.Procedure.Success;");
						sb.AppendLine("            }");
					}
					sb.AppendLine("            var _r_ = new {}();", m.resultTypeName);
					if (Serializable.class.isAssignableFrom(m.resultType)) {
						sb.AppendLine("            var _param_ = _rpc_.Result.getParams();");
						sb.AppendLine("            if (_param_.size() > 0)");
						sb.AppendLine("                _r_.Decode(_param_.Wrap());");
						if (m.returnTypeHasResultCode)
							sb.AppendLine("            _r_.setResultCode(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());");
					} else {
						if (!m.resultTypeNames.isEmpty()) {
							sb.AppendLine("            var _param_ = _rpc_.Result.getParams();");
							sb.AppendLine("            if (_param_.size() > 0) {");
							sb.AppendLine("                var _bb_ = _param_.Wrap();");
							for (var typeName : m.resultTypeNames)
								Gen.Instance.GenDecode(sb, "                ", "_bb_", typeName.getKey(), "_r_." + typeName.getValue());
							sb.AppendLine("            }");
						}
						if (m.returnTypeHasResultCode)
							sb.AppendLine("            _r_.resultCode = _rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode();");
					}
					sb.AppendLine("            _f_.SetResult(_r_);");
				}
				sb.AppendLine("            return Zeze.Transaction.Procedure.Success;");
				sb.AppendLine("        });");
				sb.AppendLine("        return _f_;");
			}
			sb.AppendLine("    }");
			sb.AppendLine();

			// Handles
			sbHandles.AppendLine("        _app_.getZeze().Redirect.Handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", module.getFullName(), m.method.getName());
			sbHandles.AppendLine("            Zeze.Transaction.TransactionLevel.{}, (_hash_, _params_) -> {", m.transactionLevel);
			boolean genLocal = false;
			for (int i = 0; i < m.inputParameters.size(); ++i) {
				var p = m.inputParameters.get(i);
				Gen.Instance.GenLocalVariable(sbHandles, "                ", p.getType(), p.getName());
				genLocal = true;
			}
			if (genLocal)
				sbHandles.AppendLine("                var _b_ = _params_.Wrap();");
			Gen.Instance.GenDecode(sbHandles, "                ", "_b_", m.inputParameters);
			var normalCall = m.GetNormalCallString();
			var sep = normalCall.isEmpty() ? "" : ", ";
			if (returnName.equals("void")) {
				sbHandles.AppendLine("                super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
				sbHandles.AppendLine("                return null;");
			} else {
				if (normalCall.isEmpty())
					sbHandles.AppendLine("                //noinspection CodeBlock2Expr");
				sbHandles.AppendLine("                return super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
			}
			sbHandles.Append("            }, _result_ -> ");
			if (m.resultType != null && Serializable.class.isAssignableFrom(m.resultType)) {
				sbHandles.AppendLine("{");
				sbHandles.AppendLine("                var _r_ = ({})_result_;", m.resultTypeName);
				sbHandles.AppendLine("                int _s_ = _r_.getPreAllocSize();");
				sbHandles.AppendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate(Math.min(_s_, 65536));");
				sbHandles.AppendLine("                _r_.Encode(_b_);");
				sbHandles.AppendLine("                int _t_ = _b_.WriteIndex;");
				sbHandles.AppendLine("                if (_t_ > _s_)");
				sbHandles.AppendLine("                    _r_.setPreAllocSize(_t_);");
				sbHandles.AppendLine("                return new Zeze.Net.Binary(_b_);");
				sbHandles.Append("            }");
			} else if (!m.resultTypeNames.isEmpty()) {
				sbHandles.AppendLine("{");
				sbHandles.AppendLine("                var _r_ = ({})_result_;", m.resultTypeName);
				sbHandles.AppendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
				for (var typeName : m.resultTypeNames)
					Gen.Instance.GenEncode(sbHandles, "                ", "_b_", typeName.getKey(), "_r_." + typeName.getValue());
				sbHandles.AppendLine("                return new Zeze.Net.Binary(_b_);");
				sbHandles.Append("            }");
			} else
				sbHandles.Append("Zeze.Net.Binary.Empty");
			sbHandles.AppendLine("));");
		}

		var ctor = getCtor(module.getClass(), userApp);
		if (ctor.getParameterCount() == 1) {
			sb.AppendLine("    public {}({} _app_) {", genClassName, ctor.getParameters()[0].getType().getName().replace('$', '.'));
			sb.AppendLine("        super(_app_);");
		} else
			sb.AppendLine("    public {}(Zeze.AppBase _app_) {", genClassName);
		sb.AppendLine("        _redirect_ = _app_.getZeze().Redirect;");
		sb.AppendLine();
		sb.Append(sbHandles.toString());
		sb.AppendLine("    }");
		sb.AppendLine("}");
		return sb.toString();
	}

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	private static void ChoiceTargetRunLoopback(StringBuilderCs sb, MethodOverride m, String returnName) {
		if (m.annotation instanceof RedirectHash)
			sb.AppendLine("        var _t_ = _redirect_.ChoiceHash(this, {}, {});",
					m.hashOrServerIdParameter.getName(), m.getConcurrentLevelSource());
		else if (m.annotation instanceof RedirectToServer)
			sb.AppendLine("        var _t_ = _redirect_.ChoiceServer(this, {});", m.hashOrServerIdParameter.getName());
		else if (m.annotation instanceof RedirectAll)
			return; // RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。

		sb.AppendLine("        if (_t_ == null) { // local: loop-back");
		if (returnName.equals("void")) {
			sb.AppendLine("            _redirect_.RunVoid(Zeze.Transaction.TransactionLevel.{},", m.transactionLevel);
			sb.AppendLine("                () -> super.{}({}));", m.method.getName(), m.GetBaseCallString());
			sb.AppendLine("            return;");
		} else {
			sb.AppendLine("            return _redirect_.RunFuture(Zeze.Transaction.TransactionLevel.{},", m.transactionLevel);
			sb.AppendLine("                () -> super.{}({}));", m.method.getName(), m.GetBaseCallString());
		}
		sb.AppendLine("        }");
		sb.AppendLine();
	}

	private static void GenRedirectAll(StringBuilderCs sb, StringBuilderCs sbHandles,
									   IModule module, MethodOverride m) throws Throwable {
		sb.Append("        var _c_ = new Zeze.Arch.RedirectAllContext<>({}, ", m.hashOrServerIdParameter.getName());
		if (m.resultTypeName != null) {
			sb.AppendLine("_params_ -> {");
			sb.AppendLine("            var _r_ = new {}();", m.resultTypeName);
			sb.AppendLine("            if (_params_ != null) {");
			sb.AppendLine("                var _b_ = _params_.Wrap();");
			for (var typeName : m.resultTypeNames)
				Gen.Instance.GenDecode(sb, "                ", "_b_", typeName.getKey(), "_r_." + typeName.getValue());
			sb.AppendLine("            }");
			sb.AppendLine("            return _r_;");
			sb.AppendLine("        });");
		} else
			sb.AppendLine("null);");
		sb.AppendLine("        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();");
		sb.AppendLine("        var _a_ = _p_.Argument;");
		sb.AppendLine("        _a_.setModuleId({});", module.getId());
		sb.AppendLine("        _a_.setHashCodeConcurrentLevel({});", m.hashOrServerIdParameter.getName());
		sb.AppendLine("        _a_.setMethodFullName(\"{}:{}\");", module.getFullName(), m.method.getName());
		sb.AppendLine("        _a_.setServiceNamePrefix(_redirect_.ProviderApp.ServerServiceNamePrefix);");
		sb.AppendLine("        _a_.setSessionId(_redirect_.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(_c_));");
		if (m.inputParameters.size() > 0) {
			sb.AppendLine("        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
			Gen.Instance.GenEncode(sb, "        ", "_b_", m.inputParameters);
			sb.AppendLine("        _a_.setParams(new Zeze.Net.Binary(_b_));");
		}
		if (m.resultType != null)
			sb.AppendLine("        return _redirect_.RedirectAll(this, _p_, _c_);");
		else
			sb.AppendLine("        _redirect_.RedirectAll(this, _p_, _c_);");
		sb.AppendLine("    }");
		sb.AppendLine();

		// handles
		sbHandles.AppendLine("        _app_.getZeze().Redirect.Handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", module.getFullName(), m.method.getName());
		sbHandles.AppendLine("            Zeze.Transaction.TransactionLevel.{}, (_hash_, _params_) -> {", m.transactionLevel);
		if (!m.inputParameters.isEmpty()) {
			sbHandles.AppendLine("                var _b_ = _params_.Wrap();");
			for (int i = 0; i < m.inputParameters.size(); ++i) {
				var p = m.inputParameters.get(i);
				Gen.Instance.GenLocalVariable(sbHandles, "                ", p.getType(), p.getName());
			}
			Gen.Instance.GenDecode(sbHandles, "                ", "_b_", m.inputParameters);
		}

		var normalCall = m.GetNormalCallString();
		if (m.resultType != null)
			sbHandles.AppendLine("                return super.{}(_hash_{}{});", m.method.getName(), normalCall.isEmpty() ? "" : ", ", normalCall);
		else {
			sbHandles.AppendLine("                super.{}(_hash_{}{});", m.method.getName(), normalCall.isEmpty() ? "" : ", ", normalCall);
			sbHandles.AppendLine("                return null;");
		}
		sbHandles.Append("            }, _result_ -> ");
		if (m.resultTypeName != null && !m.resultTypeNames.isEmpty()) {
			sbHandles.AppendLine("{");
			sbHandles.AppendLine("                var _r_ = ({})_result_;", m.resultTypeName);
			sbHandles.AppendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
			for (var typeName : m.resultTypeNames)
				Gen.Instance.GenEncode(sbHandles, "                ", "_b_", typeName.getKey(), "_r_." + typeName.getValue());
			sbHandles.AppendLine("                return new Zeze.Net.Binary(_b_);");
			sbHandles.Append("            }");
		} else
			sbHandles.Append("Zeze.Net.Binary.Empty");
		sbHandles.AppendLine("));");
	}
}
