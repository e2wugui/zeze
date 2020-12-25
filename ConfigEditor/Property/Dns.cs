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

        public override Result VerifyCell(DataGridView grid, int columnIndex, int rowIndex)
        {
            throw new NotImplementedException();
        }
    }
}
