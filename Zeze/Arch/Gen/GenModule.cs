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
        public static readonly GenModule Instance = new GenModule();

        private bool CheckAddMethod(MethodInfo method, OverrideType type, object[] attrs, List<MethodOverride> result)
        {
            if (attrs.Length == 1)
            {
                result.Add(new MethodOverride(method, type, attrs[0] as Attribute));
                return true;
            }
            return false;
        }

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            List<MethodOverride> overrides = new List<MethodOverride>();
            var methods = module.GetType().GetMethods();
            foreach (var method in methods)
            {
                if (CheckAddMethod(method, OverrideType.Redirect, method.GetCustomAttributes(typeof(RedirectAttribute), false), overrides))
                    continue;
                if (CheckAddMethod(method, OverrideType.RedirectWithHash, method.GetCustomAttributes(typeof(RedirectHashAttribute), false), overrides))
                    continue;
                if (CheckAddMethod(method, OverrideType.RedirectAll, method.GetCustomAttributes(typeof(RedirectAllHashAttribute), false), overrides))
                    continue;
                if (CheckAddMethod(method, OverrideType.RedirectToServer, method.GetCustomAttributes(typeof(RedirectToServerAttribute), false), overrides))
                    continue;
            }
            if (overrides.Count == 0)
                return module; // 没有需要重定向的方法。


            string genClassName = $"_ModuleRedirect_{module.FullName.Replace('.', '_')}_Gen_";
            if (null == SrcDirWhenPostBuild)
            {
                module.UnRegister();
                //Console.WriteLine($"'{module.FullName}' Replaced.");
                // from Game.App.Start. try load new module instance.
                var newModule = (Zeze.IModule)Activator.CreateInstance(Type.GetType(genClassName));
                /*TODO newModule.Initialize(Game.App.Instance);*/
                return newModule;
            }

            string srcFileName = System.IO.Path.Combine(SrcDirWhenPostBuild,
                module.FullName.Replace('.', System.IO.Path.DirectorySeparatorChar), $"Module{module.Name}.cs");

            long srcLastWriteTimeTicks = System.IO.File.GetLastWriteTime(srcFileName).Ticks;
            string genFileName = System.IO.Path.Combine(SrcDirWhenPostBuild, "Gen", genClassName + ".cs");

            if (false == System.IO.File.Exists(genFileName)
                || System.IO.File.GetLastWriteTime(genFileName).Ticks != srcLastWriteTimeTicks)
            {
                Console.WriteLine("ModuleRedirect '" + module.FullName + "' Gen Now ...");
                HasNewGen = true;
                string code = GenModuleCode(module, genClassName, overrides);
                //*
                //System.IO.File.Delete(genFileName); // 如果被vs占用，删除也没用。
                System.IO.StreamWriter sw = new System.IO.StreamWriter(genFileName, false, Encoding.UTF8);
                sw.Write(code);
                sw.Close();
                System.IO.File.SetLastWriteTime(genFileName, new DateTime(srcLastWriteTimeTicks));
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

        public string SrcDirWhenPostBuild { get; set; } // ugly
        public bool HasNewGen { get; private set; } = false;

        private string GenModuleCode(Zeze.IModule module, string genClassName, List<MethodOverride> overrides)
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine($"public class {genClassName} : {module.FullName}.Module{module.Name}");
            sb.AppendLine($"{{");

            // TaskCompletionSource<long> void
            StringBuilder sbHandles = new StringBuilder();
            StringBuilder sbContexts = new StringBuilder();
            foreach (var methodOverride in overrides)
            {
                methodOverride.PrepareParameters();
                var parametersDefine = Gen.Instance.ToDefineString(methodOverride.ParametersAll);
                var methodNameWithHash = GetMethodNameWithHash(methodOverride.Method.Name);
                var (returnType, returnTypeName) = GetReturnType(methodOverride.Method.ReturnParameter.ParameterType);
                var actions = GenAction.GetActions(methodOverride.ParametersNormal);
                Verify(methodOverride, actions);

                sb.AppendLine($"    public override {returnTypeName} {methodOverride.Method.Name}({parametersDefine})");
                sb.AppendLine($"    {{");
                sb.AppendLine($"        if (Zezex.ModuleRedirect.Instance.IsLocalServer(\"{module.FullName}\"))");
                sb.AppendLine($"        {{");
                switch (returnType)
                {
                    case ReturnType.Void:
                        sb.AppendLine($"            base.{methodOverride.Method.Name}({methodOverride.GetBaseCallString()});");
                        sb.AppendLine($"            return;");
                        break;
                    case ReturnType.TaskCompletionSource:
                        sb.AppendLine($"            return base.{methodOverride.Method.Name}({methodOverride.GetBaseCallString()});");
                        break;
                }
                sb.AppendLine($"        }}");
                sb.AppendLine($"");

                if (methodOverride.OverrideType == OverrideType.RedirectAll)
                {
                    GenRedirectAllContext(sbContexts, methodOverride, actions);
                    GenRedirectAll(sb, sbHandles, module, methodOverride, actions);
                    continue;
                }
                string rpcVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"        var {rpcVarName} = new Zezex.Provider.ModuleRedirect();");
                sb.AppendLine($"        {rpcVarName}.Argument.ModuleId = {module.Id};");
                sb.AppendLine($"        {rpcVarName}.Argument.RedirectType = {methodOverride.GetRedirectType()};");
                sb.AppendLine($"        {rpcVarName}.Argument.HashCode = {methodOverride.GetChoiceHashOrServerCodeSource()};");
                sb.AppendLine($"        {rpcVarName}.Argument.MethodFullName = \"{module.FullName}:{methodOverride.Method.Name}\";");
                sb.AppendLine($"        {rpcVarName}.Argument.ServiceNamePrefix = Game.App.ServerServiceNamePrefix;");
                if (methodOverride.ParametersNormal.Count > 0)
                {
                    // normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
                    sb.AppendLine($"        {{");
                    sb.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
                    Gen.Instance.GenEncode(sb, "            ", methodOverride.ParametersNormal);
                    sb.AppendLine($"            {rpcVarName}.Argument.Params = new Zeze.Net.Binary(_bb_);");
                    sb.AppendLine($"        }}");
                }
                sb.AppendLine($"");
                string futureVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"        var {futureVarName} = new System.Threading.Tasks.TaskCompletionSource<long>();");
                sb.AppendLine($"");
                sb.AppendLine($"        {rpcVarName}.Send(Zezex.ModuleRedirect.RandomLink(), (_) =>");
                sb.AppendLine($"        {{");
                sb.AppendLine($"            if ({rpcVarName}.IsTimeout)");
                sb.AppendLine($"            {{");
                sb.AppendLine($"                {futureVarName}.SetException(new System.Exception(\"{module.FullName}:{methodOverride.Method.Name} Rpc Timeout.\"));");
                sb.AppendLine($"            }}");
                sb.AppendLine($"            else if (Zezex.Provider.ModuleRedirect.ResultCodeSuccess != {rpcVarName}.ResultCode)");
                sb.AppendLine($"            {{");
                sb.AppendLine($"                {futureVarName}.SetException(new System.Exception($\"{module.FullName}:{methodOverride.Method.Name} Rpc Error {{{rpcVarName}.ResultCode}}.\"));");
                sb.AppendLine($"            }}");
                sb.AppendLine($"            else");
                sb.AppendLine($"            {{");
                if (actions.Count > 0)
                {
                    foreach (var action in actions)
                    {
                        action.GenActionDecode(sb, "                ");
                    }
                    var actionVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                    sb.AppendLine($"                foreach (var {actionVarName} in {rpcVarName}.Result.Actions)");
                    sb.AppendLine($"                {{");
                    sb.AppendLine($"                    switch ({actionVarName}.Name)");
                    sb.AppendLine($"                    {{");
                    foreach (var action in actions)
                    {
                        sb.AppendLine($"                        case \"{action.VarName}\": _{action.VarName}_({actionVarName}.Params); break;");
                    }
                    sb.AppendLine($"                    }}");
                    sb.AppendLine($"                }}");
                }
                sb.AppendLine($"                {futureVarName}.SetResult({rpcVarName}.Result.ReturnCode);");
                sb.AppendLine($"            }}");
                sb.AppendLine($"            return Zeze.Transaction.Procedure.Success;");
                sb.AppendLine($"        }});");
                sb.AppendLine($"");
                if (returnType == ReturnType.TaskCompletionSource)
                {
                    sb.AppendLine($"        return {futureVarName};");
                }
                sb.AppendLine($"    }}");
                sb.AppendLine($"");

                sbHandles.AppendLine($"        Zezex.ModuleRedirect.Instance.Handles.Add(\"{module.FullName}:{methodOverride.Method.Name}\", (long _sessionid_, int _hash_, Zeze.Net.Binary _params_, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_) =>");
                sbHandles.AppendLine($"        {{");
                sbHandles.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
                for (int i = 0; i < methodOverride.ParametersNormal.Count; ++i)
                {
                    var p = methodOverride.ParametersNormal[i];
                    if (Gen.IsDelegate(p.ParameterType))
                        continue; // define later.
                    Gen.Instance.GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
                }
                Gen.Instance.GenDecode(sbHandles, "            ", methodOverride.ParametersNormal);

                if (actions.Count > 0)
                {
                    foreach (var action in actions)
                    {
                        action.GenActionEncode(sbHandles, "            ");
                    }
                }
                string normalcall = methodOverride.GetNarmalCallString();
                string sep = string.IsNullOrEmpty(normalcall) ? "" : ", ";
                var returnCodeVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                var returnParamsVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sbHandles.AppendLine($"            var {returnCodeVarName} = base.{methodNameWithHash}(_hash_{sep}{normalcall});");
                sbHandles.AppendLine($"            var {returnParamsVarName} = Zeze.Net.Binary.Empty;");
                sbHandles.AppendLine($"            return ({returnCodeVarName},{returnParamsVarName});");
                sbHandles.AppendLine($"        }});");
                sbHandles.AppendLine($"");
            }
            sb.AppendLine($"    public {genClassName}() : base(Game.App.Instance)");
            sb.AppendLine($"    {{");
            sb.Append(sbHandles.ToString());
            sb.AppendLine($"    }}");
            sb.AppendLine($"");
            sb.Append(sbContexts.ToString());
            sb.AppendLine($"}}");
            return sb.ToString();
        }

        void GenRedirectAll(StringBuilder sb, StringBuilder sbHandles, Zeze.IModule module, MethodOverride methodOverride, List<GenAction> actions)
        {
            string reqVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"        var {reqVarName} = new Zezex.Provider.ModuleRedirectAllRequest();");
            sb.AppendLine($"        {reqVarName}.Argument.ModuleId = {module.Id};");
            sb.AppendLine($"        {reqVarName}.Argument.HashCodeConcurrentLevel = {methodOverride.GetConcurrentLevelSource()};");
            sb.AppendLine($"        // {reqVarName}.Argument.HashCodes = // setup in linkd;");
            sb.AppendLine($"        {reqVarName}.Argument.MethodFullName = \"{module.FullName}:{methodOverride.Method.Name}\";");
            sb.AppendLine($"        {reqVarName}.Argument.ServiceNamePrefix = Game.App.ServerServiceNamePrefix;");

            int actionCountSkipOnHashEnd = GetActionCountSkipOnHashEnd(actions);
            string initOnHashEnd = "";
            bool first = true;
            StringBuilder actionVarNames = new StringBuilder();
            foreach (var action in actions)
            {
                if (action.IsOnHashEnd)
                {
                    initOnHashEnd = $"{{ OnHashEnd = {action.VarName} }}";
                    continue;
                }

                if (first)
                    first = false;
                else
                    actionVarNames.Append(", ");
                actionVarNames.Append($"{action.VarName}");
            }
            string contextVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"        var {contextVarName} = new Context{methodOverride.Method.Name}({reqVarName}.Argument.HashCodeConcurrentLevel, {reqVarName}.Argument.MethodFullName, {actionVarNames}){initOnHashEnd};");
            sb.AppendLine($"        {reqVarName}.Argument.SessionId = App.Server.AddManualContextWithTimeout({contextVarName});");
            if (methodOverride.ParametersNormal.Count > 0)
            {
                // normal 包括了 out 参数，这个不需要 encode，所以下面可能仍然是空的，先这样了。
                sb.AppendLine($"        {{");
                sb.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
                Gen.Instance.GenEncode(sb, "            ", methodOverride.ParametersNormal);
                sb.AppendLine($"            {reqVarName}.Argument.Params = new Zeze.Net.Binary(_bb_);");
                sb.AppendLine($"        }}");
            }
            sb.AppendLine($"");
            sb.AppendLine($"        {reqVarName}.Send(Zezex.ModuleRedirect.RandomLink());");
            sb.AppendLine($"    }}");
            sb.AppendLine($"");

            // handles
            sbHandles.AppendLine($"        Zezex.ModuleRedirect.Instance.Handles.Add(\"{module.FullName}:{methodOverride.Method.Name}\", (long _sessionid_, int _hash_, Zeze.Net.Binary _params_, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_) =>");
            sbHandles.AppendLine($"        {{");
            sbHandles.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
            for (int i = 0; i < methodOverride.ParametersNormal.Count; ++i)
            {
                var p = methodOverride.ParametersNormal[i];
                if (Gen.IsDelegate(p.ParameterType))
                    continue; // define later.
                Gen.Instance.GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
            }
            Gen.Instance.GenDecode(sbHandles, "            ", methodOverride.ParametersNormal);

            if (actionCountSkipOnHashEnd > 0)
            {
                foreach (var action in actions)
                {
                    if (action.IsOnHashEnd)
                        continue;
                    action.GenActionEncode(sbHandles, "            ");
                }
            }
            string normalcall = methodOverride.GetNarmalCallString((pInfo) => Gen.IsOnHashEnd(pInfo));
            string sep = string.IsNullOrEmpty(normalcall) ? "" : ", ";
            var returnCodeVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            sbHandles.AppendLine($"            var {returnCodeVarName} = base.{GetMethodNameWithHash(methodOverride.Method.Name)}(_sessionid_, _hash_{sep}{normalcall});");
            sbHandles.AppendLine($"            return ({returnCodeVarName}, Zeze.Net.Binary.Empty);");
            sbHandles.AppendLine($"        }});");
            sbHandles.AppendLine($"");
        }

        private (ReturnType, string) GetReturnType(Type type)
        {
            if (type == typeof(void))
                return (ReturnType.Void, "void");
            if (type == typeof(TaskCompletionSource<long>))
                return (ReturnType.TaskCompletionSource, "System.Threading.Tasks.TaskCompletionSource<long>");
            throw new Exception("ReturnType Must Be void Or TaskCompletionSource<long>");
        }

        private string GetMethodNameWithHash(string name)
        {
            if (!name.StartsWith("Run"))
                throw new Exception("Method Name Need StartsWith 'Run'.");
            return name.Substring(3);
        }

        private void Verify(MethodOverride method, List<GenAction> actions)
        {
            switch (method.OverrideType)
            {
                case OverrideType.RedirectAll:
                    if (method.Method.ReturnType != typeof(void))
                        throw new Exception("RedirectAll ReturnType Must Be void");

                    // 如果每个hash分组处理需要多个回调，把这个检查去掉。
                    if (actions.Count > 2)
                        throw new Exception("RedirectAll (actions.Count > 2)");

                    int end = 0;
                    foreach (var action in actions)
                    {
                        if (action.IsOnHashEnd)
                        {
                            end++;
                            continue;
                        }

                        if (action.GenericArguments.Length < 3)
                            throw new Exception("RedirectAll callback must have parameters: long,int");
                        if (action.GenericArguments[0] != typeof(long))
                            throw new Exception("RedirectAll callback first parameter muse be long(sessionId)");
                        if (action.GenericArguments[1] != typeof(int))
                            throw new Exception("RedirectAll callback second parameter muse be int(hash-index)");
                        if (action.GenericArguments[2] != typeof(long))
                            throw new Exception("RedirectAll callback thrid parameter muse be int(return-code)");
                    }
                    if (end > 1)
                        throw new Exception("RedirectAll Too Many OnHashEnd.");
                    break;
            }
        }

        private int GetActionCountSkipOnHashEnd(List<GenAction> actions)
        {
            int count = 0;
            foreach (var action in actions)
            {
                if (action.IsOnHashEnd)
                    continue;
                ++count;
            }
            return count;
        }

        void GenRedirectAllContext(StringBuilder sb, MethodOverride methodOverride, List<GenAction> actions)
        {
            sb.AppendLine($"    public class Context{methodOverride.Method.Name} : Zezex.Provider.ModuleProvider.ModuleRedirectAllContext");
            sb.AppendLine($"    {{");
            foreach (var action in actions)
            {
                if (action.IsOnHashEnd)
                    continue;
                sb.AppendLine($"        private System.Action{action.GetGenericArgumentsDefine()} {action.VarName};");
            }
            sb.AppendLine($"");
            StringBuilder actionVarNames = new StringBuilder();
            bool first = true;
            foreach (var action in actions)
            {
                if (action.IsOnHashEnd)
                    continue;

                if (first)
                    first = false;
                else
                    actionVarNames.Append(", ");
                actionVarNames.Append($"System.Action{action.GetGenericArgumentsDefine()} {action.VarName}");
            }
            sb.AppendLine($"        public Context{methodOverride.Method.Name}(int _c_, string _n_, {actionVarNames}) : base(_c_, _n_)");
            sb.AppendLine($"        {{");
            int actionCountSkipOnHashEnd = GetActionCountSkipOnHashEnd(actions);
            foreach (var action in actions)
            {
                if (action.IsOnHashEnd)
                    continue;
                sb.AppendLine($"            this.{action.VarName} = {action.VarName};");
            }
            sb.AppendLine($"        }}");
            sb.AppendLine($"");
            sb.AppendLine($"        public override long ProcessHashResult(int _hash_, long _returnCode_, Zeze.Net.Binary _params, System.Collections.Generic.IList<Zezex.Provider.BActionParam> _actions_)");
            sb.AppendLine($"        {{");
            if (actionCountSkipOnHashEnd > 0)
            {
                foreach (var action in actions)
                {
                    if (action.IsOnHashEnd)
                        continue;
                    action.GenActionDecode(sb, "            ", "base.SessionId, _hash_, _returnCode_", 3);
                }
                var actionVarName = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"            foreach (var {actionVarName} in _actions_)");
                sb.AppendLine($"            {{");
                sb.AppendLine($"                switch ({actionVarName}.Name)");
                sb.AppendLine($"                {{");
                foreach (var action in actions)
                {
                    if (action.IsOnHashEnd)
                        continue;
                    sb.AppendLine($"                    case \"{action.VarName}\": _{action.VarName}_({actionVarName}.Params); break;");
                }
                sb.AppendLine($"                }}");
                sb.AppendLine($"            }}");
            }
            sb.AppendLine($"            return Zeze.Transaction.Procedure.Success;");
            sb.AppendLine($"        }}");
            sb.AppendLine($"    }}");
            sb.AppendLine($"");
        }

    }
}
