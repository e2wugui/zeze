using Org.BouncyCastle.Utilities.Collections;
using System.Collections.Generic;
using System.IO;
using System.Text;
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
            if (sw == null)
                return;

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
            MakeCpp(baseDir);
        }

        private void GetInclude(HashSet<string> result, string staticMethod)
        {
            // demo.Module1.ModuleModule1.staticMethod
            if (string.IsNullOrEmpty(staticMethod))
                return;

            staticMethod = staticMethod.Replace("::", ".");
            var paths = staticMethod.Split(".");
            var sb = new StringBuilder();
            for (var i = 0; i < paths.Length - 1; ++i)
            {
                if (i > 0)
                    sb.Append("/");
                sb.Append(paths[i]);
            }
            sb.Append(".h");
            result.Add(sb.ToString());
        }

        private void AddIf(HashSet<string> result, TypeDynamic d)
        {
            GetInclude(result, d.DynamicParams.CreateBeanFromSpecialTypeId);
            GetInclude(result, d.DynamicParams.GetSpecialTypeIdFromBean);
        }

        public void MakeCpp(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".cpp");
            if (sw == null)
                return;

            sw.WriteLine();
            sw.WriteLine($"#include \"Gen/{bean.Space.Path("/", bean.Name + ".hpp")}\"");
            var dependIncludes = new HashSet<string>();
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                if (vt is TypeDynamic dy0)
                    AddIf(dependIncludes, dy0);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    AddIf(dependIncludes, dy1);
                else if (vt is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    AddIf(dependIncludes, dy2);
            }
            foreach (var inc in dependIncludes)
            {
                sw.WriteLine($"#include \"{inc}\"");
            }
            sw.WriteLine();
            var paths = bean.Space.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            WriteCpp(sw);
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }

        private void GenDynamicSpecialMethodHpp(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}static const int64_t DynamicTypeId_{var.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                }
                sw.WriteLine();
            }
            sw.WriteLine($"{prefix}static Zeze::DynamicBean constructDynamicBean_{var.NameUpper1}();");
            sw.WriteLine($"{prefix}static int64_t GetSpecialTypeIdFromBean_{var.Id}(const Zeze::Bean* bean);");
            sw.WriteLine($"{prefix}static Zeze::Bean* CreateBeanFromSpecialTypeId_{var.Id}(int64_t typeId);");
            sw.WriteLine();
        }

        private void GenDynamicSpecialMethodCpp(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            sw.WriteLine($"{prefix}Zeze::DynamicBean {bean.Name}::constructDynamicBean_{var.NameUpper1}() {{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
                sw.WriteLine($"{prefix}    return Zeze::DynamicBean({bean.Name}::GetSpecialTypeIdFromBean_{var.Id}, {bean.Name}::CreateBeanFromSpecialTypeId_{var.Id});");
            else
                sw.WriteLine($"{prefix}    return Zeze::DynamicBean({type.DynamicParams.GetSpecialTypeIdFromBean.Replace(".", "::")}, {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace(".", "::")});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}int64_t {bean.Name}::GetSpecialTypeIdFromBean_{var.Id}(const Zeze::Bean* bean) {{");
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
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace(".", "::")}(bean);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}Zeze::Bean* {bean.Name}::CreateBeanFromSpecialTypeId_{var.Id}(int64_t typeId) {{");
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
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace(".", "::")}(typeId);");
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
                    GenDynamicSpecialMethodHpp(sw, "    ", v, dy0, false);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethodHpp(sw, "    ", v, dy1, true);
                else if (vt is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethodHpp(sw, "    ", v, dy2, true);
                else
                    addBlankLine = true;
            }
            if (addBlankLine)
                sw.WriteLine();

            Property.Make(bean, sw, "    ");
            Construct.MakeHpp(bean, sw, "    ");
            Assign.MakeHpp(bean, sw, "    ");
            sw.WriteLine("    virtual int64_t TypeId() const override {");
            sw.WriteLine("        return TYPEID;");
            sw.WriteLine("    }");
            sw.WriteLine();
            //java.Tostring.Make(bean, sw, "    ");
            Encode.MakeHpp(bean, sw, "    ");
            Decode.MakeHpp(bean, sw, "    ");
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

        private void WriteCpp(StreamWriter sw)
        {
            sw.WriteLine();
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                if (vt is TypeDynamic dy0)
                    GenDynamicSpecialMethodCpp(sw, "", v, dy0, false);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethodCpp(sw, "", v, dy1, true);
                else if (vt is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethodCpp(sw, "", v, dy2, true);
            }
            Construct.MakeCpp(bean, sw, "");
            Assign.MakeCpp(bean, sw, "");
            Encode.MakeCpp(bean, sw, "");
            Decode.MakeCpp(bean, sw, "");
        }

    }
}
