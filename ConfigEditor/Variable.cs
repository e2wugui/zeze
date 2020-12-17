using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;
using System.Text.RegularExpressions;


namespace ConfigEditor
{
    public class Variable
    {
        public string Name { get; set; }
        public string Type { get; set; }
        public string Value { get; set; }
        public string Foreign { get; set; }
        public string Properties { get; set; } // unique;
        public string Comment { get; set; }

        public int GridColumnNameWidth { get; set; }
        public int GridColumnValueWidth { get; set; }

        public XmlElement Self { get; private set; }
        public BeanDefine Parent { get; }
        public BeanDefine Reference { get; private set; } // type is List

        public enum EType
        {
            Zero = 0,
            Int = 1,
            Long = 2,
            Double = 3,
            String = 4,
            //Bean = 5,
            List = 6,
            Float = 7,
            Enum = 8,
        }

        public EType GetEType()
        {
            if (Type == null || Type.Length == 0)
                return EType.Zero;

            switch (Type)
            {
                case "int": return EType.Int;
                case "long": return EType.Long;
                case "double": return EType.Double;
                case "string": return EType.String;
                case "list": return EType.List;
                case "float": return EType.Float;
                case "enum": return EType.Enum;
                default:
                    throw new Exception("Unknown Type " + Type);
                    //return EType.Bean;
            }
        }

        public void BuildGridLastRow(DataGridView grid, Bean rowData)
        {
            switch (GetEType())
            {
                case EType.List:
                    break;
                case EType.Enum:
                    break;
                default:
                    break;
            }
        }

            public void BuildGridColumns(DataGridView grid)
        {
            switch (GetEType())
            {
                case EType.List:
                    {
                        DataGridViewCell s = new DataGridViewTextBoxCell() { Value = "[" };
                        grid.Columns.Add(new DataGridViewColumn(s)
                        { Name = this.Name, Width = 20, Tag = this, HeaderText = "[", ReadOnly = true, ToolTipText = Comment });
                        Parent.Document.Main.OpenDocument(Value, out var r);
                        Reference = r;
                        r.BuildGridColumns(grid);
                        DataGridViewCell e = new DataGridViewTextBoxCell() { Value = "]" };
                        grid.Columns.Add(new DataGridViewColumn(e)
                        { Name = this.Name, Width = 20, Tag = this, HeaderText = "]", ReadOnly = true, ToolTipText = Comment });
                    }
                    break;
                    /*
                case EType.Bean:
                    Parent.Document.Main.OpenDocument(Type, out var r);
                    Reference = r;
                    r.BuildGridColumns(grid);
                    break;
                    */

                case EType.Enum:
                    {
                        DataGridViewCell template = new DataGridViewComboBoxCell();
                        grid.Columns.Add(new DataGridViewColumn(template)
                        { Name = this.Name, Tag = this, Width = GridColumnValueWidth, ToolTipText = Comment });
                    }
                    break;

                default:
                    {
                        DataGridViewCell template = new DataGridViewTextBoxCell();
                        grid.Columns.Add(new DataGridViewColumn(template)
                        { Name = this.Name, Tag = this, Width = GridColumnValueWidth, ToolTipText = Comment });
                    }
                    break;
            }
        }

        public Variable(BeanDefine bean)
        {
            this.Parent = bean;
        }

        private void SetAttribute(string name, string value)
        {
            if (null != value && value.Length > 0)
                Self.SetAttribute(name, value);
        }
        public void Save(XmlElement bean)
        {
            if (null == Name) // 添加在beandefine尾部的var，不需要保存。
                return;

            if (null == Self)
            {
                Self = Parent.Document.Xml.CreateElement("variable");
                bean.AppendChild(Self);
            }
            Self.SetAttribute("name", Name);
            SetAttribute("type", Type);
            SetAttribute("value", Value);
            SetAttribute("foreign", Foreign);
            SetAttribute("properties", Properties);

            Self.SetAttribute("GridColumnWidth", GridColumnNameWidth.ToString());
            Self.SetAttribute("GridColumnValueWidth", GridColumnValueWidth.ToString());
        }

        public Variable(BeanDefine bean, XmlElement self)
        {
            this.Self = self;
            this.Parent = bean;

            Name = self.GetAttribute("name");
            Type = self.GetAttribute("type");
            Value = self.GetAttribute("value");

            string v = self.GetAttribute("GridColumnWidth");
            this.GridColumnNameWidth = v.Length > 0 ? int.Parse(v) : 0;
            v = self.GetAttribute("GridColumnValueWidth");
            this.GridColumnValueWidth = v.Length > 0 ? int.Parse(v) : 0;

            Comment = self.GetAttribute("comment");
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
