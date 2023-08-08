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
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + beanKey.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Serialize.Serializable;");

            sw.WriteLine();
            if (beanKey.Comment.Length > 0)
                sw.WriteLine(beanKey.Comment);
            sw.WriteLine("@SuppressWarnings({\"UnusedAssignment\", \"RedundantIfStatement\", \"RedundantSuppression\", \"MethodMayBeStatic\", \"PatternVariableCanBeUsed\", \"NullableProblems\", \"SuspiciousNameCombination\"})");
            sw.WriteLine($"public final class {beanKey.Name} implements Zeze.Transaction.BeanKey, Comparable<{beanKey.Name}> {{");

            // declare enums
            foreach (Enum e in beanKey.Enums)
                sw.WriteLine($"    public static final {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (beanKey.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            foreach (Variable v in beanKey.Variables)
            {
                string final = v.VariableType is BeanKey ? "final " : "";
                sw.WriteLine($"    private {final}{TypeName.GetName(v.VariableType)} {v.NamePrivate};{v.Comment}");
            }
            sw.WriteLine();

            Construct.Make(beanKey, sw, "    ");
            // params construct
            if (beanKey.Variables.Count > 0)
            {
                sw.WriteLine("    public " + beanKey.Name + "(" + ParamName.GetParamList(beanKey.Variables) + ") {");
                foreach (Variable v in beanKey.Variables)
                {
                    if (!v.VariableType.IsJavaPrimitive)
                    {
                        sw.WriteLine($"        if ({v.NamePrivate}_ == null)");
                        sw.WriteLine($"            throw new IllegalArgumentException();");
                        if (v.VariableType is TypeString)
                        {
                            // BeanKey 类型为String时，限制长度为256。
                            // 这个主要是 RelationalMapping的限制。
                            // 另外对于KV也有限制总长度不能超过3070-porlardbx（3072 mysql）。
                            // 【这里没法区分具体使用什么数据库，先全部限制一下。】
                            sw.WriteLine($"        if ({v.NamePrivate}_.length() > 256)");
                            sw.WriteLine($"            throw new IllegalArgumentException();");
                        }
                    }
                    sw.WriteLine("        this." + v.NamePrivate + " = " + v.NamePrivate + "_;");
                }
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            PropertyBeanKey.Make(beanKey, sw, "    ");
            Tostring.Make(beanKey, sw, "    ");
            Encode.Make(beanKey, sw, "    ");
            Decode.Make(beanKey, sw, "    ");
            Equal.Make(beanKey, sw, "    ", true);
            HashCode.Make(beanKey, sw, "    ", true);
            Compare.Make(beanKey, sw, "    ");
            NegativeCheck.Make(beanKey, sw, "    ");
            DecodeResultSet.Make(beanKey, sw, "    ");
            EncodeSQLStatement.Make(beanKey, sw, "    ");
            sw.WriteLine("}");
        }
    }
}
