using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.confcs
{
    public class BeanFormatter
    {
        readonly Bean bean;
        readonly Project project;

        public BeanFormatter(Project prj, Bean bean)
        {
            this.project = prj;
            this.bean = bean;
        }

        public void Make(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("using ByteBuffer = Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("using Environment = System.Environment;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine();
            sw.WriteLine("namespace " + bean.Space.Path());
            sw.WriteLine("{");
            sw.WriteLine();
            sw.WriteLine($"    public sealed class {bean.Name} : Zeze.Util.ConfBean");
            sw.WriteLine("    {");
            WriteDefine(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void WriteDefine(StreamWriter sw)
        {
            // declare enums
            foreach (Enum e in bean.Enums)
                sw.WriteLine("        public const int " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (bean.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            bean.Variables.Sort((a, b) => a.Id - b.Id);
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                if (vt is TypeDynamic)
                    sw.WriteLine("        " + TypeName.GetName(vt) + " " + v.NamePrivate + ";" + v.Comment);
                else
                    sw.WriteLine("        public " + TypeName.GetName(vt) + " " + v.NameUpper1 + ";" + v.Comment);
            }
            sw.WriteLine();

            Property.Make(bean, sw, "        ");
            Construct.Make(bean, sw, "        ");
            cs.Assign.Make(bean, sw, "        ");
            // Copy
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
            sw.WriteLine("        public override Zeze.Util.ConfBean CopyBean()");
            sw.WriteLine("        {");
            sw.WriteLine("            return Copy();");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public const long TYPEID = " + bean.TypeId + ";");
            sw.WriteLine("        public override long TypeId => TYPEID;");
            sw.WriteLine();
            cs.Decode.Make(bean, sw, "        ");
            var macro = project.MacroEditor;
            if (false == string.IsNullOrEmpty(macro))
                sw.WriteLine($"#if {macro}");
            {
                cs.Tostring.Make(bean, sw, "        ");
                cs.Encode.Make(bean, sw, "        ");
                cs.NegativeCheck.Make(bean, sw, "        ");
            }
            if (false == string.IsNullOrEmpty(macro))
                sw.WriteLine($"#endif // {macro}");
        }
    }
}
