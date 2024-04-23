using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    class NegativeCheck : Visitor
    {
        readonly TextWriter sw;
        readonly Variable var;
        readonly string varname;
        readonly string prefix;

        string Getter => var != null ? var.Getter : varname;
        string NamePrivate => var != null ? var.NamePrivate : varname;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            var sw1 = new StringWriter();
            sw1.NewLine = "\n";
            foreach (Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw1, var, null, prefix + "    "));
            }
            var s = sw1.ToString();
            if (s.Length > 0)
            {
                sw.WriteLine(prefix + "@Override");
                sw.WriteLine(prefix + "public boolean negativeCheck() {");
                sw.Write(s);
                sw.WriteLine(prefix + "    return false;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public boolean negativeCheck() {");
            foreach (Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var, null, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        NegativeCheck(TextWriter sw, Variable var, string varname, string prefix)
        {
            this.sw = sw;
            this.var = var;
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
            sw.WriteLine(prefix + "if (" + Getter + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + "if (" + Getter + " < 0)");
            sw.WriteLine(prefix + "    return true;");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + "if (" + Getter + " < 0)");
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
                sw.WriteLine(prefix + "for (var _v_ : " + NamePrivate + ") {");
                type.ValueType.Accept(new NegativeCheck(sw, null, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeSet type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + NamePrivate + ") {");
                type.ValueType.Accept(new NegativeCheck(sw, null, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeMap type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "for (var _v_ : " + NamePrivate + ".values()) {");
                type.ValueType.Accept(new NegativeCheck(sw, null, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(Bean type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + NamePrivate + ".negativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        public void Visit(BeanKey type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + Getter + ".negativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        public void Visit(TypeDynamic type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "if (" + NamePrivate + ".negativeCheck())");
                sw.WriteLine(prefix + "    return true;");
            }
        }

        public void Visit(TypeQuaternion type)
        {
        }

        public void Visit(TypeVector2 type)
        {
        }

        public void Visit(TypeVector2Int type)
        {
        }

        public void Visit(TypeVector3 type)
        {
        }

        public void Visit(TypeVector3Int type)
        {
        }

        public void Visit(TypeVector4 type)
        {
        }

        public void Visit(TypeDecimal type)
        {
            sw.WriteLine(prefix + "if (" + Getter + ".signum() == -1)");
            sw.WriteLine(prefix + "    return true;");
        }
    }
}
