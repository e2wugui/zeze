using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.python
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

        public void GenEmptyProtocolHandles(StreamWriter sw)
        {
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (var p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine();
                            sw.WriteLine($"    def process_{rpc.Name}_request(self, r):");
                            sw.WriteLine($"        raise Exception(\"not implement for process_{rpc.Name}_request\")");
                        }
                    }
                    else
                    {
                        if ((p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine();
                            sw.WriteLine($"    def process_{p.Name}(self, r):");
                            sw.WriteLine($"        raise Exception(\"not implement for process_{p.Name}\")");
                        }
                    }
                }
            }
        }

        public void Make()
        {
            MakeInterface();
            FileChunkGen = new FileChunkGen();
            string fullDir = module.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, $"Module{moduleName}.py");
            if (FileChunkGen.LoadFile(fullFileName))
            {
                //TODO
                // FileChunkGen.SaveFile(fullFileName, GenChunkByName, GenBeforeChunkByName);
                return;
            }
            FileSystem.CreateDirectory(fullDir);
            using var sw = Program.OpenStreamWriter(fullFileName);
            if (sw == null)
                return;

            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import gen.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine($"class Module{moduleName}({module.Path(".", "AbstractModule")}):");
            ConstructorGen(sw);
            sw.WriteLine();
            sw.WriteLine("    def init(self):");
            sw.WriteLine("        pass");
            sw.WriteLine();
            sw.WriteLine("    def start(self):");
            sw.WriteLine("        pass");
            sw.WriteLine();
            sw.WriteLine("    def stop(self):");
            sw.WriteLine("        pass");
            sw.WriteLine();
            sw.WriteLine("    def start_last(self):");
            sw.WriteLine("        pass");
            GenEmptyProtocolHandles(sw);
        }

        const string ChunkNameModuleGen = "GEN MODULE";
        const string ChunkNameImport = "IMPORT GEN";

        string GetHandleName(Protocol p)
        {
            if (p is Rpc rpc)
                return $"process_{rpc.Name}_request";
            return $"process_{p.Name}";
        }

        void NewProtocolHandle(StreamWriter sw)
        {
            var handles = GetProcessProtocols();
            var protoMap = new Dictionary<string, Protocol>();
            foreach (var p in handles)
            {
                if (p is Rpc)
                    protoMap[p.Name + "_request"] = p;
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
                        int p = line.IndexOf("def process_", StringComparison.Ordinal);
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
            sw.WriteLine("    def __init__(self, app):");
            sw.WriteLine("        super().__init__(app)");
        }

        public void RegisterProtocols(StreamWriter sw)
        {
            var written = false;
            var serv = module.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (var p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        string fullName = rpc.Space.Path(".", rpc.Name);
                        sw.WriteLine($"        self.app.{serv.Name}.add_protocol_handle({rpc.TypeId}, \"{fullName}\", {fullName}, " +
                                     ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0 ? $"self.process_{rpc.Name}_request" : "None") +
                                     $")  # {rpc.Space.Id}, {rpc.Id}");
                        written = true;
                    }
                    else if ((p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                    {
                        string fullName = p.Space.Path(".", p.Name);
                        sw.WriteLine($"        self.app.{serv.Name}.add_protocol_handle({p.TypeId}, \"{fullName}\", {fullName}, " +
                                     $"self.process_{p.Name})  # {p.Space.Id}, {p.Id}");
                        written = true;
                    }
                }
            }
            if (!written)
                sw.WriteLine("        pass");
        }

        public void UnRegisterProtocols(StreamWriter sw)
        {
            var written = false;
            var serv = module.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (var p in module.Protocols.Values)
                {
                    if (p is Rpc || (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                    {
                        sw.WriteLine($"        self.app.{serv.Name}.remove_protocol_handle({p.TypeId})");
                        written = true;
                    }
                }
            }
            if (!written)
                sw.WriteLine("        pass");
        }

        void ModuleGen(StreamWriter sw)
        {
            sw.WriteLine();
            sw.WriteLine("    def __init__(self, app):");
            sw.WriteLine("        self.app = app");
            sw.WriteLine("        self.register()");
            sw.WriteLine();
            sw.WriteLine("    def register(self):");
            sw.WriteLine("        # register protocol factory and handles");
            RegisterProtocols(sw);
            sw.WriteLine();
            sw.WriteLine("    def unregister(self):");
            sw.WriteLine("        # unregister protocol factory and handles");
            UnRegisterProtocols(sw);
        }

        public void GenEnums(StreamWriter sw)
        {
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (var e in module.Enums)
            {
                sw.WriteLine(string.IsNullOrEmpty(e.Comment)
                    ? $"    {e.Name} = {e.Value}  {Maker.toPythonComment(e.Comment)}"
                    : $"    {e.Name} = {e.Value}");
            }
        }

        public void GenAbstractProtocolHandles(StreamWriter sw)
        {
            foreach (var p in GetProcessProtocols())
            {
                if (p is Rpc rpc)
                {
                    sw.WriteLine();
                    sw.WriteLine($"    def process_{rpc.Name}_request(self, r):");
                    sw.WriteLine($"        raise NotImplementedError(\"process_{rpc.Name}_request\")");
                }
                else
                {
                    sw.WriteLine();
                    sw.WriteLine($"    def process_{p.Name}(self, p):");
                    sw.WriteLine($"        raise NotImplementedError(\"process_{p.Name}\")");
                }
            }
        }

        public void MakeInterface()
        {
            using var sw = module.OpenWriter(genDir, "AbstractModule.py");
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import gen.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            var classBase = !project.EnableBase || string.IsNullOrEmpty(module.ClassBase) ? "" : $"({module.ClassBase})";
            sw.WriteLine($"# noinspection PyMethodMayBeStatic,PyPep8Naming");
            sw.WriteLine($"class AbstractModule{classBase}:");
            if (module.Comment.Length > 0)
                sw.WriteLine($"{Maker.toPythonComment(module.Comment, "    ")}");
            sw.WriteLine($"    ModuleId = {module.Id}");
            sw.WriteLine($"    ModuleName = \"{moduleName}\"");
            sw.WriteLine($"    ModuleFullName = \"{module.Path()}\"");
            sw.WriteLine();
            sw.WriteLine($"    def get_id(self):");
            sw.WriteLine($"        return AbstractModule.ModuleId");
            sw.WriteLine();
            sw.WriteLine($"    def get_name(self):");
            sw.WriteLine($"        return AbstractModule.ModuleName");
            sw.WriteLine();
            sw.WriteLine($"    def get_full_name(self):");
            sw.WriteLine($"        return AbstractModule.ModuleFullName");
            GenEnums(sw);
            GenAbstractProtocolHandles(sw);
            ModuleGen(sw);
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
