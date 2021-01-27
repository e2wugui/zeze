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

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public static App Instance { get; } = new App();");
            sw.WriteLine("");

            sw.WriteLine("        public Zeze.Application Zeze { get; private set; }");
            sw.WriteLine("");

            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("        public " + m.Path(".", $"Module{m.Name}") + " " + m.Path("_") + " { get; private set; }");
                sw.WriteLine("");
            }

            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        public " + m.FullName + " " + m.Name + " { get; private set; }");
                sw.WriteLine("");
            }

            sw.WriteLine("        public void StartModules(Zeze.Config config = null)");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            sw.WriteLine("                if (null != Zeze)");
            sw.WriteLine("                    return;");
            sw.WriteLine("");
            sw.WriteLine("                Zeze = new Zeze.Application(config);");
            sw.WriteLine("");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("                " + m.Name + " = new " + m.FullName + "(Zeze);");
            }
            sw.WriteLine("");
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("                " + m.Path("_") + " = new " + m.Path(".", $"Module{m.Name}") + "(this);");
            }
            sw.WriteLine("");
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("                " + m.Path("_") + ".Start(this);");
            }
            sw.WriteLine("");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("                " + m.Name + ".Start();");
            }
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void StopModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            sw.WriteLine("                if (null == Zeze)");
            sw.WriteLine("                    return;");
            sw.WriteLine("");
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("                " + m.Path("_") + ".Stop(this);");
            }
            sw.WriteLine("");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("                " + m.Name + ".Close();");
                sw.WriteLine("                " + m.Name + " = null;");
            }
            sw.WriteLine("                Zeze = null;");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartial()
        {
            using System.IO.StreamWriter sw = project.Solution.OpenWriter(srcDir, "App.cs", false);
            if (sw == null)
                return;

            sw.WriteLine("");
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start()");
            sw.WriteLine("        {");
            sw.WriteLine("            StartModules(); // 启动模块，装载配置什么的。");
            sw.WriteLine("            Zeze.Start(); // 启动数据库");
            sw.WriteLine("            // 启动网络等等。");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void Stop()");
            sw.WriteLine("        {");
            sw.WriteLine("            // 关闭网络等等。");
            sw.WriteLine("            Zeze.Stop(); // 关闭数据库");
            sw.WriteLine("            StopModules(); // 关闭模块,，卸载配置什么的。");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
