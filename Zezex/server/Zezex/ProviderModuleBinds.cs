using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zezex
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
            return Zezex.Provider.BModule.ChoiceTypeDefault;
        }

        public void BuildStaticBinds(Dictionary<string, Zeze.IModule> AllModules,
            int AutoKeyLocalId, Dictionary<int, Zezex.Provider.BModule> modules)
        {
            Dictionary<string, int> binds = new Dictionary<string, int>();

            // special binds
            foreach (var m in Modules.Values)
            {
                if (m.Providers.Contains(AutoKeyLocalId))
                    binds.Add(m.FullName, Zezex.Provider.BModule.ConfigTypeSpecial);
            }

            // default binds
            if (false == ProviderNoDefaultModule.Contains(AutoKeyLocalId))
            {
                foreach (var m in AllModules.Values)
                {
                    if (IsDynamicModule(m.FullName))
                        continue; // 忽略动态注册的模块。
                    if (Modules.ContainsKey(m.FullName))
                        continue; // 忽略已经有特别配置的模块
                    binds.Add(m.FullName, Zezex.Provider.BModule.ConfigTypeDefault);
                }
            }

            // output
            foreach (var bind in binds)
            {
                if (AllModules.TryGetValue(bind.Key, out var m))
                    modules.Add(m.Id, new Zezex.Provider.BModule()
                    {
                        ChoiceType = GetModuleChoiceType(bind.Key),
                        ConfigType = bind.Value,
                    });
            }
        }

        public class Module
        { 
            public string FullName { get; }
            public int ChoiceType { get; }
            public HashSet<int> Providers { get; } = new HashSet<int>();

            private int GetChoiceType(XmlElement self)
            {
                switch (self.GetAttribute("ChoiceType"))
                {
                    case "ChoiceTypeHashUserId":
                        return Zezex.Provider.BModule.ChoiceTypeHashUserId;

                    case "ChoiceTypeHashRoleId":
                        return Zezex.Provider.BModule.ChoiceTypeHashRoleId;

                    default:
                        return Zezex.Provider.BModule.ChoiceTypeDefault;
                }
            }

            public Module(XmlElement self)
            {
                FullName = self.GetAttribute("name");
                ChoiceType = GetChoiceType(self);
                ProviderModuleBinds.SplitIntoSet(self.GetAttribute("providers"), Providers);
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
                set.Add(int.Parse(provider));
        }
    }
}
