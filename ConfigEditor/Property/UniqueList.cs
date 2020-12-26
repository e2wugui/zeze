using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class UniqueList : IProperty
    {
        public override string Name => "unique.list";

        public override string Comment => "在所有 List.Item 中保持唯一（横向）。";

        public override void VerifyCell(VerifyParam p)
        {
            int colListStart = p.FormMain.FindColumnListStart(p.Grid, p.ColumnIndex);
            if (colListStart < 0)
                return; // Not A List Item
            int colListEnd = p.FormMain.FindColumnListEnd(p.Grid, p.ColumnIndex);
            if (colListStart < 0)
                return; // Not A List Item. FindColumnListStart 应该足够判断了，都判断一下吧。

            // collect list item cell
            int colBeanBegin = colListStart + 1;
            HashSet<DataGridViewCell> cells = new HashSet<DataGridViewCell>();
            while (colBeanBegin < colListEnd)
            {
                colBeanBegin = p.FormMain.DoActionUntilBeanEnd(p.Grid, colBeanBegin, colListEnd, (int col) =>
                {
                    ColumnTag tag = (ColumnTag)p.Grid.Columns[col].Tag;
                    if (tag.Tag != ColumnTag.ETag.Normal)
                        return;

                    if (tag.Path.Count == p.ColumnTag.Path.Count // same level.
                        && tag.PathLast.Define == p.ColumnTag.PathLast.Define) // same var
                    {
                        cells.Add(p.Grid.Rows[p.RowIndex].Cells[col]);
                    }
                });
            }

            // count same value
            HashSet<DataGridViewCell> same = new HashSet<DataGridViewCell>();
            DataGridViewCell current = p.Grid[p.ColumnIndex, p.RowIndex];
            string currentValue = current.Value as string;
            if (currentValue == null)
                currentValue = "";
            foreach (var c in cells)
            {
                string str = c.Value as string;
                if (null == str)
                    str = "";
                if (currentValue.Equals(str))
                    same.Add(c);
            }

            // report
            if (same.Count > 1)
            {
                p.Cells = same;
                ReportVerifyResult(p, Result.Error, "UniqueList 重复啦。");
                p.Cells = null;
            }
            else
            {
                // TODO 恢复受影响的其他cell.
                ReportVerifyResult(p);
            }
        }
    }
}
