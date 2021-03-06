﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Property
{
    public class Directory : IProperty
    {
        public override string Name => "directory";

        public override Group Group => Group.DataType;

        public override string Comment => "表明此项数据是个目录，自动验证目录是否存在。";

        public override void VerifyCell(VerifyParam param)
        {
            if (string.IsNullOrEmpty(param.FormMain.ConfigProject.ResourceDirectory))
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, "资源路径没有配置。");
            }
            else
            {
                string path = System.IO.Path.Combine(param.FormMain.ConfigProject.ResourceDirectory, param.NewValue);
                if (false == System.IO.Directory.Exists(path))
                    param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, "路径不存在。");
                else
                    param.FormMain.FormError.RemoveError(param.Cell, this);
            }
        }
    }
}
