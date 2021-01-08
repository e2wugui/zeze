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
                InitializeView();
            }
        }

        public class Column
        {
            public string HeaderText { get; set; }
            public string ToolTipText { get; set; }
            public bool ReadOnly { get; set; }
            public ColumnTag Tag { get; set; }
        }

        class Row
        {
            public GridData Parent { get; }

            // 动态存储行数据，这样增加列的时候不用每一行修改。仅在需要创建（一般是输入）时才创建。
            // 列的顺序调整也不需要调整 Row.Cells。
            private Dictionary<Column, Cell> Cells = new Dictionary<Column, Cell>();

            public Cell GetCell(int columnIndex)
            {
                var column = Parent.Columns[columnIndex];
                if (Cells.TryGetValue(column, out var cell))
                {
                    return cell;
                }
                cell = new Cell();
                Cells.Add(column, cell);
                return cell;
            }

            public Row(GridData parent)
            {
                Parent = parent;
            }

            /// <summary>
            /// 目前仅在 RemoveColumn 中用来删除不需要的数据。
            /// RemoveColumn 会同步 DataGridView.Columns。
            /// 如果外面直接调用这个方法，将不会同步 DataGridView。
            /// </summary>
            /// <param name="column"></param>
            public void RemoveCell(Column column)
            {
                Cells.Remove(column);
            }
        }

        public class Cell
        {
            public string Value { get; set; } = "";
            public System.Drawing.Color BackColor { get; set; } = System.Drawing.Color.White;
        }

        public class CellTemporary
        {
            Cell Cell { get; }

            public int ColumnIndex { get; set; }
            public int RowIndex { get; set; }
            public string Value
            {
                get
                {
                    return Cell.Value;
                }
                set
                {
                    Cell.Value = value;
                }
            }
            public System.Drawing.Color BackColor { get { return Cell.BackColor; } set { Cell.BackColor = value; } }

            public DataGridView View { get; }

            public CellTemporary(Cell store, DataGridView view)
            {
                Cell = store;
                View = view;
            }

            public void Invalidate()
            {
                View?.InvalidateCell(ColumnIndex, RowIndex);
            }
        }

        private List<Column> Columns = new List<Column>();
        private List<Row> Rows = new List<Row>();

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
                    Width = column.Tag == null ? 80 : column.Tag.PathLast.Define.GridColumnValueWidth,
                    HeaderText = column.HeaderText,
                    ReadOnly = column.ReadOnly,
                    ToolTipText = column.ToolTipText,
                    Tag = column.Tag,
                    Frozen = false,
                    AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                });
        }

        public int ColumnCount => Columns.Count;
        public int RowCount => Rows.Count;

        public void InsertColumn(int columnIndex, Column column)
        {
            Columns.Insert(columnIndex, column);
            InsertColumnToView(columnIndex, column);
        }

        public void RemoveColumn(int columnIndex)
        {
            var col = Columns[columnIndex];
            foreach (var row in Rows)
            {
                row.RemoveCell(col);
            }
            Columns.RemoveAt(columnIndex);

            View?.Columns.RemoveAt(columnIndex);
        }

        public void InsertRow(int rowIndex)
        {
            Rows.Insert(rowIndex, new Row(this));
            View?.Rows.Insert(rowIndex, 1);
        }

        public void RemoveRow(int rowIndex)
        {
            Rows.RemoveAt(rowIndex);
            View?.Rows.RemoveAt(rowIndex);
        }

        public CellTemporary GetCell(int columnIndex, int rowIndex)
        {
            Cell store = Rows[rowIndex].GetCell(columnIndex);
            return new CellTemporary(store, View) { ColumnIndex = columnIndex, RowIndex = rowIndex };
        }
    }
}
