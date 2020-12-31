using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class Server : IProperty
    {
        public static readonly string PName = "server";

        public override string Name => PName;
        public override string Comment => "生成server发布数据时，包含此项数据";
        public override Group Group => Group.DataOutputType;

        public override void VerifyCell(VerifyParam param)
        {
        }
    }
}
