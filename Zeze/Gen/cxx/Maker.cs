﻿using System.IO;

namespace Zeze.Gen.cxx
{
    public class Maker
    {
        public Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        string BaseClass(Service s)
        {
            return s.Base.Length > 0 ? s.Base : "Zeze::Net::ToLuaService";
        }
 
        public void MakeCxx()
        {
            string projectBasedir = Project.GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;
            Program.AddGenDir(genDir);
            foreach (var bean in Project.AllBeans.Values)
            {
                new BeanFormatter(bean).Make(genDir);
            }
            foreach (var beanKey in Project.AllBeanKeys.Values)
            {
                new BeanKeyFormatter(beanKey).Make(genDir);
            }
        }

        public void Make()
        {
            string projectBasedir = Project.GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;
            Program.AddGenDir(genDir);
            {
                using StreamWriter sw = Project.Solution.OpenWriter(genDir, "App.h");
                sw.WriteLine("// auto-generated");
                sw.WriteLine();
                foreach (var m in Project.Services.Values)
                    sw.WriteLine($"#include \"{m.Name}.h\"");
                sw.WriteLine();
                sw.WriteLine($"namespace {Project.Solution.Name}");
                sw.WriteLine($"{{");
                sw.WriteLine($"    class App");
                sw.WriteLine($"    {{");
                sw.WriteLine($"    public:");
                sw.WriteLine($"        static App & Instance() {{ static App instance; return instance; }}");
                sw.WriteLine();
                foreach (var m in Project.Services.Values)
                    sw.WriteLine($"        {Project.Solution.Path("::", m.Name)} {m.Name};");
                sw.WriteLine($"    }};");
                sw.WriteLine($"}}");
            }

            foreach (var m in Project.Services.Values)
            {
                using StreamWriter sw = Project.Solution.OpenWriter(srcDir, $"{m.Name}.h", false);
                if (sw == null)
                    continue;
                //sw.WriteLine("// auto-generated");
                sw.WriteLine();
                sw.WriteLine($"#include \"ToLuaService.h\"");
                sw.WriteLine();
                sw.WriteLine($"namespace {Project.Solution.Name}");
                sw.WriteLine($"{{");
                sw.WriteLine($"    class {m.Name} : public {BaseClass(m)}");
                sw.WriteLine($"    {{");
                sw.WriteLine($"    public:");
                sw.WriteLine($"        {m.Name}() : {BaseClass(m)}(\"{m.Name}\") {{}}");
                sw.WriteLine($"    }};");
                sw.WriteLine($"}}");
            }
        }
    }
}
