using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Module : ModuleSpace
    {
        public Module(ModuleSpace space, XmlElement self) : base(space, self, true)
        {
            if (space.Modules.ContainsKey(Name))
                throw new Exception("duplicate module name" + Name);
            space.Modules.Add(Name, this);
            Program.AddNamedObject(space.Path(".", Name), this);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
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
                        break;
                    case "table":
                        new Table(this, e);
                        break;
                    case "cbean":
                        Console.WriteLine("TODO cbean");
                        break;
                    default:
                        throw new Exception("unknown nodename=" + e.Name + " in module=" + Path("."));
                }
            }
        }

        public void Depends(List<Protocol> depends)
        {
            depends.AddRange(Protocols.Values);

            foreach (Module module in Modules.Values)
            {
                module.Depends(depends);
            }
        }

        public void Depends(List<Table> depends)
        {
            depends.AddRange(Tables.Values);

            foreach (Module module in Modules.Values)
            {
                module.Depends(depends);
            }
        }
    }
}
