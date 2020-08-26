using System;
using System.Collections.Generic;
using System.Text;

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
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, module.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed  partial class " + module.Name + " : Abstract" + module.Name);
            sw.WriteLine("    {");
            foreach (Table table in module.Tables.Values)
            {
                sw.WriteLine("        private " + table.Name + " _" + table.Name + " = new " + table.Name + "();");
            }
            sw.WriteLine("");
            sw.WriteLine("        public " + module.Name + "(" + module.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            foreach (Table table in module.Tables.Values)
            {
                sw.WriteLine($"            app.Zeze.AddTable(\"{table.DatabaseName}\", _{table.Name});");
            }
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartialImplement()
        {
            using System.IO.StreamWriter sw = module.OpenWriter(srcDir, module.Name + ".cs", false);

            if (null == sw)
                return;

            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class " + module.Name + " : Abstract" + module.Name);
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
                        if ((serviceHandleFlags & Program.HandleServerFlag) != 0)
                        {
                            sw.WriteLine("        public override int Process" + rpc.Name + "Server(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        if ((serviceHandleFlags & Program.HandleClientFlag) != 0)
                        {
                            sw.WriteLine("        public override int Process" + rpc.Name + "Client(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                            sw.WriteLine("        public override int Process" + rpc.Name + "Timeout(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("            return Zeze.Transaction.Procedure.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags))
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
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, "Abstract" + module.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public abstract class Abstract" + module.Name);
            sw.WriteLine("    {");
 
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((serviceHandleFlags & Program.HandleServerFlag) != 0)
                        {
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Server(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        if ((serviceHandleFlags & Program.HandleClientFlag) != 0)
                        {
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Client(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                            sw.WriteLine("        public abstract int Process" + rpc.Name + "Timeout(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags))
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
