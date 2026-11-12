using System.IO;
using System.Collections.Generic;
using System.Linq;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
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
            java.JSpecify.GenPackageInfo(bean.Space, baseDir);

            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated rocks @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Serialize.IByteBuffer;");
            sw.WriteLine(java.JSpecify.NullableImport);
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            var extraInterface = bean.Interface == "" ? "" : " implements " + bean.Interface;
            var extraSuppress = bean.Interface == "" ? "" : ", \"override\"";
            sw.WriteLine($"@SuppressWarnings({{\"ConstantConditions\", \"ConstantValue\", \"UnusedAssignment\", \"RedundantIfStatement\", \"SwitchStatementWithTooFewBranches\", \"RedundantSuppression\", \"SuspiciousNameCombination\"{extraSuppress}}})");
            sw.WriteLine($"public final class {bean.Name} extends Zeze.Raft.RocksRaft.Bean{extraInterface} {{"); // extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly
            WriteDefine(sw);
            sw.WriteLine("}");
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

            WriteDynamicFields(sw);
            // declare variables
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                string final = vt is TypeCollection
                               || vt is TypeMap
                               || vt is TypeSortedMap
                               || vt is Bean
                               || vt is TypeDynamic
                    ? "final " : "";
                sw.WriteLine("    private " + final + TypeName.GetName(vt) + " " + v.NamePrivate + ";" + v.Comment);
                // ReadOnlyMap
                /*
                if (vt is TypeMap pmap)
                {
                    var key = TypeName.GetName(pmap.KeyType);
                    var value = pmap.ValueType.IsNormalBean
                        ? TypeName.GetName(pmap.ValueType) + "ReadOnly"
                        : TypeName.GetName(pmap.ValueType);
                    var readonlyTypeName = $"Zeze.Transaction.Collections.PMapReadOnly<{key},{value},{TypeName.GetName(pmap.ValueType)}>";
                    sw.WriteLine($"        private {readonlyTypeName} {v.NamePrivate}ReadOnly;");
                }
                */
            }
            if (bean.Variables.Count > 0)
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

            Tostring.Make(bean, sw, "    ");
            Encode.Make(bean, sw, "    ");
            Decode.Make(bean, sw, "    ");
            InitChildrenTableKey.Make(bean, sw, "    ");

            LeaderApplyNoRecursive.Make(bean, sw, "    ");
            FollowerApply.Make(bean, sw, "    ");
        }

        internal static TypeDynamic GetDynamicType(Type type)
        {
            return type switch
            {
                TypeDynamic dynamic => dynamic,
                TypeList list => list.ValueType as TypeDynamic,
                TypeMap map => map.ValueType as TypeDynamic,
                TypeSortedMap map => map.ValueType as TypeDynamic,
                _ => null,
            };
        }

        internal static string DynamicFieldName(Variable variable) => $"dynamicField_{variable.Id}";

        private void WriteDynamicFields(StreamWriter sw)
        {
            var registrations = new List<string>();
            foreach (var variable in bean.Variables)
            {
                var dynamic = GetDynamicType(variable.VariableType);
                if (dynamic == null)
                    continue;
                var field = DynamicFieldName(variable);
                sw.WriteLine($"    private static final Zeze.Raft.RocksRaft.DynamicField {field} =");
                sw.WriteLine($"            new Zeze.Raft.RocksRaft.DynamicField(TYPEID, {variable.Id},");
                sw.WriteLine($"                    {bean.Name}::getSpecialTypeId_{variable.Id}, {bean.Name}::createDynamicValue_{variable.Id});");
                sw.WriteLine();
                if (variable.VariableType is not TypeDynamic)
                {
                    sw.WriteLine($"    public static Zeze.Raft.RocksRaft.DynamicBean newDynamicBean_{variable.NameUpper1}() {{");
                    sw.WriteLine($"        return {field}.newValue();");
                    sw.WriteLine("    }");
                    sw.WriteLine();
                }

                sw.WriteLine($"    private static long getSpecialTypeId_{variable.Id}(Zeze.Raft.RocksRaft.Bean value) {{");
                if (string.IsNullOrEmpty(dynamic.DynamicParams.GetSpecialTypeIdFromBean))
                {
                    sw.WriteLine("        var typeId = value.typeId();");
                    foreach (var real in dynamic.RealBeans)
                        sw.WriteLine($"        if (typeId == {real.Value.TypeId}L) return {real.Key}L;");
                    sw.WriteLine($"        throw new IllegalArgumentException(\"unknown type for {bean.FullName}.{variable.Name}: \" + typeId);");
                }
                else
                    sw.WriteLine($"        return {dynamic.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(value);");
                sw.WriteLine("    }");
                sw.WriteLine();

                sw.WriteLine($"    private static Zeze.Raft.RocksRaft.@Nullable Bean createDynamicValue_{variable.Id}(long typeId) {{");
                if (string.IsNullOrEmpty(dynamic.DynamicParams.CreateBeanFromSpecialTypeId))
                {
                    foreach (var real in dynamic.RealBeans)
                        sw.WriteLine($"        if (typeId == {real.Key}L) return new {real.Value.FullName}();");
                    sw.WriteLine("        return null;");
                }
                else
                {
                    var factory = dynamic.DynamicParams.CreateBeanFromSpecialTypeId.Replace("::", ".");
                    sw.WriteLine($"        Zeze.Raft.RocksRaft.@Nullable Bean value = {factory}(typeId);");
                    sw.WriteLine("        if (value == null) return null;");
                    foreach (var real in dynamic.RealBeans.Values.Distinct())
                        sw.WriteLine($"        if (value.getClass() == {real.FullName}.class) return value;");
                    sw.WriteLine($"        throw new IllegalArgumentException(\"undeclared RocksRaft dynamic payload for {bean.FullName}.{variable.Name}: \" + value.getClass().getName());");
                }
                sw.WriteLine("    }");
                sw.WriteLine();

                if (!variable.Transient)
                {
                    registrations.Add(variable.VariableType switch
                    {
                        TypeList => $"{field}.registerListLog();",
                        TypeMap map => $"{field}.registerMapLog({BoxingName.GetBoxingName(map.KeyType)}.class);",
                        TypeSortedMap map => $"{field}.registerSortedMapLog({BoxingName.GetBoxingName(map.KeyType)}.class);",
                        _ => $"{field}.registerValueLog();",
                    });
                }
            }
            if (registrations.Count == 0)
                return;
            sw.WriteLine("    public static void registerDynamicFields() {");
            foreach (var registration in registrations)
                sw.WriteLine($"        {registration}");
            sw.WriteLine("    }");
            sw.WriteLine();
        }
    }
}
