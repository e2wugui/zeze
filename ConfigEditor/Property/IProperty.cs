using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public enum Result
    {
        Ok,
        Warn,
        Error,
    }

    // 使用 abstract class 可以在子类写 override，这样删除接口时会报错。
    public abstract class IProperty
    {
        public abstract string Name { get; }
        public abstract Result VerifyCell(DataGridView grid, int columnIndex, int rowIndex);
    }
}
