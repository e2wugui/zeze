using System.Collections.Generic;
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
            var genCommonDir = string.IsNullOrEmpty(Project.CommonDir) ? Project.GenDir : Project.CommonDir;
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            // gen common（各实体文件互相独立，并行生成）
            Program.ParallelEach(Project.AllBeans.Values,
                bean => new BeanFormatter(bean).Make(genCommonDir, Project));
            Program.ParallelEach(Project.AllBeanKeys.Values,
                beanKey => new BeanKeyFormatter(beanKey).Make(genCommonDir));
            Program.ParallelEach(Project.AllProtocols.Values, protocol =>
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir);
            });

            // gen project
            Program.ParallelEach(Project.AllOrderDefineModules,
                mod => new ModuleFormatter(Project, mod, genDir, srcDir).Make());
            // 收集需要生成类映射的Bean（mod.MappingClassBeans 已在 Compile 期填好、此处只读，串行收集即可）
            var MappingClassBeans = new HashSet<Bean>();
            foreach (Module mod in Project.AllOrderDefineModules)
            {
                foreach (var bean in mod.MappingClassBeans)
                    MappingClassBeans.Add(bean);
            }
            Program.ParallelEach(Project.Services.Values,
                ma => new ServiceFormatter(ma, genDir, srcDir).Make());
            Program.ParallelEach(Project.AllTables.Values, table =>
            {
                if (Project.GenTables.Contains(table.Gen))
                    new TableFormatter(table, genCommonDir).Make();
            });
            new Schemas(Project, genDir).Make();

            new App(Project, genDir, srcDir).Make();

            if (Project.MappingClass)
            {
                Program.ParallelEach(MappingClassBeans,
                    bean => new MappingClass(genDir, srcDir, bean).Make());
            }
        }
    }
}
