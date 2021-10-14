using System;
using System.Collections.Generic;
using System.Text;
using NLog.Layouts;

namespace Zeze.Gen.java
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
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, $"AbstractModule{module.Name}.java");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("package " + module.Path() + ";");
            sw.WriteLine("");
            sw.WriteLine($"public abstract class AbstractModule{module.Name} {{");
            sw.WriteLine($"    public static final int ModuleId = {module.Id};");
            sw.WriteLine("");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine("    private " + table.Name + " _" + table.Name + " = new " + table.Name + "();");
            }
            sw.WriteLine("");
            sw.WriteLine($"    public {project.Solution.Name}.App App;");
            sw.WriteLine("");
            // <-- begin old AbstractModule
            sw.WriteLine($"        @Override ");
            sw.WriteLine($"        public String getFullName() {{ return \"{module.Path()}\"; }}");
            sw.WriteLine($"        @Override ");
            sw.WriteLine($"        public String getName() {{ return \"{module.Name}\"; }}" );
            sw.WriteLine($"        @Override ");
            sw.WriteLine($"        public int getId() {{ return {module.Id}; }}");
            sw.WriteLine("");
            // declare enums
            foreach (Types.Enum e in module.Enums)
            {
                sw.WriteLine("        public final static int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (module.Enums.Count > 0)
            {
                sw.WriteLine("");
            }

            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Request(Zeze.Net.Protocol _p);");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine("        public abstract int Process" + p.Name + "(Zeze.Net.Protocol _p);");
                        sw.WriteLine("");
                    }
                }
            }
            // <-- end old AbstractModule
            sw.WriteLine($"    public Module{module.Name}({project.Solution.Name}.App app) {{");
            sw.WriteLine("        App = app;");
            sw.WriteLine("        // register protocol factory and handles");
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine("        {");
                        sw.WriteLine("            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();");
                        sw.WriteLine($"            App.{serv.Name}.AddFactoryHandle({rpc.TypeId}, ");
                        sw.WriteLine($"            factoryHandle.Factory = () -> new {rpc.Space.Path(".", rpc.Name)}();");
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                            sw.WriteLine($"            factoryHandle.Handle = (_p) -> Process{rpc.Name}Request(_p);");
                        if (p.NoProcedure)
                            sw.WriteLine($"            factoryHandle.NoProcedure = true;");
                        sw.WriteLine("        }");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine("        {");
                        sw.WriteLine("            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();");
                        sw.WriteLine($"            App.{serv.Name}.AddFactoryHandle({p.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine($"            factoryHandle.Factory = () -> new {p.Space.Path(".", p.Name)}(),");
                        sw.WriteLine($"            factoryHandle.Handle = (_p) -> Process{p.Name}(_p);");
                        if (p.NoProcedure)
                            sw.WriteLine($"            factoryHandle.NoProcedure = true,");
                        sw.WriteLine( "       }");
                    }
                }
            }
            sw.WriteLine("        // register table");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"        App.Zeze.AddTable(App.Zeze.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
            sw.WriteLine("    }");
            sw.WriteLine("");
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void UnRegister() {");
            if (serv != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"        App.{serv.Name}.getFactorys().remove({rpc.TypeId});");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"        App.{serv.Name}.getFactorys().remove({p.TypeId});");
                    }
                }
            }
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen))
                    sw.WriteLine($"        App.Zeze.RemoveTable(App.Zeze.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
            sw.WriteLine("    }");
            sw.WriteLine("");
            sw.WriteLine("}");
        }

        public void MakePartialImplement()
        {
            using System.IO.StreamWriter sw = module.OpenWriter(srcDir, $"Module{module.Name}.java", false);

            if (null == sw)
                return;

            sw.WriteLine("package " + module.Path() + ";");
            sw.WriteLine("");
            sw.WriteLine($"public class Module{module.Name} extends AbstractModule{module.Name} {{");
            sw.WriteLine("    public void Start(" + project.Solution.Name + ".App app) {");
            sw.WriteLine("    }");
            sw.WriteLine("");
            sw.WriteLine("    public void Stop(" + project.Solution.Name + ".App app) {");
            sw.WriteLine("    }");
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
                            sw.WriteLine("    @Override");
                            sw.WriteLine("    public int Process" + rpc.Name + "Request(Zeze.Net.Protocol _r) {");
                            sw.WriteLine($"        var r = ({rpc.ShortNameIf(module)})_r;");
                            sw.WriteLine($"        return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("    }");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine("    @Override");
                        sw.WriteLine("    public int Process" + p.Name + "(Zeze.Net.Protocol _p) {");
                        sw.WriteLine($"        var p = ({p.ShortNameIf(module)})_p;");
                        sw.WriteLine("        return Zeze.Transaction.Procedure.NotImplement;");
                        sw.WriteLine("    }");
                        sw.WriteLine("");
                    }
                }
            }
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
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
