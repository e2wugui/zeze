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
        private string _Properties = "";

        public string Name { get; set; }
        public EType Type { get; set; } = EType.Auto;

        public string Value { get; set; } = "";
        public string Foreign { get; set; }

        public List<Property.IProperty> PropertiesList { get; private set; } = new List<Property.IProperty>(); // 优化

        public string Properties
        {
            get
            {
                return _Properties;
            }
            set
            {
                if (_Properties.Equals(value))
                    return;

                _Properties = value;
                PropertiesList = Parent.Document.Main.PropertyManager.Parse(_Properties);
            }
        }

        public string Comment { get; set; }

        public int GridColumnNameWidth { get; set; }
        public int GridColumnValueWidth { get; set; }

        public XmlElement Self { get; private set; }
        public BeanDefine Parent { get; }
        public BeanDefine Reference { get; set; } // type is List

        public void Depends(HashSet<BeanDefine> deps)
        {
            if (null != Reference)
                Reference.Depends(deps);
        }

        public string CanChangeTo(EType newType)
        {
            if (newType == Type)
                return null; // same is ok

            if (newType == EType.List)
                return "Type.List 只能在新增的时候设置。不能通过修改设置";

            if (Type == EType.List)
                return "Type.List 设置以后不能修改。";

            return null;
        }

        public BeanDefine Delete()
        {
            if (null != Self)
                Self.ParentNode.RemoveChild(Self);
            Parent.Document.IsChanged = true;
            Parent.Variables.Remove(this);
            return Reference?.DecRefCount();
        }

        public enum EType
        {
            Auto = 0,
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

        public EType ToEType(string type)
        {
            type = type.ToLower();
            switch (type)
            {
                case "": case "auto": return EType.Auto;
                case "int": return EType.Int;
                case "long": return EType.Long;
                case "double": return EType.Double;
                case "string": return EType.String;
                case "list": return EType.List;
                case "float": return EType.Float;
                case "enum": return EType.Enum;
                default:
                    throw new Exception("Unknown Type " + type);
                    //return EType.Bean;
            }
        }

        public int BuildGridColumns(DataGridView grid, int columnIndex, ColumnTag tag, int listIndex)
        {
            switch (Type)
            {
                case EType.List:
                    {
                        DataGridViewCell s = new DataGridViewTextBoxCell() { Value = "[" };
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(s)
                        {
                            Name = this.Name,
                            Width = 20,
                            HeaderText = "[" + this.Name,
                            ReadOnly = true,
                            ToolTipText = Name + ":" + Value + ":" + Comment,
                            Tag = tag.Copy(ColumnTag.ETag.ListStart).AddVar(this, -1),
                            Frozen = false,
                            AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                        });
                        for (int i = 0; i < grid.RowCount; ++i)
                        {
                            grid.Rows[i].Cells[columnIndex].Value = "[";
                        }

                        if (null == Reference)
                        {
                            Parent.Document.Main.OpenDocument(Value, out var r);
                            Reference = r ?? throw new Exception("list reference bean not found: " + Value);
                        }

                        ++columnIndex;
                        int colAdded = Reference.BuildGridColumns(grid, columnIndex,
                            tag.Copy(tag.Tag).AddVar(this, listIndex >= 0 ? listIndex : 0), -1);

                        DataGridViewCell e = new DataGridViewTextBoxCell() { Value = "]" };
                        columnIndex += colAdded;
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(e)
                        {
                            Name = this.Name,
                            Width = 20,
                            HeaderText = "]" + this.Name,
                            ReadOnly = true,
                            ToolTipText = Name + ": 双击此列增加List Item。",
                            Tag = tag.Copy(ColumnTag.ETag.ListEnd).AddVar(this, -1), // 初始为-1，以后在Bean.SetDataToGrid中修改。
                            Frozen = false,
                            AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                        });
                        for (int i = 0; i < grid.RowCount; ++i)
                        {
                            grid.Rows[i].Cells[columnIndex].Value = "]";
                        }
                        return colAdded + 2;
                    }

                case EType.Enum:
                    {
                        DataGridViewCell template = new DataGridViewTextBoxCell();
                        ColumnTag current = tag.Copy(tag.Tag).AddVar(this, -1);
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(template)
                        {
                            Name = this.Name,
                            Width = GridColumnValueWidth,
                            ToolTipText = Name + ":" + Comment,
                            Tag = current,
                            Frozen = false,
                            AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                        });
                        // 自动完成来实现enum选择。不使用Combobox.
                        // TODO 实现 enum 的时候需要确认 cell.Value 的类型。编辑器使用的Column，然后类型是枚举而不是string。
                        current.BuildUniqueIndex(grid, columnIndex);
                        return 1;
                    }

                default:
                    {
                        DataGridViewCell template = new DataGridViewTextBoxCell();
                        ColumnTag current = tag.Copy(tag.Tag).AddVar(this, -1);
                        grid.Columns.Insert(columnIndex, new DataGridViewColumn(template)
                        {
                            Name = this.Name,
                            Width = GridColumnValueWidth,
                            ToolTipText = Name + ":" + Comment,
                            Tag = current,
                            Frozen = false,
                            AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                        });
                        current.BuildUniqueIndex(grid, columnIndex);
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
            SetAttribute("type", System.Enum.GetName(typeof(EType), Type));
            SetAttribute("value", Value);
            SetAttribute("foreign", Foreign);
            SetAttribute("properties", Properties);
            SetAttribute("comment", Comment);

            if (GridColumnNameWidth > 0)
                Self.SetAttribute("nw", GridColumnNameWidth.ToString());
            if (GridColumnValueWidth > 0)
                Self.SetAttribute("vw", GridColumnValueWidth.ToString());
        }

        public VarDefine(BeanDefine bean, XmlElement self)
        {
            this.Self = self;
            this.Parent = bean;

            Name = self.GetAttribute("name");
            Type = ToEType(self.GetAttribute("type"));
            Value = self.GetAttribute("value");

            string v = self.GetAttribute("nw");
            this.GridColumnNameWidth = v.Length > 0 ? int.Parse(v) : 0;
            v = self.GetAttribute("vw");
            this.GridColumnValueWidth = v.Length > 0 ? int.Parse(v) : 0;
            Properties = self.GetAttribute("properties");

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
