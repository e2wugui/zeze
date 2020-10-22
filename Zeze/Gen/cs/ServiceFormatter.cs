using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class ServiceFormatter
    {
        Service service;
        string genDir;
        string srcDir;

        public ServiceFormatter(Service service, string genDir, string srcDir)
        {
            this.service = service;
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
            return service.Base.Length > 0 ? service.Base : "Zeze.Net.Service";
        }

        public void MakePartialInGen()
        {
            using System.IO.StreamWriter sw = service.Project.Solution.OpenWriter(genDir, service.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + service.Project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class " + service.Name + " : " + BaseClass());
            sw.WriteLine("    {");
            sw.WriteLine("        public " + service.Name + "(Zeze.Application zeze) : base(\"" + service.Name + "\", zeze)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartialInSrc()
        {
            using System.IO.StreamWriter sw = service.Project.Solution.OpenWriter(srcDir, service.Name + ".cs", false);
            if (null == sw)
                return;

            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + service.Project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class " + service.Name);
            sw.WriteLine("    {");
            sw.WriteLine("        // 重载需要的方法。");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
