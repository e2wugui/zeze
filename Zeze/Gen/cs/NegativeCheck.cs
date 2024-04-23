using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    class NegativeCheck : Visitor
    {
        readonly StreamWriter sw;
        readonly string varname;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override bool NegativeCheck()");
            sw.WriteLine(prefix + "{");
            foreach (Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.NameUpper1, prefix + "    "));
            }
            sw.WriteLine(prefix + "    return false;");
            sw.WriteLine(prefix + "}");
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public bool NegativeCheck()");
            sw.WriteLine(prefix + "{");
            foreach (Variable var in bean.Variables)
            {
                if (var.AllowNegative)
                    continue;
                var.VariableType.Accept(new NegativeCheck(sw, var.NameUpper1, prefix + "    "));
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

        public void Visit(Bean type)
        {
            if (type.IsNeedNegativeCheck)
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck()) return true;");
        }

        public void Visit(BeanKey type)
        {
            if (type.IsNeedNegativeCheck)
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck()) return true;");
        }

        public void Visit(TypeByte type)
        {
        }

        public void Visit(TypeDouble type)
        {
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
        }

        public void Visit(TypeBool type)
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
                sw.WriteLine(prefix + "foreach (var _v_ in " + varname + ")");
                sw.WriteLine(prefix + "{");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeSet type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "foreach (var _v_ in " + varname + ")");
                sw.WriteLine(prefix + "{");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeMap type)
        {
            if (type.IsNeedNegativeCheck)
            {
                sw.WriteLine(prefix + "foreach (var _v_ in " + varname + ".Values)");
                sw.WriteLine(prefix + "{");
                type.ValueType.Accept(new NegativeCheck(sw, "_v_", prefix + "    "));
                sw.WriteLine(prefix + "}");
            }
        }

        public void Visit(TypeFloat type)
        {
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
        }

        public void Visit(TypeDynamic type)
        {
            if (type.IsNeedNegativeCheck)
                sw.WriteLine(prefix + "if (" + varname + ".NegativeCheck()) return true;");
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
            sw.WriteLine(prefix + "if (" + varname + " < 0) return true;");
        }
    }
}
