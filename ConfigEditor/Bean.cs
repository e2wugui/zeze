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
            public string Value { get; set; }
            public int GridColumnNameWidth { get; set; }
            public int GridColumnValueWidth { get; set; }

            public Bean Parent { get; set; }
            public List<Bean> Beans { get; } = new List<Bean>(); // 变量是list或者bean的时候用来存储数据。
            public XmlElement SelfList { get; set; }
            public XmlElement Self { get; set; }

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
                Self.SetAttribute("nw", GridColumnNameWidth.ToString());
                Self.SetAttribute("vw", GridColumnValueWidth.ToString());

                int notNullCount = 0;
                foreach (var b in Beans)
                {
                    if (null != b)
                        ++notNullCount;
                }
                if (notNullCount > 0) // 这里没有判断Type，直接根据数据来决定怎么保存。
                {
                    if (null == SelfList)
                    {
                        SelfList = Parent.Document.Xml.CreateElement("list");
                        Self.AppendChild(SelfList);
                    }
                    foreach (var b in Beans)
                    {
                        b?.Save(SelfList);
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
            }
        }

        public enum EUpdate
        {
            Data,
            Grid,
            DeleteData,
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
                        return false; // end of bean
                }

                ColumnTag.VarInfo varInfo = tag.Path[pathIndex];
                if (false == VariableMap.TryGetValue(varInfo.Define.Name, out var varData))
                {
                    if (EUpdate.Data != uptype)
                        continue; // data not found. done.
                    varData = new VarData(this, varInfo.Define.Name);
                    VariableMap.Add(varInfo.Define.Name, varData);
                }
                if (varInfo.Define.GetEType() == VarDefine.EType.List)
                {
                    if (uptype == EUpdate.DeleteData)
                    {
                        if (ColumnTag.ETag.ListStart != tag.Tag)
                            throw new Exception("应该仅在Tag为ListStart时移除数据. see FormMain.deleteVariable...");
                        if (pathIndex + 1 < tag.Path.Count)
                        {
                            Bean bean1 = varData.Beans[varInfo.ListIndex];
                            if (null != bean1)
                                bean1.Update(grid, cells, ref colIndex, pathIndex + 1, uptype); // always return true;
                        }
                        else
                        {
                            DeleteVarData(varInfo.Define.Name);
                        }
                        return true;
                    }
                    if (ColumnTag.ETag.ListStart == tag.Tag)
                        continue;

                    if (tag.Tag == ColumnTag.ETag.ListEnd)
                    {
                        int curListCount = -varInfo.ListIndex;
                        int add = 0;
                        for (int i = curListCount; i < varData.Beans.Count; ++i)
                        {
                            add += tag.PathLast.Define.Parent.BuildGridColumns(grid, colIndex + add,
                                tag.Parent(ColumnTag.ETag.Normal), i, false);
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
                                varData.Beans.Add(null); // List中间的Bean先使用null填充。
                            }
                            Bean create = new Bean(Document);
                            varData.Beans.Add(create);
                            if (create.Update(grid, cells, ref colIndex, pathIndex + 1, uptype))
                                return true;
                        }
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
                        varData.Value = (string)cells[colIndex].Value; // OnGridCellEndEdit update data
                        return true;
                    case EUpdate.Grid:
                        cells[colIndex].Value = varData.Value; // upate to grid
                        break; // Update Grid 等到 ColumnTag.ETag.AddVariable 才返回。在这个函数开头。
                    case EUpdate.DeleteData:
                        DeleteVarData(varInfo.Define.Name);
                        return true;
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
            foreach (var v in VariableMap.Values)
            {
                v.Save(Self);
            }
        }
    }
}
