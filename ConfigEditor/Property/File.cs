using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class File : IProperty
    {
        public override string Name => "file";
        public override string Comment => "表明此项数据是个文件，自动验证文件是否存在。";

        public override Group Group => Group.DataType;

        public override Result VerifyCell(DataGridView grid, int columnIndex, int rowIndex)
        {
            throw new NotImplementedException();
        }
    }
}
