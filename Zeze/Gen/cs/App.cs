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
            sw.WriteLine("using System.Collections.Generic;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public static App Instance { get; } = new App();");
            sw.WriteLine("");
            sw.WriteLine("        public Zeze.Application Zeze { get; set; }");
            sw.WriteLine("");
            sw.WriteLine("        public Dictionary<string, Zeze.IModule> Modules { get; } = new Dictionary<string, Zeze.IModule>();");
            sw.WriteLine("");

            foreach (Module m in project.AllModules.Values)
            {
                var fullname = m.Path("_");
                sw.WriteLine($"        public {m.Path(".", $"Module{m.Name}")} {fullname} {{ get; set; }}");
                sw.WriteLine("");
            }

            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        public " + m.FullName + " " + m.Name + " { get; set; }");
                sw.WriteLine("");
            }

            sw.WriteLine("        public void Create(Zeze.Config config = null)");
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
            foreach (Module m in project.AllModules.Values)
            {
                var fullname = m.Path("_");
                sw.WriteLine("                " + fullname + " = new " + m.Path(".", $"Module{m.Name}") + "(this);");
                sw.WriteLine($"                {fullname} = ({m.Path(".", $"Module{m.Name}")})ReplaceModuleInstance({fullname});");
                sw.WriteLine($"                Modules.Add({fullname}.Name, {fullname});");
            }
            sw.WriteLine("");
            sw.WriteLine("                Zeze.Schemas = new " + project.Solution.Path(".", "Schemas") + "();");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void Destroy()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Module m in project.AllModules.Values)
            {
                var fullname = m.Path("_");
                sw.WriteLine("                " + fullname + " = null;");
            }
            sw.WriteLine("                Modules.Clear();");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("                " + m.Name + " = null;");
            }
            sw.WriteLine("                Zeze = null;");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void StartModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (var m in project.ModuleStartOrder)
            {
                sw.WriteLine("                " + m.Path("_") + ".Start(this);");
            }
            foreach (Module m in project.AllModules.Values)
            {
                if (project.ModuleStartOrder.Contains(m))
                    continue;
                sw.WriteLine("                " + m.Path("_") + ".Start(this);");
            }
            sw.WriteLine("");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void StopModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Module m in project.AllModules.Values)
            {
                sw.WriteLine("                " + m.Path("_") + ".Stop(this);");
            }
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void StartService()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("                " + m.Name + ".Start();");
            }
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void StopService()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("                " + m.Name + ".Stop();");
            }
            sw.WriteLine("            }");
            sw.WriteLine("        }");
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
            sw.WriteLine("        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)");
            sw.WriteLine("        {");
            sw.WriteLine("            return module;");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void Start()");
            sw.WriteLine("        {");
            sw.WriteLine("            Create();");
            sw.WriteLine("            StartModules(); // 启动模块，装载配置什么的。");
            sw.WriteLine("            Zeze.Start(); // 启动数据库");
            sw.WriteLine("            StartService(); // 启动网络");
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public void Stop()");
            sw.WriteLine("        {");
            sw.WriteLine("            StopService(); // 关闭网络");
            sw.WriteLine("            Zeze.Stop(); // 关闭数据库");
            sw.WriteLine("            StopModules(); // 关闭模块,，卸载配置什么的。");
            sw.WriteLine("            Destroy();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
