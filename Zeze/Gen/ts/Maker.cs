namespace Zeze.Gen.ts
{
    public class Maker
    {
        public Project Project { get;  }

        public Maker(Project project)
        {
            Project = project;
        }

        public void Make()
        {
            string genDir = Project.GenDir;

            using System.IO.StreamWriter sw = Program.OpenWriterNoPath(genDir, Project.Solution.Name + "/gen.ts");
            if (sw != null)
            {
                sw.WriteLine("// auto-generated");
                sw.WriteLine("/* eslint-disable camelcase, class-methods-use-this, lines-between-class-members, max-classes-per-file, new-cap, no-bitwise, no-plusplus, no-underscore-dangle, no-unused-vars, no-use-before-define, prettier/prettier */");
                sw.WriteLine("import { Zeze } from '../Zeze/zeze';");
                foreach (Types.Bean bean in Project.AllBeans.Values)
                {
                    new BeanFormatter(bean).Make(sw);
                }
                foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                {
                    new BeanKeyFormatter(beanKey).Make(sw);
                }
                foreach (Protocol protocol in Project.AllProtocols.Values)
                {
                    sw.WriteLine();
                    if (protocol is Rpc rpc)
                        new RpcFormatter(rpc).Make(sw);
                    else
                        new ProtocolFormatter(protocol).Make(sw);
                }
            }
            foreach (Module mod in Project.AllOrderDefineModules)
            {
                new ModuleFormatter(Project, mod, genDir).Make();
            }
            new App(Project, genDir).Make();
            /*
            foreach (Service ma in Project.Services.Values)
            {
                new ServiceFormatter(ma, genDir, srcDir).Make();
            }
            */
        }
    }
}
