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
                case VarDefine.EType.Enum: return "Enum" + var.Name; // TODO
                case VarDefine.EType.Float: return "number";
                case VarDefine.EType.Int: return "number";
                case VarDefine.EType.List: return $"Array<_{var.Reference.FullName().Replace('.', '_')}>";
                case VarDefine.EType.Date: return "Date";
                case VarDefine.EType.Long: return "bigint";
                case VarDefine.EType.String: return "string";
                default: throw new Exception("unknown type");
            }
        }
    }
}
