using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class ManagerFormatter
    {
        Manager manager;
        string genDir;
        string srcDir;

        public ManagerFormatter(Manager manager, string genDir, string srcDir)
        {
            this.manager = manager;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakePartialInGen();
            MakePartialInSrc();
        }

        public string BaseClass()
        {
            return manager.Base.Length > 0 ? manager.Base : "Zeze.Net.Manager";
        }

        public void MakePartialInGen()
        {
            using System.IO.StreamWriter sw = manager.Project.Solution.OpenWriter(genDir, manager.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + manager.Project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class " + manager.Name + " : " + BaseClass());
            sw.WriteLine("    {");
            sw.WriteLine("        public " + manager.Name + "()");
            sw.WriteLine("        {");
            foreach (Protocol p in manager.GetAllProtocols())
            {
                sw.WriteLine("            this.Factorys.Add(" + p.Id + ", () => new " + p.Space.Path(".", p.Name) + "());");
            }
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartialInSrc()
        {
            using System.IO.StreamWriter sw = manager.Project.Solution.OpenWriter(srcDir, manager.Name + ".cs", false);
            if (null == sw)
                return;

            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + manager.Project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class " + manager.Name + " : " + BaseClass());
            sw.WriteLine("    {");
            sw.WriteLine("        // 重载需要的方法。");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
