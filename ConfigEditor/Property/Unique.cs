using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class Unique : IProperty
    {
        public override string Name => "unique";
        public override string Comment => "验证是否在该列所有数据中唯一";

        public override void VerifyCell(VerifyParam p)
        {
            DataGridViewCell cell = p.Grid[p.ColumnIndex, p.RowIndex];
            string value = cell.Value as string;
            if (null == value)
                value = "";

            if (false == p.ColumnTag.UniqueIndex.TryGetValue(value, out var cells))
                throw new Exception("数据都建了索引，找不到？哪里漏了吧。");

            if (cells.Count > 1)
            {
                p.FormMain.FormError.AddError(cells, this, ErrorLevel.Error, "Unique 重复了");
            }
            else
            {
                p.FormMain.FormError.RemoveError(p.Cell, this);
            }
        }
    }
}
