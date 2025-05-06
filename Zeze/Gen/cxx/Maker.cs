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

        public void MakeCxx()
        {
            string projectBasedir = Project._GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;

            if (Project.IsNewVersionDir())
            {
                genDir = Project.GenDir;
                srcDir = Project.SrcDir;
            }
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            foreach (var bean in Project.AllBeans.Values)
            {
                new BeanFormatter(bean).Make(genDir);
            }
            foreach (var beanKey in Project.AllBeanKeys.Values)
            {
                new BeanKeyFormatter(beanKey).Make(genDir);
            }
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genDir);
                else
                    new ProtocolFormatter(protocol).Make(genDir);
            }
            foreach (Module mod in Project.AllOrderDefineModules)
            {
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
            }
            foreach (var m in Project.Services.Values)
            {
                new ServiceFormatter(m, genDir, srcDir).Make();
            }
            new App(Project, genDir, srcDir).Make();
        }

        public void Make()
        {
            string projectBasedir = Project._GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;

            if (Project.IsNewVersionDir())
            {
                genDir = Project.GenDir;
                srcDir = Project.SrcDir;
            }
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            {
                using StreamWriter sw = Project.Solution.OpenWriter(genDir, "App.h");
                if (sw != null)
                {
                    sw.WriteLine("// auto-generated");
                    sw.WriteLine();
                    foreach (var m in Project.Services.Values)
                        sw.WriteLine($"#include \"{Project.Solution.Name}/{m.Name}.h\"");
                    sw.WriteLine();
                    sw.WriteLine($"namespace {Project.Solution.Name}");
                    sw.WriteLine($"{{");
                    sw.WriteLine($"    class App");
                    sw.WriteLine($"    {{");
                    sw.WriteLine($"    public:");
                    sw.WriteLine($"        static App& Instance() {{ static App instance; return instance; }}");
                    sw.WriteLine();
                    foreach (var m in Project.Services.Values)
                        sw.WriteLine($"        {Project.Solution.Path("::", m.Name)} {m.Name};");
                    sw.WriteLine($"    }};");
                    sw.WriteLine($"}}");
                }
            }

            foreach (var m in Project.Services.Values)
            {
                new ServiceFormatter(m, genDir, srcDir).Make();
            }
        }
    }
}
