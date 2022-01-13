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
                Gen(sw, doc, doc.BeanDefine, "    ", flags);
                sw.WriteLine($"}}"); // end of namespace
            }
        }

        public static void Gen(System.IO.StreamWriter sw, Document doc, EnumDefine e, string prefix, Property.DataOutputFlags flags)
        {
            sw.WriteLine($"{prefix}public enum {e.Name}");
            sw.WriteLine($"{prefix}{{");
            foreach (var v in e.ValueMap.Values)
            {
                sw.WriteLine($"{prefix}    {v.Name} = {v.Value},");
            }
            sw.WriteLine($"{prefix}}}");
        }

        public static void Gen(System.IO.StreamWriter sw, Document doc, BeanDefine bean, string prefix, Property.DataOutputFlags flags)
        {
            sw.WriteLine($"{prefix}public class {bean.Name}");
            sw.WriteLine($"{prefix}{{");

            // sub bean
            foreach (var sub in bean.BeanDefines.Values)
            {
                Gen(sw, doc, sub, prefix + "    ", flags);
            }

            foreach (var e in bean.EnumDefines.Values)
            {
                Gen(sw, doc, e, prefix + "    ", flags);
            }

            if (false == FormMain.Instance.PropertyManager.Properties.TryGetValue(Property.IdList.PName, out var pid))
                throw new Exception("Property.Id miss!");

            // var property
            foreach (var var in bean.Variables)
            {
                if (0 == (var.DataOutputFlags & flags))
                    continue;

                sw.WriteLine($"{prefix}    public {TypeHelper.GetName(var)} V{var.Name} {{ get; set; }}{TypeHelper.GetDefaultInitialize(var)}");
                if (var.Type == VarDefine.EType.List)
                {
                    foreach (var varRef in var.Reference.Variables)
                    {
                        if (0 == (varRef.DataOutputFlags & flags))
                            continue;
                        if (false == varRef.IsKeyable())
                            continue;

                        if (false == varRef.PropertiesList.Contains(pid))
                            continue;

                        sw.WriteLine($"{prefix}    public Dictionary<{TypeHelper.GetName(varRef)}, {var.Reference.FullName()}> V{var.Name}Map{varRef.Name} {{ get; }} = new Dictionary<{TypeHelper.GetName(varRef)}, {var.Reference.FullName()}>();");
                    }
                }
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
                TypeHelper.GenLoader(sw, doc, var, prefix + "                    ", flags);
                sw.WriteLine($"{prefix}                    break;");
                sw.WriteLine();
            }

            sw.WriteLine($"{prefix}                default:");
            sw.WriteLine($"{prefix}                    throw new Exception(\"unknown var name: \" + e.Name);");
            sw.WriteLine($"{prefix}            }}");
            sw.WriteLine($"{prefix}        }}");
            sw.WriteLine($"{prefix}    }}");
            sw.WriteLine($"{prefix}}}"); // end of bean
            sw.WriteLine();
        }
    }
}
