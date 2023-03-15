using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.cxx
{
    public class ModuleFormatter
    {
        readonly Project project;
        public readonly Module module;
        readonly string genDir;
        readonly string srcDir;
        readonly string moduleName;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
            moduleName = Program.Upper1(module.Name);
        }

        FileChunkGen FileChunkGen;

        public bool GenEmptyProtocolHandles(StreamWriter sw, bool shortIf = true)
        {
            bool written = false;
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            if (written)
                                sw.WriteLine();
                            written = true;
                            sw.WriteLine("    @Override");
                            sw.WriteLine($"    protected long Process{rpc.Name}Request({rpc.Space.Path(".", rpc.Name)} r) {{");
                            sw.WriteLine($"        return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("    }");
                        }
                    }
                    else
                    {
                        if ((p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            if (written)
                                sw.WriteLine();
                            written = true;
                            sw.WriteLine("    @Override");
                            sw.WriteLine($"    protected long Process{p.Name}({p.Space.Path(".", p.Name)} p) {{");
                            sw.WriteLine("        return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("    }");
                        }
                    }
                }
            }
            return written;
        }

        public void Make()
        {
            MakeInterface();
            FileChunkGen = new FileChunkGen();
            string fullDir = module.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, $"Module{moduleName}.java");
            if (FileChunkGen.LoadFile(fullFileName))
            {
                fixProcessParam(FileChunkGen);
                FileChunkGen.SaveFile(fullFileName, GenChunkByName, GenBeforeChunkByName);
                return;
            }
            // new file
            FileSystem.CreateDirectory(fullDir);
            using StreamWriter sw = Program.OpenStreamWriter(fullFileName);
            sw.WriteLine("package " + module.Path() + ";");
            sw.WriteLine();
            // sw.WriteLine(FileChunkGen.ChunkStartTag + " " + ChunkNameImport);
            // ImportGen(sw);
            // sw.WriteLine(FileChunkGen.ChunkEndTag + " " + ChunkNameImport);
            // sw.WriteLine();
            sw.WriteLine($"public class Module{moduleName} extends AbstractModule {{");
            // sw.WriteLine($"    public Module{moduleName}({project.Solution.Name}.App app) {{");
            // sw.WriteLine("        super(app);");
            // sw.WriteLine("    }");
            // sw.WriteLine();
            sw.WriteLine("    public void Start(" + project.Solution.Name + ".App app) throws Exception {");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void Stop(" + project.Solution.Name + ".App app) throws Exception {");
            sw.WriteLine("    }");
            sw.WriteLine();
            if (GenEmptyProtocolHandles(sw))
                sw.WriteLine();
            sw.WriteLine("    " + FileChunkGen.ChunkStartTag + " " + ChunkNameModuleGen + " @formatter:off");
            ConstructorGen(sw);
            sw.WriteLine("    " + FileChunkGen.ChunkEndTag + " " + ChunkNameModuleGen + " @formatter:on");
            sw.WriteLine("}");
        }

        const string ChunkNameModuleGen = "GEN MODULE";
        const string ChunkNameImport = "IMPORT GEN";

        string GetHandleName(Protocol p)
        {
            if (p is Rpc rpc)
                return $"Process{rpc.Name}Request";
            return $"Process{p.Name}";
        }

        void fixProcessParam(FileChunkGen chunkGen)
        {
            foreach (var chunk in chunkGen.Chunks)
            {
                if (chunk.State != FileChunkGen.State.Normal)
                    continue;
                var lines = chunk.Lines;
                for (int i = 0; i < lines.Count - 1; i++)
                {
                    string line = lines[i];
                    if (!line.Contains("long Process"))
                        continue;
                    int p = line.IndexOf('(');
                    if (p < 0)
                        continue;
                    int q = line.IndexOf(')', p + 1);
                    if (q < 0)
                        continue;
                    string[] subs = line.Substring(p + 1, q - p - 1).Split(" ");
                    if (subs.Length != 2 || subs[0] != "Zeze.Net.Protocol" && subs[0] != "Protocol")
                        continue;
                    for (int j = 1; j <= 2; j++)
                    {
                        string nextLine = lines[i + j].Replace('\t', ' ').Trim();
                        if (!nextLine.StartsWith("var "))
                            continue;
                        int e = nextLine.IndexOf('=');
                        if (e < 0)
                            continue;
                        int a = nextLine.IndexOf('(');
                        if (a < 0)
                            continue;
                        int b = nextLine.IndexOf(')', a + 1);
                        if (b < 0)
                            continue;
                        if (nextLine.Substring(b + 1).Trim() != subs[1] + ';')
                            continue;
                        lines[i] = line.Substring(0, p + 1) + nextLine.Substring(a + 1, b - a - 1).Trim()
                            + ' ' + nextLine.Substring(3, e - 3).Trim() + line.Substring(q);
                        lines.RemoveAt(i + j);
                        i += j - 1;
                        break;
                    }
                }
            }
        }

        void NewProtocolHandle(StreamWriter sw)
        {
            var handles = GetProcessProtocols();
            var protoMap = new Dictionary<string, Protocol>();
            foreach (var p in handles)
            {
                if (p is Rpc)
                    protoMap[p.Name + "Request"] = p;
                else
                    protoMap[p.Name] = p;
            }
            // 找出现有的可能是协议实现的函数
            var exist = new HashSet<Protocol>();
            foreach (var chunk in FileChunkGen.Chunks)
            {
                if (chunk.State == FileChunkGen.State.Normal)
                {
                    foreach (var line in chunk.Lines)
                    {
                        int p = line.IndexOf("long Process");
                        if (p >= 0)
                        {
                            int q = line.IndexOf('(', p + 12);
                            if (q >= 0)
                            {
                                if (protoMap.TryGetValue(line.Substring(p + 12, q - p - 12).Trim(), out var h))
                                    exist.Add(h);
                            }
                        }
                    }
                }
            }

            // New Protocol
            foreach (var h in handles)
            {
                if (exist.Contains(h))
                    continue;

                var hName = GetHandleName(h);
                if (h is Rpc rpc)
                {
                    string fullName = rpc.Space.Path(".", rpc.Name);
                    sw.WriteLine("    @Override");
                    sw.WriteLine($"    protected long {hName}({fullName} r) {{");
                    sw.WriteLine($"        return Zeze.Transaction.Procedure.NotImplement;");
                    sw.WriteLine("    }");
                    sw.WriteLine("");
                }
                else
                {
                    string fullName = h.Space.Path(".", h.Name);
                    sw.WriteLine("    @Override");
                    sw.WriteLine($"    protected long {hName}({fullName} p) {{");
                    sw.WriteLine("        return Zeze.Transaction.Procedure.NotImplement;");
                    sw.WriteLine("    }");
                    sw.WriteLine("");
                }
            }
        }

        void GenChunkByName(StreamWriter writer, FileChunkGen.Chunk chunk)
        {
            switch (chunk.Name)
            {
                case ChunkNameModuleGen:
                    ConstructorGen(writer);
                    break;
                case ChunkNameImport:
                    ImportGen(writer);
                    break;
                default:
                    throw new Exception("unknown Chunk.Name=" + chunk.Name);
            }
        }

        void GenBeforeChunkByName(StreamWriter writer, FileChunkGen.Chunk chunk)
        {
            switch (chunk.Name)
            {
                case ChunkNameModuleGen:
                    NewProtocolHandle(writer);
                    break;
            }
        }

        void ImportGen(StreamWriter sw)
        {
        }

        void ConstructorGen(StreamWriter sw)
        {
            sw.WriteLine($"    public Module{moduleName}({project.Solution.Name}.App app) {{");
            sw.WriteLine("        super(app);");
            sw.WriteLine("    }");
        }

        public void RegisterProtocols(StreamWriter sw, bool isFirst = true, string serviceVarName = null)
        {
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App.{serv.Name}" : serviceVarName;
                bool defReflect = false;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if (!defReflect && isFirst)
                        {
                            defReflect = true;
                            sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(getClass());");
                        }
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        string fullName = rpc.Space.Path(".", rpc.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>({fullName}.class, {fullName}.TypeId_);");
                        sw.WriteLine($"            factoryHandle.Factory = {fullName}::new;");
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine($"            factoryHandle.Handle = this::Process{rpc.Name}Request;");
                            sw.WriteLine($"            factoryHandle.Level = _reflect.getTransactionLevel(\"Process{rpc.Name}Request\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel});");
                            sw.WriteLine($"            factoryHandle.Mode = _reflect.getDispatchMode(\"Process{rpc.Name}Request\", Zeze.Transaction.DispatchMode.Normal);");
                        }
                        else
                        {
                            sw.WriteLine($"            factoryHandle.Level = _reflect.getTransactionLevel(\"Process{rpc.Name}Response\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel});");
                            sw.WriteLine($"            factoryHandle.Mode = _reflect.getDispatchMode(\"Process{rpc.Name}Response\", Zeze.Transaction.DispatchMode.Normal);");
                        }
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({rpc.TypeId}L, factoryHandle); // {rpc.Space.Id}, {rpc.Id}");
                        sw.WriteLine("        }");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        if (!defReflect && isFirst)
                        {
                            defReflect = true;
                            sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(this.getClass());");
                        }
                        string fullName = p.Space.Path(".", p.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>({fullName}.class, {fullName}.TypeId_);");
                        sw.WriteLine($"            factoryHandle.Factory = {fullName}::new;");
                        sw.WriteLine($"            factoryHandle.Handle = this::Process{p.Name};");
                        sw.WriteLine($"            factoryHandle.Level = _reflect.getTransactionLevel(\"Process{p.Name}\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel});");
                        sw.WriteLine($"            factoryHandle.Mode = _reflect.getDispatchMode(\"Process{p.Name}\", Zeze.Transaction.DispatchMode.Normal);");
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({p.TypeId}L, factoryHandle); // {p.Space.Id}, {p.Id}");
                        sw.WriteLine("        }");
                    }
                }
            }
        }

        public void UnRegisterProtocols(StreamWriter sw, string serviceVarName = null)
        {
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App.{serv.Name}" : serviceVarName;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"        {serviceVar}.getFactorys().remove({rpc.TypeId}L);");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"        {serviceVar}.getFactorys().remove({p.TypeId}L);");
                    }
                }
            }
        }

        void ModuleGen(StreamWriter sw)
        {
            sw.WriteLine();
            sw.WriteLine($"    public final {project.Solution.Name}.App App;");
            sw.WriteLine();

            sw.WriteLine($"    public AbstractModule({project.Solution.Name}.App app) {{");
            sw.WriteLine("        App = app;");
            sw.WriteLine("        // register protocol factory and handles");
            RegisterProtocols(sw);
            sw.WriteLine("        // register table");
            sw.WriteLine("        // register servlet");
            bool writtenHeader = true;
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void UnRegister() {");
            UnRegisterProtocols(sw);
            sw.WriteLine("    }");
        }

        public void MakePartialImplement()
        {
            using StreamWriter sw = module.OpenWriter(srcDir, $"Module{moduleName}.java", false);

            if (sw == null)
                return;

            sw.WriteLine("package " + module.Path() + ";");
            sw.WriteLine();
            sw.WriteLine($"public class Module{moduleName} extends AbstractModule{moduleName} {{");
            sw.WriteLine("}");
        }

        public void GenEnums(StreamWriter sw)
        {
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine($"    public static final {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
        }

        public void GenAbstractProtocolHandles(StreamWriter sw)
        {
            var protocols = GetProcessProtocols();
            if (protocols.Count > 0)
                sw.WriteLine();
            foreach (Protocol p in protocols)
            {
                if (p is Rpc rpc)
                    sw.WriteLine($"    protected abstract long Process{rpc.Name}Request({rpc.Space.Path(".", rpc.Name)} r) throws Exception;");
                else
                    sw.WriteLine($"    protected abstract long Process{p.Name}({p.Space.Path(".", p.Name)} p) throws Exception;");
            }
        }

        public void MakeInterface()
        {
            using StreamWriter sw = module.OpenWriter(genDir, "AbstractModule.java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + module.Path() + ";");
            sw.WriteLine();
            if (module.Comment.Length > 0)
                sw.WriteLine(module.Comment);
            var classBase = (!project.EnableBase || string.IsNullOrEmpty(module.ClassBase)) ? "" : $"extends {module.ClassBase} ";
            sw.WriteLine($"public abstract class AbstractModule {classBase}implements Zeze.IModule {{");
            sw.WriteLine($"    public static final int ModuleId = {module.Id};");
            sw.WriteLine($"    public static final String ModuleName = \"{moduleName}\";");
            sw.WriteLine($"    public static final String ModuleFullName = \"{module.Path()}\";");
            sw.WriteLine();
            sw.WriteLine($"    @Override public int getId() {{ return ModuleId; }}");
            sw.WriteLine($"    @Override public String getName() {{ return ModuleName; }}");
            sw.WriteLine($"    @Override public String getFullName() {{ return ModuleFullName; }}");
            sw.WriteLine($"    @Override public String getWebPathBase() {{ return \"{module.WebPathBase}\";}}");
            // declare enums
            GenEnums(sw);
            GenAbstractProtocolHandles(sw);
            ModuleGen(sw);
            sw.WriteLine("}");
        }

        List<Protocol> GetProcessProtocols()
        {
            var result = new List<Protocol>();
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                            result.Add(rpc);
                    }
                    else
                    {
                        if ((p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                            result.Add(p);
                    }
                }
            }
            return result;
        }
    }
}
