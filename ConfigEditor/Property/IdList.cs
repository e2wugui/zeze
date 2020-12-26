using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class IdList : UniqueList
    {
        public override string Name => "id.list";

        public override string Comment => "在所有 List.Item 中保持唯一（横向）。并且生成代码时被当作Map.Key。";

        public override void VerifyCell(VerifyParam param)
        {
            base.VerifyCell(param);
        }
    }
}
