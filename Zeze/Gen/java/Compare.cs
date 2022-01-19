using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Compare : Visitor
	{
		public static void Make(BeanKey bean, StreamWriter sw, string prefix)
		{
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public int compareTo(" + bean.Name + " _o_) {");
            sw.WriteLine(prefix + "    if (_o_ == this) return 0;");
            sw.WriteLine(prefix + "    if (_o_ != null) {");
            sw.WriteLine(prefix + "        int _c_" + (bean.Variables.Count > 0 ? ";" : " = 0;"));
            foreach (Variable var in bean.Variables)
			{
                Compare e = new Compare(var, "_o_");
				var.VariableType.Accept(e);
				sw.WriteLine(prefix + "        _c_ = " + e.text + ";");
                sw.WriteLine(prefix + "        if (_c_ != 0) return _c_;");
			}
			sw.WriteLine(prefix + "        return _c_;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    throw new NullPointerException(\"compareTo: another object is null\");");
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
		}

        readonly Variable variable;
        readonly string another;
        string text;
        
        public Compare(Variable var, string another)
        {
            variable = var;
            this.another = another;
        }

        public void Visit(TypeBool type)
        {
            text = $"Boolean.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeByte type)
        {
            text = $"Byte.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeShort type)
        {
            text = $"Short.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeInt type)
        {
            text = $"Integer.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeLong type)
        {
            text = $"Long.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeFloat type)
        {
            text = $"Float.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeDouble type)
        {
            text = $"Double.compare({variable.NamePrivate}, {another}.{variable.NamePrivate})";
        }

        public void Visit(TypeBinary type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeString type)
        {
            text = variable.NamePrivate + ".compareTo(" + another + "." + variable.NamePrivate + ")";
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
            text = variable.NamePrivate + ".compareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(BeanKey type)
        {
            text = variable.NamePrivate + ".compareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
    }
}
