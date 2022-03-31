using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    class NegativeCheck : Visitor
    {
        readonly StreamWriter sw;
        readonly string varname;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@SuppressWarnings(\"RedundantIfStatement\")");
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public boolean NegativeCheck() {");
            foreach (Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.Getter, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public boolean NegativeCheck() {");
            foreach (Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.Getter, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
        }

        NegativeCheck(StreamWriter sw, string varname, string prefix)
        {
            this.sw = sw;
            this.varname = varname;
            this.prefix = prefix;
        }

        public void Visit(TypeBool type)
        {
        }

        public void Visit(TypeByte type)
        {
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        public void Visit(TypeFloat type)
        {
        }

        public void Visit(TypeDouble type)
        {
        }


        public void Visit(TypeBinary type)
        {
        }

        public void Visit(TypeString type)
        {
        }

        public void Visit(TypeList type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + varname + ") {");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeSet type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + varname + ") {");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeMap type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + varname + ".values()) {");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(Bean type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        public void Visit(BeanKey type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        public void Visit(TypeDynamic type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }
    }
}
