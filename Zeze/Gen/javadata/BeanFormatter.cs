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

        public void Make(StreamWriter sw, string className)
        {
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            var static0 = bean.OnlyData ? "" : "static ";
            var final = bean.Extendable ? "" : "final ";
            if (bean.OnlyData)
                sw.WriteLine("@SuppressWarnings({\"ForLoopReplaceableByForEach\", \"NullableProblems\", \"RedundantIfStatement\", \"RedundantSuppression\", \"UnusedAssignment\"})");
            else
                sw.WriteLine("@SuppressWarnings(\"ForLoopReplaceableByForEach\")");
            sw.WriteLine($"public {static0}{final}class {className} extends Zeze.Transaction.Data {{");
            WriteDefine(sw);
            sw.WriteLine("}");
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static final class DynamicData_{var.Name} extends Zeze.Transaction.DynamicData {{");
            prefix += "    ";
            var hasConst = false;
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}public static final long DynamicTypeId_{var.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                    hasConst = true;
                }
            }
            if (hasConst)
                sw.WriteLine();
            sw.WriteLine($"{prefix}static {{");
            sw.WriteLine($"{prefix}    registerJsonParser(DynamicData_{var.Name}.class);");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public long toTypeId(Zeze.Transaction.Data data) {{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean))
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    var _typeId_ = data.typeId();");
                sw.WriteLine($"{prefix}    if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.Data.TYPEID;");
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_typeId_ == {real.Value.TypeId}L)");
                    sw.WriteLine($"{prefix}        return {real.Key}L; // {real.Value.FullName}");
                }
                sw.WriteLine($"{prefix}    throw new UnsupportedOperationException(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}\");");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(data);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public Zeze.Transaction.Data toData(long typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateDataFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (typeId == {real.Key}L)");
                    if (real.Value.OnlyData)
                        sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
                    else
                        sw.WriteLine($"{prefix}        return new {real.Value.FullName}.Data();");
                }
                sw.WriteLine($"{prefix}    if (typeId == Zeze.Transaction.EmptyBean.Data.TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.Data.instance;");
                sw.WriteLine($"{prefix}    return null;");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateDataFromSpecialTypeId.Replace("::", ".")}(typeId);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}@Override");
            sw.WriteLine($"{prefix}public DynamicData_{var.Name} copy() {{");
            sw.WriteLine($"{prefix}    return (DynamicData_{var.Name})super.copy();");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine($"{prefix[..^4]}}}");
            sw.WriteLine();
        }

        public void WriteDefine(StreamWriter sw)
        {
            var className = bean.OnlyData ? bean.Name : bean.Name + ".Data";
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
            java.Reset.Make(bean, sw, "    ", true, false);
            Assign.Make(bean, sw, "    ");
            // Copy
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + className + " copy() {");
            sw.WriteLine("        var copy = new " + className + "();");
            sw.WriteLine("        copy.assign(this);");
            sw.WriteLine("        return copy;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public static void swap({className} a, {className} b) {{");
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
            sw.WriteLine("    @Override");
            sw.WriteLine($"    public {className} clone() {{");
            sw.WriteLine($"        return ({className})super.clone();");
            sw.WriteLine("    }");
            sw.WriteLine();
            java.Tostring.Make(bean, sw, "    ", true);
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            if (bean.GenEquals)
            {
                sw.WriteLine();
                java.Equal.Make(bean, sw, "    ", !bean.OnlyData);
                java.HashCode.Make(bean, sw, "    ", true);
            }
            //NegativeCheck.Make(bean, sw, "    ");
        }
    }
}
