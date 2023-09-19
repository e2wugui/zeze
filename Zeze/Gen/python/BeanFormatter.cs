using System;
using System.IO;
using Zeze.Gen.Types;
using Enum = Zeze.Gen.Types.Enum;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.python
{
    public class BeanFormatter
    {
        readonly Bean bean;

        public BeanFormatter(Bean bean)
        {
            this.bean = bean;
        }

        public void Make(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".py");
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("from zeze.buffer import ByteBuffer\n");
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(Maker.toPythonComment(bean.Comment));
            sw.WriteLine($"class {bean.Name}(Bean):");
            WriteDefine(sw);
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}@staticmethod");
            sw.WriteLine($"{prefix}def dynamic_bean2id_{var.Name}(_b_):");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean))
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    var _t_ = _b_.type_id();");
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if _t_ == {real.Value.TypeId}:");
                    sw.WriteLine($"{prefix}        return {real.Key}  # {real.Value.FullName}");
                }
                sw.WriteLine($"{prefix}    if _t_ == EmptyBean.TYPEID:");
                sw.WriteLine($"{prefix}        return EmptyBean.TYPEID");
                sw.WriteLine($"{prefix}    raise Exception(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}\")");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(_b_)");
            }
            sw.WriteLine();
            sw.WriteLine($"{prefix}@staticmethod");
            sw.WriteLine($"{prefix}def dynamic_id2bean_{var.Name}(_i_):");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if _i_ == {real.Key}:");
                    sw.WriteLine($"{prefix}        return {real.Value.FullName}()");
                }
                sw.WriteLine($"{prefix}    if _i_ == EmptyBean.TYPEID");
                sw.WriteLine($"{prefix}        return EmptyBean()");
                sw.WriteLine($"{prefix}    return None");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace("::", ".")}(_i_)");
            }
            sw.WriteLine();
        }

        public void WriteDefine(StreamWriter sw)
        {
            if (bean.CustomTypeId)
                throw new Exception("custom TypeId is NOT allowed for python: " + bean.Name);
            sw.WriteLine("    TYPEID = " + bean.TypeId);
            sw.WriteLine();
            // declare enums
            foreach (Enum e in bean.Enums)
                sw.WriteLine($"    {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + Maker.toPythonComment(e.Comment, true));
            if (bean.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            bool addBlankLine = false;
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                addBlankLine = false;
                if (vt is TypeDynamic dy0)
                    GenDynamicSpecialMethod(sw, "    ", v, dy0);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethod(sw, "    ", v, dy1);
                else if (vt is TypeCollection col && col.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethod(sw, "    ", v, dy2);
                else
                    addBlankLine = true;
            }
            if (addBlankLine)
                sw.WriteLine();

            Construct.Make(bean, sw, "    ");
            sw.WriteLine("    def type_id(self):");
            sw.WriteLine($"        return {bean.Name}.TYPEID");
            sw.WriteLine();
            sw.WriteLine("    def type_name(self):");
            sw.WriteLine($"        return \"{bean.FullName}\"");
            sw.WriteLine();
            Reset.Make(bean, sw, "    ");
            Assign.Make(bean, sw, "    ");
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            if (bean.Equalable)
            {
                HashCode.Make(bean, sw, "    ");
                Equal.Make(bean, sw, "    ");
            }
            Tostring.Make(bean, sw, "    ");
        }
    }
}
