package Zeze.Arch.Gen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllDoneHandle;
import Zeze.Arch.RedirectToServer;
import Zeze.Util.Str;
import Zeze.Util.StringBuilderCs;
import org.mdkt.compiler.InMemoryJavaCompiler;

/** 
 把模块的方法调用发送到其他服务器实例上执行。
 被重定向的方法用注解标明。
 被重定向的方法需要是virtual的。
 实现方案：
 Game.App创建Module的时候调用回调。
 在回调中判断是否存在需要拦截的方法。
 如果需要就动态生成子类实现代码并编译并返回新的实例。

 注意：
 使用 virtual override 的方式可以选择拦截部分方法。
 可以提供和原来模块一致的接口。
*/
public class GenModule {
	public static GenModule Instance = new GenModule();

	/**
	 * 源代码跟目录。
	 * 指定的时候，生成到文件，总是覆盖。
	 * 没有指定的时候，先查看目标类是否存在，存在则直接class.forName装载，否则生成到内存并动态编译。
	 */
	public String GenFileSrcRoot = null;

	private void tryCollectMethod(ArrayList<MethodOverride> result, OverrideType type, Method method) {
		var tLevelAnn = method.getAnnotation(Zeze.Util.TransactionLevel.class);
		var tLevel = Zeze.Transaction.TransactionLevel.Serializable;
		if (null != tLevelAnn)
			tLevel = Zeze.Transaction.TransactionLevel.valueOf(tLevelAnn.Level());
		switch (type) {
			case RedirectAll:
				var annotation2 = method.getAnnotation(RedirectAll.class);
				if (null != annotation2)
					result.add(new MethodOverride(tLevel, method, OverrideType.RedirectAll, annotation2));
				break;
			case RedirectHash:
				var annotation3 = method.getAnnotation(Zeze.Arch.RedirectHash.class);
				if (null != annotation3)
					result.add(new MethodOverride(tLevel, method, OverrideType.RedirectHash, annotation3));
				break;
			case RedirectToServer:
				var annotation4 = method.getAnnotation(RedirectToServer.class);
				if (null != annotation4)
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
		var path = new File(GenFileSrcRoot);
		for (int i = 0; i < ns.length; ++i) {
			path = new File(path, ns[i]);
		}
		return path;
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
		overrides.sort(new Comparator<MethodOverride>() {
			@Override
			public int compare(MethodOverride o1, MethodOverride o2) {
				return o1.method.getName().compareTo(o2.method.getName());
			}
		});

		String genClassName = module.getFullName().replace('.', '_') + "_Redirect";
		String namespace = getNamespace(module.getFullName());
		try {
			if (GenFileSrcRoot == null) {
				// 不需要生成到文件的时候，尝试装载已经存在的生成模块子类。
				try {
					Class <?> moduleClass = Class.forName(namespace + "." + genClassName);
					module.UnRegister();
					var newModuleInstance = (Zeze.IModule) moduleClass.getDeclaredConstructor(new Class[0]).newInstance();
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
					module, genClassName, overrides);

			if (GenFileSrcRoot != null) {
				var file = new File(getNamespaceFilePath(module.getFullName()), genClassName + ".java");
				try {
					var tmp = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
					tmp.write(code);
					tmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// 生成带File需要再次编译，所以这里返回原来的module。
				return module;
			}
			Class<?> moduleClass = compiler.compile(genClassName, code);
			module.UnRegister();
			var newModuleInstance = (Zeze.IModule) moduleClass.getDeclaredConstructor(new Class[0]).newInstance();
			newModuleInstance.Initialize(userApp);
			return newModuleInstance;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private InMemoryJavaCompiler compiler;

	private GenModule() {
		compiler = InMemoryJavaCompiler.newInstance();
		compiler.ignoreWarnings();
	}

	private ReturnTypeAndName GetReturnType(Class<?> type)  {
		if (type == void.class)
			return new ReturnTypeAndName(ReturnType.Void, "void");
		if (type == Zeze.Util.TaskCompletionSource.class) {
			// java 怎么获得模板参数列表，检查一下模板参数类型必须Long.
			return new ReturnTypeAndName(ReturnType.TaskCompletionSource, "Zeze.Util.TaskCompletionSource<Long>");
		}
		throw new RuntimeException("ReturnType Must Be void Or TaskCompletionSource<Long>");
	}

	private void Verify(MethodOverride method) {
		switch (method.overrideType) {
			case RedirectAll:
				if (method.method.getReturnType() != void.class)
					throw new RuntimeException("RedirectAll ReturnType Must Be void");
				if (method.ParameterRedirectAllDoneHandle != null && method.ResultHandle == null)
					throw new RuntimeException("RedirectAll Has RedirectAllDoneHandle But Miss ResultHandle.");
				break;
		}
	}

	private String GenModuleCode(String pkg, Zeze.IModule module, String genClassName, List<MethodOverride> overrides) throws Throwable {
		var sb = new Zeze.Util.StringBuilderCs();
		sb.AppendLine(pkg);
		sb.AppendLine("");
		sb.AppendLine("import Zeze.Net.Binary;");
		sb.AppendLine("import Zeze.Beans.ProviderDirect.*;");
		sb.AppendLine("");

		sb.AppendLine(Str.format("public class {} extends {}.Module{}", genClassName, module.getFullName(), module.getName()));
		sb.AppendLine("{");

		// TaskCompletionSource<int> void
		var sbHandles = new Zeze.Util.StringBuilderCs();
		var sbContexts = new Zeze.Util.StringBuilderCs();
		for  (var m : overrides)  {
			m.PrepareParameters();

			var parametersDefine = m.GetDefineString();
			var methodNameHash = m.method.getName();
			var rtn = GetReturnType(m.method.getReturnType());
			Verify(m);
			if (null != m.ResultHandle)
				m.ResultHandle.Verify(m);

			sb.AppendLine("    @Override");
			sb.AppendLine(Str.format("    public {} {}({}){}",
					rtn.ReturnTypeName, m.method.getName(), parametersDefine, m.getThrows()));
			sb.AppendLine("    {");

			ChoiceTargetRunLoopback(sb, m, rtn);

			if (m.overrideType == OverrideType.RedirectAll) {
				GenRedirectAllContext(sbContexts, module, m);
				GenRedirectAll(sb, sbHandles, module, m);
				continue;
			}

			var rpcVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("        var {} = new ModuleRedirect();", rpcVarName));
			sb.AppendLine(Str.format("        {}.Argument.setModuleId({});", rpcVarName, module.getId()));
			sb.AppendLine(Str.format("        {}.Argument.setRedirectType({});", rpcVarName, m.getRedirectType()));
			sb.AppendLine(Str.format("        {}.Argument.setHashCode({});", rpcVarName, m.GetChoiceHashOrServerCodeSource()));
			sb.AppendLine(Str.format("        {}.Argument.setMethodFullName(\"{}:{}\");", rpcVarName, module.getFullName(), m.method.getName()));
			sb.AppendLine(Str.format("        {}.Argument.setServiceNamePrefix(App.ProviderApp.ServerServiceNamePrefix);", rpcVarName));
			if (m.ParametersNormal.size() > 0) {
				// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
				sb.AppendLine(Str.format("        {"));
				sb.AppendLine(Str.format("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();"));
				Gen.Instance.GenEncode(sb, "            ", "_bb_", m.ParametersNormal);
				sb.AppendLine(Str.format("            {}.Argument.setParams(new Binary(_bb_));", rpcVarName));
				sb.AppendLine(Str.format("        }"));
			}
			sb.AppendLine(Str.format(""));
			String futureVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("        var {} = new Zeze.Util.TaskCompletionSource<Long>();", futureVarName));
			sb.AppendLine(Str.format(""));
			sb.AppendLine(Str.format("        {}.Send(_target_, (thisRpc) ->", rpcVarName));
			sb.AppendLine(Str.format("        {"));
			sb.AppendLine(Str.format("            if ({}.isTimeout())", rpcVarName));
			sb.AppendLine(Str.format("            {"));
			sb.AppendLine(Str.format("                {}.SetException(new RuntimeException(\"{}:{} Rpc Timeout.\"));", futureVarName, module.getFullName(), m.method.getName()));
			sb.AppendLine(Str.format("            }"));
			sb.AppendLine(Str.format("            else if (ModuleRedirect.ResultCodeSuccess != {}.getResultCode())", rpcVarName));
			sb.AppendLine(Str.format("            {"));
			sb.AppendLine(Str.format("                {}.SetException(new RuntimeException(\"{}:{} Rpc Error=\" + {}.getResultCode()));", futureVarName, module.getFullName(), m.method.getName(), rpcVarName));
			sb.AppendLine(Str.format("            }"));
			sb.AppendLine(Str.format("            else"));
			sb.AppendLine(Str.format("            {"));
			if (null != m.ResultHandle) {
				// decode and run if has result
				String bb = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				sb.AppendLine(Str.format("                var {} = Zeze.Serialize.ByteBuffer.Wrap({}.Result.getParams());", bb, rpcVarName));
				m.ResultHandle.GenDecodeAndCallback("                ", sb, m, bb);
			}
			sb.AppendLine(Str.format("                {}.SetResult(0L);", futureVarName));
			sb.AppendLine(Str.format("            }"));
			sb.AppendLine(Str.format("            return Zeze.Transaction.Procedure.Success;"));
			sb.AppendLine(Str.format("        });"));
			sb.AppendLine(Str.format(""));
			if (rtn.ReturnType == ReturnType.TaskCompletionSource)
			{
				sb.AppendLine(Str.format("        return {};", futureVarName));
			}
			sb.AppendLine(Str.format("    }"));
			sb.AppendLine(Str.format(""));

			// Handles
			var tmpHandleName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(Str.format("        var {} = new Zeze.Arch.RedirectHandle();", tmpHandleName));
			sbHandles.AppendLine(Str.format("        {}.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.{};", tmpHandleName, m.TransactionLevel));
			sbHandles.AppendLine(Str.format("        {}.RequestHandle = (Long _sessionid_, Integer _hash_, Binary _params_) ->", tmpHandleName));
			sbHandles.AppendLine(Str.format("        {"));
			var rbbVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Wrap(_params_);", rbbVarName));
			for (int i = 0; i < m.ParametersNormal.size(); ++i)
			{
				var p = m.ParametersNormal.get(i);
				if (Gen.IsKnownDelegate(p.getType()))
					continue; // define later.
				Gen.Instance.GenLocalVariable(sbHandles, "            ", p.getType(), p.getName());
			}
			Gen.Instance.GenDecode(sbHandles, "            ", rbbVarName, m.ParametersNormal);

			var callResultParamName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(Str.format("            var {} = new Zeze.Util.OutObject<Binary>();", callResultParamName));
			sbHandles.AppendLine(Str.format("            {}.Value = Binary.Empty;", callResultParamName));
			if (null != m.ResultHandle) {
				var reqBBVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				var resultVarNames = new ArrayList<String>();
				for (int i = 0; i < m.ResultHandle.GenericArguments.length; ++i)
					resultVarNames.add("tmp" + Gen.Instance.TmpVarNameId.incrementAndGet());
				sbHandles.AppendLine(Str.format("            {} {} = ({}) -> {",
						m.ResultHandle.GetDefineName(), m.ResultHandle.Parameter.getName(), m.ResultHandle.GetCallString(resultVarNames)));
				sbHandles.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Allocate();", reqBBVarName));
				m.ResultHandle.GenEncode(resultVarNames, "                ", sbHandles, m, reqBBVarName);
				sbHandles.AppendLine(Str.format("                {}.Value = new Binary({});", callResultParamName, reqBBVarName));
				sbHandles.AppendLine(Str.format("            };"));
				// action.GenActionEncode(sbHandles, "            ");
			}
			String normalcall = m.GetNarmalCallString();
			String sep = normalcall.isEmpty() ? "" : ", ";
			sbHandles.AppendLine(Str.format("            super.{}(_hash_{}{});", methodNameHash, sep, normalcall));
			sbHandles.AppendLine(Str.format("            return {}.Value;", callResultParamName));
			sbHandles.AppendLine(Str.format("        };"));
			sbHandles.AppendLine(Str.format(""));
			sbHandles.AppendLine(Str.format("        App.Zeze.Redirect.Handles.put(\"{}:{}\", {});", module.getFullName(), m.method.getName(), tmpHandleName));
		}
		sb.AppendLine(Str.format("    public {}() ", genClassName));
		sb.AppendLine(Str.format("    {"));
		sb.AppendLine("        super(Game.App.Instance);");
		sb.Append(sbHandles.toString());
		sb.AppendLine(Str.format("    }"));
		sb.AppendLine(Str.format(""));
		sb.Append(sbContexts.toString());
		sb.AppendLine(Str.format("}"));
		return sb.toString();
	}

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	void ChoiceTargetRunLoopback(StringBuilderCs sb, MethodOverride methodOverride, ReturnTypeAndName rtn) throws Throwable {
		switch (methodOverride.overrideType) {
		case RedirectHash:
			sb.AppendLine("        // RedirectHash");
			sb.AppendLine(Str.format("        var _target_ = App.Zeze.Redirect.ChoiceHash(this, {});",
					methodOverride.ParameterHashOrServer.getName()));
			break;
		case RedirectToServer:
			sb.AppendLine("        // RedirectToServer");
			sb.AppendLine(Str.format("        var _target_ = App.Zeze.Redirect.ChoiceServer(this, {});",
					methodOverride.ParameterHashOrServer.getName()));
			break;
		case RedirectAll:
			sb.AppendLine("        // RedirectAll");
			// RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。
			return;
		}

		sb.AppendLine("        if (_target_ == null)");
		sb.AppendLine("        {");
		sb.AppendLine("            // local: loop-back");
		switch (rtn.ReturnType)
		{
		case Void:
			sb.AppendLine(Str.format("            App.Zeze.Redirect.RunVoid(() -> super.{}({}));",
					methodOverride.method.getName(), methodOverride.GetBaseCallString()));
			sb.AppendLine(Str.format("            return;"));
			break;

		case TaskCompletionSource:
			sb.AppendLine(Str.format("            return App.Zeze.Redirect.RunFuture(() -> super.{}({}));",
					methodOverride.method.getName(), methodOverride.GetBaseCallString()));
			break;
		}
		sb.AppendLine("        }");
		sb.AppendLine("");
	}

	void GenRedirectAll(Zeze.Util.StringBuilderCs sb, Zeze.Util.StringBuilderCs sbHandles,
						Zeze.IModule module, MethodOverride m) throws Throwable {
		var reqVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sb.AppendLine(Str.format("        var {} = new ModuleRedirectAllRequest();", reqVarName));
		sb.AppendLine(Str.format("        {}.Argument.setModuleId({});", reqVarName, module.getId()));
		sb.AppendLine(Str.format("        {}.Argument.setHashCodeConcurrentLevel({});", reqVarName, m.GetConcurrentLevelSource()));
		sb.AppendLine(Str.format("        // {}.Argument.setHashCodes(); = // setup in linkd;", reqVarName));
		sb.AppendLine(Str.format("        {}.Argument.setMethodFullName(\"{}:{}\");", reqVarName, module.getFullName(), m.method.getName()));
		sb.AppendLine(Str.format("        {}.Argument.setServiceNamePrefix(App.ProviderApp.ServerServiceNamePrefix);", reqVarName));

		String initOnHashEnd = (null == m.ResultHandle) ? "" : ", " + m.ResultHandle.Parameter.getName();
		String contextVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sb.AppendLine(Str.format("        var {} = new Context{}({}.Argument.getHashCodeConcurrentLevel(), {}.Argument.getMethodFullName(){});",
				contextVarName, m.method.getName(), reqVarName, reqVarName, initOnHashEnd));
		sb.AppendLine(Str.format("        {}.Argument.setSessionId(App.Server.AddManualContextWithTimeout({}));",
				reqVarName, contextVarName));
		if (m.ParametersNormal.size() > 0)
		{
			// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
			sb.AppendLine(Str.format("        {"));
			String bbVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Allocate();", bbVarName));
			Gen.Instance.GenEncode(sb, "            ", bbVarName, m.ParametersNormal);
			sb.AppendLine(Str.format("            {}.Argument.setParams(new Binary({}));", reqVarName, bbVarName));
			sb.AppendLine(Str.format("        }"));
		}
		sb.AppendLine(Str.format(""));
		sb.AppendLine(Str.format("        App.Zeze.Redirect.RedirectAll(this, {});", reqVarName));
		sb.AppendLine(Str.format("    }"));
		sb.AppendLine(Str.format(""));

		// handles
		var handleVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sbHandles.AppendLine(Str.format("        var {} = new Zeze.Arch.RedirectHandle();", handleVarName));
		sbHandles.AppendLine(Str.format("        {}.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.{};", handleVarName, m.TransactionLevel));
		sbHandles.AppendLine(Str.format("        {}.RequestHandle = (Long _sessionid_, Integer _hash_, Binary _params_) ->", handleVarName));
		sbHandles.AppendLine(Str.format("        {"));
		var handleBBName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sbHandles.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Wrap(_params_);", handleBBName));
		for (int i = 0; i < m.ParametersNormal.size(); ++i)
		{
			var p = m.ParametersNormal.get(i);
			if (Gen.IsKnownDelegate(p.getType()))
				continue; // define later.
			Gen.Instance.GenLocalVariable(sbHandles, "            ", p.getType(), p.getName());
		}
		Gen.Instance.GenDecode(sbHandles, "            ", handleBBName, m.ParametersNormal);

		var callResultParamName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sbHandles.AppendLine(Str.format("            var {} = new Zeze.Util.OutObject<Binary>();", callResultParamName));
		sbHandles.AppendLine(Str.format("            {}.Value = Binary.Empty;", callResultParamName));
		if (null != m.ResultHandle) {
			var resultVarNames = new ArrayList<String>();
			for (int i = 0; i < m.ResultHandle.GenericArguments.length; ++i)
				resultVarNames.add("tmp" + Gen.Instance.TmpVarNameId.incrementAndGet());

			sbHandles.AppendLine(Str.format("            {} {} = ({}) -> {",
					m.ResultHandle.GetDefineName(), m.ResultHandle.Parameter.getName(), m.ResultHandle.GetCallString(resultVarNames)));
			var bb1 = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(Str.format("                var {} = Zeze.Serialize.ByteBuffer.Allocate();", bb1));
			m.ResultHandle.GenEncode(resultVarNames, "                ", sbHandles, m, bb1);
			sbHandles.AppendLine(Str.format("                {}.Value = new Binary({});", callResultParamName, bb1));
			sbHandles.AppendLine(Str.format("            };"));
			// action.GenActionEncode(sbHandles, "            ");
		}
		String normalcall = m.GetNarmalCallString((pInfo) -> pInfo.getType() == RedirectAllDoneHandle.class);
		String sep = normalcall.isEmpty() ? "" : ", ";
		sbHandles.AppendLine(Str.format("            super.{}(_sessionid_, _hash_{}{});",
				m.method.getName(), sep, normalcall));
		sbHandles.AppendLine(Str.format("            return {}.Value;", callResultParamName));
		sbHandles.AppendLine(Str.format("        };"));
		sbHandles.AppendLine(Str.format(""));

		sbHandles.AppendLine(Str.format("        App.Zeze.Redirect.Handles.put(\"{}:{}\", {});",
				module.getFullName(), m.method.getName(), handleVarName));
	}

	void GenRedirectAllContext(Zeze.Util.StringBuilderCs sb, Zeze.IModule module, MethodOverride m) throws Throwable {
		sb.AppendLine(Str.format("    public static class Context{} extends Zeze.Arch.ModuleRedirectAllContext", m.method.getName()));
		sb.AppendLine(Str.format("    {"));
		if (null != m.ResultHandle)
			sb.AppendLine(Str.format("        private {} _rrh_;", m.ResultHandle.GetDefineName()));
		sb.AppendLine(Str.format(""));
		if (null != m.ResultHandle)
			sb.AppendLine(Str.format("        public Context{}(int _c_, String _n_, {} _r_) {",
					m.method.getName(), m.ResultHandle.GetDefineName()));
		else
			sb.AppendLine(Str.format("        public Context{}(int _c_, String _n_) {", m.method.getName()));

		sb.AppendLine(Str.format("        	super(_c_, _n_);"));
		if (null != m.ResultHandle)
			sb.AppendLine("            this._rrh_ = _r_;");
		sb.AppendLine("        }");
		sb.AppendLine("");
		sb.AppendLine("        @Override");
		sb.AppendLine("        public long ProcessHashResult(Zeze.Application zeze, int _hash_, Binary _params) throws Throwable");
		sb.AppendLine("        {");
		if (null != m.ResultHandle) {
			var bb = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Wrap(_params);", bb));
			m.ResultHandle.GenDecodeAndCallback("zeze", "            ", sb, "_rrh_", m, bb);
		}
		sb.AppendLine("            return Zeze.Transaction.Procedure.Success;");
		sb.AppendLine("        }");
		sb.AppendLine("    }");
		sb.AppendLine("");
	}
}
