using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class UniqueList : IProperty
    {
        public override string Name => "unique.list";

        public override string Comment => "在所有 List.Item 中保持唯一（横向）。";

        public override void VerifyCell(VerifyParam param)
        {
            throw new NotImplementedException();
        }
    }
}
