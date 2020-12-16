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
            public ConfigEditor.Variable Define { get; private set; }

            public XmlElement Self { get; set; }

            public Variable(Bean bean, string name)
            {
                this.Parent = bean;
                this.Name = name;
                this.Define = bean.Define.GetVariable(Self.Name);
            }

            public void SetDefine(BeanDefine bd)
            {
                this.Define = bd.GetVariable(Self.Name);
                foreach (var b in Beans)
                {
                    b.SetDefine(Define.Parent);
                }
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
                        case "bean":
                            Beans.Add(new Bean(e));
                            ++childElementCount;
                            break;
                        case "list":
                            foreach (XmlNode bInList in e.ChildNodes)
                            {
                                if (XmlNodeType.Element != bInList.NodeType)
                                    continue;
                                XmlElement eInList = (XmlElement)bInList;
                                if (eInList.Name != "bean")
                                    throw new Exception("Unknown Element In List");
                                Beans.Add(new Bean(eInList));
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
                    Self = Parent.Define.Document.Xml.CreateElement(Name);
                    bean.AppendChild(Self);
                }
                else
                {
                    if (this.Self.Name != Name)
                    {
                        // Name Change
                        XmlElement e = Parent.Define.Document.Xml.CreateElement(Name);
                        bean.ReplaceChild(e, Self);
                        Self = e;
                    }
                }
                Self.SetAttribute("GridColumnWidth", GridColumnNameWidth.ToString());
                Self.SetAttribute("GridColumnValueWidth", GridColumnValueWidth.ToString());
                // update value
                switch (Define.GetEType())
                {
                    case ConfigEditor.Variable.EType.Bean:
                        Beans[0].Save(Self);
                        break;

                    case ConfigEditor.Variable.EType.List:
                        XmlElement list = Parent.Define.Document.Xml.CreateElement("list");
                        Self.AppendChild(list);
                        foreach (var b in Beans)
                        {
                            b.Save(list);
                        }
                        break;
                    default:
                        Self.InnerText = Value;
                        break;
                }
            }
        }

        public List<Variable> Variables { get; } = new List<Variable>();
        public XmlElement Self { get; set; }
        public BeanDefine Define { get; private set; }

        public Bean(XmlElement self)
        {
            this.Self = self;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;
                XmlElement e = (XmlElement)node;
                AddVariable(new Variable(this, e));
            }
        }

        public Bean()
        {
        }

        public void SetDefine(BeanDefine define)
        {
            this.Define = define;
            foreach (var e in Variables)
            {
                e.SetDefine(define);
            }
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

        public void Save(XmlElement parent)
        {
            if (null == Self)
            {
                Self = Define.Document.Xml.CreateElement("bean");
                parent.AppendChild(Self);
            }
            foreach (var v in Variables)
            {
                v.Save(Self);
            }
        }
    }
}
