using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Gen.cs
{
    public class TypeHelper
    {
        public static string GetName(VarDefine var)
        {
            switch (var.Type)
            {
                case VarDefine.EType.Double: return "double";
                case VarDefine.EType.Enum: return "Enum" + var.Name;
                case VarDefine.EType.Float: return "float";
                case VarDefine.EType.Int: return "int";
                case VarDefine.EType.List: return $"List<{"Config." + var.Value}>";
                case VarDefine.EType.Long: return "long";
                case VarDefine.EType.String: return "string";
                case VarDefine.EType.Undecided: return "string"; // TODO
                default: throw new Exception("unknown type");
            }
        }

        public static void GenLoader(System.IO.StreamWriter sw, VarDefine var, string prefix, Property.DataOutputFlags flags)
        {
            switch (var.Type)
            {
                case VarDefine.EType.Double:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText)) V{var.Name} = double.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.Enum:
                    // TODO
                    break;

                case VarDefine.EType.Float:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText)) V{var.Name} = float.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.Int:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText)) V{var.Name} = int.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.List:
                    sw.WriteLine($"{prefix}foreach (XmlNode nodeList in e.ChildNodes)");
                    sw.WriteLine($"{prefix}{{");
                    sw.WriteLine($"{prefix}    if (XmlNodeType.Element != nodeList.NodeType)");
                    sw.WriteLine($"{prefix}        continue;");
                    sw.WriteLine($"{prefix}    XmlElement eList = (XmlElement)nodeList;");
                    sw.WriteLine($"{prefix}    switch (eList.Name)");
                    sw.WriteLine($"{prefix}    {{");
                    sw.WriteLine($"{prefix}        case \"list\":");
                    sw.WriteLine($"{prefix}            foreach (XmlNode bInList in eList.ChildNodes)");
                    sw.WriteLine($"{prefix}            {{");
                    sw.WriteLine($"{prefix}                if (XmlNodeType.Element != bInList.NodeType)");
                    sw.WriteLine($"{prefix}                    continue;");
                    sw.WriteLine($"{prefix}                XmlElement eInList = (XmlElement)bInList;");
                    sw.WriteLine($"{prefix}                if (!eInList.Name.Equals(\"bean\"))");
                    sw.WriteLine($"{prefix}                    throw new Exception(\"Unknown Element In List\");");
                    sw.WriteLine($"{prefix}                V{var.Name}.Add(new {var.Reference.FullName()}(eInList));");
                    sw.WriteLine($"{prefix}            }}");
                    sw.WriteLine($"{prefix}            break;");
                    sw.WriteLine($"{prefix}        default:");
                    sw.WriteLine($"{prefix}            throw new Exception(\"Unknown Element In Var\");");
                    sw.WriteLine($"{prefix}    }}");
                    sw.WriteLine($"{prefix}}}");
                    break;

                case VarDefine.EType.Long:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText)) V{var.Name} = long.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.String:
                    sw.WriteLine($"{prefix}V{var.Name} = e.InnerText;");
                    break;

                case VarDefine.EType.Undecided:
                    sw.WriteLine($"{prefix}V{var.Name} = e.InnerText;");
                    break; // TODO

                default:
                    throw new Exception("unknown type");
            }
        }
    }
}
