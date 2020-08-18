using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class HashCode : Visitor
    {
		public static void Make(BeanKey bean, System.IO.StreamWriter sw, String prefix)
		{
			sw.WriteLine(prefix + "public override int GetHashCode()");
			sw.WriteLine(prefix + "{");
			sw.WriteLine(prefix + "    int _h_ = 0;");
			foreach (Variable var in bean.Variables)
			{
				HashCode e = new HashCode(var.NamePrivate);
				var.VariableType.Accept(e);
				sw.WriteLine(prefix + "    _h_ += " + e.text + ";");
			}
			sw.WriteLine(prefix + "    return _h_;");
			sw.WriteLine(prefix + "}");
			sw.WriteLine("");
		}

        private String varname;
        private String text;

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
            text = "(int)" + varname;
        }

        public void Visit(TypeDouble type)
        {
            text = "(int)System.BitConverter.DoubleToInt64Bits(" + varname + ")";
            throw new NotImplementedException();
        }

        public void Visit(TypeInt type)
        {
            text = varname;
        }

        public void Visit(TypeLong type)
        {
            text = "(int)" + varname;
        }

        public void Visit(TypeBool type)
        {
            text = varname + " ? 1 : 0";
        }

        public void Visit(TypeBinary type)
        {
            throw new NotImplementedException();
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
            text = "System.BitConverter.SingleToInt32Bits(" + varname + ")";
        }

        public void Visit(TypeShort type)
        {
            text = varname;
        }
	}
}
