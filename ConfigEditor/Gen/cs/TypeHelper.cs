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
            switch (var.TypeNow)
            {
                case VarDefine.EType.Double: return "double";
                case VarDefine.EType.Enum: return "Enum" + var.Name; // TODO
                case VarDefine.EType.Float: return "float";
                case VarDefine.EType.Int: return "int";
                case VarDefine.EType.List: return $"List<{var.Reference.FullName()}>";
                case VarDefine.EType.Date: return "DateTime";
                case VarDefine.EType.Long: return "long";
                case VarDefine.EType.String: return "string";
                default: throw new Exception("unknown type");
            }
        }

        public static void GenLoader(System.IO.StreamWriter sw, Document doc, VarDefine var, string prefix, Property.DataOutputFlags flags)
        {
            switch (var.TypeNow)
            {
                case VarDefine.EType.Double:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText))");
                    sw.WriteLine($"{prefix}    V{var.Name} = double.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.Enum:
                    // TODO
                    break;

                case VarDefine.EType.Float:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText))");
                    sw.WriteLine($"{prefix}    V{var.Name} = float.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.Int:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText))");
                    sw.WriteLine($"{prefix}    V{var.Name} = int.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.List:
                    sw.WriteLine($"{prefix}Manager.LoadList(e, (XmlElement eInList) =>");
                    sw.WriteLine($"{prefix}{{");
                    sw.WriteLine($"{prefix}    var beanInList = new {var.Reference.FullName()}(eInList);");
                    sw.WriteLine($"{prefix}    V{var.Name}.Add(beanInList);");
                    if (false == doc.Main.PropertyManager.Properties.TryGetValue(Property.IdList.PName, out var pid))
                        throw new Exception("Property.Id miss!");
                    foreach (var varRef in var.Reference.Variables)
                    {
                        if (false == varRef.IsKeyable())
                            continue;

                        if (false == varRef.PropertiesList.Contains(pid))
                            continue;

                        sw.WriteLine($"{prefix}    V{var.Name}Map{varRef.Name}.Add(beanInList.V{varRef.Name}, beanInList);");
                    }
                    sw.WriteLine($"{prefix}}});");
                    break;

                case VarDefine.EType.Long:
                    sw.WriteLine($"{prefix}if (!string.IsNullOrEmpty(e.InnerText))");
                    sw.WriteLine($"{prefix}    V{var.Name} = long.Parse(e.InnerText);");
                    break;

                case VarDefine.EType.String:
                    sw.WriteLine($"{prefix}V{var.Name} = e.InnerText;");
                    break;

                case VarDefine.EType.Date:
                    sw.WriteLine($"{prefix}V{var.Name} = DateTime.Parse(e.InnerText);");
                    break;

                default:
                    throw new Exception("unknown type");
            }
        }
    }
}
