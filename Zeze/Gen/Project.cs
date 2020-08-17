using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Zeze.Gen.cs;

namespace Zeze.Gen
{
    public class Project
    {
        public string Name { get; private set; }
        public Solution Solution { get; private set; }
        public string Platform { get; private set; }
        public string Generates { get; private set; }
        public SortedDictionary<string, Manager> Managers { get; private set; } = new SortedDictionary<string, Manager>();

        // setup when compile
        public List<Module> Modules { get; private set; }

        private XmlElement self;

        public HashSet<Module> GetAllModules()
        {
            HashSet<Module> all = new HashSet<Module>();
            foreach (Module m in Modules)
            {
                m.Depends(all);
            }
            foreach (Manager manager in Managers.Values)
            {
                foreach (Module m in manager.Modules)
                {
                    m.Depends(all);
                }
            }
            return all;
        }

        public String FullName => Solution.Name + "." + Name;

        public Project(Solution solution, XmlElement self)
        {
            Solution = solution;

            Name = self.GetAttribute("name").Trim();
            Platform = self.GetAttribute("platform").Trim();
            Generates = self.GetAttribute("generates").Trim();

            Program.AddNamedObject(FullName, this);

            this.self = self; // 保存，在编译的时候使用。

            if (Solution.Projects.ContainsKey(Name))
                throw new Exception("duplicate project name: " + Name);
            Solution.Projects.Add(Name, this);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "module":
                        // ref 对象在编译的时候查找和设置。将保存在 Modules 中。
                        break;
                    case "manager":
                        new Manager(this, e);
                        break;
                }
            }
        }

        public void Compile()
        {
            ICollection<string> refs = Program.Refs(self, "module");
            List<string> refFulNames = Program.ToFullNameIfNot(Solution.Name, refs);
            Modules = Program.CompileModuleRef(refFulNames);

            foreach (Manager manager in Managers.Values)
            {
                manager.Compile();
            }
        }

        /// setup in make
        public HashSet<Module> AllModules { get; private set; }
        public HashSet<Protocol> AllProtocols { get; private set; }
        public HashSet<Table> AllTables { get; private set; }
        public HashSet<Types.Bean> AllBeans { get; private set; }
        public HashSet<Types.BeanKey> AllBeanKeys { get; private set; }

        public void Make()
        {
            AllModules = GetAllModules();

            AllProtocols = new HashSet<Protocol>();
            foreach (Module mod in AllModules) // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
            {
                mod.Depends(AllProtocols);
            }

            AllTables = new HashSet<Table>();
            foreach (Module mod in AllModules) // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
            {
                mod.Depends(AllTables);
            }

            AllBeans = new HashSet<Types.Bean>();
            AllBeanKeys = new HashSet<Types.BeanKey>();
            {
                HashSet<Types.Type> depends = new HashSet<Types.Type>();
                foreach (Protocol protocol in AllProtocols)
                {
                    protocol.Depends(depends);
                }
                foreach (Table table in AllTables)
                {
                    table.Depends(depends);
                }
                foreach (string n in Program.Refs(self, "bean"))
                {
                    depends.Add(Program.GetNamedObject<Types.Bean>(n));
                }
                foreach (string n in Program.Refs(self, "beankey"))
                {
                    depends.Add(Program.GetNamedObject<Types.BeanKey>(n));
                }
                foreach (Types.Type type in depends)
                {
                    if (type.IsBean)
                    {
                        if (type.IsKeyable)
                            AllBeanKeys.Add(type as Types.BeanKey);
                        else
                            AllBeans.Add(type as Types.Bean);
                    }
                }
            }

            if (Platform.Length == 0)
                Platform = "cs";

            switch (Platform)
            {
                case "cs":
                    new Zeze.Gen.cs.Maker(this).Make();
                    break;
                default:
                    throw new Exception("unsupport platform: " + Platform);
            }
        }
    }
}
