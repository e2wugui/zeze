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
        public SortedDictionary<string, BeanDefine> BeanDefines { get; } = new SortedDictionary<string, BeanDefine>();

        public XmlElement Self { get; private set; }
        public Document Document { get; }
        public BeanDefine Parent { get; }
        private int RefCount = 1;
        public bool Locked { get; set; } = false;

        public void UpdateForeign(string oldForeign, string newForeign)
        {
            foreach (var v in Variables)
            {
                v.UpdateForeign(oldForeign, newForeign);
            }
            foreach (var b in BeanDefines.Values)
            {
                b.UpdateForeign(oldForeign, newForeign);
            }
        }

        public void Move(VarDefine src, VarDefine before)
        {
            int srcIndex = Variables.IndexOf(src);
            if (srcIndex < 0)
                return;
            int beforeIndex = Variables.IndexOf(before);
            if (beforeIndex < 0)
                return;
            Variables.RemoveAt(srcIndex); // 0 1 2 3
            Variables.Insert(beforeIndex, src);
            Document.IsChanged = true;
        }

        /// <summary>
        /// 查看这个Bean以及Sub Bean是否被依赖（存在于deps中）。
        /// </summary>
        /// <param name="deps"></param>
        /// <returns></returns>
        public bool InDepends(HashSet<BeanDefine> deps)
        {
            if (deps.Contains(this))
                return true;

            foreach (var b in BeanDefines.Values)
            {
                if (b.InDepends(deps))
                    return true;
            }

            return false;
        }

        /// <summary>
        /// 收集这个 bean 所有的依赖的bean。
        /// </summary>
        /// <param name="deps"></param>
        public void Depends(HashSet<BeanDefine> deps)
        {
            if (deps.Add(this)) // 因为可能循环引用，只有加入成功才继续变量的 Depends。
            {
                foreach (var b in BeanDefines.Values)
                {
                    b.Depends(deps);
                }
                foreach (var v in Variables)
                {
                    v.Depends(deps);
                }
            }
        }
        /// <summary>
        /// AddVariable
        /// return null means successed.
        /// </summary>
        /// <param name="name"></param>
        /// <param name="type"></param>
        /// <param name="reference"></param>
        /// <returns>error string.</returns>
        public (VarDefine, bool, string) AddVariable(string name,
            VarDefine.EType type = VarDefine.EType.Undecided, string reference = null)
        {
            if (GetVariable(name) != null)
                return (null, false, "duplicate variable name");

            VarDefine var = new VarDefine(this)
            {
                Name = name,
                Type = type,
                Value = reference,
                GridColumnValueWidth = 50,
            };

            bool create = false;
            if (type == VarDefine.EType.List)
            {
                if (string.IsNullOrEmpty(reference))
                {
                    var.Value = var.FullName();
                    var.Reference = new BeanDefine(Document, var.Name, this);
                    BeanDefines.Add(var.Name, var.Reference);
                    create = true;
                }
                else
                {
                    Document.Main.OpenDocument(var.Value, out var r);
                    if (null == r)
                    {
                        return (null, false, "list reference bean not found.");
                    }
                    var.Reference = r;
                    r.AddRefCount();
                }
            }

            Variables.Add(var);
            Document.IsChanged = true;
            return (var, create, "");
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

        public void ForEach(Action<BeanDefine> action)
        {
            action(this);

            foreach (var bd in BeanDefines.Values)
            {
                bd.ForEach(action);
            }
        }

        public BeanDefine DecRefCount()
        {
            if (null == Parent)
                return null; // root BeanDefine 永远存在
            --RefCount;

            if (RefCount <= 0)
            {
                Document.IsChanged = true;
                if (null != Self)
                    Self.ParentNode.RemoveChild(Self);
                Parent.BeanDefines.Remove(this.Name);
                return this;
            }
            return null;
        }

        public void AddRefCount()
        {
            if (null == Parent)
                return; // root BeanDefine 永远存在
            ++RefCount;
            Document.IsChanged = true;
        }

        public VarDefine GetVariable(string name)
        {
            foreach (var v in Variables)
            {
                if (v.Name == name)
                    return v;
            }
            return null;
        }

        public BeanDefine GetSubBeanDefine(string name)
        {
            if (BeanDefines.TryGetValue(name, out var bd))
                return bd;
            return null;
        }

        public BeanDefine Search(string [] path, int offset)
        {
            BeanDefine r = this;
            for (int i = offset; i < path.Length && null != r; ++i)
            {
                r = r.GetSubBeanDefine(path[i]);
            }
            return r;
        }

        public int BuildGridColumns(DataGridView grid, int columnIndex, ColumnTag tag, int listIndex)
        {
            int colAdded = 0;
            foreach (var v in Variables)
            {
                colAdded += v.BuildGridColumns(grid, columnIndex + colAdded, tag, listIndex);
            }
            // 这里创建的列用来新增。
            grid.Columns.Insert(columnIndex + colAdded, new DataGridViewColumn(new DataGridViewTextBoxCell())
            {
                HeaderText = ",",
                Width = 20,
                ReadOnly = true,
                ToolTipText = "双击增加列",
                // 使用跟List一样的规则设置ListIndex，仅用于Delete List Item，此时这个Bean肯定在List中。
                Tag = tag.Copy(ColumnTag.ETag.AddVariable).AddVar(new VarDefine(this), listIndex >= 0 ? listIndex : 0),
                Frozen = false,
                AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
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
            if (Parent != null) // root BeanDefine 自动设置成文件名。
                Self.SetAttribute("name", Name);
            Self.SetAttribute("RefCount", RefCount.ToString());
            Self.SetAttribute("Locked", Locked.ToString());

            foreach (var e in Enums)
            {
                e.Save(Self);
            }

            foreach (var b in BeanDefines.Values)
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
            this.Self = self;

            Name = self.GetAttribute("name");
            if (Name.Length == 0)
                Name = doc.Name;
            string tmp = self.GetAttribute("RefCount");
            RefCount = tmp.Length > 0 ? int.Parse(tmp) : 0;
            tmp = self.GetAttribute("Locked");
            Locked = tmp.Length > 0 ? bool.Parse(tmp) : false;

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                switch (e.Name)
                {
                    case "BeanDefine":
                        var bdnew = new BeanDefine(Document, e, this);
                        BeanDefines.Add(bdnew.Name, bdnew);
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
