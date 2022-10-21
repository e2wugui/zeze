using System.IO;

namespace Zeze.Gen.confcs
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
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir);
            string genCommonDir = string.IsNullOrEmpty(Project.GenCommonRelativeDir)
                ? genDir : Path.Combine(projectDir, Project.GenCommonRelativeDir);

            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;

            Program.AddGenDir(genDir);

            foreach (Types.Bean bean in Project.AllBeans.Values)
                new BeanFormatter(Project, bean).Make(genCommonDir);
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                new cs.BeanKeyFormatter(beanKey).Make(genCommonDir);

            // conf+cs 的ModuleFormatter仅生成enum。
            foreach (Module mod in Project.AllOrderDefineModules)
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
        }
    }
}
