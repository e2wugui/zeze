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
                sw.WriteLine("@SuppressWarnings({\"NullableProblems\", \"RedundantIfStatement\", \"RedundantSuppression\", \"SuspiciousNameCombination\", \"SwitchStatementWithTooFewBranches\", \"UnusedAssignment\"})");
                var final = bean.Extendable ? "" : "final ";
                sw.WriteLine($"public {final}class {bean.Name} extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly {{");
                WriteDefine(sw, project);
                if (Program.isData(bean))
                {
                    sw.WriteLine();
                    new javadata.BeanFormatter(bean).Make(sw, "Data");
                }
                sw.WriteLine("}");
            }
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
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {bean.Name}::getSpecialTypeIdFromBean_{var.Id}, {bean.Name}::createBeanFromSpecialTypeId_{var.Id});");
            else
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static long getSpecialTypeIdFromBean_{var.Id}(Zeze.Transaction.Bean bean) {{");
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
                sw.WriteLine($"{prefix}    throw new UnsupportedOperationException(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}\");");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(bean);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_{var.Id}(long typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (typeId == {real.Key}L)");
                    sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
                }
                sw.WriteLine($"{prefix}    if (typeId == Zeze.Transaction.EmptyBean.TYPEID)");
                sw.WriteLine($"{prefix}        return new Zeze.Transaction.EmptyBean();");
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
                    ? "final " : "";

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
            sw.WriteLine("        var copy = new " + bean.Name + "();");
            sw.WriteLine("        copy.assign(this);");
            sw.WriteLine("        return copy;");
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
            Tostring.Make(bean, sw, "    ", false);
            Encode.Make(bean, sw, "    ", bean.Base == "");
            Decode.Make(bean, sw, "    ", bean.Base == "");
            if (bean.Equalable)
            {
                Equal.Make(bean, sw, "    ", false);
                HashCode.Make(bean, sw, "    ", false);
            }
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
                sw.WriteLine($"{prefix}    var vars = super.variables();");
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
                    sw.WriteLine($"{prefix}    vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data({v.Id}, \"{v.Name}\", \"{type}\", \"{key}\", \"{value}\"));");
                }
                sw.WriteLine($"{prefix}    return vars;");
                sw.WriteLine($"{prefix}}}");
            }
        }
    }
}
