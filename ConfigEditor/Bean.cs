using System;
using System.Collections.Generic;
using System.Xml;
using System.Windows.Forms;

namespace ConfigEditor
{
    public class Bean
    {
        public class VarData
        {
            public string Name { get; set; }
            public string Value { get; set; } = "";
            public int GridColumnNameWidth { get; set; }
            public int GridColumnValueWidth { get; set; }

            public Bean Parent { get; set; }
            public List<Bean> Beans { get; } = new List<Bean>(); // 变量是list或者bean的时候用来存储数据。
            public XmlElement SelfList { get; set; }
            public XmlElement Self { get; set; }

            public void DeleteBeanAt(int index)
            {
                if (index >= Beans.Count)
                    return;

                Bean exist = Beans[index];
                if (null != exist && null != exist.Self && null != SelfList)
                {
                    SelfList.RemoveChild(exist.Self);
                }

                Beans.RemoveAt(index);
                Parent.Document.IsChanged = true;
            }

            public VarData(Bean bean, string name)
            {
                this.Parent = bean;
                this.Name = name;
            }

            public VarData(Bean bean, XmlElement self)
            {
                this.Parent = bean;
                this.Self = self;
                this.Name = self.Name;

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
                                if (eInList.Name != "bean")
                                    throw new Exception("Unknown Element In List");
                                Beans.Add(new Bean(bean.Document, eInList));
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
                    Self = Parent.Document.Xml.CreateElement(Name);
                    bean.AppendChild(Self);
                }
                else
                {
                    if (this.Self.Name != Name)
                    {
                        // Name Change
                        XmlElement e = Parent.Document.Xml.CreateElement(Name);
                        bean.ReplaceChild(e, Self);
                        Self = e;
                    }
                }
                if (GridColumnNameWidth > 0)
                    Self.SetAttribute("nw", GridColumnNameWidth.ToString());
                if (GridColumnValueWidth > 0)
                    Self.SetAttribute("vw", GridColumnValueWidth.ToString());

                if (Beans.Count > 0) // 这里没有判断Type，直接根据数据来决定怎么保存。
                {
                    if (null == SelfList)
                    {
                        SelfList = Parent.Document.Xml.CreateElement("list");
                        Self.AppendChild(SelfList);
                    }
                    for (int i = 0; i < Beans.Count; ++i)
                    {
                        Bean b = Beans[i];
                        b.RowIndex = i;
                        b.Save(SelfList);
                    }
                }
                else
                {
                    Self.InnerText = Value;
                }
            }
        }

        public SortedDictionary<string, VarData> VariableMap { get; } = new SortedDictionary<string, VarData>();
        public XmlElement Self { get; set; }
        public Document Document { get; }

        public int RowIndex { get; set; } = -1; // 仅在Save时设置，写到文件以后，好读点。

        public Bean(Document doc, XmlElement self)
        {
            this.Self = self;
            this.Document = doc;

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

        public Bean(Document doc)
        {
            this.Document = doc;
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

        public enum EUpdate
        {
            Data,
            Grid,
            DeleteData,
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

        public bool Update(DataGridView grid, DataGridViewCellCollection cells,
            ref int colIndex, int pathIndex, EUpdate uptype)
        {
            // ColumnCount maybe change in loop
            for (; colIndex < grid.ColumnCount; ++colIndex)
            {
                ColumnTag tag = (ColumnTag)grid.Columns[colIndex].Tag;
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
                    switch (uptype)
                    {
                        case EUpdate.Data:
                            break; // will new data
                        case EUpdate.Grid:
                            if (varInfo.Define.Type == VarDefine.EType.List)
                            {
                                if (tag.Tag == ColumnTag.ETag.ListStart)
                                    ++colIndex;
                                colIndex = Document.Main.FindColumnListEnd(grid, colIndex);
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
                if (varInfo.Define.Type == VarDefine.EType.List)
                {
                    if (uptype == EUpdate.DeleteData)
                    {
                        if (pathIndex + 1 < tag.Path.Count)
                        {
                            if (varInfo.ListIndex < varData.Beans.Count)
                            {
                                Bean bean1 = varData.Beans[varInfo.ListIndex];
                                if (null != bean1)
                                    bean1.Update(grid, cells, ref colIndex, pathIndex + 1, uptype); // always return true;
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
                        if (EUpdate.Data == uptype)
                        {
                            for (int i = varData.Beans.Count; i < varInfo.ListIndex; ++i)
                            {
                                varData.Beans.Add(new Bean(Document));
                            }
                            Bean create = new Bean(Document);
                            varData.Beans.Add(create);
                            if (create.Update(grid, cells, ref colIndex, pathIndex + 1, uptype))
                                return true;
                        }
                        // 忽略剩下的没有数据的item直到ListEnd。
                        colIndex = Document.Main.FindColumnListEnd(grid, colIndex);
                        continue;
                    }

                    Bean bean = varData.Beans[varInfo.ListIndex];
                    if (null != bean)
                    {
                        if (bean.Update(grid, cells, ref colIndex, pathIndex + 1, uptype))
                            return true;
                        continue;
                    }
                    if (EUpdate.Data == uptype)
                    {
                        Bean create = new Bean(Document);
                        varData.Beans[varInfo.ListIndex] = create;
                        if (create.Update(grid, cells, ref colIndex, pathIndex + 1, uptype))
                            return true;
                    }
                    continue;
                }

                if (pathIndex + 1 != tag.Path.Count)
                    throw new Exception("Remain Path, But Is Not A List");

                switch (uptype)
                {
                    case EUpdate.Data:
                        // OnGridCellEndEdit update data
                        // TODO Enum Type
                        DataGridViewCell cell = cells[colIndex];
                        string newValue = cell.Value as string;
                        varData.Value = newValue;
                        return true;

                    case EUpdate.Grid:
                        cells[colIndex].Value = varData.Value; // upate to grid
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

        public void Save(XmlElement parent)
        {
            if (null == Self)
            {
                Self = Document.Xml.CreateElement("bean");
                parent.AppendChild(Self);
            }
            if (RowIndex >= 0)
                Self.SetAttribute("row", RowIndex.ToString());
            foreach (var v in VariableMap.Values)
            {
                v.Save(Self);
            }
        }
    }
}
