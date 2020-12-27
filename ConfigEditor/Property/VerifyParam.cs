using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    /// <summary>
    /// 使用这个类来包装验证Cell需要的参数。
    /// 便于以后需要的时候添加参数。
    /// </summary>
    public class VerifyParam
    {
        public FormMain FormMain { get; set; }
        public DataGridView Grid { get; set; }
        public int ColumnIndex { get; set; }
        public int RowIndex { get; set; }
        public ColumnTag ColumnTag { get; set; }

        // helper
        public DataGridViewCell Cell { get { return Grid[ColumnIndex, RowIndex]; } }
    }
}
