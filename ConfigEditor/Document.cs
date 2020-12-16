using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class Document
    {
        public string FileName { get; private set; } // fullpath
        public string RelateName { get; private set; } // for search
        public string Name { get; private set; } // FileNameWithoutExtension

        public BeanDefine BeanDefine { get; private set; } // bean in this file

        public List<Bean> Beans { get; } = new List<Bean>();

        public Document(FormMain fm, string fileName)
        {
            FileName = System.IO.Path.GetFullPath(fileName);
            if (!FileName.StartsWith(fm.Config.GetHome()))
            {
                throw new Exception("文件必须在Home(开始运行时选择的)目录下");
            }
            string relate = FileName.Substring(fm.Config.GetHome().Length);
            string[] relates = relate.Split(new char[] { '/', '\\' });
            RelateName = relates[0];
            for (int i = 1; i < relates.Length; ++i)
            {
                Name = System.IO.Path.GetFileNameWithoutExtension(relates[i]); // store last
                foreach (var c in Name)
                {
                    if (char.IsWhiteSpace(c) || c == '.')
                        throw new Exception("Config FileName and path cannot use WhiteSpace and '.'");
                }
                RelateName = RelateName + '.' + Name;
            }
        }

        public XmlDocument Xml { get; private set; }

        public DataGridView Grid { get; set; }

        public void Save()
        {
            if (null == Xml)
            {
                Xml = new XmlDocument();
                Xml.AppendChild(Xml.CreateElement("ZezeConfig"));
            }
            BeanDefine?.Save();
            foreach (var b in Beans)
            {
                b.Save();
            }
            using (TextWriter sw = new StreamWriter(FileName, false, Encoding.UTF8))
            {
                Xml.Save(sw);
            }
        }

        public void Open()
        {
            if (null != Xml)
                throw new Exception("Duplicate Open Document for " + FileName);
            Xml = new XmlDocument();
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
                        this.BeanDefine = new BeanDefine(this, e);
                        break;
                    case "bean":
                        Beans.Add(new Bean(this, e));
                        break;
                }
            }
        }
    }

}
