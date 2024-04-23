using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Compare : Visitor
	{
		public static void Make(BeanKey bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + "public int CompareTo(object _o1_)");
			sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    if (_o1_ == this) return 0;");
            sw.WriteLine(prefix + "    if (_o1_ is " + bean.Name + " _o_)");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        int _c_" + (bean.VariablesIdOrder.Count > 0 ? ";" : " = 0;"));
            foreach (Variable var in bean.VariablesIdOrder)
			{
                Compare e = new Compare(var, "_o_");
				var.VariableType.Accept(e);
				sw.WriteLine(prefix + "        _c_ = " + e.text + ";");
                sw.WriteLine(prefix + "        if (_c_ != 0) return _c_;");
			}
			sw.WriteLine(prefix + "        return _c_;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    throw new Exception(\"CompareTo: another object is not " + bean.FullName + "\");");
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

        public void Visit(Bean type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(BeanKey type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeByte type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeDouble type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeInt type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeLong type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeBool type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeBinary type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeString type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
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
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeShort type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeQuaternion type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeDecimal type)
        {
            text = variable.NamePrivate + ".CompareTo(" + another + "." + variable.NamePrivate + ")";
        }
    }
}
