using System;
using System.IO;
using Zeze.Gen.Types;
using Enum = Zeze.Gen.Types.Enum;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.java
{
    public class BeanFormatter
    {
        readonly Bean bean;

        public BeanFormatter(Bean bean)
        {
            this.bean = bean;
        }

        public void MakeRedirectResult(string baseDir, Project project)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine($"public class {bean.Name} extends Zeze.Arch.RedirectResult {{");
            foreach (var v in bean.Variables)
            {
                sw.WriteLine($"    public {TypeName.GetName(v.VariableType)} {v.Name};{v.Comment}");
            }
            sw.WriteLine();
            ConstructRedirectResult.Make(bean, sw, "    ");
            sw.WriteLine("}");
        }

        public void MakeReadOnly(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + "ReadOnly.java");
            if (sw == null)
                return;

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

        public void Make(string baseDir, Project project)
        {
            if (bean.RedirectResult)
            {
                MakeRedirectResult(baseDir, project);
                return;
            }

            if (!Program.isOnlyData(bean))
                MakeReadOnly(baseDir);

            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Serialize.IByteBuffer;");
            sw.WriteLine();
            if (Program.isOnlyData(bean))
                new javadata.BeanFormatter(bean).Make(sw, bean.Name);
            else
            {
                if (bean.Comment.Length > 0)
                    sw.WriteLine(bean.Comment);
                var extraSuppress1 = bean.GenEquals ? "" : "\"EqualsAndHashcode\", ";
                var extraSuppress2 = bean.Interface == "" ? "" : ", \"override\"";
                sw.WriteLine($"@SuppressWarnings({{{extraSuppress1}\"NullableProblems\", \"RedundantIfStatement\", \"RedundantSuppression\", \"SuspiciousNameCombination\", \"SwitchStatementWithTooFewBranches\", \"UnusedAssignment\"{extraSuppress2}}})");
                var final = bean.Extendable ? "" : "final ";
                var extraInterface = bean.Interface == "" ? "" : ", " + bean.Interface;
                sw.WriteLine($"public {final}class {bean.Name} extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly{extraInterface} {{");
                WriteDefine(sw, project);
                if (Program.isData(bean))
                {
                    sw.WriteLine();
                    new javadata.BeanFormatter(bean).Make(sw, "Data");
                }
                sw.WriteLine("}");
            }
        }

        string GetAndCreateDynamicBean(string beanName, int varId, TypeDynamic type)
        {
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                return $"{beanName}::getSpecialTypeIdFromBean_{varId}, " +
                       $"{beanName}::createBeanFromSpecialTypeId_{varId}";
            }
            return $"{type.DynamicParams.GetSpecialTypeIdFromBean}, " +
                   $"{type.DynamicParams.CreateBeanFromSpecialTypeId}";
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}public static final long DynamicTypeId_{var.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                }
            }
            else
            {
                var vt = var.VariableType;
                if (vt is TypeCollection)
                {
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta1<Zeze.Transaction.DynamicBean> meta1{var.NamePrivate}");
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta1.createDynamicListMeta({GetAndCreateDynamicBean(bean.Name, var.Id, type)});");
                }
                else if (vt is TypeMap map)
                {
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta2<{BoxingName.GetBoxingName(map.KeyType)}, Zeze.Transaction.DynamicBean> meta2{var.NamePrivate}");
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta2.createDynamicMapMeta({BoxingName.GetBoxingName(map.KeyType)}.class, {GetAndCreateDynamicBean(bean.Name, var.Id, type)});");
                }
            }
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.DynamicBean newDynamicBean_{var.NameUpper1}() {{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {bean.Name}::getSpecialTypeIdFromBean_{var.Id}, {bean.Name}::createBeanFromSpecialTypeId_{var.Id});");
            else
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static long getSpecialTypeIdFromBean_{var.Id}(Zeze.Transaction.Bean _b_) {{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean))
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    var _t_ = _b_.typeId();");
                sw.WriteLine($"{prefix}    if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.TYPEID;");
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_t_ == {real.Value.TypeId}L)");
                    sw.WriteLine($"{prefix}        return {real.Key}L; // {real.Value.FullName}");
                }
                sw.WriteLine($"{prefix}    throw new UnsupportedOperationException(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}\");");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(_b_);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_{var.Id}(long _t_) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_t_ == {real.Key}L)");
                    sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
                }
                sw.WriteLine($"{prefix}    if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)");
                sw.WriteLine($"{prefix}        return new Zeze.Transaction.EmptyBean();");
                sw.WriteLine($"{prefix}    return null;");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace("::", ".")}(_t_);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void WriteDefine(StreamWriter sw, Project project)
        {
            if (bean.CustomTypeId)
                throw new Exception("custom TypeId is NOT allowed for java: " + bean.Name);
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
                    ? "final "
                    : "";

                if (vt is Bean)
                    sw.WriteLine($"    private {final}Zeze.Transaction.Collections.CollOne<{TypeName.GetName(vt)}> {v.NamePrivate};{v.Comment}");
                else
                    sw.WriteLine($"    private {final}{TypeName.GetName(vt)} {v.NamePrivate};{v.Comment}");

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
            Reset.Make(bean, sw, "    ", false, bean.Base == "");
            Assign.Make(bean, sw, "    ", project, bean.Base == "");
            // Copy
            sw.WriteLine("    public " + bean.Name + " copyIfManaged() {");
            sw.WriteLine("        return isManaged() ? copy() : this;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + bean.Name + " copy() {");
            sw.WriteLine("        var _c_ = new " + bean.Name + "();");
            sw.WriteLine("        _c_.assign(this);");
            sw.WriteLine("        return _c_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public static void swap({bean.Name} _a_, {bean.Name} _b_) {{");
            sw.WriteLine($"        var _s_ = _a_.copy();");
            sw.WriteLine("        _a_.assign(_b_);");
            sw.WriteLine("        _b_.assign(_s_);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public long typeId() {");
            sw.WriteLine("        return TYPEID;");
            sw.WriteLine("    }");
            sw.WriteLine();
            // Log.Make(bean, sw, "    ");
            Tostring.Make(bean, sw, "    ", false);
            Encode.Make(bean, sw, "    ", bean.Base == "");
            Decode.Make(bean, sw, "    ", bean.Base == "");
            Equal.Make(bean, sw, "    ", false); // 对Java项目来说因Zeze.History需要,所以必须生成
            if (bean.GenEquals)
                HashCode.Make(bean, sw, "    ", false);
            sw.WriteLine();
            InitChildrenTableKey.Make(bean, sw, "    ");
            // InitChildrenTableKey.MakeReset(bean, sw, "    ");
            NegativeCheck.Make(bean, sw, "    ");
            FollowerApply.Make(bean, sw, "    ");
            DecodeResultSet.Make(bean, sw, "    ");
            EncodeSQLStatement.Make(bean, sw, "    ");
            GenVariables(bean, sw, "    ");
        }

        public void GenVariables(Bean bean, StreamWriter sw, string prefix)
        {
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine();
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {{");
                sw.WriteLine($"{prefix}    var _v_ = super.variables();");
                foreach (var v in bean.VariablesIdOrder)
                {
                    string type = v.Type;
                    string key = v.Key;
                    string value = v.Value;

                    var vType = v.VariableType;
                    if (vType.IsBean)
                    {
                        type = Variable.GetBeanFullName(vType);
                    }
                    else if (vType.IsCollection)
                    {
                        if (vType is TypeMap map)
                        {
                            if (map.KeyType.IsBean)
                                key = Variable.GetBeanFullName(map.KeyType);
                            if (map.ValueType.IsBean)
                                value = Variable.GetBeanFullName(map.ValueType);
                        }
                        else if (vType is TypeList list)
                        {
                            if (list.ValueType.IsBean)
                                value = Variable.GetBeanFullName(list.ValueType);
                        }
                        else if (vType is TypeSet set)
                        {
                            if (set.ValueType.IsBean)
                                value = Variable.GetBeanFullName(set.ValueType);
                        }
                    }
                    sw.WriteLine($"{prefix}    _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data({v.Id}, \"{v.Name}\", \"{type}\", \"{key}\", \"{value}\"));");
                }
                sw.WriteLine($"{prefix}    return _v_;");
                sw.WriteLine($"{prefix}}}");
            }
        }
    }
}
