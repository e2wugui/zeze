package Zezex;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import Game.Rank.BRankList;
import Zeze.Net.Binary;
import Zeze.Transaction.Transaction;
import Zeze.Util.Func4;
import Zezex.Provider.BActionParam;

import java.net.URI;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

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
public class ModuleRedirect {
	// 本应用：hash分组的一些配置。
	public static final int ChoiceType = Zezex.Provider.BModule.ChoiceTypeHashAccount;
	public static int GetChoiceHashCode() {
		String account = GetLoginSession().getAccount();
		return Zeze.Serialize.ByteBuffer.calc_hashnr(account);
	}

	public static Game.Login.Session GetLoginSession() {
		return (Game.Login.Session)Transaction.getCurrent().getTopProcedure().getUserState();
	}

	public static ModuleRedirect Instance = new ModuleRedirect();

	// TODO delete me
	private String SrcDirWhenPostBuild;
	public final String getSrcDirWhenPostBuild() {
		return SrcDirWhenPostBuild;
	}
	public final void setSrcDirWhenPostBuild(String value) {
		SrcDirWhenPostBuild = value;
	}
	private boolean HasNewGen = false;
	public final boolean getHasNewGen() {
		return HasNewGen;
	}
	private void setHasNewGen(boolean value) {
		HasNewGen = value;
	}

	public enum OverrideType {
		Redirect,
		RedirectWithHash,
		RedirectAll,
	}

	public static class MethodOverride {
		public java.lang.reflect.Method Method;
		public OverrideType OverrideType = ModuleRedirect.OverrideType.Redirect;
		public Annotation Attribute;

		public MethodOverride(java.lang.reflect.Method method, OverrideType type, Annotation attribute) {
			Method = method;
			OverrideType = type;
			Attribute = attribute;
		}

		public java.lang.reflect.Parameter ParameterFirstWithHash;
		public ArrayList<java.lang.reflect.Parameter> ParametersNormal = new ArrayList<> ();
		public java.lang.reflect.Parameter ParameterLastWithMode;
		public java.lang.reflect.Parameter[] ParametersAll;
		public java.lang.reflect.Parameter ParameterRedirectResultHandle;
		public java.lang.reflect.Parameter ParameterRedirectAllResultHandle;
		public java.lang.reflect.Parameter ParameterRedirectAllDoneHandle;

		public final void PrepareParameters() {
			ParametersAll = Method.getParameters();
			ParametersNormal.addAll(Arrays.asList(ParametersAll));

			if (OverrideType == OverrideType.RedirectWithHash) {
				ParameterFirstWithHash = ParametersAll[0];
				if (ParameterFirstWithHash.getType() != Integer.class) {
					throw new RuntimeException("ModuleRedirectWithHash: type of first parameter must be 'int'");
				}
				if (false == ParameterFirstWithHash.getName().equals("hash")) {
					throw new RuntimeException("ModuleRedirectWithHash: name of first parameter must be 'hash'");
				}
				ParametersNormal.remove(0);
			}

			if (!ParametersNormal.isEmpty()
					&& ParametersNormal.get(ParametersNormal.size() - 1).getType() == Zeze.TransactionModes.class) {
				ParameterLastWithMode = ParametersNormal.get(ParametersNormal.size() - 1);
				ParametersNormal.remove(ParametersNormal.size() - 1);
			}

			for (var p : ParametersAll) {
				if (p.getType() == Zezex.RedirectAllDoneHandle.class)
					ParameterRedirectAllDoneHandle = p;
				else if (p.getType() == Zezex.RedirectAllResultHandle.class)
					ParameterRedirectAllResultHandle = p;
				else if (p.getType() == Zezex.RedirectResultHandle.class)
					ParameterRedirectResultHandle = p;
			}
		}

		public final String GetNarmalCallString() {
			return GetNarmalCallString(null);
		}

		public final String GetNarmalCallString(Zeze.Util.Func1<java.lang.reflect.Parameter, Boolean> skip) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (int i = 0; i < ParametersNormal.size(); ++i) {
				var p = ParametersNormal.get(i);
				if (null != skip && skip.call(p)) {
					continue;
				}
				if (first) {
					first = false;
				}
				else {
					sb.append(", ");
				}
				sb.append(p.getName());
			}
			return sb.toString();
		}

		public final String GetModeCallString() {
			if (ParameterLastWithMode == null) {
				return "";
			}
			if (ParametersAll.length == 1) { // 除了mode，没有其他参数。
				return ParameterLastWithMode.getName();
			}
			return String.format(", {0}", ParameterLastWithMode.getName());
		}

		public final String GetHashCallString(String varname) {
			if (ParameterFirstWithHash == null) {
				return "";
			}
			if (ParametersAll.length == 1) { // 除了hash，没有其他参数。
				return varname;
			}
			return String.format("{0}, ", varname);
		}

