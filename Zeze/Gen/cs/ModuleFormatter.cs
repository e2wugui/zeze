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

            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path("."));
            sw.WriteLine("{");
            sw.WriteLine("    public sealed  partial class " + module.Name + " : I" + module.Name);
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
            sw.WriteLine("namespace " + module.Path("."));
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class " + module.Name + " : I" + module.Name);
            sw.WriteLine("    {");
            if (module.ReferenceManager != null)
            {
                int managerHandleFlags = module.ReferenceManager.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & Program.HandleServerFlag) != 0)
                        {
                            sw.WriteLine("        public void On" + rpc.Name + "Server(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        if ((rpc.HandleFlags & Program.HandleClientFlag) != 0)
                        {
                            sw.WriteLine("        public void On" + rpc.Name + "Client(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                            sw.WriteLine("        public void On" + rpc.Name + "Timeout(" + rpc.Name + " rpc)");
                            sw.WriteLine("        {");
                            sw.WriteLine("        }");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & managerHandleFlags))
                    {
                        sw.WriteLine("        public void On" + p.Name + "(" + p.Name + " protocol)");
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
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, "I" + module.Name + ".cs");

            sw.WriteLine("");
            sw.WriteLine("namespace " + module.Path("."));
            sw.WriteLine("{");
            sw.WriteLine("    public interface I" + module.Name);
            sw.WriteLine("    {");
 
            if (module.ReferenceManager != null)
            {
                int managerHandleFlags = module.ReferenceManager.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & Program.HandleServerFlag) != 0)
                        {
                            sw.WriteLine("        public void On" + rpc.Name + "Server(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        if ((rpc.HandleFlags & Program.HandleClientFlag) != 0)
                        {
                            sw.WriteLine("        public void On" + rpc.Name + "Client(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                            sw.WriteLine("        public void On" + rpc.Name + "Timeout(" + rpc.Name + " rpc);");
                            sw.WriteLine("");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & managerHandleFlags))
                    {
                        sw.WriteLine("        public void On" + p.Name + "(" + p.Name + " protocol);");
                        sw.WriteLine("");
                    }
                }
            }
 
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
