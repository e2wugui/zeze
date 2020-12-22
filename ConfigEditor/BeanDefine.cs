using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class BeanDefine
    {
        public string Name { get; set; }
        public List<Enum> Enums { get; } = new List<Enum>();
        public List<VarDefine> Variables { get; } = new List<VarDefine>();
        public List<BeanDefine> BeanDefines { get; } = new List<BeanDefine>();

        public XmlElement Self { get; private set; }
        public Document Document { get; }
        public BeanDefine Parent { get; }
        public bool IsLocked { get; set; } = false;
        
        public VarDefine GetVariable(string name)
        {
            foreach (var v in Variables)
            {
                if (v.Name == name)
                    return v;
            }
            return null;
        }

        public BeanDefine GetSubBeanDefine(string name, bool createRefBeanIfNotExist)
        {
            foreach (var v in BeanDefines)
            {
                if (v.Name == name)
                    return v;
            }
            if (createRefBeanIfNotExist)
            {
                Document.IsChanged = true;
                BeanDefine sub = new BeanDefine(Document, name, this);
                BeanDefines.Add(sub);
                return sub;
            }
            return null;
        }

        public BeanDefine Search(string [] path, int offset, bool createRefBeanIfNotExist)
        {
            BeanDefine r = this;
            for (int i = offset; i < path.Length && null != r; ++i)
            {
                r = r.GetSubBeanDefine(path[i], createRefBeanIfNotExist);
            }
            return r;
        }

        public int BuildGridColumns(DataGridView grid, int columnIndex, ColumnTag tag, int listIndex, bool createRefBeanIfNotExist)
        {
            int colAdded = 0;
            foreach (var v in Variables)
            {
                colAdded += v.BuildGridColumns(grid, columnIndex + colAdded, tag, listIndex, createRefBeanIfNotExist);
            }
            // 这里创建的列用来新增。
            grid.Columns.Insert(columnIndex + colAdded, new DataGridViewColumn(new DataGridViewTextBoxCell())
            {
                HeaderText = ",",
                Width = 20,
                ReadOnly = true,
                Tag = tag.Copy(ColumnTag.ETag.AddVariable).AddVar(new VarDefine(this), listIndex),
            });
            for (int i = 0; i < grid.RowCount; ++i)
            {
                grid.Rows[i].Cells[columnIndex + colAdded].Value = ",";
            }
            ++colAdded;
            return colAdded;
        }

        public BeanDefine(Document doc, string name = null, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            this.Name = null != name ? name : doc.Name;
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
            Self.SetAttribute("name", Name);
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
                        Variables.Add(new VarDefine(this, e));
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
