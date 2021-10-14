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
        public string Gendir { get; private set; }
        public string ScriptDir { get; private set; }
        public HashSet<String> GenTables { get; } = new HashSet<string>();
        public SortedDictionary<string, Service> Services { get; private set; } = new SortedDictionary<string, Service>();

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
            foreach (Service service in Services.Values)
            {
                foreach (Module m in service.Modules)
                {
                    m.Depends(all);
                }
            }
            return all;
        }

        public Project(Solution solution, XmlElement self)
        {
            Solution = solution;

            Name = self.GetAttribute("name").Trim();
            Platform = self.GetAttribute("platform").Trim();
            Gendir = self.GetAttribute("gendir").Trim();
            if (Gendir.Length == 0)
                Gendir = ".";
            ScriptDir = self.GetAttribute("scriptdir").Trim();

            foreach (string target in self.GetAttribute("GenTables").Split(','))
                GenTables.Add(target);

            //Program.AddNamedObject(FullName, this);

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
                    case "service":
                        new Service(this, e);
                        break;
                    case "ModuleStartOrder":
                        var refs = Program.Refs(e, "start", "module");
                        List<string> refFulNames = Program.ToFullNameIfNot(Solution.Name, refs);
                        for (int i = 0; i < refFulNames.Count; ++i)
                            ModuleStartOrderNames.Add(Program.FullModuleNameToFullClassName(refFulNames[i]));
                        break;
                    case "bean":
                    case "beankey":
                        // Make 的时候解析。
                        break;
                    default:
                        throw new Exception("unkown element name: " + e.Name);
                }
            }
        }

        public List<string> ModuleStartOrderNames { get; private set; } = new List<string>();
        public List<Module> ModuleStartOrder { get; private set; }

        public void Compile()
        {
            ICollection<string> refs = Program.Refs(self, "module");
            List<string> refFulNames = Program.ToFullNameIfNot(Solution.Name, refs);            
            for (int i = 0; i < refFulNames.Count; ++i)
                refFulNames[i] = Program.FullModuleNameToFullClassName(refFulNames[i]);

            Modules = Program.CompileModuleRef(refFulNames);
            ModuleStartOrder = Program.CompileModuleRef(ModuleStartOrderNames);

            foreach (Service service in Services.Values)
            {
                service.Compile();
            }
        }

        /// setup in make
        public SortedDictionary<string, Module> AllModules { get; } = new SortedDictionary<string, Module>();
        public SortedDictionary<string, Protocol> AllProtocols { get; } = new SortedDictionary<string, Protocol>();
        public SortedDictionary<string, Table> AllTables { get; } = new SortedDictionary<string, Table>();
        public SortedDictionary<string, Types.Bean> AllBeans { get; } = new SortedDictionary<string, Types.Bean>();
        public SortedDictionary<string, Types.BeanKey> AllBeanKeys { get; } = new SortedDictionary<string, Types.BeanKey>();

        public void Make()
        {
            foreach (var m in GetAllModules())
                AllModules[m.FullName] = m;

            var _AllProtocols = new HashSet<Protocol>();
            foreach (Module mod in AllModules.Values) // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
            {
                mod.Depends(_AllProtocols);
            }
            foreach (var p in _AllProtocols)
                AllProtocols[p.FullName] = p;

            var _AllTables = new HashSet<Table>();
            foreach (Module mod in AllModules.Values) // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
            {
                mod.Depends(_AllTables);
            }
            foreach (var t in _AllTables)
                AllTables[t.FullName] = t;

            var _AllBeans = new HashSet<Types.Bean>();
            var _AllBeanKeys = new HashSet<Types.BeanKey>();
            {
                HashSet<Types.Type> depends = new HashSet<Types.Type>();
                foreach (Protocol protocol in AllProtocols.Values)
                {
                    protocol.Depends(depends);
                }
                foreach (Table table in AllTables.Values)
                {
                    table.Depends(depends);
                }
                // 加入模块中定义的所有bean和beankey。
                foreach (Module mod in AllModules.Values)
                {
                    foreach (var b in mod.BeanKeys.Values)
                        depends.Add(b);
                    foreach (var b in mod.Beans.Values)
                        depends.Add(b);
                }
                // 加入额外引用的bean,beankey，一般引入定义在不是本项目模块中的。
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
                            _AllBeanKeys.Add(type as Types.BeanKey);
                        else
                            _AllBeans.Add(type as Types.Bean);
                    }
                }
            }
            foreach (var b in _AllBeans)
                AllBeans[b.FullName] = b;
            foreach (var b in _AllBeanKeys)
                AllBeanKeys[b.FullName] = b;

            if (Platform.Length == 0)
                Platform = "cs";

            // 设置Module被哪个Service引用。必须在Make前设置。换 Project 会覆盖调引用。
            foreach (Service service in Services.Values)
            {
                service.SetModuleReference();
            }

            switch (Platform)
            {
                case "cs":
                    new global::Zeze.Gen.cs.Maker(this).Make();
                    break;
                case "lua":
                    new Zeze.Gen.lua.Maker(this).Make();
                    break;
                case "cs+lua":
                    new global::Zeze.Gen.cs.Maker(this).Make();
                    new Zeze.Gen.lua.Maker(this).Make();
                    break;
                case "cxx+lua":
                    new cxx.Maker(this).Make();
                    new Zeze.Gen.lua.Maker(this).Make();
                    break;
                case "ts":
                case "cxx+ts":
                    new ts.Maker(this).Make();
                    break;
                case "cs+ts":
                    new global::Zeze.Gen.cs.Maker(this).Make();
                    new ts.Maker(this).Make();
                    break;
                case "java":
                    new java.Maker(this).Make();
                    break;
                default:
                    throw new Exception("unsupport platform: " + Platform);
            }
        }
    }
}
