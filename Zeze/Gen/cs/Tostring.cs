using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Tostring : Types.Visitor
    {
		private System.IO.StreamWriter sw;
		private string var;
		private String prefix;
        private string sep;

		public static void Make(Types.Bean bean, System.IO.StreamWriter sw, String prefix)
		{
            sw.WriteLine(prefix + "public override string ToString()");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    System.Text.StringBuilder sb = new System.Text.StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    return sb.ToString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public override void BuildString(System.Text.StringBuilder sb, int level)");
			sw.WriteLine(prefix + "{");
            sw.WriteLine($"{prefix}    sb.Append(new string(' ', level)).Append(\"{bean.FullName}: {{\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    level++;");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                var sep = i == bean.Variables.Count - 1 ? "" : ",";
                var.VariableType.Accept(new Tostring(sw, var.NameUpper1, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    sb.Append(\"}\");");
            sw.WriteLine(prefix + "}");
			sw.WriteLine("");
		}

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, String prefix)
        {
            sw.WriteLine(prefix + "public override string ToString()");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    System.Text.StringBuilder sb = new System.Text.StringBuilder();");
            sw.WriteLine(prefix + "    BuildString(sb, 0);");
            sw.WriteLine(prefix + "    sb.Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    return sb.ToString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void BuildString(System.Text.StringBuilder sb, int level)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine($"{prefix}    sb.Append(new string(' ', level)).Append(\"{bean.FullName}: {{\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    level++;");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                var sep = i == bean.Variables.Count - 1 ? "" : ",";
                var.VariableType.Accept(new Tostring(sw, var.NameUpper1, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    sb.Append(\"}\");");
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
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + var + ".BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + var + ".BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeByte type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeInt type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeLong type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeBool type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeString type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=[\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"foreach (var Item in {var})");
            sw.WriteLine(prefix + "{");
            type.ValueType.Accept(new Tostring(sw, "Item", prefix + "    ", ","));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"]{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=[\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"foreach (var Item in {var})");
            sw.WriteLine(prefix + "{");
            type.ValueType.Accept(new Tostring(sw, "Item", prefix + "    ", ","));
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"]{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=[\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "level++;");
            sw.WriteLine(prefix + $"foreach (var _kv_ in {var})");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    sb.Append(\"(\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "    var Key = _kv_.Key;");
            type.KeyType.Accept(new Tostring(sw, "Key", prefix + "    ", ","));
            sw.WriteLine(prefix + "    var Value = _kv_.Value;");
            type.ValueType.Accept(new Tostring(sw, "Value", prefix + "    ", ","));
            sw.WriteLine(prefix + "    sb.Append(\")\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine(prefix + "level--;");
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"]{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeShort type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append({var}).Append(\"{sep}\").Append(Environment.NewLine);");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"sb.Append(new string(' ', level)).Append(\"{var}\").Append(\"=\").Append(Environment.NewLine);");
            sw.WriteLine(prefix + var + ".Bean.BuildString(sb, level + 1);");
            sw.WriteLine(prefix + $"sb.Append(\"{sep}\").Append(Environment.NewLine);");
        }
    }
}
