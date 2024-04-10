using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.java
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
            if (sw == null)
                return;

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
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void StartLast() throws Exception {");
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

        bool defReflect = false; // 是否定义了_reflect变量。

        public void RegisterProtocols(StreamWriter sw, bool isFirst = true, string serviceVarName = null)
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
                        switch (p.CriticalLevel)
                        {
                            case Protocol.eCriticalPlus:
                                break;
                            case Protocol.eCritical:
                                sw.WriteLine("            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eCritical;");
                                break;
                            case Protocol.eNormal:
                                sw.WriteLine("            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eNormal;");
                                break;
                            case Protocol.eSheddable:
                                sw.WriteLine("            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eSheddable;");
                                break;
                            default:
                                throw new NotSupportedException(p.CriticalLevel.ToString());
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
                            sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(getClass());");
                        }
                        string fullName = p.Space.Path(".", p.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>({fullName}.class, {fullName}.TypeId_);");
                        sw.WriteLine($"            factoryHandle.Factory = {fullName}::new;");
                        sw.WriteLine($"            factoryHandle.Handle = this::Process{p.Name};");
                        sw.WriteLine($"            factoryHandle.Level = _reflect.getTransactionLevel(\"Process{p.Name}\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel});");
                        sw.WriteLine($"            factoryHandle.Mode = _reflect.getDispatchMode(\"Process{p.Name}\", Zeze.Transaction.DispatchMode.Normal);");
                        switch (p.CriticalLevel)
                        {
                            case Protocol.eCriticalPlus:
                                break;
                            case Protocol.eCritical:
                                sw.WriteLine("            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eCritical;");
                                break;
                            case Protocol.eNormal:
                                sw.WriteLine("            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eNormal;");
                                break;
                            case Protocol.eSheddable:
                                sw.WriteLine("            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eSheddable;");
                                break;
                            default:
                                throw new NotSupportedException(p.CriticalLevel.ToString());
                        }
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({p.TypeId}L, factoryHandle); // {p.Space.Id}, {p.Id}");
                        sw.WriteLine("        }");
                    }
                }
            }
        }

        public void RegisterZezeTables(StreamWriter sw, string zeze = null)
        {
            var zezeVar = string.IsNullOrEmpty(zeze) ? "App.Zeze" : zeze;
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                {
                    if (project.Hot && module.Hot)
                    {
                        //if (table.IsMemory) // 暂时禁掉。这是模块状态的一部分了，需要 upgrade 支持。【已经允许了】
                        //    throw new Exception("hot module can not use memory table.");

                        sw.WriteLine($"        {zezeVar}.replaceTable({zezeVar}.getConfig().getTableConf(_{table.Name}.getName()).getDatabaseName(), _{table.Name});");
                    }
                    else
                        sw.WriteLine($"        {zezeVar}.addTable({zezeVar}.getConfig().getTableConf(_{table.Name}.getName()).getDatabaseName(), _{table.Name});");
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

        public void UnRegisterZezeTables(StreamWriter sw, string zeze = null)
        {
            var zezeVar = string.IsNullOrEmpty(zeze) ? "App.Zeze" : zeze;
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                {
                    if (false == module.Hot || false == project.Hot)
                        sw.WriteLine($"        {zezeVar}.removeTable({zezeVar}.getConfig().getTableConf(_{table.Name}.getName()).getDatabaseName(), _{table.Name});");
                }
            }
        }

        private string GetCollectionLogTemplateName(Types.Type type)
        {
            if (type is Types.TypeList tlist)
            {
                string value = BoxingName.GetBoxingName(tlist.ValueType);
                var version = tlist.ValueType.IsNormalBeanOrRocks ? "2" : "1";
                return $"() -> new Zeze.Raft.RocksRaft.LogList{version}<>({value}.class)";
            }
            else if (type is Types.TypeSet tset)
            {
                string value = BoxingName.GetBoxingName(tset.ValueType);
                return $"() -> new Zeze.Raft.RocksRaft.LogSet1<>({value}.class)";
            }
            else if (type is Types.TypeMap tmap)
            {
                string key = BoxingName.GetBoxingName(tmap.KeyType);
                string value = BoxingName.GetBoxingName(tmap.ValueType);
                var version = tmap.ValueType.IsNormalBeanOrRocks ? "2" : "1";
                return $"() -> new Zeze.Raft.RocksRaft.LogMap{version}<>({key}.class, {value}.class)";
            }
            throw new Exception();
        }

        public void RegisterRocksTables(StreamWriter sw)
        {
            var depends = new HashSet<Types.Type>();
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks)
                {
                    var key = TypeName.GetName(table.KeyType);
                    var value = TypeName.GetName(table.ValueType);
                    table.ValueType.Depends(depends, null);
                    sw.WriteLine($"        rocks.registerTableTemplate(\"{table.Name}\", {key}.class, {value}.class);");
                }
            }
            var logfactorys = new HashSet<string>();
            foreach (var dep in depends)
            {
                if (dep.IsBean && dep.IsKeyable) // is beankey
                {
                    var depname = TypeName.GetName(dep);
                    logfactorys.Add($"() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>({depname}.class)");
                    continue;
                }
                if (dep.IsCollection)
                {
                    logfactorys.Add(GetCollectionLogTemplateName(dep));
                    continue;
                }
                // 基本类型 java 全部自动注册。
            }
            foreach (var fac in logfactorys)
            {
                sw.WriteLine($"        Zeze.Raft.RocksRaft.Rocks.registerLog({fac});");
            }
        }

        public void DefineZezeTables(StreamWriter sw)
        {
            bool isMultiInstance = module.MultiInstance && project is Component;

            if (isMultiInstance)
            {
                sw.WriteLine();
                sw.WriteLine($"    protected final String multiInstanceName;");
            }

            bool written = false;
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && false == table.IsRocks)
                {
                    if (!written)
                    {
                        written = true;
                        sw.WriteLine();
                    }
                    if (isMultiInstance)
                        sw.WriteLine("    protected final " + table.FullName + " _" + table.Name + ";");
                    else
                        sw.WriteLine("    protected final " + table.FullName + " _" + table.Name + " = new " + table.FullName + "();");
                }
            }

            if (isMultiInstance)
            {
                sw.WriteLine();
                sw.WriteLine($"    protected Abstract{project.Name}(String name) {{");
                sw.WriteLine($"        multiInstanceName = name;");
                if (written)
                    sw.WriteLine("        var suffix = name.isEmpty() ? name : \"__\" + name;");
                foreach (Table table in module.Tables.Values)
                {
                    if (project.GenTables.Contains(table.Gen) && false == table.IsRocks)
                    {
                        sw.WriteLine($"        _{table.Name} = new {table.FullName}(suffix);");
                    }
                }
                sw.WriteLine($"    }}");
            }
        }

        void ModuleGen(StreamWriter sw)
        {
            DefineZezeTables(sw);

            sw.WriteLine();
            sw.WriteLine($"    public final {project.Solution.Name}.App App;");
            sw.WriteLine();

            sw.WriteLine($"    public AbstractModule({project.Solution.Name}.App app) {{");
            sw.WriteLine("        App = app;");
            sw.WriteLine("        Register();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void Register() {");
            sw.WriteLine("        // register protocol factory and handles");
            RegisterProtocols(sw);
            sw.WriteLine("        // register table");
            RegisterZezeTables(sw);
            sw.WriteLine("        // register servlet");
            bool writtenHeader = true;
            RegisterHttpServlet(sw, ref writtenHeader);
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void UnRegister() {");
            sw.WriteLine("        // unregister protocol factory and handles");
            UnRegisterProtocols(sw);
            sw.WriteLine("        // unregister table");
            UnRegisterZezeTables(sw);
            sw.WriteLine("        // unregister servlet");
            writtenHeader = true;
            UnRegisterHttpServlet(sw, ref writtenHeader);
            sw.WriteLine("    }");
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
            if (sw == null)
                return;

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
            sw.WriteLine();
            sw.WriteLine($"    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();");
            sw.WriteLine($"    @Override public void lock() {{ __thisLock.lock(); }}");
            sw.WriteLine($"    @Override public void unlock() {{ __thisLock.unlock(); }}");
            sw.WriteLine($"    @Override public java.util.concurrent.locks.Lock getLock() {{ return __thisLock; }}");
            if (!string.IsNullOrEmpty(module.WebPathBase))
                sw.WriteLine($"    @Override public String getWebPathBase() {{ return \"{module.WebPathBase}\";}}");
            // declare enums
            GenEnums(sw);
            GenAbstractProtocolHandles(sw);
            GenAbstractHttpHandles(sw);
            ModuleGen(sw);
            sw.WriteLine("}");
        }

        public void GenAbstractHttpHandles(StreamWriter sw)
        {
            Service serv = module.ReferenceService;
            if (serv == null)
                return;

            if ((serv.HandleFlags & Program.HandleServletFlag) == 0)
                return;

            var written = false;
            foreach (var s in module.Servlets.Values)
            {
                if (!written)
                {
                    written = true;
                    sw.WriteLine();
                }
                sw.WriteLine($"    protected abstract void OnServlet{s.Name}(Zeze.Netty.HttpExchange x) throws Exception;");
            }

            foreach (var s in module.ServletStreams.Values)
            {
                if (!written)
                {
                    written = true;
                    sw.WriteLine();
                }
                sw.WriteLine($"    protected abstract void OnServletBeginStream{s.Name}(Zeze.Netty.HttpExchange x, long from, long to, long size) throws Exception;");
                sw.WriteLine($"    protected abstract void OnServletStreamContent{s.Name}(Zeze.Netty.HttpExchange x, io.netty.handler.codec.http.HttpContent c) throws Exception;");
                sw.WriteLine($"    protected abstract void OnServletEndStream{s.Name}(Zeze.Netty.HttpExchange x) throws Exception;");
            }
        }

        public void UnRegisterHttpServlet(StreamWriter sw, ref bool writtenHeader)
        {
            Service serv = module.ReferenceService;
            if (serv == null || (serv.HandleFlags & Program.HandleServletFlag) == 0)
                return;

            var httpVar = writtenHeader ? "App.HttpServer" : "httpServer";

            if (module.Servlets.Count > 0 || module.ServletStreams.Count > 0)
            {
                if (!writtenHeader)
                {
                    writtenHeader = true;
                    sw.WriteLine();
                    sw.WriteLine("    public void UnRegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {");
                }
            }

            foreach (var s in module.Servlets.Values)
            {
                var path = module.WebPathBase.Length > 0 ? module.WebPathBase + s.Name : "/" + module.Path("/", s.Name);
                sw.WriteLine($"        {httpVar}.removeHandler(\"{path}\");");
            }

            foreach (var s in module.ServletStreams.Values)
            {
                var path = module.WebPathBase.Length > 0 ? module.WebPathBase + s.Name : "/" + module.Path("/", s.Name);
                sw.WriteLine($"        {httpVar}.removeHandler(\"{path}\");");
            }
        }

        public void RegisterHttpServlet(StreamWriter sw, ref bool writtenHeader)
        {
            Service serv = module.ReferenceService;
            if (serv == null || (serv.HandleFlags & Program.HandleServletFlag) == 0)
                return;

            var httpVar = writtenHeader ? "App.HttpServer" : "httpServer";

            if (module.Servlets.Count > 0 || module.ServletStreams.Count > 0)
            {
                if (!writtenHeader)
                {
                    writtenHeader = true;
                    sw.WriteLine();
                    sw.WriteLine("    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {");
                    sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(getClass());");
                }
                else if (!defReflect)
                {
                    defReflect = true;
                    sw.WriteLine("        var _reflect = new Zeze.Util.Reflect(getClass());");
                }
            }

            foreach (var s in module.Servlets.Values)
            {
                var path = module.WebPathBase.Length > 0 ? module.WebPathBase + s.Name : "/" + module.Path("/", s.Name);
                sw.WriteLine($"        {httpVar}.addHandler(\"{path}\", {s.MaxContentLength},");
                sw.WriteLine($"                _reflect.getTransactionLevel(\"OnServlet{s.Name}\", Zeze.Transaction.TransactionLevel.{s.TransactionLevel}),");
                sw.WriteLine($"                _reflect.getDispatchMode(\"OnServlet{s.Name}\", Zeze.Transaction.DispatchMode.Normal),");
                sw.WriteLine($"                this::OnServlet{s.Name});");
            }

            foreach (var s in module.ServletStreams.Values)
            {
                var path = module.WebPathBase.Length > 0 ? module.WebPathBase + s.Name : "/" + module.Path("/", s.Name);
                sw.WriteLine($"        {httpVar}.addHandler(\"{path}\",");
                sw.WriteLine($"                _reflect.getTransactionLevel(\"OnServletBeginStream{s.Name}\", Zeze.Transaction.TransactionLevel.{s.TransactionLevel}),");
                sw.WriteLine($"                _reflect.getDispatchMode(\"OnServletBeginStream{s.Name}\", Zeze.Transaction.DispatchMode.Direct),");
                sw.WriteLine($"                this::OnServletBeginStream{s.Name}, this::OnServletStreamContent{s.Name}, this::OnServletEndStream{s.Name});");
            }
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
