using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Tostring : Visitor
    {
        public const int INDENT_SIZE = 4;

		readonly StreamWriter sw;
		readonly string varname;
        readonly string getter;
        readonly string prefix;
        readonly char sep;

		public static void Make(Bean bean, StreamWriter sw, string prefix)
		{
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public String toString() {");
            sw.WriteLine(prefix + "    var sb = new StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    return sb.toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void BuildString(StringBuilder sb, int level) {");
            sw.WriteLine($"{prefix}    sb.append(Zeze.Util.Str.indent(level)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    level += " + INDENT_SIZE + ';');
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.Name, var.Getter, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    level -= " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + "    sb.append(Zeze.Util.Str.indent(level)).append(\"}\");");
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
		}

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public String toString() {");
            sw.WriteLine(prefix + "    var sb = new StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    return sb.toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void BuildString(StringBuilder sb, int level) {");
            sw.WriteLine($"{prefix}    sb.append(Zeze.Util.Str.indent(level)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    level += " + INDENT_SIZE + ';');
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.Name, var.Getter, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    level -= " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + "    sb.append(Zeze.Util.Str.indent(level)).append(\"}\");");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Tostring(StreamWriter sw, string varname, string getter, string prefix, char sep)
        {
            this.sw = sw;
            this.varname = varname;
            this.getter = getter;
            this.prefix = prefix;
            this.sep = sep;
        }

        void Visitor.Visit(Bean type)
        {
            sw.WriteLine(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append('=').append(System.lineSeparator());");
            sw.WriteLine(prefix + getter + ".BuildString(sb, level + " + INDENT_SIZE + ");");
            sw.Write(prefix + "sb");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        void Visitor.Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append('=').append(System.lineSeparator());");
            sw.WriteLine(prefix + getter + ".BuildString(sb, level + " + INDENT_SIZE + ");");
            sw.Write(prefix + "sb");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        private void formatSimple()
        {
            sw.Write(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append('=').append({getter})");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeByte type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeDouble type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeInt type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeLong type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeBool type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeBinary type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeString type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level += " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + $"for (var _item_ : {getter}) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", "_item_", prefix + "    ", ','));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level -= " + INDENT_SIZE + ';');
            sw.Write(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(']')");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level += " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + $"for (var _item_ : {getter}) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", "_item_", prefix + "    ", ','));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level -= " + INDENT_SIZE + ';');
            sw.Write(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(']')");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level += " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + $"for (var _kv_ : {getter}.entrySet()) {{");
            sw.WriteLine(prefix + "    sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());");
            type.KeyType.Accept(new Tostring(sw, "Key", "_kv_.getKey()", prefix + "    ", ','));
            type.ValueType.Accept(new Tostring(sw, "Value", "_kv_.getValue()", prefix + "    ", ','));
            sw.WriteLine(prefix + "    sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level -= " + INDENT_SIZE + ';');
            sw.Write(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(']')");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeFloat type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeShort type)
        {
            formatSimple();
        }

        void Visitor.Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"sb.append(Zeze.Util.Str.indent(level)).append(\"{varname}\").append('=').append(System.lineSeparator());");
            sw.WriteLine(prefix + getter + ".getBean().BuildString(sb, level + " + INDENT_SIZE + ");");
            sw.Write(prefix + "sb");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }
    }
}
