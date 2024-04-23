using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
{
    public class Construct : Visitor
    {
		readonly StreamWriter sw;
		readonly Variable variable;
		readonly string prefix;
        readonly string beanName;

        public static void MakeHpp(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + $"{bean.Name}();");
            var hasImmutable = false;
            foreach (var var in bean.Variables)
            {
                if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
                    hasImmutable = true;
            }
            if (hasImmutable)
            {
                sw.Write(prefix + $"{bean.Name}(");
                var first = true;
                foreach (var var in bean.Variables)
                {
                    if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
                    {
                        if (first)
                            first = false;
                        else
                            sw.Write(", ");
                        sw.Write($"{ParamName.GetName(var.VariableType)} {var.NameUpper1}_");
                    }
                }

                sw.WriteLine(");");
            }
        }

        public static void MakeCpp(Bean bean, StreamWriter sw, string prefix)
		{
			sw.WriteLine(prefix + $"{bean.Name}::{bean.Name}()");
            var dot = ": ";
            foreach (var v in bean.Variables)
            {
                if (v.VariableType is TypeDynamic)
                {
                    sw.WriteLine(prefix + $"    {dot}{v.NameUpper1}(GetSpecialTypeIdFromBean_{v.Id}, CreateBeanFromSpecialTypeId_{v.Id})");
                    dot = ", ";
                }
            }
            sw.WriteLine(prefix + "{");
            var hasImmutable = false;
            foreach (var var in bean.Variables)
            {
                if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
                    hasImmutable = true;
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
            }
            sw.WriteLine(prefix + "}");
			sw.WriteLine();
            if (hasImmutable)
            {
                sw.Write(prefix + $"{bean.Name}::{bean.Name}(");
                var first = true;
                foreach (var var in bean.Variables)
                {
                    if (var.VariableType.IsImmutable && false == bean.Version.Equals(var.Name))
                    {
                        if (first)
                            first = false;
                        else
                            sw.Write(", ");
                        sw.Write($"{ParamName.GetName(var.VariableType)} {var.NameUpper1}_");
                    }
                }

                sw.WriteLine(")");
                dot = ": ";
                foreach (var v in bean.Variables)
                {
                    if (v.VariableType is TypeDynamic)
                    {
                        sw.WriteLine(prefix + $"    {dot}{v.NameUpper1}(GetSpecialTypeIdFromBean_{v.Id}, CreateBeanFromSpecialTypeId_{v.Id})");
                        dot = ", ";
                    }
                }
                sw.WriteLine(prefix + "{");
                foreach (var var in bean.Variables)
                {
                    if (var.VariableType.IsImmutable)
                    {
                        if (false == bean.Version.Equals(var.Name))
                        {
                            sw.WriteLine($"{prefix}    {var.NameUpper1} = {var.NameUpper1}_;");
                        }
                    }
                    else
                        var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
                }

                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "// for decode only");
            sw.WriteLine(prefix + bean.Name + "() {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Construct(sw, var, prefix + "    ", bean.Name));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Construct(StreamWriter sw, Variable variable, string prefix, string beanName)
		{
			this.sw = sw;
			this.variable = variable;
			this.prefix = prefix;
            this.beanName = beanName;
        }

		void Initial(string def)
		{
            string value = variable.Initial;
            sw.WriteLine(prefix + variable.NameUpper1 + $" = {(value.Length > 0 ? value : def)};");
        }

        public void Visit(TypeBool type)
        {
            Initial("false");
        }

        public void Visit(TypeByte type)
        {
            Initial("0");
        }

        public void Visit(TypeShort type)
        {
            Initial("0");
        }

        public void Visit(TypeInt type)
        {
            Initial("0");
        }

        public void Visit(TypeLong type)
        {
            Initial("0");
        }

        public void Visit(TypeFloat type)
        {
            Initial("0.0f");
        }

        public void Visit(TypeDouble type)
        {
            Initial("0.0");
        }

        public void Visit(TypeBinary type)
        {
        }

        public void Visit(TypeString type)
        {
            string value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine(prefix + variable.NameUpper1 + " = \"" + value + "\";");
        }

        public void Visit(TypeList type)
        {
        }

        public void Visit(TypeSet type)
        {
        }

        public void Visit(TypeMap type)
        {
        }

        public void Visit(Bean type)
        {
            var value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine($"{prefix}{variable.NamePrivate} = {TypeName.GetName(type)}({value});");
        }

        public void Visit(BeanKey type)
        {
            var value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine($"{prefix}{variable.NamePrivate} = {TypeName.GetName(type)}({value});");
        }

        public void Visit(TypeDynamic type)
        {
        }

        public void InitialVector(Type type)
        {
            var value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine($"{prefix}{variable.NamePrivate} = {TypeName.GetName(type)}({value});");
        }

        public void Visit(TypeVector2 type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector2Int type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector3 type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector3Int type)
        {
            InitialVector(type);
        }

        public void Visit(TypeVector4 type)
        {
            InitialVector(type);
        }

        public void Visit(TypeQuaternion type)
        {
            InitialVector(type);
        }

        public void Visit(TypeDecimal type)
        {
            string value = variable.Initial;
            if (value.Length > 0)
                sw.WriteLine(prefix + variable.NameUpper1 + " = \"" + value + "\";");
        }
    }
}
