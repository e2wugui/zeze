using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Maker
    {
        public Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        public void Make()
        {
            string projectBasedir = Project.GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string genCommonDir = string.IsNullOrEmpty(Project.GenCommonRelativeDir)
                ? genDir : Path.Combine(projectDir, Project.GenCommonRelativeDir, "Gen");

            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;

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
            foreach (Module mod in Project.AllOrderDefineModules)
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
            foreach (Service ma in Project.Services.Values)
                new ServiceFormatter(ma, genDir, srcDir).Make();
            foreach (Table table in Project.AllTables.Values)
            {
                if (Project.GenTables.Contains(table.Gen))
                    new TableFormatter(table, genCommonDir).Make();
            }
            new Schemas(Project, genDir).Make();

            new App(Project, genDir, srcDir).Make();
        }
    }
}
