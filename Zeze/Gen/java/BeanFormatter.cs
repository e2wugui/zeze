﻿using System.IO;
using Zeze.Collections;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class BeanFormatter
    {
        readonly Bean bean;

        public BeanFormatter(Bean bean)
        {
            this.bean = bean;
        }

        public void MakeReadOnly(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + "ReadOnly.java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            //sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            //sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine($"public interface {bean.Name}ReadOnly {{");
            PropertyReadOnly.Make(bean, sw, "    ");
            sw.WriteLine("}");
        }

        public void Make(string baseDir)
        {
            MakeReadOnly(baseDir);

            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine("@SuppressWarnings({\"UnusedAssignment\", \"RedundantIfStatement\", \"SwitchStatementWithTooFewBranches\", \"RedundantSuppression\"})");
            sw.WriteLine($"public final class {bean.Name} extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly {{");
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
            sw.WriteLine($"{prefix}public static Zeze.Transaction.DynamicBean newDynamicBean_{var.NameUpper1}() {{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {bean.Name}::getSpecialTypeIdFromBean_{var.NameUpper1}, {bean.Name}::createBeanFromSpecialTypeId_{var.NameUpper1});");
            else
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static long getSpecialTypeIdFromBean_{var.NameUpper1}(Zeze.Transaction.Bean bean) {{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean)) 
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    var _typeId_ = bean.typeId();");
                sw.WriteLine($"{prefix}    if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.TYPEID;");
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
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_{var.NameUpper1}(long typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (typeId == {real.Key}L)");
                    sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
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
                string final = vt is TypeCollection
                    || vt is TypeMap
                    || vt is Bean
                    || vt is TypeDynamic
                    // || vt is TypeVector2
                    // || vt is TypeVector2Int
                    // || vt is TypeVector3
                    // || vt is TypeVector3Int
                    // || vt is TypeVector4
                    // || vt is TypeQuaternion
                    ? "final " : "";

                if (vt is Bean)
                    sw.WriteLine($"        {final}Zeze.Transaction.Collections.CollOne<{TypeName.GetName(vt)}> {v.NamePrivate};{v.Comment}");
                else
                    sw.WriteLine("    private " + final + TypeName.GetName(vt) + " " + v.NamePrivate + ";" + v.Comment);

                // ReadOnlyMap
                /*
                if (vt is TypeMap pmap)
                {
                    var key = TypeName.GetName(pmap.KeyType);
                    var value = pmap.ValueType.IsNormalBean
                        ? TypeName.GetName(pmap.ValueType) + "ReadOnly"
                        : TypeName.GetName(pmap.ValueType);
                    var readonlyTypeName = $"Zeze.Transaction.Collections.CollMapReadOnly<{key},{value},{TypeName.GetName(pmap.ValueType)}>";
                    sw.WriteLine($"        private {readonlyTypeName} {v.NamePrivate}ReadOnly;");
                }
                */
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
            sw.WriteLine("    public " + bean.Name + " copyIfManaged() {");
            sw.WriteLine("        return isManaged() ? copy() : this;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + bean.Name + " copy() {");
            sw.WriteLine("        var copy = new " + bean.Name + "();");
            sw.WriteLine("        copy.assign(this);");
            sw.WriteLine("        return copy;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Deprecated");
            sw.WriteLine("    public " + bean.Name + " Copy() {");
            sw.WriteLine("        return copy();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public static void swap({bean.Name} a, {bean.Name} b) {{");
            sw.WriteLine($"        {bean.Name} save = a.copy();");
            sw.WriteLine("        a.assign(b);");
            sw.WriteLine("        b.assign(save);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public long typeId() {");
            sw.WriteLine("        return TYPEID;");
            sw.WriteLine("    }");
            sw.WriteLine();
            Log.Make(bean, sw, "    ");
            Tostring.Make(bean, sw, "    ");
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            InitChildrenTableKey.Make(bean, sw, "    ");
            InitChildrenTableKey.MakeReset(bean, sw, "    ");
            NegativeCheck.Make(bean, sw, "    ");
            FollowerApply.Make(bean, sw, "    ");
        }
    }
}
