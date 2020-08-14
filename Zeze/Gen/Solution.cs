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
                        new Types.Bean(this, e);
                        break;
                    case "module":
                        new Module(this, e);
                        break;
                    case "project":
                        new Project(this, e);
                        break;
                    case "beankey":
                        new Types.BeanKey(this, e);
                        break;
                    case "import":
                        Program.ImportSolution(e.GetAttribute("file"));
                        break;
                    default:
                        throw new Exception("unknown nodename " + e.Name + " in solution=" + Name);
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
            // 生成主要是写文件，没有太大必要并发。姑且弄一个。
            List<Task> tasks = new List<Task>();
            foreach (Project project in Projects.Values)
            {
                tasks.Add(Task.Run(project.Make));
            }
            Task.WaitAll(tasks.ToArray());
        }
    }
}
