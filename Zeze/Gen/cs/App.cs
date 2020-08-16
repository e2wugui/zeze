using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class App
    {
        Project project;
        string genDir;
        string srcDir;

        public App(Project project, string genDir, string srcDir)
        {
            this.project = project;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakePartialGen();
            MakePartial();
        }

        public void MakePartialGen()
        {
            using System.IO.StreamWriter sw = project.Solution.OpenWriter(genDir, "App.cs");

            sw.WriteLine("");
            sw.WriteLine("namespace " + project.Solution.Path("."));
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public static App Instance { get; } = new App();");
            sw.WriteLine("");

            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("        public " + m.Path(".", m.Name) + " " + m.Path("_", m.Name) + " { get; } = new " + m.Path(".", m.Name) + "();");
                sw.WriteLine("");
            }

            foreach (Manager m in project.Managers.Values)
            {
                sw.WriteLine("        public " + m.FullName + " " + m.Name + " { get; } = new " + m.FullName + "();");
                sw.WriteLine("");
            }

            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartial()
        {
            using System.IO.StreamWriter sw = project.Solution.OpenWriter(srcDir, "App.cs", false);
            if (sw == null)
                return;

            sw.WriteLine("");
            sw.WriteLine("namespace " + project.Solution.Path("."));
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        // 在这里定义你的全局变量吧");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
