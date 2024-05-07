using System;
using System.Collections.Generic;
using System.IO;
using System.Xml;
using Zeze.Util;

namespace Zeze.Gen
{
    public class ModuleSpace
    {
        public string Name { get; internal set; }
        public string NamePinyin => Program.ToPinyin(Name);
        public ModuleSpace Parent { get; private set; }
        public Util.Ranges ProtocolIdRanges { get; } = new Util.Ranges();
        public string DefaultTransactionLevel { get; private set; }
        public bool GenEquals { get; private set; }
        public int Id { get; }
        public bool UseData { get; private set; }
        public bool OnlyData { get; private set; }

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

        public List<string> Paths()
        {
            var path = new List<string>();
            path.Add(Name);
            for (ModuleSpace p = Parent; p != null; p = p.Parent)
                path.Add(p.Name);
            path.Reverse();
            return path;
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
            return Program.OpenWriterNoPath(GetFullPath(baseDir), fileName, overwrite);
        }

        public Dictionary<string, Module> Modules { get; private set; } = new();
        public SortedDictionary<string, Types.Bean> Beans { get; private set; } = new();
        public SortedDictionary<string, Types.BeanKey> BeanKeys { get; private set; } = new();
        public SortedDictionary<string, Protocol> Protocols { get; private set; } = new();
        public SortedDictionary<string, Table> Tables { get; private set; } = new();
        public SortedDictionary<string, Servlet> Servlets { get; private set; } = new();
        public SortedDictionary<string, ServletStream> ServletStreams { get; private set; } = new();
        public List<Types.Enum> Enums { get; private set; } = new();
        public HashSet<Types.Bean> MappingClassBeans { get; } = new();
        // 从其他项目引入的协议，这个协议仅仅生成相关代码，但不会注册到Service也不会在Module中生成Handle。
        public SortedDictionary<string, Protocol> ProtocolsImport { get; private set; } = new();

        public void AddMappingClassBean(Types.Bean bean)
        {
            MappingClassBeans.Add(bean);
        }

        public void Add(ServletStream ss)
        {
            ServletStreams.Add(ss.Name, ss);
        }

        public void Add(Servlet servlet)
        {
            Servlets.Add(servlet.Name, servlet);
        }

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
            Program.CheckReserveName(Name, parent?.Path());
            DefaultTransactionLevel = self.GetAttribute("DefaultTransactionLevel").Trim();
            GenEquals = parent != null && parent.GenEquals || self.GetAttribute("equals") == "true";
            switch (self.GetAttribute("UseData"))
            {
                case "true":
                    UseData = true;
                    OnlyData = false;
                    break;
                case "false":
                    UseData = false;
                    OnlyData = false;
                    break;
                case "only":
                    UseData = true;
                    OnlyData = true;
                    break;
                default:
                    UseData = parent is { UseData: true };
                    OnlyData = parent is { OnlyData: true };
                    break;
            }

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
            foreach (var p in Program.CompileProtocolRef(Program.Refs(Self, "protocolref", "import")))
            {
                ProtocolIdRanges.CheckAdd(p.Id);
                ProtocolsImport.Add(p.Name, p);
            }
        }
    }
}
