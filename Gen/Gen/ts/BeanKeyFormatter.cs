using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.ts
{
    public class BeanKeyFormatter
    {
        Types.BeanKey beanKey;

        public BeanKeyFormatter(Types.BeanKey beanKey)
        {
            this.beanKey = beanKey;
        }

        public static string GetParamListWithDefault(ICollection<Types.Variable> variables)
        {
            StringBuilder plist = new StringBuilder();
            bool first = true;
            foreach (Types.Variable var in variables)
            {
                if (first)
                    first = false;
                else
                    plist.Append(", ");
                plist.Append(var.NamePrivate).Append("_: ").Append(TypeName.GetName(var.VariableType)).Append(" = ").Append(Default.GetDefault(var));
            }
            return plist.ToString();
        }

        public void Make(System.IO.StreamWriter sw)
        {
            sw.WriteLine();
            sw.WriteLine("export class " + beanKey.Space.Path("_", beanKey.Name) + " implements Zeze.Bean {");
            sw.WriteLine("    public static readonly TYPEID: bigint = " + beanKey.TypeId + "n;");
            sw.WriteLine("    public TypeId(): bigint { return " + beanKey.Space.Path("_", beanKey.Name) + ".TYPEID; }");
            sw.WriteLine();
            // declare enums
            foreach (Types.Enum e in beanKey.Enums)
                sw.WriteLine("    public static readonly " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (beanKey.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            foreach (Types.Variable v in beanKey.Variables)
                sw.WriteLine($"    public {v.Name}: {TypeName.GetName(v.VariableType)};{v.Comment}");
            if (beanKey.Variables.Count > 0)
                sw.WriteLine();

            // params construct with init
            sw.WriteLine("    public constructor(" + GetParamListWithDefault(beanKey.Variables) + ") {");
            foreach (Types.Variable v in beanKey.Variables)
                sw.WriteLine("        this." + v.Name + " = " + v.NamePrivate + "_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            Encode.Make(beanKey, sw, "    ");
            Decode.Make(beanKey, sw, "    ");
            sw.WriteLine("}");
        }
    }
}
