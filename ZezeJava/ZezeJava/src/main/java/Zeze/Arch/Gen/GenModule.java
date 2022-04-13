package Zeze.Arch.Gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllDoneHandle;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectToServer;
import Zeze.Util.StringBuilderCs;
import org.mdkt.compiler.InMemoryJavaCompiler;

/**
 * 把模块的方法调用发送到其他服务器实例上执行。
 * 被重定向的方法用注解标明。
 * 被重定向的方法需要是virtual的。
 * 实现方案：
 * Game.App创建Module的时候调用回调。
 * 在回调中判断是否存在需要拦截的方法。
 * 如果需要就动态生成子类实现代码并编译并返回新的实例。
 * <p>
 * 注意：
 * 使用 virtual override 的方式可以选择拦截部分方法。
 * 可以提供和原来模块一致的接口。
 */
public class GenModule {
	public static final GenModule Instance = new GenModule();

	/**
	 * 源代码跟目录。
	 * 指定的时候，生成到文件，总是覆盖。
	 * 没有指定的时候，先查看目标类是否存在，存在则直接class.forName装载，否则生成到内存并动态编译。
	 */
	public String GenFileSrcRoot;

	private void tryCollectMethod(ArrayList<MethodOverride> result, OverrideType type, Method method) {
		var tLevelAnn = method.getAnnotation(Zeze.Util.TransactionLevel.class);
		var tLevel = Zeze.Transaction.TransactionLevel.Serializable;
		if (null != tLevelAnn)
			tLevel = Zeze.Transaction.TransactionLevel.valueOf(tLevelAnn.Level());
		switch (type) {
		case RedirectAll:
			var annotation2 = method.getAnnotation(RedirectAll.class);
			if (annotation2 != null)
				result.add(new MethodOverride(tLevel, method, OverrideType.RedirectAll, annotation2));
			break;
		case RedirectHash:
			var annotation3 = method.getAnnotation(RedirectHash.class);
			if (annotation3 != null)
				result.add(new MethodOverride(tLevel, method, OverrideType.RedirectHash, annotation3));
			break;
		case RedirectToServer:
			var annotation4 = method.getAnnotation(RedirectToServer.class);
			if (annotation4 != null)
				result.add(new MethodOverride(tLevel, method, OverrideType.RedirectToServer, annotation4));
			break;
		}
	}

	public String getNamespace(String fullClassName) {
		var names = fullClassName.split("\\.");
		var ns = new StringBuilder();
		for (int i = 0; i < names.length; ++i) {
			if (i > 0)
				ns.append(".");
			ns.append(names[i]);
		}
		return ns.toString();
	}

	public File getNamespaceFilePath(String fullClassName) {
		var ns = fullClassName.split("\\.");
		return Paths.get(GenFileSrcRoot, ns).toFile();
	}

