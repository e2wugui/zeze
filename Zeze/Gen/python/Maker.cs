using System.IO;
using Zeze.Gen.Types;

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
            string genDir = Path.Combine(Project.GenDir, Project.GenRelativeDir, "gen");
            string genCommonDir = string.IsNullOrEmpty(Project.GenCommonRelativeDir)
                ? genDir
                : Path.Combine(Project.GenDir, Project.GenCommonRelativeDir, "gen");
            string srcDir = string.IsNullOrEmpty(Project.ScriptDir)
                ? Project.GenDir
                : Path.Combine(Project.GenDir, Project.ScriptDir);

            Program.AddGenDir(genDir);

            // gen common
            foreach (Bean bean in Project.AllBeans.Values)
                new BeanFormatter(bean).Make(genCommonDir, Project);
            foreach (BeanKey beanKey in Project.AllBeanKeys.Values)
                new BeanKeyFormatter(beanKey).Make(genCommonDir, Project);
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir);
            }
            GenInit(genCommonDir);

            // gen project
            foreach (Module module in Project.AllOrderDefineModules)
                new ModuleFormatter(Project, module, genDir, srcDir).Make();
            foreach (Service service in Project.Services.Values)
                new ServiceFormatter(service, genDir, srcDir).Make();

            new App(Project, genDir, srcDir).Make();
        }

        public void GenInit(string baseDir)
        {
            {
                using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(baseDir, "__init__.py"));
                if (sw == null)
                    return;
                foreach (var file in Directory.GetFiles(baseDir))
                {
                    if (!file.EndsWith(".py"))
                        continue;
                    var s = file[..^3].Replace('\\', '/');
                    var p = s.LastIndexOf('/');
                    if (p >= 0)
                        s = s[(p + 1)..];
                    if (s == "__init__")
                        continue;
                    sw.WriteLine($"from .{s} import {s}");
                }
            }
            foreach (var path in Directory.GetDirectories(baseDir))
                GenInit(path);
        }
    }
}
