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

            public Bean Parent { get; set; }
            public List<Bean> Beans { get; } = new List<Bean>(); // 变量是list或者bean的时候用来存储数据。

            public XmlElement Self { get; set; }

            public Variable(Bean bean, string name)
            {
                this.Parent = bean;
                this.Name = name;
            }

            public Variable(Bean bean, XmlElement self)
            {
                this.Parent = bean;
                this.Self = self;
                this.Name = self.Name;

                string v = self.GetAttribute("GridColumnWidth");
                this.GridColumnNameWidth = v.Length > 0 ? int.Parse(v) : 0;
                v = self.GetAttribute("GridColumnValueWidth");
                this.GridColumnValueWidth = v.Length > 0 ? int.Parse(v) : 0;

                XmlNodeList childNodes = self.ChildNodes;
                int childElementCount = 0;
                foreach (XmlNode node in childNodes)
                {
                    if (XmlNodeType.Element != node.NodeType)
                        continue;
                    XmlElement e = (XmlElement)node;
                    switch (e.Name)
                    {
                        case "list":
                            foreach (XmlNode bInList in e.ChildNodes)
                            {
                                if (XmlNodeType.Element != bInList.NodeType)
                                    continue;
                                XmlElement eInList = (XmlElement)bInList;
                                if (eInList.Name != "bean")
                                    throw new Exception("Unknown Element In List");
                                Beans.Add(new Bean(bean.Document, eInList));
                            }
                            ++childElementCount;
                            break;
                        default:
                            throw new Exception("Unknown Element In Var");
                    }
                }
                this.Value = (childElementCount == 0) ? self.InnerText : "";
            }

            public void Save(XmlElement bean)
            {
                if (null == this.Self)
                {
                    // new
                    Self = Parent.Document.Xml.CreateElement(Name);
                    bean.AppendChild(Self);
                }
                else
                {
                    if (this.Self.Name != Name)
                    {
                        // Name Change
                        XmlElement e = Parent.Document.Xml.CreateElement(Name);
                        bean.ReplaceChild(e, Self);
                        Self = e;
                    }
                }
                Self.SetAttribute("GridColumnWidth", GridColumnNameWidth.ToString());
                Self.SetAttribute("GridColumnValueWidth", GridColumnValueWidth.ToString());

                if (Beans.Count > 0) // 这里没有判断Type，直接根据数据来决定怎么保存。
                {
                    XmlElement list = Parent.Document.Xml.CreateElement("list");
                    Self.AppendChild(list);
                    foreach (var b in Beans)
                    {
                        b.Save(list);
                    }
                }
                else
                {
                    Self.InnerText = Value;
                }
            }
        }

        public SortedDictionary<string, Variable> VariableMap { get; } = new SortedDictionary<string, Variable>();
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
                Variable var = new Variable(this, e);
                VariableMap[var.Name] = var;
            }
        }

        public Bean(Document doc)
        {
            this.Document = doc;
        }

        public void AddOrUpdate(ConfigEditor.Variable varDefine, string varValue)
        {
            if (varDefine.Parent == Document.BeanDefine)
            {
                // top level
                if (VariableMap.TryGetValue(varDefine.Name, out var exist))
                {
                    exist.Value = varValue;
                }
                else
                {
                    VariableMap.Add(varDefine.Name, new Variable(this, varDefine.Name));
                }
            }
            else
            {
                // inner level

            }
        }

        public void Save(XmlElement parent)
        {
            if (null == Self)
            {
                Self = Document.Xml.CreateElement("bean");
                parent.AppendChild(Self);
            }
            foreach (var v in VariableMap.Values)
            {
                v.Save(Self);
            }
        }
    }
}