		public final String GetBaseCallString() {
			return String.format("{0}{1}{2}", GetHashCallString("hash"), GetNarmalCallString(), GetModeCallString());
		}

		public final String GetChoiceHashCodeSource() {
			switch (OverrideType) {
				case RedirectWithHash:
					return "hash"; // parameter name

				case Redirect:
					var attr = (Zezex.Redirect)Attribute;
					if (attr.ChoiceHashCodeSource().isEmpty())
						return "Zezex.ModuleRedirect.GetChoiceHashCode()";
					return attr.ChoiceHashCodeSource();

				default:
					throw new RuntimeException("error state");
			}
		}

		public final String GetConcurrentLevelSource() {
			if (OverrideType != OverrideType.RedirectAll) {
				throw new RuntimeException("is not RedirectAll");
			}
			var attr = (Zezex.RedirectAll)Attribute;
			return attr.GetConcurrentLevelSource();
		}
	}

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
		}
	}
	public final Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
		var overrides = new ArrayList<MethodOverride>();
		var methods = module.getClass().getMethods();
		for (var method : methods) {
			tryCollectMethod(overrides, OverrideType.Redirect, method);
			tryCollectMethod(overrides, OverrideType.RedirectWithHash, method);
			tryCollectMethod(overrides, OverrideType.RedirectAll, method);
		}
		if (overrides.isEmpty()) {
			return module; // 没有需要重定向的方法。
		}

		String genClassName = String.format("_ModuleRedirect_{0}_Gen_", module.getFullName().replace('.', '_'));
		String code = GenModuleCode(module, genClassName, overrides);
		try {
			var tmp = new FileWriter(genClassName + ".java", java.nio.charset.StandardCharsets.UTF_8);
			tmp.write(code);
			tmp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return module;
		//module.UnRegister();
		//return CompileCode(code, genClassName);
	}

	private Zeze.IModule CompileCode(String code, String genClassName) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager stdManager = compiler.getStandardFileManager(null, null, null);
		try (var manager = new MemoryJavaFileManager(stdManager)) {
			var javaFileObject = manager.makeStringSource(genClassName, code);
			var task = compiler.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject));
			if (false == task.call())
				throw new RuntimeException("compile failed.");
			try (MemoryClassLoader classLoader = new MemoryClassLoader(manager.getClassBytes())) {
				return (Zeze.IModule)classLoader.loadClass(genClassName).getDeclaredConstructor(new Class[0]).newInstance();
			}
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	static class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

		// compiled classes in bytes:
		final Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

		MemoryJavaFileManager(JavaFileManager fileManager) {
			super(fileManager);
		}

		public Map<String, byte[]> getClassBytes() {
			return new HashMap<String, byte[]>(this.classBytes);
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
			classBytes.clear();
		}

		@Override
		public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, Kind kind,
												   FileObject sibling) throws IOException {
			if (kind == Kind.CLASS) {
				return new MemoryOutputJavaFileObject(className);
			} else {
				return super.getJavaFileForOutput(location, className, kind, sibling);
			}
		}

		JavaFileObject makeStringSource(String name, String code) {
			return new MemoryInputJavaFileObject(name, code);
		}

		static class MemoryInputJavaFileObject extends SimpleJavaFileObject {

			final String code;

			MemoryInputJavaFileObject(String name, String code) {
				super(URI.create("string:///" + name), Kind.SOURCE);
				this.code = code;
			}

			@Override
			public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
				return CharBuffer.wrap(code);
			}
		}

		class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
			final String name;

			MemoryOutputJavaFileObject(String name) {
				super(URI.create("string:///" + name), Kind.CLASS);
				this.name = name;
			}

			@Override
			public OutputStream openOutputStream() {
				return new FilterOutputStream(new ByteArrayOutputStream()) {
					@Override
					public void close() throws IOException {
						out.close();
						ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
						classBytes.put(name, bos.toByteArray());
					}
				};
			}
		}
	}

	static class MemoryClassLoader extends URLClassLoader {

		Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

		public MemoryClassLoader(Map<String, byte[]> classBytes) {
			super(new URL[0], MemoryClassLoader.class.getClassLoader());
			this.classBytes.putAll(classBytes);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] buf = classBytes.get(name);
			if (buf == null) {
				return super.findClass(name);
			}
			classBytes.remove(name);
			return defineClass(name, buf, 0, buf.length);
		}
	}

	public static class Return {
		public int ReturnCode;
		public Binary EncodedParameters;
		public Return(int rc, Binary params) {
			ReturnCode = rc;
			EncodedParameters = params;
		}
	}

	/**
	 0) long [in] sessionid
	 1) int [in] hash
	 2) Zeze.Net.Binary [in] encoded parameters
	 3) List<Zezex.Provider.BActionParam> [result] result for callback. avoid copy.
	 4) Return [return]
		 Func不能使用ref，而Zeze.Net.Binary是只读的。就这样吧。
	*/
	public ConcurrentHashMap<String,
				Func4<Long, Integer, Binary, Collection<BActionParam>, Return>> Handles = new ConcurrentHashMap <>();

	enum ReturnType {
		Void,
		TaskCompletionSource
	}

	static class ReturnTypeAndName {
		public ReturnType ReturnType;
		public String ReturnTypeName;
		public ReturnTypeAndName(ReturnType t, String n) {
			ReturnType = t;
			ReturnTypeName = n;
		}
	}

	private ReturnTypeAndName GetReturnType(Class<?> type)  {
		if (type == void.class)
			return new ReturnTypeAndName(ReturnType.Void, "void");
		if (type == Zeze.Util.TaskCompletionSource.class) {
			// java 怎么获得模板参数列表，检查一下模板参数类型必须Integer.
			return new ReturnTypeAndName(ReturnType.TaskCompletionSource, "Zeze.Util.TaskCompletionSource<Integer>");
		}
		throw new RuntimeException("ReturnType Must Be void Or TaskCompletionSource<Integer>");
	}

	private String GetMethodNameWithHash(String name) {
		if (!name.startsWith("Run"))
			throw new RuntimeException("Method Name Need StartsWith 'Run'.");
		return name.substring(3);
	}

	private void Verify(MethodOverride method) {
		switch (method.OverrideType) {
			case RedirectAll:
				if (method.Method.getReturnType() != void.class)
					throw new RuntimeException("RedirectAll ReturnType Must Be void");
				if (method.ParameterRedirectAllDoneHandle != null && method.ParameterRedirectAllResultHandle == null)
					throw new RuntimeException("RedirectAll Has RedirectAllDoneHandle But Miss RedirectAllResultHandle.");
				break;
		}
	}

	private String GenModuleCode(Zeze.IModule module, String genClassName, List<MethodOverride> overrides)  {
		var sb = new Zeze.Util.StringBuilderCs();
		sb.AppendLine(String.format("public class {0} extends {1}.Module{2}", genClassName, module.getFullName(), module.getName()));
		sb.AppendLine("{");

		// TaskCompletionSource<int> void
		var sbHandles = new Zeze.Util.StringBuilderCs();
		var sbContexts = new Zeze.Util.StringBuilderCs();
		for  (var methodOverride : overrides)  {
			methodOverride.PrepareParameters();
			var parametersDefine = ToDefineString(methodOverride.ParametersAll);
			var methodNameWithHash = GetMethodNameWithHash(methodOverride.Method.getName());
			var rtn = GetReturnType(methodOverride.Method.getReturnType());
			Verify(methodOverride);

			sb.AppendLine("    @Override");
			sb.AppendLine(String.format("    public {0} {1} ({2})",
					rtn.ReturnType, methodOverride.Method.getName(), parametersDefine));
			sb.AppendLine("    {");
			sb.AppendLine(String.format("        if (Zezex.ModuleRedirect.Instance.IsLocalServer(\"{0}\"))", module.getFullName()));
			sb.AppendLine("        {");
			switch (rtn.ReturnType)
			{
				case Void:
					sb.AppendLine(String.format("            base.{0}({1});",
							methodOverride.Method.getName(), methodOverride.GetBaseCallString()));
					sb.AppendLine(String.format("            return;"));
					break;
				case TaskCompletionSource:
					sb.AppendLine(String.format("            return base.{0}({1});",
							methodOverride.Method.getName(), methodOverride.GetBaseCallString()));
					break;
			}
			sb.AppendLine("        }");
			sb.AppendLine("");

			if (methodOverride.OverrideType == OverrideType.RedirectAll) {
				GenRedirectAllContext(sbContexts, module, methodOverride);
				GenRedirectAll(sb, sbHandles, module, methodOverride);
				continue;
			}
			var rpcVarName = "tmp" + TmpVarNameId.incrementAndGet();
			sb.AppendLine(String.format("        var {0} = new Zezex.Provider.ModuleRedirect();", rpcVarName));
			sb.AppendLine(String.format("        {0}.Argument.setModuleId({1});", rpcVarName, module.getId()));
			sb.AppendLine(String.format("        {0}.Argument.setHashCode({1});", rpcVarName, methodOverride.GetChoiceHashCodeSource()));
			sb.AppendLine(String.format("        {0}.Argument.setMethodFullName(\"{1}:{2}\");", rpcVarName, module.getFullName(), methodOverride.Method.getName()));
			sb.AppendLine(String.format("        {0}.Argument.setServiceNamePrefix(Game.App.ServerServiceNamePrefix);", rpcVarName));
			if (methodOverride.ParametersNormal.size() > 0) {
				// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
				sb.AppendLine(String.format("        {"));
				sb.AppendLine(String.format("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();"));
				GenEncode(sb, "            ", methodOverride.ParametersNormal);
				sb.AppendLine(String.format("            {0}.Argument.setParams(new Zeze.Net.Binary(_bb_));", rpcVarName));
				sb.AppendLine(String.format("        }}"));
			}
			sb.AppendLine(String.format(""));
			String sessionVarName = "tmp" + TmpVarNameId.incrementAndGet();
			String futureVarName = "tmp" + TmpVarNameId.incrementAndGet();
			sb.AppendLine(String.format("        var {0} = Zezex.ModuleRedirect.GetLoginSession();", sessionVarName));
			sb.AppendLine(String.format("        var {0} = new Zeze.Util.TaskCompletionSource<Integer>();", futureVarName));
			sb.AppendLine(String.format(""));
			sb.AppendLine(String.format("        {0}.Send({1}.Link, (_) =>", rpcVarName, sessionVarName));
			sb.AppendLine(String.format("        {"));
			sb.AppendLine(String.format("            if ({0}.isTimeout())", rpcVarName));
			sb.AppendLine(String.format("            {"));
			sb.AppendLine(String.format("                {0}.SetException(new RuntimeException(\"{1}:{2} Rpc Timeout.\"));", futureVarName, module.getFullName(), methodOverride.Method.getName()));
			sb.AppendLine(String.format("            }"));
			sb.AppendLine(String.format("            else if (Zezex.Provider.ModuleRedirect.ResultCodeSuccess != {0}.getResultCode())", rpcVarName));
			sb.AppendLine(String.format("            {"));
			sb.AppendLine(String.format("                {0}.SetException(new RuntimeException(\"{1}:{2} Rpc Error=\" + {3}.getResultCode()));", futureVarName, module.getFullName(), methodOverride.Method.getName(), rpcVarName));
			sb.AppendLine(String.format("            }"));
			sb.AppendLine(String.format("            else"));
			sb.AppendLine(String.format("            {"));
			if (null != methodOverride.ParameterRedirectResultHandle) {
				// decode and run if has result
				String redirectResultVarName = "tmp" + TmpVarNameId.incrementAndGet();
				sb.AppendLine(String.format("                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap({0}.Result.getActions().get(0));", rpcVarName));
				var resultClass = module.getClassByMethodName(methodOverride.Method.getName());
				GenLocalVariable(sb, "                ", resultClass, redirectResultVarName);
				GenDecode(sb, "                ", resultClass, redirectResultVarName);
				sb.AppendLine(String.format("                {0}.handle({1});", methodOverride.ParameterRedirectResultHandle.getName(), redirectResultVarName));
			}
			sb.AppendLine(String.format("                {0}.SetResult({1}.Result.getReturnCode());", futureVarName, rpcVarName));
			sb.AppendLine(String.format("            }"));
			sb.AppendLine(String.format("            return Zeze.Transaction.Procedure.Success;"));
			sb.AppendLine(String.format("        });"));
			sb.AppendLine(String.format(""));
			if (rtn.ReturnType == ReturnType.TaskCompletionSource)
			{
				sb.AppendLine(String.format("        return {0};", futureVarName));
			}
			sb.AppendLine(String.format("    }"));
			sb.AppendLine(String.format(""));

			// Handles
			sbHandles.AppendLine(String.format("        Zezex.ModuleRedirect.Instance.Handles.Add(\"{0}:{1}\", (long _sessionid_, int _hash_, Zeze.Net.Binary _params_, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_) =>", module.getFullName(), methodOverride.Method.getName()));
			sbHandles.AppendLine(String.format("        {{"));
			sbHandles.AppendLine(String.format("            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);"));
			for (int i = 0; i < methodOverride.ParametersNormal.size(); ++i)
			{
				var p = methodOverride.ParametersNormal.get(i);
				if (IsKnownDelegate(p.getType()))
					continue; // define later.
				GenLocalVariable(sbHandles, "            ", p.getType(), p.getName());
			}
			GenDecode(sbHandles, "            ", methodOverride.ParametersNormal);

			if (null != methodOverride.ParameterRedirectResultHandle) {
				var actionVarName = "tmp" + TmpVarNameId.incrementAndGet();
				var resultVarName = "tmp" + TmpVarNameId.incrementAndGet();
				sbHandles.AppendLine(String.format("{0}Zezex.RedirectResultHandle {1} = ({2}}) -> {", "    ", actionVarName, resultVarName));
				sbHandles.AppendLine("        var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
				var resultClass = module.getClassByMethodName(methodOverride.Method.getName());
				GenEncode(sbHandles, "        ", resultClass, resultVarName);
				var paramVarName = "tmp" + TmpVarNameId.incrementAndGet();
				sbHandles.AppendLine("        var " + paramVarName + " = new Zezex.Provider.BActionParam();");
				sbHandles.AppendLine("        " + paramVarName + ".setName(\"" + methodOverride.ParameterRedirectResultHandle.getName() + "\");");
				sbHandles.AppendLine("        " + paramVarName + ".setParams(new Zeze.Net.Binary(_bb_));");
				sbHandles.AppendLine("        _actions_.add(" + paramVarName + ");");
				sbHandles.AppendLine(String.format("		}"));
				// action.GenActionEncode(sbHandles, "            ");
			}
			String normalcall = methodOverride.GetNarmalCallString();
			String sep = normalcall.isEmpty() ? "" : ", ";
			var returnCodeVarName = "tmp" + TmpVarNameId.incrementAndGet();
			var returnParamsVarName = "tmp" + TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(String.format("            var {0} = super.{1}(_hash_{2}{3});", returnCodeVarName, methodNameWithHash, sep, normalcall));
			sbHandles.AppendLine(String.format("            var {0} = Zeze.Net.Binary.Empty;", returnParamsVarName));
			sbHandles.AppendLine(String.format("            return new Return({0},{1});", returnCodeVarName, returnParamsVarName));
			sbHandles.AppendLine(String.format("        });"));
			sbHandles.AppendLine(String.format(""));
		}
		sb.AppendLine(String.format("    public {0}() ", genClassName));
		sb.AppendLine(String.format("    {"));
		sb.AppendLine("        super(Game.App.Instance);");
		sb.Append(sbHandles.toString());
		sb.AppendLine(String.format("    }"));
		sb.AppendLine(String.format(""));
		sb.Append(sbContexts.toString());
		sb.AppendLine(String.format("}"));
		return sb.toString();
	}

	private boolean IsKnownDelegate(Class<?> type) {
		if (type.getAnnotation(FunctionalInterface.class) != null) {
			if (type == Zezex.RedirectAllDoneHandle.class || type == Zezex.RedirectAllResultHandle.class
					|| type == Zezex.RedirectResultHandle.class)
				return true;
			throw new RuntimeException("Unknown Delegate!");
		}
		return false;
	}

	void GenRedirectAll(Zeze.Util.StringBuilderCs sb, Zeze.Util.StringBuilderCs sbHandles,
						Zeze.IModule module, MethodOverride m) {
		var reqVarName = "tmp" + TmpVarNameId.incrementAndGet();
		sb.AppendLine(String.format("        var {0} = new Zezex.Provider.ModuleRedirectAllRequest();", reqVarName));
		sb.AppendLine(String.format("        {0}.Argument.setModuleId({1});", reqVarName, module.getId()));
		sb.AppendLine(String.format("        {0}.Argument.setHashCodeConcurrentLevel({1});", reqVarName, m.GetConcurrentLevelSource()));
		sb.AppendLine(String.format("        // {0}.Argument.setHashCodes(); = // setup in linkd;", reqVarName));
		sb.AppendLine(String.format("        {0}.Argument.setMethodFullName(\"{1}:{2}\");", reqVarName, module.getFullName(), m.Method.getName()));
		sb.AppendLine(String.format("        {0}.Argument.setServiceNamePrefix(Game.App.ServerServiceNamePrefix;)", reqVarName));

		String initOnHashEnd = (null == m.ParameterRedirectAllResultHandle) ? "" : ", " + m.ParameterRedirectAllResultHandle.getName();
		String contextVarName = "tmp" + TmpVarNameId.incrementAndGet();
		sb.AppendLine(String.format("        var {0} = new Context{1}({2}.Argument.getHashCodeConcurrentLevel(), {3}.Argument.getMethodFullName(){4});",
				contextVarName, m.Method.getName(), reqVarName, reqVarName, initOnHashEnd));
		sb.AppendLine(String.format("        {0}.Argument.setSessionId(App.Server.AddManualContextWithTimeout({1}));", reqVarName, contextVarName));
		if (m.ParametersNormal.size() > 0)
		{
			// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
			sb.AppendLine(String.format("        {"));
			sb.AppendLine(String.format("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();"));
			GenEncode(sb, "            ", m.ParametersNormal);
			sb.AppendLine(String.format("            {0}.Argument.setParams(new Zeze.Net.Binary(_bb_));", reqVarName));
			sb.AppendLine(String.format("        }"));
		}
		sb.AppendLine(String.format(""));
		var sessionVarName = "tmp" + TmpVarNameId.incrementAndGet();
		sb.AppendLine(String.format("        var {0} = Zezex.ModuleRedirect.GetLoginSession();", sessionVarName));
		sb.AppendLine(String.format("        {0}.Send({1}.getLink());", reqVarName, sessionVarName));
		sb.AppendLine(String.format("    }"));
		sb.AppendLine(String.format(""));

		// handles
		sbHandles.AppendLine(String.format("        Zezex.ModuleRedirect.Instance.Handles.Add(\"{0}:{1}\", (long _sessionid_, int _hash_, Zeze.Net.Binary _params_, java.util.List<Zezex.Provider.BActionParam> _actions_) ->",
				module.getFullName(), m.Method.getName()));
		sbHandles.AppendLine(String.format("        {"));
		sbHandles.AppendLine(String.format("            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);"));
		for (int i = 0; i < m.ParametersNormal.size(); ++i)
		{
			var p = m.ParametersNormal.get(i);
			if (IsKnownDelegate(p.getType()))
				continue; // define later.
			GenLocalVariable(sbHandles, "            ", p.getType(), p.getName());
		}
		GenDecode(sbHandles, "            ", m.ParametersNormal);

		if (null != m.ParameterRedirectAllResultHandle) {
			var actionVarName = "tmp" + TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine(String.format("{0}var {1} = (_sidtmp1_, _hashtmp2_, _rctmp3_, _beantmp4_) -> {", "    ", actionVarName));
			sbHandles.AppendLine("        var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
			GenEncode(sbHandles, "        ", long.class, "_sidtmp1_");
			GenEncode(sbHandles, "        ", int.class, "_hashtmp2_");
			GenEncode(sbHandles, "        ", int.class, "_rctmp3_");
			var resultClass = module.getClassByMethodName(m.Method.getName());
			GenEncode(sbHandles, "        ", resultClass, "_beantmp4_");
			var paramVarName = "tmp" + TmpVarNameId.incrementAndGet();
			sbHandles.AppendLine("        var " + paramVarName + " = new Zezex.Provider.BActionParam();");
			sbHandles.AppendLine("        " + paramVarName + ".setName(\"" + m.ParameterRedirectAllResultHandle.getName() + "\");");
			sbHandles.AppendLine("        " + paramVarName + ".setParams(new Zeze.Net.Binary(_bb_));");
			sbHandles.AppendLine("        _actions_.add(" + paramVarName + ");");
			sbHandles.AppendLine(String.format("		}"));
			// action.GenActionEncode(sbHandles, "            ");
		}
		String normalcall = m.GetNarmalCallString((pInfo) -> pInfo.getType() == Zezex.RedirectAllDoneHandle.class);
		String sep = normalcall.isEmpty() ? "" : ", ";
		var returnCodeVarName = "tmp" + TmpVarNameId.incrementAndGet();
		sbHandles.AppendLine(String.format("            var {0} = super.{1}(_sessionid_, _hash_{2}{3});",
				returnCodeVarName, GetMethodNameWithHash(m.Method.getName()), sep, normalcall));
		sbHandles.AppendLine(String.format("            return new Return({0}, Zeze.Net.Binary.Empty);", returnCodeVarName));
		sbHandles.AppendLine(String.format("        }});"));
		sbHandles.AppendLine(String.format(""));
	}

	void GenRedirectAllContext(Zeze.Util.StringBuilderCs sb, Zeze.IModule module, MethodOverride m) {
		sb.AppendLine(String.format("    public static class Context{0} extends Zezex.Provider.ModuleProvider.ModuleRedirectAllContext", m.Method.getName()));
		sb.AppendLine(String.format("    {"));
		if (null != m.ParameterRedirectAllResultHandle)
			sb.AppendLine(String.format("        private Zezex.RedirectAllResulthandle redirectAllResulthandle;"));
		sb.AppendLine(String.format(""));
		if (null != m.ParameterRedirectAllResultHandle)
			sb.AppendLine(String.format("        public Context{0}(int _c_, string _n_, Zezex.RedirectAllResulthandle _r_) {", m.Method.getName()));
		else
			sb.AppendLine(String.format("        public Context{0}(int _c_, string _n_) {", m.Method.getName()));

		sb.AppendLine(String.format("        	super(_c_, _n_);"));
		if (null != m.ParameterRedirectAllResultHandle)
			sb.AppendLine("            this.redirectAllResulthandle = _r);");
		sb.AppendLine("        }");
		sb.AppendLine("");
		sb.AppendLine("        @Override");
		sb.AppendLine("        public int ProcessHashResult(int _hash_, int _returnCode_, Zeze.Net.Binary _params, java.util.List<Zezex.Provider.BActionParam> _actions_)");
		sb.AppendLine("        {");
		if (null != m.ParameterRedirectAllResultHandle) {
			sb.AppendLine("            Zezex.RedirectAllResulthandle _decode_and_run_ = (_params_) -> {");
			sb.AppendLine("            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
			var resultClass = module.getClassByMethodName(m.Method.getName());
			GenLocalVariable(sb, "            ", resultClass, "_result_bean_");
			GenDecode(sb, "            ", resultClass, "_result_bean_");
			sb.AppendLine(String.format("            redirectAllResulthandle.run(super.SessionId, _hash_, _returnCode_, _result_bean_);"));
			sb.AppendLine("       };");

			sb.AppendLine("            _decode_and_run_.handle(_actions_.get(0).getParams());");
			sb.AppendLine("            }");
		}
		sb.AppendLine("            return Zeze.Transaction.Procedure.Success;");
		sb.AppendLine("        }");
		sb.AppendLine("    }");
		sb.AppendLine("");
	}

	public boolean IsLocalServer(String moduleName) {
		var module = Game.App.Instance.getProviderModuleBinds().getModules().get(moduleName);
		if (null != module) {
			return module.getProviders().contains(Game.App.Instance.Zeze.getConfig().getServerId());
		}
		return false;
	}

	static class KnownSerializer {
		public Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> Encoder;
		public Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> Decoder;
		public Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> Define;
		public Zeze.Util.Func0<String> TypeName;

		public KnownSerializer(Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> enc,
							   Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> dec,
							   Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> def,
							   Zeze.Util.Func0<String> typeName) {
			Encoder = enc;
			Decoder = dec;
			Define = def;
			TypeName = typeName;
		}
	}

	private HashMap<java.lang.Class, KnownSerializer> Serializer = new HashMap<>();
	private AtomicLong TmpVarNameId = new AtomicLong();

	private ModuleRedirect() {
		Serializer.put(Zeze.Net.Binary.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteBinary({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadBinary();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}Zeze.Net.Binary {1} = null;", prefix, varName)),
				() -> "Zeze.Net.Binary")
		);
		Serializer.put(Boolean.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteBool({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadBool();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}bool {1} = false;", prefix, varName)),
				() -> "boolean")
		);
		Serializer.put(boolean.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteBool({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadBool();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}bool {1} = false;", prefix, varName)),
				() -> "boolean")
		);
		Serializer.put(Byte.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteByte({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadByte();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}byte {1} = 0;", prefix, varName)),
				() -> "byte")
		);
		Serializer.put(byte.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteByte({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadByte();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}byte {1} = 0;", prefix, varName)),
				() -> "byte")
		);
		Serializer.put(Zeze.Serialize.ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteByteBuffer({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = Zeze.Serialize.ByteBuffer.Wrap(_bb_.ReadBytes());", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}Zeze.Serialize.ByteBuffer {1} = null;", prefix, varName)),
				() -> "Zeze.Serialize.ByteBuffer")
		);
		Serializer.put(byte[].class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteBytes({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadBytes();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}byte[] {1} = null;", prefix, varName)),
				() -> "byte[]")
		);
		Serializer.put(Double.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteDouble({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadDouble();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}double {1} = 0.0;", prefix, varName)),
				() -> "double")
		);
		Serializer.put(double.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteDouble({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadDouble();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}double {1} = 0.0;", prefix, varName)),
				() -> "double")
		);
		Serializer.put(Float.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteFloat({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadFloat();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}float {1} = 0.0;", prefix, varName)),
				() -> "float")
		);
		Serializer.put(float.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteFloat({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadFloat();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}float {1} = 0.0;", prefix, varName)),
				() -> "float")
		);
		Serializer.put(Integer.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteInt({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadInt();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}int {1} = 0;", prefix, varName)),
				() -> "int")
		);
		Serializer.put(int.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteInt({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadInt();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}int {1} = 0;", prefix, varName)),
				() -> "int")
		);
		Serializer.put(Long.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteLong({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadLong();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}long {1} = 0;", prefix, varName)),
				() -> "long")
		);
		Serializer.put(long.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteLong({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadLong();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}long {1} = 0;", prefix, varName)),
				() -> "long")
		);
		Serializer.put(Short.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteShort({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadShort();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}short {1} = 0;", prefix, varName)),
				() -> "short")
		);
		Serializer.put(short.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteShort({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadShort();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}short {1} = 0;", prefix, varName)),
				() -> "short")
		);
		Serializer.put(String.class, new KnownSerializer(
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}_bb_.WriteString({1});", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}{1} = _bb_.ReadString();", prefix, varName)),
				(sb, prefix, varName) -> sb.AppendLine(String.format("{0}string {1} = null;", prefix, varName)),
				() -> "String")
		);
	}

	public String GetTypeName(Class<?> type) {
		var kn = Serializer.get(type);
		if (null != kn)
			return kn.TypeName.call();

		if (Zeze.Transaction.Bean.class.isAssignableFrom(type))
			return type.getTypeName();

		return type.getTypeName();
	}

	public void GenLocalVariable(Zeze.Util.StringBuilderCs sb, String prefix, Class<?> type, String varName) {
		var kn = Serializer.get(type);
		if (null != kn) {
			kn.Define.run(sb, prefix, varName);
			return;
		}

		if (Zeze.Transaction.Bean.class.isAssignableFrom(type)) {
			sb.AppendLine(String.format("{0}{1} {2} = new {3}();", prefix, type.getTypeName(), varName, type.getTypeName()));
			return;
		}
	}

	public void GenEncode(Zeze.Util.StringBuilderCs sb, String prefix, Class<?> type, String varName) {
		var kn = Serializer.get(type);
		if (null != kn) {
			kn.Encoder.run(sb, prefix, varName);
			return;
		}

		if (Zeze.Transaction.Bean.class.isAssignableFrom(type)) {
			sb.AppendLine(String.format("{0}{1}.Encode(_bb_);", prefix, varName));
			return;
		}

		sb.AppendLine(String.format("{0}try {", prefix));
		sb.AppendLine(String.format("{0}	try (var output = new java.io.ByteArrayOutputStream(); var objOutput = new java.io.ObjectOutputStream(output))", prefix));
		sb.AppendLine(String.format("{0}	{", prefix));
		sb.AppendLine(String.format("{0}		objOutput.writeObject(varName);", prefix));
		sb.AppendLine(String.format("{0}		_bb_.WriteBytes(output.toByteArray());", prefix));
		sb.AppendLine(String.format("{0}	}", prefix));
		sb.AppendLine(String.format("{0}} catch (Throwable e) {", prefix));
		sb.AppendLine(String.format("{0}	throw new RuntimeException(e);", prefix));
		sb.AppendLine(String.format("{0}}", prefix));
	}

	public void GenDecode(Zeze.Util.StringBuilderCs sb, String prefix, Class<?> type, String varName) {
		var kn = Serializer.get(type);
		if (null != kn) {
			kn.Decoder.run(sb, prefix, varName);
			return;
		}

		if (Zeze.Transaction.Bean.class.isAssignableFrom(type)) {
			var tmp0 = String.format("tmp{0}", TmpVarNameId.incrementAndGet());
			sb.AppendLine(String.format("{0}var {1} = new {2}();", prefix, tmp0, type.getTypeName()));
			sb.AppendLine(String.format("{0}{1}.Decode(_bb_);", prefix, tmp0));
			return;
		}
		String tmp1 = "tmp" + TmpVarNameId.incrementAndGet();
		String tmp2 = "tmp" + TmpVarNameId.incrementAndGet();
		sb.AppendLine(String.format("{0}try {", prefix));
		sb.AppendLine(String.format("{0}	var {1} = _bb_.ReadByteBuffer();", prefix, tmp1));
		sb.AppendLine(String.format("{0}	try (var input = new java.io.ByteArrayInputStream({1}); var objinput = new java.io.ObjectInputStream(inpue)) {", prefix, tmp1));
		sb.AppendLine(String.format("{0}		{1} = (2)objinput.readObject();", prefix, varName, GetTypeName(type)));
		sb.AppendLine(String.format("{0}	}", prefix));
		sb.AppendLine(String.format("{0}} catch (Throwable e) {", prefix));
		sb.AppendLine(String.format("{0}	throw new RuntimeException(e);", prefix));
		sb.AppendLine(String.format("{0}}", prefix));
	}

	private boolean IsOut(Class<?> type) {
		return type == Zeze.Util.OutObject.class;
	}

	private boolean IsRef(Class<?> type) {
		return type == Zeze.Util.RefObject.class;
	}

	public void GenEncode(Zeze.Util.StringBuilderCs sb, String prefix, List<java.lang.reflect.Parameter> parameters) {
		for (int i = 0; i < parameters.size(); ++i)  {
			var p = parameters.get(i);
			if (IsOut(p.getType()))
				continue;
			if (IsKnownDelegate(p.getType()))
				continue;
			GenEncode(sb, prefix, p.getType(), p.getName());
		}
	}

	public void GenDecode(Zeze.Util.StringBuilderCs sb, String prefix, List<java.lang.reflect.Parameter> parameters) {
		for (int i = 0; i < parameters.size(); ++i) {
			var p = parameters.get(i);
			if (IsOut(p.getType()))
				continue;
			if (IsKnownDelegate(p.getType()))
				continue;
			GenDecode(sb, prefix, p.getType(), p.getName());
		}
	}

	public String ToDefineString(java.lang.reflect.Parameter[] parameters) {
		var sb = new Zeze.Util.StringBuilderCs();
		boolean first = true;
		for (var p : parameters) {
			if (first)
				first = false;
			else
				sb.Append(", ");
			sb.Append(GetTypeName(p.getType()));
			sb.Append(" ");
			sb.Append(p.getName());
		}
		return sb.toString();
	}
}
