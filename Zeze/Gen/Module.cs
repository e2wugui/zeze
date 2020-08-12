using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Module : ModuleSpace
    {
        public string Id { get; private set; }


        public Module(ModuleSpace space, XmlElement self) : base(space, self)
        {
            if (space.Modules.ContainsKey(Name))
                throw new Exception("duplicate module name" + Name);
            space.Modules.Add(Name, this);

            Id = self.GetAttribute("id");
            short id = short.Parse(Id);
            Solution.ModuleIdAllowRanges.AssertInclude(id);
            Solution.ModuleIdCurrentRanges.CheckAdd(id);

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
                }
            }
        }
    }
}
