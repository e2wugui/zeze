using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Diagnostics.Contracts;
using System.Net.Mime;
using System.Text;

namespace Zeze.Gen.ts
{
    public class BeanFormatter
    {
        Types.Bean bean;

        public BeanFormatter(Types.Bean bean)
        {
            this.bean = bean;
        }

        public void Make(System.IO.StreamWriter sw)
        {
            sw.WriteLine("export class " + bean.Space.Path("_", bean.Name) + " implements Zeze.Bean {");
            // declare enums
            foreach (Types.Enum e in bean.Enums)
            {
                sw.WriteLine("    public static readonly " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine("");
            }
            // declare variables
            foreach (Types.Variable v in bean.Variables)
            {
                sw.WriteLine($"    public {v.Name}: {TypeName.GetName(v.VariableType)}; {v.Comment}");
            }
            sw.WriteLine("");
            sw.WriteLine();
            Construct.Make(bean, sw, "    ");
            sw.WriteLine();
            sw.WriteLine("    public static readonly TYPEID: bigint = " + bean.TypeId + "n;");
            sw.WriteLine("    public TypeId(): bigint { return " + bean.Space.Path("_", bean.Name) + ".TYPEID; }");
            sw.WriteLine();
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            sw.WriteLine("}");
        }

    }
}
