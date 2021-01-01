using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Gen.cs
{
    public class BeanFormatter
    {
        public static void Gen(string srcHome, Document doc, Property.DataOutputFlags flags)
        {
            using (System.IO.StreamWriter sw = doc.OpenStreamWriter(srcHome, ".cs"))
            {
                sw.WriteLine($"// auto generated.");
                sw.WriteLine();
                sw.WriteLine($"using System;");
                sw.WriteLine($"using System.Collections.Generic;");
                sw.WriteLine($"using System.Xml;");
                sw.WriteLine();
                sw.WriteLine($"namespace {doc.Namespace}");
                sw.WriteLine($"{{");
                Gen(sw, doc.BeanDefine, "    ", flags);
                sw.WriteLine($"}}"); // end of namespace
            }
        }

        public static void Gen(System.IO.StreamWriter sw, BeanDefine bean, string prefix, Property.DataOutputFlags flags)
        {
            sw.WriteLine($"{prefix}public class {bean.Name}");
            sw.WriteLine($"{prefix}{{");

            // sub bean
            foreach (var sub in bean.BeanDefines.Values)
            {
                Gen(sw, sub, prefix + "    ", flags);
            }

            // var property
            foreach (var var in bean.Variables)
            {
                if (0 == (var.DataOutputFlags & flags))
                    continue;

                // TODO 类型推断。Enum。Map索引。
                sw.WriteLine($"{prefix}    public {TypeHelper.GetName(var)} V{var.Name} {{ get; set; }}");
            }
            sw.WriteLine();

            // load。解析代码都生成吧，虽然有点烦。不过不会代码依赖也好。
            sw.WriteLine($"{prefix}    public {bean.Name}(XmlElement self)");
            sw.WriteLine($"{prefix}    {{");
            sw.WriteLine($"{prefix}        foreach (XmlNode node in self.ChildNodes)");
            sw.WriteLine($"{prefix}        {{");
            sw.WriteLine($"{prefix}            if (XmlNodeType.Element != node.NodeType)");
            sw.WriteLine($"{prefix}                continue;");
            sw.WriteLine($"{prefix}            XmlElement e = (XmlElement)node;");
            sw.WriteLine($"{prefix}            switch (e.Name)");
            sw.WriteLine($"{prefix}            {{");

            foreach (var var in bean.Variables)
            {
                if (0 == (var.DataOutputFlags & flags))
                    continue;
                sw.WriteLine($"{prefix}                case \"{var.Name}\":");
                TypeHelper.GenLoader(sw, var, prefix + "                    ", flags);
                sw.WriteLine($"{prefix}                    break;");
                sw.WriteLine();
            }

            // 删除Var时可能有些文件没有删除数据（在LoadToGrid的时候才删除）；
            // 导出数据为了效率，不严格判断是否被删除，所以可能存在多余的Var数据。
            /*
            sw.WriteLine($"{prefix}                default:");
            sw.WriteLine($"{prefix}                    throw new Exception(\"unkown var name: \" + e.Name);");
            */
            sw.WriteLine($"{prefix}            }}");
            sw.WriteLine($"{prefix}        }}");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}}}"); // end of bean
            sw.WriteLine();
        }
    }
}
