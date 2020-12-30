using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor
{
    public class ProjectConfig
    {
        public string ResourceHome { get; set; } // 这个配置用来给 Property.File 用来检查文件是否存在。
    }
}
