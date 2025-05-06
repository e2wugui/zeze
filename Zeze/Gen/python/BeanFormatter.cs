using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Gen.Types;
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

        public void Make(string baseDir, Project project)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".py");
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("from zeze.bean import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine("from zeze.buffer import ByteBuffer");
            sw.WriteLine("from zeze.util import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine("from zeze.vector import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import gen.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine("# noinspection GrazieInspection,PyPep8Naming,PyShadowingBuiltins");
            sw.WriteLine($"class {bean.Name}(Bean):");
            if (bean.Comment.Length > 0)
                sw.WriteLine($"{Maker.toPythonComment(bean.Comment, "    ")}");
            WriteDefine(sw, project);
        }

        public static SortedSet<string> CollectImports(List<Variable> vars)
        {
            var r = new SortedSet<string>();
            foreach (var var in vars)
            {
                switch (var.VariableType)
                {
                    case Bean b:
                        r.Add(b.FullName);
                        break;
                    case TypeDynamic dyn:
                    {
                        foreach (var b1 in dyn.RealBeans.Values)
                            r.Add(b1.FullName);
                        break;
                    }
                    case TypeList list:
                    {
                        switch (list.ValueType)
                        {
                            case Bean b1:
                                r.Add(b1.FullName);
                                break;
                            case TypeDynamic dyn1:
                                foreach (var b2 in dyn1.RealBeans.Values)
                                    r.Add(b2.FullName);
                                break;
                        }
                        break;
                    }
                    case TypeMap map:
                    {
                        switch (map.KeyType)
                        {
                            case Bean b1:
                                r.Add(b1.FullName);
                                break;
                            case TypeDynamic dyn1:
                                foreach (var b2 in dyn1.RealBeans.Values)
                                    r.Add(b2.FullName);
                                break;
                        }
                        switch (map.ValueType)
                        {
                            case Bean b1:
                                r.Add(b1.FullName);
                                break;
                            case TypeDynamic dyn1:
                                foreach (var b2 in dyn1.RealBeans.Values)
                                    r.Add(b2.FullName);
                                break;
                        }
                        break;
                    }
                }
            }
            return r;
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type, Project project)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}@staticmethod");
            sw.WriteLine($"{prefix}def dynamic_bean2id_{var.Name}(_b_):");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean))
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    _t_ = _b_.type_id()");
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
                sw.WriteLine($"{prefix}    # noinspection PyShadowingNames");
                sw.WriteLine($"{prefix}    import {project.Solution.Name} as {project.Solution.Name}");
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
                sw.WriteLine($"{prefix}    if _i_ == EmptyBean.TYPEID:");
                sw.WriteLine($"{prefix}        return EmptyBean()");
                sw.WriteLine($"{prefix}    return None");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    # noinspection PyShadowingNames");
                sw.WriteLine($"{prefix}    import {project.Solution.Name} as {project.Solution.Name}");
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace("::", ".")}(_i_)");
            }
        }

        public void WriteDefine(StreamWriter sw, Project project)
        {
            if (bean.CustomTypeId)
                throw new Exception("custom TypeId is NOT allowed for python: " + bean.Name);
            sw.WriteLine("    TYPEID = " + bean.TypeId);
            // declare enums
            if (bean.Enums.Count > 0)
                sw.WriteLine();
            foreach (var e in bean.Enums)
            {
                sw.WriteLine(string.IsNullOrEmpty(e.Comment)
                    ? $"    {e.Name} = {e.Value}  {Maker.toPythonComment(e.Comment)}"
                    : $"    {e.Name} = {e.Value}");
            }

            // declare variables
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                if (vt is TypeDynamic dy0)
                    GenDynamicSpecialMethod(sw, "    ", v, dy0, project);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethod(sw, "    ", v, dy1, project);
                else if (vt is TypeCollection col && col.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethod(sw, "    ", v, dy2, project);
            }

            Construct.Make(bean, sw, "    ");
            sw.WriteLine();
            sw.WriteLine("    def type_id(self):");
            sw.WriteLine($"        return {bean.Name}.TYPEID");
            sw.WriteLine();
            sw.WriteLine("    def type_name(self):");
            sw.WriteLine($"        return \"{bean.FullName}\"");
            Reset.Make(bean, sw, "    ");
            Assign.Make(bean, sw, "    ");
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            if (bean.GenEquals)
            {
                HashCode.Make(bean, sw, "    ");
                Equal.Make(bean, sw, "    ");
            }
            Tostring.Make(bean, sw, "    ");
        }
    }
}
