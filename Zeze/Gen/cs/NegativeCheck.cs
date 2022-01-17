using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    class NegativeCheck : Types.Visitor
    {
        private System.IO.StreamWriter sw;
        private string varname;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override bool NegativeCheck()");
            sw.WriteLine(prefix + "{");
            foreach (Types.Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.NameUpper1, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public bool NegativeCheck()");
            sw.WriteLine(prefix + "{");
            foreach (Types.Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.NameUpper1, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
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
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck()) return true;");
        }

        void Visitor.Visit(BeanKey type)
        {
            if (type.IsNeedNegativeCheck)
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck()) return true;");
        }

        void Visitor.Visit(TypeByte type)
        {
        }

        void Visitor.Visit(TypeDouble type)
        {
        }

        void Visitor.Visit(TypeInt type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
        }

        void Visitor.Visit(TypeLong type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
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
                sw.WriteLine(prefix + "foreach (var _v_ in " + varname + ")");
                sw.WriteLine(prefix + "{");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        void Visitor.Visit(TypeSet type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "foreach (var _v_ in " + varname + ")");
                sw.WriteLine(prefix + "{");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        void Visitor.Visit(TypeMap type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "foreach (var _v_ in " + varname + ".Values)");
                sw.WriteLine(prefix + "{");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        void Visitor.Visit(TypeFloat type)
        {
        }

        void Visitor.Visit(TypeShort type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            if (type.IsNeedNegativeCheck)
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck()) return true;");
        }
    }
}
