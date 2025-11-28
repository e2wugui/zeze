using System;
using System.Collections.Generic;
using System.Xml;
using Zeze.Builtin.Provider;

namespace Zeze.Arch
{
    public class ProviderModuleBinds
    {
        public static ProviderModuleBinds Load(string xmlfile = null)
        {
            if (xmlfile == null)
                xmlfile = "provider.module.binds.xml";

            if (System.IO.File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                return new ProviderModuleBinds(doc.DocumentElement);
            }
            return new ProviderModuleBinds();
        }

        private bool IsDynamicModule(string name)
        {
            if (Modules.TryGetValue(name,out var m))
            {
                return m.Providers.Count == 0;
            }
            return false;
        }

        private int GetModuleChoiceType(string name)
        {
            if (Modules.TryGetValue(name, out var m))
            {
                return m.ChoiceType;
            }
            return BModule.ChoiceTypeDefault;
        }

        public void BuildDynamicBinds(Dictionary<string, IModule> AllModules, int serverId, Dictionary<int, BModule> result)
        {
            foreach (var m in AllModules)
            {
                if (Modules.TryGetValue(m.Value.FullName, out var cm))
                {
                    if (!cm.Dynamic)
                        continue;

                    if (cm.Providers.Count > 0 && !cm.Providers.Contains(serverId))
                        continue; // dynamic providers. isEmpty means enable in all server.

                    var tempVar = new BModule();
                    tempVar.ChoiceType = cm.ChoiceType;
                    tempVar.Dynamic = true;
                    result.Add(m.Value.Id, tempVar);
                }
            }
        }

        public void BuildStaticBinds(Dictionary<string, IModule> AllModules,
            int serverId, Dictionary<int, BModule> modules)
        {
            Dictionary<string, bool> binds = new Dictionary<string, bool>();

            // special binds
            foreach (var m in Modules.Values)
            {
                if (!m.Dynamic && m.Providers.Contains(serverId))
                    binds.Add(m.FullName, false);
            }

            // default binds
            if (false == ProviderNoDefaultModule.Contains(serverId))
            {
                foreach (var m in AllModules.Values)
                {
                    if (IsDynamicModule(m.FullName))
                        continue; // 忽略动态注册的模块。
                    if (Modules.ContainsKey(m.FullName))
                        continue; // 忽略已经有特别配置的模块
                    binds.Add(m.FullName, false);
                }
            }

            // output
            foreach (var bind in binds)
            {
                if (AllModules.TryGetValue(bind.Key, out var m))
                    modules.Add(m.Id, new BModule()
                    {
                        ChoiceType = GetModuleChoiceType(bind.Key),
                        Dynamic = bind.Value,
                    });
            }
        }

        public class Module
        { 
            public string FullName { get; }
            public int ChoiceType { get; }
            public HashSet<int> Providers { get; } = new HashSet<int>();

            public bool Dynamic { get; }

            private int GetChoiceType(XmlElement self)
            {
                switch (self.GetAttribute("ChoiceType"))
                {
                    case "ChoiceTypeHashAccount":
                        return BModule.ChoiceTypeHashAccount;

                    case "ChoiceTypeHashRoleId":
                        return BModule.ChoiceTypeHashRoleId;

                    default:
                        return BModule.ChoiceTypeDefault;
                }
            }

            public Module(XmlElement self)
            {
                FullName = self.GetAttribute("name");
                ChoiceType = GetChoiceType(self);

                ProviderModuleBinds.SplitIntoSet(self.GetAttribute("providers"), Providers);

                string attr = self.GetAttribute("dynamic").Trim();
                Dynamic = !string.IsNullOrEmpty(attr) && bool.Parse(attr);
            }
        }

        public Dictionary<string, Module> Modules { get; } = new Dictionary<string, Module>();
        public HashSet<int> ProviderNoDefaultModule { get; } = new HashSet<int>();

        private ProviderModuleBinds()
        { 
        }

        private ProviderModuleBinds(XmlElement self)
        {
            if (false == self.Name.Equals("ProviderModuleBinds"))
                throw new Exception("is it a ProviderModuleBinds config.");

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "module":
                        AddModule(new Module(e));
                        break;

                    case "ProviderNoDefaultModule":
                        SplitIntoSet(e.GetAttribute("providers"), ProviderNoDefaultModule);
                        break;

                    default:
                        throw new Exception("unknown node name: " + e.Name);
                }
            }
        }

        private void AddModule(Module module)
        {
            Modules.Add(module.FullName, module);
        }

        private static void SplitIntoSet(string providers, HashSet<int> set)
        {
            foreach (var provider in providers.Split(','))
            {
                var p = provider.Trim();
                if (p.Length == 0)
                    continue;
                set.Add(int.Parse(p));
            }
        }
    }
}
