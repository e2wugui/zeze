using System.Collections.Generic;
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
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;
            var genCommonDir = Project.GenDir;
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            var host = "127.0.0.1";
            var port = 4545;
            var hotDistribute = new HotDistribute(host, port, Project, genCommonDir);
            // gen common
            foreach (Bean bean in Project.AllBeans.Values)
            {
                hotDistribute.GenBean(bean);
            }
            foreach (BeanKey beanKey in Project.AllBeanKeys.Values)
            {
                new BeanKeyFormatter(beanKey).Make(genCommonDir);
            }
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir);
            }

            // gen project
            var MappingClassBeans = new HashSet<Bean>();
            foreach (Module mod in Project.AllOrderDefineModules)
            {
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
                // 收集需要生成类映射的Bean。
                foreach (var bean in mod.MappingClassBeans)
                    MappingClassBeans.Add(bean);
            }
            foreach (Service ma in Project.Services.Values)
                new ServiceFormatter(ma, genDir, srcDir).Make();
            foreach (Table table in Project.AllTables.Values)
            {
                if (Project.GenTables.Contains(table.Gen))
                    new TableFormatter(table, genCommonDir).Make();
            }
            new Schemas(Project, genDir).Make();

            new App(Project, genDir, srcDir).Make();

            if (Project.MappingClass)
            {
                foreach (var bean in MappingClassBeans)
                {
                    new MappingClass(genDir, srcDir, bean).Make();
                }
            }
        }
    }
}
