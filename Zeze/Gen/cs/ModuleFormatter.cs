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
            sw.WriteLine("        // TODO define table here.");
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
            if (module.ReferenceManager != null)
            {
                int managerHandleFlags = module.ReferenceManager.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((managerHandleFlags & Program.HandleServerFlag) != 0)
                        {
                            sw.WriteLine("        public override void On" + rpc.Name + "Server(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        if ((managerHandleFlags & Program.HandleClientFlag) != 0)
                        {
                            sw.WriteLine("        public override void On" + rpc.Name + "Client(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                            sw.WriteLine("        public override void On" + rpc.Name + "Timeout(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & managerHandleFlags))
                    {
                        sw.WriteLine("        public override void On" + p.Name + "(" + p.Name + " protocol)");
                        sw.WriteLine("        {");
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
 
            if (module.ReferenceManager != null)
            {
                int managerHandleFlags = module.ReferenceManager.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((managerHandleFlags & Program.HandleServerFlag) != 0)
                        {
                            sw.WriteLine("        public abstract void On" + rpc.Name + "Server(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        if ((managerHandleFlags & Program.HandleClientFlag) != 0)
                        {
                            sw.WriteLine("        public abstract void On" + rpc.Name + "Client(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                            sw.WriteLine("        public abstract void On" + rpc.Name + "Timeout(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & managerHandleFlags))
                    {
                        sw.WriteLine("        public abstract void On" + p.Name + "(" + p.Name + " protocol);");
                        sw.WriteLine("");
                    }
                }
            }
 
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
