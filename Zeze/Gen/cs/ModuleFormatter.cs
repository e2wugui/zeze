using System;
using System.Collections.Generic;
using System.Text;
using NLog.Layouts;

namespace Zeze.Gen.cs
{
    public class ModuleFormatter
    {
        Project project;
        Module module;
        string genDir;
        string srcDir;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakeInterface();
            MakePartialImplement();
            MakePartialImplementInGen();
        }

        public void MakePartialImplementInGen()
        {
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, "Module.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed  partial class Module : AbstractModule");
            sw.WriteLine("    {");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine("        private " + table.Name + " _" + table.Name + " = new " + table.Name + "();");
            }
            sw.WriteLine("");
            sw.WriteLine("        public Module(" + module.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("            // register protocol factory and handles");
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if (((rpc.HandleFlags & serviceHandleFlags) != 0)
                            || ((rpc.HandleFlags & serviceHandleFlags) == 0 || (rpc.HandleFlags & Program.HandleRpcTwoway) != 0))
                        {
                            sw.WriteLine($"            app.{serv.Name}.AddFactoryHandle({rpc.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                            sw.WriteLine("            {");
                            sw.WriteLine($"                Factory = () => new {rpc.Space.Path(".", rpc.Name)}(),");
                            if ((rpc.HandleFlags & serviceHandleFlags) != 0)
                                sw.WriteLine($"                HandleRequest = Zeze.Net.Service.MakeHandle<{rpc.Name}>(this, GetType().GetMethod(nameof(Process{rpc.Name}Request))),");
                            if ((rpc.HandleFlags & serviceHandleFlags) == 0 || (rpc.HandleFlags & Program.HandleRpcTwoway) != 0)
                            {
                                sw.WriteLine($"                HandleResponse = Zeze.Net.Service.MakeHandle<{rpc.Name}>(this, GetType().GetMethod(nameof(Process{rpc.Name}Response))),");
                                sw.WriteLine($"                HandleTimeout = Zeze.Net.Service.MakeHandle<{rpc.Name}>(this, GetType().GetMethod(nameof(Process{rpc.Name}Timeout))),");
                            }
                            sw.WriteLine("            });");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            app.{serv.Name}.AddFactoryHandle({p.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine( "            {");
                        sw.WriteLine($"                Factory = () => new {p.Space.Path(".", p.Name)}(),");
                        sw.WriteLine($"                HandleRequest = Zeze.Net.Service.MakeHandle<{p.Name}>(this, GetType().GetMethod(nameof(Process{p.Name}))),");
                        sw.WriteLine( "            });");
                    }
                }
            }
            sw.WriteLine("            // register table");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"            app.Zeze.AddTable(app.Zeze.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartialImplement()
        {
            using System.IO.StreamWriter sw = module.OpenWriter(srcDir, "Module.cs", false);

            if (null == sw)
                return;

            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class Module : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start(" + module.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void Stop(" + module.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine("");
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags) != 0)
                        {
                            sw.WriteLine("        public override int Process" + rpc.Name + "Request(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        if ((rpc.HandleFlags & serviceHandleFlags) == 0 || (rpc.HandleFlags & Program.HandleRpcTwoway) != 0)
                        {
                            sw.WriteLine("        public override int Process" + rpc.Name + "Response(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("            // 如果使用同步发送rpc请求，结果通过wait得到，不会触发这个异步回调");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                            sw.WriteLine("        public override int Process" + rpc.Name + "Timeout(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("            // 如果使用同步发送rpc请求，结果通过wait得到，不会触发这个异步回调");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine("        public override int Process" + p.Name + "(" + p.Name + " protocol)");
                        sw.WriteLine("        {");
                        sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                        sw.WriteLine("        }");
                        sw.WriteLine("");
                    }
                }
            }
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakeInterface()
        {
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, "AbstractModule.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public abstract class AbstractModule");
            sw.WriteLine("    {");
 
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags) != 0)
                        {
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Request(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        if ((rpc.HandleFlags & serviceHandleFlags) == 0 || (rpc.HandleFlags & Program.HandleRpcTwoway) != 0)
                        {
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Response(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Timeout(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine("        public abstract int Process" + p.Name + "(" + p.Name + " protocol);");
                        sw.WriteLine("");
                    }
                }
            }
 
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