	public final <T extends Zeze.AppBase> Zeze.IModule ReplaceModuleInstance(T userApp, Zeze.IModule module) {
		var overrides = new ArrayList<MethodOverride>();
		var methods = module.getClass().getDeclaredMethods();
		for (var method : methods) {
			tryCollectMethod(overrides, OverrideType.RedirectHash, method);
			tryCollectMethod(overrides, OverrideType.RedirectAll, method);
			tryCollectMethod(overrides, OverrideType.RedirectToServer, method);
		}
		if (overrides.isEmpty()) {
			return module; // 没有需要重定向的方法。
		}

		// 按方法名排序，避免每次生成结果发生变化。
		overrides.sort(Comparator.comparing(o -> o.method.getName()));

		String genClassName = module.getFullName().replace('.', '_') + "_Redirect";
		String namespace = getNamespace(module.getFullName());
		try {
			if (GenFileSrcRoot == null) {
				// 不需要生成到文件的时候，尝试装载已经存在的生成模块子类。
				try {
					Class<?> moduleClass = Class.forName(namespace + "." + genClassName);
					module.UnRegister();
					var newModuleInstance = (Zeze.IModule)moduleClass.getDeclaredConstructor(userApp.getClass()).newInstance(userApp);
					newModuleInstance.Initialize(userApp);
					return newModuleInstance;
				} catch (Throwable ex) {
					// skip try load error
					// continue gen if error
				}
			}

			String code = GenModuleCode(
					// 生成文件的时候，生成package.
					(GenFileSrcRoot != null ? "package " + namespace + ";" : ""),
					module, genClassName, overrides, userApp.getClass().getName());

			if (GenFileSrcRoot != null) {
				byte[] oldBytes = null;
				byte[] newBytes = code.getBytes(StandardCharsets.UTF_8);
				var file = new File(getNamespaceFilePath(module.getFullName()), genClassName + ".java");
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
				// 生成带File需要再次编译，所以这里返回原来的module。
				return module;
			}
			Class<?> moduleClass = compiler.compile(genClassName, code);
			module.UnRegister();
			var newModuleInstance = (Zeze.IModule)moduleClass.getDeclaredConstructor(userApp.getClass()).newInstance(userApp);
			newModuleInstance.Initialize(userApp);
			return newModuleInstance;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private final InMemoryJavaCompiler compiler;

	private GenModule() {
		compiler = InMemoryJavaCompiler.newInstance();
		compiler.ignoreWarnings();
	}

	private ReturnTypeAndName GetReturnType(Class<?> type) {
		if (type == void.class)
			return new ReturnTypeAndName(ReturnType.Void, "void");
		if (type == Zeze.Util.TaskCompletionSource.class) {
			// java 怎么获得模板参数列表，检查一下模板参数类型必须Long.
			return new ReturnTypeAndName(ReturnType.TaskCompletionSource, "Zeze.Util.TaskCompletionSource<Long>");
		}
		throw new RuntimeException("ReturnType Must Be void Or TaskCompletionSource<Long>");
	}

	private void Verify(MethodOverride method) {
		//noinspection SwitchStatementWithTooFewBranches
		switch (method.overrideType) {
		case RedirectAll:
			if (method.method.getReturnType() != void.class)
				throw new RuntimeException("RedirectAll ReturnType Must Be void");
			if (method.ParameterRedirectAllDoneHandle != null && method.ResultHandle == null)
				throw new RuntimeException("RedirectAll Has RedirectAllDoneHandle But Miss ResultHandle.");
			break;
		}
	}

	private String GenModuleCode(String pkg, Zeze.IModule module, String genClassName, List<MethodOverride> overrides, String userAppName) throws Throwable {
		var sb = new Zeze.Util.StringBuilderCs();
		sb.AppendLine("// auto-generated @formatter:off");
		sb.AppendLine(pkg);
		sb.AppendLine();
		sb.AppendLine("import Zeze.Net.Binary;");
		sb.AppendLine();

		sb.AppendLine("public final class {} extends {}.Module{} {", genClassName, module.getFullName(), module.getName());

		// TaskCompletionSource<int> void
		var sbHandles = new Zeze.Util.StringBuilderCs();
		for (var m : overrides)  {
			m.PrepareParameters();

			var parametersDefine = m.GetDefineString();
			var methodNameHash = m.method.getName();
			var rtn = GetReturnType(m.method.getReturnType());
			Verify(m);
			if (null != m.ResultHandle)
				m.ResultHandle.Verify(m);

			sb.AppendLine("    @Override");
			sb.AppendLine("    public {} {}({}) {", rtn.ReturnTypeName, m.method.getName(), parametersDefine); // m.getThrows() // 继承方法允许不标throws

			ChoiceTargetRunLoopback(sb, m, rtn);

			if (m.overrideType == OverrideType.RedirectAll) {
				GenRedirectAll(sb, sbHandles, module, m);
				continue;
			}

			var rpcVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			var argVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine("        var {} = new Zeze.Beans.ProviderDirect.ModuleRedirect();", rpcVarName);
			sb.AppendLine("        var {} = {}.Argument;", argVarName, rpcVarName);
			sb.AppendLine("        {}.setModuleId({});", argVarName, module.getId());
			sb.AppendLine("        {}.setRedirectType({});", argVarName, m.getRedirectType());
			sb.AppendLine("        {}.setHashCode({});", argVarName, m.GetChoiceHashOrServerCodeSource());
			sb.AppendLine("        {}.setMethodFullName(\"{}:{}\");", argVarName, module.getFullName(), m.method.getName());
			sb.AppendLine("        {}.setServiceNamePrefix(App.ProviderApp.ServerServiceNamePrefix);", argVarName);
			if (m.ParametersNormal.size() > 0) {
				// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
				sb.AppendLine("        {");
				sb.AppendLine("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
				Gen.Instance.GenEncode(sb, "            ", "_bb_", m.ParametersNormal);
				sb.AppendLine("            {}.setParams(new Binary(_bb_));", argVarName);
				sb.AppendLine("        }");
			}
			sb.AppendLine();
			String futureVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine("        var {} = new Zeze.Util.TaskCompletionSource<Long>();", futureVarName);
			sb.AppendLine();
			sb.AppendLine("        {}.Send(_target_, _rpc_ -> {", rpcVarName);
			sb.AppendLine("            if (_rpc_.isTimeout())");
			sb.AppendLine("                {}.SetException(new Zeze.Net.RpcTimeoutException(\"{}:{} Rpc Timeout.\"));", futureVarName, module.getFullName(), m.method.getName());
			sb.AppendLine("            else if (_rpc_.getResultCode() != Zeze.Beans.ProviderDirect.ModuleRedirect.ResultCodeSuccess)");
			sb.AppendLine("                {}.SetException(new IllegalStateException(\"{}:{} Rpc Error=\" + _rpc_.getResultCode()));", futureVarName, module.getFullName(), m.method.getName());
			sb.AppendLine("            else {");
			if (null != m.ResultHandle) {
				// decode and run if has result
				String bb = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				sb.AppendLine("                var {} = _rpc_.Result.getParams().Wrap();", bb);
				m.ResultHandle.GenDecodeAndCallback("                ", sb, m, bb);
			}
			sb.AppendLine("                {}.SetResult(0L);", futureVarName);
			sb.AppendLine("            }");
			sb.AppendLine("            return Zeze.Transaction.Procedure.Success;");
			sb.AppendLine("        });");
			if (rtn.ReturnType == ReturnType.TaskCompletionSource)
			{
				sb.AppendLine("        return {};", futureVarName);
			}
			sb.AppendLine("    }");
			sb.AppendLine();

			// Handles
			sbHandles.AppendLine();
			sbHandles.AppendLine("        App.Zeze.Redirect.Handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", module.getFullName(), m.method.getName());
			sbHandles.AppendLine("            Zeze.Transaction.TransactionLevel.{}, (_sessionId_, _hash_, _params_) -> {", m.TransactionLevel);
			var rbbVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			boolean genLocal = false;
			for (int i = 0; i < m.ParametersNormal.size(); ++i)
			{
				var p = m.ParametersNormal.get(i);
				if (Gen.IsKnownDelegate(p.getType()))
					continue; // define later.
				Gen.Instance.GenLocalVariable(sbHandles, "                ", p.getType(), p.getName());
				genLocal = true;
			}
			if (genLocal)
				sbHandles.AppendLine("                var {} = _params_.Wrap();", rbbVarName);
			Gen.Instance.GenDecode(sbHandles, "                ", rbbVarName, m.ParametersNormal);

			if (null != m.ResultHandle) {
				var callResultParamName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				sbHandles.AppendLine("                var {} = new Zeze.Util.OutObject<>(Binary.Empty);", callResultParamName);
				var reqBBVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				var resultVarNames = new ArrayList<String>();
				for (int i = 0; i < m.ResultHandle.GenericArguments.length; ++i)
					resultVarNames.add("tmp" + Gen.Instance.TmpVarNameId.incrementAndGet());
				// action.GenActionEncode(sbHandles, "            ");
				String normalCall = m.GetNormalCallString(p -> p == m.ResultHandle.Parameter);
				String sep = normalCall.isEmpty() ? "" : ", ";
				sbHandles.AppendLine("                super.{}(_hash_{}{}, ({}) -> {", methodNameHash, sep, normalCall, m.ResultHandle.GetCallString(resultVarNames));
				sbHandles.AppendLine("                    var {} = Zeze.Serialize.ByteBuffer.Allocate();", reqBBVarName);
				m.ResultHandle.GenEncode(resultVarNames, "                    ", sbHandles, m, reqBBVarName);
				sbHandles.AppendLine("                    {}.Value = new Binary({});", callResultParamName, reqBBVarName);
				sbHandles.AppendLine("                });");
				sbHandles.AppendLine("                return {}.Value;", callResultParamName);
			} else {
				String normalCall = m.GetNormalCallString();
				String sep = normalCall.isEmpty() ? "" : ", ";
				sbHandles.AppendLine("                super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
				sbHandles.AppendLine("                return Binary.Empty;");
			}
			sbHandles.AppendLine("            }));");
		}
		sb.AppendLine("    @SuppressWarnings(\"deprecation\")");
		sb.AppendLine("    public {}({} app) {", genClassName, userAppName);
		sb.AppendLine("        super(app);");
		sb.Append(sbHandles.toString());
		sb.AppendLine("    }");
		sb.AppendLine("}");
		return sb.toString();
	}

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	void ChoiceTargetRunLoopback(StringBuilderCs sb, MethodOverride methodOverride, ReturnTypeAndName rtn) {
		switch (methodOverride.overrideType) {
		case RedirectHash:
			sb.AppendLine("        // RedirectHash");
			sb.AppendLine("        var _target_ = App.Zeze.Redirect.ChoiceHash(this, {});", methodOverride.ParameterHashOrServer.getName());
			break;
		case RedirectToServer:
			sb.AppendLine("        // RedirectToServer");
			sb.AppendLine("        var _target_ = App.Zeze.Redirect.ChoiceServer(this, {});", methodOverride.ParameterHashOrServer.getName());
			break;
		case RedirectAll:
			sb.AppendLine("        // RedirectAll");
			// RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。
			return;
		}

		sb.AppendLine("        if (_target_ == null) {");
		sb.AppendLine("            // local: loop-back");
		switch (rtn.ReturnType)
		{
		case Void:
			sb.AppendLine("            App.Zeze.Redirect.RunVoid(() -> super.{}({}));", methodOverride.method.getName(), methodOverride.GetBaseCallString());
			sb.AppendLine("            return;");
			break;
		case TaskCompletionSource:
			sb.AppendLine("            return App.Zeze.Redirect.RunFuture(() -> super.{}({}));", methodOverride.method.getName(), methodOverride.GetBaseCallString());
			break;
		}
		sb.AppendLine("        }");
		sb.AppendLine();
	}

	void GenRedirectAll(Zeze.Util.StringBuilderCs sb, Zeze.Util.StringBuilderCs sbHandles,
						Zeze.IModule module, MethodOverride m) throws Throwable {
		var reqVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		var argVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sb.AppendLine("        var {} = new Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest();", reqVarName);
		sb.AppendLine("        var {} = {}.Argument;", argVarName, reqVarName);
		sb.AppendLine("        {}.setModuleId({});", argVarName, module.getId());
		sb.AppendLine("        {}.setHashCodeConcurrentLevel({});", argVarName, m.GetConcurrentLevelSource());
		sb.AppendLine("        {}.setMethodFullName(\"{}:{}\");", argVarName, module.getFullName(), m.method.getName());
		sb.AppendLine("        {}.setServiceNamePrefix(App.ProviderApp.ServerServiceNamePrefix);", argVarName);
		sb.AppendLine("        {}.setSessionId(App.ServerDirect.AddManualContextWithTimeout(", argVarName);
		sb.AppendLine("                new Zeze.Arch.ModuleRedirectAllContext<>({}.getHashCodeConcurrentLevel(), _params_ -> {", argVarName);
		sb.AppendLine("                    var _result_ = new {}();", m.ResultType.getName().replace('$', '.'));
		sb.AppendLine("                    if (_params_ != null) {");
		if (m.ResultHandle != null) {
			var bb = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine("                        var {} = _params_.Wrap();", bb);
			for (var typeName : m.ResultTypeNames)
				Gen.Instance.GenDecode(sb, "                        ", bb, typeName.getKey(), "_result_." + typeName.getValue());
		}
		sb.AppendLine("                    }");
		sb.AppendLine("                    return _result_;");
		sb.AppendLine("                }{}{})));", m.ResultHandle != null ? ", " + m.ResultHandle.Parameter.getName() : "",
				m.ParameterRedirectAllDoneHandle != null ? ", " + m.ParameterRedirectAllDoneHandle.getName() : "");
		if (m.ParametersNormal.size() > 0)
		{
			// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
			sb.AppendLine("        {");
			String bbVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine("            var {} = Zeze.Serialize.ByteBuffer.Allocate();", bbVarName);
			Gen.Instance.GenEncode(sb, "            ", bbVarName, m.ParametersNormal);
			sb.AppendLine("            {}.setParams(new Binary({}));", argVarName, bbVarName);
			sb.AppendLine("        }");
		}
		sb.AppendLine("        App.Zeze.Redirect.RedirectAll(this, {});", reqVarName);
		sb.AppendLine("    }");
		sb.AppendLine();

		// handles
		sbHandles.AppendLine();
		sbHandles.AppendLine("        App.Zeze.Redirect.Handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", module.getFullName(), m.method.getName());
		sbHandles.AppendLine("            Zeze.Transaction.TransactionLevel.{}, (_sessionId_, _hash_, _params_) -> {", m.TransactionLevel);
		var handleBBName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sbHandles.AppendLine("                var {} = _params_.Wrap();", handleBBName);
		for (int i = 0; i < m.ParametersNormal.size(); ++i)
		{
			var p = m.ParametersNormal.get(i);
			if (Gen.IsKnownDelegate(p.getType()))
				continue; // define later.
			Gen.Instance.GenLocalVariable(sbHandles, "                ", p.getType(), p.getName());
		}
		Gen.Instance.GenDecode(sbHandles, "                ", handleBBName, m.ParametersNormal);

		if (m.ResultHandle != null) {
			sbHandles.AppendLine("                var _result_ = new {}();", m.ResultType.getName().replace('$', '.'));
			sbHandles.AppendLine("                _result_.setSessionId(_sessionId_);");
			sbHandles.AppendLine("                _result_.setHash(_hash_);");
			String normalCall = m.GetNormalCallString(pInfo -> pInfo.getType() == RedirectAllDoneHandle.class || pInfo == m.ResultHandle.Parameter);
			String sep = normalCall.isEmpty() ? "" : ", ";
			var bb1 = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine("                super.{}({}{}_result_);", m.method.getName(), normalCall, sep);
			sbHandles.AppendLine("                var {} = Zeze.Serialize.ByteBuffer.Allocate();", bb1);
			for (var typeName : m.ResultTypeNames)
				Gen.Instance.GenEncode(sbHandles, "                ", bb1, typeName.getKey(), "_result_." + typeName.getValue());
			sbHandles.AppendLine("                return new Binary({});", bb1);
		} else {
			String normalCall = m.GetNormalCallString(pInfo -> pInfo.getType() == RedirectAllDoneHandle.class);
			String sep = normalCall.isEmpty() ? "" : ", ";
			sbHandles.AppendLine("                super.{}(_sessionId_, _hash_{}{});", m.method.getName(), sep, normalCall);
			sbHandles.AppendLine("                return Binary.Empty;");
		}
		sbHandles.AppendLine("            }));");
	}
}
