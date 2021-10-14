using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;

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
            string projectBasedir = Project.Gendir;
            string projectDir = System.IO.Path.Combine(projectBasedir, Project.Name);
            string genDir = Project.ScriptDir.Length > 0
                ? System.IO.Path.Combine(projectDir, Project.ScriptDir) : projectDir;

            using System.IO.StreamWriter sw = Program.OpenWriterNoPath(genDir, "gen.ts");
            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("import { Zeze } from \"zeze\"");
            sw.WriteLine("");
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
                if (protocol is Rpc rpc)
                {
                   new RpcFormatter(rpc).Make(sw);
                }
                else
                    new ProtocolFormatter(protocol).Make(sw);
            }
            foreach (Module mod in Project.AllModules.Values)
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
