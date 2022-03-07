using System.IO;

namespace Zeze.Gen.cs
{
    public class MakerZeze
    {
        public Project Project { get; }

        public MakerZeze(Project project)
        {
            Project = project;
        }

        public void Make()
        {
            string projectBasedir = Project.Gendir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = projectDir;
            string srcDir = projectDir;

            foreach (Types.Bean bean in Project.AllBeans.Values)
            {
                if (bean.IsRocks)
                    new rrcs.BeanFormatter(bean).Make(genDir);
                else
                    new BeanFormatter(bean).Make(genDir);
            }
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                new BeanKeyFormatter(beanKey).Make(genDir);
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genDir);
                else
                    new ProtocolFormatter(protocol).Make(genDir);
            }
            foreach (Table table in Project.AllTables.Values)
            {
                if (Project.GenTables.Contains(table.Gen))
                {
                    if (false == table.IsRocks)
                        new TableFormatter(table, genDir).Make();
                    // rocks table 不需要生成代码。
                }
            }
            // Module.ReferenceService
            // Service 指到内部服务，定义仅为了引用。
            // App 自定义。
        }
    }
}
