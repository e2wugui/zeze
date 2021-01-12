using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Windows.Forms;
using System.Xml;

namespace ConfigEditor
{
    public class BeanDefine
    {
        private string _Name;
        private SortedDictionary<string, EnumDefine> _EnumDefines = new SortedDictionary<string, EnumDefine>();
        private SortedDictionary<string, BeanDefine> _BeanDefines = new SortedDictionary<string, BeanDefine>();
        private List<VarDefine> _Variables = new List<VarDefine>();

        public string Name
        {
            get
            {
                return _Name;
            }
            /*set
            {
                _Name = value;
                Document.IsChanged = true;
            }*/
        }
        public string NamePinyin => Tools.ToPinyin(Name);
        public ReadOnlyDictionary<string, EnumDefine> EnumDefines { get; }
        public ReadOnlyDictionary<string, BeanDefine> BeanDefines { get; }
        public ReadOnlyCollection<VarDefine> Variables { get; }

        public XmlElement Self { get; private set; }
        public Document Document { get; }
        public BeanDefine Parent { get; }
        private int RefCount = 1;
        public bool Locked { get; set; } = false;

        public EnumDefine ChangeEnumName(string oldName, string newName)
        {
            if (_EnumDefines.TryGetValue(oldName, out var e))
            {
                _EnumDefines.Remove(oldName);
                e.Name = newName;
                _EnumDefines.Add(newName, e);
                return e;
            }
            return null;
        }

        public void InitializeListReference()
        {
            foreach (var v in _Variables)
            {
                v.InitializeListReference();
            }
            foreach (var b in _BeanDefines.Values)
            {
                b.InitializeListReference();
            }
        }

        public void UpdateForeign(string oldForeign, string newForeign)
        {
            foreach (var v in _Variables)
            {
                v.UpdateForeign(oldForeign, newForeign);
            }
            foreach (var b in _BeanDefines.Values)
            {
                b.UpdateForeign(oldForeign, newForeign);
            }
        }

        public void Move(VarDefine src, VarDefine before)
        {
            int srcIndex = _Variables.IndexOf(src);
            if (srcIndex < 0)
                return;
            int beforeIndex = _Variables.IndexOf(before);
            if (beforeIndex < 0)
                return;
            _Variables.RemoveAt(srcIndex);
            _Variables.Insert(beforeIndex, src);
            if (null != src.Self && null != before.Self)
            {
                XmlNode parent = src.Self.ParentNode;
                bool srcFirst = true;
                foreach (var childNode in parent.ChildNodes)
                {
                    if (childNode == src.Self)
                    {
                        srcFirst = true;
                        break;
                    }
                    if (childNode == before.Self)
                    {
                        srcFirst = false;
                        break;
                    }
                }
                parent.RemoveChild(src.Self);
                if (srcFirst)
                    parent.InsertAfter(src.Self, before.Self);
                else
                    parent.InsertBefore(src.Self, before.Self);
            }
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

            foreach (var b in _BeanDefines.Values)
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
                foreach (var b in _BeanDefines.Values)
                {
                    b.Depends(deps);
                }
                foreach (var v in _Variables)
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
            switch (type)
            {
                case VarDefine.EType.List:
                    if (string.IsNullOrEmpty(reference))
                    {
                        var.Value = var.FullName();
                        var.Reference = new BeanDefine(Document, var.Name, this);
                        _BeanDefines.Add(var.Name, var.Reference);
                        create = true;
                    }
                    else
                    {
                        var r = FormMain.Instance.Documents.SearchReference(var.Value);
                        if (null == r)
                        {
                            return (null, false, "list reference bean not found.");
                        }
                        var.Reference = r;
                        r.AddRefCount();
                    }
                    break;

                case VarDefine.EType.Enum:
                    _EnumDefines.Add(var.Name, new EnumDefine(this, var.Name));
                    break;
            }

            _Variables.Add(var);
            var.CreateXmlElementIfNeed(); // 调整变量顺序的时候需要这个创建好。
            Document.IsChanged = true;
            return (var, create, "");
        }

        public string FullNamePinyin()
        {
            string name = NamePinyin;
            for (var b = Parent; null != b; b = b.Parent)
            {
                name = b.NamePinyin + "." + name;
            }
            string relatePath = Document.File.Parent.RelateName.Replace(System.IO.Path.DirectorySeparatorChar, '.');
            relatePath = Tools.ToPinyin(relatePath);
            return string.IsNullOrEmpty(relatePath) ? name : relatePath + "." + name;
        }

        public string FullName()
        {
            string name = Name;
            for (var b = Parent; null != b; b = b.Parent)
            {
                name = b.Name + "." + name;
            }
            string relatePath = Document.File.Parent.RelateName.Replace(System.IO.Path.DirectorySeparatorChar, '.');
            return string.IsNullOrEmpty(relatePath) ? name : relatePath + "." + name;
        }

