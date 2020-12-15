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

        public Dictionary<string, Bean> Beans { get; } = new Dictionary<string, Bean>(); // bean in this file

        public Bean GetBean(string name)
        {
            if (Beans.TryGetValue(name, out var b))
                return b;
            return null;
        }

        public Bean GetDefaultBean()
        {
            return GetBean(Name);
        }

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

        public DataGridView Grid { get; private set; }
        public void BuildNew(DataGridView grid)
        {
            Grid = grid;
        }

        public void Open(DataGridView grid)
        {
            Grid = grid;

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
                        new Bean(this, e);
                        break;
                    case "bean":
                        break;
                }
            }
        }
    }

}
