using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Solution : ModuleSpace
    {
        public Zeze.Util.Ranges ModuleIdAllowRanges { get; private set; }
        public Zeze.Util.Ranges ModuleIdCurrentRanges { get; private set; } = new Zeze.Util.Ranges();

        public SortedDictionary<string, Project> Projects { get; private set; } = new SortedDictionary<string, Project>();

        public Solution(XmlElement self) : base(null, self)
        {
            if (false == self.Name.Equals("solution"))
                throw new Exception("node name is not solution");

            ModuleIdAllowRanges = new Zeze.Util.Ranges(self.GetAttribute("ModuleIdAllowRanges"));
            Program.GlobalModuleIdChecker.CheckAdd(ModuleIdAllowRanges);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "bean":
                        Console.WriteLine("bean " + e.GetAttribute("name"));
                        break;
                    case "module":
                        new Module(this, e);
                        break;
                    case "project":
                        new Project(this, e);
                        break;
                }
            }
        }
    }
}
