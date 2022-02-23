using System;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.lua
{
    public class ModuleFormatter
    {
        readonly Project project;
        readonly ModuleSpace module;
        readonly string genDir;
        readonly string srcDir;

        public ModuleFormatter(Project project, ModuleSpace module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakeGen();
            MakeSrc();
        }

        public void MakeGen()
        {
            using StreamWriter sw = (module.Parent == null)
                ? Program.OpenStreamWriter(Path.Combine(genDir, module.Name + ".lua"))
                : module.Parent.OpenWriter(genDir, module.Name + ".lua");
            MakeGen(sw);
            sw.WriteLine();
            sw.WriteLine("return " + module.Name + "");
        }

        public void MakeGen(StreamWriter sw)
        {
            sw.WriteLine("-- auto-generated");
            sw.WriteLine();
            sw.WriteLine("local " + module.Name + " = {}");
            //sw.WriteLine("" + module.Name + ".ModuleId = " + module.Id);
            sw.WriteLine();
            // declare enums
            sw.WriteLine($"{module.Name}.ResultCode = {{");
            foreach (Types.Enum e in module.Enums)
            {
                sw.WriteLine($"    {e.Name} = {e.Value}, --{e.Comment}");
            }
            sw.WriteLine($"}}");
            foreach (var b in module.BeanKeys.Values)
            {
                BeanFormatter.Make(module.Name, b.Name, b.TypeId, b.Variables, b.Enums, sw);
            }
            sw.WriteLine();
            foreach (var b in module.Beans.Values)
            {
                BeanFormatter.Make(module.Name, b.Name, b.TypeId, b.Variables, b.Enums, sw);
            }
            sw.WriteLine();
            foreach (var p in module.Protocols.Values)
            {
                ProtocolFormatter.Make(module.Name, p, sw);
            }
        }

        const string ChunkNameRegisterProtocol = "REGISTER PROTOCOL";

        void GenChunkByName(StreamWriter writer, Util.FileChunkGen.Chunk chunk)
        {
            switch (chunk.Name)
            {
                case ChunkNameRegisterProtocol:
                    RegisterProtocol(writer);
                    break;
                default:
                    throw new Exception("unknown Chunk.Name=" + chunk.Name);
            }
        }

        void RegisterProtocol(StreamWriter sw)
        {
            Module realmod = (Module)module;
            Service serv = realmod.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = realmod.ReferenceService.HandleFlags;
                foreach (Protocol p in realmod.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags) != 0)
                        {
                            sw.WriteLine($"    Zeze.ProtocolHandles[{p.TypeId}] = {module.Name}Impl.Process{p.Name}Request");
                        }
                        continue;
                    }

                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                    {
                        sw.WriteLine($"    Zeze.ProtocolHandles[{p.TypeId}] = {module.Name}Impl.Process{p.Name}");
                    }
                }
            }
        }

        public void MakeSrc()
        {
            if (module.Parent == null)
                return; // must be solution

            Util.FileChunkGen fcg = new("-- ZEZE_FILE_CHUNK {{{", "-- ZEZE_FILE_CHUNK }}}");
            string fullDir = module.Parent.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, module.Name + "Impl.lua");
            if (fcg.LoadFile(fullFileName))
                fcg.SaveFile(fullFileName, GenChunkByName);
            else
            {
                FileSystem.CreateDirectory(fullDir);
                using StreamWriter sw = Program.OpenStreamWriter(fullFileName);

                sw.WriteLine($"local {module.Name}Impl = {{}}");
                sw.WriteLine();
                sw.WriteLine("local Zeze = require 'Zeze'");
                sw.WriteLine();
                sw.WriteLine($"function {module.Name}Impl:Init()");
                sw.WriteLine("    " + fcg.ChunkStartTag + " " + ChunkNameRegisterProtocol);
                RegisterProtocol(sw);
                sw.WriteLine("    " + fcg.ChunkEndTag + " " + ChunkNameRegisterProtocol);
                sw.WriteLine($"end");
                sw.WriteLine();
                Module realmod = (Module)module;
                Service serv = realmod.ReferenceService;
                if (serv != null)
                {
                    int serviceHandleFlags = realmod.ReferenceService.HandleFlags;
                    foreach (Protocol p in realmod.Protocols.Values)
                    {
                        if (p is Rpc rpc)
                        {
                            if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags) != 0)
                            {
                                sw.WriteLine($"function {module.Name}Impl.Process{p.Name}Request(rpc)");
                                sw.WriteLine($"    -- write rpc request handle here");
                                sw.WriteLine($"end");
                                sw.WriteLine($"");
                            }
                            continue;
                        }
                        if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                        {
                            sw.WriteLine($"function {module.Name}Impl.Process{p.Name}(p)");
                            sw.WriteLine($"    -- write handle here");
                            sw.WriteLine($"end");
                            sw.WriteLine($"");
                        }
                    }
                }
                sw.WriteLine();
                sw.WriteLine($"return {module.Name}Impl");
            }
        }
    }
}
