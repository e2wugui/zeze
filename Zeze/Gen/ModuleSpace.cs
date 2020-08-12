using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class ModuleSpace
    {
        public string Name { get; private set; }
        public ModuleSpace Parent { get; private set; }

        public ModuleSpace GetRootModuleSpace()
        {
            ModuleSpace last = this;
            for (ModuleSpace p = Parent; null != p; p = p.Parent)
            {
                last = p;
            }
            return last;
        }

        public Solution Solution => (Solution)GetRootModuleSpace();

        public Dictionary<string, Module> Modules { get; private set; } = new Dictionary<string, Module>();
        //public SortedDictionary<string, Bean> Beans { get; private set; } = new SortedDictionary<string, Bean>();

        public ModuleSpace(ModuleSpace parent, XmlElement self)
        {
            Parent = parent;
            Name = self.GetAttribute("name");
        }
    }
}
