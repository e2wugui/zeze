using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Net.Mime;
using System.Text;

namespace Zeze.Gen.cs
{
    public class BeanFormatter
    {
        Types.Bean bean;

        public BeanFormatter(Types.Bean bean)
        {
            this.bean = bean;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("using Zeze.Serialize;");
            sw.WriteLine("using System;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine();
            sw.WriteLine("namespace " + bean.Space.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public interface {bean.Name}ReadOnly");
            sw.WriteLine("    {");
            PropertyReadOnly.Make(bean, sw, "        ");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public sealed class {bean.Name} : Zeze.Transaction.Bean, {bean.Name}ReadOnly");
            sw.WriteLine("    {");
            WriteDefine(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void WriteDefine(System.IO.StreamWriter sw)
        {
            // declare enums
            foreach (Types.Enum e in bean.Enums)
            {
                sw.WriteLine("        public const int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine();
            }

            // declare variables
            foreach (Types.Variable v in bean.Variables)
            {
                sw.WriteLine("        private " + TypeName.GetName(v.VariableType) + " " + v.NamePrivate + ";" + v.Comment);
                if (v.VariableType is Types.TypeMap pmap)
                {
                    var key = TypeName.GetName(pmap.KeyType);
                    var value = pmap.ValueType.IsNormalBean
                        ? TypeName.GetName(pmap.ValueType) + "ReadOnly"
                        : TypeName.GetName(pmap.ValueType);
                    var readonlyTypeName = $"Zeze.Transaction.Collections.PMapReadOnly<{key},{value},{TypeName.GetName(pmap.ValueType)}>";
                    sw.WriteLine($"        private {readonlyTypeName} {v.NamePrivate}ReadOnly;");
                }
            }
            sw.WriteLine();

            Property.Make(bean, sw, "        ");
            sw.WriteLine();
            Construct.Make(bean, sw, "        ");
            Assign.Make(bean, sw, "        ");
            // Copy
            sw.WriteLine("        public " + bean.Name + " CopyIfManaged()");
            sw.WriteLine("        {");
            sw.WriteLine("            return IsManaged ? Copy() : this;");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public " + bean.Name + " Copy()");
            sw.WriteLine("        {");
            sw.WriteLine("            var copy = new " + bean.Name + "();");
            sw.WriteLine("            copy.Assign(this);");
            sw.WriteLine("            return copy;");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine($"        public static void Swap({bean.Name} a, {bean.Name} b)");
            sw.WriteLine("        {");
            sw.WriteLine($"            {bean.Name} save = a.Copy();");
            sw.WriteLine("            a.Assign(b);");
            sw.WriteLine("            b.Assign(save);");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public override Zeze.Transaction.Bean CopyBean()");
            sw.WriteLine("        {");
            sw.WriteLine("            return Copy();");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public const long TYPEID = " + bean.TypeId + ";");
            sw.WriteLine("        public override long TypeId => TYPEID;");
            sw.WriteLine();
            Log.Make(bean, sw, "        ");
            Tostring.Make(bean, sw, "        ");
            Encode.Make(bean, sw, "        ");
            Decode.Make(bean, sw, "        ");
            InitChildrenTableKey.Make(bean, sw, "        ");
            NegativeCheck.Make(bean, sw, "        ");
        }
    }
}
