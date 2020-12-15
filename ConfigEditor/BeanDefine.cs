using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class BeanDefine
    {
        public string Name { get; set; }
        public List<Enum> Enums { get; } = new List<Enum>();
        public List<Variable> Variables { get; } = new List<Variable>();
        public XmlElement Self { get; private set; }
        public Document Document { get; }

        public BeanDefine(Document doc)
        {
            this.Document = doc;
        }

        public void Save()
        {
            if (null == Self)
            {
                Self = Document.Xml.CreateElement("BeanDefine");
                Document.Xml.DocumentElement.AppendChild(Self);
            }
            foreach (var e in Enums)
            {
                e.Save(Self);
            }
            foreach (var v in Variables)
            {
                v.Save(Self);
            }
        }

        public BeanDefine(Document doc, XmlElement self)
        {
            this.Document = doc;
            Name = self.GetAttribute("name");
            if (Name.Length == 0)
                Name = doc.Name;

            doc.BeanDefines.Add(Name, this);
            this.Self = self;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                String nodename = e.Name;
                switch (e.Name)
                {
                    case "variable":
                        Variables.Add(new Variable(this, e));
                        break;
                    case "enum":
                        Enums.Add(new Enum(this, e));
                        break;
                    default:
                        throw new Exception("node=" + nodename);
                }
            }
        }
    }
}
