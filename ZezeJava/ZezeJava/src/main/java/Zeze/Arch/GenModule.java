package Zeze.Arch;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Transaction.Transaction;
import Zeze.Util.Func4;
import Zeze.Util.Str;
import Zeze.Beans.Provider.*;
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
	// 本应用：hash分组的一些配置。
	public static final int ChoiceType = BModule.ChoiceTypeHashAccount;
	public static int GetChoiceHashCode() {
		return 0;
		//String account = ((Game.Login.Session) Transaction.getCurrent().getTopProcedure().getUserState()).getAccount();
		//return Zeze.Serialize.ByteBuffer.calc_hashnr(account);
	}

	public static AsyncSocket RandomLink() {
		return null;
		//return Game.App.Instance.Server.RandomLink();
	}

	public static GenModule Instance = new GenModule();

	private void tryCollectMethod(ArrayList<MethodOverride> result, OverrideType type, Method method) {
		switch (type) {
			case Redirect:
				var annotation1 = method.getAnnotation(Redirect.class);
				if (null != annotation1)
					result.add(new MethodOverride(method, OverrideType.Redirect, annotation1));
				break;
			case RedirectAll:
				var annotation2 = method.getAnnotation(RedirectAll.class);
				if (null != annotation2)
					result.add(new MethodOverride(method, OverrideType.RedirectAll, annotation2));
				break;
			case RedirectWithHash:
				var annotation3 = method.getAnnotation(RedirectWithHash.class);
				if (null != annotation3)
					result.add(new MethodOverride(method, OverrideType.RedirectWithHash, annotation3));
				break;
			case RedirectToServer:
				var annotation4 = method.getAnnotation(RedirectToServer.class);
				if (null != annotation4)
					result.add(new MethodOverride(method, OverrideType.RedirectToServer, annotation4));
				break;
		}
	}
	public final Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
		var overrides = new ArrayList<MethodOverride>();
		var methods = module.getClass().getDeclaredMethods();
		for (var method : methods) {
			tryCollectMethod(overrides, OverrideType.Redirect, method);
			tryCollectMethod(overrides, OverrideType.RedirectWithHash, method);
			tryCollectMethod(overrides, OverrideType.RedirectAll, method);
			tryCollectMethod(overrides, OverrideType.RedirectToServer, method);
		}
		if (overrides.isEmpty()) {
			return module; // 没有需要重定向的方法。
		}
		String genClassName = Str.format("_ModuleRedirect_{}_", module.getFullName().replace('.', '_'));
		try {
			String code = GenModuleCode(module, genClassName, overrides);
			//*
			try {
				var tmp = new FileWriter(genClassName + ".java", java.nio.charset.StandardCharsets.UTF_8);
				tmp.write(code);
				tmp.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return module;
			/*/
			module.UnRegister();
			Class<?> moduleClass = compiler.compile(genClassName, code);
			var newModuleInstance = (Zeze.IModule) moduleClass.getDeclaredConstructor(new Class[0]).newInstance();
			newModuleInstance.Initialize(Game.App.Instance);
			return newModuleInstance;
			// */
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

	private String GetMethodNameWithHash(String name) {
		if (!name.startsWith("Run"))
			throw new RuntimeException("Method Name Need StartsWith 'Run'.");
		return name.substring(3);
	}

	private void Verify(MethodOverride method) {
		switch (method.overrideType) {
			case RedirectAll:
				if (method.method.getReturnType() != void.class)
					throw new RuntimeException("RedirectAll ReturnType Must Be void");
				if (method.ParameterRedirectAllDoneHandle != null && method.ParameterRedirectAllResultHandle == null)
					throw new RuntimeException("RedirectAll Has RedirectAllDoneHandle But Miss RedirectAllResultHandle.");
				break;
		}
	}

	private String GenModuleCode(Zeze.IModule module, String genClassName, List<MethodOverride> overrides) throws Throwable {
		var sb = new Zeze.Util.StringBuilderCs();
		sb.AppendLine("");
		sb.AppendLine("import java.util.List;");
		sb.AppendLine("import Zeze.Net.Binary;");
		sb.AppendLine("import Zezex.Provider.BActionParam;");
		sb.AppendLine("");
		sb.AppendLine(Str.format("public class {} extends {}.Module{}", genClassName, module.getFullName(), module.getName()));
		sb.AppendLine("{");

		// TaskCompletionSource<int> void
		var sbHandles = new Zeze.Util.StringBuilderCs();
		var sbContexts = new Zeze.Util.StringBuilderCs();
		for  (var methodOverride : overrides)  {
			methodOverride.PrepareParameters();
			var parametersDefine = Gen.Instance.ToDefineString(methodOverride.ParametersAll);
			var methodNameWithHash = GetMethodNameWithHash(methodOverride.method.getName());
			var rtn = GetReturnType(methodOverride.method.getReturnType());
			Verify(methodOverride);

			sb.AppendLine("    @Override");
			sb.AppendLine(Str.format("    public {} {} ({}){}",
					rtn.ReturnTypeName, methodOverride.method.getName(), parametersDefine, methodOverride.getThrows()));
			sb.AppendLine("    {");
			sb.AppendLine(Str.format("        if (Zezex.ModuleRedirect.Instance.IsLocalServer(\"{}\"))", module.getFullName()));
			sb.AppendLine("        {");
			switch (rtn.ReturnType)
			{
				case Void:
					sb.AppendLine(Str.format("            super.{}({});",
							methodOverride.method.getName(), methodOverride.GetBaseCallString()));
					sb.AppendLine(Str.format("            return;"));
					break;
				case TaskCompletionSource:
					sb.AppendLine(Str.format("            return super.{}({});",
							methodOverride.method.getName(), methodOverride.GetBaseCallString()));
					break;
			}
			sb.AppendLine("        }");
			sb.AppendLine("");

			if (methodOverride.overrideType == OverrideType.RedirectAll) {
				GenRedirectAllContext(sbContexts, module, methodOverride);
				GenRedirectAll(sb, sbHandles, module, methodOverride);
				continue;
			}
			var rpcVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("        var {} = new Zeze.Beans.Provider.ModuleRedirect();", rpcVarName));
			sb.AppendLine(Str.format("        {}.Argument.setModuleId({});", rpcVarName, module.getId()));
			sb.AppendLine(Str.format("        {}.Argument.setRedirectType({});", rpcVarName, methodOverride.getRedirectType()));
			sb.AppendLine(Str.format("        {}.Argument.setHashCode({});", rpcVarName, methodOverride.GetChoiceHashOrServerCodeSource()));
			sb.AppendLine(Str.format("        {}.Argument.setMethodFullName(\"{}:{}\");", rpcVarName, module.getFullName(), methodOverride.method.getName()));
			sb.AppendLine(Str.format("        {}.Argument.setServiceNamePrefix(Game.App.ServerServiceNamePrefix);", rpcVarName));
			if (methodOverride.ParametersNormal.size() > 0) {
				// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
				sb.AppendLine(Str.format("        {"));
				sb.AppendLine(Str.format("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();"));
				Gen.Instance.GenEncode(sb, "            ", "_bb_", methodOverride.ParametersNormal);
				sb.AppendLine(Str.format("            {}.Argument.setParams(new Binary(_bb_));", rpcVarName));
				sb.AppendLine(Str.format("        }"));
			}
			sb.AppendLine(Str.format(""));
			String futureVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("        var {} = new Zeze.Util.TaskCompletionSource<Long>();", futureVarName));
			sb.AppendLine(Str.format(""));
			sb.AppendLine(Str.format("        {}.Send(RandomLink(), (thisRpc) ->", rpcVarName));
			sb.AppendLine(Str.format("        {"));
			sb.AppendLine(Str.format("            if ({}.isTimeout())", rpcVarName));
			sb.AppendLine(Str.format("            {"));
			sb.AppendLine(Str.format("                {}.SetException(new RuntimeException(\"{}:{} Rpc Timeout.\"));", futureVarName, module.getFullName(), methodOverride.method.getName()));
			sb.AppendLine(Str.format("            }"));
			sb.AppendLine(Str.format("            else if (Zezex.Provider.ModuleRedirect.ResultCodeSuccess != {}.getResultCode())", rpcVarName));
			sb.AppendLine(Str.format("            {"));
			sb.AppendLine(Str.format("                {}.SetException(new RuntimeException(\"{}:{} Rpc Error=\" + {}.getResultCode()));", futureVarName, module.getFullName(), methodOverride.method.getName(), rpcVarName));
			sb.AppendLine(Str.format("            }"));
			sb.AppendLine(Str.format("            else"));
			sb.AppendLine(Str.format("            {"));
			if (null != methodOverride.ParameterRedirectResultHandle) {
				// decode and run if has result
				String redirectResultVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				String redirectResultBBVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				sb.AppendLine(Str.format("                var {} = Zeze.Serialize.ByteBuffer.Wrap({}.Result.getActions().get(0));", redirectResultBBVarName, rpcVarName));
				var resultClass = module.getClassByMethodName(methodOverride.method.getName());
				Gen.Instance.GenLocalVariable(sb, "                ", resultClass, redirectResultVarName);
				Gen.Instance.GenDecode(sb, "                ", redirectResultBBVarName, resultClass, redirectResultVarName);
				sb.AppendLine(Str.format("                {}.handle({});", methodOverride.ParameterRedirectResultHandle.getName(), redirectResultVarName));
			}
			sb.AppendLine(Str.format("                {}.SetResult({}.Result.getReturnCode());", futureVarName, rpcVarName));
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
			sbHandles.AppendLine(Str.format("        Zezex.ModuleRedirect.Instance.Handles.put(\"{}:{}\", (Long _sessionid_, Integer _hash_, Binary _params_, List<BActionParam> _actions_) ->", module.getFullName(), methodOverride.method.getName()));
			sbHandles.AppendLine(Str.format("        {"));
			var rbbVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Wrap(_params_);", rbbVarName));
			for (int i = 0; i < methodOverride.ParametersNormal.size(); ++i)
			{
				var p = methodOverride.ParametersNormal.get(i);
				if (Gen.IsKnownDelegate(p.getType()))
					continue; // define later.
				Gen.Instance.GenLocalVariable(sbHandles, "            ", p.getType(), p.getName());
			}
			Gen.Instance.GenDecode(sbHandles, "            ", rbbVarName, methodOverride.ParametersNormal);

			if (null != methodOverride.ParameterRedirectResultHandle) {
				var actionVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				var resultVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				var reqBBVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				sbHandles.AppendLine(Str.format("{}Zezex.RedirectResultHandle {} = ({}) -> {", "    ", actionVarName, resultVarName));
				sbHandles.AppendLine(Str.format("        var {} = Zeze.Serialize.ByteBuffer.Allocate();", reqBBVarName));
				var resultClass = module.getClassByMethodName(methodOverride.method.getName());
				Gen.Instance.GenEncode(sbHandles, "        ", reqBBVarName, resultClass, resultVarName);
				var paramVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
				sbHandles.AppendLine("        var " + paramVarName + " = new BActionParam();");
				sbHandles.AppendLine("        " + paramVarName + ".setName(\"" + methodOverride.ParameterRedirectResultHandle.getName() + "\");");
				sbHandles.AppendLine(Str.format("        " + paramVarName + ".setParams(new Binary({}));", reqBBVarName));
				sbHandles.AppendLine("        _actions_.add(" + paramVarName + ");");
				sbHandles.AppendLine(Str.format("		}"));
				// action.GenActionEncode(sbHandles, "            ");
			}
			String normalcall = methodOverride.GetNarmalCallString();
			String sep = normalcall.isEmpty() ? "" : ", ";
			var returnCodeVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			var returnParamsVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(Str.format("            var {} = super.{}(_hash_{}{});", returnCodeVarName, methodNameWithHash, sep, normalcall));
			sbHandles.AppendLine(Str.format("            var {} = Binary.Empty;", returnParamsVarName));
			sbHandles.AppendLine(Str.format("            return new Zezex.ModuleRedirect.Return({}, {});", returnCodeVarName, returnParamsVarName));
			sbHandles.AppendLine(Str.format("        });"));
			sbHandles.AppendLine(Str.format(""));
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

	void GenRedirectAll(Zeze.Util.StringBuilderCs sb, Zeze.Util.StringBuilderCs sbHandles,
						Zeze.IModule module, MethodOverride m) throws Throwable {
		var reqVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sb.AppendLine(Str.format("        var {} = new Zezex.Provider.ModuleRedirectAllRequest();", reqVarName));
		sb.AppendLine(Str.format("        {}.Argument.setModuleId({});", reqVarName, module.getId()));
		sb.AppendLine(Str.format("        {}.Argument.setHashCodeConcurrentLevel({});", reqVarName, m.GetConcurrentLevelSource()));
		sb.AppendLine(Str.format("        // {}.Argument.setHashCodes(); = // setup in linkd;", reqVarName));
		sb.AppendLine(Str.format("        {}.Argument.setMethodFullName(\"{}:{}\");", reqVarName, module.getFullName(), m.method.getName()));
		sb.AppendLine(Str.format("        {}.Argument.setServiceNamePrefix(Game.App.ServerServiceNamePrefix);", reqVarName));

		String initOnHashEnd = (null == m.ParameterRedirectAllResultHandle) ? "" : ", " + m.ParameterRedirectAllResultHandle.getName();
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
		sb.AppendLine(Str.format("        {}.Send(RandomLink());", reqVarName));
		sb.AppendLine(Str.format("    }"));
		sb.AppendLine(Str.format(""));

		// handles
		sbHandles.AppendLine(Str.format("        Zezex.ModuleRedirect.Instance.Handles.put(\"{}:{}\", (Long _sessionid_, Integer _hash_, Binary _params_, List<BActionParam> _actions_) ->",
				module.getFullName(), m.method.getName()));
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

		if (null != m.ParameterRedirectAllResultHandle) {
			var session = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			var hash = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			var rc = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			var result = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();

			sbHandles.AppendLine(Str.format("{}        Zezex.RedirectAllResultHandle {} = ({}, {}, {}, {}) -> {", "    ",
					m.ParameterRedirectAllResultHandle.getName(), session, hash, rc, result));
			sbHandles.AppendLine("                var _bb1_ = Zeze.Serialize.ByteBuffer.Allocate();");
			Gen.Instance.GenEncode(sbHandles, "                ", "_bb1_", long.class, session);
			Gen.Instance.GenEncode(sbHandles, "                ", "_bb1_", int.class, hash);
			Gen.Instance.GenEncode(sbHandles, "                ", "_bb1_", long.class, rc);
			var resultClass = module.getClassByMethodName(m.method.getName());
			Gen.Instance.GenEncode(sbHandles, "                ", "_bb1_", resultClass, result);
			var paramVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine("                var " + paramVarName + " = new BActionParam();");
			sbHandles.AppendLine("                " + paramVarName + ".setName(\"" + m.ParameterRedirectAllResultHandle.getName() + "\");");
			sbHandles.AppendLine("                " + paramVarName + ".setParams(new Binary(_bb_));");
			sbHandles.AppendLine("                _actions_.add(" + paramVarName + ");");
			sbHandles.AppendLine(Str.format("            };"));
			// action.GenActionEncode(sbHandles, "            ");
		}
		String normalcall = m.GetNarmalCallString((pInfo) -> pInfo.getType() == RedirectAllDoneHandle.class);
		String sep = normalcall.isEmpty() ? "" : ", ";
		var returnCodeVarName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
		sbHandles.AppendLine(Str.format("            var {} = super.{}(_sessionid_, _hash_{}{});",
				returnCodeVarName, GetMethodNameWithHash(m.method.getName()), sep, normalcall));
		sbHandles.AppendLine(Str.format("            return new Zezex.ModuleRedirect.Return({}, Binary.Empty);", returnCodeVarName));
		sbHandles.AppendLine(Str.format("        });"));
		sbHandles.AppendLine(Str.format(""));
	}

	void GenRedirectAllContext(Zeze.Util.StringBuilderCs sb, Zeze.IModule module, MethodOverride m) throws Throwable {
		sb.AppendLine(Str.format("    public static class Context{} extends Zezex.Provider.ModuleProvider.ModuleRedirectAllContext", m.method.getName()));
		sb.AppendLine(Str.format("    {"));
		if (null != m.ParameterRedirectAllResultHandle)
			sb.AppendLine(Str.format("        private Zezex.RedirectAllResultHandle redirectAllResultHandle;"));
		sb.AppendLine(Str.format(""));
		if (null != m.ParameterRedirectAllResultHandle)
			sb.AppendLine(Str.format("        public Context{}(int _c_, String _n_, Zezex.RedirectAllResultHandle _r_) {", m.method.getName()));
		else
			sb.AppendLine(Str.format("        public Context{}(int _c_, String _n_) {", m.method.getName()));

		sb.AppendLine(Str.format("        	super(_c_, _n_);"));
		if (null != m.ParameterRedirectAllResultHandle)
			sb.AppendLine("            this.redirectAllResultHandle = _r_;");
		sb.AppendLine("        }");
		sb.AppendLine("");
		sb.AppendLine("        @Override");
		sb.AppendLine("        public long ProcessHashResult(int _hash_, long _returnCode_, Binary _params, List<BActionParam> _actions_) throws Throwable");
		sb.AppendLine("        {");
		if (null != m.ParameterRedirectAllResultHandle) {
			var allHashResultBBName = "tmp" + Gen.Instance.TmpVarNameId.incrementAndGet();
			sb.AppendLine(Str.format("            var {} = Zeze.Serialize.ByteBuffer.Wrap(_actions_.get(0).getParams());", allHashResultBBName));
			var resultClass = module.getClassByMethodName(m.method.getName());
			Gen.Instance.GenLocalVariable(sb, "            ", resultClass, "_result_bean_");
			Gen.Instance.GenDecode(sb, "            ", allHashResultBBName, resultClass, "_result_bean_");
			sb.AppendLine(Str.format("            redirectAllResultHandle.handle(super.getSessionId(), _hash_, _returnCode_, _result_bean_);"));
		}
		sb.AppendLine("            return Zeze.Transaction.Procedure.Success;");
		sb.AppendLine("        }");
		sb.AppendLine("    }");
		sb.AppendLine("");
	}

	// loopback 优化
	public boolean IsLocalServer(String moduleName) {
		// 要实现真正的 loopback，
		// 需要实现server-server之间直连并且可以得到当前的可用服务。
		// 通过linkd转发时，当前server没有足够信息做这个优化。
		return false;
	}

}
