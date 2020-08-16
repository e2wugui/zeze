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

        public string FullName => Project.Solution.Path(".", Name);

        // setup when compile
        public List<Module> Modules { get; private set; }

        private HashSet<Protocol> AllProtocols;
        public HashSet<Protocol> GetAllProtocols()
        {
            if (AllProtocols != null)
                return AllProtocols;
            AllProtocols = new HashSet<Protocol>();
            foreach (Module module in Modules)
            {
                module.Depends(AllProtocols);
            }
            return AllProtocols;
        }

        //public HashSet<Module> AllModules { get; private set; } = new HashSet<Module>();

        public Manager(Project project, XmlElement self)
        {
            this.self = self;
            Project = project;
            Name = self.GetAttribute("name").Trim();
            Handle = self.GetAttribute("handle");
            Class = self.GetAttribute("class");

            Program.AddNamedObject(FullName, this);

            if (project.Managers.ContainsKey(Name))
                throw new Exception("duplicate manager " + Name + " in project " + project.Name);
            project.Managers.Add(Name, this);

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

        public void Compile()
        {
            ICollection<string> refs = Program.Refs(self, "module");
            List<string> refFulNames = Program.ToFullNameIfNot(Project.Solution.Name, refs);
            Modules = Program.CompileModuleRef(refFulNames);
            /*
            foreach (Module m in Modules)
            {
                m.Depends(AllModules);
            }
            */
        }
    }
}
