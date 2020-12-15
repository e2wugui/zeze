using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class Document
    {
        public string FileName { get; private set; } // fullpath
        public string RelateName { get; private set; } // for search
        public string Name { get; private set; } // FileNameWithoutExtension

        public SortedDictionary<string, BeanDefine> BeanDefines { get; } = new SortedDictionary<string, BeanDefine>(); // bean in this file

        public BeanDefine GetBeanDefine(string name)
        {
            if (BeanDefines.TryGetValue(name, out var b))
                return b;
            return null;
        }

        public BeanDefine GetDefaultBeanDefine()
        {
            return GetBeanDefine(Name);
        }

        public List<Bean> Beans { get; } = new List<Bean>();

        public Document(FormMain fm, string fileName)
        {
            FileName = System.IO.Path.GetFullPath(fileName);
            if (!FileName.StartsWith(fm.Config.Home))
            {
                throw new Exception("文件必须在Home(运行时选择的目录)目录下");
            }
            string relate = FileName.Substring(fm.Config.Home.Length);
            string[] relates = relate.Split(new char[] { '/', '\\' });
            RelateName = relates[0];
            for (int i = 1; i < relates.Length; ++i)
            {
                Name = System.IO.Path.GetFileNameWithoutExtension(relates[i]); // store last
                RelateName = RelateName + '/' + Name;
            }
        }

        public XmlDocument Xml { get; } = new XmlDocument();

        public DataGridView Grid { get; set; }

        public void Save()
        {
            foreach (var d in BeanDefines.Values)
            {
                d.Save();
            }
            foreach (var b in Beans)
            {
                b.Save();
            }
        }

        public void Open()
        {
            Xml.Load(FileName);
            XmlElement self = Xml.DocumentElement;
            if (false == self.Name.Equals("ZezeConfig"))
                throw new Exception("node name is not ZezeConfig");
            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "BeanDefine":
                        new BeanDefine(this, e);
                        break;
                    case "bean":
                        Beans.Add(new Bean(this, e));
                        break;
                }
            }
        }
    }

}
