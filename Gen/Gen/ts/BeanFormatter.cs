using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class BeanFormatter
    {
        Bean bean;

        public BeanFormatter(Bean bean)
        {
            this.bean = bean;
        }

        public void Make(System.IO.StreamWriter sw)
        {
            sw.WriteLine();
            sw.WriteLine("export class " + bean.Space.Path("_", bean.Name) + " implements Zeze.Bean {");
            sw.WriteLine("    public static readonly TYPEID: bigint = " + bean.TypeId + "n;");
            sw.WriteLine("    public TypeId(): bigint { return " + bean.Space.Path("_", bean.Name) + ".TYPEID; }");
            sw.WriteLine();
            // declare enums
            foreach (var e in bean.Enums)
                sw.WriteLine("    public static readonly " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (bean.Enums.Count > 0)
                sw.WriteLine();
            // declare variables
            foreach (var v in bean.Variables)
                sw.WriteLine($"    public {v.Name}: {TypeName.GetName(v.VariableType)};{v.Comment}");
            if (bean.Variables.Count > 0)
                sw.WriteLine();
            Construct.Make(bean, sw, "    ");
            MakeDynamicStaticFunc(sw);
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            sw.WriteLine("}");
        }

        private void GenDynamicSpecialMethod(System.IO.StreamWriter sw, Variable v, TypeDynamic d, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in d.RealBeans)
                {
                    sw.WriteLine($"    public static readonly DynamicTypeId_{v.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} : bigint = {real.Key}n;");
                }
                if (d.RealBeans.Count > 0)
                    sw.WriteLine();
            }

            sw.WriteLine($"    public static GetSpecialTypeIdFromBean_{v.Id}(bean: Zeze.Bean): bigint {{");
            sw.WriteLine($"        switch (bean.TypeId()) {{");
            sw.WriteLine($"            case Zeze.EmptyBean.TYPEID: return Zeze.EmptyBean.TYPEID;");
            foreach (var real in d.RealBeans)
            {
                sw.WriteLine($"            case {real.Value.TypeId}n: return {real.Key}n; // {real.Value.FullName}");
            }
            sw.WriteLine($"            default: throw new Error(\"Unknown Bean! dynamic@{((Bean)v.Bean).FullName}:{v.Name}\");");
            sw.WriteLine($"        }}");
            sw.WriteLine($"    }}");
            sw.WriteLine();
            sw.WriteLine($"    public static CreateBeanFromSpecialTypeId_{v.Id}(typeId: bigint): Zeze.Bean | null {{");
            sw.WriteLine($"        switch (typeId) {{");
            //sw.WriteLine($"            case Zeze.EmptyBean.TYPEID: return new Zeze.EmptyBean();");
            foreach (var real in d.RealBeans)
            {
                sw.WriteLine($"            case {real.Key}n: return new {real.Value.Space.Path("_", real.Value.Name)}();");
            }
            sw.WriteLine($"            default: return null;");
            sw.WriteLine($"        }}");
            sw.WriteLine($"    }}");
            sw.WriteLine();
        }

        public void MakeDynamicStaticFunc(System.IO.StreamWriter sw)
        {
            foreach (var v in bean.Variables)
            {
                if (v.VariableType is TypeDynamic d)
                    GenDynamicSpecialMethod(sw, v, d, false);
                else if (v.VariableType is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethod(sw, v, dy1, true);
                else if (v.VariableType is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethod(sw, v, dy2, true);
            }
        }
    }
}
