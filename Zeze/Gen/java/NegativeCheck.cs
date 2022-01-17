using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    class NegativeCheck : Types.Visitor
    {
        private System.IO.StreamWriter sw;
        private string varname;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public boolean NegativeCheck() {");
            foreach (Types.Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.Getter, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
        }

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public boolean NegativeCheck() {");
            foreach (Types.Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.Getter, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
        }

        NegativeCheck(System.IO.StreamWriter sw, string varname, string prefix)
        {
            this.sw = sw;
            this.varname = varname;
            this.prefix = prefix;
        }

        void Visitor.Visit(Bean type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        void Visitor.Visit(BeanKey type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        void Visitor.Visit(TypeByte type)
        {
        }

        void Visitor.Visit(TypeDouble type)
        {
        }

        void Visitor.Visit(TypeInt type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        void Visitor.Visit(TypeLong type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        void Visitor.Visit(TypeBool type)
        {
        }

        void Visitor.Visit(TypeBinary type)
        {
        }

        void Visitor.Visit(TypeString type)
        {
        }

        void Visitor.Visit(TypeList type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + varname + ") {");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        void Visitor.Visit(TypeSet type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + varname + ") {");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        void Visitor.Visit(TypeMap type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + varname + ".values()) {");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        void Visitor.Visit(TypeFloat type)
        {
        }

        void Visitor.Visit(TypeShort type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }
    }
}
