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

        public string Path(string sep, string ObjectName = null)
        {
            string path = Name;
            for (ModuleSpace p = Parent; null != p; p = p.Parent)
            {
                path = p.Name + sep + path;
            }
            if (null == ObjectName)
                return path;

            return path + sep + ObjectName;
        }

        public Dictionary<string, Module> Modules { get; private set; } = new Dictionary<string, Module>();
        public SortedDictionary<string, Types.Bean> Beans { get; private set; } = new SortedDictionary<string, Types.Bean>();
        public SortedDictionary<string, Protocol> Protocols { get; private set; } = new SortedDictionary<string, Protocol>();
        public SortedDictionary<string, Table> Tables { get; private set; } = new SortedDictionary<string, Table>();

        public void Add(Types.Bean bean)
        {
            Solution.AddNamedObject(Path(".", bean.Name), bean);
            Beans.Add(bean.Name, bean);
        }

        public void Add(Protocol protocol)
        {
            Solution.AddNamedObject(Path(".", protocol.Name), protocol);
            Protocols.Add(protocol.Name, protocol);
        }

        public void Add(Table table)
        {
            Solution.AddNamedObject(Path(".", table.Name), table);
            Tables.Add(table.Name, table);
        }

        public ModuleSpace(ModuleSpace parent, XmlElement self)
        {
            Parent = parent;
            Name = self.GetAttribute("name").Trim();
        }
    }
}
