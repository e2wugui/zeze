﻿using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Xml;
using Zeze.Gen.cs;
using Zeze.Gen.Types;

namespace Zeze.Gen
{
    public class Project
    {
        public string Name { get; private set; }
        public Solution Solution { get; private set; }
        public string Platform { get; private set; }
        public string GenDir { get; private set; }
        public string ScriptDir { get; private set; }
        public string GenRelativeDir { get; private set; }
        public string GenCommonRelativeDir { get; private set; }
        public HashSet<string> GenTables { get; set; } = new HashSet<string>();
        public SortedDictionary<string, Service> Services { get; private set; } = new SortedDictionary<string, Service>();
        public bool EnableBase { get; private set; } = false;
        public bool ClientScript { get; private set; } = false;

        // setup when compile
        public List<Module> Modules { get; private set; }

        private XmlElement Self;
        private XmlElement ModuleStartSelf;
        private XmlElement FollowerApplyTablesSelf;

        public string BuiltinNG { get; set; }
        public bool BuiltinNotGen => BuiltinNG.Equals("true");

        public string ComponentPresentModuleFullName { get; private set; }
        public string IncludeAllModules { get; private set; } = "false";
        public string MacroEditor { get; private set; }
        public static Project MakingInstance { get; set; }
        public string SolutionName { get; set; }
        public bool MappingClass { get; set; }
        public bool IsUnity { get; private set; } = false;
        public bool NoRecursiveModule { get; private set; } = false;

        public List<Module> GetAllOrderdRefModules()
        {
            HashSet<Module> unique = new HashSet<Module>();
            List<Module> modules = new List<Module>();
            foreach (Module m in Modules)
            {
                m.Depends(unique, modules, NoRecursiveModule);
            }
            foreach (Service service in Services.Values)
            {
                foreach (Module m in service.Modules)
                {
                    m.Depends(unique, modules, NoRecursiveModule);
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
                m.Depends(unique, modules, false);
            }
            return modules;
        }
        
        public Project(Solution solution, XmlElement self)
        {
            Solution = solution;

            Name = self.GetAttribute("name").Trim();
            Platform = self.GetAttribute("platform").Trim();
            GenDir = self.GetAttribute("gendir").Trim();
            if (GenDir.Length == 0)
                GenDir = ".";
            GenRelativeDir = self.GetAttribute("genrelativedir").Trim();
            GenCommonRelativeDir = self.GetAttribute("GenCommonRelativeDir").Trim();
            ScriptDir = self.GetAttribute("scriptdir").Trim();
            ComponentPresentModuleFullName = self.GetAttribute("PresentModuleFullName");
            BuiltinNG = self.GetAttribute("BuiltinNG");
            foreach (string target in self.GetAttribute("GenTables").Split(','))
                GenTables.Add(target);
            IncludeAllModules = self.GetAttribute("IncludeAllModules");
            MacroEditor = self.GetAttribute("MacroEditor");
            SolutionName = self.GetAttribute("SolutionName");
            MappingClass = self.GetAttribute("MappingClass").Equals("true");
            IsUnity = self.GetAttribute("IsUnity").Equals("true");
            EnableBase = self.GetAttribute("EnableBase").Equals("true");
            NoRecursiveModule = self.GetAttribute("NoRecursiveModule").Equals("true");
            //Program.AddNamedObject(FullName, this);
            ClientScript = self.GetAttribute("ClientScript").Equals("true");

            Self = self; // 保存，在编译的时候使用。

            if (Solution.Projects.ContainsKey(Name))
                throw new Exception("duplicate project name: " + Name);
            Solution.Projects.Add(Path.Combine(GenRelativeDir, Name), this);

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
                    case "FollowerApplyTables":
                        FollowerApplyTablesSelf = e;
                        // Make conf+cs+net 的时候解析。
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

        public SortedDictionary<string, Protocol> AllProtocols { get; } = new();
        public SortedDictionary<string, Table> AllTables { get; } = new();
        public SortedDictionary<string, Types.Bean> AllBeans { get; } = new();
        public SortedDictionary<string, Types.BeanKey> AllBeanKeys { get; } = new();
        // 所有的UseData的协议以来的类型。生成的时候需要过滤掉不是bean以及不是beankey的。
        public HashSet<Types.Type> Datas { get; } = new();

        public bool isData(Types.Type type)
        {
            return Datas.Contains(type);
        }

        public void Make()
        {
            MakingInstance = this;
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
                        else if (type is Bean)
                            _AllBeans.Add(type as Types.Bean);
                    }
                }
            }
            foreach (var b in _AllBeans)
            {
                AllBeans[b.FullName] = b;
                b.DetectCircle(new HashSet<Types.Type>());
                if (false == string.IsNullOrEmpty(b.Version) && false == IsTableValueType(b))
                {
                    Console.WriteLine($"WARNING: bean '{b.FullName}' has version but never use in any table.");
                }
            }
            foreach (var b in _AllBeanKeys)
            {
                AllBeanKeys[b.FullName] = b;
                b.DetectCircle(new HashSet<Types.Type>());
            }

