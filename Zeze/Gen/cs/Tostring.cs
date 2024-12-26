﻿using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Tostring : Visitor
    {
        public const int INDENT_SIZE = 4;

        readonly StreamWriter sw;
		readonly string var;
		readonly string prefix;
        readonly char sep;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool varNameUpper = true)
		{
            sw.WriteLine(prefix + "public override string ToString()");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    var sb = new System.Text.StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    return sb.ToString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public override void BuildString(System.Text.StringBuilder sb, int level)");
			sw.WriteLine(prefix + "{");
            sw.WriteLine($"{prefix}    sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{bean.FullName}: {{\").Append(Environment.NewLine);");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE};");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, varNameUpper ? var.NameUpper1 : var.Name, prefix + "    ", sep));
            }
            if (bean.Base != "")
            {
                sw.WriteLine($"{prefix}    base.BuildString(sb, level);");
                sw.WriteLine($"{prefix}    sb.Append(Environment.NewLine);");
            }
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE};");
            sw.WriteLine(prefix + "    sb.Append(Zeze.Util.Str.Indent(level)).Append('}');");
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
		}

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override string ToString()");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    var sb = new System.Text.StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    return sb.ToString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void BuildString(System.Text.StringBuilder sb, int level)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine($"{prefix}    sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{bean.FullName}: {{\").Append(Environment.NewLine);");
            sw.WriteLine($"{prefix}    level += {INDENT_SIZE};");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.NameUpper1, prefix + "    ", sep));
            }
            sw.WriteLine($"{prefix}    level -= {INDENT_SIZE};");
            sw.WriteLine(prefix + "    sb.Append(Zeze.Util.Str.Indent(level)).Append('}');");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Tostring(StreamWriter sw, string var, string prefix, char sep)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
            this.sep = sep;
        }

        void FormatSimple()
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append('=').Append({var})");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeBool type)
        {
            FormatSimple();
        }

        public void Visit(TypeByte type)
        {
            FormatSimple();
        }

        public void Visit(TypeShort type)
        {
            FormatSimple();
        }

        public void Visit(TypeInt type)
        {
            FormatSimple();
        }

        public void Visit(TypeLong type)
        {
            FormatSimple();
        }

        public void Visit(TypeFloat type)
        {
            FormatSimple();
        }

        public void Visit(TypeDouble type)
        {
            FormatSimple();
        }

        public void Visit(TypeBinary type)
        {
            FormatSimple();
        }

        public void Visit(TypeString type)
        {
            FormatSimple();
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=[\").Append(Environment.NewLine);");
            var prefix1 = prefix;
            if (type is TypeArray)
            {
                sw.WriteLine($"{prefix}if ({var} != null)");
                sw.WriteLine($"{prefix}{{");
                prefix1 = prefix + "    ";
            }
            sw.WriteLine($"{prefix1}level += {INDENT_SIZE};");
            sw.WriteLine(prefix1 + $"foreach (var Item in {var})");
            sw.WriteLine(prefix1 + "{");
            type.ValueType.Accept(new Tostring(sw, "Item", prefix1 + "    ", ','));
            sw.WriteLine(prefix1 + "}");
            sw.WriteLine($"{prefix1}level -= {INDENT_SIZE};");
            if (type is TypeArray)
                sw.WriteLine($"{prefix}}}");
            sw.Write(prefix + "sb.Append(Zeze.Util.Str.Indent(level)).Append(']')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=[\").Append(Environment.NewLine);");
            sw.WriteLine($"{prefix}level += {INDENT_SIZE};");
            sw.WriteLine(prefix + $"foreach (var Item in {var})");
            sw.WriteLine(prefix + "{");
            type.ValueType.Accept(new Tostring(sw, "Item", prefix + "    ", ','));
            sw.WriteLine(prefix + "}");
            sw.WriteLine($"{prefix}level -= {INDENT_SIZE};");
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(']')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=[\").Append(Environment.NewLine);");
            sw.WriteLine($"{prefix}level += {INDENT_SIZE};");
            sw.WriteLine(prefix + $"foreach (var _kv_ in {var})");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    var Key = _kv_.Key;");
            type.KeyType.Accept(new Tostring(sw, "Key", prefix + "    ", ','));
            sw.WriteLine(prefix + "    var Value = _kv_.Value;");
            type.ValueType.Accept(new Tostring(sw, "Value", prefix + "    ", ','));
            sw.WriteLine(prefix + "    sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine($"{prefix}level -= {INDENT_SIZE};");
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(']')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append('=').Append(Environment.NewLine);");
            sw.WriteLine(prefix + $"{var}.BuildString(sb, level + {INDENT_SIZE});");
            sw.Write(prefix + "sb");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append('=').Append(Environment.NewLine);");
            sw.WriteLine(prefix + $"{var}.BuildString(sb, level + {INDENT_SIZE});");
            sw.Write(prefix + "sb");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append('=').Append(Environment.NewLine);");
            if (Project.MakingInstance.Platform.StartsWith("conf+cs"))
                sw.WriteLine(prefix + $"{var}?.BuildString(sb, level + {INDENT_SIZE});");
            else
                sw.WriteLine(prefix + $"{var}.Bean.BuildString(sb, level + {INDENT_SIZE});");
            sw.Write(prefix + "sb");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=(\").Append({var}.x).Append(',').Append({var}.y).Append(',').Append({var}.z).Append(',').Append({var}.w).Append(')')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeVector2 type)
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=(\").Append({var}.x).Append(',').Append({var}.y).Append(')')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=(\").Append({var}.x).Append(',').Append({var}.y).Append(')')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeVector3 type)
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=(\").Append({var}.x).Append(',').Append({var}.y).Append(',').Append({var}.z).Append(')')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=(\").Append({var}.x).Append(',').Append({var}.y).Append(',').Append({var}.z).Append(')')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeVector4 type)
        {
            sw.Write(prefix + $"sb.Append(Zeze.Util.Str.Indent(level)).Append(\"{var}\").Append(\"=(\").Append({var}.x).Append(',').Append({var}.y).Append(',').Append({var}.z).Append(',').Append({var}.w).Append(')')");
            if (sep != 0)
                sw.Write($".Append('{sep}')");
            sw.WriteLine(".Append(Environment.NewLine);");
        }

        public void Visit(TypeDecimal type)
        {
            FormatSimple();
        }

        public void Visit(TypeGTable type)
        {
            //throw new System.NotImplementedException();
        }
    }
}
