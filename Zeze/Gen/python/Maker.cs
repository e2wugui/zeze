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

        public static string toPythonComment(string s, bool prefixSpace = false)
        {
            if (s.StartsWith("//"))
                return (prefixSpace ? " #" : "#") + s[2..];
            if (s.StartsWith("/*") && s.EndsWith("*/"))
                return (prefixSpace ? " \"\"\"" : "\"\"\"") + s.Substring(2, s.Length - 4) + "\"\"\"";
            return s;
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
                new BeanFormatter(bean).Make(genCommonDir);
            foreach (BeanKey beanKey in Project.AllBeanKeys.Values)
                new BeanKeyFormatter(beanKey).Make(genCommonDir);
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir);
            }

            // gen project
            foreach (Module module in Project.AllOrderDefineModules)
                new ModuleFormatter(Project, module, genDir, srcDir).Make();
            foreach (Service service in Project.Services.Values)
                new ServiceFormatter(service, genDir, srcDir).Make();

            new App(Project, genDir, srcDir).Make();
        }
    }
}