        // ClassName 的前面加上字符 '_'
        public string _FullName()
        {
            string name = "_" + Name;
            for (var b = Parent; null != b; b = b.Parent)
            {
                name = b.Name + "." + name;
            }
            string relatePath = Document.File.Parent.RelateName.Replace(System.IO.Path.DirectorySeparatorChar, '.');
            return string.IsNullOrEmpty(relatePath) ? name : relatePath + "." + name;
        }

        public void CollectFullNameIncludeSubBeanDefine(List<string> result)
        {
            result.Add(FullName());
            foreach (var sub in _BeanDefines.Values)
            {
                sub.CollectFullNameIncludeSubBeanDefine(result);
            }
        }

        public bool ForEach(Func<BeanDefine, bool> action)
        {
            if (!action(this))
                return false;

            foreach (var bd in _BeanDefines.Values)
            {
                if (!bd.ForEach(action))
                    return false;
            }
            return true;
        }

        public BeanDefine DecRefCount()
        {
            if (null == Parent)
                return null; // root BeanDefine 永远存在

            --RefCount;
            Document.IsChanged = true;

            if (RefCount <= 0)
            {
                if (null != Self)
                    Self.ParentNode.RemoveChild(Self);
                Parent._BeanDefines.Remove(this.Name);
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
            foreach (var v in _Variables)
            {
                if (v.Name == name)
                    return v;
            }
            return null;
        }

        public BeanDefine GetSubBeanDefine(string name)
        {
            if (_BeanDefines.TryGetValue(name, out var bd))
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

        public int BuildGridColumns(GridData grid, int columnIndex, ColumnTag tag, int listIndex)
        {
            int colAdded = 0;
            foreach (var v in _Variables)
            {
                colAdded += v.BuildGridColumns(grid, columnIndex + colAdded, tag, listIndex);
            }
            // 这里创建的列用来新增。
            grid.InsertColumn(columnIndex + colAdded, new GridData.Column()
            {
                HeaderText = ",",
                ReadOnly = true,
                ToolTipText = "双击增加列",
                // 使用跟List一样的规则设置ListIndex，仅用于Delete List Item，此时这个Bean肯定在List中。
                ColumnTag = tag.Copy(ColumnTag.ETag.AddVariable).AddVar(new VarDefine(this), listIndex >= 0 ? listIndex : 0),
            });
            for (int i = 0; i < grid.RowCount; ++i)
            {
                grid.GetCell(columnIndex + colAdded, i).Value = ",";
            }
            ++colAdded;
            return colAdded;
        }

        public BeanDefine(Document doc, string name = null, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            this._Name = null != name ? name : doc.Name;
            EnumDefines = new ReadOnlyDictionary<string, EnumDefine>(_EnumDefines);
            BeanDefines = new ReadOnlyDictionary<string, BeanDefine>(_BeanDefines);
            Variables = new ReadOnlyCollection<VarDefine>(_Variables);
        }

        public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
        {
            XmlElement self = create ? null : Self;

            if (null == self)
            {
                self = xml.CreateElement("BeanDefine");
                parent.AppendChild(self);
                if (false == create)
                    Self = self;
            }

            if (Parent != null) // root BeanDefine 自动设置成文件名。
                self.SetAttribute("name", Name);

            self.SetAttribute("RefCount", RefCount.ToString());
            self.SetAttribute("Locked", Locked.ToString());

            foreach (var e in _EnumDefines.Values)
            {
                e.SaveAs(xml, self, create);
            }

            foreach (var b in _BeanDefines.Values)
            {
                b.SaveAs(xml, self, create);
            }

            foreach (var v in _Variables)
            {
                v.SaveAs(xml, self, create);
            }
        }

        public BeanDefine(Document doc, XmlElement self, BeanDefine parent = null)
        {
            this.Document = doc;
            this.Parent = parent;
            this.Self = self;

            EnumDefines = new ReadOnlyDictionary<string, EnumDefine>(_EnumDefines);
            BeanDefines = new ReadOnlyDictionary<string, BeanDefine>(_BeanDefines);
            Variables = new ReadOnlyCollection<VarDefine>(_Variables);

            _Name = self.GetAttribute("name");
            if (Name.Length == 0)
                _Name = doc.Name;
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
                        _BeanDefines.Add(bdnew.Name, bdnew);
                        break;

                    case "variable":
                        _Variables.Add(new VarDefine(this, e));
                        break;

                    case "enum":
                        var enew = new EnumDefine(this, e);
                        _EnumDefines.Add(enew.Name, enew);
                        break;

                    default:
                        throw new Exception("node=" + e.Name);
                }
            }
        }

        public void RemoveEnumDefines(string name)
        {
            if (_EnumDefines.Remove(name))
                Document.IsChanged = true;
        }

        public EnumDefine GetEnumDefine(string name)
        {
            if (_EnumDefines.TryGetValue(name, out var e))
                return e;
            return null;
        }

        public void AddEnumDefine(EnumDefine def)
        {
            _EnumDefines.Add(def.Name, def);
            Document.IsChanged = true;
        }

        public void RemoveVariable(VarDefine var)
        {
            if (_Variables.Remove(var))
                Document.IsChanged = true;
        }
    }
}
