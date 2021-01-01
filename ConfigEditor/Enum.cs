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

        public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
        {
            XmlElement self = create ? null : Self;

            if (null == self)
            {
                self = xml.CreateElement("enum");
                parent.AppendChild(self);
                if (false == create)
                    Self = self;
            }
            self.SetAttribute("name", Name);

            foreach (var v in Values)
            {
                v.SaveAs(xml, self, create);
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

            public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
            {
                XmlElement self = create ? null : Self;

                if (null == self)
                {
                    self = xml.CreateElement("value");
                    parent.AppendChild(self);
                    if (false == create)
                        Self = self;
                }
                self.SetAttribute("name", Name);
                self.SetAttribute("value", Val);
            }

            public Value(Enum e, XmlElement self)
            {
                this.Self = self;
                this.Enum = e;
                Name = self.GetAttribute("name");
                Val = self.GetAttribute("value");
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
