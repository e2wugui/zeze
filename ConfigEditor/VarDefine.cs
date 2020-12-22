using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;
using System.Text.RegularExpressions;
using System.Text;

namespace ConfigEditor
{
    public class VarDefine
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

        public string FullName()
        {
            string name = Name;
            for (var b = Parent; null != b; b = b.Parent)
            {
                name = b.Name + "." + name;
            }
            return name;
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

        public int BuildGridColumns(DataGridView grid, int columnIndex, ColumnTag tag, int listIndex, bool createRefBeanIfNotExist = false)
        {
            switch (GetEType())
            {
                case EType.List:
                    {
                        DataGridViewCell s = new DataGridViewTextBoxCell() { Value = "[" };
                        string listStartText = "[" + this.Name;
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(s)
                        {
                            Name = this.Name,
                            Width = 20,
                            HeaderText = listStartText,
                            ReadOnly = true,
                            ToolTipText = Comment,
                            Tag = tag.Copy(ColumnTag.ETag.ListStart).AddVar(this, -1),
                        });
                        for (int i = 0; i < grid.RowCount; ++i)
                        {
                            grid.Rows[i].Cells[columnIndex].Value = listStartText;
                        }
                        Parent.Document.Main.OpenDocument(Value, out var r, createRefBeanIfNotExist);
                        Reference = r ?? throw new Exception("list reference bean not found: " + Value);
                        ++columnIndex;
                        int colAdded = r.BuildGridColumns(grid, columnIndex,
                            tag.Copy(tag.Tag).AddVar(this, listIndex >= 0 ? listIndex : 0),
                            -1, createRefBeanIfNotExist);
                        DataGridViewCell e = new DataGridViewTextBoxCell() { Value = "]" };
                        columnIndex += colAdded;
                        string listEndText = "]" + this.Name;
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(e)
                        {
                            Name = this.Name,
                            Width = 20,
                            HeaderText = listEndText,
                            ReadOnly = true,
                            ToolTipText = Comment,
                            Tag = tag.Copy(ColumnTag.ETag.ListEnd).AddVar(this, -1), // 初始为-1，以后在Bean.SetDataToGrid中修改。
                        });
                        for (int i = 0; i < grid.RowCount; ++i)
                        {
                            grid.Rows[i].Cells[columnIndex].Value = listEndText;
                        }
                        return colAdded + 2;
                    }

                case EType.Enum:
                    {
                        DataGridViewCell template = new DataGridViewComboBoxCell();
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(template)
                        {
                            Name = this.Name,
                            Width = GridColumnValueWidth,
                            ToolTipText = Comment,
                            Tag = tag.Copy(tag.Tag).AddVar(this, listIndex),
                        });
                        return 1;
                    }

                default:
                    {
                        DataGridViewCell template = new DataGridViewTextBoxCell();
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(template)
                        {
                            Name = this.Name,
                            Width = GridColumnValueWidth,
                            ToolTipText = Comment,
                            Tag = tag.Copy(tag.Tag).AddVar(this, listIndex),
                        });
                        return 1;
                    }
            }
        }

        public VarDefine(BeanDefine bean)
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

            Self.SetAttribute("nw", GridColumnNameWidth.ToString());
            Self.SetAttribute("vw", GridColumnValueWidth.ToString());
        }

        public VarDefine(BeanDefine bean, XmlElement self)
        {
            this.Self = self;
            this.Parent = bean;

            Name = self.GetAttribute("name");
            Type = self.GetAttribute("type");
            Value = self.GetAttribute("value");

            string v = self.GetAttribute("nw");
            this.GridColumnNameWidth = v.Length > 0 ? int.Parse(v) : 0;
            v = self.GetAttribute("vw");
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
