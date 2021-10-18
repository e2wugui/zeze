package Zezex;

import java.lang.annotation.Annotation;
import java.util.*;
import java.io.*;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Transaction.Transaction;
import Zeze.Util.Func4;
import Zezex.Provider.BActionParam;

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
		var tempVar = (Game.Login.Session)Transaction.getCurrent().getTopProcedure().getUserState();
		return tempVar;
	}

	public static ModuleRedirect Instance = new ModuleRedirect();

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
		RedirectAll;
	}

	public static class MethodOverride {
		private java.lang.reflect.Method Method;
		public final java.lang.reflect.Method getMethod() {
			return Method;
		}
		private OverrideType OverrideType = ModuleRedirect.OverrideType.Redirect;
		public final OverrideType getOverrideType() {
			return OverrideType;
		}
		private Annotation Attribute;
		public final Annotation getAttribute() {
			return Attribute;
		}

		public MethodOverride(java.lang.reflect.Method method, OverrideType type, Annotation attribute) {
			Method = method;
			OverrideType = type;
			Attribute = attribute;
		}

		private ParameterInfo ParameterFirstWithHash;
		public final ParameterInfo getParameterFirstWithHash() {
			return ParameterFirstWithHash;
		}
		private void setParameterFirstWithHash(ParameterInfo value) {
			ParameterFirstWithHash = value;
		}
		private ArrayList<ParameterInfo> ParametersNormal = new ArrayList<ParameterInfo> ();
		public final ArrayList<ParameterInfo> getParametersNormal() {
			return ParametersNormal;
		}
		private ParameterInfo ParameterLastWithMode;
		public final ParameterInfo getParameterLastWithMode() {
			return ParameterLastWithMode;
		}
		private void setParameterLastWithMode(ParameterInfo value) {
			ParameterLastWithMode = value;
		}

		private ParameterInfo[] ParametersAll;
		public final ParameterInfo[] getParametersAll() {
			return ParametersAll;
		}
		private void setParametersAll(ParameterInfo[] value) {
			ParametersAll = value;
		}

		public final void PrepareParameters() {
			setParametersAll(getMethod().GetParameters());
			getParametersNormal().addAll(Arrays.asList(getParametersAll()));

			if (getOverrideType() == OverrideType.RedirectWithHash) {
				setParameterFirstWithHash(getParametersAll()[0]);
				if (getParameterFirstWithHash().ParameterType != Integer.class) {
					throw new RuntimeException("ModuleRedirectWithHash: type of first parameter must be 'int'");
				}
				if (false == getParameterFirstWithHash().Name.equals("hash")) {
					throw new RuntimeException("ModuleRedirectWithHash: name of first parameter must be 'hash'");
				}
				getParametersNormal().remove(0);
			}

			if (!getParametersNormal().isEmpty() && getParametersNormal().get(getParametersNormal().size() - 1).ParameterType == Zeze.TransactionModes.class) {
				setParameterLastWithMode(getParametersNormal().get(getParametersNormal().size() - 1));
				getParametersNormal().remove(getParametersNormal().size() - 1);
			}
		}


		public final String GetNarmalCallString() {
			return GetNarmalCallString(null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public string GetNarmalCallString(Func<ParameterInfo, bool> skip = null)
		public final String GetNarmalCallString(Zeze.Util.Func1<ParameterInfo, Boolean> skip) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (int i = 0; i < getParametersNormal().size(); ++i) {
				var p = getParametersNormal().get(i);
				if (null != skip && skip.call(p)) {
					continue;
				}
				if (first) {
					first = false;
				}
				else {
					sb.append(", ");
				}
				String prefix = "";
				if (p.IsOut) {
					prefix = "out ";
				}
				else if (p.ParameterType.IsByRef) {
					prefix = "ref ";
				}

				if (tangible.StringHelper.isNullOrEmpty(prefix)) {
					sb.append(p.Name);
				}
				else {
					sb.append(prefix).append(p.Name);
				}
			}
			return sb.toString();
		}

		public final String GetModeCallString() {
			if (getParameterLastWithMode() == null) {
				return "";
			}
			if (getParametersAll().length == 1) { // 除了mode，没有其他参数。
				return getParameterLastWithMode().Name;
			}
			return String.format(", %1$s", getParameterLastWithMode().Name);
		}

		public final String GetHashCallString(String varname) {
			if (getParameterFirstWithHash() == null) {
				return "";
			}
			if (getParametersAll().length == 1) { // 除了hash，没有其他参数。
				return varname;
			}
			return String.format("%1$s, ", varname);
		}

		public final String GetBaseCallString() {
			return String.format("%1$s%2$s%3$s", GetHashCallString("hash"), GetNarmalCallString(), GetModeCallString());
		}

		public final String GetChoiceHashCodeSource() {
			switch (getOverrideType()) {
				case RedirectWithHash:
					return "hash"; // parameter name

				case Redirect:
					Attribute tempVar = getAttribute();
					var attr = tempVar instanceof ModuleRedirectAttribute ? (ModuleRedirectAttribute)tempVar : null;
					if (tangible.StringHelper.isNullOrEmpty(attr.getChoiceHashCodeSource())) {
						return "Game.ModuleRedirect.GetChoiceHashCode()";
					}
					return attr.getChoiceHashCodeSource();

				default:
					throw new RuntimeException("error state");
			}
		}

		public final String GetConcurrentLevelSource() {
			if (getOverrideType() != OverrideType.RedirectAll) {
				throw new RuntimeException("is not RedirectAll");
			}
			Attribute tempVar = getAttribute();
			var attr = tempVar instanceof ModuleRedirectAllAttribute ? (ModuleRedirectAllAttribute)tempVar : null;
			return attr.getGetConcurrentLevelSource();
		}
	}

	private boolean CheckAddMethod(java.lang.reflect.Method method, OverrideType type, Object[] attrs, ArrayList<MethodOverride> result) {
		if (attrs.length == 1) {
			result.add(new MethodOverride(method, type, attrs[0] instanceof Attribute ? (Attribute)attrs[0] : null));
			return true;
		}
		return false;
	}

	public final Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
		ArrayList<MethodOverride> overrides = new ArrayList<MethodOverride>();
		var methods = module.getClass().getMethods();
		for (var method : methods) {
			if (CheckAddMethod(method, OverrideType.Redirect, method.GetCustomAttributes(ModuleRedirectAttribute.class, false), overrides)) {
				continue;
			}
			if (CheckAddMethod(method, OverrideType.RedirectWithHash, method.GetCustomAttributes(ModuleRedirectWithHashAttribute.class, false), overrides)) {
				continue;
			}
			if (CheckAddMethod(method, OverrideType.RedirectAll, method.GetCustomAttributes(ModuleRedirectAllAttribute.class, false), overrides)) {
				continue;
			}
		}
		if (overrides.isEmpty()) {
			return module; // 没有需要重定向的方法。
		}


		String genClassName = String.format("_ModuleRedirect_%1$s_Gen_", module.FullName.replace('.', '_'));
		if (getSrcDirWhenPostBuild().equals(null)) {
			module.UnRegister();
			//Console.WriteLine($"'{module.FullName}' Replaced.");
			// from Game.App.Start. try load new module instance.
			return (Zeze.IModule)java.lang.Class.forName(genClassName).newInstance();
		}

		String srcFileName = Path.Combine(getSrcDirWhenPostBuild(), module.FullName.replace('.', File.separatorChar), String.format("Module%1$s.cs", module.Name));

		long srcLastWriteTimeTicks = System.IO.File.GetLastWriteTime(srcFileName).getTime();
		String genFileName = Path.Combine(getSrcDirWhenPostBuild(), "Gen", genClassName + ".cs");

		if (false == (new File(genFileName)).isFile() || System.IO.File.GetLastWriteTime(genFileName).getTime() != srcLastWriteTimeTicks) {
			System.out.println("ModuleRedirect '" + module.FullName + "' Gen Now ...");
			setHasNewGen(true);
			String code = GenModuleCode(module, genClassName, overrides);
			//*
			//System.IO.File.Delete(genFileName); // 如果被vs占用，删除也没用。
//C# TO JAVA CONVERTER WARNING: The java.io.OutputStreamWriter constructor does not accept all the arguments passed to the System.IO.StreamWriter constructor:
//ORIGINAL LINE: System.IO.StreamWriter sw = new System.IO.StreamWriter(genFileName, false, Encoding.UTF8);
			OutputStreamWriter sw = new OutputStreamWriter(genFileName, java.nio.charset.StandardCharsets.UTF_8);
			sw.write(code);
			sw.close();
			System.IO.File.SetLastWriteTime(genFileName, LocalDateTime.of(srcLastWriteTimeTicks));
			/*/
			// .net core, .net 5.0+ 不支持编译。 
			module.UnRegister();
			return CompileCode(code, genClassName);
			//*/
		}
		return module;
	}

	/*
	private Zeze.IModule CompileCode(string code, string genClassName)
	{
	    var options = new CompilerParameters();
	    options.GenerateExecutable = false;
	    options.GenerateInMemory = true;
	    var provider = CodeDomProvider.CreateProvider("CSharp");
	    var result = provider.CompileAssemblyFromSource(options, code);
	    if (result.Errors.Count > 0)
	    {
	        // Display compilation errors.
	        foreach (var ce in result.Errors)
	        {
	            Console.WriteLine(ce.ToString());
	        }
	        throw new Exception("Compile Error.");
	    }
	    var type = result.CompiledAssembly.GetType(genClassName);
	    return (Zeze.IModule)Activator.CreateInstance(type);
	}
	// */

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
				Func4<Long, Integer, Binary, List<BActionParam>, Return>> Handles = new ConcurrentHashMap <>();

	enum ReturnType {
		Void,
		TaskCompletionSource
	}
