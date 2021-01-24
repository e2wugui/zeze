using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Tostring : Types.Visitor
    {
		private System.IO.StreamWriter sw;
		private Types.Variable var;
		private String prefix;
        private string sep;

		public static void Make(Types.Bean bean, System.IO.StreamWriter sw, String prefix)
		{
			sw.WriteLine(prefix + "public override string ToString()");
			sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    System.Text.StringBuilder sb = new System.Text.StringBuilder();");
            sw.WriteLine(prefix + "    sb.Append(\"" + bean.FullName + "{\");");
            string sep = "";
            foreach (Types.Variable var in bean.Variables)
			{
				var.VariableType.Accept(new Tostring(sw, var, prefix + "    ", sep));
                if (sep.Length == 0)
                    sep = ",";
			}
            sw.WriteLine(prefix + "    sb.Append(\"}\");");
            sw.WriteLine(prefix + "    return sb.ToString();");
            sw.WriteLine(prefix + "}");
			sw.WriteLine("");
		}

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, String prefix)
        {
            sw.WriteLine(prefix + "public override string ToString()");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    System.Text.StringBuilder sb = new System.Text.StringBuilder();");
            sw.WriteLine(prefix + "    sb.Append(\"" + bean.FullName + "{\");");
            string sep = "";
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Tostring(sw, var, prefix + "    ", sep));
                if (sep.Length == 0)
                    sep = ",";
            }
            sw.WriteLine(prefix + "    sb.Append(\"}\");");
            sw.WriteLine(prefix + "    return sb.ToString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Tostring(System.IO.StreamWriter sw, Types.Variable var, String prefix, string sep)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
            this.sep = sep;
        }

        private void WriteAppend()
        {
            sw.WriteLine(prefix + "sb.Append(\"" + sep + "\").Append(\"" + var.NameUpper1 + ":\").Append(" + var.NameUpper1 + ");");
        }

        void Visitor.Visit(Bean type)
        {
            WriteAppend();
        }

        void Visitor.Visit(BeanKey type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeByte type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeDouble type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeInt type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeLong type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeBool type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + "sb.Append(\"" + sep + "\").Append(\"" + var.NameUpper1 + ":\").Append(" + var.NameUpper1 + ");");
        }

        void Visitor.Visit(TypeString type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeList type)
        {
            sw.WriteLine(prefix + "sb.Append(\"" + sep + "\").Append(\"" + var.NameUpper1 + ":\");");
            sw.WriteLine(prefix + "ByteBuffer.BuildString(sb, " + var.NameUpper1 + ");");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "sb.Append(\"" + sep + "\").Append(\"" + var.NameUpper1 + ":\");");
            sw.WriteLine(prefix + "ByteBuffer.BuildString(sb, " + var.NameUpper1 + ");");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "sb.Append(\"" + sep + "\").Append(\"" + var.NameUpper1 + ":\");");
            sw.WriteLine(prefix + "ByteBuffer.BuildString(sb, " + var.NameUpper1 + ");");
        }

        void Visitor.Visit(TypeFloat type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeShort type)
        {
            WriteAppend();
        }

        void Visitor.Visit(TypeDynamic type)
        {
            WriteAppend();
        }
    }
}
