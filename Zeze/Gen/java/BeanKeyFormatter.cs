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
            sw.WriteLine("import Zeze.Serialize.IByteBuffer;");
            sw.WriteLine();
            if (beanKey.Comment.Length > 0)
                sw.WriteLine(beanKey.Comment);
            sw.WriteLine("@SuppressWarnings({\"MethodMayBeStatic\", \"NullableProblems\", \"PatternVariableCanBeUsed\", \"RedundantIfStatement\", \"RedundantSuppression\", \"SuspiciousNameCombination\", \"UnusedAssignment\"})");
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
            if (beanKey.Variables.Count > 0)
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
            GenVariables(beanKey, sw, "    ");
            sw.WriteLine("}");
        }

        public void GenVariables(BeanKey bean, StreamWriter sw, string prefix)
        {
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine();
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {{");
                sw.WriteLine($"{prefix}    var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();");
                foreach (var v in bean.VariablesIdOrder)
                {
                    string type = v.Type;
                    string key = v.Key;
                    string value = v.Value;

                    var vType = v.VariableType;
                    if (vType.IsBean)
                    {
                        type = Variable.GetBeanFullName(vType);
                    }
                    else if (vType.IsCollection)
                    {
                        if (vType is TypeMap map)
                        {
                            if (map.KeyType.IsBean)
                                key = Variable.GetBeanFullName(map.KeyType);
                            if (map.ValueType.IsBean)
                                value = Variable.GetBeanFullName(map.ValueType);
                        }
                        else if (vType is TypeList list)
                        {
                            if (list.ValueType.IsBean)
                                value = Variable.GetBeanFullName(list.ValueType);
                        }
                        else if (vType is TypeSet set)
                        {
                            if (set.ValueType.IsBean)
                                value = Variable.GetBeanFullName(set.ValueType);
                        }
                    }
                    sw.WriteLine($"{prefix}    vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data({v.Id}, \"{v.Name}\", \"{type}\", \"{key}\", \"{value}\"));");
                }
                sw.WriteLine($"{prefix}    return vars;");
                sw.WriteLine($"{prefix}}}");
            }
        }
    }
}
