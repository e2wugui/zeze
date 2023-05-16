using Org.BouncyCastle.Utilities.Collections;
using System.Collections.Generic;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
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
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".hpp");
            sw.WriteLine("#pragma once");
            sw.WriteLine();
            sw.WriteLine("#include \"zeze/cxx/Bean.h\"");
            var includes = new HashSet<Type>();
            bean.DependsVariables(includes);
            foreach (var inc in includes)
            {
                if (inc is Bean b)
                {
                    sw.WriteLine($"#include \"Gen/{b.Space.Path("/", b.Name + ".hpp")}\"");
                }
                else if (inc is BeanKey k)
                {
                    sw.WriteLine($"#include \"Gen/{k.Space.Path("/", k.Name + ".hpp")}\"");
                }
            }
            sw.WriteLine();
            var paths = bean.Space.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine($"class {bean.Name} : public Zeze::Bean {{");
            sw.WriteLine($"public:");
            WriteDefine(sw);
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}static const int64_t DynamicTypeId_{var.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                }
            }
            sw.WriteLine();
            sw.WriteLine($"{prefix}static Zeze::DynamicBean constructDynamicBean_{var.NameUpper1}() {{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
                sw.WriteLine($"{prefix}    return Zeze::DynamicBean({bean.Name}::GetSpecialTypeIdFromBean_{var.Id}, {bean.Name}::CreateBeanFromSpecialTypeId_{var.Id});");
            else
                sw.WriteLine($"{prefix}    return Zeze::DynamicBean({type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}static int64_t GetSpecialTypeIdFromBean_{var.Id}(const Zeze::Bean* bean) {{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean)) 
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    auto _typeId_ = bean->TypeId();");
                sw.WriteLine($"{prefix}    if (_typeId_ == Zeze::EmptyBean::TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze::EmptyBean::TYPEID;");
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_typeId_ == {real.Value.TypeId}L)");
                    sw.WriteLine($"{prefix}        return {real.Key}LL; // {real.Value.FullCxxName}");
                }
                sw.WriteLine($"{prefix}    throw std::exception(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullCxxName}:{var.Name}\");");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean}(bean);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}static Zeze::Bean* CreateBeanFromSpecialTypeId_{var.Id}(int64_t typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (typeId == {real.Key}LL)");
                    sw.WriteLine($"{prefix}        return new {real.Value.FullCxxName}();");
                }
                sw.WriteLine($"{prefix}    return nullptr;");
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
            sw.WriteLine("    static const int64_t TYPEID = " + bean.TypeId + "LL;");
            sw.WriteLine();
            // declare enums
            foreach (Enum e in bean.Enums)
            {
                sw.WriteLine($"    static const {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
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
                
                sw.WriteLine($"    {TypeName.GetName(vt)} {v.NameUpper1};{v.Comment}");

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
            sw.WriteLine("    virtual int64_t TypeId() const override {");
            sw.WriteLine("        return TYPEID;");
            sw.WriteLine("    }");
            sw.WriteLine();
            //java.Tostring.Make(bean, sw, "    ");
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            /*
            if (bean.Equalable)
            {
                sw.WriteLine();
                java.Equal.Make(bean, sw, "    ", true);
                java.HashCode.Make(bean, sw, "    ");
            }
            */
            //NegativeCheck.Make(bean, sw, "    ");
        }
    }
}
