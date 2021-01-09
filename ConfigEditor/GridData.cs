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
    }
}
