using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public class GridData
    {
        /// <summary>
        /// DataGridView 同步不能在后台线程中调用。
        /// 必须在把控制权交给 UI 时设置 DataGridView。
        /// 一般流程是后台线程装载初始化 GridData，准备好以后，交给 UI。
        /// UI 创建 DataGridView 并和 GridData 建立关联。
        /// 自此之后的 GridData 操作就会和 DataGridView 同步。
        /// 但是就不能在其他线程再访问了。
        /// </summary>

        private DataGridView _View;

        public DataGridView View
        {
            get
            {
                return _View;
            }
            set
            {
                _View = value;
                if (_View != null)
                    InitializeView();
            }
        }

        public Document Document { get; }
        public GridData(Document doc)
        {
            Document = doc;
        }

        public class Column
        {
            public string HeaderText { get; set; }
            public string ToolTipText { get; set; }
            public bool ReadOnly { get; set; }
            public ColumnTag ColumnTag { get; set; }
        }

        public class Row
        {
            public GridData GridData { get; }
            public List<Cell> Cells { get; } = new List<Cell>();

            public Row(GridData parent)
            {
                GridData = parent;
                for (int col = 0; col < GridData.ColumnCount; ++col)
                    Cells.Add(new Cell(this));
            }
        }

        public class Cell
        {
            public string Value { get; set; } = "";
            public System.Drawing.Color BackColor { get; set; } = System.Drawing.Color.White;
            public string ToolTipText { get; set; }
            public Row Row { get; }

            public void Invalidate()
            {
                if (null != Row.GridData.View)
                {
                    int col = Row.Cells.IndexOf(this);
                    int row = Row.GridData.IndexOfRow(Row);
                    Row.GridData.View.InvalidateCell(col, row);
                }
            }

            public Cell(Row row)
            {
                Row = row;
            }
        }

        private List<Column> Columns = new List<Column>();
        private List<Row> Rows = new List<Row>();

        public int IndexOfRow(Row row)
        {
            return Rows.IndexOf(row);
        }

        private void InitializeView()
        {
            View.SuspendLayout();
            View.Columns.Clear();
            View.Rows.Clear();
            for (int i = 0; i < Columns.Count; ++i)
            {
                InsertColumnToView(i, Columns[i]);
            }
            View.RowCount = Rows.Count;
            View.ResumeLayout();
        }

        private void InsertColumnToView(int columnIndex, Column column)
        {
            View?.Columns.Insert(columnIndex,
                new DataGridViewColumn(new DataGridViewTextBoxCell())
                {
                    Width = column.ColumnTag == null ? 80 : column.ColumnTag.PathLast.Define.GridColumnValueWidth,
                    HeaderText = column.HeaderText,
                    ReadOnly = column.ReadOnly,
                    ToolTipText = column.ToolTipText,
                    Tag = column.ColumnTag,
                    Frozen = false,
                    AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                });
        }

        public int ColumnCount => Columns.Count;
        public int RowCount => Rows.Count;

        public Column GetColumn(int columnIndex)
        {
            return Columns[columnIndex];
        }

        public void InsertColumn(int columnIndex, Column column)
        {
            Columns.Insert(columnIndex, column);
            foreach (var row in Rows)
            {
                row.Cells.Insert(columnIndex, new Cell(row));
                // TODO fromerror sync
            }
            InsertColumnToView(columnIndex, column);
        }

        public void RemoveColumn(int columnIndex)
        {
            foreach (var row in Rows)
            {
                row.Cells.RemoveAt(columnIndex);
                // TODO fromerror sync
            }
            Columns.RemoveAt(columnIndex);
            View?.Columns.RemoveAt(columnIndex);
        }

        public void AddRow()
        {
            InsertRow(RowCount);
        }

        public void InsertRow(int rowIndex)
        {
            var row = new Row(this);
            Rows.Insert(rowIndex, row);

            // SetSpecialColumnText
            for (int colIndex = 0; colIndex < ColumnCount; ++colIndex)
            {
                Column col = Columns[colIndex];
                switch (col.ColumnTag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                        row.Cells[colIndex].Value = ",";
                        break;
                    case ColumnTag.ETag.ListStart:
                        row.Cells[colIndex].Value = "[";
                        break;
                    case ColumnTag.ETag.ListEnd:
                        row.Cells[colIndex].Value = "]";
                        break;
                }
            }

            View?.Rows.Insert(rowIndex, 1);
        }

        public void BuildUniqueIndexOnAddRow(int rowIndex)
        {
            var row = Rows[rowIndex];
            for (int i = 0; i < ColumnCount; ++i)
            {
                ColumnTag tag = Columns[i].ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        continue;
                }
                var cell = row.Cells[i];
                tag.AddUniqueIndex(cell.Value, cell);
            }
        }

        public void RemoveRow(int rowIndex)
        {
            Rows.RemoveAt(rowIndex);
            View?.Rows.RemoveAt(rowIndex);
        }

        public Row GetRow(int rowIndex)
        {
            return Rows[rowIndex];
        }

        public Cell GetCell(int columnIndex, int rowIndex)
        {
            return Rows[rowIndex].Cells[columnIndex];
        }

        public int FindColumnListEnd(int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex; c < ColumnCount; ++c)
            {
                ColumnTag tag = Columns[c].ColumnTag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            --skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            ++skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListStart:
                        ++skipNestListCount;
                        break;
                    //case ColumnTag.ETag.AddVariable:
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        return c;
                }
            }
            return -1;
        }

        public void VerifyAll()
        {
            try
            {
                FormMain.Instance.FormError.RemoveErrorByGrid(this);

                for (int rowIndex = 0; rowIndex < RowCount; ++rowIndex)
                {
                    for (int colIndex = 0; colIndex < ColumnCount; ++colIndex)
                    {
                        ColumnTag tag = Columns[colIndex].ColumnTag;

                        if (tag.Tag != ColumnTag.ETag.Normal)
                            continue;

                        Cell cell = GetCell(colIndex, rowIndex);
                        var param = new Property.VerifyParam()
                        {
                            FormMain = FormMain.Instance,
                            Grid = this,
                            ColumnIndex = colIndex,
                            RowIndex = rowIndex,
                            ColumnTag = tag,
                            NewValue = cell.Value,
                        };

                        foreach (var p in tag.PathLast.Define.PropertiesList)
                        {
                            p.VerifyCell(param);
                        }
                        tag.PathLast.Define.Verify(param);
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        public void UpdateWhenAddVariable(VarDefine var)
        {
            for (int c = 0; c < ColumnCount; ++c)
            {
                ColumnTag tagref = Columns[c].ColumnTag;
                if (tagref.Tag == ColumnTag.ETag.AddVariable && tagref.PathLast.Define.Parent == var.Parent)
                {
                    c += var.BuildGridColumns(this, c, tagref.Parent(ColumnTag.ETag.Normal), -1);

                    // 如果是List，第一次加入的时候，默认创建一个Item列。
                    // 但是仍然有问题：如果这个Item没有输入数据，下一次打开时，不会默认创建。需要手动增加Item。
                    if (var.Type == VarDefine.EType.List)
                    {
                        ColumnTag tagListEnd = Columns[c - 1].ColumnTag;
                        ColumnTag tagListEndCopy = tagListEnd.Copy(ColumnTag.ETag.Normal);
                        tagListEndCopy.PathLast.ListIndex = -tagListEnd.PathLast.ListIndex; // 肯定是0，保险写法。
                        --tagListEnd.PathLast.ListIndex;
                        c += var.Reference.BuildGridColumns(this, c - 1, tagListEndCopy, -1);
                    }
                    //((Document)gridref.Tag).IsChanged = true; // 引用的Grid仅更新界面，数据实际上没有改变。
                }
            }
        }

        public void DeleteVariable(VarDefine var)
        {
            var updateParam = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.DeleteData }; // never change
            for (int c = 0; c < gridref.ColumnCount; ++c)
            {
                ColumnTag tagref = (ColumnTag)gridref.Columns[c].Tag;
                if (tagref.PathLast.Define == var)
                {
                    // delete data
                    for (int r = 0; r < gridref.RowCount - 1; ++r)
                    {
                        DataGridViewCellCollection cells = gridref.Rows[r].Cells;
                        int colref = c;
                        doc.Beans[r].Update(gridref, cells, ref colref, 0, updateParam);
                    }
                    // delete columns
                    switch (tagref.Tag)
                    {
                        case ColumnTag.ETag.Normal:
                            gridref.Columns.RemoveAt(c);
                            --c;
                            break;
                        case ColumnTag.ETag.ListStart:
                            int colListEnd = FindCloseListEnd(gridref, c);
                            while (colListEnd >= c)
                            {
                                gridref.Columns.RemoveAt(colListEnd);
                                --colListEnd;
                            }
                            --c;
                            break;
                        default:
                            MessageBox.Show("ListEnd?");
                            break;
                    }
                    doc.IsChanged = true;
                }
            }
        }
    }
}
