using System;
using System.Collections.Generic;
using System.Xml;

namespace ConfigEditor
{
    public class Bean
    {
        public class Variable
        {
            public string Name { get; set; }
            public string Value { get; set; }
            public int GridColumnNameWidth { get; set; }
            public int GridColumnValueWidth { get; set; }

            public Bean Bean { get; set; }
            public XmlElement Self { get; set; }

            public Variable(Bean bean)
            {
                this.Bean = bean;
            }

            public Variable(Bean bean, XmlElement self)
            {
                this.Bean = bean;
                this.Self = self;
                this.Name = self.Name;
                this.Value = self.InnerText;
                string v = self.GetAttribute("GridColumnWidth");
                this.GridColumnNameWidth = v.Length > 0 ? int.Parse(v) : 0;
                v = self.GetAttribute("GridColumnValueWidth");
                this.GridColumnValueWidth = v.Length > 0 ? int.Parse(v) : 0;
            }

            public void Save(XmlElement bean)
            {
                if (null == this.Self)
                {
                    // new
                    Self = Bean.Document.Xml.CreateElement(Name);
                    bean.AppendChild(Self);
                }
                else
                {
                    if (this.Self.Name != Name)
                    {
                        // Name Change
                        XmlElement e = Bean.Document.Xml.CreateElement(Name);
                        bean.ReplaceChild(e, Self);
                        Self = e;
                    }
                }
                // update value
                Self.InnerText = Value;
                Self.SetAttribute("GridColumnWidth", GridColumnNameWidth.ToString());
                Self.SetAttribute("GridColumnValueWidth", GridColumnValueWidth.ToString());
            }
        }

        public List<Variable> Variables { get; } = new List<Variable>();
        public XmlElement Self { get; set; }
        public Document Document { get; }

        public Bean(Document doc, XmlElement self)
        {
            this.Self = self;
            this.Document = doc;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;
                XmlElement e = (XmlElement)node;
                AddVariable(new Variable(this, e));
            }
        }

        public Bean(Document doc)
        {
            this.Document = doc;
        }

        public void AddVariable(Variable var)
        {
            foreach (var e in Variables)
            {
                if (e.Name == var.Name)
                    throw new Exception("Duplicate Variable Name of " + var.Name);
            }
            Variables.Add(var);
        }

        public void Save()
        {
            if (null == Self)
            {
                Self = Document.Xml.CreateElement("");
                Document.Xml.DocumentElement.AppendChild(Self);
            }
            foreach (var v in Variables)
            {
                v.Save(Self);
            }
        }
    }
}
