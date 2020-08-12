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

        // 用来保存可命名对象（bean,protocol,rpc,table,...)，用来 1 检查命名是否重复，2 查找对象。
        // key 为全名：包含完整的名字空间。
        // 目前 Module，Project，Manager等
        public SortedDictionary<string, object> NamedObjects { get; private set; } = new SortedDictionary<string, object>();

        public void AddNamedObject(string fullName, object obj)
        {
            if (NamedObjects.ContainsKey(fullName))
                throw new Exception("duplicate name: " + fullName);
            NamedObjects.Add(fullName, obj);
        }

        public T GetNamedObject<T>(string fullName)
        {
            object value = null;
            if (NamedObjects.TryGetValue(fullName, out value))
            {
                if (value is T)
                    return (T)value;
                throw new Exception("NamedObject is not " + fullName); // 怎么得到模板参数类型？
            }
            throw new Exception("NamedObject not found: " + fullName);
        }

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
                }
            }
        }
    }
}
