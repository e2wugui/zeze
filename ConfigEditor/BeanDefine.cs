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
        private Dictionary<string, ReferenceFrom> _ReferenceFroms = new Dictionary<string, ReferenceFrom>();
        private bool _Locked = false;
        private SortedDictionary<string, EnumDefine> _EnumDefines = new SortedDictionary<string, EnumDefine>();
        private SortedDictionary<string, BeanDefine> _BeanDefines = new SortedDictionary<string, BeanDefine>();
        private List<VarDefine> _Variables = new List<VarDefine>();

        // 此类定义已经没人引用了，但是由于存在嵌套的类，不能删除。
        // 此时 FormDefine 不显示编辑这样的类定义。
        public bool NamespaceOnly => _ReferenceFroms.Count == 0 && Parent != null;
        public ReadOnlyDictionary<string, ReferenceFrom> ReferenceFroms { get; }
        public class ReferenceFrom
        {
            public enum Reasons
            {
                List,
                Foreign,
            }

            public string FullName { get; }
            public string VarName { get; }
            public Reasons Reason { get; }

            public XmlElement Self { get; internal set; }

            public ReferenceFrom(string fullName, string varName, Reasons reason)
            {
                FullName = fullName;
                VarName = varName;
                Reason = reason;
            }

            public void SaveAs(XmlDocument xml, XmlElement parent, bool create)
            {
                XmlElement self = create ? null : Self;

                if (null == self)
                {
                    self = xml.CreateElement("ReferenceFrom");
                    parent.AppendChild(self);
                    if (false == create)
                        Self = self;
                }

                self.SetAttribute("FullName", FullName);
                self.SetAttribute("VarName", VarName);
                self.SetAttribute("Reason", System.Enum.GetName(typeof(Reasons), Reason));
            }

            public ReferenceFrom(XmlElement self)
            {
                this.Self = self;

                FullName = self.GetAttribute("FullName");
                VarName = self.GetAttribute("VarName");
                Reason = (Reasons)System.Enum.Parse(typeof(Reasons), self.GetAttribute("Reason"));
            }
        }

        public string Name
        {
            get
            {
                return _Name;
            }
            set
            {
                if (_Name.Equals(value))
                    return;

                RemoveReferenceFromMe();
                Parent?._BeanDefines.Remove(_Name);
                _Name = value;
                Parent?._BeanDefines.Add(_Name, this);
                AddReferenceFromMe();
                if (Parent == null)
                {
                    // rename file
                    Document.File.Rename(_Name);
                }
                Document.IsChanged = true;
                UpdateReferenceFroms();
            }
        }

        public bool Locked
        {
            get
            {
                return _Locked;
            }
            set
            {
                _Locked = value;
                Document.IsChanged = true;
            }
        }
        public string NamePinyin => Tools.ToPinyin(Name);
        public ReadOnlyDictionary<string, EnumDefine> EnumDefines { get; }
        public ReadOnlyDictionary<string, BeanDefine> BeanDefines { get; }
        public ReadOnlyCollection<VarDefine> Variables { get; }

        public XmlElement Self { get; private set; }
        public Document Document { get; }
        public BeanDefine Parent { get; }

        private void RemoveReferenceFromMe()
        {
            foreach (var v in _Variables)
            {
                v.Reference?.RemoveReferenceFrom(v, null, null, true);
            }
            foreach (var sub in _BeanDefines.Values)
            {
                sub.RemoveReferenceFromMe();
            }
        }

        private void AddReferenceFromMe()
        {
            foreach (var v in _Variables)
            {
                switch (v.Type)
                {
                    case VarDefine.EType.List:
                        v.Reference?.AddReferenceFrom(v,  ReferenceFrom.Reasons.List);
                        break;
                    default:
                        //if (false == string.IsNullOrEmpty(v.Foreign) && v.Foreign.IndexOf(':') > 0)
                        v.Reference?.AddReferenceFrom(v, BeanDefine.ReferenceFrom.Reasons.Foreign);
                        break;
                }
            }
            foreach (var sub in _BeanDefines.Values)
            {
                sub.AddReferenceFromMe();
            }
        }

        private void UpdateReferenceFroms()
        {
            var newFullName = FullName();
            // 更新自己引用别人的FullName，需要旧的名字。改变_Name前先Remove，之后重新Add。
            foreach (var reff in ReferenceFroms.Values)
            {
                var refBeanDefine = FormMain.Instance.Documents.SearchReference(reff.FullName);
                switch (reff.Reason)
                {
                    case BeanDefine.ReferenceFrom.Reasons.List:
                        refBeanDefine.GetVariable(reff.VarName).Value = newFullName;
                        break;

                    case BeanDefine.ReferenceFrom.Reasons.Foreign:
                        // beanFullName 肯定是 root，引用保持不变，不需要重新SearchReference。
                        var refVar = refBeanDefine.GetVariable(reff.VarName);
                        var refForeignVarName = refVar.Foreign.Substring(refVar.Foreign.IndexOf(':') + 1);
                        refVar.SetRawForeign($"{newFullName}:{refForeignVarName}");
                        break;
                }
            }
            foreach (var sub in _BeanDefines.Values)
            {
                sub.UpdateReferenceFroms();
            }
        }

        public EnumDefine ChangeEnumName(string oldName, string newName)
        {
            if (_EnumDefines.TryGetValue(oldName, out var e))
            {
                _EnumDefines.Remove(oldName);
                e.Name = newName;
                _EnumDefines.Add(newName, e);
                Document.IsChanged = true;
                return e;
            }
            return null;
        }

        public void InitializeReference()
        {
            foreach (var v in _Variables)
            {
                v.InitializeReference();
            }
            foreach (var b in _BeanDefines.Values)
            {
                b.InitializeReference();
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

            VarDefine var = new VarDefine(this, name)
            {
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
                        if (_BeanDefines.TryGetValue(var.Name, out var exist))
                        {
                            if (false == exist.NamespaceOnly)
                                return (null, false, $"尝试为变量'{var.Name}'创建一个 BeanDefine 失败：已经存在。");
                        }
                        else
                        {
                            exist = new BeanDefine(Document, var.Name, this);
                            _BeanDefines.Add(var.Name, exist);
                        }
                        var.Reference = exist;
                        exist.AddReferenceFrom(var, ReferenceFrom.Reasons.List);
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
                        r.AddReferenceFrom(var, ReferenceFrom.Reasons.List);
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
            return FullName(Name);
        }

        public string FullName(string ReplaceThisName)
        {
            string name = ReplaceThisName;
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
            return FullName("_" + Name);
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

        private bool Deleting = false;

        private void TryDeleteThis(HashSet<BeanDefine> deletedBeanDefines, HashSet<EnumDefine> deletedEnumDefines)
        {
            // 删除类定义的时候，一路往上找，把能删除的都删除。就像删除空目录。
            for (var cur = this; null != cur; cur = cur.Parent)
            {
                if (null == cur.Parent || Deleting)
                    return; // never delete root bean

                if (cur._ReferenceFroms.Count > 0)
                    break;

                Deleting = true; // var.Delete 会递归
                try
                {
                    foreach (var var in _Variables.ToArray())
                    {
                        var.Delete(deletedBeanDefines, deletedEnumDefines);
                    }
                }
                finally
                {
                    Deleting = false;
                }

                // 仍然有子类，说明里面的类被其他地方引用了。
                // 此时 This 作用变成一个名字空间。
                if (cur._BeanDefines.Count > 0)
                    break;

                if (null != cur.Self)
                    cur.Self.ParentNode.RemoveChild(cur.Self);

                cur.Self = null;
                cur.Parent._BeanDefines.Remove(cur.Name);
                deletedBeanDefines?.Add(cur);
            }
        }

        // 如果由于不再被引用，需要删除类定义时，返回this，否则返回null。
        public void RemoveReferenceFrom(VarDefine var,
            HashSet<BeanDefine> deletedBeanDefines, HashSet<EnumDefine> deletedEnumDefines,
            bool willAddBack = false)
        {
            var fullName = var.Parent.FullName();
            var referenceFromKey = $"{fullName}:{var.Name}";
            if (_ReferenceFroms.TryGetValue(referenceFromKey, out var exist))
            {
                if (null != exist.Self)
                    exist.Self.ParentNode.RemoveChild(exist.Self);
                exist.Self = null;
                _ReferenceFroms.Remove(referenceFromKey);
                // 本来想Foreign弄成弱引用，由于Foreign只能引用root，肯定不会被删除。强弱就无所谓了。
                if (false == willAddBack)
                    TryDeleteThis(deletedBeanDefines, deletedEnumDefines);
                Document.IsChanged = true;
            }
        }

        public void AddReferenceFrom(VarDefine var, ReferenceFrom.Reasons reason)
        {
            var fullName = var.Parent.FullName();
            _ReferenceFroms.Add($"{fullName}:{var.Name}", new ReferenceFrom(fullName, var.Name, reason));
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
                ColumnTag = tag.Copy(ColumnTag.ETag.AddVariable).AddVar(new VarDefine(this, ""), listIndex >= 0 ? listIndex : 0),
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
            ReferenceFroms = new ReadOnlyDictionary<string, ReferenceFrom>(_ReferenceFroms);
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

            self.SetAttribute("Locked", Locked.ToString());

            foreach (var r in _ReferenceFroms.Values)
            {
                r.SaveAs(xml, self, create);
            }
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
            ReferenceFroms = new ReadOnlyDictionary<string, ReferenceFrom>(_ReferenceFroms);

            _Name = self.GetAttribute("name");
            if (Name.Length == 0)
                _Name = doc.Name;
            string tmp = self.GetAttribute("Locked");
            _Locked = tmp.Length > 0 ? bool.Parse(tmp) : false;

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

                    case "ReferenceFrom":
                        var rf = new ReferenceFrom(e);
                        _ReferenceFroms.Add($"{rf.FullName}:{rf.VarName}", rf);
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
