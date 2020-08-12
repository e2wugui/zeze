using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Manager // Zeze.Net.Manager
    {
        public Project Project { get; private set; }
        public string Name { get; private set; }
        public string Handle { get; private set; }
        public string Class { get; private set; }

        private XmlElement self;

        // setup when compile
        public SortedDictionary<string, Module> Modules { get; private set; } = new SortedDictionary<string, Module>();

        public Manager(Project project, XmlElement self)
        {
            this.self = self;

            Project = project;
            Name = self.GetAttribute("name").Trim();
            Handle = self.GetAttribute("handle");
            Class = self.GetAttribute("class");

            if (project.Managers.ContainsKey(Name))
                throw new Exception("duplicate manager " + Name + " in project " + project.Name);

            /*
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
                }
            }
            */
        }
    }
}
