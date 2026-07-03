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
            var genCommonDir = string.IsNullOrEmpty(Project.CommonDir) ? Project.GenDir : Project.CommonDir;
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;

            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            Program.ParallelEach(Project.AllBeans.Values,
                bean => new BeanFormatter(Project, bean).Make(genCommonDir));
            Program.ParallelEach(Project.AllBeanKeys.Values,
                beanKey => new cs.BeanKeyFormatter(beanKey).Make(genCommonDir));
            Program.ParallelEach(Project.AllProtocols.Values, protocol =>
            {
                if (protocol is Rpc rpc)
                    new cs.RpcFormatter(rpc).Make(genCommonDir, true);
                else
                    new cs.ProtocolFormatter(protocol).Make(genCommonDir, true);
            });

            // conf+cs 的ModuleFormatter仅生成enum。
            Program.ParallelEach(Project.AllOrderDefineModules,
                mod => new ModuleFormatter(Project, mod, genDir, srcDir).Make());
        }
    }
}
