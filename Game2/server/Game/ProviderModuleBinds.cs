using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Game
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
            return gnet.Provider.BBind.ChoiceTypeDefault;
        }

        public void BuildStaticBinds(Dictionary<string, Zeze.IModule> AllModules, int AutoKeyLocalId, Dictionary<int, int> modules)
        {
            HashSet<string> binds = new HashSet<string>();

            // special binds
            foreach (var m in Modules.Values)
            {
                if (m.Providers.Contains(AutoKeyLocalId))
                    binds.Add(m.Name);
            }

            // default binds
            if (false == ProviderNoDefaultModule.Contains(AutoKeyLocalId))
            {
                foreach (var m in AllModules.Values)
                {
                    if (IsDynamicModule(m.Name))
                        continue;
                    binds.Add(m.Name);
                }
            }

            // output
            foreach (var mname in binds)
            {
                if (AllModules.TryGetValue(mname, out var m))
                    modules.Add(m.Id, GetModuleChoiceType(mname));
            }
        }

        public class Module
        { 
            public string Name { get; }
            public int ChoiceType { get; }
            public HashSet<int> Providers { get; } = new HashSet<int>();

            private int GetChoiceType(XmlElement self)
            {
                switch (self.GetAttribute("ChoiceType"))
                {
                    case "ChoiceTypeHashUserId":
                        return gnet.Provider.BBind.ChoiceTypeHashUserId;

                    case "ChoiceTypeHashRoleId":
                        return gnet.Provider.BBind.ChoiceTypeHashRoleId;

                    default:
                        return gnet.Provider.BBind.ChoiceTypeDefault;
                }
            }

            public Module(XmlElement self)
            {
                Name = self.GetAttribute("name");
                ChoiceType = GetChoiceType(self);
                ProviderModuleBinds.ToSet(self.GetAttribute("providers"), Providers);
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
                        ToSet(e.GetAttribute("providers"), ProviderNoDefaultModule);
                        break;

                    default:
                        throw new Exception("unknown node name: " + e.Name);
                }
            }
        }

        private void AddModule(Module module)
        {
            Modules.Add(module.Name, module);
        }

        private static void ToSet(string providers, HashSet<int> set)
        {
            foreach (var provider in providers.Split(','))
                set.Add(int.Parse(provider));
        }
    }
}
