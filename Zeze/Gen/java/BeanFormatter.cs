using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Net.Mime;
using System.Text;

namespace Zeze.Gen.java
{
    public class BeanFormatter
    {
        Types.Bean bean;

        public BeanFormatter(Types.Bean bean)
        {
            this.bean = bean;
        }

        public void MakeReadOnly(string baseDir)
        {
            using System.IO.StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + "ReadOnly.java");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine("");
            sw.WriteLine("import Zeze.Serialize.*;");
            sw.WriteLine("");
            sw.WriteLine($"public interface {bean.Name}ReadOnly {{");
            //PropertyReadOnly.Make(bean, sw, "    "); // java 不支持ReadOnly
            sw.WriteLine("}");
        }
        public void Make(string baseDir)
        {
            MakeReadOnly(baseDir);

            using System.IO.StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine("");
            sw.WriteLine("import Zeze.Serialize.*;");
            sw.WriteLine("");
            sw.WriteLine($"public final class {bean.Name} : Zeze.Transaction.Bean, {bean.Name}ReadOnly {{");
            WriteDefine(sw);
            sw.WriteLine("}");
        }

        public void WriteDefine(System.IO.StreamWriter sw)
        {
            // declare enums
            foreach (Types.Enum e in bean.Enums)
            {
                sw.WriteLine("    public static final int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine("");
            }

            // declare variables
            foreach (Types.Variable v in bean.Variables)
            {
                sw.WriteLine("    private " + TypeName.GetName(v.VariableType) + " " + v.NamePrivate + ";" + v.Comment);
                // ReadOnlyMap
                /*
                if (v.VariableType is Types.TypeMap pmap)
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
            sw.WriteLine("");

            Property.Make(bean, sw, "        ");
            sw.WriteLine();
            Construct.Make(bean, sw, "        ");
            Assign.Make(bean, sw, "        ");
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
            sw.WriteLine("    public final static long TYPEID = " + bean.TypeId + ";");
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
            NegativeCheck.Make(bean, sw, "    ");
        }
    }
}
