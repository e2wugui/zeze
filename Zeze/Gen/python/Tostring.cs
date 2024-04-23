using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Tostring : Visitor
    {
        public const int INDENT_SIZE = 4;

        readonly StreamWriter sw;
        readonly string varName;
        readonly string getter;
        readonly string prefix;
        readonly char sep;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def __str__(self):");
            sw.WriteLine($"{prefix}    sb = []");
            sw.WriteLine($"{prefix}    self.build_string(sb, 0)");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    return \"\".join(sb)");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def build_string(self, sb, level):");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}    sb.append(\"{bean.FullName}: {{\")");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE}");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.Name, $"self.{var.Name}", prefix + "    ", sep));
            }
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}    sb.append('}}')");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def __str__(self):");
            sw.WriteLine($"{prefix}    sb = []");
            sw.WriteLine($"{prefix}    self.build_string(sb, 0)");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    return \"\".join(sb)");
            sw.WriteLine();
            sw.WriteLine($"{prefix}def build_string(self, sb, level):");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}    sb.append(\"{bean.FullName}: {{\")");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE}");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.Name, $"self.{var.Name}", prefix + "    ", sep));
            }
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}    sb.append('}}')");
        }

        public Tostring(StreamWriter sw, string varName, string getter, string prefix, char sep)
        {
            this.sw = sw;
            this.varName = varName;
            this.getter = getter;
            this.prefix = prefix;
            this.sep = sep;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}=\\n\")");
            sw.WriteLine($"{prefix}{getter}.build_string(sb, level + {INDENT_SIZE})");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}=\\n\")");
            sw.WriteLine($"{prefix}{getter}.buildString(sb, level + {INDENT_SIZE})");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        void formatSimple()
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}=\")");
            sw.WriteLine($"{prefix}sb.append(str({getter}))");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        public void Visit(TypeBool type)
        {
            formatSimple();
        }

        public void Visit(TypeByte type)
        {
            formatSimple();
        }

        public void Visit(TypeShort type)
        {
            formatSimple();
        }

        public void Visit(TypeInt type)
        {
            formatSimple();
        }

        public void Visit(TypeLong type)
        {
            formatSimple();
        }

        public void Visit(TypeFloat type)
        {
            formatSimple();
        }

        public void Visit(TypeDouble type)
        {
            formatSimple();
        }

        public void Visit(TypeBinary type)
        {
            formatSimple();
        }

        public void Visit(TypeString type)
        {
            formatSimple();
        }

        public void Visit(TypeDecimal type)
        {
            formatSimple();
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}=[\")");
            sw.WriteLine($"{prefix}if len({getter}) > 0:");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    for _v_ in {getter}:");
            type.ValueType.Accept(new Tostring(sw, "Item", "_v_", prefix + "        ", ','));
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(']')");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}={{\")");
            sw.WriteLine($"{prefix}if len({getter}) > 0:");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    for _v_ in {getter}:");
            type.ValueType.Accept(new Tostring(sw, "Item", "_v_", prefix + "        ", ','));
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append('}}')");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}={{\")");
            sw.WriteLine($"{prefix}if len({getter}) > 0:");
            sw.WriteLine($"{prefix}    sb.append('\\n')");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    for _k_, _v_ in {getter}:");
            // sw.WriteLine(prefix + "        sb.append(indent(level)).append('(').append('\\n')");
            type.KeyType.Accept(new Tostring(sw, "Key", "_k_", prefix + "        ", ','));
            type.ValueType.Accept(new Tostring(sw, "Value", "_v_", prefix + "        ", ','));
            // sw.WriteLine(prefix + "        sb.append(indent(level)).append(')').append('\\n')");
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE}");
            sw.WriteLine($"{prefix}    sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append('}}')");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}sb.append(indent(level))");
            sw.WriteLine($"{prefix}sb.append(\"{varName}=\")");
            sw.WriteLine($"{prefix}sb.append('\\n')");
            sw.WriteLine($"{prefix}{getter}.buildString(sb, level + {INDENT_SIZE})");
            sw.WriteLine(sep != 0 ? $"{prefix}sb.append(\"{sep}\\n\")" : $"{prefix}sb.append('\\n')");
        }

        public void Visit(TypeQuaternion type)
        {
            formatSimple();
        }

        public void Visit(TypeVector2 type)
        {
            formatSimple();
        }

        public void Visit(TypeVector2Int type)
        {
            formatSimple();
        }

        public void Visit(TypeVector3 type)
        {
            formatSimple();
        }

        public void Visit(TypeVector3Int type)
        {
            formatSimple();
        }

        public void Visit(TypeVector4 type)
        {
            formatSimple();
        }
    }
}
