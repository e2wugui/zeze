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
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, $"Module{module.Name}Gen.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public partial class Module{module.Name} : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine($"        public const int ModuleId = {module.Id};");
            sw.WriteLine("");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine("        private " + table.Name + " _" + table.Name + " = new " + table.Name + "();");
            }
            sw.WriteLine("");
            sw.WriteLine($"        public {project.Solution.Name}.App App {{ get; }}");
            sw.WriteLine("");
            sw.WriteLine($"        public Module{module.Name}({project.Solution.Name}.App app)");
            sw.WriteLine("        {");
            sw.WriteLine("            App = app;");
            sw.WriteLine("            // register protocol factory and handles");
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"            App.{serv.Name}.AddFactoryHandle({rpc.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine("            {");
                        sw.WriteLine($"                Factory = () => new {rpc.Space.Path(".", rpc.Name)}(),");
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                            sw.WriteLine($"                Handle = Zeze.Net.Service.MakeHandle<{rpc.Name}>(this, GetType().GetMethod(nameof(Process{rpc.Name}Request))),");
                        if (p.NoProcedure)
                            sw.WriteLine($"                NoProcedure = true,");
                        sw.WriteLine("            });");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            App.{serv.Name}.AddFactoryHandle({p.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine( "            {");
                        sw.WriteLine($"                Factory = () => new {p.Space.Path(".", p.Name)}(),");
                        sw.WriteLine($"                Handle = Zeze.Net.Service.MakeHandle<{p.Name}>(this, GetType().GetMethod(nameof(Process{p.Name}))),");
                        if (p.NoProcedure)
                            sw.WriteLine($"                NoProcedure = true,");
                        sw.WriteLine( "            });");
                    }
                }
            }
            sw.WriteLine("            // register table");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"            App.Zeze.AddTable(App.Zeze.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public override void UnRegister()");
            sw.WriteLine("        {");
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"            App.{serv.Name}.Factorys.TryRemove({rpc.TypeId}, out var _);");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            App.{serv.Name}.Factorys.TryRemove({p.TypeId}, out var _);");
                    }
                }
            }
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"            App.Zeze.RemoveTable(App.Zeze.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartialImplement()
        {
            using System.IO.StreamWriter sw = module.OpenWriter(srcDir, $"Module{module.Name}.cs", false);

            if (null == sw)
                return;

            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public partial class Module{module.Name} : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start(" + project.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void Stop(" + project.Solution.Name + ".App app)");
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
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine("        public override int Process" + rpc.Name + "Request(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
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
            sw.WriteLine("    public abstract class AbstractModule : Zeze.IModule");
            sw.WriteLine("    {");
            sw.WriteLine($"        public string FullName => \"{module.Path()}\";");
            sw.WriteLine($"        public string Name => \"{module.Name}\";");
            sw.WriteLine($"        public int Id => {module.Id};");
            sw.WriteLine($"        public abstract void UnRegister();");

            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Request(" + rpc.Name + " rpc);");
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
