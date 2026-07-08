using System.IO;

namespace Zeze.Gen.python
{
    public class Maker
    {
        public Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        public static string toPythonComment(string s, string prefix = "")
        {
            s = s.Trim();
            if (s.StartsWith("//"))
                s = "#" + s[2..];
            if (s.StartsWith("/*") && s.EndsWith("*/"))
                s = "\"\"\"" + s.Substring(2, s.Length - 4) + "\"\"\"";
            s = s.Replace("\r", "").Replace("\t", "    ");
            return string.IsNullOrEmpty(prefix) ? s : prefix + s.Replace("\n", "\n" + prefix);
        }

        public void Make()
        {
            var genCommonDir = string.IsNullOrEmpty(Project.CommonDir) ? Project.GenDir : Project.CommonDir;
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            // gen common
            Program.ParallelEach(Project.AllBeans.Values,
                bean => new BeanFormatter(bean).Make(genCommonDir, Project));
            Program.ParallelEach(Project.AllBeanKeys.Values,
                beanKey => new BeanKeyFormatter(beanKey).Make(genCommonDir, Project));
            Program.ParallelEach(Project.AllProtocols.Values, protocol =>
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir, Project);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir, Project);
            });
            new App(Project, genCommonDir).Make();
            Program.FlushOutputs();
            GenInit(genCommonDir);

            // gen project
            Program.ParallelEach(Project.AllOrderDefineModules,
                module => new ModuleFormatter(Project, module, genDir, srcDir).Make());
            Program.ParallelEach(Project.Services.Values,
                service => new ServiceFormatter(service, srcDir).Make());
        }

        public void GenInit(string baseDir)
        {
            {
                using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(baseDir, "__init__.py"));
                if (sw == null)
                    return;
                foreach (var path in Directory.GetDirectories(baseDir))
                {
                    var s = path.Replace('\\', '/');
                    var p = s.LastIndexOf('/');
                    if (p >= 0)
                        s = s[(p + 1)..];
                    if (s.StartsWith("_"))
                        continue;
                    sw.WriteLine($"from . import {s.Replace('/', '.')}");
                }
                foreach (var file in Directory.GetFiles(baseDir))
                {
                    if (!file.EndsWith(".py"))
                        continue;
                    var s = file[..^3].Replace('\\', '/');
                    var p = s.LastIndexOf('/');
                    if (p >= 0)
                        s = s[(p + 1)..];
                    if (s.StartsWith("_"))
                        continue;
                    sw.WriteLine($"from .{s} import {s}");
                }
            }
            foreach (var path in Directory.GetDirectories(baseDir))
                GenInit(path);
        }
    }
}
