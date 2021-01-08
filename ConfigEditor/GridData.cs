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
        public DataGridView DataGridView { get; set; }

        public class Column
        {
            public string HeaderText { get; set; }
            public string ToolTipText { get; set; }
            public bool ReadOnly { get; set; }
            public string CellTemplateValue { get; set; }
            public ColumnTag Tag { get; set; }
        }

        public class Row
        {
            public GridData Parent { get; }

            // 动态存储行数据，这样增加列的时候不用每一行修改。仅在需要创建（一般是输入）时才创建。
            // 列的顺序调整也不需要调整 Row.Cells。
            private Dictionary<Column, Cell> Cells = new Dictionary<Column, Cell>();

            // maybe null
            public Cell GetCell(int columnIndex)
            {
                var column = Parent.Columns[columnIndex];
                if (Cells.TryGetValue(column, out var cell))
                {
                    return cell;
                }
                return null;
            }

            public Cell GetCellOrAdd(int columnIndex)
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
            public string Value { get; set; }
            public System.Drawing.Color BackColor { get; set; }
        }

        private List<Column> Columns = new List<Column>();
        private List<Row> Rows = new List<Row>();

        public void InsertColumn(int columnIndex, Column column)
        {
            Columns.Insert(columnIndex, column);

            DataGridView?.Columns.Insert(columnIndex,
                new DataGridViewColumn(
                    new DataGridViewTextBoxCell()
                    {
                        Value = column.CellTemplateValue
                    })
                {
                    Width = column.Tag.PathLast.Define.GridColumnValueWidth,
                    HeaderText = column.HeaderText,
                    ReadOnly = column.ReadOnly,
                    ToolTipText = column.ToolTipText,
                    Tag = column.Tag,
                    Frozen = false,
                    AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                });
        }

        public void RemoveColumn(int columnIndex)
        {
            var col = Columns[columnIndex];
            foreach (var row in Rows)
            {
                row.RemoveCell(col);
            }
            Columns.RemoveAt(columnIndex);

            DataGridView?.Columns.RemoveAt(columnIndex);
        }

        // TODO Virtual: 行同步
        public void InsertRow(int rowIndex, Row row)
        {
            Rows.Insert(rowIndex, row);
        }

        public Row GetRow(int rowIndex)
        {
            return Rows[rowIndex];
        }

        public Row AddRow()
        {
            var row = new Row(this);
            Rows.Add(row);
            return row;
        }
    }
}
