using System;
using System.Collections.Generic;
using System.IO;

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

        Zeze.Util.FileChunkGen FileChunkGen;

        public bool GenEmptyProtocolHandles(StreamWriter sw, bool shortIf = true)
        {
            var projectFlags = Program.HandleCSharpFlags;
            if (project.ClientScript)
                projectFlags |= Program.HandleScriptClientFlag;

            bool written = false;
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & projectFlags) != 0)
                        {
                            if (written)
                                sw.WriteLine();
                            written = true;
                            if (GenCpp)
                            {
                                sw.WriteLine($"    int64_t Module{moduleName}::Process{rpc.Name}Request(Zeze::Net::Protocol* _r) {{");
                                sw.WriteLine($"        return Zeze::ResultCode::NotImplement;");
                                sw.WriteLine("    }");
                            }
                            else
                            {
                                sw.WriteLine($"    virtual int64_t Process{rpc.Name}Request(Zeze::Net::Protocol* _r) override;");
                            }
                        }
                    }
                    else
                    {
                        if ((p.HandleFlags & serviceHandleFlags & projectFlags) != 0)
                        {
                            if (written)
                                sw.WriteLine();
                            written = true;
                            if (GenCpp)
                            {
                                sw.WriteLine($"    int64_t Module{moduleName}::Process{p.Name}(Zeze::Net::Protocol* _p) {{");
                                sw.WriteLine("        return Zeze::ResultCode::NotImplement;");
                                sw.WriteLine("    }");
                            }
                            else
                            {
                                sw.WriteLine($"    virtual int64_t Process{p.Name}(Zeze::Net::Protocol* _p) override;");
                            }
                        }
                    }
                }
            }
            return written;
        }

        private void NewH()
        {
            // new file
            string fullDir = module.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, $"Module{moduleName}.h");
            Zeze.Util.FileSystem.CreateDirectory(fullDir);
            using StreamWriter sw = Program.OpenStreamWriter(fullFileName);
            if (sw == null)
                return;

            sw.WriteLine("#pragma once");
            sw.WriteLine();
            sw.WriteLine(FileChunkGen.ChunkStartTag + " " + ChunkNameImport);
            ImportGen(sw);
            sw.WriteLine(FileChunkGen.ChunkEndTag + " " + ChunkNameImport);
            sw.WriteLine();

            var paths = module.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            sw.WriteLine();
            sw.WriteLine($"class Module{moduleName} : public AbstractModule {{");
            sw.WriteLine("public:");
            sw.WriteLine("    void Start();");
            sw.WriteLine("    void Stop();");
            sw.WriteLine();
            if (GenEmptyProtocolHandles(sw))
                sw.WriteLine();
            sw.WriteLine("    " + FileChunkGen.ChunkStartTag + " " + ChunkNameModuleGen);
            ConstructorGen(sw);
            sw.WriteLine("    " + FileChunkGen.ChunkEndTag + " " + ChunkNameModuleGen);
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }

        private void NewCpp()
        {
            // new file
            string fullDir = module.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, $"Module{moduleName}.cpp");
            Zeze.Util.FileSystem.CreateDirectory(fullDir);
            using StreamWriter sw = Program.OpenStreamWriter(fullFileName);
            if (sw == null)
                return;

            sw.WriteLine();
            sw.WriteLine(FileChunkGen.ChunkStartTag + " " + ChunkNameImport);
            ImportGen(sw);
            sw.WriteLine(FileChunkGen.ChunkEndTag + " " + ChunkNameImport);
            sw.WriteLine();

            var paths = module.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            sw.WriteLine();
            sw.WriteLine($"    void Module{moduleName}::Start() {{");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    void Module{moduleName}::Stop() {{");
            sw.WriteLine("    }");
            sw.WriteLine();
            if (GenEmptyProtocolHandles(sw))
                sw.WriteLine();
            sw.WriteLine("    " + FileChunkGen.ChunkStartTag + " " + ChunkNameModuleGen);
            ConstructorGen(sw);
            sw.WriteLine("    " + FileChunkGen.ChunkEndTag + " " + ChunkNameModuleGen);
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }

        private bool GenCpp;
        public void Make()
        {
            MakeInterface();
            {
                GenCpp = false;
                FileChunkGen = new Zeze.Util.FileChunkGen();
                string fullDir = module.GetFullPath(srcDir);
                string fullFileName = Path.Combine(fullDir, $"Module{moduleName}.h");
                if (FileChunkGen.LoadFile(fullFileName))
                {
                    FileChunkGen.SaveFile(fullFileName, GenChunkByName, GenBeforeChunkByName);
                    return;
                }
                NewH();
            }
            {
                GenCpp = true;
                FileChunkGen = new Zeze.Util.FileChunkGen();
                string fullDir = module.GetFullPath(srcDir);
                string fullFileName = Path.Combine(fullDir, $"Module{moduleName}.cpp");
                if (FileChunkGen.LoadFile(fullFileName))
                {
                    FileChunkGen.SaveFile(fullFileName, GenChunkByName, GenBeforeChunkByName);
                    return;
                }
                NewCpp();
            }
        }

        const string ChunkNameModuleGen = "GEN MODULE";
        const string ChunkNameImport = "IMPORT GEN";

        string GetHandleName(Protocol p)
        {
            if (p is Rpc rpc)
                return $"Process{rpc.Name}Request";
            return $"Process{p.Name}";
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
                if (chunk.State == Zeze.Util.FileChunkGen.State.Normal)
                {
                    foreach (var line in chunk.Lines)
                    {
                        int p = line.IndexOf("int64_t Process");
                        if (p >= 0)
                        {
                            int q = line.IndexOf('(', p + 15);
                            if (q >= 0)
                            {
                                if (protoMap.TryGetValue(line.Substring(p + 15, q - p - 15).Trim(), out var h))
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
                    if (GenCpp)
                    {
                        sw.WriteLine($"    int64_t Module{moduleName}::{hName}(Zeze::Net::Protocol* _r) {{");
                        sw.WriteLine($"        return Zeze::ResultCode::NotImplement;");
                        sw.WriteLine("    }");
                        sw.WriteLine("");
                    }
                    else
                    {
                        sw.WriteLine($"    virtual int64_t {hName}(Zeze::Net::Protocol* _r) override;");
                    }
                }
                else
                {
                    if (GenCpp)
                    {
                        sw.WriteLine($"    int64_t Module{moduleName}::{hName}(Zeze::Net::Protocol* _p) {{");
                        sw.WriteLine("        return Zeze::ResultCode::NotImplement;");
                        sw.WriteLine("    }");
                        sw.WriteLine("");
                    }
                    else
                    {
                        sw.WriteLine($"    virtual int64_t {hName}(Zeze::Net::Protocol* _p) override;");
                    }
                }
            }
        }

        void GenChunkByName(StreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk)
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

        void GenBeforeChunkByName(StreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk)
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
            if (GenCpp)
            {
                sw.WriteLine($"#include \"Module{moduleName}.h\"");
                sw.WriteLine($"#include \"Gen/{project.Solution.Name}/App.h\"");
            }
            else
            {
                sw.WriteLine($"#include \"Gen/{module.Path("/")}/AbstractModule.hpp\"");
            }
        }

        void ConstructorGen(StreamWriter sw)
        {
            if (GenCpp)
            {
                sw.WriteLine($"    Module{moduleName}::Module{moduleName}({project.Solution.Name}::App* app) : AbstractModule(app) {{");
                sw.WriteLine("    }");
            }
            else
            {
                sw.WriteLine($"    Module{moduleName}({project.Solution.Name}::App* app);");
            }
        }

        public List<Protocol> GetRegisterProtocols()
        {
            var projectFlags = Program.HandleCSharpFlags;
            if (project.ClientScript)
                projectFlags |= Program.HandleScriptClientFlag;

            var result = new List<Protocol>();
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        result.Add(p);
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & projectFlags))
                    {
                        result.Add(p);
                    }
                }
            }
            return result;
        }

        public void RegisterProtocols(StreamWriter sw, bool isFirst = true, string serviceVarName = null)
        {
            var projectFlags = Program.HandleCSharpFlags;
            if (project.ClientScript)
                projectFlags |= Program.HandleScriptClientFlag;
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App->{serv.Name}" : serviceVarName;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        string fullName = rpc.Space.Path("::", rpc.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;");
                        sw.WriteLine($"            factoryHandle.Factory = []() {{ return new {fullName}(); }};");
                        if ((rpc.HandleFlags & serviceHandleFlags & projectFlags) != 0)
                        {
                            sw.WriteLine($"            factoryHandle.Handle = std::bind(&AbstractModule::Process{rpc.Name}Request, this, std::placeholders::_1);");
                        }
                        sw.WriteLine($"            {serviceVar}->AddProtocolFactory({rpc.TypeId}LL, factoryHandle); // {rpc.Space.Id}, {rpc.Id}");
                        sw.WriteLine("        }");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & projectFlags))
                    {
                        string fullName = p.Space.Path("::", p.Name);
                        sw.WriteLine("        {");
                        sw.WriteLine($"            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;");
                        sw.WriteLine($"            factoryHandle.Factory = []() {{ return new {fullName}(); }};");
                        sw.WriteLine($"            factoryHandle.Handle = std::bind(&AbstractModule::Process{p.Name}, this, std::placeholders::_1);");
                        sw.WriteLine($"            {serviceVar}->AddProtocolFactory({p.TypeId}LL, factoryHandle); // {p.Space.Id}, {p.Id}");
                        sw.WriteLine("        }");
                    }
                }
            }
        }

        public void UnRegisterProtocols(StreamWriter sw, string serviceVarName = null)
        {
            /*
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
                        sw.WriteLine($"        {serviceVar}.GetFactorys().erase({rpc.TypeId}LL);");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"        {serviceVar}.GetFactorys().erase({p.TypeId}LL);");
                    }
                }
            }
            */
        }

        void ModuleGen(StreamWriter sw)
        {
            sw.WriteLine();
            sw.WriteLine($"    {project.Solution.Name}::App* App;");
            sw.WriteLine();

            sw.WriteLine($"    AbstractModule({project.Solution.Name}::App* app);");
            sw.WriteLine("    virtual void UnRegister() override;");
        }

        public void GenEnums(StreamWriter sw)
        {
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine($"    static const {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
        }

        public void GenAbstractProtocolHandles(StreamWriter sw)
        {
            var protocols = GetProcessProtocols();
            if (protocols.Count > 0)
                sw.WriteLine();
            foreach (Protocol p in protocols)
            {
                if (p is Rpc rpc)
                    sw.WriteLine($"    virtual int64_t Process{rpc.Name}Request(Zeze::Net::Protocol* _r) = 0;");
                else
                    sw.WriteLine($"    virtual int64_t Process{p.Name}(Zeze::Net::Protocol* _p) = 0;");
            }
        }

        public void MakeInterface()
        {
            // gen AbstractModule.cpp
            using StreamWriter swcpp = module.OpenWriter(genDir, "AbstractModule.cpp");
            if (swcpp == null)
                return;
            swcpp.WriteLine($"#include \"AbstractModule.hpp\"");
            swcpp.WriteLine($"#include \"Gen/{project.Solution.Name}/App.h\"");
            swcpp.WriteLine();
            var paths = module.Paths();
            foreach (var path in paths)
            {
                swcpp.WriteLine($"namespace {path} {{");
            }
            swcpp.WriteLine($"    const char* AbstractModule::ModuleName = \"{moduleName}\";");
            swcpp.WriteLine($"    const char* AbstractModule::ModuleFullName = \"{module.Path()}\";");
            swcpp.WriteLine();
            swcpp.WriteLine($"    AbstractModule::AbstractModule({project.Solution.Name}::App* app)");
            swcpp.WriteLine("    {");
            swcpp.WriteLine("        App = app;");
            swcpp.WriteLine("        // register protocol factory and handles");
            RegisterProtocols(swcpp);
            swcpp.WriteLine("    }");
            swcpp.WriteLine();
            swcpp.WriteLine("    void AbstractModule::UnRegister()");
            swcpp.WriteLine("    {");
            UnRegisterProtocols(swcpp);
            swcpp.WriteLine("    }");
            foreach (var path in paths)
            {
                swcpp.WriteLine($"}}");
            }

            using StreamWriter sw = module.OpenWriter(genDir, "AbstractModule.hpp");
            if (sw == null)
                return;

            sw.WriteLine("#pragma once");
            sw.WriteLine();
            sw.WriteLine("#include \"zeze/cxx/IModule.h\"");
            foreach (Protocol p in GetRegisterProtocols())
            {
                sw.WriteLine($"#include \"Gen/{p.Space.Path("/", p.Name + ".hpp")}\"");
            }
            sw.WriteLine();
            sw.WriteLine($"namespace {project.Solution.Name} {{");
                sw.WriteLine("    class App;");
            sw.WriteLine("}");
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            sw.WriteLine();
            if (module.Comment.Length > 0)
                sw.WriteLine(module.Comment);
            var classBase = (!project.EnableBase || string.IsNullOrEmpty(module.ClassBase)) ? "" : $": public {module.ClassBase} ";
            sw.WriteLine($"class AbstractModule {classBase} : public Zeze::IModule {{");
            sw.WriteLine($"public:");
            sw.WriteLine($"    static const int ModuleId = {module.Id};");
            sw.WriteLine($"    static const char* ModuleName;");
            sw.WriteLine($"    static const char* ModuleFullName;");
            sw.WriteLine();
            sw.WriteLine($"    virtual int GetId() const override {{ return ModuleId; }}");
            sw.WriteLine($"    virtual const char* GetName() const override {{ return ModuleName; }}");
            sw.WriteLine($"    virtual const char* GetFullName() const override {{ return ModuleFullName; }}");
            // declare enums
            GenEnums(sw);
            GenAbstractProtocolHandles(sw);
            ModuleGen(sw);
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }

        List<Protocol> GetProcessProtocols()
        {
            var result = new List<Protocol>();
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                var projectFlags = Program.HandleCSharpFlags;
                if (project.ClientScript)
                    projectFlags |= Program.HandleScriptClientFlag;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & projectFlags) != 0)
                            result.Add(rpc);
                    }
                    else
                    {
                        if ((p.HandleFlags & serviceHandleFlags & projectFlags) != 0)
                            result.Add(p);
                    }
                }
            }
            return result;
        }
    }
}
