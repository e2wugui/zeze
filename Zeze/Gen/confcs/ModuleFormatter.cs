using System.IO;

namespace Zeze.Gen.confcs
{
    public class ModuleFormatter
    {
        readonly Project project;
        internal readonly Module module;
        readonly string genDir;
        readonly string srcDir;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            using StreamWriter sw = module.OpenWriter(genDir, $"AbstractModule{module.Name}.cs");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            if (module.Comment.Length > 0)
                sw.WriteLine(module.Comment);
            sw.WriteLine($"// ReSharper disable RedundantNameQualifier UnusedParameter.Global");
            sw.WriteLine($"// ReSharper disable once CheckNamespace");
            sw.WriteLine($"namespace {module.Path()}");
            sw.WriteLine($"{{");
            sw.WriteLine($"    public abstract class AbstractModule{module.Name}");
            sw.WriteLine($"    {{");
            sw.WriteLine($"        public string FullName => \"{module.Path()}\";");
            sw.WriteLine($"        public string Name => \"{module.Name}\";");
            sw.WriteLine($"        public int Id => {module.Id};");
            GenEnums(sw);
            sw.WriteLine();
            sw.WriteLine($"        public void Register(Zeze.Net.Service service)");
            sw.WriteLine($"        {{");
            RegisterProtocols(sw, "service");
            sw.WriteLine($"        }}");
            GenAbstractProtocolHandles(sw);
            sw.WriteLine($"    }}");
            sw.WriteLine($"}}");
        }

        public void GenEnums(StreamWriter sw)
        {
            // declare enums
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine($"        public const {TypeName.GetName(Types.Type.Compile(e.Type))} {e.Name} = {e.Value};{e.Comment}");
        }

        public void RegisterProtocols(StreamWriter sw, string serviceVar)
        {
            sw.WriteLine("            // register protocol factory and handles");
            foreach (Protocol p in module.Protocols.Values)
            {
                if (p is Rpc rpc)
                {
                    // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                    sw.WriteLine($"            {serviceVar}.AddFactoryHandle({rpc.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                    sw.WriteLine("            {");
                    if ((rpc.HandleFlags & Program.HandleCSharpFlags) != 0)
                    {
                        sw.WriteLine($"                Factory = () => new {rpc.Space.Path(".", rpc.Name)}(),");
                        sw.WriteLine($"                Handle = Process{rpc.Name}Request");
                    }
                    else
                        sw.WriteLine($"                Factory = () => new {rpc.Space.Path(".", rpc.Name)}()");
                    sw.WriteLine("            });");
                    continue;
                }
                if (0 != (p.HandleFlags & Program.HandleCSharpFlags))
                {
                    sw.WriteLine($"            {serviceVar}.AddFactoryHandle({p.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle");
                    sw.WriteLine("            {");
                    sw.WriteLine($"                Factory = () => new {p.Space.Path(".", p.Name)}(),");
                    sw.WriteLine($"                Handle = Process{p.Name}");
                    sw.WriteLine("            });");
                }
            }
        }

        public void GenAbstractProtocolHandles(StreamWriter sw)
        {
            foreach (Protocol p in module.Protocols.Values)
            {
                if (p is Rpc rpc)
                {
                    if ((rpc.HandleFlags & Program.HandleCSharpFlags) != 0)
                    {
                        sw.WriteLine();
                        sw.WriteLine($"        protected abstract System.Threading.Tasks.Task<long> Process{rpc.Name}Request(Zeze.Net.Protocol p);");
                    }
                }
                else if ((p.HandleFlags & Program.HandleCSharpFlags) != 0)
                {
                    sw.WriteLine();
                    sw.WriteLine($"        protected abstract System.Threading.Tasks.Task<long> Process{p.Name}(Zeze.Net.Protocol p);");
                }
            }
        }
    }
}
