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
        public static new readonly string PName = "id";
        public override string Name => PName;
        public override string Comment => "验证是否在该列所有数据中唯一。并且生成代码时被当作Map.Key。";

        public override void VerifyCell(VerifyParam param)
        {
            base.VerifyCell(param);
        }
    }
}
