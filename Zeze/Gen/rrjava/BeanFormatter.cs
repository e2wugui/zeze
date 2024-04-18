using System.IO;
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
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated rocks @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Serialize.IByteBuffer;");
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine("@SuppressWarnings({\"UnusedAssignment\", \"RedundantIfStatement\", \"SwitchStatementWithTooFewBranches\", \"RedundantSuppression\", \"NullableProblems\", \"SuspiciousNameCombination\"})");
            sw.WriteLine($"public final class {bean.Name} extends Zeze.Raft.RocksRaft.Bean {{"); // extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly
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

            // declare variables
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                string final = vt is TypeCollection
                               || vt is TypeMap
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
    }
}
