using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class Id : Unique
    {
        public override string Name => "id";
        public override string Comment => "验证是否在该列所有数据中唯一。并且生成代码时被当作Map.Key。";

        public override Result VerifyCell(DataGridView grid, int columnIndex, int rowIndex)
        {
            return base.VerifyCell(grid, columnIndex, rowIndex);
        }
    }
}
