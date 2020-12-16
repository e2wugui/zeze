using System;
using System.Collections.Generic;
using System.Xml;

namespace ConfigEditor
{
    public class BeanDefine
    {
        public string Name { get; set; }
        public List<Enum> Enums { get; } = new List<Enum>();
        public List<Variable> Variables { get; } = new List<Variable>();
        public List<BeanDefine> BeanDefines { get; } = new List<BeanDefine>();

        public XmlElement Self { get; private set; }
        public Document Document { get; }
        public BeanDefine Parent { get; }

        public BeanDefine CreateSubBeanDefine()
        {
            return new BeanDefine(Document, this);
        }

        public BeanDefine(Document doc, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            this.Name = doc.Name;
        }

        public void Save()
        {
            if (null == Self)
            {
                Self = Document.Xml.CreateElement("BeanDefine");
                if (Parent == null)
                    Document.Xml.DocumentElement.AppendChild(Self);
                else
                    Parent.Self.AppendChild(Self);
            }
            foreach (var e in Enums)
            {
                e.Save(Self);
            }
            foreach (var b in BeanDefines)
            {
                b.Save();
            }
            foreach (var v in Variables)
            {
                v.Save(Self);
            }
        }

        public BeanDefine(Document doc, XmlElement self, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            Name = self.GetAttribute("name");
            if (Name.Length == 0)
                Name = doc.Name;
            this.Self = self;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                switch (e.Name)
                {
                    case "BeanDefine":
                        BeanDefines.Add(new BeanDefine(Document, e, this));
                        break;
                    case "variable":
                        Variables.Add(new Variable(this, e));
                        break;
                    case "enum":
                        Enums.Add(new Enum(this, e));
                        break;
                    default:
                        throw new Exception("node=" + e.Name);
                }
            }
        }
    }
}
