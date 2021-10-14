using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Tostring : Types.Visitor
    {
		private System.IO.StreamWriter sw;
		private string varname;
        private string getter;
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
                var.VariableType.Accept(new Tostring(sw, var.Name, var.Getter, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    sb.append(\"}\");");
            sw.WriteLine(prefix + "}");
			sw.WriteLine("");
		}

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, String prefix)
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
            sw.WriteLine($"{prefix}    sb.append(\" \".repeat(level * 4)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    level++;");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                var sep = i == bean.Variables.Count - 1 ? "" : ",";
                var.VariableType.Accept(new Tostring(sw, var.Name, var.Getter, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    sb.append(\"}\");");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Tostring(System.IO.StreamWriter sw, string varname, string getter, String prefix, string sep)
        {
            this.sw = sw;
            this.varname = varname;
            this.getter = getter;
            this.prefix = prefix;
            this.sep = sep;
        }

        void Visitor.Visit(Bean type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + getter + ".BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.append(\"{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + getter + ".BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.append(\"{sep}\").append(System.lineSeparator());");
        }

        private void formatSimple()
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=\").append({getter}).append(\"{sep}\").append(System.lineSeparator());");
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
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"for (var _item_ : {getter}) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", "_item_", prefix + "    ", ","));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"]{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"for (var _item_ : {getter}) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", "_item_", prefix + "    ", ","));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"]{sep}\").append(System.lineSeparator());");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=[\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"for (var _kv_ : {getter}().entrySet()) {{");
            sw.WriteLine(prefix + "    sb.append(\"(\").append(System.lineSeparator());");
            type.KeyType.Accept(new Tostring(sw, "Key", "_kv_.getKey()", prefix + "    ", ","));
            type.ValueType.Accept(new Tostring(sw, "Value", "_kv_.getValue()", prefix + "    ", ","));
            sw.WriteLine(prefix + "    sb.append(\")\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"]{sep}\").append(System.lineSeparator());");
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
            sw.WriteLine(prefix + $"sb.append(\" \".repeat(level * 4)).append(\"{varname}\").append(\"=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + getter + ".getBean().BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.append(\"{sep}\").append(System.lineSeparator());");
        }
    }
}
