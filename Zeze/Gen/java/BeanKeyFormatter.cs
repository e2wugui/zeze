using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class BeanKeyFormatter
    {
        readonly BeanKey beanKey;

        public BeanKeyFormatter(BeanKey beanKey)
        {
            this.beanKey = beanKey;
        }

        public void Make(string baseDir)
        {
            using StreamWriter sw = beanKey.Space.OpenWriter(baseDir, beanKey.Name + ".java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + beanKey.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Serialize.Serializable;");

            sw.WriteLine();
            sw.WriteLine($"public final class {beanKey.Name} implements Serializable, Comparable<{beanKey.Name}> {{");

            // declare enums
            foreach (Enum e in beanKey.Enums)
                sw.WriteLine("    public static final int " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (beanKey.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            foreach (Variable v in beanKey.Variables)
                sw.WriteLine("    private " + TypeName.GetName(v.VariableType) + " " + v.NamePrivate + ";" + v.Comment);
            sw.WriteLine();

            sw.WriteLine("    // for decode only");
            sw.WriteLine("    public " + beanKey.Name + "() {");
            sw.WriteLine("    }");
            sw.WriteLine();

            // params construct
            {
                sw.WriteLine("    public " + beanKey.Name + "(" + ParamName.GetParamList(beanKey.Variables) + ") {");
                foreach (Variable v in beanKey.Variables)
                    sw.WriteLine("        this." + v.NamePrivate + " = " + v.NamePrivate + "_;");
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            PropertyBeanKey.Make(beanKey, sw, "    ");
            sw.WriteLine();
            Tostring.Make(beanKey, sw, "    ");
            Encode.Make(beanKey, sw, "    ");
            Decode.Make(beanKey, sw, "    ");
            Equal.Make(beanKey, sw, "    ");
            HashCode.Make(beanKey, sw, "    ");
            Compare.Make(beanKey, sw, "    ");
            NegativeCheck.Make(beanKey, sw, "    ");
            sw.WriteLine("}");
        }
    }
}
