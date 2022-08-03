using System.IO;
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
            sw.WriteLine($"public interface {bean.Name}ReadOnly {{");
            //PropertyReadOnly.Make(bean, sw, "    "); // java 不支持ReadOnly
            sw.WriteLine("}");
        }
        public void Make(string baseDir)
        {
            //MakeReadOnly(baseDir);

            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine();
            // sw.WriteLine($"public final class {bean.Name} extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly {{");
            sw.WriteLine("@SuppressWarnings({\"UnusedAssignment\", \"RedundantIfStatement\", \"SwitchStatementWithTooFewBranches\", \"RedundantSuppression\"})");
            sw.WriteLine($"public final class {bean.Name} extends Zeze.Transaction.Bean {{");
            WriteDefine(sw);
            sw.WriteLine("}");
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Types.Variable var, TypeDynamic type, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}public static final long DynamicTypeId{var.NameUpper1}{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                }
                if (type.RealBeans.Count > 0)
                    sw.WriteLine();
            }

            sw.WriteLine($"{prefix}public static long GetSpecialTypeIdFromBean_{var.NameUpper1}(Zeze.Transaction.Bean bean) {{");
            sw.WriteLine($"{prefix}    var _typeId_ = bean.getTypeId();");
            sw.WriteLine($"{prefix}    if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)");
            sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.TYPEID;");
            foreach (var real in type.RealBeans)
            {
                sw.WriteLine($"{prefix}    if (_typeId_ == {real.Value.TypeId}L)");
                sw.WriteLine($"{prefix}        return {real.Key}L; // {real.Value.FullName}");
            }
            sw.WriteLine($"{prefix}    throw new RuntimeException(\"Unknown Bean! dynamic@{(var.Bean as Bean).FullName}:{var.Name}\");");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_{var.NameUpper1}(long typeId) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            foreach (var real in type.RealBeans)
            {
                sw.WriteLine($"{prefix}    if (typeId == {real.Key}L)");
                sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
            }
            sw.WriteLine($"{prefix}    return null;");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
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
                    // || vt is TypeVector2
                    // || vt is TypeVector2Int
                    // || vt is TypeVector3
                    // || vt is TypeVector3Int
                    // || vt is TypeVector4
                    // || vt is TypeQuaternion
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
                    var readonlyTypeName = $"Zeze.Transaction.Collections.CollMapReadOnly<{key},{value},{TypeName.GetName(pmap.ValueType)}>";
                    sw.WriteLine($"        private {readonlyTypeName} {v.NamePrivate}ReadOnly;");
                }
                */
                if (vt is TypeDynamic dy0)
                    GenDynamicSpecialMethod(sw, "        ", v, dy0, false);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethod(sw, "        ", v, dy1, true);
                else if (vt is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethod(sw, "        ", v, dy2, true);
            }
            if (bean.Variables.Count > 0)
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
            sw.WriteLine("    public Zeze.Transaction.Bean CopyBean() {");
            sw.WriteLine("        return Copy();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public static final long TYPEID = " + bean.TypeId + "L;");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public long getTypeId() {");
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
