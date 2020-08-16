﻿using System;
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
            using System.IO.StreamWriter sw = module.OpenWriter(genDir, module.Name + ".cs"); // 正式版不覆盖

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
            foreach (Protocol p in module.Protocols.Values)
            { 
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
            foreach (Protocol p in module.Protocols.Values)
            {
            }
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
