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
            int colListStart = GridData.FindColumnListStart(p.Grid, p.ColumnIndex);
            if (colListStart < 0)
                return; // Not A List Item
            int colListEnd = GridData.FindColumnListEnd(p.Grid, p.ColumnIndex);
            if (colListStart < 0)
                return; // Not A List Item. FindColumnListStart 应该足够判断了，都判断一下吧。

            // collect list item cell
            int colBeanBegin = colListStart + 1;
            List<GridData.Cell> cells = new List<GridData.Cell>();
            while (colBeanBegin < colListEnd)
            {
                colBeanBegin = GridData.DoActionUntilBeanEnd(p.Grid, colBeanBegin, colListEnd, (int col) =>
                {
                    ColumnTag tag = (ColumnTag)p.Grid.GetColumn(col).ColumnTag;
                    if (tag.Tag != ColumnTag.ETag.Normal)
                        return;

                    if (tag.Path.Count == p.ColumnTag.Path.Count // same level.
                        && tag.PathLast.Define == p.ColumnTag.PathLast.Define) // same var
                    {
                        cells.Add(p.Grid.GetRow(p.RowIndex).Cells[col]);
                    }
                });
            }
            ColumnTag tagListEnd = p.Grid.GetColumn(colListEnd).ColumnTag;
            int pathListVar = tagListEnd.Path.Count - 1;
            Bean.VarData varList = tagListEnd.PathLast.Define.Parent.Document.Beans[p.RowIndex]
                .GetVarData(0, tagListEnd, pathListVar);
            int varListCount = varList == null ? 0 : varList.Beans.Count;

            // count same oldValue
            HashSet<GridData.Cell> same = new HashSet<GridData.Cell>();
            if (p.OldValue != null)
            {
                // load时 varListCount 包含所有数据。
                // Validating 时，如果是新增的，varListCount少1。
                // 计数 oldValue 不需要判断新数据，这里不需要额外判断。
                for (int i = 0; i < varListCount; ++i)
                {
                    var c = cells[i];
                    if (c == p.Cell)
                        continue;

                    string str = c.Value as string;
                    if (null == str)
                        str = "";
                    if (p.OldValue.Equals(str))
                        same.Add(c);
                }
                if (same.Count == 1)
                {
                    p.FormMain.FormError.RemoveError(same.First(), this);
                }
                same.Clear();
            }

            // count same newValue
            // load时 varListCount 包含所有数据。
            // Validating 时，如果是新增的，varListCount少1。当前新增cell在后面Add。所以不需要特别处理。
            for (int i = 0; i < varListCount; ++i)
            {
                var c = cells[i];
                if (c == p.Cell)
                    continue;

                string str = c.Value as string;
                if (null == str)
                    str = "";
                if (p.NewValue.Equals(str))
                    same.Add(c);
            }

            // report
            if (same.Count >= 1)
            {
                same.Add(p.Cell);
                p.FormMain.FormError.AddError(same, this, ErrorLevel.Error, Name + " 重复啦。");
            }
            else
            {
                p.FormMain.FormError.RemoveError(p.Cell, this);
            }
        }
    }
}