            if (Platform.Length == 0)
                Platform = "cs";

            // 设置Module被哪个Service引用。必须在Make前设置。换 Project 会覆盖调引用。
            foreach (Service service in Services.Values)
            {
                service.SetModuleReference();
            }

            var saved = new Dictionary<Solution, string>();
            // save
            foreach (var sol in Program.Solutions.Values)
            {
                if (!sol.Name.Equals("Zeze"))
                    saved.Add(sol, sol.Name);
            }

            // replace
            if (false == string.IsNullOrEmpty(SolutionName))
            {
                foreach (var e in saved)
                    e.Key.Name = SolutionName + e.Key.Name;
            }

            DeleteBuiltinIf(AllBeanKeys);
            DeleteBuiltinIf(AllBeans);
            DeleteBuiltinIf(AllProtocols);
            DeleteBuiltinIf(AllTables);

            foreach (var p in AllProtocols.Values)
            {
                if (p.UseData)
                    p.Depends(Datas);
            }
            foreach (var b in AllBeans.Values)
            {
                if (b.UseData)
                    b.Depends(Datas);
            }

            MakePlatform();

            // rollback
            foreach (var e in saved)
                e.Key.Name = e.Value;
        }

        private void DeleteBuiltinIf<T>(SortedDictionary<string, T> all)
        {
            var collect = new List<string>();
            foreach (var key in all.Keys)
                if (key.StartsWith("Zeze.Builtin.") && BuiltinNotGen)
                    collect.Add(key);
            foreach (var key in collect)
                all.Remove(key);
        }

        public bool IsTableValueType(Bean bean)
        {
            foreach (var t in AllTables.Values)
            {
                if (t.ValueType == bean)
                    return true;
            }
            return false;
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
                case "cxx":
                    var cxx = new cxx.Maker(this);
                    cxx.MakeCxx();
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
                case "conf+cs+net":
                    var dependsFollowerApplyTables = new HashSet<Types.Type>();
                    if (FollowerApplyTablesSelf != null)
                    {
                        var followerApplyTables = Program.Refs(FollowerApplyTablesSelf, "table", "name");
                        foreach (var tFullName in followerApplyTables)
                        {
                            var table = Program.GetNamedObject<Table>(tFullName);
                            table.Depends(dependsFollowerApplyTables);
                        }
                        var refBeans = Program.Refs(FollowerApplyTablesSelf, "bean", "ref");
                        foreach (var refBean in refBeans)
                        {
                            var bean = Program.GetNamedObject<Bean>(refBean);
                            bean.Depends(dependsFollowerApplyTables);
                        }
                    }

                    // 警告，confcs.Maker为了简化，没有生成Gen目录，而是直接放到ProjectName下，这样和需要实现的代码混在一起，
                    // 会导致自己实现代码被删除，这个platform有网络协议以及模块。
                    // 需要重新写一份，选择需要生成的内容以及重新定义生成目录。

                    new cs.Maker(this).MakeConfCsNet(dependsFollowerApplyTables);
                    break;
                default:
                    throw new Exception("Project: unsupport platform: " + Platform);
            }
            Program.FlushOutputs();
        }
    }
}
