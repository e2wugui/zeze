using System.IO;

namespace Zeze.Gen.cs
{
    public class App
    {
        readonly Project project;
        readonly string genDir;
        readonly string srcDir;

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
            using StreamWriter sw = project.Solution.OpenWriter(genDir, "App.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("using System.Collections.Generic;");
            sw.WriteLine();
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App : Zeze.AppBase");
            sw.WriteLine("    {");
            sw.WriteLine("        public static App Instance { get; } = new App();");
            sw.WriteLine();
            sw.WriteLine("        public Zeze.Application Zz { get; set; }");
            sw.WriteLine();
            sw.WriteLine("        public Dictionary<string, Zeze.IModule> Modules { get; } = new Dictionary<string, Zeze.IModule>();");
            sw.WriteLine();

            foreach (Module m in project.AllOrderDefineModules)
            {
                var fullname = m.Path("_");
                sw.WriteLine($"        public {m.Path(".", $"Module{m.Name}")} {fullname} {{ get; set; }}");
                sw.WriteLine();
            }

            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        public " + m.FullName + " " + m.Name + " { get; set; }");
                sw.WriteLine();
            }

            sw.WriteLine("        public void CreateZeze(Zeze.Config config = null)");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            sw.WriteLine("                if (Zz != null)");
            sw.WriteLine("                    throw new System.Exception(\"Zz Has Created!\");");
            sw.WriteLine();
            sw.WriteLine($"                Zz = new Zeze.Application(\"{project.Solution.Name}\", config);");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void CreateService()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("                " + m.Name + " = new " + m.FullName + "(Zz);");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void CreateModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Module m in project.AllOrderDefineModules)
            {
                var fullname = m.Path("_");
                sw.WriteLine($"                {fullname} = new {m.Path(".", $"Module{m.Name}")}(this);");
                sw.WriteLine($"                {fullname}.Initialize(this);");
                sw.WriteLine($"                {fullname} = ({m.Path(".", $"Module{m.Name}")})ReplaceModuleInstance({fullname});");
                sw.WriteLine($"                Modules.Add({fullname}.FullName, {fullname});");
            }
            sw.WriteLine();
            sw.WriteLine("                Zz.Schemas = new " + project.Solution.Path(".", "Schemas") + "();");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void DestroyModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                var fullname = m.Path("_");
                sw.WriteLine("                " + fullname + " = null;");
            }
            sw.WriteLine("                Modules.Clear();");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void DestroyService()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("                " + m.Name + " = null;");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void DestroyZeze()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            sw.WriteLine("                Zz = null;");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void StartModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine("                " + m.Path("_") + ".Start(this);");
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (project.ModuleStartOrder.Contains(m))
                    continue;
                sw.WriteLine("                " + m.Path("_") + ".Start(this);");
            }
            sw.WriteLine();
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void StopModules()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (project.ModuleStartOrder.Contains(m))
                    continue; // Stop later
                sw.WriteLine("                " + m.Path("_") + "?.Stop(this);");
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var m = project.ModuleStartOrder[i];
                sw.WriteLine("                " + m.Path("_") + "?.Stop(this);");
            }
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void StartService()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("                " + m.Name + ".Start();");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void StopService()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("                " + m.Name + "?.Stop();");
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

            sw.WriteLine();
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start()");
            sw.WriteLine("        {");
            sw.WriteLine("            Create();");
            sw.WriteLine("            Zeze.StartAsync().Wait(); // 启动数据库");
            sw.WriteLine("            StartModules(); // 启动模块，装载配置什么的。");
            sw.WriteLine("            StartService(); // 启动网络");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void Stop()");
            sw.WriteLine("        {");
            sw.WriteLine("            StopService(); // 关闭网络");
            sw.WriteLine("            StopModules(); // 关闭模块，卸载配置什么的。");
            sw.WriteLine("            Zeze.Stop(); // 关闭数据库");
            sw.WriteLine("            Destroy();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
