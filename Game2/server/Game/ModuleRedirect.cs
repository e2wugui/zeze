using System;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Game
{
    /// <summary>
    /// 把模块的方法调用发送到其他服务器实例上执行。
    /// 被重定向的方法用注解标明。
    /// 被重定向的方法需要是virtual的。
    /// 实现方案：
    /// Game.App创建Module的时候调用回调。
    /// 在回调中判断是否存在需要拦截的方法。
    /// 如果需要就动态生成子类实现代码并编译并返回新的实例。
    ///
    /// 注意：
    /// 使用 virtual override 的方式可以选择拦截部分方法。
    /// 可以提供和原来模块一致的接口。
    /// </summary>
    public class ModuleRedirect
    {
        // 本应用：hash分组的一些配置。
        public const int ChoiceType = gnet.Provider.BBind.ChoiceTypeHashUserId;
        public static int GetChoiceHashCode()
        {
            return (Zeze.Transaction.Transaction.Current.UserState as Login.Session).Account.GetHashCode();
        }

        public static ModuleRedirect Instance = new ModuleRedirect();

        public string SrcDirWhenPostBuild { get; set; } // ugly
        public bool HasNewGen { get; private set; } = false;

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            List<System.Reflection.MethodInfo> overrides = new List<System.Reflection.MethodInfo>();
            var methods = module.GetType().GetMethods();
            foreach (var method in methods)
            {
                var attrs = method.GetCustomAttributes(typeof(ModuleRedirectAttribute), false);
                if (attrs.Length == 0)
                    continue;
                if (false == method.IsVirtual)
                    throw new Exception("ModuleRedirect Need Virtual。");
                overrides.Add(method);
            }
            if (overrides.Count == 0)
                return module; // 没有需要重定向的方法。


            string genClassName = $"_ModuleRedirect_{module.FullName.Replace('.', '_')}_Gen_";
            if (null == SrcDirWhenPostBuild)
            {
                module.UnRegister();
                //Console.WriteLine($"'{module.FullName}' Replaced.");
                // from Game.App.Start. try load new module instance.
                return (Zeze.IModule)Activator.CreateInstance(Type.GetType(genClassName));
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

        public Dictionary<string, Func<gnet.Provider.ModuleRedirect, int>> Handles { get; }
            = new Dictionary<string, Func<gnet.Provider.ModuleRedirect, int>>();

        enum ReturnType
        {
            Void,
            TaskCompletionSource,
        }
        private (ReturnType, string) GetReturnType(Type type)
        {
            if (type == typeof(void))
                return (ReturnType.Void, "void");
            if (type == typeof(TaskCompletionSource<int>))
                return (ReturnType.TaskCompletionSource, "System.Threading.Tasks.TaskCompletionSource<int>");
            throw new Exception("ReturnType Must Be void Or TaskCompletionSource<int>");
        }

        private bool HasModeParamter(ParameterInfo[] parameters)
        {
            if (parameters.Length == 0)
                return false;
            if (parameters[parameters.Length - 1].ParameterType == typeof(Zeze.TransactionModes))
                return true;
            return false;
        }

        private string GetChoiceHashCodeSource(MethodInfo method)
        {
            var attrs = method.GetCustomAttributes(typeof(ModuleRedirectAttribute), false);
            if (null == attrs || attrs.Length == 0)
                return "Game.ModuleRedirect.GetChoiceHashCode()";
            var attr = attrs[0] as ModuleRedirectAttribute;
            if (string.IsNullOrEmpty(attr.ChoiceHashCodeSource))
                return "Game.ModuleRedirect.GetChoiceHashCode()";
            return attr.ChoiceHashCodeSource;
        }

        private string GetMethodNameWithHash(string name)
        {
            if (!name.StartsWith("Run"))
                throw new Exception("Method Name Need StartsWith 'Run'.");
            return name.Substring(3);
        }

        private string GenModuleCode(Zeze.IModule module, string genClassName, List<System.Reflection.MethodInfo> overrides)
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine($"public class {genClassName} : {module.FullName}.Module{module.Name}");
            sb.AppendLine($"{{");

            // TaskCompletionSource<int> void
            StringBuilder sbHandles = new StringBuilder();
            foreach (var method in overrides)
            {
                var parameters = method.GetParameters();
                var parametersDefine = ToDefineString(parameters);
                var (parametersCallNoMode, modeCall) = ToCallString(parameters);

                var methodNameWithHash = GetMethodNameWithHash(method.Name);
                var (returnType, returnTypeName) = GetReturnType(method.ReturnParameter.ParameterType);

                sb.AppendLine($"    public override {returnTypeName} {method.Name}({parametersDefine})");
                sb.AppendLine($"    {{");
                sb.AppendLine($"        if (Game.ModuleRedirect.Instance.IsLocalServer(\"{module.FullName}\"))");
                sb.AppendLine($"        {{");
                switch (returnType)
                {
                    case ReturnType.Void:
                        sb.AppendLine($"            base.{method.Name}({parametersCallNoMode}{modeCall});");
                        sb.AppendLine($"            return;");
                        break;
                    case ReturnType.TaskCompletionSource:
                        sb.AppendLine($"            return base.{method.Name}({parametersCallNoMode}{modeCall});");
                        break;
                }
                sb.AppendLine($"        }}");
                sb.AppendLine($"");
                string rpcVarName = "tmp" + TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"        var {rpcVarName} = new gnet.Provider.ModuleRedirect();");
                sb.AppendLine($"        {rpcVarName}.Argument.ModuleId = {module.Id};");
                sb.AppendLine($"        {rpcVarName}.Argument.HashCode = {GetChoiceHashCodeSource(method)};");
                sb.AppendLine($"        {rpcVarName}.Argument.MethodFullName = \"{module.FullName}:{method.Name}\";");
                sb.AppendLine($"        var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
                GenEncode(sb, "        ", parameters);
                sb.AppendLine($"        {rpcVarName}.Argument.Params = new Zeze.Net.Binary(_bb_);");
                sb.AppendLine($"");
                string sessionVarName = "tmp" + TmpVarNameId.IncrementAndGet();
                string futureVarName = "tmp" + TmpVarNameId.IncrementAndGet();
                sb.AppendLine($"        var {sessionVarName} = Zeze.Transaction.Transaction.Current.UserState as Game.Login.Session;");
                sb.AppendLine($"        var {futureVarName} = new System.Threading.Tasks.TaskCompletionSource<int>();");
                sb.AppendLine($"        {rpcVarName}.Send({sessionVarName}.Link, (_) =>");
                sb.AppendLine($"        {{");
                sb.AppendLine($"            if ({rpcVarName}.IsTimeout)");
                sb.AppendLine($"                {futureVarName}.SetException(new System.Exception(\"{module.FullName}:{method.Name} Rpc Timeout.\"));");
                sb.AppendLine($"            else if (gnet.Provider.ModuleRedirect.ResultCodeSuccess != {rpcVarName}.ResultCode)");
                sb.AppendLine($"                {futureVarName}.SetException(new System.Exception($\"{module.FullName}:{method.Name} Rpc Error {{{rpcVarName}.ResultCode}}.\"));");
                sb.AppendLine($"            else");
                sb.AppendLine($"                {futureVarName}.SetResult({rpcVarName}.Result.ReturnCode);");
                sb.AppendLine($"            // TODO decode Result.Params and callback.");
                sb.AppendLine($"            return Zeze.Transaction.Procedure.Success;");
                sb.AppendLine($"        }});");
                if (returnType == ReturnType.TaskCompletionSource)
                    sb.AppendLine($"        return {futureVarName};");
                sb.AppendLine($"    }}");
                sb.AppendLine($"");

                sbHandles.AppendLine($"        Game.ModuleRedirect.Instance.Handles.Add(\"{module.FullName}:{method.Name}\", (rpc) =>");
                sbHandles.AppendLine($"        {{");
                sbHandles.AppendLine($"            var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(rpc.Argument.Params);");
                foreach (var p in parameters)
                {
                    GenLocalVariable(sbHandles, "            ", p.ParameterType, p.Name);
                }
                GenDecode(sbHandles, "            ", parameters);
                sbHandles.AppendLine($"            rpc.Result.ReturnCode = base.{methodNameWithHash}(rpc.Argument.HashCode, {parametersCallNoMode});");
                sbHandles.AppendLine($"            // TODO encode out|ref params to rpc.Result.Params");
                sbHandles.AppendLine($"            return rpc.Result.ReturnCode;");
                sbHandles.AppendLine($"        }});");
                sbHandles.AppendLine($"");
            }
            sb.AppendLine($"    public {genClassName}() : base(Game.App.Instance)");
            sb.AppendLine($"    {{");
            sb.Append(sbHandles.ToString());
            sb.AppendLine($"    }}");
            sb.AppendLine($"");
            sb.AppendLine($"}}");

            return sb.ToString();
        }

        public bool IsLocalServer(string moduleName)
        {
            if (Game.App.Instance.ProviderModuleBinds.Modules.TryGetValue(moduleName, out var module))
            {
                return module.Providers.Contains(Game.App.Instance.Zeze.Config.AutoKeyLocalId);
            }
            return false;
        }

        private Dictionary<Type, (
            Action<StringBuilder, string, string>, // encoder
            Action<StringBuilder, string, string>, // decoder
            Action<StringBuilder, string, string>, // define local variable
            Func<string> // typename
            )>
            Serializer = new Dictionary<Type, (Action<StringBuilder, string, string>, Action<StringBuilder, string, string>, Action<StringBuilder, string, string>, Func<string>)>();

        private Dictionary<string, (
            Action<StringBuilder, string, string, Type, Type>, // encoder
            Action<StringBuilder, string, string, Type, Type>, // decoder
            Action<StringBuilder, string, string, Type, Type>, // define local variable
            Func<Type, Type, string> // typename
            )>
            System_Collections_Generic_Serializer = new Dictionary<string, (Action<StringBuilder, string, string, Type, Type>, Action<StringBuilder, string, string, Type, Type>, Action<StringBuilder, string, string, Type, Type>, Func<Type, Type, string>)>();

        private Zeze.Util.AtomicLong TmpVarNameId = new Zeze.Util.AtomicLong();

        private ModuleRedirect()
        {
            /*
            Serializer[typeof(void)] = (
                (sb, prefix, varName) => { },
                (sb, prefix, varName) => { },
                (sb, prefix, varName) => { },
                () => "void"
                );
            */

            Serializer[typeof(Zeze.Net.Binary)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteBinary({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadBinary();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}Zeze.Net.Binary {varName};"),
                () => "Zeze.Net.Binary"
                );

            Serializer[typeof(bool)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteBool({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadBool();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}bool {varName};"),
                () => "bool"
                );

            Serializer[typeof(byte)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteByte({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadByte();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}byte {varName};"),
                () => "byte"
                );

            Serializer[typeof(Zeze.Serialize.ByteBuffer)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteByteBuffer({varName});"),
                // 这里不用ReadByteBuffer，这个方法和原来的buffer共享内存，除了编解码时用用，开放给应用不大好。
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = Zeze.Serialize.ByteBuffer.Wrap(_bb_.ReadBytes());"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}Zeze.Serialize.ByteBuffer {varName};"),
                () => "Zeze.Serialize.ByteBuffer"
                );

            Serializer[typeof(byte[])] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteBytes({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadBytes();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}byte[] {varName};"),
                () => "byte[]"
                );

            Serializer[typeof(double)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteDouble({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadDouble();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}double {varName};"),
                () => "double"
                );

            Serializer[typeof(float)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteFloat({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadFloat();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}float {varName};"),
                () => "float"
                );

            Serializer[typeof(int)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteInt({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadInt();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}int {varName};"),
                () => "int"
                );

            Serializer[typeof(long)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteLong({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadLong();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}long {varName};"),
                () => "long"
                );

            Serializer[typeof(short)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteShort({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadShort();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}short {varName};"),
                () => "short"
                );

            Serializer[typeof(string)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteString({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadString();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}string {varName};"),
                () => "string"
               );

            Serializer[typeof(uint)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteUint({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadUint();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}uint {varName};"),
                () => "uint"
                );

            Serializer[typeof(ulong)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteUlong({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadUlong();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}ulong {varName};"),
                () => "ulong"
                );

            /////////////////////////////////////////////////////////////////////////
            ///
            System_Collections_Generic_Serializer["System.Collections.Generic.Dictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.HashSet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.HashSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.HashSet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.ICollection"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.ICollection<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IDictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IDictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IEnumerable"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IEnumerable<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IList<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyCollection"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlyCollection<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyDictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlyDictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlyList<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlySet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.HashSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlySet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.ISet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.HashSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.ISet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.KeyValuePair"] = (
                (sb, prefix, varName, key, value) =>
                {
                    GenEncode(sb, prefix, key, $"{varName}.Key");
                    GenEncode(sb, prefix, value, $"{varName}.Value");
                },
                (sb, prefix, varName, key, value) =>
                {
                    string tmpKey = "tmpKey" + TmpVarNameId.IncrementAndGet();
                    string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
                    GenLocalVariable(sb, prefix, key, tmpKey);
                    GenLocalVariable(sb, prefix, value, tmpValue);
                    GenDecode(sb, prefix, key, $"{tmpKey}");
                    GenDecode(sb, prefix, value, $"{tmpValue}");
                    sb.AppendLine($"{prefix}{varName} = new System.Collections.Generic.KeyValuePair<{GetTypeName(key)}, {GetTypeName(value)}>({tmpKey}, {tmpValue});");
                },
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}System.Collections.Generic.KeyValuePair<{GetTypeName(key)}, {GetTypeName(value)}> {varName};"),
                (key, value) => $"System.Collections.Generic.KeyValuePair<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.LinkedList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.LinkedList<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.LinkedList<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.List"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.List<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.Queue"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Queue<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.Queue<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.SortedDictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.SortedDictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.SortedDictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.SortedList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.SortedList<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.SortedList<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.SortedSet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.SortedSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.SortedSet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.Stack"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), // 反过来的，see decode。
                (sb, prefix, varName, key, value) =>
                {
                    string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
                    string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
                    string tmpReverse = "tmpReverse" + TmpVarNameId.IncrementAndGet();
                    string tmpReverseValue = "tmpReverse" + TmpVarNameId.IncrementAndGet();
                    sb.AppendLine($"{prefix}int {tmpi} = _bb_.ReadInt();");
                    sb.AppendLine($"{prefix}{GetTypeName(value)} [] {tmpReverse} = new {GetTypeName(value)}[{tmpi}];");
                    sb.AppendLine($"{prefix}for (; {tmpi} > 0; --{tmpi})");
                    sb.AppendLine($"{prefix}{{");
                    GenLocalVariable(sb, prefix + "    ", value, tmpValue);
                    GenDecode(sb, prefix + "    ", value, $"{tmpValue}");
                    sb.AppendLine($"{prefix}    {tmpReverse}[{tmpi} - 1] = {tmpValue};");
                    sb.AppendLine($"{prefix}}}");
                    sb.AppendLine($"{prefix}foreach (var {tmpReverseValue} in {tmpReverse})");
                    sb.AppendLine($"{prefix}{{");
                    sb.AppendLine($"{prefix}    {varName}.Push({tmpReverseValue});");
                    sb.AppendLine($"{prefix}}}");
                },
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Stack<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.Stack<{GetTypeName(value)}>"
            );
        }

        private void GenEncodeGeneric_2(StringBuilder sb, string prefix, string varName, Type key, Type value)
        {
            string tmp = "tmp" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}_bb_.WriteInt({varName}.Count);");
            sb.AppendLine($"{prefix}foreach (var {tmp} in {varName})");
            sb.AppendLine($"{prefix}{{");
            GenEncode(sb, prefix + "    ", key, $"{tmp}.Key");
            GenEncode(sb, prefix + "    ", value, $"{tmp}.Value");
            sb.AppendLine($"{prefix}}}");
        }

        private void GenDecodeGeneric_2(StringBuilder sb, string prefix, string varName, Type key, Type value)
        {
            string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
            string tmpKey = "tmpKey" + TmpVarNameId.IncrementAndGet();
            string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}for (int {tmpi} = _bb_.ReadInt(); {tmpi} > 0; --{tmpi})");
            sb.AppendLine($"{prefix}{{");
            GenLocalVariable(sb, prefix + "    ", key, tmpKey);
            GenLocalVariable(sb, prefix + "    ", value, tmpValue);
            GenDecode(sb, prefix + "    ", key, $"{tmpKey}");
            GenDecode(sb, prefix + "    ", value, $"{tmpValue}");
            sb.AppendLine($"{prefix}    {varName}.Add({tmpKey}, {tmpValue});");
            sb.AppendLine($"{prefix}}}");
        }

        private void GenEncodeGeneric_1(StringBuilder sb, string prefix, string varName, Type value)
        {
            string tmp = "tmp" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}_bb_.WriteInt({varName}.Count);");
            sb.AppendLine($"{prefix}foreach (var {tmp} in {varName})");
            sb.AppendLine($"{prefix}{{");
            GenEncode(sb, prefix + "    ", value, $"{tmp}");
            sb.AppendLine($"{prefix}}}");
        }

        private void GenDecodeGeneric_1(StringBuilder sb, string prefix, string varName, Type value)
        {
            string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
            string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}for (int {tmpi} = _bb_.ReadInt(); {tmpi} > 0; --{tmpi})");
            sb.AppendLine($"{prefix}{{");
            GenLocalVariable(sb, prefix + "    ", value, tmpValue);
            GenDecode(sb, prefix + "    ", value, $"{tmpValue}");
            sb.AppendLine($"{prefix}    {varName}.Add({tmpValue});");
            sb.AppendLine($"{prefix}}}");
        }

        public string GetFullNameNoGenericParameters(Type type)
        {
            string className = type.IsGenericType ? type.Name.Substring(0, type.Name.IndexOf('`')) : type.Name;
            // 处理嵌套类名字。
            string fullName = className;
            for (Type declaring = type.DeclaringType; declaring != null; declaring = declaring.DeclaringType)
            {
                fullName = declaring.Name + "." + fullName;
            }
            return null != type.Namespace ? type.Namespace + "." + fullName : fullName;
        }

        public string GetTypeName(Type type)
        {
            if (Serializer.TryGetValue(type, out var basic))
            {
                return basic.Item4();
            }

            if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
            {
                return type.FullName;
            }

            string fullName = GetFullNameNoGenericParameters(type);
            if (false == type.IsGenericType)
                return fullName;

            Type[] parameters = type.GenericTypeArguments;
            if (System_Collections_Generic_Serializer.TryGetValue(fullName, out var generic))
            {
                switch (parameters.Length)
                {
                    case 1:
                        return generic.Item4(null, parameters[0]);

                    case 2:
                        return generic.Item4(parameters[0], parameters[1]);

                    default:
                        break; // fall down.
                }
            }
            fullName += "<";
            bool first = true;
            foreach (var parameter in parameters)
            {
                if (first)
                    first = false;
                else
                    fullName += ", ";
                fullName += GetTypeName(parameter);
            }
            fullName += ">";
            return fullName;
        }

        public void GenLocalVariable(StringBuilder sb, string prefix, Type type, string varName)
        {
            if (Serializer.TryGetValue(type, out var basic))
            {
                basic.Item3(sb, prefix, varName);
                return;
            }

            if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
            {
                sb.AppendLine($"{prefix}{type.FullName} {varName} = new {type.FullName}();");
                return;
            }

            string typename = GetTypeName(type);
            if (false == type.IsGenericType)
            {
                // decode 不需要初始化。JsonSerializer.Deserialize
                sb.AppendLine($"{prefix}{typename} {varName};");
                return;
            }
            Type[] parameters = type.GenericTypeArguments;
            if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
            {
                switch (parameters.Length)
                {
                    case 1:
                        generic.Item3(sb, prefix, varName, null, parameters[0]);
                        return;

                    case 2:
                        generic.Item3(sb, prefix, varName, parameters[0], parameters[1]);
                        return;

                    default:
                        break; // fall down.
                }
            }
            // decode 不需要初始化。JsonSerializer.Deserialize
            sb.AppendLine($"{prefix}{typename} {varName};");
        }

        public void GenEncode(StringBuilder sb, string prefix, Type type, string varName)
        {
            if (Serializer.TryGetValue(type, out var basic))
            {
                basic.Item1(sb, prefix, varName);
                return;
            }

            if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
            {
                sb.AppendLine($"{prefix}{varName}.Encode(_bb_);");
                return;
            }

            if (type.IsGenericType)
            {
                if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
                {
                    Type[] parameters = type.GenericTypeArguments;
                    switch (parameters.Length)
                    {
                        case 1:
                            generic.Item1(sb, prefix, varName, null, parameters[0]);
                            return;

                        case 2:
                            generic.Item1(sb, prefix, varName, parameters[0], parameters[1]);
                            return;

                        default:
                            break; // fall down.
                    }
                }
                // fall down
            }

            // Utf8Json https://aloiskraus.wordpress.com/2019/09/29/net-serialization-benchmark-2019-roundup/
            sb.AppendLine($"{prefix}_bb_.WriteBytes(System.Text.Json.JsonSerializer.SerializeToUtf8Bytes({varName}, typeof({GetTypeName(type)})));");
        }

        public void GenDecode(StringBuilder sb, string prefix, Type type, string varName)
        {
            if (Serializer.TryGetValue(type, out var p))
            {
                p.Item2(sb, prefix, varName);
                return;
            }

            if (type.IsSubclassOf(typeof(Zeze.Transaction.Bean)))
            {
                var tmp0 = $"tmp{TmpVarNameId.IncrementAndGet()}";
                sb.AppendLine($"{prefix}var {tmp0} = new {type.FullName}();");
                sb.AppendLine($"{prefix}{tmp0}.Decode(_bb_);");
                return;
            }

            if (type.IsGenericType)
            {
                if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
                {
                    Type[] parameters = type.GenericTypeArguments;
                    switch (parameters.Length)
                    {
                        case 1:
                            generic.Item2(sb, prefix, varName, null, parameters[0]);
                            return;

                        case 2:
                            generic.Item2(sb, prefix, varName, parameters[0], parameters[1]);
                            return;

                        default:
                            break; // fall down.
                    }
                }
                // fall down
            }

            string tmp1 = "tmp" + TmpVarNameId.IncrementAndGet();
            string tmp2 = "tmp" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}var {tmp1} = _bb_.ReadByteBuffer();");
            sb.AppendLine($"{prefix}var {tmp2} = new System.ReadOnlySpan<byte>({tmp1}.Bytes, {tmp1}.ReadIndex, {tmp1}.Size);");
            sb.AppendLine($"{prefix}{varName} = System.Text.Json.JsonSerializer.Deserialize<{GetTypeName(type)}> ({tmp2});");
        }

        public void GenEncode(StringBuilder sb, string prefix, System.Reflection.ParameterInfo[] parameters)
        {
            foreach (var p in parameters)
            {
                if (p.IsOut)
                    continue;
                GenEncode(sb, prefix, p.ParameterType, p.Name);
            }
        }

        public void GenDecode(StringBuilder sb, string prefix, System.Reflection.ParameterInfo[] parameters)
        {
            foreach (var p in parameters)
            {
                if (p.IsOut)
                    continue;
                GenDecode(sb, prefix, p.ParameterType, p.Name);
            }
        }

        public string ToDefineString(System.Reflection.ParameterInfo[] parameters)
        {
            StringBuilder sb = new StringBuilder();
            bool first = true;
            foreach (var p in parameters)
            {
                if (first)
                    first = false;
                else
                    sb.Append(", ");
                string prefix = "";
                if (p.IsOut)
                    prefix = "out ";
                else if (p.ParameterType.IsByRef)
                    prefix = "ref ";
                sb.Append(GetTypeName(p.ParameterType)).Append(" ").Append(prefix).Append(p.Name);
            }
            return sb.ToString();
        }

        public (string, string) ToCallString(System.Reflection.ParameterInfo[] parameters)
        {
            StringBuilder sb = new StringBuilder();
            bool first = true;
            int callCount = HasModeParamter(parameters) ? parameters.Length - 1 : parameters.Length;
            for (int i = 0; i < callCount; ++i)
            {
                var p = parameters[i];
                if (first)
                    first = false;
                else
                    sb.Append(", ");
                string prefix = "";
                if (p.IsOut)
                    prefix = "out ";
                else if (p.ParameterType.IsByRef)
                    prefix = "ref ";

                if (string.IsNullOrEmpty(prefix))
                    sb.Append(p.Name);
                else
                    sb.Append(prefix).Append("_" + p.Name + "_");
            }
            string separatorMode = callCount == 0 ? "" : ", ";
            string modeCall = (callCount == parameters.Length) ? "" : $"{separatorMode}{parameters[parameters.Length - 1].Name}";
            return (sb.ToString(), modeCall);
        }
    }

    [System.AttributeUsage(System.AttributeTargets.Method)]
    public class ModuleRedirectAttribute : System.Attribute
    {
        public string ChoiceHashCodeSource { get; }
        public ModuleRedirectAttribute(string source = null)
        {
            ChoiceHashCodeSource = source;
        }
    }
}