//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private (ReturnType, string) GetReturnType(Type type)
//		{
//			if (type == typeof(void))
//				return (ReturnType.Void, "void");
//			if (type == typeof(TaskCompletionSource<int>))
//				return (ReturnType.TaskCompletionSource, "System.Threading.Tasks.TaskCompletionSource<int>");
//			throw new Exception("ReturnType Must Be void Or TaskCompletionSource<int>");
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private string GetMethodNameWithHash(string name)
//		{
//			if (!name.StartsWith("Run"))
//				throw new Exception("Method Name Need StartsWith 'Run'.");
//			return name.Substring(3);
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private List<ParameterInfo> GetOutOrRef(List<ParameterInfo> parameters)
//		{
//			var result = new List<ParameterInfo>();
//			for (int i = 0; i < parameters.Count; ++i)
//			{
//				var p = parameters[i];
//				if (p.IsOut)
//					result.Add(p);
//				else if (p.ParameterType.IsByRef)
//					result.Add(p);
//			}
//			return result;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private static bool IsDelegate(Type type)
//		{
//			if (type.IsByRef)
//				type = type.GetElementType();
//			return type == typeof(Delegate) || type.IsSubclassOf(typeof(Delegate));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private static bool IsActionDelegate(Type sourceType)
//		{
//			if (sourceType.IsSubclassOf(typeof(MulticastDelegate)) && sourceType.GetMethod("Invoke").ReturnType == typeof(void))
//				return true;
//			return false;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public class ActionGen
//		{
//			public Type ActionType
//			{
//				get;
//			}
//
//			public Type[] GenericArguments
//			{
//				get;
//			}
//			public string[] GenericArgumentVarNames
//			{
//				get;
//			}
//			public string VarName
//			{
//				get;
//			}
//
//			public ActionGen(Type actionType, string varName)
//			{
//				if (false == ModuleRedirect.IsActionDelegate(actionType))
//					throw new Exception("Need A Action Callback.");
//
//				ActionType = actionType;
//				VarName = varName;
//
//				GenericArguments = ActionType.GetGenericArguments();
//				GenericArgumentVarNames = new string[GenericArguments.Length];
//				for (int i = 0; i < GenericArguments.Length; ++i)
//				{
//					var arg = GenericArguments[i];
//
//					if (ModuleRedirect.IsDelegate(arg))
//						throw new Exception("Action GenericArgument IsDelegate.");
//
//					// 这个好像不可能，判断一下吧。
//					if (arg.IsByRef)
//						throw new Exception("Action GenericArgument IsByRef.");
//
//					GenericArgumentVarNames[i] = "tmp" + ModuleRedirect.Instance.TmpVarNameId.IncrementAndGet();
//				}
//			}
//
//			public string GetGenericArgumentsDefine()
//			{
//				if (GenericArguments.Length == 0)
//					return string.Empty;
//
//				StringBuilder sb = new StringBuilder();
//				sb.Append("<");
//				for (int i = 0; i < GenericArguments.Length; ++i)
//				{
//					var argType = GenericArguments[i];
//					if (i > 0)
//						sb.Append(", ");
//					sb.Append(ModuleRedirect.Instance.GetTypeName(argType));
//				}
//				sb.Append(">");
//
//				return sb.ToString();
//			}
//
//			private string GetGenericArgumentVarNamesDefine(int offset = 0)
//			{
//				StringBuilder sb = new StringBuilder();
//				for (int i = offset; i < GenericArgumentVarNames.Length; ++i)
//				{
//					if (i > offset)
//						sb.Append(", ");
//					sb.Append(GenericArgumentVarNames[i]);
//				}
//				return sb.ToString();
//			}
//
//			public void GenActionEncode(StringBuilder sb, string prefix)
//			{
//				sb.AppendLine(string.Format("{0}System.Action{1} {2} = ({3}) =>", prefix, GetGenericArgumentsDefine(), VarName, GetGenericArgumentVarNamesDefine()));
//				sb.AppendLine(string.Format("{0}{{", prefix));
//				if (GenericArguments.Length > 0)
//				{
//					sb.AppendLine(string.Format("{0}    var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();", prefix));
//					GenEncode(sb, prefix + "    ");
//					sb.AppendLine(string.Format("{0}    _actions_.Add(new Zezex.Provider.BActionParam() {{ Name = \"{1}\", Params = new Zeze.Net.Binary(_bb_) }});", prefix, VarName));
//				}
//				sb.AppendLine(string.Format("{0};", prefix}}));
//			}
//
//			public void GenActionDecode(StringBuilder sb, string prefix, string callcontext = "", int offset = 0)
//			{
//				sb.AppendLine(string.Format("{0}System.Action<Zeze.Net.Binary> _{1}_ = (_params_) =>", prefix, VarName));
//				sb.AppendLine(string.Format("{0}{{", prefix));
//				if (GenericArguments.Length > 0)
//				{
//					sb.AppendLine(string.Format("{0}    var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);", prefix));
//					GenDecode(sb, prefix + "    ", offset);
//					string sep = string.IsNullOrEmpty(callcontext) ? "" : ", ";
//					sb.AppendLine(string.Format("{0}    {1}({2}{3}{4});", prefix, VarName, callcontext, sep, GetGenericArgumentVarNamesDefine(offset)));
//				}
//				sb.AppendLine(string.Format("{0};", prefix}}));
//			}
//
//			private void GenEncode(StringBuilder sb, string prefix)
//			{
//				for (int i = 0; i < GenericArguments.Length; ++i)
//				{
//					var type = GenericArguments[i];
//					ModuleRedirect.Instance.GenEncode(sb, prefix, type, GenericArgumentVarNames[i]);
//				}
//			}
//
//			private void GenDecode(StringBuilder sb, string prefix, int offset = 0)
//			{
//				for (int i = offset; i < GenericArguments.Length; ++i)
//				{
//					var type = GenericArguments[i];
//					ModuleRedirect.Instance.GenLocalVariable(sb, prefix, type, GenericArgumentVarNames[i]);
//					ModuleRedirect.Instance.GenDecode(sb, prefix, type, GenericArgumentVarNames[i]);
//				}
//			}
//
//			public bool IsOnHashEnd
//			{
//				get
//				{
//					return ModuleRedirect.IsOnHashEnd(GenericArguments);
//				}
//			}
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private static bool IsOnHashEnd(Type[] GenericArguments)
//		{
//			if (GenericArguments.Length != 1)
//				return false;
//			if (GenericArguments[0] != typeof(Zezex.Provider.ModuleProvider.ModuleRedirectAllContext))
//				return false;
//			return true;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private static bool IsOnHashEnd(ParameterInfo pInfo)
//		{
//			var pType = pInfo.ParameterType;
//			if (!IsActionDelegate(pType))
//				return false;
//			return IsOnHashEnd(pType.GetGenericArguments());
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private List<ActionGen> GetActions(List<ParameterInfo> parameters)
//		{
//			List<ActionGen> result = new List<ActionGen>();
//			for (int i = 0; i < parameters.Count; ++i)
//			{
//				var p = parameters[i];
//				if (IsDelegate(p.ParameterType))
//				{
//					result.Add(new ActionGen(p.ParameterType, p.Name));
//				}
//			}
//			return result;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private void Verify(MethodOverride method, List<ParameterInfo> outRefParams, List<ActionGen> actions)
//		{
//			switch (method.OverrideType)
//			{
//				case OverrideType.RedirectAll:
//					if (outRefParams.Count > 0)
//						throw new Exception("RedirectAll Not Support out|ref.");
//					if (method.Method.ReturnType != typeof(void))
//						throw new Exception("RedirectAll ReturnType Must Be void");
//
//					// 如果每个hash分组处理需要多个回调，把这个检查去掉。
//					if (actions.Count > 2)
//						throw new Exception("RedirectAll (actions.Count > 2)");
//
//					int end = 0;
//					foreach (var action in actions)
//					{
//						if (action.IsOnHashEnd)
//						{
//							end++;
//							continue;
//						}
//
//						if (action.GenericArguments.Length < 3)
//							throw new Exception("RedirectAll callback must have parameters: long,int");
//						if (action.GenericArguments[0] != typeof(long))
//							throw new Exception("RedirectAll callback first parameter muse be long(sessionId)");
//						if (action.GenericArguments[1] != typeof(int))
//							throw new Exception("RedirectAll callback second parameter muse be int(hash-index)");
//						if (action.GenericArguments[2] != typeof(int))
//							throw new Exception("RedirectAll callback thrid parameter muse be int(return-code)");
//					}
//					if (end > 1)
//						throw new Exception("RedirectAll Too Many OnHashEnd.");
//					break;
//			}
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private string GenModuleCode(Zeze.IModule module, string genClassName, List<MethodOverride> overrides)
//		{
//			StringBuilder sb = new StringBuilder();
//			sb.AppendLine(string.Format("public class {0} : {1}.Module{2}", genClassName, module.FullName, module.Name));
//			sb.AppendLine(string.Format("{{"));
//
//			// TaskCompletionSource<int> void
//			StringBuilder sbHandles = new StringBuilder();
//			StringBuilder sbContexts = new StringBuilder();
//			foreach (var methodOverride in overrides)
//			{
//				methodOverride.PrepareParameters();
//				var parametersDefine = ToDefineString(methodOverride.ParametersAll);
//				var parametersOutOrRef = GetOutOrRef(methodOverride.ParametersNormal);
//				var methodNameWithHash = GetMethodNameWithHash(methodOverride.Method.Name);
//				var(returnType, returnTypeName) = GetReturnType(methodOverride.Method.ReturnParameter.ParameterType);
//				var actions = GetActions(methodOverride.ParametersNormal);
//				Verify(methodOverride, parametersOutOrRef, actions);
//
//				sb.AppendLine(string.Format("    public override {0} {1}({2})", returnTypeName, methodOverride.Method.Name, parametersDefine));
//				sb.AppendLine(string.Format("    {{"));
//				sb.AppendLine(string.Format("        if (Game.ModuleRedirect.Instance.IsLocalServer(\"{0}\"))", module.FullName));
//				sb.AppendLine(string.Format("        {{"));
//				switch (returnType)
//				{
//					case ReturnType.Void:
//						sb.AppendLine(string.Format("            base.{0}({1});", methodOverride.Method.Name, methodOverride.GetBaseCallString()));
//						sb.AppendLine(string.Format("            return;"));
//						break;
//					case ReturnType.TaskCompletionSource:
//						sb.AppendLine(string.Format("            return base.{0}({1});", methodOverride.Method.Name, methodOverride.GetBaseCallString()));
//						break;
//				}
//				sb.AppendLine(string.Format("        }}"));
//				sb.AppendLine(string.Format(""));
//
//				if (methodOverride.OverrideType == OverrideType.RedirectAll)
//				{
//					GenRedirectAllContext(sbContexts, methodOverride, actions);
//					GenRedirectAll(sb, sbHandles, module, methodOverride, actions);
//					continue;
//				}
//				string rpcVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//				sb.AppendLine(string.Format("        var {0} = new Zezex.Provider.ModuleRedirect();", rpcVarName));
//				sb.AppendLine(string.Format("        {0}.Argument.ModuleId = {1};", rpcVarName, module.Id));
//				sb.AppendLine(string.Format("        {0}.Argument.HashCode = {1};", rpcVarName, methodOverride.GetChoiceHashCodeSource()));
//				sb.AppendLine(string.Format("        {0}.Argument.MethodFullName = \"{1}:{2}\";", rpcVarName, module.FullName, methodOverride.Method.Name));
//				sb.AppendLine(string.Format("        {0}.Argument.ServiceNamePrefix = Game.App.ServerServiceNamePrefix;", rpcVarName));
//				if (methodOverride.ParametersNormal.Count > 0)
//				{
//					// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
//					sb.AppendLine(string.Format("        {{"));
//					sb.AppendLine(string.Format("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();"));
//					GenEncode(sb, "            ", methodOverride.ParametersNormal);
//					sb.AppendLine(string.Format("            {0}.Argument.Params = new Zeze.Net.Binary(_bb_);", rpcVarName));
//					sb.AppendLine(string.Format("        }}"));
//				}
//				sb.AppendLine(string.Format(""));
//				string sessionVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//				string futureVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//				sb.AppendLine(string.Format("        var {0} = Game.ModuleRedirect.GetLoginSession();", sessionVarName));
//				sb.AppendLine(string.Format("        var {0} = new System.Threading.Tasks.TaskCompletionSource<int>();", futureVarName));
//				sb.AppendLine(string.Format(""));
//				foreach (var outOrRef in parametersOutOrRef)
//				{
//					GenLocalVariable(sb, "        ", outOrRef.ParameterType, "_" + outOrRef.Name + "_");
//					if (!outOrRef.IsOut && outOrRef.ParameterType.IsByRef)
//						sb.AppendLine(string.Format("        _{0}_ = {1};", outOrRef.Name, outOrRef.Name));
//				}
//				sb.AppendLine(string.Format("        {0}.Send({1}.Link, (_) =>", rpcVarName, sessionVarName));
//				sb.AppendLine(string.Format("        {{"));
//				sb.AppendLine(string.Format("            if ({0}.IsTimeout)", rpcVarName));
//				sb.AppendLine(string.Format("            {{"));
//				sb.AppendLine(string.Format("                {0}.SetException(new System.Exception(\"{1}:{2} Rpc Timeout.\"));", futureVarName, module.FullName, methodOverride.Method.Name));
//				sb.AppendLine(string.Format("            }}"));
//				sb.AppendLine(string.Format("            else if (Zezex.Provider.ModuleRedirect.ResultCodeSuccess != {0}.ResultCode)", rpcVarName));
//				sb.AppendLine(string.Format("            {{"));
//				sb.AppendLine(string.Format("                {0}.SetException(new System.Exception($\"{1}:{2} Rpc Error {{{3}.ResultCode}}.\"));", futureVarName, module.FullName, methodOverride.Method.Name, rpcVarName));
//				sb.AppendLine(string.Format("            }}"));
//				sb.AppendLine(string.Format("            else"));
//				sb.AppendLine(string.Format("            {{"));
//				if (actions.Count > 0)
//				{
//					foreach (var action in actions)
//					{
//						action.GenActionDecode(sb, "                ");
//					}
//					var actionVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//					sb.AppendLine(string.Format("                foreach (var {0} in {1}.Result.Actions)", actionVarName, rpcVarName));
//					sb.AppendLine(string.Format("                {{"));
//					sb.AppendLine(string.Format("                    switch ({0}.Name)", actionVarName));
//					sb.AppendLine(string.Format("                    {{"));
//					foreach (var action in actions)
//					{
//						sb.AppendLine(string.Format("                        case \"{0}\": _{1}_({2}.Params); break;", action.VarName, action.VarName, actionVarName));
//					}
//					sb.AppendLine(string.Format("                    }}"));
//					sb.AppendLine(string.Format("                }}"));
//				}
//				if (parametersOutOrRef.Count > 0)
//				{
//					sb.AppendLine(string.Format("                {{"));
//					sb.AppendLine(string.Format("                    var _bb_ = Zeze.Serialize.ByteBuffer.Wrap({0}.Result.Params);", rpcVarName));
//					foreach (var outOrRef in parametersOutOrRef)
//					{
//						GenDecode(sb, "                    ", outOrRef.ParameterType, "_" + outOrRef.Name + "_");
//					}
//					sb.AppendLine(string.Format("                }}"));
//				}
//				sb.AppendLine(string.Format("                {0}.SetResult({1}.Result.ReturnCode);", futureVarName, rpcVarName));
//				sb.AppendLine(string.Format("            }}"));
//				sb.AppendLine(string.Format("            return Zeze.Transaction.Procedure.Success;"));
//				sb.AppendLine(string.Format("        }});"));
//				sb.AppendLine(string.Format(""));
//				if (parametersOutOrRef.Count > 0)
//				{
//					sb.AppendLine(string.Format("        {0}.Task.Wait();", futureVarName));
//					foreach (var outOrRef in parametersOutOrRef)
//					{
//						sb.AppendLine(string.Format("        {0} = _{1}_;", outOrRef.Name, outOrRef.Name));
//					}
//				}
//				if (returnType == ReturnType.TaskCompletionSource)
//				{
//					sb.AppendLine(string.Format("        return {0};", futureVarName));
//				}
//				sb.AppendLine(string.Format("    }}"));
//				sb.AppendLine(string.Format(""));
//
//				sbHandles.AppendLine(string.Format("        Game.ModuleRedirect.Instance.Handles.Add(\"{0}:{1}\", (long _sessionid_, int _hash_, Zeze.Net.Binary _params_, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_) =>", module.FullName, methodOverride.Method.Name));
//				sbHandles.AppendLine(string.Format("        {{"));
//				sbHandles.AppendLine(string.Format("            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);"));
//				for (int i = 0; i < methodOverride.ParametersNormal.Count; ++i)
//				{
//					var p = methodOverride.ParametersNormal[i];
//					if (IsDelegate(p.ParameterType))
//						continue; // define later.
//					GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
//				}
//				GenDecode(sbHandles, "            ", methodOverride.ParametersNormal);
//
//				if (actions.Count > 0)
//				{
//					foreach (var action in actions)
//					{
//						action.GenActionEncode(sbHandles, "            ");
//					}
//				}
//				string normalcall = methodOverride.GetNarmalCallString();
//				string sep = string.IsNullOrEmpty(normalcall) ? "" : ", ";
//				var returnCodeVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//				var returnParamsVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//				sbHandles.AppendLine(string.Format("            var {0} = base.{1}(_hash_{2}{3});", returnCodeVarName, methodNameWithHash, sep, normalcall));
//				sbHandles.AppendLine(string.Format("            var {0} = Zeze.Net.Binary.Empty;", returnParamsVarName));
//				if (parametersOutOrRef.Count > 0)
//				{
//					sbHandles.AppendLine(string.Format("            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(); // reuse _bb_"));
//					foreach (var outOrRef in parametersOutOrRef)
//					{
//						GenEncode(sbHandles, "            ", outOrRef.ParameterType, outOrRef.Name);
//					}
//					sbHandles.AppendLine(string.Format("            {0} = new Zeze.Net.Binary(_bb_);", returnParamsVarName));
//				}
//				sbHandles.AppendLine(string.Format("            return ({0},{1});", returnCodeVarName, returnParamsVarName));
//				sbHandles.AppendLine(string.Format("        }});"));
//				sbHandles.AppendLine(string.Format(""));
//			}
//			sb.AppendLine(string.Format("    public {0}() : base(Game.App.Instance)", genClassName));
//			sb.AppendLine(string.Format("    {{"));
//			sb.Append(sbHandles.ToString());
//			sb.AppendLine(string.Format("    }}"));
//			sb.AppendLine(string.Format(""));
//			sb.Append(sbContexts.ToString());
//			sb.AppendLine(string.Format("}}"));
//			return sb.ToString();
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	void GenRedirectAll(StringBuilder sb, StringBuilder sbHandles, Zeze.IModule module, MethodOverride methodOverride, List<ActionGen> actions)
//		{
//			string reqVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("        var {0} = new Zezex.Provider.ModuleRedirectAllRequest();", reqVarName));
//			sb.AppendLine(string.Format("        {0}.Argument.ModuleId = {1};", reqVarName, module.Id));
//			sb.AppendLine(string.Format("        {0}.Argument.HashCodeConcurrentLevel = {1};", reqVarName, methodOverride.GetConcurrentLevelSource()));
//			sb.AppendLine(string.Format("        // {0}.Argument.HashCodes = // setup in linkd;", reqVarName));
//			sb.AppendLine(string.Format("        {0}.Argument.MethodFullName = \"{1}:{2}\";", reqVarName, module.FullName, methodOverride.Method.Name));
//			sb.AppendLine(string.Format("        {0}.Argument.ServiceNamePrefix = Game.App.ServerServiceNamePrefix;", reqVarName));
//
//			int actionCountSkipOnHashEnd = GetActionCountSkipOnHashEnd(actions);
//			string initOnHashEnd = "";
//			bool first = true;
//			StringBuilder actionVarNames = new StringBuilder();
//			foreach (var action in actions)
//			{
//				if (action.IsOnHashEnd)
//				{
//					initOnHashEnd = string.Format("{{ OnHashEnd = {0} }}", action.VarName);
//					continue;
//				}
//
//				if (first)
//					first = false;
//				else
//					actionVarNames.Append(", ");
//				actionVarNames.Append(string.Format("{0}", action.VarName));
//			}
//			string contextVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("        var {0} = new Context{1}({2}.Argument.HashCodeConcurrentLevel, {3}.Argument.MethodFullName, {4}){5};", contextVarName, methodOverride.Method.Name, reqVarName, reqVarName, actionVarNames, initOnHashEnd));
//			sb.AppendLine(string.Format("        {0}.Argument.SessionId = App.Server.AddManualContextWithTimeout({1});", reqVarName, contextVarName));
//			if (methodOverride.ParametersNormal.Count > 0)
//			{
//				// normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
//				sb.AppendLine(string.Format("        {{"));
//				sb.AppendLine(string.Format("            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();"));
//				GenEncode(sb, "            ", methodOverride.ParametersNormal);
//				sb.AppendLine(string.Format("            {0}.Argument.Params = new Zeze.Net.Binary(_bb_);", reqVarName));
//				sb.AppendLine(string.Format("        }}"));
//			}
//			sb.AppendLine(string.Format(""));
//			string sessionVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("        var {0} = Game.ModuleRedirect.GetLoginSession();", sessionVarName));
//			sb.AppendLine(string.Format("        {0}.Send({1}.Link);", reqVarName, sessionVarName));
//			sb.AppendLine(string.Format("    }}"));
//			sb.AppendLine(string.Format(""));
//
//			// handles
//			sbHandles.AppendLine(string.Format("        Game.ModuleRedirect.Instance.Handles.Add(\"{0}:{1}\", (long _sessionid_, int _hash_, Zeze.Net.Binary _params_, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_) =>", module.FullName, methodOverride.Method.Name));
//			sbHandles.AppendLine(string.Format("        {{"));
//			sbHandles.AppendLine(string.Format("            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);"));
//			for (int i = 0; i < methodOverride.ParametersNormal.Count; ++i)
//			{
//				var p = methodOverride.ParametersNormal[i];
//				if (IsDelegate(p.ParameterType))
//					continue; // define later.
//				GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
//			}
//			GenDecode(sbHandles, "            ", methodOverride.ParametersNormal);
//
//			if (actionCountSkipOnHashEnd > 0)
//			{
//				foreach (var action in actions)
//				{
//					if (action.IsOnHashEnd)
//						continue;
//					action.GenActionEncode(sbHandles, "            ");
//				}
//			}
//			string normalcall = methodOverride.GetNarmalCallString((pInfo) => IsOnHashEnd(pInfo));
//			string sep = string.IsNullOrEmpty(normalcall) ? "" : ", ";
//			var returnCodeVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//			sbHandles.AppendLine(string.Format("            var {0} = base.{1}(_sessionid_, _hash_{2}{3});", returnCodeVarName, GetMethodNameWithHash(methodOverride.Method.Name), sep, normalcall));
//			sbHandles.AppendLine(string.Format("            return ({0}, Zeze.Net.Binary.Empty);", returnCodeVarName));
//			sbHandles.AppendLine(string.Format("        }});"));
//			sbHandles.AppendLine(string.Format(""));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private int GetActionCountSkipOnHashEnd(List<ActionGen> actions)
//		{
//			int count = 0;
//			foreach (var action in actions)
//			{
//				if (action.IsOnHashEnd)
//					continue;
//				++count;
//			}
//			return count;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	void GenRedirectAllContext(StringBuilder sb, MethodOverride methodOverride, List<ActionGen> actions)
//		{
//			sb.AppendLine(string.Format("    public class Context{0} : Zezex.Provider.ModuleProvider.ModuleRedirectAllContext", methodOverride.Method.Name));
//			sb.AppendLine(string.Format("    {{"));
//			foreach (var action in actions)
//			{
//				if (action.IsOnHashEnd)
//					continue;
//				sb.AppendLine(string.Format("        private System.Action{0} {1};", action.GetGenericArgumentsDefine(), action.VarName));
//			}
//			sb.AppendLine(string.Format(""));
//			StringBuilder actionVarNames = new StringBuilder();
//			bool first = true;
//			foreach (var action in actions)
//			{
//				if (action.IsOnHashEnd)
//					continue;
//
//				if (first)
//					first = false;
//				else
//					actionVarNames.Append(", ");
//				actionVarNames.Append(string.Format("System.Action{0} {1}", action.GetGenericArgumentsDefine(), action.VarName));
//			}
//			sb.AppendLine(string.Format("        public Context{0}(int _c_, string _n_, {1}) : base(_c_, _n_)", methodOverride.Method.Name, actionVarNames));
//			sb.AppendLine(string.Format("        {{"));
//			int actionCountSkipOnHashEnd = GetActionCountSkipOnHashEnd(actions);
//			foreach (var action in actions)
//			{
//				if (action.IsOnHashEnd)
//					continue;
//				sb.AppendLine(string.Format("            this.{0} = {1};", action.VarName, action.VarName));
//			}
//			sb.AppendLine(string.Format("        }}"));
//			sb.AppendLine(string.Format(""));
//			sb.AppendLine(string.Format("        public override int ProcessHashResult(int _hash_, int _returnCode_, Zeze.Net.Binary _params, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_)"));
//			sb.AppendLine(string.Format("        {{"));
//			if (actionCountSkipOnHashEnd > 0)
//			{
//				foreach (var action in actions)
//				{
//					if (action.IsOnHashEnd)
//						continue;
//					action.GenActionDecode(sb, "            ", "base.SessionId, _hash_, _returnCode_", 3);
//				}
//				var actionVarName = "tmp" + TmpVarNameId.IncrementAndGet();
//				sb.AppendLine(string.Format("            foreach (var {0} in _actions_)", actionVarName));
//				sb.AppendLine(string.Format("            {{"));
//				sb.AppendLine(string.Format("                switch ({0}.Name)", actionVarName));
//				sb.AppendLine(string.Format("                {{"));
//				foreach (var action in actions)
//				{
//					if (action.IsOnHashEnd)
//						continue;
//					sb.AppendLine(string.Format("                    case \"{0}\": _{1}_({2}.Params); break;", action.VarName, action.VarName, actionVarName));
//				}
//				sb.AppendLine(string.Format("                }}"));
//				sb.AppendLine(string.Format("            }}"));
//			}
//			sb.AppendLine(string.Format("            return Zeze.Transaction.Procedure.Success;"));
//			sb.AppendLine(string.Format("        }}"));
//			sb.AppendLine(string.Format("    }}"));
//			sb.AppendLine(string.Format(""));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public bool IsLocalServer(string moduleName)
//		{
//			if (Game.App.Instance.ProviderModuleBinds.Modules.TryGetValue(moduleName, out var module))
//			{
//				return module.Providers.Contains(Game.App.Instance.Zeze.Config.ServerId);
//			}
//			return false;
//		}

	private HashMap<java.lang.Class, (tangible.Action3Param<StringBuilder, String, String>, tangible.Action3Param<StringBuilder, String, String>, tangible.Action3Param<StringBuilder, String, String>, tangible.Func0Param<String>)> Serializer = new HashMap<java.lang.Class, (tangible.Action3Param<StringBuilder, String, String>, tangible.Action3Param<StringBuilder, String, String>, tangible.Action3Param<StringBuilder, String, String>, tangible.Func0Param<String>)>();

	private HashMap<String, (tangible.Action5Param<StringBuilder, String, String, java.lang.Class, java.lang.Class>, tangible.Action5Param<StringBuilder, String, String, java.lang.Class, java.lang.Class>, tangible.Action5Param<StringBuilder, String, String, java.lang.Class, java.lang.Class>, tangible.Func2Param<java.lang.Class, java.lang.Class, String>)> System_Collections_Generic_Serializer = new HashMap<String, (tangible.Action5Param<StringBuilder, String, String, java.lang.Class, java.lang.Class>, tangible.Action5Param<StringBuilder, String, String, java.lang.Class, java.lang.Class>, tangible.Action5Param<StringBuilder, String, String, java.lang.Class, java.lang.Class>, tangible.Func2Param<java.lang.Class, java.lang.Class, String>)>();

	private Zeze.Util.AtomicLong TmpVarNameId = new Zeze.Util.AtomicLong();

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private ModuleRedirect()
//		{
// /*
// Serializer[typeof(void)] = (
//     (sb, prefix, varName) => { },
//     (sb, prefix, varName) => { },
//     (sb, prefix, varName) => { },
//     () => "void"
//     );
// */
//
//			Serializer[typeof(Zeze.Net.Binary)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteBinary({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadBinary();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}Zeze.Net.Binary {1} = null;", prefix, varName)), () => "Zeze.Net.Binary");
//
//			Serializer[typeof(bool)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteBool({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadBool();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}bool {1} = false;", prefix, varName)), () => "bool");
//
//			Serializer[typeof(byte)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteByte({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadByte();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}byte {1} = 0;", prefix, varName)), () => "byte");
//
//			Serializer[typeof(Zeze.Serialize.ByteBuffer)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteByteBuffer({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = Zeze.Serialize.ByteBuffer.Wrap(_bb_.ReadBytes());", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}Zeze.Serialize.ByteBuffer {1} = null;", prefix, varName)), () => "Zeze.Serialize.ByteBuffer");
//
//			Serializer[typeof(byte[])] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteBytes({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadBytes();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}byte[] {1} = null;", prefix, varName)), () => "byte[]");
//
//			Serializer[typeof(double)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteDouble({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadDouble();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}double {1} = 0.0;", prefix, varName)), () => "double");
//
//			Serializer[typeof(float)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteFloat({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadFloat();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}float {1} = 0.0;", prefix, varName)), () => "float");
//
//			Serializer[typeof(int)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteInt({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadInt();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}int {1} = 0;", prefix, varName)), () => "int");
//
//			Serializer[typeof(long)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteLong({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadLong();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}long {1} = 0;", prefix, varName)), () => "long");
//
//			Serializer[typeof(short)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteShort({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadShort();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}short {1} = 0;", prefix, varName)), () => "short");
//
//			Serializer[typeof(string)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteString({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadString();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}string {1} = null;", prefix, varName)), () => "string");
//
//			Serializer[typeof(uint)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteUint({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadUint();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}uint {1} = 0;", prefix, varName)), () => "uint");
//
//			Serializer[typeof(ulong)] = ((sb, prefix, varName) => sb.AppendLine(string.Format("{0}_bb_.WriteUlong({1});", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}{1} = _bb_.ReadUlong();", prefix, varName)), (sb, prefix, varName) => sb.AppendLine(string.Format("{0}ulong {1} = 0;", prefix, varName)), () => "ulong");
//
//			/////////////////////////////////////////////////////////////////////////
//			/**
//			*/
//			System_Collections_Generic_Serializer["System.Collections.Generic.Dictionary"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.Dictionary<{2}, {3}>();", prefix, varName, GetTypeName(key), GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.Dictionary<{0}, {1}>", GetTypeName(key), GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.HashSet"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.HashSet<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.HashSet<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.ICollection"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.List<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.ICollection<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IDictionary"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.Dictionary<{2}, {3}>();", prefix, varName, GetTypeName(key), GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IDictionary<{0}, {1}>", GetTypeName(key), GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IEnumerable"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.List<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IEnumerable<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IList"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.List<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IList<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyCollection"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.List<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IReadOnlyCollection<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyDictionary"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.Dictionary<{2}, {3}>();", prefix, varName, GetTypeName(key), GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IReadOnlyDictionary<{0}, {1}>", GetTypeName(key), GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyList"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.List<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IReadOnlyList<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlySet"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.HashSet<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.IReadOnlySet<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.ISet"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.HashSet<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.ISet<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.KeyValuePair"] = ((sb, prefix, varName, key, value) =>
//				{
//					GenEncode(sb, prefix, key, string.Format("{0}.Key", varName));
//					GenEncode(sb, prefix, value, string.Format("{0}.Value", varName));
//				}
//			   , (sb, prefix, varName, key, value) =>
//				{
//					string tmpKey = "tmpKey" + TmpVarNameId.IncrementAndGet();
//					string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
//					GenLocalVariable(sb, prefix, key, tmpKey);
//					GenLocalVariable(sb, prefix, value, tmpValue);
//					GenDecode(sb, prefix, key, string.Format("{0}", tmpKey));
//					GenDecode(sb, prefix, value, string.Format("{0}", tmpValue));
//					sb.AppendLine(string.Format("{0}{1} = new System.Collections.Generic.KeyValuePair<{2}, {3}>({4}, {5});", prefix, varName, GetTypeName(key), GetTypeName(value), tmpKey, tmpValue));
//				}
//			   , (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}System.Collections.Generic.KeyValuePair<{1}, {2}> {3} = null;", prefix, GetTypeName(key), GetTypeName(value), varName)), (key, value) => string.Format("System.Collections.Generic.KeyValuePair<{0}, {1}>", GetTypeName(key), GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.LinkedList"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.LinkedList<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.LinkedList<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.List"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.List<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.List<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.Queue"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.Queue<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.Queue<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.SortedDictionary"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.SortedDictionary<{2}, {3}>();", prefix, varName, GetTypeName(key), GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.SortedDictionary<{0}, {1}>", GetTypeName(key), GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.SortedList"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.SortedList<{2}, {3}>();", prefix, varName, GetTypeName(key), GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.SortedList<{0}, {1}>", GetTypeName(key), GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.SortedSet"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.SortedSet<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.SortedSet<{0}>", GetTypeName(value)));
//
//			System_Collections_Generic_Serializer["System.Collections.Generic.Stack"] = ((sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), (sb, prefix, varName, key, value) =>
//			{
//				string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
//				string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
//				string tmpReverse = "tmpReverse" + TmpVarNameId.IncrementAndGet();
//				string tmpReverseValue = "tmpReverse" + TmpVarNameId.IncrementAndGet();
//				sb.AppendLine(string.Format("{0}int {1} = _bb_.ReadInt();", prefix, tmpi));
//				sb.AppendLine(string.Format("{0}{1} [] {2} = new {3}[{4}];", prefix, GetTypeName(value), tmpReverse, GetTypeName(value), tmpi));
//				sb.AppendLine(string.Format("{0}for (; {1} > 0; --{2})", prefix, tmpi, tmpi));
//				sb.AppendLine(string.Format("{0}{{", prefix));
//				GenLocalVariable(sb, prefix + "    ", value, tmpValue);
//				GenDecode(sb, prefix + "    ", value, string.Format("{0}", tmpValue));
//				sb.AppendLine(string.Format("{0}    {1}[{2} - 1] = {3};", prefix, tmpReverse, tmpi, tmpValue));
//				sb.AppendLine(string.Format("{0}", prefix}}));
//				sb.AppendLine(string.Format("{0}foreach (var {1} in {2})", prefix, tmpReverseValue, tmpReverse));
//				sb.AppendLine(string.Format("{0}{{", prefix));
//				sb.AppendLine(string.Format("{0}    {1}.Push({2});", prefix, varName, tmpReverseValue));
//				sb.AppendLine(string.Format("{0}", prefix}}));
//			}
//		   , (sb, prefix, varName, key, value) => sb.AppendLine(string.Format("{0}var {1} = new System.Collections.Generic.Stack<{2}>();", prefix, varName, GetTypeName(value))), (key, value) => string.Format("System.Collections.Generic.Stack<{0}>", GetTypeName(value)));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private void GenEncodeGeneric_2(StringBuilder sb, string prefix, string varName, Type key, Type value)
//		{
//			string tmp = "tmp" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("{0}_bb_.WriteInt({1}.Count);", prefix, varName));
//			sb.AppendLine(string.Format("{0}foreach (var {1} in {2})", prefix, tmp, varName));
//			sb.AppendLine(string.Format("{0}{{", prefix));
//			GenEncode(sb, prefix + "    ", key, string.Format("{0}.Key", tmp));
//			GenEncode(sb, prefix + "    ", value, string.Format("{0}.Value", tmp));
//			sb.AppendLine(string.Format("{0}", prefix}}));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private void GenDecodeGeneric_2(StringBuilder sb, string prefix, string varName, Type key, Type value)
//		{
//			string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
//			string tmpKey = "tmpKey" + TmpVarNameId.IncrementAndGet();
//			string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("{0}for (int {1} = _bb_.ReadInt(); {2} > 0; --{3})", prefix, tmpi, tmpi, tmpi));
//			sb.AppendLine(string.Format("{0}{{", prefix));
//			GenLocalVariable(sb, prefix + "    ", key, tmpKey);
//			GenLocalVariable(sb, prefix + "    ", value, tmpValue);
//			GenDecode(sb, prefix + "    ", key, string.Format("{0}", tmpKey));
//			GenDecode(sb, prefix + "    ", value, string.Format("{0}", tmpValue));
//			sb.AppendLine(string.Format("{0}    {1}.Add({2}, {3});", prefix, varName, tmpKey, tmpValue));
//			sb.AppendLine(string.Format("{0}", prefix}}));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private void GenEncodeGeneric_1(StringBuilder sb, string prefix, string varName, Type value)
//		{
//			string tmp = "tmp" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("{0}_bb_.WriteInt({1}.Count);", prefix, varName));
//			sb.AppendLine(string.Format("{0}foreach (var {1} in {2})", prefix, tmp, varName));
//			sb.AppendLine(string.Format("{0}{{", prefix));
//			GenEncode(sb, prefix + "    ", value, string.Format("{0}", tmp));
//			sb.AppendLine(string.Format("{0}", prefix}}));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	private void GenDecodeGeneric_1(StringBuilder sb, string prefix, string varName, Type value)
//		{
//			string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
//			string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("{0}for (int {1} = _bb_.ReadInt(); {2} > 0; --{3})", prefix, tmpi, tmpi, tmpi));
//			sb.AppendLine(string.Format("{0}{{", prefix));
//			GenLocalVariable(sb, prefix + "    ", value, tmpValue);
//			GenDecode(sb, prefix + "    ", value, string.Format("{0}", tmpValue));
//			sb.AppendLine(string.Format("{0}    {1}.Add({2});", prefix, varName, tmpValue));
//			sb.AppendLine(string.Format("{0}", prefix}}));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public string GetFullNameNoGenericParameters(Type type)
//		{
//			string className = type.IsGenericType ? type.Name.Substring(0, type.Name.IndexOf('`')) : type.Name;
//			// 处理嵌套类名字。
//			string fullName = className;
//			for (Type declaring = type.DeclaringType; declaring != null; declaring = declaring.DeclaringType)
//			{
//				fullName = declaring.Name + "." + fullName;
//			}
//			return null != type.Namespace ? type.Namespace + "." + fullName : fullName;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public string GetTypeName(Type type)
//		{
//			if (type.IsByRef)
//				type = type.GetElementType();
//
//			if (Serializer.TryGetValue(type, out var basic))
//			{
//				return basic.Item4();
//			}
//
//			if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
//			{
//				return type.FullName;
//			}
//
//			string fullName = GetFullNameNoGenericParameters(type);
//			if (false == type.IsGenericType)
//				return fullName;
//
//			Type[] parameters = type.GenericTypeArguments;
//			if (System_Collections_Generic_Serializer.TryGetValue(fullName, out var generic))
//			{
//				switch (parameters.Length)
//				{
//					case 1:
//						return generic.Item4(null, parameters[0]);
//
//					case 2:
//						return generic.Item4(parameters[0], parameters[1]);
//
//					default:
//						break; // fall down.
//				}
//			}
//			fullName += "<";
//			bool first = true;
//			foreach (var parameter in parameters)
//			{
//				if (first)
//					first = false;
//				else
//					fullName += ", ";
//				fullName += GetTypeName(parameter);
//			}
//			fullName += ">";
//			return fullName;
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public void GenLocalVariable(StringBuilder sb, string prefix, Type type, string varName)
//		{
//			if (type.IsByRef)
//				type = type.GetElementType();
//
//			if (Serializer.TryGetValue(type, out var basic))
//			{
//				basic.Item3(sb, prefix, varName);
//				return;
//			}
//
//			if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
//			{
//				sb.AppendLine(string.Format("{0}{1} {2} = new {3}();", prefix, type.FullName, varName, type.FullName));
//				return;
//			}
//
//			string typename = GetTypeName(type);
//			if (false == type.IsGenericType)
//			{
//				// decode 不需要初始化。JsonSerializer.Deserialize
//				sb.AppendLine(string.Format("{0}{1} {2} = default({3});", prefix, typename, varName, typename));
//				return;
//			}
//			Type[] parameters = type.GenericTypeArguments;
//			if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
//			{
//				switch (parameters.Length)
//				{
//					case 1:
//						generic.Item3(sb, prefix, varName, null, parameters[0]);
//						return;
//
//					case 2:
//						generic.Item3(sb, prefix, varName, parameters[0], parameters[1]);
//						return;
//
//					default:
//						break; // fall down.
//				}
//			}
//			// decode 不需要初始化。JsonSerializer.Deserialize
//			sb.AppendLine(string.Format("{0}{1} {2} = default({3});", prefix, typename, varName, typename));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public void GenEncode(StringBuilder sb, string prefix, Type type, string varName)
//		{
//			if (type.IsByRef)
//				type = type.GetElementType();
//
//			if (Serializer.TryGetValue(type, out var basic))
//			{
//				basic.Item1(sb, prefix, varName);
//				return;
//			}
//
//			if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
//			{
//				sb.AppendLine(string.Format("{0}{1}.Encode(_bb_);", prefix, varName));
//				return;
//			}
//
//			if (type.IsGenericType)
//			{
//				if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
//				{
//					Type[] parameters = type.GenericTypeArguments;
//					switch (parameters.Length)
//					{
//						case 1:
//							generic.Item1(sb, prefix, varName, null, parameters[0]);
//							return;
//
//						case 2:
//							generic.Item1(sb, prefix, varName, parameters[0], parameters[1]);
//							return;
//
//						default:
//							break; // fall down.
//					}
//				}
//				// fall down
//			}
//
//			// Utf8Json https://aloiskraus.wordpress.com/2019/09/29/net-serialization-benchmark-2019-roundup/
//			sb.AppendLine(string.Format("{0}_bb_.WriteBytes(System.Text.Json.JsonSerializer.SerializeToUtf8Bytes({1}, typeof({2})));", prefix, varName, GetTypeName(type)));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public void GenDecode(StringBuilder sb, string prefix, Type type, string varName)
//		{
//			if (type.IsByRef)
//				type = type.GetElementType();
//
//			if (Serializer.TryGetValue(type, out var p))
//			{
//				p.Item2(sb, prefix, varName);
//				return;
//			}
//
//			if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
//			{
//				var tmp0 = string.Format("tmp{0}", TmpVarNameId.IncrementAndGet());
//				sb.AppendLine(string.Format("{0}var {1} = new {2}();", prefix, tmp0, type.FullName));
//				sb.AppendLine(string.Format("{0}{1}.Decode(_bb_);", prefix, tmp0));
//				return;
//			}
//
//			if (type.IsGenericType)
//			{
//				if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
//				{
//					Type[] parameters = type.GenericTypeArguments;
//					switch (parameters.Length)
//					{
//						case 1:
//							generic.Item2(sb, prefix, varName, null, parameters[0]);
//							return;
//
//						case 2:
//							generic.Item2(sb, prefix, varName, parameters[0], parameters[1]);
//							return;
//
//						default:
//							break; // fall down.
//					}
//				}
//				// fall down
//			}
//
//			string tmp1 = "tmp" + TmpVarNameId.IncrementAndGet();
//			string tmp2 = "tmp" + TmpVarNameId.IncrementAndGet();
//			sb.AppendLine(string.Format("{0}var {1} = _bb_.ReadByteBuffer();", prefix, tmp1));
//			sb.AppendLine(string.Format("{0}var {1} = new System.ReadOnlySpan<byte>({2}.Bytes, {3}.ReadIndex, {4}.Size);", prefix, tmp2, tmp1, tmp1, tmp1));
//			sb.AppendLine(string.Format("{0}{1} = System.Text.Json.JsonSerializer.Deserialize<{2}> ({3});", prefix, varName, GetTypeName(type), tmp2));
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public void GenEncode(StringBuilder sb, string prefix, List<ParameterInfo> parameters)
//		{
//			for (int i = 0; i < parameters.Count; ++i)
//			{
//				var p = parameters[i];
//				if (p.IsOut)
//					continue;
//				if (IsDelegate(p.ParameterType))
//					continue;
//				GenEncode(sb, prefix, p.ParameterType, p.Name);
//			}
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public void GenDecode(StringBuilder sb, string prefix, List<ParameterInfo> parameters)
//		{
//			for (int i = 0; i < parameters.Count; ++i)
//			{
//				var p = parameters[i];
//				if (p.IsOut)
//					continue;
//				if (IsDelegate(p.ParameterType))
//					continue;
//				GenDecode(sb, prefix, p.ParameterType, p.Name);
//			}
//		}

//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//	public string ToDefineString(System.Reflection.ParameterInfo[] parameters)
//		{
//			StringBuilder sb = new StringBuilder();
//			bool first = true;
//			foreach (var p in parameters)
//			{
//				if (first)
//					first = false;
//				else
//					sb.Append(", ");
//				string prefix = "";
//				if (p.IsOut)
//					prefix = "out ";
//				else if (p.ParameterType.IsByRef)
//					prefix = "ref ";
//				sb.Append(prefix).Append(GetTypeName(p.ParameterType)).Append(" ").Append(p.Name);
//			}
//			return sb.ToString();
//		}
}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [System.AttributeUsage(System.AttributeTargets.Method)] public class ModuleRedirectAllAttribute : System.Attribute
//C# TO JAVA CONVERTER TODO TASK: Local functions are not converted by C# to Java Converter:
//[System.AttributeUsage(System.AttributeTargets.Method)]
//public class ModuleRedirectAllAttribute : System.Attribute
//	{
//		public string GetConcurrentLevelSource
//		{
//			get;
//		}
//
//		public ModuleRedirectAllAttribute(string source)
//		{
//			GetConcurrentLevelSource = source;
//		}
//	}
