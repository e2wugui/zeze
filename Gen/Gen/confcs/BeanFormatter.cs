using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.confcs
{
    public class BeanFormatter
    {
        readonly Bean bean;
        readonly Project project;
        readonly bool followerApply;

        public BeanFormatter(Project prj, Bean bean, bool followerApply = false)
        {
            this.project = prj;
            this.bean = bean;
            this.followerApply = followerApply;
        }

        public void Make(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".cs");
            if (sw == null)
                return;

            var preClass = bean.Extendable ? "" : "sealed ";
            var baseClass = bean.Base == "" ? "Zeze.Util.ConfBean" : bean.Base;
            var extraInterface = bean.Interface == "" ? "" : ", " + bean.Interface;

            sw.WriteLine("// auto-generated");
            sw.WriteLine("using ByteBuffer = Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("using Environment = System.Environment;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine("// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier");
            sw.WriteLine("// ReSharper disable once CheckNamespace");
            sw.WriteLine("namespace " + bean.Space.Path());
            sw.WriteLine("{");
            sw.WriteLine("    [System.Serializable]");
            sw.WriteLine($"    public {preClass}class {bean.Name} : {baseClass}{extraInterface}");
            sw.WriteLine("    {");
            WriteDefine(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void WriteDefine(StreamWriter sw)
        {
            // declare enums
            foreach (Enum e in bean.Enums)
                sw.WriteLine($"        public const {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (bean.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            foreach (Variable v in bean.Variables)
                sw.WriteLine("        public " + TypeName.GetName(v.VariableType) + " " + v.NameUpper1 + ";" + v.Comment);
            sw.WriteLine();

            Property.Make(bean, sw, "        ");
            Construct.Make(bean, sw, "        ");
            // cs.Assign.Make(bean, sw, "        ");
            // Copy
            // sw.WriteLine();
            // sw.WriteLine("        public override " + bean.Name + " Copy()");
            // sw.WriteLine("        {");
            // sw.WriteLine("            var copy = new " + bean.Name + "();");
            // sw.WriteLine("            copy.Assign(this);");
            // sw.WriteLine("            return copy;");
            // sw.WriteLine("        }");
            // sw.WriteLine();
            // sw.WriteLine($"        public static void Swap({bean.Name} a, {bean.Name} b)");
            // sw.WriteLine("        {");
            // sw.WriteLine($"            {bean.Name} save = a.Copy();");
            // sw.WriteLine("            a.Assign(b);");
            // sw.WriteLine("            b.Assign(save);");
            // sw.WriteLine("        }");
            // sw.WriteLine();
            if (bean.Base == "")
                sw.WriteLine("        public const long TYPEID = " + bean.TypeId + ";");
            else
                sw.WriteLine("        public new const long TYPEID = " + bean.TypeId + ";");
            sw.WriteLine("        public override long TypeId => " + bean.TypeId + ";");
            sw.WriteLine();
            var macro = project.MacroEditor;
            if (false == string.IsNullOrEmpty(macro))
                sw.WriteLine($"#if {macro}");
            {
                cs.Tostring.Make(bean, sw, "        ");
                cs.Encode.Make(bean, sw, "        ");
                // cs.NegativeCheck.Make(bean, sw, "        ");
            }
            if (false == string.IsNullOrEmpty(macro))
                sw.WriteLine($"#endif // {macro}");
            cs.Decode.Make(bean, sw, "        ");
            if (followerApply)
                FollowerApply.Make(bean, sw, "        ");

            cs.ClearParameters.Make(bean, sw, "        ");
        }
    }
}
