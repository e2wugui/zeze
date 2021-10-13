using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Tostring : Types.Visitor
    {
		private System.IO.StreamWriter sw;
		private string var; // TODO 需要类型，临时变量。
        private String prefix;
        private string sep;

		public static void Make(Types.Bean bean, System.IO.StreamWriter sw, String prefix)
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
            sw.WriteLine($"{prefix}    sb.append(\" \".repeat(level * 4)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    level++;");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                var sep = i == bean.Variables.Count - 1 ? "" : ",";
                var.VariableType.Accept(new Tostring(sw, var.NameUpper1, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    sb.append(\"}\");");
            sw.WriteLine(prefix + "}");
			sw.WriteLine("");
		}

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, String prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public override String toString() {");
            sw.WriteLine(prefix + "    var sb = new StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    return sb.toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void BuildString(StringBuilder sb, int level) {");
            sw.WriteLine($"{prefix}    sb.append(\" \".repeat(level * 4)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    level++;");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                var sep = i == bean.Variables.Count - 1 ? "" : ",";
                var.VariableType.Accept(new Tostring(sw, var.NameUpper1, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    sb.append(\"}\");");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Tostring(System.IO.StreamWriter sw, string var, String prefix, string sep)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
            this.sep = sep;
        }

        void Visitor.Visit(Bean type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + $"get{var}()" + ".BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.Append(\"{sep}\").Append(System.lineSeparator());");
        }

        void Visitor.Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + $"get{var}()" + ".BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeByte type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeInt type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeLong type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeBool type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeString type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"for (var Item : get{var}()) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", prefix + "    ", ","));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"]{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"for (var Item : get{var}()) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", prefix + "    ", ","));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"]{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"for (var _kv_ : get{var}().entrySet()) {{");
            sw.WriteLine(prefix + "    sb.append(\"(\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    var Key = _kv_.Key;");
            type.KeyType.Accept(new Tostring(sw, "Key", prefix + "    ", ","));
            sw.WriteLine(prefix + "    var Value = _kv_.Value;");
            type.ValueType.Accept(new Tostring(sw, "Value", prefix + "    ", ","));
            sw.WriteLine(prefix + "    sb.append(\")\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"]{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").Append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeShort type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(get{var}()).append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{var}\").append(\"=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + var + ".getBean().BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.append(\"{sep}\").append(System.lineSeparator());");
        }
    }
}
