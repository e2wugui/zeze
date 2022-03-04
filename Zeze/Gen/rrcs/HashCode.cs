using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrcs
{
    public class HashCode : Visitor
    {
		public static void Make(BeanKey bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "public override int GetHashCode()");
			sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    const int _prime_ = 31;");
            sw.WriteLine(prefix + "    int _h_ = 0;");
            foreach (Variable var in bean.Variables)
			{
				HashCode e = new HashCode(var.NamePrivate);
				var.VariableType.Accept(e);
				sw.WriteLine(prefix + "    _h_ = _h_ * _prime_ + " + e.text + ";");
			}
			sw.WriteLine(prefix + "    return _h_;");
			sw.WriteLine(prefix + "}");
			sw.WriteLine();
		}

        readonly string varname;
        string text;

        public HashCode(string varname)
        {
            this.varname = varname;
        }

        public void Visit(Bean type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(BeanKey type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeByte type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeDouble type)
        {
            //text = "(int)System.BitConverter.DoubleToInt64Bits(" + varname + ")";
            throw new NotImplementedException();
        }

        public void Visit(TypeInt type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeLong type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeBool type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeBinary type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeString type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeList type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeSet type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeMap type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeFloat type)
        {
            text = "System.BitConverter.SingleToInt32Bits(" + varname + ").GetHashCode()";
        }

        public void Visit(TypeShort type)
        {
            text = varname + ".GetHashCode()";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
	}
}
