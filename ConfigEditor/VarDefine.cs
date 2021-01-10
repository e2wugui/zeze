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
        public EType Type { get; set; } = EType.Undecided;
        public EType TypeDetected { get; set; } = EType.Undecided; // 在导出数据完成时设置，仅在 Build 流程中使用。
        public EType TypeNow => (Type != EType.Undecided) ? Type : TypeDetected; // 仅在 Build 流程中使用。
        public string Value { get; set; } = "";
        public string Foreign { get; set; }
        public string Default { get; set; }
        public string DefaultPinyin => Tools.ToPinyin(Default);
        public string NamePinyin  => Tools.ToPinyin(Name);
        public List<Property.IProperty> PropertiesList { get; private set; } = new List<Property.IProperty>(); // 优化

        public bool IsKeyable() // 仅在 Build 流程中使用。
        {
            switch (TypeNow)
            {
                case EType.Int:
                case EType.Long:
                case EType.String:
                    return true;
            }
            return false;
        }

        private EType Detect(string value)
        {
            // 下面的判断有优先级顺序。不支持自动发现所有类型。
            if (string.IsNullOrEmpty(value))
                return EType.String;

            if (int.TryParse(value, out var _))
                return EType.Int;
            if (long.TryParse(value, out var _))
                return EType.Long;
            if (double.TryParse(value, out var _))
                return EType.Double;
            if (DateTime.TryParse(value, out var _))
                return EType.Date;
            return EType.String;
        }

        /// <summary>
        /// 用于检测 value 是否和 type 匹配。用来验证 default。// build 过程中会再次检测
        /// </summary>
        /// <param name="type"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        public bool CheckType(EType type, string value)
        {
            if (string.IsNullOrEmpty(value))
                return true;

            switch (type)
            {
                case EType.Undecided: return true;
                case EType.Date: return DateTime.TryParse(value, out var _);
                case EType.Double: return double.TryParse(value, out var _);
                case EType.Enum:
                    return Parent.EnumDefines.TryGetValue(Name, out var enumDefine)
                        && enumDefine.ValueMap.TryGetValue(value, out var _);

                case EType.Float: return float.TryParse(value, out var _);
                case EType.Int: return int.TryParse(value, out var _);
                case EType.List: return true;
                case EType.Long: return long.TryParse(value, out var _);
                case EType.String: return true;
                default: throw new Exception("unknown type");
            }
        }

        public void DetectType(string value)
        {
            if (Type != EType.Undecided)
            {
                // 基本类型 verify，已经在 FormMain.buildButton_Click 里面做过了。
                /*
                */
                return;
            }
            EType valueType = Detect(value);
            if (valueType > TypeDetected)
                TypeDetected = valueType;
        }

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
                PropertiesList = FormMain.Instance.PropertyManager.Parse(_Properties);
            }
        }

        public Property.DataOutputFlags DataOutputFlags
        {
            get
            {
                Property.DataOutputFlags flags = Property.DataOutputFlags.None;

                if (FormMain.Instance.PropertyManager.Properties.TryGetValue(Property.Server.PName, out var server))
                {
                    if (PropertiesList.Contains(server))
                        flags |= Property.DataOutputFlags.Server;
                }


                if (FormMain.Instance.PropertyManager.Properties.TryGetValue(Property.Client.PName, out var client))
                {
                    if (PropertiesList.Contains(client))
                        flags |= Property.DataOutputFlags.Client;
                }

                if (flags == Property.DataOutputFlags.None)
                    flags = Property.DataOutputFlags.All; // default to all.

                return flags;
            }
        }

        public string Comment { get; set; }

        public int GridColumnNameWidth { get; set; }
        public int GridColumnValueWidth { get; set; }

        public XmlElement Self { get; private set; }
        public BeanDefine Parent { get; }
        public BeanDefine Reference { get; set; } // type is List

        public void UpdateForeign(string oldForeign, string newForeign)
        {
            if (string.IsNullOrEmpty(Foreign))
                return;

            if (Foreign.Equals(oldForeign))
            {
                Foreign = newForeign;
                Parent.Document.IsChanged = true;

                // 这个方法肯定在 FormDefine 打开时调用。否则下面增加重新 Reload 的代码不会被触发。
                if (Parent.Document.GridData.View != null)
                {
                    FormMain.Instance.ReloadGridsAfterFormDefineClosed.Add(Parent.Document.GridData.View);
                }
            }
        }

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

        public (BeanDefine, EnumDefine) Delete()
        {
            if (null != Self)
                Self.ParentNode.RemoveChild(Self);
            if (Parent.EnumDefines.TryGetValue(Name, out var enumDefine))
                enumDefine.Delete();
            Parent.Document.IsChanged = true;
            Parent.Variables.Remove(this);
            return (Reference?.DecRefCount(), enumDefine);
        }

        public enum EType
        {
            Undecided = 0,
            Int = 1,
            Long = 2,
            Double = 3,
            Date = 4,
            String = 5,
            List = 6,
            Float = 7,
            Enum = 8,
        }

        public static EType ToEType(string type)
        {
            type = type.ToLower();
            switch (type)
            {
                case "": case "undecided":  return EType.Undecided;
                case "int": return EType.Int;
                case "long": return EType.Long;
                case "double": return EType.Double;
                case "date": return EType.Date;
                case "string": return EType.String;
                case "list": return EType.List;
                case "float": return EType.Float;
                case "enum": return EType.Enum;
                default:
                    throw new Exception("Unknown Type " + type);
                    //return EType.Bean;
            }
        }

        public string FullNamePinyin()
        {
            return Parent.FullNamePinyin() + "." + NamePinyin;
        }

        public string FullName()
        {
            return Parent.FullName() + "." + Name;
        }

        public void Verify(Property.VerifyParam param)
        {
            var typeName = System.Enum.GetName(typeof(EType), Type).ToLower();
            if (param.FormMain.PropertyManager.BuildIns.TryGetValue(typeName, out var p))
            {
                p.VerifyCell(param);
            }
            if (param.FormMain.PropertyManager.BuildIns.TryGetValue(Property.ForengnVerify.PName, out var f))
            {
                f.VerifyCell(param);
            }
        }

        // return null if check ok
        public string OpenForeign(out VarDefine foreignVar)
        {
            return OpenForeign(Foreign, out foreignVar);
        }

        public string OpenForeign(string foreign, out VarDefine foreignVar)
        {
            foreignVar = null;
            if (string.IsNullOrEmpty(foreign))
                return null;

            string[] newForeign = foreign.Split(':');
            if (newForeign.Length != 2)
                return "错误的Foreign格式。sample 'ConfigName:VarName'";

            var beanRef = FormMain.Instance.Documents.SearchReference(newForeign[0]);
            if (null == beanRef)
                return "foreign Bean 不存在。";

            foreignVar = beanRef.GetVariable(newForeign[1]);
            if (null == foreignVar)
                return "foreign Bean 变量不存在。";

            if (foreignVar.Parent.Parent != null)
                return "只能 foreign 到文档的 Root Bean。";

            if (foreignVar.Type == EType.List)
                return "foreign VarType 不能为 List。";

            if (foreignVar.Type != Type)
                return "foreign Bean 变量类型和当前数据列的类型不匹配。";

            return null;
        }

        public void InitializeListReference()
        {
            if (Type == EType.List)
            {
                Reference = FormMain.Instance.Documents.SearchReference(Value);
                if (null == Reference)
                    throw new Exception("list reference bean not found: " + Value);
            }
        }

        public int BuildGridColumns(GridData grid, int columnIndex, ColumnTag tag, int listIndex)
        {
            switch (Type)
            {
                case EType.List:
                    {
                        grid.InsertColumn(columnIndex, new GridData.Column()
                        {
                            HeaderText = "[" + this.Name,
                            ReadOnly = true,
                            ToolTipText = Name + ":" + Value + ":" + Comment,
                            ColumnTag = tag.Copy(ColumnTag.ETag.ListStart).AddVar(this, -1),
                        });
                        for (int i = 0; i < grid.RowCount; ++i)
                        {
                            grid.GetCell(columnIndex, i).Value = "[";
                        }

                        if (null == Reference)
                            throw new Exception("List Reference Not Initialize.");

                        ++columnIndex;
                        int colAdded = 0;
                        if (listIndex >= 0)
                        {
                            colAdded = Reference.BuildGridColumns(grid, columnIndex, tag.Copy(tag.Tag).AddVar(this, listIndex), -1);
                        }

                        columnIndex += colAdded;
                        grid.InsertColumn(columnIndex, new GridData.Column()
                        {
                            HeaderText = "]" + this.Name,
                            ReadOnly = true,
                            ToolTipText = Name + ": 双击此列增加List Item。",
                            // 这里的 PathLast.ListIndex 是List中最大的Item数量，以后在Bean.Update中修改。
                            ColumnTag = tag.Copy(ColumnTag.ETag.ListEnd).AddVar(this, 0),
                        });
                        for (int i = 0; i < grid.RowCount; ++i)
                        {
                            grid.GetCell(columnIndex, i).Value = "]";
                        }
                        return colAdded + 2;
                    }
                    /*
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
                        current.BuildUniqueIndex(grid, columnIndex);
                        return 1;
                    }
                    */

                default:
                    {
                        ColumnTag current = tag.Copy(tag.Tag).AddVar(this, -1);
                        grid.InsertColumn(columnIndex, new GridData.Column()
                        {
                            HeaderText = Name,
                            ToolTipText = Name + ":" + Comment,
                            ColumnTag = current,
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

        private void SetAttribute(XmlElement e, string name, string value)
        {
            if (null == value)
                value = "";
            e.SetAttribute(name, value);
        }

        public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
        {
            XmlElement self = create ? null : Self;

            if (null == Name) // 添加在beandefine尾部的var，不需要保存。应该是没有加到Bean.Varaibles里面的。
                return;

            if (null == self)
            {
                self = xml.CreateElement("variable");
                parent.AppendChild(self);
                if (false == create)
                    Self = self;
            }

            self.SetAttribute("name", Name);
            SetAttribute(self, "type", System.Enum.GetName(typeof(EType), Type));
            SetAttribute(self, "value", Value);
            SetAttribute(self, "foreign", Foreign);
            SetAttribute(self, "properties", Properties);
            SetAttribute(self, "comment", Comment);
            SetAttribute(self, "default", Default);

            if (GridColumnNameWidth > 0)
                self.SetAttribute("nw", GridColumnNameWidth.ToString());
            if (GridColumnValueWidth > 0)
                self.SetAttribute("vw", GridColumnValueWidth.ToString());
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
            Default = self.GetAttribute("default");

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
