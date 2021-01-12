using System;
using System.Collections.Generic;
using System.Xml;
using System.Windows.Forms;
using System.Collections.ObjectModel;

namespace ConfigEditor
{
    public class Bean
    {
        public class VarData
        {
            private string _Name;
            private string _Value = "";
            private int _GridColumnNameWidth;
            private int _GridColumnValueWidth;

            public string Name
            {
                get
                {
                    return _Name;
                }
                set
                {
                    _Name = value;
                    Parent.Document.IsChanged = true;
                }
            }
            public string Value
            {
                get
                {
                    return _Value;
                }
                set
                {
                    _Value = value;
                    Parent.Document.IsChanged = true;
                }
            }
            public string ValuePinyin => Tools.ToPinyin(Value);
            public int GridColumnNameWidth
            {
                get
                {
                    return _GridColumnNameWidth;
                }
                set
                {
                    _GridColumnNameWidth = value;
                    Parent.Document.IsChanged = true;
                }
            }
            public int GridColumnValueWidth
            {
                get
                {
                    return _GridColumnValueWidth;
                }
                set
                {
                    _GridColumnValueWidth = value;
                    Parent.Document.IsChanged = true;
                }
            }

            public Bean Parent { get; set; }
            private List<Bean> _Beans = new List<Bean>(); // 变量是list的时候用来存储数据。
            public ReadOnlyCollection<Bean> Beans { get; }
            public XmlElement SelfList { get; private set; }
            public XmlElement Self { get; private set; }

            public void SetBeanAt(int index, Bean bean)
            {
                _Beans[index] = bean;
                Parent.Document.IsChanged = true;
            }

            public void AddBean(Bean bean)
            {
                _Beans.Add(bean);
                Parent.Document.IsChanged = true;
            }

            public void DeleteBeanAt(int index)
            {
                if (index >= _Beans.Count)
                    return;

                Bean exist = _Beans[index];
                if (null != exist && null != exist.Self && null != SelfList)
                {
                    SelfList.RemoveChild(exist.Self);
                }

                _Beans.RemoveAt(index);
                Parent.Document.IsChanged = true;
            }

            public VarData(Bean bean, string name)
            {
                this.Parent = bean;
                this._Name = name;
                Beans = new ReadOnlyCollection<Bean>(_Beans);
            }

            public VarData(Bean bean, XmlElement self)
            {
                this.Parent = bean;
                this.Self = self;
                this._Name = self.Name;
                Beans = new ReadOnlyCollection<Bean>(_Beans);

                string v = self.GetAttribute("nw");
                this.GridColumnNameWidth = v.Length > 0 ? int.Parse(v) : 0;
                v = self.GetAttribute("vw");
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
                            if (null != SelfList)
                                throw new Exception("duplicate list");
                            SelfList = e;
                            foreach (XmlNode bInList in e.ChildNodes)
                            {
                                if (XmlNodeType.Element != bInList.NodeType)
                                    continue;
                                XmlElement eInList = (XmlElement)bInList;
                                if (!eInList.Name.Equals("bean"))
                                    throw new Exception("Unknown Element In List");
                                _Beans.Add(new Bean(bean.Document, eInList));
                            }
                            ++childElementCount;
                            break;
                        default:
                            throw new Exception("Unknown Element In Var");
                    }
                }
                this.Value = (childElementCount == 0) ? self.InnerText : "";
            }

