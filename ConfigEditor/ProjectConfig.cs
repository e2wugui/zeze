using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor
{
    [System.AttributeUsage(System.AttributeTargets.Property)]
    public class ShowNameAttribute : System.Attribute
    {
        public string Name { get; }
        public string Tips { get; }
        public ShowNameAttribute(string name, string tips)
        {
            Name = name;
            Tips = tips;
        }
    }

    public class ProjectConfig
    {
        [ShowName("资源路径", "用来验证配置中的文件名是否存在。")]
        public string ResourceHome { get; set; } // 这个配置用来给 Property.File 用来检查文件是否存在。

        [ShowName("数据导出目录", "Build的时候，把编辑数据到处成发布格式时的输出目录")]
        public string BuildDataDirectory { get; set; }

        [ShowName("代码生成目录", "")]
        public string BuildSrcDirectory { get; set; }
    }
}
