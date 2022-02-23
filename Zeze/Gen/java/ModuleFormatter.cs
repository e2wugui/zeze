using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.java
{
    public class ModuleFormatter
    {
        readonly Project project;
        readonly Module module;
        readonly string genDir;
        readonly string srcDir;
        readonly string moduleName;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
            moduleName = string.Concat(module.Name[..1].ToUpper(), module.Name.AsSpan(1));
        }

        FileChunkGen FileChunkGen;

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
            sw.WriteLine("    public void Start(" + project.Solution.Name + ".App app) throws Throwable {");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void Stop(" + project.Solution.Name + ".App app) throws Throwable {");
            sw.WriteLine("    }");
            sw.WriteLine();
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine("    @Override");
                            sw.WriteLine($"    protected long Process{rpc.Name}Request({rpc.Space.Path(".", rpc.Name)} r) {{");
                            sw.WriteLine($"        return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("    }");
                            sw.WriteLine();
                        }
                    }
                    else
                    {
                        if ((p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine("    @Override");
                            sw.WriteLine($"    protected long Process{p.Name}({p.Space.Path(".", p.Name)} p) {{");
                            sw.WriteLine("        return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("    }");
                            sw.WriteLine();
                        }
                    }
                }
            }
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

        void ModuleGen(StreamWriter sw)
        {
            sw.WriteLine($"    public static final int ModuleId = {module.Id};");
            sw.WriteLine();
            bool genTable = false;
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                {
                    sw.WriteLine("    protected final " + table.Name + " _" + table.Name + " = new " + table.Name + "();");
                    genTable = true;
                }
            }
            if (genTable)
                sw.WriteLine();
            sw.WriteLine($"    public {project.Solution.Name}.App App;");
            sw.WriteLine();

            sw.WriteLine($"    public AbstractModule({project.Solution.Name}.App app) {{");
            sw.WriteLine("        App = app;");
            sw.WriteLine("        // register protocol factory and handles");
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                bool defReflect = false;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if (!defReflect)
                        {
                            defReflect = true;
                            sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(this.getClass());");
                        }
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        string fullName = rpc.Space.Path(".", rpc.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<{fullName}>();");
                        sw.WriteLine($"            factoryHandle.Factory = {fullName}::new;");
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                            sw.WriteLine($"            factoryHandle.Handle = this::Process{rpc.Name}Request;");
                        sw.WriteLine($"            factoryHandle.Level = _reflect.getTransactionLevel(\"Process{rpc.Name}Request\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel});");
                        sw.WriteLine($"            App.{serv.Name}.AddFactoryHandle({rpc.TypeId}L, factoryHandle); // {rpc.Space.Id}, {rpc.Id}");
                        sw.WriteLine("        }");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        if (!defReflect)
                        {
                            defReflect = true;
                            sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(this.getClass());");
                        }
                        string fullName = p.Space.Path(".", p.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<{fullName}>();");
                        sw.WriteLine($"            factoryHandle.Factory = {fullName}::new;");
                        sw.WriteLine($"            factoryHandle.Handle = this::Process{p.Name};");
                        sw.WriteLine($"            factoryHandle.Level = _reflect.getTransactionLevel(\"Process{p.Name}\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel});");
                        sw.WriteLine($"            App.{serv.Name}.AddFactoryHandle({p.TypeId}L, factoryHandle); // {p.Space.Id}, {p.Id}");
                        sw.WriteLine( "        }");
                    }
                }
            }
            sw.WriteLine("        // register table");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_{table.Name}.getName()).getDatabaseName(), _{table.Name});");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void UnRegister() {");
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"        App.{serv.Name}.getFactorys().remove({rpc.TypeId}L);");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"        App.{serv.Name}.getFactorys().remove({p.TypeId}L);");
                    }
                }
            }
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_{table.Name}.getName()).getDatabaseName(), _{table.Name});");
            }
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

        public void MakeInterface()
        {
            using StreamWriter sw = module.OpenWriter(genDir, "AbstractModule.java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + module.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("public abstract class AbstractModule extends Zeze.IModule {");
            sw.WriteLine($"    public String getFullName() {{ return \"{module.Path()}\"; }}");
            sw.WriteLine($"    public String getName() {{ return \"{moduleName}\"; }}");
            sw.WriteLine($"    public int getId() {{ return {module.Id}; }}");
            sw.WriteLine();
            // declare enums
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine("    public static final int " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (module.Enums.Count > 0)
                sw.WriteLine();

            foreach (Protocol p in GetProcessProtocols())
            {
                if (p is Rpc rpc)
                    sw.WriteLine($"    protected abstract long Process{rpc.Name}Request({rpc.Space.Path(".", rpc.Name)} r) throws Throwable;");
                else
                    sw.WriteLine($"    protected abstract long Process{p.Name}({p.Space.Path(".", p.Name)} p) throws Throwable;");
            }
            sw.WriteLine();
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
