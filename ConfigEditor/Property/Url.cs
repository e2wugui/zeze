﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Net;

namespace ConfigEditor.Property
{
    public class Url : IProperty
    {
        public override string Name => "url";
        public override string Comment => "表明此数据是个url，自动验证是否可达。";

        public override Group Group => Group.DataType;

        public async override void VerifyCell(VerifyParam p)
        {
            if (p.NewValue.Length == 0)
            {
                p.FormMain.FormError.RemoveError(p.Cell, this);
                return;
            }

            string error = await Task.Run<string>(
                () =>
                {
                    try
                    {
                        WebRequest req = WebRequest.Create(p.NewValue);
                        using (WebResponse res = req.GetResponse())
                        {
                            if (res is HttpWebResponse httpres)
                            {
                                if (httpres.StatusCode == HttpStatusCode.OK || httpres.StatusCode == HttpStatusCode.PartialContent)
                                    return null;
                                return httpres.StatusDescription;
                            }
                            else
                            {
                                // 不是 http，尝试读取一下。
                                byte[] buffer = new byte[256];
                                int rc = res.GetResponseStream().Read(buffer, 0, buffer.Length);
                                if (rc >= 0)
                                    return null;
                                return "rc < 0?";
                            }
                        }
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
