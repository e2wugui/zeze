using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;
using System.Xml;
using System.Threading.Tasks;

namespace Zeze.Gen
{
    public class Solution : ModuleSpace
    {
        public global::Zeze.Util.Ranges ModuleIdAllowRanges { get; private set; }
        public global::Zeze.Util.Ranges ModuleIdCurrentRanges { get; private set; } = new global::Zeze.Util.Ranges();

        public SortedDictionary<string, Project> Projects { get; private set; } = new SortedDictionary<string, Project>();

        public Solution(XmlElement self) : base(null, self)
        {
            if (false == self.Name.Equals("solution"))
                throw new Exception("node name is not solution");

            ModuleIdAllowRanges = new global::Zeze.Util.Ranges(self.GetAttribute("ModuleIdAllowRanges"));
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
                        new Types.Bean(this, e);
                        break;
                    case "module":
                        new Module(this, e);
                        break;
                    case "project":
                        new Project(this, e);
                        break;
                    case "component":
                        new Component(this, e);
                        break;
                    case "beankey":
                        new Types.BeanKey(this, e);
                        break;
                    case "import":
                        Program.ImportSolution(e.GetAttribute("file"));
                        break;
                    case "external":
                        new Types.External(this, e);
                        break;
                    case "externalkey":
                        new Types.ExternalBeanKey(this, e);
                        break;
                    default:
                        throw new Exception("unknown nodename " + e.Name + " in solution=" + Name);
                }
            }
        }

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
                        Types.Bean.BeautifulVariableId(e);
                        break;
                    case "module":
                        Module.BeautifulVariableId(e);
                        break;
                    case "beankey":
                        Types.BeanKey.BeautifulVariableId(e);
                        break;
                }
            }
        }

        public override void Compile()
        {
            foreach (Project project in Projects.Values)
            {
                project.Compile();
            }
            base.Compile();
        }

        public void Make()
        {
            foreach (Project project in Projects.Values)
            {
                project.Make();
            }
        }
    }
}
