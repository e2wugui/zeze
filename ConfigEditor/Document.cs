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
        public FormMain Main { get; }
        public bool IsChanged { get; set; } = false;

        public void SetFileName(string fileName)
        {
            FileName = System.IO.Path.GetFullPath(fileName);
            if (!FileName.StartsWith(Main.ConfigEditor.GetHome()))
            {
                throw new Exception("文件必须在Home(开始运行时选择的)目录下");
            }
            string relate = FileName.Substring(Main.ConfigEditor.GetHome().Length + 1);
            if (relate.EndsWith(".xml"))
                relate = relate.Substring(0, relate.Length - 4);
            string[] relates = relate.Split(new char[] { '/', '\\' });
            Main.VerifyName(relates[0], false);
            RelateName = relates[0];
            Name = RelateName;
            for (int i = 1; i < relates.Length; ++i)
            {
                Name = relates[i]; // store last
                Main.VerifyName(relates[i], false);
                RelateName = RelateName + '.' + Name;
            }
            BeanDefine.Name = Name;
        }

        public Document(FormMain fm)
        {
            Main = fm;
            BeanDefine = new BeanDefine(this);
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
            BeanDefine.Save();
            for (int i = 0; i < Beans.Count; ++i)
            {
                Bean b = Beans[i];
                b.RowIndex = i;
                b.Save(Xml.DocumentElement);
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
                    default:
                        throw new Exception("Unknown Element Name " + e.Name);
                }
            }
        }
    }

}
