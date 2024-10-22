using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch.Gen
{
    public class GenModule
    {
        /// <summary>
        /// 0) long [in] sessionid
        /// 1) int [in] hash
        /// 2) Zeze.Net.Binary [in] encoded parameters
        /// 3) List<Zezex.Provider.BActionParam> [result] result for callback. avoid copy.
        /// 4) (int ReturnCode, Zeze.Net.Binary encoded-parameters) [return]
        ///     Func不能使用ref，而Zeze.Net.Binary是只读的。就这样吧。
        /// </summary>
        /*
        public Dictionary<string,
            Func<long, int, Zeze.Net.Binary, IList<Zezex.Provider.BActionParam>,
            (long, Zeze.Net.Binary)>
            > Handles { get; }
            = new Dictionary<string, Func<
                long, int, Zeze.Net.Binary,
                IList<Zezex.Provider.BActionParam>,
                (long, Zeze.Net.Binary)>>();
        */
        public static readonly GenModule Instance = new();

        private static bool CheckAddMethod(MethodInfo method, OverrideType type, object[] attrs, List<MethodOverride> result)
        {
            if (attrs.Length == 1)
            {
                result.Add(new MethodOverride(method, type, attrs[0] as Attribute));
                return true;
            }
            return false;
        }

        public static M CreateRedirectModule<M, T>(T userApp, M module, string genClassName)
            where T : AppBase
            where M : IModule
        {
            foreach (var assembly in AppDomain.CurrentDomain.GetAssemblies())
            {
                Type replaceModuleType = null;
                try
                {
                    replaceModuleType = assembly.GetType(genClassName);
                }
                catch (Exception)
                {
                    continue;
                }
                if (null != replaceModuleType)
                    return (M)Activator.CreateInstance(replaceModuleType, userApp);
            }
            throw new Exception($"RedirectOverride Not Found: {genClassName}");
        }

        public static M CreateRedirectModule<M, T>(T userApp, M module)
            where T : AppBase
            where M : IModule
        {
            var moduleName = module.FullName.Replace('.', '_');
            var genClassName = $"Redirect_{moduleName}";
            return CreateRedirectModule(userApp, module, genClassName);
        }

        public M ReplaceModuleInstance<T, M>(T userApp, M module)
            where T : AppBase
            where M : IModule
        {
            lock (this)
            {
                var overrides = new List<MethodOverride>();
                var methods = module.GetType().GetMethods(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);
                foreach (var method in methods)
                {
                    if (CheckAddMethod(method, OverrideType.RedirectHash, method.GetCustomAttributes(typeof(RedirectHashAttribute), false), overrides))
                        continue;
                    if (CheckAddMethod(method, OverrideType.RedirectAll, method.GetCustomAttributes(typeof(RedirectAllAttribute), false), overrides))
                        continue;
                    if (CheckAddMethod(method, OverrideType.RedirectToServer, method.GetCustomAttributes(typeof(RedirectToServerAttribute), false), overrides))
                        continue;
                }
                if (overrides.Count == 0)
                    return module; // 没有需要重定向的方法。

                overrides.Sort((a, b) => a.Method.Name.CompareTo(b.Method.Name));

                var genClassName = $"Redirect_{module.FullName.Replace('.', '_')}";
                if (null == GenRedirect)
                {
                    //Console.WriteLine($"'{module.FullName}' Replaced.");
                    // from Game.App.Start. try load new module instance.
                    return CreateRedirectModule(userApp, module, genClassName);
                }

                var srcFileName = System.IO.Path.Combine(GenRedirect,
                    module.FullName.Replace('.', System.IO.Path.DirectorySeparatorChar), $"Module{module.Name}.cs");

                var srcLastWriteTimeTicks = System.IO.File.GetLastWriteTime(srcFileName).Ticks;
                var genFileName = System.IO.Path.Combine(GenRedirect, genClassName + ".cs");

                if (false == System.IO.File.Exists(genFileName)
                    || System.IO.File.GetLastWriteTime(genFileName).Ticks != srcLastWriteTimeTicks)
                {
                    Console.WriteLine("ModuleRedirect '" + module.FullName + "' Gen Now ...");
                    HasNewGen = true;
                    var userAppName = module.IsBuiltin ? "Zeze.AppBase" : userApp.GetType().FullName;
                    string code = GenModuleCode(module, genClassName, overrides, userAppName);
                    //*
                    //System.IO.File.Delete(genFileName); // 如果被vs占用，删除也没用。
                    var sw = new System.IO.StreamWriter(genFileName, false, Encoding.UTF8);
                    sw.Write(code);
                    sw.Close();
                    System.IO.File.SetLastWriteTime(genFileName, new DateTime(srcLastWriteTimeTicks));
                    /*/
                    // .net core, .net 5.0+ 不支持编译。 
                    return CompileCode(code, genClassName);
                    //*/
                }
                return module;
            }
        }

        /*
        private Zeze.IModule CompileCode<T>(T userApp, string code, string genClassName)
            where T : AppBase
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
            return (Zeze.IModule)Activator.CreateInstance(type, userApp);
        }
        // */

        public string GenRedirect { get; set; } // ugly
        public bool HasNewGen { get; private set; } = false;

        // 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
        static void ChoiceTargetRunLoopback(IModule module, StringBuilder sb, MethodOverride m)
        {
            switch (m.OverrideType)
            {
                case OverrideType.RedirectHash:
                    sb.AppendLine($"        // RedirectHash");
                    sb.AppendLine($"        var _target_ = App.Zeze.Redirect.ChoiceHash(this, {m.ParameterHashOrServer.Name}, {m.GetConcurrentLevelSource()});");
                    break;
                case OverrideType.RedirectToServer:
                    sb.AppendLine($"        // RedirectToServer");
                    sb.AppendLine($"        var _target_ = App.Zeze.Redirect.ChoiceServer(this, {m.ParameterHashOrServer.Name});");
                    break;
                case OverrideType.RedirectAll:
                    sb.AppendLine("        // RedirectAll");
                    // RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。
                    return;
            }

            sb.AppendLine("        if (_target_ == null) {");
            sb.AppendLine("            // local: loop-back");
            var callstr = $"base.{m.Method.Name}({m.GetBaseCallString()})";
            switch (m.TransactionLevel)
            {
                case Zeze.Transaction.TransactionLevel.Serializable:
                case Zeze.Transaction.TransactionLevel.AllowDirtyWhenAllRead:
                    var procName = $"\"{module.FullName}:{m.Method.Name}\"";
                    if (m.MethodMode.IsAsync)
                    {
                        if (m.MethodMode.HasResult)
                        {
                            var returnResult = "returnResult" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                            sb.AppendLine($"            var {returnResult} = default({Gen.Instance.GetTypeName(m.MethodMode.ResultType)});");
                            sb.AppendLine($"            await App.Zeze.NewProcedure(async () => {{ {returnResult} = await {callstr}; return 0; }}, {procName}).ExecuteAsync();");
                            sb.AppendLine($"            return {returnResult};");
                        }
                        else
                        {
                            sb.AppendLine($"            await App.Zeze.NewProcedure(async () => {{ await {callstr}; return 0; }}, {procName}).ExecuteAsync();");
                            sb.AppendLine($"            return;");
                        }
                    }
                    else
                    {
                        sb.AppendLine($"            App.Zeze.NewProcedure(() => {{ {callstr}; return System.Threading.Tasks.Task.FromResult(0L); }}, {procName}).Execute();");
                        sb.AppendLine($"            return;");
                    }
                    break;

                default:
                    if (m.MethodMode.IsAsync)
                    {
                        if (m.MethodMode.HasResult)
                        {
                            sb.AppendLine($"            return await App.Zeze.Redirect.RunResultAsync(async () => await {callstr});");
                        }
                        else
                        {
                            sb.AppendLine($"            await App.Zeze.Redirect.RunAsync(async () => await {callstr});");
                            sb.AppendLine($"            return;");
                        }
                    }
                    else
                    {
                        sb.AppendLine($"            App.Zeze.Redirect.RunVoid(() => {callstr});");
                        sb.AppendLine($"            return;");
                    }
                    break;
            }
            sb.AppendLine("        }");
            sb.AppendLine();
        }

        private static string GenModuleCode(Zeze.IModule module, string genClassName, List<MethodOverride> overrides, string userAppName)
        {
            var sb = new StringBuilder();
            var clsName = module.IsBuiltin ? "" : $".Module{module.Name}";
            sb.AppendLine($"public class {genClassName} : {module.FullName}{clsName}");
            sb.AppendLine($"{{");

            // TaskCompletionSource<long> void
            var sbHandles = new StringBuilder();
            var sbContexts = new StringBuilder();
            foreach (var m in overrides)
            {
                m.PrepareParameters();
                var pdefine = Gen.Instance.ToDefineString(m.ParametersAll);
                m.ResultHandle?.Verify(m);
                var publicOrProtected = m.Method.IsPublic ? "public" : "protected";
                sb.AppendLine($"    {publicOrProtected} override {m.MethodMode.GetCallReturnName()} {m.Method.Name}({pdefine})");
                sb.AppendLine($"    {{");

                ChoiceTargetRunLoopback(module, sb, m);

                if (m.OverrideType == OverrideType.RedirectAll)
                {
                    GenRedirectAll(sb, sbHandles, module, m);
                    continue;
                }

                string rpcVarName = "rpc" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"        var {rpcVarName} = new Zeze.Builtin.ProviderDirect.ModuleRedirect();");
                sb.AppendLine($"        {rpcVarName}.Argument.ModuleId = {module.Id};");
                sb.AppendLine($"        {rpcVarName}.Argument.RedirectType = {m.GetRedirectType()};");
                sb.AppendLine($"        {rpcVarName}.Argument.HashCode = {m.GetChoiceHashOrServerCodeSource()};");
                sb.AppendLine($"        {rpcVarName}.Argument.MethodFullName = \"{module.FullName}:{m.Method.Name}\";");
                sb.AppendLine($"        {rpcVarName}.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;");
                if (m.ParametersNormal.Count > 0)
                {
                    // normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
                    sb.AppendLine($"        {{");
                    sb.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
                    Gen.Instance.GenEncode(sb, "            ", m.ParametersNormal);
                    sb.AppendLine($"            {rpcVarName}.Argument.Params = new Zeze.Net.Binary(_bb_);");
                    sb.AppendLine($"        }}");
                }
                sb.AppendLine($"");

                string futureVarName = "future" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"        var {futureVarName} = new System.Threading.Tasks.TaskCompletionSource{m.MethodMode.GetFutureDefine()}();");
                sb.AppendLine($"");
                var asyncNeed = null != m.ResultHandle
                    && (m.TransactionLevel == Transaction.TransactionLevel.Serializable
                    || m.TransactionLevel == Transaction.TransactionLevel.AllowDirtyWhenAllRead);
                var asyncWord = asyncNeed ? "async" : "";
                sb.AppendLine($"        {rpcVarName}.Send(_target_, {asyncWord}(_) =>");
                sb.AppendLine($"        {{");
                sb.AppendLine($"            if ({rpcVarName}.IsTimeout)");
                sb.AppendLine($"            {{");
                sb.AppendLine($"                {futureVarName}.TrySetException(new System.Exception(\"{module.FullName}:{m.Method.Name} Rpc Timeout.\"));");
                sb.AppendLine($"            }}");
                sb.AppendLine($"            else if (0 != {rpcVarName}.ResultCode)");
                sb.AppendLine($"            {{");
                sb.AppendLine($"                {futureVarName}.TrySetException(new System.Exception($\"{module.FullName}:{m.Method.Name} Rpc Error {{{rpcVarName}.ResultCode}}.\"));");
                sb.AppendLine($"            }}");
                sb.AppendLine($"            else");
                sb.AppendLine($"            {{");
                sb.AppendLine($"                var _bb_ = Zeze.Serialize.ByteBuffer.Wrap({rpcVarName}.Result.Params);");
                if (null != m.ResultHandle)
                {
                    // decode and run if has result
                    m.ResultHandle.GenDecodeAndCallback("                ", sb, m);
                }
                m.MethodMode.GenFutureDecodeAndSet("                ", sb, futureVarName);
                sb.AppendLine($"            }}");
                sb.AppendLine($"            return {asyncSuccessResult(asyncNeed)};");
                sb.AppendLine($"        }});");
                sb.AppendLine($"");
                if (m.MethodMode.IsAsync)
                {
                    if (m.MethodMode.HasResult)
                    {
                        sb.AppendLine($"        return await {futureVarName}.Task;");
                    }
                    else
                    {
                        sb.AppendLine($"        await {futureVarName}.Task;");
                    }
                }
                sb.AppendLine($"    }}");
                sb.AppendLine($"");

                // Handles
                var hName = "hName" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sbHandles.AppendLine($"        var {hName} = new Zeze.Arch.RedirectHandle();");
                sbHandles.AppendLine($"        {hName}.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.{m.TransactionLevel};");
                sbHandles.AppendLine($"        {hName}.RequestHandle = {(m.MethodMode.IsAsync ? "async " : "")}(_sessionId_, _HashOrServerId_, _params_) =>");
                sbHandles.AppendLine($"        {{");
                sbHandles.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
                for (int i = 0; i < m.ParametersNormal.Count; ++i)
                {
                    var p = m.ParametersNormal[i];
                    if (Gen.IsDelegate(p.ParameterType))
                        continue; // define later.
                    Gen.Instance.GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
                }
                Gen.Instance.GenDecode(sbHandles, "            ", m.ParametersNormal);

                sbHandles.AppendLine($"            // WARNING reuse var _bb_ to encode result.");
                sbHandles.AppendLine($"            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);");
                if (null != m.ResultHandle)
                {
                    var resultVarNames = new List<string>();
                    for (int i = 0; i < m.ResultHandle.GenericArguments.Length; ++i)
                        resultVarNames.Add("tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet());
                    var actionName = Gen.Instance.GetTypeName(m.ResultHandle.Parameter.ParameterType);
                    var actionCall = m.ResultHandle.GetCallString(resultVarNames);
                    sbHandles.AppendLine($"            {actionName} {m.ResultHandle.Parameter.Name} = ({actionCall}) =>");
                    sbHandles.AppendLine($"            {{");
                    m.ResultHandle.GenEncode(resultVarNames, "                ", sbHandles, m);
                    sbHandles.AppendLine($"            }};");
                }
                if (m.MethodMode.IsAsync) 
                {
                    // 是否有结果都在这个分支处理。
                    var bcall = m.GetNormalCallString();
                    m.MethodMode.GenCallAndEncode("            ", sbHandles, $"base.{m.Method.Name}(_HashOrServerId_, {bcall})");
                    sbHandles.AppendLine($"            return new Zeze.Net.Binary(_bb_);");
                }
                else
                {
                    var bcall = m.GetNormalCallString();
                    var sep = bcall.Length == 0 ? "" : ", "; 
                    sbHandles.AppendLine($"            base.{m.Method.Name}(_HashOrServerId_{sep}{bcall});");
                    sbHandles.AppendLine($"            return System.Threading.Tasks.Task.FromResult(Zeze.Net.Binary.Empty);");
                }
                sbHandles.AppendLine($"        }};");
                sbHandles.AppendLine($"        App.Zeze.Redirect.Handles.TryAdd(\"{module.FullName}:{m.Method.Name}\", {hName});");
            }

            sb.AppendLine($"    public {genClassName}({userAppName} app) : base(app)");
            sb.AppendLine($"    {{");
            sb.Append(sbHandles);
            sb.AppendLine($"    }}");
            sb.AppendLine($"");
            sb.Append(sbContexts);
            sb.AppendLine($"}}");
            return sb.ToString();
        }

        static string asyncSuccessResult(bool asyncNeed)
        {
            return asyncNeed ? "Zeze.Util.ResultCode.Success" : "System.Threading.Tasks.Task.FromResult(Zeze.Util.ResultCode.Success)"; 
        }

        static void GenRedirectAll(StringBuilder sb, StringBuilder sbHandles, Zeze.IModule module, MethodOverride m)
        {
            var req = "reqall" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"        var {req} = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();");
            sb.AppendLine($"        {req}.Argument.ModuleId = {module.Id};");
            sb.AppendLine($"        {req}.Argument.HashCodeConcurrentLevel = {m.GetConcurrentLevelSource()};");
            sb.AppendLine($"        // {req}.Argument.HashCodes = // setup in linkd;");
            sb.AppendLine($"        {req}.Argument.MethodFullName = \"{module.FullName}:{m.Method.Name}\";");
            sb.AppendLine($"        {req}.Argument.ServiceNamePrefix = App.Zeze.Redirect.ProviderApp.ServerServiceNamePrefix;");
            if (m.ParametersNormal.Count > 0)
            {
                sb.AppendLine($"        {{");
                sb.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
                Gen.Instance.GenEncode(sb, "            ", m.ParametersNormal);
                sb.AppendLine($"            {req}.Argument.Params = new Zeze.Net.Binary(_bb_);");
                sb.AppendLine($"        }}");
            }

            var decoderVar = "decoder" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            var resultType = m.MethodMode.GetRedirectAllGenericParameter();
            if (null != resultType)
            {
                sb.AppendLine($"        var {decoderVar} = (Zeze.Net.Binary _params_) => ");
                sb.AppendLine($"        {{");
                var resultVar = "result" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
                Gen.Instance.GenLocalVariable(sb, "            ", resultType, resultVar);
                Gen.Instance.GenDecode(sb, "            ", resultType, resultVar);
                sb.AppendLine($"            return {resultVar};");
                sb.AppendLine($"        }};");
            }
            else
            {
                sb.AppendLine($"        var {decoderVar} = Zeze.Arch.Gen.VoidResult.Decoder;");
            }
            var processing = m.ResultHandle != null ? m.ResultHandle.Parameter.Name : "";
            var contextVarName = "ctxall" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"        var {contextVarName} = new {Gen.Instance.GetTypeName(m.MethodMode.ResultType)}(");
            sb.AppendLine($"            {req}.Argument.HashCodeConcurrentLevel, {req}.Argument.MethodFullName,");
            sb.AppendLine($"            {decoderVar}{(processing.Length > 0 ? ", " : "")}{processing});");
            sb.AppendLine($"        {req}.Argument.SessionId = App.Zeze.Redirect.ProviderApp.ProviderDirectService.AddManualContextWithTimeout({contextVarName});");
            sb.AppendLine($"");
            sb.AppendLine($"        App.Zeze.Redirect.RedirectAll(this, {req});");
            sb.AppendLine($"");
            sb.AppendLine($"        await {contextVarName}.Future.Task;");
            sb.AppendLine($"        return {contextVarName};");
            sb.AppendLine($"    }}");
            sb.AppendLine($"");

            // handles
            var hName = "hName" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            sbHandles.AppendLine($"        var {hName} = new Zeze.Arch.RedirectHandle();");
            sbHandles.AppendLine($"        {hName}.RequestTransactionLevel = Zeze.Transaction.TransactionLevel.{m.TransactionLevel};");
            sbHandles.AppendLine($"        {hName}.RequestHandle = async (_sessionId_, _hash_, _params_) =>");
            sbHandles.AppendLine($"        {{");
            sbHandles.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
            for (int i = 0; i < m.ParametersNormal.Count; ++i)
            {
                var p = m.ParametersNormal[i];
                if (Gen.IsDelegate(p.ParameterType))
                    continue; // define later.
                Gen.Instance.GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
            }
            Gen.Instance.GenDecode(sbHandles, "            ", m.ParametersNormal);
            sbHandles.AppendLine($"            // WARNING reuse var _bb_ to encode result.");
            sbHandles.AppendLine($"            _bb_ = Zeze.Serialize.ByteBuffer.Allocate(1024);");
            if (m.MethodMode.IsAsync)
            {
                // 是否有结果都在这个分支处理。
                var bcall = m.GetNormalCallString((p) => m.ResultHandle != null && p == m.ResultHandle.Parameter);
                m.MethodMode.GenCallAndEncodeRedirectAll("            ", sbHandles, $"base.{m.Method.Name}(_hash_, {bcall})");
                sbHandles.AppendLine($"            return new Zeze.Net.Binary(_bb_);");
            }
            else
            {
                var bcall = m.GetNormalCallString((p) => m.ResultHandle != null && p == m.ResultHandle.Parameter);
                var sep = bcall.Length == 0 ? "" : ", ";
                sbHandles.AppendLine($"            base.{m.Method.Name}(_hash_{sep}{bcall});");
                sbHandles.AppendLine($"            return Zeze.Net.Binary.Empty;");
            }
            sbHandles.AppendLine($"        }};");

            sbHandles.AppendLine($"        App.Zeze.Redirect.Handles.TryAdd(\"{module.FullName}:{m.Method.Name}\", {hName});");
            sbHandles.AppendLine($"");
        }
    }
}
