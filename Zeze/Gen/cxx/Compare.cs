using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
{
    public class Compare : Visitor
	{
		public static void Make(BeanKey bean, StreamWriter sw, string prefix)
		{
            sw.WriteLine(prefix + "int CompareTo(const " + bean.Name + "& _o_) const {");
            sw.WriteLine(prefix + "    if (&_o_ == this)");
            sw.WriteLine(prefix + "        return 0;");
            sw.WriteLine(prefix + "    int _c_" + (bean.VariablesIdOrder.Count > 0 ? ";" : " = 0;"));
            foreach (Variable var in bean.VariablesIdOrder)
            {
                Compare e = new Compare(var, "_o_");
                var.VariableType.Accept(e);
                sw.WriteLine(prefix + "    _c_ = " + e.text + ";");
                sw.WriteLine(prefix + "    if (_c_ != 0)");
                sw.WriteLine(prefix + "        return _c_;");
            }
            sw.WriteLine(prefix + "    return _c_;");
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
            sw.WriteLine($"{prefix}bool operator<(const {bean.Name}& _o_) const {{");
            sw.WriteLine($"{prefix}    return CompareTo(_o_) < 0;");
            sw.WriteLine($"{prefix}}}");
            // sw.WriteLine();
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
            text = $"Zeze::Boolean::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeByte type)
        {
            text = $"Zeze::Byte::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeShort type)
        {
            text = $"Zeze::Short::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeInt type)
        {
            text = $"Zeze::Integer::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeLong type)
        {
            text = $"Zeze::Long::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeFloat type)
        {
            text = $"Zeze::Float::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeDouble type)
        {
            text = $"Zeze::Double::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeBinary type)
        {
            text = $"Zeze::String::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }

        public void Visit(TypeString type)
        {
            text = $"Zeze::String::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
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
            throw new NotImplementedException();
        }

        public void Visit(BeanKey type)
        {
            text = variable.NameUpper1 + ".CompareTo(" + another + "." + variable.NameUpper1 + ")";
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
            text = variable.NameUpper1 + ".CompareTo(" + another + "." + variable.NameUpper1 + ")";
        }

        public void Visit(TypeVector3 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            text = variable.NameUpper1 + ".CompareTo(" + another + "." + variable.NameUpper1 + ")";
        }

        public void Visit(TypeVector4 type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeDecimal type)
        {
            text = $"Zeze::String::Compare({variable.NameUpper1}, {another}.{variable.NameUpper1})";
        }
    }
}
