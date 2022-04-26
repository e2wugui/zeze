using System.IO;

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
 
        public void Make()
        {
            string projectBasedir = Project.GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = projectDir;

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
                using StreamWriter sw = Project.Solution.OpenWriter(genDir, $"{m.Name}.h", false);
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
