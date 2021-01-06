using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Gen.ts
{
    public class TypeHelper
    {
        public static string GetName(VarDefine var)
        {
            switch (var.TypeNow)
            {
                case VarDefine.EType.Double: return "number";
                case VarDefine.EType.Enum: return var.FullName().Replace('.', '_');
                case VarDefine.EType.Float: return "number";
                case VarDefine.EType.Int: return "number";
                case VarDefine.EType.List: return $"Array<_{var.Reference.FullName().Replace('.', '_')}>";
                case VarDefine.EType.Date: return "Date";
                case VarDefine.EType.Long: return "bigint";
                case VarDefine.EType.String: return "string";
                default: throw new Exception("unknown type");
            }
        }

        public static string GetDefaultInitialize(VarDefine var)
        {
            switch (var.TypeNow)
            {
                case VarDefine.EType.Double:
                    if (string.IsNullOrEmpty(var.Default))
                        break;
                    return $" = {var.Default}";

                case VarDefine.EType.Enum:
                    if (string.IsNullOrEmpty(var.Default))
                        break;
                    return $" = {var.FullName().Replace('.', '_')}.{var.Default}";

                case VarDefine.EType.Float:
                    if (string.IsNullOrEmpty(var.Default))
                        break;
                    return $" = {var.Default}";

                case VarDefine.EType.Int:
                    if (string.IsNullOrEmpty(var.Default))
                        break;
                    return $" = {var.Default}";

                case VarDefine.EType.List:
                    return $" = new Array<{var.Reference.FullName()}>()";

                case VarDefine.EType.Date:
                    if (string.IsNullOrEmpty(var.Default))
                        return $" = new Date()";
                    return $" = new Date(\"{var.Default}\")";

                case VarDefine.EType.Long:
                    if (string.IsNullOrEmpty(var.Default))
                        break;
                    return $" = {var.Default}";

                case VarDefine.EType.String:
                    return $" = \"{var.Default}\"";

                default:
                    throw new Exception("unknown type");
            }
            return "";
        }
    }
}
