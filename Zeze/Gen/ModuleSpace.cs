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
        public global::Zeze.Util.Ranges ProtocolIdRanges { get; } = new global::Zeze.Util.Ranges();
        public short Id { get; }

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

        public int PathDepth()
        {
            int depth = 0;
            for (ModuleSpace p = Parent; null != p; p = p.Parent)
                ++depth;
            return depth;
        }

        public string Path(string sep = ".", string ObjectName = null)
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

        public string CreateDirectory(string baseDir)
        {
            string fullName = Path(Convert.ToString(System.IO.Path.DirectorySeparatorChar));
            string fullDir = System.IO.Path.Combine(baseDir, fullName);
            //Program.Print("CreateDirectory:" + fullDir);
            System.IO.Directory.CreateDirectory(fullDir);
            return fullDir;
        }

        public System.IO.StreamWriter OpenWriter(string baseDir, string fileName, bool overwrite = true)
        {
            string fullDir = CreateDirectory(baseDir);
            string fullFileName = System.IO.Path.Combine(fullDir, fileName);
            bool exists = System.IO.File.Exists(fullFileName);
            if (!exists || overwrite)
            {
                Program.Print("file " + (exists ? "overwrite" : "new") + " '" + fullFileName + "'");
                System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
                return sw;
            }
            Program.Print("file skip '" + fullFileName + "'");
            return null;
        }

        public Dictionary<string, Module> Modules { get; private set; } = new Dictionary<string, Module>();
        public SortedDictionary<string, Types.Bean> Beans { get; private set; } = new SortedDictionary<string, Types.Bean>();
        public SortedDictionary<string, Types.BeanKey> BeanKeys { get; private set; } = new SortedDictionary<string, Types.BeanKey>();
        public SortedDictionary<string, Protocol> Protocols { get; private set; } = new SortedDictionary<string, Protocol>();
        public SortedDictionary<string, Table> Tables { get; private set; } = new SortedDictionary<string, Table>();

        public void Add(Types.Bean bean)
        {
            Program.AddNamedObject(Path(".", bean.Name), bean);
            Beans.Add(bean.Name, bean);
        }
        public void Add(Types.BeanKey bean)
        {
            Program.AddNamedObject(Path(".", bean.Name), bean);
            BeanKeys.Add(bean.Name, bean);
        }

        public void Add(Protocol protocol)
        {
            Program.AddNamedObject(Path(".", protocol.Name), protocol);
            Protocols.Add(protocol.Name, protocol);
        }

        public void Add(Table table)
        {
            Program.AddNamedObject(Path(".", table.Name), table);
            Tables.Add(table.Name, table);
        }

        public ModuleSpace(ModuleSpace parent, XmlElement self, bool hasId = false)
        {
            Parent = parent;
            Name = self.GetAttribute("name").Trim();

            if (hasId)
            {
                Id = short.Parse(self.GetAttribute("id"));
                if (Id <= 0)
                    throw new Exception("module id <= 0 is reserved. @" + this.Path());

                Solution.ModuleIdAllowRanges.AssertInclude(Id);
                Solution.ModuleIdCurrentRanges.CheckAdd(Id);
            }
        }

        public virtual void Compile()
        {
            foreach (Types.Bean bean in Beans.Values)
            {
                bean.Compile();
            }
            foreach (Types.BeanKey beanKey in BeanKeys.Values)
            {
                beanKey.Compile();
            }
            foreach (Protocol protocol in Protocols.Values)
            {
                protocol.Compile();
            }
            foreach (Table table in Tables.Values)
            {
                table.Compile();
            }
            foreach (Module module in Modules.Values)
            {
                module.Compile();
            }
        }
    }
}
