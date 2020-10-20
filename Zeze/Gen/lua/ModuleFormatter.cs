using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.lua
{
    public class ModuleFormatter
    {
        Project project;
        ModuleSpace module;
        string genDir;
        string srcDir;

        public ModuleFormatter(Project project, ModuleSpace module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakeGen();
            MakeSrc();
        }

        public void MakeGen()
        {
            using System.IO.StreamWriter sw = module.Parent.OpenWriter(genDir, module.Name + ".lua");
            sw.WriteLine("-- auto-generated");
            sw.WriteLine();
            sw.WriteLine("local module = {}");
            sw.WriteLine("module.ModuleId = " + module.Id);
            sw.WriteLine();
            foreach (var b in module.BeanKeys.Values)
            {
                BeanKeyFormatter.Make(b, sw);
            }
            foreach (var b in module.Beans.Values)
            {
                BeanFormatter.Make(b, sw);
            }
            foreach (var p in module.Protocols.Values)
            {
                ProtocolFormatter.Make(p, sw);
            }
            sw.WriteLine();
            sw.WriteLine("return module");
        }

        public void MakeSrc()
        {
        }
    }
}
