using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class Url : IProperty
    {
        public override string Name => "url";
        public override string Comment => "表明此数据是个url，自动验证是否可达。";

        public override Group Group => Group.DataType;

        public override void VerifyCell(VerifyParam param)
        {
            throw new NotImplementedException();
        }
    }
}
