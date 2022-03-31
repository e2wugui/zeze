using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class HashCode : Visitor
    {
		public static void Make(BeanKey bean, StreamWriter sw, string prefix)
		{
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int hashCode() {");
            sw.WriteLine(prefix + "    final int _prime_ = 31;");
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

        public void Visit(TypeBool type)
        {
            text = $"Boolean.hashCode({varname})";
        }

        public void Visit(TypeByte type)
        {
            text = $"Byte.hashCode({varname})";
        }

        public void Visit(TypeShort type)
        {
            text = $"Short.hashCode({varname})";
        }

        public void Visit(TypeInt type)
        {
            text = $"Integer.hashCode({varname})";
        }

        public void Visit(TypeLong type)
        {
            text = $"Long.hashCode({varname})";
        }

        public void Visit(TypeFloat type)
        {
            text = $"Float.hashCode({varname})";
        }

        public void Visit(TypeDouble type)
        {
            text = $"Double.hashCode({varname})";
        }

        public void Visit(TypeBinary type)
        {
            text = varname + ".hashCode()";
        }

        public void Visit(TypeString type)
        {
            text = varname + ".hashCode()";
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

        public void Visit(Bean type)
        {
            text = varname + ".hashCode()";
        }

        public void Visit(BeanKey type)
        {
            text = varname + ".hashCode()";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
	}
}
