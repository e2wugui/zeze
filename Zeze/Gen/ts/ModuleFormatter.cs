using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Util;

namespace Zeze.Gen.ts
{
    public class ModuleFormatter
    {
        Project project;
        Module module;
        string genDir;

        public ModuleFormatter(Project project, Module module, string genDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
        }

        private const string ChunkNameRegisterProtocol = "REGISTER PROTOCOL";
        private const string ChunkNameImport = "IMPORT GEN";
        private const string ChunkNameModuleEnums = "MODULE ENUMS";

        private void GenChunkByName(System.IO.StreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk)
        {
            switch (chunk.Name)
            {
                case ChunkNameRegisterProtocol:
                    RegisterProtocol(writer);
                    break;
                case ChunkNameImport:
                    Import(writer);
                    break;
                case ChunkNameModuleEnums:
                    PrintModuleEnums(writer);
                    break;
                default:
                    throw new Exception("unknown Chunk.Name=" + chunk.Name);
            }
        }

        private void Import(System.IO.StreamWriter sw)
        {
            sw.WriteLine("import { Zeze } from \"zeze\"");
            var needp = NeedRegisterProtocold();
            if (needp.Count > 0)
            {
                StringBuilder importp = new StringBuilder();
                foreach (var p in needp)
                {
                    importp.Append(p.Space.Path("_", p.Name)).Append(", ");
                }
                sw.WriteLine("import { " + importp.ToString() + "} from \"gen\"");
            }
            sw.WriteLine("import { demo_App } from \"demo/App\"");
        }

        private List<Protocol> NeedRegisterProtocold()
        {
            List<Protocol> need = new List<Protocol>();
            Service serv = module.ReferenceService;
            if (serv == null)
                return need;

            int serviceHandleFlags = module.ReferenceService.HandleFlags;
            foreach (Protocol p in module.Protocols.Values)
            {
                if (p is Rpc rpc)
                {
                    // rpc 总是需要注册.
                    need.Add(p);
                    continue;
                }
                if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                {
                    need.Add(p);
                }
            }
            return need;
        }

        private void RegisterProtocol(System.IO.StreamWriter sw)
        {
            Service serv = module.ReferenceService;
            if (serv == null)
                return;

            int serviceHandleFlags = module.ReferenceService.HandleFlags;
            foreach (Protocol p in module.Protocols.Values)
            {
                string fullName = p.Space.Path("_", p.Name);
                string factory = "() => { return new " + fullName + "(); }";
                if (p is Rpc rpc)
                {
                    string handle = ((rpc.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags) != 0)
                        ? "this.Process" + rpc.Name + "Request.bind(this)" : "null";
                    sw.WriteLine($"        app.{serv.Name}.FactoryHandleMap.set({rpc.TypeId}n, new Zeze.ProtocolFactoryHandle({factory}, {handle}));");
                    continue;
                }
                if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                {
                    string handle = "this.Process" + p.Name + ".bind(this)";
                    sw.WriteLine($"        app.{serv.Name}.FactoryHandleMap.set({p.TypeId}n, new Zeze.ProtocolFactoryHandle({factory}, {handle}));");
                }
            }
        }

        private void PrintModuleEnums(System.IO.StreamWriter sw)
        {
            foreach (Types.Enum e in module.Enums)
            {
                sw.WriteLine($"    static readonly {e.Name} = {e.Value}; {e.Comment}");
            }
            if (module.Enums.Count > 0 )
            {
                sw.WriteLine();
            }
        }

        public void Make()
        {
            FileChunkGen fcg = new FileChunkGen();
            string fullDir = module.GetFullPath(genDir);
            string fullFileName = System.IO.Path.Combine(fullDir, $"Module{module.Name}.ts");
            if (fcg.LoadFile(fullFileName))
            {
                fcg.SaveFile(fullFileName, GenChunkByName);
            }
            else
            {
                // new file
                FileSystem.CreateDirectory(fullDir);
                using System.IO.StreamWriter sw = Program.OpenStreamWriter(fullFileName);
                sw.WriteLine();
                sw.WriteLine(fcg.ChunkStartTag + " " + ChunkNameImport);
                Import(sw);
                sw.WriteLine(fcg.ChunkEndTag + " " + ChunkNameImport);
                sw.WriteLine();
                sw.WriteLine("export class " + module.Path("_") + " {");
                sw.WriteLine("        " + fcg.ChunkStartTag + " " + ChunkNameModuleEnums);
                PrintModuleEnums(sw);
                sw.WriteLine("        " + fcg.ChunkEndTag + " " + ChunkNameModuleEnums);
                sw.WriteLine("    public constructor(app: " + module.Solution.Name + "_App) {");
                sw.WriteLine("        " + fcg.ChunkStartTag + " " + ChunkNameRegisterProtocol);
                RegisterProtocol(sw);
                sw.WriteLine("        " + fcg.ChunkEndTag + " " + ChunkNameRegisterProtocol);
                sw.WriteLine("    }");
                sw.WriteLine();
                sw.WriteLine("    public Start(app: " + module.Solution.Name + "_App): void {");
                sw.WriteLine("    }");
                sw.WriteLine();
                sw.WriteLine("    public Stop(app: " + module.Solution.Name + "_App): void {");
                sw.WriteLine("    }");
                sw.WriteLine();
                if (module.ReferenceService != null)
                {
                    int serviceHandleFlags = module.ReferenceService.HandleFlags;
                    foreach (Protocol p in module.Protocols.Values)
                    {
                        string fullName = p.Space.Path("_", p.Name);
                        if (p is Rpc rpc)
                        {
                            if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags) != 0)
                            {
                                sw.WriteLine("    public Process" + rpc.Name + "Request(rpc: " + fullName + "): number {");
                                sw.WriteLine("        return 0;");
                                sw.WriteLine("    }");
                                sw.WriteLine();
                            }
                            continue;
                        }
                        if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                        {
                            sw.WriteLine("    public Process" + p.Name + "(protocol: " + fullName + "): number {");
                            sw.WriteLine("        return 0;");
                            sw.WriteLine("    }");
                            sw.WriteLine();
                        }
                    }
                }
                sw.WriteLine("}");
            }
        }
    }
}
