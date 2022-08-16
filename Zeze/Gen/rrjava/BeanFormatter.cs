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

            sw.WriteLine("// auto-generated rocks @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine();
            // sw.WriteLine($"public final class {bean.Name} extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly {{");
            sw.WriteLine("@SuppressWarnings({\"UnusedAssignment\", \"RedundantIfStatement\", \"SwitchStatementWithTooFewBranches\", \"RedundantSuppression\"})");
            sw.WriteLine($"public final class {bean.Name} extends Zeze.Raft.RocksRaft.Bean {{");
            WriteDefine(sw);
            sw.WriteLine("}");
        }

        public void WriteDefine(StreamWriter sw)
        {
            // declare enums
            foreach (Enum e in bean.Enums)
            {
                sw.WriteLine("    public static final int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine();
            }

            // declare variables
            bean.Variables.Sort((a, b) => a.Id - b.Id);
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
            sw.WriteLine();

            Property.Make(bean, sw, "    ");
            Construct.Make(bean, sw, "    ");
            Assign.Make(bean, sw, "    ");

            // Copy
            sw.WriteLine("    public " + bean.Name + " CopyIfManaged() {");
            sw.WriteLine("        return isManaged() ? Copy() : this;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public " + bean.Name + " Copy() {");
            sw.WriteLine("        var copy = new " + bean.Name + "();");
            sw.WriteLine("        copy.Assign(this);");
            sw.WriteLine("        return copy;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public static void Swap({bean.Name} a, {bean.Name} b) {{");
            sw.WriteLine($"        {bean.Name} save = a.Copy();");
            sw.WriteLine("        a.Assign(b);");
            sw.WriteLine("        b.Assign(save);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + bean.Name + " CopyBean() {");
            sw.WriteLine("        return Copy();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public static final long TYPEID = " + bean.TypeId + "L;");
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
