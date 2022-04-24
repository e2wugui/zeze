using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Project
    {
        public string Name { get; private set; }
        public Solution Solution { get; private set; }
        public string Platform { get; private set; }
        public string Gendir { get; private set; }
        public string ScriptDir { get; private set; }
        public string GenRelativeDir { get; private set; }
        public string GenCommonRelativeDir { get; private set; }
        public HashSet<string> GenTables { get; } = new HashSet<string>();
        public SortedDictionary<string, Service> Services { get; private set; } = new SortedDictionary<string, Service>();

        // setup when compile
        public List<Module> Modules { get; private set; }

        private XmlElement Self;
        private XmlElement ModuleStartSelf;
        public string BuiltinNG { get; set; }
        public bool BuiltinNotGen => BuiltinNG.Equals("true");

        public string ComponentPresentModuleFullName { get; private set; }
        public string IncludeAllModules { get; private set; } = "false";

        public List<Module> GetAllOrderdRefModules()
        {
            HashSet<Module> unique = new HashSet<Module>();
            List<Module> modules = new List<Module>();
            foreach (Module m in Modules)
            {
                m.Depends(unique, modules);
            }
            foreach (Service service in Services.Values)
            {
                foreach (Module m in service.Modules)
                {
                    m.Depends(unique, modules);
                }
            }
            return modules;
        }

        public List<Module> GetSolutionAllModules()
        {
            HashSet<Module> unique = new HashSet<Module>();
            List<Module> modules = new List<Module>();
            foreach (var m in Solution.Modules.Values)
            { 
                m.Depends(unique, modules);
            }
            return modules;
        }
        
        public Project(Solution solution, XmlElement self)
        {
            Solution = solution;

            Name = self.GetAttribute("name").Trim();
            Platform = self.GetAttribute("platform").Trim();
            Gendir = self.GetAttribute("gendir").Trim();
            if (Gendir.Length == 0)
                Gendir = ".";
            GenRelativeDir = self.GetAttribute("genrelativedir").Trim();
            GenCommonRelativeDir = self.GetAttribute("GenCommonRelativeDir").Trim();
            ScriptDir = self.GetAttribute("scriptdir").Trim();
            ComponentPresentModuleFullName = self.GetAttribute("PresentModuleFullName");
            BuiltinNG = self.GetAttribute("BuiltinNG");
            foreach (string target in self.GetAttribute("GenTables").Split(','))
                GenTables.Add(target);
            IncludeAllModules = self.GetAttribute("IncludeAllModules");
            //Program.AddNamedObject(FullName, this);

            Self = self; // 保存，在编译的时候使用。

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
                        ModuleStartSelf = e;
                        break;
                    case "bean":
                    case "beankey":
                        // Make 的时候解析。
                        break;
                    default:
                        throw new Exception("unknown element name: " + e.Name);
                }
            }
        }

        public List<Module> ModuleStartOrder { get; private set; } = new List<Module>();

        public void Compile()
        {
            {
                ICollection<string> refs = Program.Refs(Self, "module");
                List<string> refFulNames = Program.ToFullNameIfNot(Solution.Name, refs);
                for (int i = 0; i < refFulNames.Count; ++i)
                    refFulNames[i] = Program.FullModuleNameToFullClassName(refFulNames[i]);

                Modules = Program.CompileModuleRef(refFulNames, $"Project={Name} module ref ");
            }
            if (ModuleStartSelf != null)
            {
                var refs = Program.Refs(ModuleStartSelf, "start", "module");
                List<string> refFulNames = Program.ToFullNameIfNot(Solution.Name, refs);
                for (int i = 0; i < refFulNames.Count; ++i)
                    refFulNames[i] = Program.FullModuleNameToFullClassName(refFulNames[i]);
                ModuleStartOrder = Program.CompileModuleRef(refFulNames, $"Project={Name} ModuleStartOrder");
            }

            foreach (Service service in Services.Values)
            {
                service.Compile();
            }
        }

        /// setup in make
        public List<Module> AllOrderDefineModules { get; private set; }

        public SortedDictionary<string, Protocol> AllProtocols { get; } = new SortedDictionary<string, Protocol>();
        public SortedDictionary<string, Table> AllTables { get; } = new SortedDictionary<string, Table>();
        public SortedDictionary<string, Types.Bean> AllBeans { get; } = new SortedDictionary<string, Types.Bean>();
        public SortedDictionary<string, Types.BeanKey> AllBeanKeys { get; } = new SortedDictionary<string, Types.BeanKey>();

        public void Make()
        {
            AllOrderDefineModules = IncludeAllModules.Equals("true") ? GetSolutionAllModules() : GetAllOrderdRefModules();
                        
            var _AllProtocols = new HashSet<Protocol>();
            foreach (Module mod in AllOrderDefineModules) // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
            {
                if (mod.FullName.StartsWith("Zeze.Builtin.") && BuiltinNotGen)
                    continue;
                mod.Depends(_AllProtocols);
            }
            foreach (var p in _AllProtocols)
                AllProtocols[p.FullName] = p;

            var _AllTables = new HashSet<Table>();
            foreach (Module mod in AllOrderDefineModules) // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
            {
                if (mod.FullName.StartsWith("Zeze.Builtin.") && BuiltinNotGen)
                    continue;
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
                foreach (Module mod in AllOrderDefineModules)
                {
                    if (mod.FullName.StartsWith("Zeze.Builtin.") && BuiltinNotGen)
                        continue;
                    foreach (var b in mod.BeanKeys.Values)
                        b.Depends(depends);
                    foreach (var b in mod.Beans.Values)
                        b.Depends(depends);
                }
                // 加入额外引用的bean,beankey，一般引入定义在不是本项目模块中的。
                foreach (string n in Program.Refs(Self, "bean"))
                {
                    Program.GetNamedObject<Types.Bean>(n).Depends(depends);
                }
                foreach (string n in Program.Refs(Self, "beankey"))
                {
                    Program.GetNamedObject<Types.BeanKey>(n).Depends(depends);
                }
                foreach (Types.Type type in depends)
                {
                    if (type.IsBean || type.IsRocks)
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

            MakePlatform();
        }

        protected virtual void MakePlatform()
        {
            switch (Platform)
            {
                case "cs":
                    new global::Zeze.Gen.cs.Maker(this).Make();
                    break;
                case "lua":
                    new Zeze.Gen.lua.Maker(this).Make();
                    break;
                case "luaclient":
                    new Zeze.Gen.luaClient.Maker(this).Make();
                    break;
                case "cs+lua":
                    new global::Zeze.Gen.cs.Maker(this).Make();
                    new Zeze.Gen.lua.Maker(this).Make();
                    break;
                case "cs+luaclient":
                    new global::Zeze.Gen.cs.Maker(this).Make();
                    new Zeze.Gen.luaClient.Maker(this).Make();
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
                case "conf+cs":
                    new confcs.Maker(this).Make();
                    break;
                default:
                    throw new Exception("Project: unsupport platform: " + Platform);
            }
            Program.FlushOutputs();
        }
    }
}
