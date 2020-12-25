using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class Dns : IProperty
    {
        public override string Name => "dns";
        public override string Comment => "表明此项数据时dns，自动验证能否解析。";

        public override Group Group => Group.DataType;

        public override Result VerifyCell(DataGridView grid, int columnIndex, int rowIndex)
        {
            throw new NotImplementedException();
        }
    }
}
