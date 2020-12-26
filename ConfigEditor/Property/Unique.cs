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

        public override void VerifyCell(VerifyParam param)
        {
            throw new NotImplementedException();
        }
    }
}
