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

            System.IO.Directory.CreateDirectory(genDir);

            string metaFileName = System.IO.Path.Combine(genDir, "ZezeNetServiceMeta.lua");
            using System.IO.StreamWriter swMeta = new System.IO.StreamWriter(metaFileName, false, Encoding.UTF8);
            swMeta.WriteLine("-- auto-generated");
            swMeta.WriteLine("local meta = {}");
            swMeta.WriteLine("meta.beans = {}");
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys)
            {
                allRefModules.Add(beanKey.Space);
                BeanFormatter.MakeMeta(beanKey.TypeId, beanKey.Variables, swMeta);
            }
            foreach (Types.Bean bean in Project.AllBeans)
            {
                allRefModules.Add(bean.Space);
                BeanFormatter.MakeMeta(bean.TypeId, bean.Variables, swMeta);
            }
            swMeta.WriteLine();
            swMeta.WriteLine("meta.protocols = {}");
            foreach (Protocol protocol in Project.AllProtocols)
            {
                if (protocol is Rpc)
                    continue;

                allRefModules.Add(protocol.Space);
                if (false == protocol.ArgumentType.IsNormalBean)
                    throw new Exception("protocol argument must be a normal bean");
                Types.Bean b = (Types.Bean)protocol.ArgumentType;
                swMeta.WriteLine($"meta.protocols[{protocol.TypeId}] = {b.TypeId}");
            }
            swMeta.WriteLine();
            swMeta.WriteLine("return meta");
            swMeta.Close();
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

            string dispatcherFileName = System.IO.Path.Combine(srcDir, "ZezeNetService.lua");
            if (false == System.IO.File.Exists(dispatcherFileName))
            {
                using System.IO.StreamWriter swDispatcher = new System.IO.StreamWriter(dispatcherFileName, false, Encoding.UTF8);

                swDispatcher.WriteLine("");
                swDispatcher.WriteLine("ZezeNetServiceProtocolHandles = {}");
                swDispatcher.WriteLine("");
                swDispatcher.WriteLine("function ZezeNetServiceDispatchProtocol(p)");
                swDispatcher.WriteLine("    local handle = ZezeNetServiceProtocolHandles[p.TypeId]");
                swDispatcher.WriteLine("    if nil == handle then");
                swDispatcher.WriteLine("        return 0");
                swDispatcher.WriteLine("    handle(p)");
                swDispatcher.WriteLine("    return 1 -- 1 if found. not result of handle ");
                swDispatcher.WriteLine("end");
                swDispatcher.WriteLine("");
                swDispatcher.WriteLine("function ZezeNetServiceHandshakeDone(service, sessionId)");
                swDispatcher.WriteLine("    ZezeNetServiceCurrentService = service");
                swDispatcher.WriteLine("    ZezeNetServiceCurrentSessionId = sessionId");
                swDispatcher.WriteLine("    -- connection ready. write you code here.");
                swDispatcher.WriteLine("end");
                swDispatcher.WriteLine("");


            }
        }
    }
}
