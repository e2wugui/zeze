using System;
using System.Windows.Forms;
using System.Xml;
using System.Text.RegularExpressions;
using System.Collections.Generic;

namespace ConfigEditor
{
    public class Enum
    {
        public string Name { get; set; }
        public List<Value> Values { get;} = new List<Value>();
        public XmlElement Self { get; private set; }
        public BeanDefine Bean { get; }

        public Enum(BeanDefine bean, string name)
        {
            this.Bean = bean;
            this.Name = name;
        }

        public Enum(BeanDefine bean, XmlElement self)
        {
            this.Bean = bean;
            this.Self = self;
            this.Name = self.GetAttribute("name");

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;
                XmlElement e = (XmlElement)node;

                switch (e.Name)
                {
                    case "value":
                        Values.Add(new Value(this, e));
                        break;
                    default:
                        throw new Exception("node=" + e.Name);
                }
            }
        }

        public void Save(XmlElement b)
        {
            if (null == Self)
            {
                Self = Bean.Document.Xml.CreateElement("enum");
                b.AppendChild(Self);
            }
            Self.SetAttribute("name", Name);
            foreach (var v in Values)
            {
                v.Save(Self);
            }
        }

        public class Value
        {
            public string Name { get; set; }
            public string Val { get; set; }
            public string Comment { get; set; }
            public XmlElement Self { get; private set; }
            public Enum Enum { get; }

            public Value(Enum e, string name, string val)
            {
                this.Enum = e;
                this.Name = name;
                this.Val = val;
            }

            public void Save(XmlElement e)
            {
                if (null == Self)
                {
                    Self = Enum.Bean.Document.Xml.CreateElement("value");
                    e.AppendChild(Self);
                }
                Self.SetAttribute("name", Name);
                Self.SetAttribute("value", Val);
            }

            public Value(Enum e, XmlElement self)
            {
                this.Self = self;
                this.Enum = e;
                Name = self.GetAttribute("name");
                Val = self.GetAttribute("value");
                Comment = self.GetAttribute("description");
                if (Comment.Length == 0)
                {
                    Comment = self.GetAttribute("comment").Trim();
                    if (Comment.Length == 0)
                    {
                        XmlNode c = self.NextSibling;
                        if (c != null && XmlNodeType.Text == c.NodeType)
                        {
                            Comment = c.InnerText.Trim();
                            Regex regex = new Regex("[\r\n]");
                            Comment = regex.Replace(Comment, "");
                        }
                    }
                }
            }

        }
    }

}
