using System;
using System.Collections.Generic;
using System.IO;
using System.Xml;
using Zeze.Util;

namespace Zeze.Gen
{
    public class ModuleSpace
    {
        public string Name { get; private set; }
        public string NamePinyin => Program.ToPinyin(Name);
        public ModuleSpace Parent { get; private set; }
        public Util.Ranges ProtocolIdRanges { get; } = new Util.Ranges();
        public int Id { get; }

        public XmlElement Self { get; }

        public ModuleSpace GetRootModuleSpace()
        {
            ModuleSpace last = this;
            for (ModuleSpace p = Parent; p != null; p = p.Parent)
                last = p;
            return last;
        }

        public Solution Solution => (Solution)GetRootModuleSpace();

        public int PathDepth()
        {
            int depth = 0;
            for (ModuleSpace p = Parent; p != null; p = p.Parent)
                ++depth;
            return depth;
        }

        public string Path(string sep = ".", string ObjectName = null)
        {
            string path = Name;
            for (ModuleSpace p = Parent; p != null; p = p.Parent)
                path = p.Name + sep + path;
            if (ObjectName == null)
                return path;

            return path + sep + ObjectName;
        }

        public string PathPinyin(string sep = ".", string ObjectName = null)
        {
            string path = NamePinyin;
            for (ModuleSpace p = Parent; p != null; p = p.Parent)
                path = p.NamePinyin + sep + path;
            if (ObjectName == null)
                return path;

            return path + sep + ObjectName;
        }

        public string GetFullPath(string baseDir, string fileName =  null)
        {
            string fullName = Path(Convert.ToString(System.IO.Path.DirectorySeparatorChar));
            string fullDir = System.IO.Path.Combine(baseDir, fullName);
            if (fileName != null)
                fullDir = System.IO.Path.Combine(fullDir, fileName);
            return fullDir;
        }

        public StreamWriter OpenWriter(string baseDir, string fileName, bool overwrite = true)
        {
            string fullDir = GetFullPath(baseDir);
            //Program.Print("CreateDirectory:" + fullDir);
            FileSystem.CreateDirectory(fullDir);
            string fullFileName = System.IO.Path.Combine(fullDir, fileName);
            bool exists = File.Exists(fullFileName);
            if (!exists || overwrite)
            {
                //Program.Print("file " + (exists ? "overwrite" : "new") + " '" + fullFileName + "'");
                StreamWriter sw = Program.OpenStreamWriter(fullFileName);
                return sw;
            }
            //Program.Print("file skip '" + fullFileName + "'");
            return null;
        }

        public Dictionary<string, Module> Modules { get; private set; } = new Dictionary<string, Module>();
        public SortedDictionary<string, Types.Bean> Beans { get; private set; } = new SortedDictionary<string, Types.Bean>();
        public SortedDictionary<string, Types.BeanKey> BeanKeys { get; private set; } = new SortedDictionary<string, Types.BeanKey>();
        public SortedDictionary<string, Protocol> Protocols { get; private set; } = new SortedDictionary<string, Protocol>();
        public SortedDictionary<string, Table> Tables { get; private set; } = new SortedDictionary<string, Table>();

        public List<Types.Enum> Enums { get; private set; } = new List<Types.Enum>();

        public void Add(Types.Enum e)
        {
            Enums.Add(e); // check duplicate
        }

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
            Self = self;

            Parent = parent;
            Name = self.GetAttribute("name").Trim();
            Program.CheckReserveName(Name);

            if (hasId)
            {
                Id = int.Parse(self.GetAttribute("id"));
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
            foreach (var p in Program.CompileProtocolRef(Program.Refs(Self, "protocolref")))
            {
                ProtocolIdRanges.CheckAdd(p.Id);
                Protocols.Add(p.Name, p);
            }
        }
    }
}
