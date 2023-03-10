using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class BeanFormatter
    {
        readonly Bean bean;

        public BeanFormatter(Bean bean)
        {
            this.bean = bean;
        }

        public void Make(StreamWriter sw)
        {
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine("@SuppressWarnings({\"UnusedAssignment\", \"RedundantIfStatement\", \"SwitchStatementWithTooFewBranches\", \"RedundantSuppression\"})");
            var final = bean.Extendable ? "" : "final ";
            sw.WriteLine($"public static {final}class Data extends Zeze.Transaction.Data {{");
            WriteDefine(sw);
            sw.WriteLine("}");
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}public static final long DynamicTypeId_{var.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                }
            }
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.DynamicBeanData newDynamicBean_{var.NameUpper1}() {{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBeanData({var.Id}, {bean.Name}.Data::getSpecialTypeIdFromBean_{var.Id}, {bean.Name}.Data::createBeanFromSpecialTypeId_{var.Id});");
            else
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBeanData({var.Id}, {type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static long getSpecialTypeIdFromBean_{var.Id}(Zeze.Transaction.Data bean) {{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean)) 
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    var _typeId_ = bean.typeId();");
                sw.WriteLine($"{prefix}    if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.Data.TYPEID;");
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_typeId_ == {real.Value.TypeId}L)");
                    sw.WriteLine($"{prefix}        return {real.Key}L; // {real.Value.FullName}");
                }
                sw.WriteLine($"{prefix}    throw new RuntimeException(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}\");");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(bean);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Data createBeanFromSpecialTypeId_{var.Id}(long typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (typeId == {real.Key}L)");
                    sw.WriteLine($"{prefix}        return new {real.Value.FullName}.Data();");
                }
                sw.WriteLine($"{prefix}    return null;");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace("::", ".")}(typeId);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void WriteDefine(StreamWriter sw)
        {
            sw.WriteLine("    public static final long TYPEID = " + bean.TypeId + "L;");
            sw.WriteLine();
            // declare enums
            foreach (Enum e in bean.Enums)
            {
                sw.WriteLine($"    public static final {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine();
            }

            // declare variables
            bean.Variables.Sort((a, b) => a.Id - b.Id);
            bool addBlankLine = false;
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                string final = vt is TypeDynamic ? "final " : "";

                sw.WriteLine($"    private {final}{TypeName.GetName(vt)} {v.NamePrivate};{v.Comment}");

                addBlankLine = false;
                if (vt is TypeDynamic dy0)
                    GenDynamicSpecialMethod(sw, "    ", v, dy0, false);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethod(sw, "    ", v, dy1, true);
                else if (vt is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethod(sw, "    ", v, dy2, true);
                else
                    addBlankLine = true;
            }
            if (addBlankLine)
                sw.WriteLine();

            Property.Make(bean, sw, "    ");
            Construct.Make(bean, sw, "    ");
            Assign.Make(bean, sw, "    ");
            // Copy
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + bean.Name + ".Data copy() {");
            sw.WriteLine("        var copy = new " + bean.Name + ".Data();");
            sw.WriteLine("        copy.assign(this);");
            sw.WriteLine("        return copy;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public static void swap({bean.Name}.Data a, {bean.Name}.Data b) {{");
            sw.WriteLine($"        var save = a.copy();");
            sw.WriteLine("        a.assign(b);");
            sw.WriteLine("        b.assign(save);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public long typeId() {");
            sw.WriteLine("        return TYPEID;");
            sw.WriteLine("    }");
            sw.WriteLine();
            java.Tostring.Make(bean, sw, "    ");
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            if (bean.Equalable)
            {
                sw.WriteLine();
                java.Equal.Make(bean, sw, "    ", true);
                java.HashCode.Make(bean, sw, "    ");
            }
            //NegativeCheck.Make(bean, sw, "    ");
        }
    }
}
