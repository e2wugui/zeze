using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Project
    {
        public string Name { get; private set; }
        public Solution Solution { get; private set; }
        public string Language { get; private set; }
        public SortedDictionary<string, Manager> Managers { get; private set; } = new SortedDictionary<string, Manager>();

        // setup when compile
        public SortedDictionary<string, Module> Modules { get; private set; } = new SortedDictionary<string, Module>();

        private XmlElement self;

        public Project(Solution solution, XmlElement self)
        {
            Solution = solution;
            Name = self.GetAttribute("name").Trim();
            Language = self.GetAttribute("language");

            this.self = self; // 保存，在编译的时候使用。

            if (Solution.Projects.ContainsKey(Name))
                throw new Exception("duplicate project name: " + Name);
            Solution.Projects.Add(Name, this);

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
                    case "manager":
                        new Manager(this, e);
                        break;
                }
            }
        }
    }
}
