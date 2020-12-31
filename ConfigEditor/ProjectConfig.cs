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
        public string ResourceDirectory { get; set; } // 这个配置用来给 Property.File 用来检查文件是否存在。

        [ShowName("数据导出目录", "把编辑数据导出到该目录下。没有配置则不导出。")]
        public string DataOutputDirectory { get; set; }

        [ShowName("Server代码生成目录", "")]
        public string ServerSrcDirectory { get; set; }

        [ShowName("Client代码生成目录", "")]
        public string ClientSrcDirectory { get; set; }

        [ShowName("客户端语言", "支持的客户端语言：ts|lua|cs")]
        public string ClientLanguage { get; set; }
    }
}
