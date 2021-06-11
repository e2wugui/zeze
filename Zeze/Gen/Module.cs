using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Zeze.Gen.Types;

namespace Zeze.Gen
{
    public class Module : ModuleSpace
    {
        public Service ReferenceService => _ReferenceService;
        private Service _ReferenceService;

        public void SetReferenceService(Service service)
        {
            _ReferenceService = service;
            foreach (Module m in Modules.Values)
            {
                m.SetReferenceService(service);
            }
        }

        public Module(ModuleSpace space, XmlElement self) : base(space, self, true)
        {
            if (space.Modules.ContainsKey(Name))
                throw new Exception("duplicate module name：" + Name);
            space.Modules.Add(Name, this);
            Program.AddNamedObject(Path(".", $"Module{Name}"), this);
            Program.AddNamedObject(Path(".", "AbstractModule"), this);

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
                    default:
                        throw new Exception("unknown nodename=" + e.Name + " in module=" + Path());
                }
            }
        }

        public void Depends(HashSet<Module> modules)
        {
            if (false == modules.Add(this))
                throw new Exception("Module ref duplicate: " + Path());

            foreach (Module module in this.Modules.Values)
            {
                module.Depends(modules);
            }
        }

        public void Depends(HashSet<Protocol> depends)
        {
            foreach (Protocol p in Protocols.Values)
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
