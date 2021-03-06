﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Property
{
    public class File : IProperty
    {
        public override string Name => "file";
        public override string Comment => "表明此项数据是个文件，自动验证文件是否存在。";

        public override Group Group => Group.DataType;

        public override void VerifyCell(VerifyParam param)
        {
            if (string.IsNullOrEmpty(param.FormMain.ConfigProject.ResourceDirectory))
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, "资源路径没有配置。");
            }
            else
            {
                string path = System.IO.Path.Combine(param.FormMain.ConfigProject.ResourceDirectory, param.NewValue);
                if (false == System.IO.File.Exists(path))
                    param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, "文件不存在。");
                else
                    param.FormMain.FormError.RemoveError(param.Cell, this);
            }
        }
    }
}
