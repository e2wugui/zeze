using System;
using System.Collections.Generic;
using System.Xml;
using Zeze.Gen.Types;

namespace Zeze.Gen
{
    public class Module : ModuleSpace
    {
        public Service ReferenceService => _ReferenceService;
        Service _ReferenceService;

        public readonly string WebPathBase;
        public readonly string ClassBase;
        public readonly bool MultiInstance = false;

        public void SetReferenceService(Service service)
        {
            _ReferenceService = service;
            foreach (Module m in Modules.Values)
            {
                m.SetReferenceService(service);
            }
        }

        public string FullName => Path();
        public string Comment { get; private set; }
        public bool Hot { get; private set; }

        public static void BeautifulVariableId(XmlElement self)
        {
            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "bean":
                        Bean.BeautifulVariableId(e);
                        break;
                    case "module":
                        Module.BeautifulVariableId(e);
                        break;
                    case "beankey":
                        BeanKey.BeautifulVariableId(e);
                        break;
                }
            }
        }

        public Module(ModuleSpace space, XmlElement self) : base(space, self, true)
        {
            Hot = self.GetAttribute("hot").Equals("true");
            if (space.Modules.ContainsKey(Name))
                throw new Exception("duplicate module name：" + Name);
            space.Modules.Add(Name, this);
            Program.AddNamedObject(Path(".", $"Module{Name}"), this);
            Program.AddNamedObject(Path(".", "AbstractModule"), this);
            WebPathBase = self.GetAttribute("WebPathBase");
            MultiInstance = self.GetAttribute("MultiInstance").Equals("true");
            ClassBase = self.GetAttribute("base");
            if (WebPathBase.Length > 0 && false == WebPathBase.EndsWith("/"))
                WebPathBase += "/";
            Comment = Bean.GetComment(self);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "enum":
                        Add(new Types.Enum(e));
                        break;
                    case "bean":
                        new Types.Bean(this, e);
                        break;
                    case "module":
                        new Module(this, e);
                        break;
                    case "protocol":
                        new Protocol(this, e);
                        break;
                    case "rpc":
                        new Rpc(this, e);
                        break;
                    case "table":
                        new Table(this, e);
                        break;
                    case "beankey":
                        new BeanKey(this, e);
                        break;
                    case "protocolref":
                        // delay parse
                        // 引进其他模块定义的协议。由于引入的协议Id对一个进程不能重复。
                        // 所以再次没法引入本Project.Service中已经包含的协议。
                        // 这个功能用来引入在其他Project.Module中定义的协议。
                        // 【注意】引入的协议保留原来的moduleId，逻辑如果需要判断moduleId的话自己特殊处理。
                        break;
                    case "servlet":
                        new Servlet(this, e);
                        break;
                    case "servletstream":
                        new ServletStream(this, e);
                        break;
                    case "external":
                        new Types.External(this, e);
                        break;
                    case "externalkey":
                        new Types.ExternalBeanKey(this, e);
                        break;
                    default:
                        throw new Exception("unknown nodename=" + e.Name + " in module=" + Path());
                }
            }
        }

        public void Depends(HashSet<Module> unique, List<Module> ordered, bool noRecursive)
        {
            if (false == unique.Add(this))
            {
                //throw new Exception("Module ref duplicate: " + Path());
                return;
            }

            ordered.Add(this);

            if (false == noRecursive)
            {
                foreach (Module module in this.Modules.Values)
                {
                    module.Depends(unique, ordered, noRecursive);
                }
            }
        }

        public void Depends(HashSet<Protocol> depends)
        {
            foreach (Protocol p in Protocols.Values)
            {
                depends.Add(p);
            }
            foreach (var p in ProtocolsImport.Values)
            {
                depends.Add(p);
            }
            foreach (Module module in Modules.Values)
            {
                module.Depends(depends);
            }
        }

        public void Depends(HashSet<Table> depends)
        {
            foreach (Table table in Tables.Values)
            {
                depends.Add(table);
            }

            foreach (Module module in Modules.Values)
            {
                module.Depends(depends);
            }
        }
    }
}
