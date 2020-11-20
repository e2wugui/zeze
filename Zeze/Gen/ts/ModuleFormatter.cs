using System;
using System.Collections.Generic;
using System.Text;
using NLog.Layouts;

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
                default:
                    throw new Exception("unknown Chunk.Name=" + chunk.Name);
            }
        }

        private void Import(System.IO.StreamWriter sw)
        {
            sw.WriteLine("import { Zeze } from \"zeze.js\"");
            var needp = NeedRegisterProtocold();
            if (needp.Count > 0)
            {
                StringBuilder importp = new StringBuilder();
                foreach (var p in needp)
                {
                    importp.Append(p.Space.Path("_", p.Name)).Append(", ");
                }
                sw.WriteLine("import { " + importp.ToString() + "} from \"gen.js\"");
            }
            sw.WriteLine("import { demo_App } from \"demo/App.js\"");
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
                    if (((rpc.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags) != 0)
                        || ((rpc.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags) == 0
                            || (rpc.HandleFlags & Program.HandleRpcTwoway) != 0))
                    {
                        need.Add(p);
                    }
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
                    sw.WriteLine($"        app.{serv.Name}.FactoryHandleMap.set({rpc.TypeId}, new Zeze.ProtocolFactoryHandle({factory}, {handle}));");
                    continue;
                }
                if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                {
                    string handle = "this.Process" + p.Name + ".bind(this)";
                    sw.WriteLine($"        app.{serv.Name}.FactoryHandleMap.set({p.TypeId}, new Zeze.ProtocolFactoryHandle({factory}, {handle}));");
                }
            }
        }
        public void Make()
        {
            Zeze.Util.FileChunkGen fcg = new Util.FileChunkGen();
            string fullDir = module.GetFullPath(genDir);
            string fullFileName = System.IO.Path.Combine(fullDir, "Module.ts");
            if (fcg.LoadFile(fullFileName))
            {
                fcg.SaveFile(fullFileName, GenChunkByName);
            }
            else
            {
                // new file
                System.IO.Directory.CreateDirectory(fullDir);
                using System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
                sw.WriteLine();
                sw.WriteLine(fcg.ChunkStartTag + " " + ChunkNameImport);
                Import(sw);
                sw.WriteLine(fcg.ChunkEndTag + " " + ChunkNameImport);
                sw.WriteLine();
                sw.WriteLine("export class " + module.Path("_", "Module") + " {");
                sw.WriteLine("    public constructor(app: " + module.Solution.Name + "_App) {");
                sw.WriteLine("        " + fcg.ChunkStartTag + " " + ChunkNameRegisterProtocol);
                RegisterProtocol(sw);
                sw.WriteLine("        " + fcg.ChunkEndTag + " " + ChunkNameRegisterProtocol);
                sw.WriteLine("    }");
                sw.WriteLine("");
                sw.WriteLine("    public Start(app: " + module.Solution.Name + "_App): void {");
                sw.WriteLine("    }");
                sw.WriteLine("");
                sw.WriteLine("    public Stop(app: " + module.Solution.Name + "_App): void {");
                sw.WriteLine("    }");
                sw.WriteLine("");
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
                                sw.WriteLine("");
                            }
                            continue;
                        }
                        if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleScriptFlags))
                        {
                            sw.WriteLine("    public Process" + p.Name + "(protocol: " + fullName + "): number {");
                            sw.WriteLine("        return 0;");
                            sw.WriteLine("    }");
                            sw.WriteLine("");
                        }
                    }
                }
                sw.WriteLine("}");
            }
        }
    }
}
