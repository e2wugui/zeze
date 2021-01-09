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
        public override string Comment => "表明此项数据时dns，自动验证能否解析。";

        public override Group Group => Group.DataType;

        public async override void VerifyCell(VerifyParam p)
        {
            if (p.NewValue.Length == 0)
            {
                p.FormMain.FormError.RemoveError(p.Cell, this);
                return;
            }

            string error = await Task.Run<string>(() =>
            {
                try
                {
                    System.Net.Dns.GetHostEntry(p.NewValue);
                    return null;
                }
                catch (Exception ex)
                {
                    return ex.Message;
                }
            });

            if (null != error)
                p.FormMain.FormError.AddError(p.Cell, this, ErrorLevel.Warn, error);
            else
                p.FormMain.FormError.RemoveError(p.Cell, this);
        }
    }
}
