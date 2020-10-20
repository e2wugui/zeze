using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;

namespace Zeze.Gen.lua
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
            string projectBasedir = Project.Gendir;
            string projectDir = System.IO.Path.Combine(projectBasedir, Project.Name);
            string genDir = System.IO.Path.Combine(projectDir, "LuaGen");
            string srcDir = System.IO.Path.Combine(projectDir, "LuaSrc");

            if (System.IO.Directory.Exists(genDir))
                System.IO.Directory.Delete(genDir, true);

            HashSet<ModuleSpace> allRefModules = new HashSet<ModuleSpace>();
            foreach (Module mod in Project.AllModules)
                allRefModules.Add(mod);

            foreach (Types.Bean bean in Project.AllBeans)
            {
                allRefModules.Add(bean.Space);
            }
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys)
            {
                allRefModules.Add(beanKey.Space);
            }
            foreach (Protocol protocol in Project.AllProtocols)
            {
                if (protocol is Rpc rpc)
                    throw new Exception("lua. unsupport rpc.");
                allRefModules.Add(protocol.Space);
            }
            /*
            foreach (Service ma in Project.Services.Values)
            {
                new ServiceFormatter(ma, genDir, srcDir).Make();
            }
            */
            foreach (ModuleSpace mod in allRefModules)
            {
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
            }

            System.IO.Directory.CreateDirectory(genDir);
            string fullFileName = System.IO.Path.Combine(genDir, "ProtocolDispatcher.lua");
            using System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
            sw.WriteLine("-- auto-generated");
        }
    }
}
