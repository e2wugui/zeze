using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.lua
{
    public class ModuleFormatter
    {
        Project project;
        ModuleSpace module;
        string genDir;
        string srcDir;

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
            using System.IO.StreamWriter sw = (null == module.Parent)
                ? new System.IO.StreamWriter(System.IO.Path.Combine(genDir, module.Name + ".lua"), false, new UTF8Encoding(false))
                : module.Parent.OpenWriter(genDir, module.Name + ".lua");
            MakeGen(sw);
            sw.WriteLine();
            sw.WriteLine("return " + module.Name + "");
        }

        public void MakeGen(System.IO.StreamWriter sw)
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

        private const string ChunkNameRegisterProtocol = "REGISTER PROTOCOL";

        private void GenChunkByName(System.IO.StreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk)
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

        private void RegisterProtocol(System.IO.StreamWriter sw)
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
            if (null == module.Parent)
                return; // must be solution

            Zeze.Util.FileChunkGen fcg = new Util.FileChunkGen("-- ZEZE_FILE_CHUNK {{{", "-- ZEZE_FILE_CHUNK }}}");
            string fullDir = module.Parent.GetFullPath(srcDir);
            string fullFileName = System.IO.Path.Combine(fullDir, module.Name + "Impl.lua");
            if (fcg.LoadFile(fullFileName))
            {
                fcg.SaveFile(fullFileName, GenChunkByName);
            }
            else
            {
                System.IO.Directory.CreateDirectory(fullDir);
                using System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, new UTF8Encoding(false));

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
