using System;
using System.Collections.Generic;
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
                sw.WriteLine();
            }
            // declare variables
            foreach (Types.Variable v in bean.Variables)
            {
                sw.WriteLine($"    public {v.Name}: {TypeName.GetName(v.VariableType)}; {v.Comment}");
            }
            sw.WriteLine();
            sw.WriteLine();
            Construct.Make(bean, sw, "    ");
            sw.WriteLine();
            sw.WriteLine("    public static readonly TYPEID: bigint = " + bean.TypeId + "n;");
            sw.WriteLine("    public TypeId(): bigint { return " + bean.Space.Path("_", bean.Name) + ".TYPEID; }");
            sw.WriteLine();
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            MakeDynamicStaticFunc(sw);
            sw.WriteLine("}");
            sw.WriteLine();
        }

        public void MakeDynamicStaticFunc(System.IO.StreamWriter sw)
        {
            foreach (var v in bean.Variables)
            {
                if (v.VariableType is Types.TypeDynamic d)
                {
                    foreach (var real in d.RealBeans)
                    {
                        sw.WriteLine($"    public static readonly DynamicTypeId{v.NameUpper1}{real.Value.Space.Path("_", real.Value.Name)} : bigint = {real.Key}n;");
                    }
                    if (d.RealBeans.Count > 0)
                        sw.WriteLine();

                    sw.WriteLine($"    public static GetSpecialTypeIdFromBean_{v.NameUpper1}(bean: Zeze.Bean): bigint {{");
                    sw.WriteLine($"        switch (bean.TypeId())");
                    sw.WriteLine($"        {{");
                    sw.WriteLine($"            case Zeze.EmptyBean.TYPEID: return Zeze.EmptyBean.TYPEID;");
                    foreach (var real in d.RealBeans)
                    {
                        sw.WriteLine($"            case {real.Value.TypeId}n: return {real.Key}n; // {real.Value.FullName}");
                    }
                    sw.WriteLine($"        }}");
                    sw.WriteLine($"        throw new Error(\"Unknown Bean! dynamic@{(v.Bean as Types.Bean).FullName}:{v.Name}\");");
                    sw.WriteLine($"    }}");
                    sw.WriteLine();
                    sw.WriteLine($"    public static CreateBeanFromSpecialTypeId_{v.NameUpper1}(typeId: bigint): Zeze.Bean {{");
                    sw.WriteLine($"        switch (typeId)");
                    sw.WriteLine($"        {{");
                    //sw.WriteLine($"            case Zeze.EmptyBean.TYPEID: return new Zeze.EmptyBean();");
                    foreach (var real in d.RealBeans)
                    {
                        sw.WriteLine($"            case {real.Key}n: return new {real.Value.Space.Path("_", real.Value.Name)}();");
                    }
                    sw.WriteLine($"        }}");
                    sw.WriteLine($"        return null;");
                    sw.WriteLine($"    }}");
                    sw.WriteLine();
                }
            }
        }
    }
}