            public void SaveAs(XmlDocument xml, XmlElement parent, bool create, Property.DataOutputFlags flags)
            {
                XmlElement self = create ? null : Self;
                XmlElement selfList = create ? null : SelfList;

                if (null == self)
                {
                    // new
                    self = xml.CreateElement(Name);
                    parent.AppendChild(self);
                    if (false == create)
                        Self = self;
                }
                else
                {
                    if (self.Name != Name)
                    {
                        // Name Change
                        XmlElement e = xml.CreateElement(Name);
                        parent.ReplaceChild(e, self);
                        self = e;
                        if (null != selfList)
                            self.AppendChild(selfList);
                    }
                }

                if (flags == Property.DataOutputFlags.All)
                {
                    if (GridColumnNameWidth > 0)
                        self.SetAttribute("nw", GridColumnNameWidth.ToString());
                    if (GridColumnValueWidth > 0)
                        self.SetAttribute("vw", GridColumnValueWidth.ToString());
                }

                if (_Beans.Count > 0) // 这里没有判断Type，直接根据数据来决定怎么保存。
                {
                    if (null == selfList)
                    {
                        selfList = xml.CreateElement("list");
                        self.AppendChild(selfList);
                        if (false == create)
                            SelfList = selfList;
                    }
                    for (int i = 0; i < _Beans.Count; ++i)
                    {
                        Bean b = _Beans[i];
                        b.RowIndex = i;
                        b.SaveAs(xml, selfList, create, flags);
                    }
                }
                else
                {
                    self.InnerText = Value;
                }
            }
        }

        public SortedDictionary<string, VarData> VariableMap { get; } = new SortedDictionary<string, VarData>();
        public XmlElement Self { get; set; }
        public Document Document { get; }

        public int RowIndex { get; set; } = -1; // 仅在Save时设置，写到文件以后，好读点。

        public string DefineFullName { get; private set; } = "";

        public Bean(Document doc, XmlElement self)
        {
            this.Self = self;
            this.Document = doc;

            this.DefineFullName = self.GetAttribute("DefineFullName");

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;
                XmlElement e = (XmlElement)node;
                VarData var = new VarData(this, e);
                VariableMap[var.Name] = var;
            }
        }

        public Bean(Document doc, string defineFullName)
        {
            this.Document = doc;
            this.DefineFullName = defineFullName;
        }

        public void DeleteVarData(string name)
        {
            if (VariableMap.TryGetValue(name, out var vardata))
            {
                if (Self != null && vardata.Self != null)
                {
                    Self.RemoveChild(vardata.Self);
                }
                VariableMap.Remove(name);
                Document.IsChanged = true;
            }
        }

        public delegate void UpdateAction(GridData grid, int colIndex, ColumnTag.VarInfo varInfo, VarData varData);

        public enum EUpdate
        {
            Data,
            Grid,
            DeleteData,
            CallAction,
        }

        public class UpdateParam
        {
            public EUpdate UpdateType { get; set; }
            public UpdateAction UpdateAction { get; set; }
        }

        public VarData GetLocalVarData(string varName)
        {
            if (VariableMap.TryGetValue(varName, out var varData))
                return varData;
            return null;
        }

        public VarData GetVarData(int pathCurrentIndex, ColumnTag tag, int pathEndIndex)
        {
            if (pathCurrentIndex > pathEndIndex)
                return null;

            ColumnTag.VarInfo varInfo = tag.Path[pathCurrentIndex];
            if (false == VariableMap.TryGetValue(varInfo.Define.Name, out var varData))
            {
                return null;
            }

            if (pathCurrentIndex == pathEndIndex)
            {
                return varData;
            }

            if (varInfo.Define.Type == VarDefine.EType.List)
            {
                if (varInfo.ListIndex >= varData.Beans.Count)
                    return null;
                Bean bean = varData.Beans[varInfo.ListIndex];
                if (null == bean)
                    return null;
                return bean.GetVarData(pathCurrentIndex + 1, tag, pathEndIndex);
            }
            throw new Exception("pathCurrentIndex != pathEndIndex And VarData Is Not A List");
        }

        public bool Update(GridData grid, GridData.Row row, ref int colIndex, int pathIndex, UpdateParam param)
        {
            // ColumnCount maybe change in loop
            for (; colIndex < grid.ColumnCount; ++colIndex)
            {
                ColumnTag tag = grid.GetColumn(colIndex).ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                        // end of bean. 
                        // 删除Define变化时没有同步的数据。
                        HashSet<string> removed = new HashSet<string>();
                        foreach (var k in VariableMap.Keys)
                        {
                            if (tag.PathLast.Define.Parent.GetVariable(k) == null)
                                removed.Add(k);
                        }
                        foreach (var k in removed)
                        {
                            DeleteVarData(k);
                        }
                        return false;
                }
                ColumnTag.VarInfo varInfo = tag.Path[pathIndex];
                if (false == VariableMap.TryGetValue(varInfo.Define.Name, out var varData))
                {
                    switch (param.UpdateType)
                    {
                        case EUpdate.Data:
                            break; // will new data
                        case EUpdate.CallAction:
                        case EUpdate.Grid:
                            if (varInfo.Define.Type == VarDefine.EType.List)
                            {
                                if (tag.Tag == ColumnTag.ETag.ListStart)
                                    ++colIndex;
                                colIndex = GridData.FindColumnListEnd(grid, colIndex);
                            }
                            continue; // data not found. continue load.

                        case EUpdate.DeleteData:
                            return true; // data not found. nothing need to delete.

                        default:
                            throw new Exception("unknown update type");
                    }
                    varData = new VarData(this, varInfo.Define.Name);
                    VariableMap.Add(varInfo.Define.Name, varData);
                }
                else if (param.UpdateType == EUpdate.CallAction)
                {
                    param.UpdateAction(grid, colIndex, varInfo, varData);
                }

                if (varInfo.Define.Type == VarDefine.EType.List)
                {
                    if (param.UpdateType == EUpdate.DeleteData)
                    {
                        if (pathIndex + 1 < tag.Path.Count)
                        {
                            if (varInfo.ListIndex < varData.Beans.Count)
                            {
                                Bean bean1 = varData.Beans[varInfo.ListIndex];
                                if (null != bean1)
                                    bean1.Update(grid, row, ref colIndex, pathIndex + 1, param);
                                // always return true;
                            }
                        }
                        else
                        {
                            if (ColumnTag.ETag.ListStart != tag.Tag)
                                throw new Exception("应该仅在Tag为ListStart时移除数据. see FormMain.deleteVariable...");
                            DeleteVarData(varInfo.Define.Name);
                        }
                        return true;
                    }
                    if (ColumnTag.ETag.ListStart == tag.Tag)
                        continue; // 此时没有进入下一级Bean，就在当前Bean再次判断，因为这里没有ListIndex。

                    if (tag.Tag == ColumnTag.ETag.ListEnd)
                    {
                        int curListCount = -varInfo.ListIndex;
                        int add = 0;
                        for (int i = curListCount; i < varData.Beans.Count; ++i)
                        {
                            ColumnTag tagSeed = tag.Copy(ColumnTag.ETag.Normal);
                            tagSeed.PathLast.ListIndex = i;
                            add += tag.PathLast.Define.Reference.BuildGridColumns(grid, colIndex + add, tagSeed, -1);
                        }
                        if (curListCount < varData.Beans.Count) // curListCount 至少为1.
                            varInfo.ListIndex = -varData.Beans.Count;
                        if (add > 0)
                            --colIndex; // 新增加了列，回退一列，继续装载数据。
                        continue;
                    }

                    if (varInfo.ListIndex >= varData.Beans.Count)
                    {
                        if (EUpdate.Data == param.UpdateType)
                        {
                            for (int i = varData.Beans.Count; i < varInfo.ListIndex; ++i)
                            {
                                varData.AddBean(new Bean(Document, varInfo.Define.Value));
                            }
                            Bean create = new Bean(Document, varInfo.Define.Value);
                            varData.AddBean(create);
                            if (create.Update(grid, row, ref colIndex, pathIndex + 1, param))
                                return true;
                        }
                        // 忽略剩下的没有数据的item直到ListEnd。
                        colIndex = GridData.FindColumnListEnd(grid, colIndex);
                        continue;
                    }

                    Bean bean = varData.Beans[varInfo.ListIndex];
                    if (null != bean)
                    {
                        if (bean.Update(grid, row, ref colIndex, pathIndex + 1, param))
                            return true;
                        continue;
                    }
                    if (EUpdate.Data == param.UpdateType)
                    {
                        Bean create = new Bean(Document, varInfo.Define.Value);
                        varData.SetBeanAt(varInfo.ListIndex, create);
                        if (create.Update(grid, row, ref colIndex, pathIndex + 1, param))
                            return true;
                    }
                    continue;
                }

                if (pathIndex + 1 != tag.Path.Count)
                    throw new Exception("Remain Path, But Is Not A List");

                switch (param.UpdateType)
                {
                    case EUpdate.Data:
                        // OnGridCellEndEdit update data
                        varData.Value = row.Cells[colIndex].Value;
                        return true;

                    case EUpdate.CallAction:
                        // 上面已经做完了。
                        break;

                    case EUpdate.Grid:
                        row.Cells[colIndex].Value = varData.Value; // upate to grid
                        break; // Update Grid 等到 ColumnTag.ETag.AddVariable 才返回。在这个函数开头。

                    case EUpdate.DeleteData:
                        DeleteVarData(varInfo.Define.Name);
                        return true;

                    default:
                        throw new Exception("unkown update type. end.");
                }
            }
            return true;
        }

        public void RenameVar(string oldName, string newName)
        {
            if (oldName.Equals(newName))
                return;

            if (VariableMap.TryGetValue(oldName, out var exist))
            {
                VariableMap.Add(newName, exist);
                exist.Name = newName;
                VariableMap.Remove(oldName);
            }
        }

        public void SaveAs(XmlDocument xml, XmlElement parent, bool create, Property.DataOutputFlags flags)
        {
            XmlElement self = create ? null : Self;

            if (null == self)
            {
                self = xml.CreateElement("bean");
                parent.AppendChild(self);
                if (false == create)
                    Self = self;
            }

            if (flags == Property.DataOutputFlags.All)
                self.SetAttribute("DefineFullName", DefineFullName);

            if (RowIndex >= 0)
                self.SetAttribute("row", RowIndex.ToString());

            // 使用 DefineFullName 找到 BeanDefine。慢的话，可以加cache优化速度。
            BeanDefine beanDefine = Document.BeanDefine;
            if (false == string.IsNullOrEmpty(DefineFullName))
                beanDefine = FormMain.Instance.Documents.SearchReference(DefineFullName);

            foreach (var varDefine in beanDefine.Variables)
            {
                if (0 == (varDefine.DataOutputFlags & flags))
                    continue;

                if (VariableMap.TryGetValue(varDefine.Name, out var varData))
                {
                    varDefine.DetectType(varData.Value);
                    varData.SaveAs(xml, self, create, flags);
                }
                else
                {
                    varDefine.DetectType("");
                }
            }
        }
    }
}
